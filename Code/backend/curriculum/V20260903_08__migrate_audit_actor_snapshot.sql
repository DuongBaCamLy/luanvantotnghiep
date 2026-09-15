SET NAMES utf8mb4;
USE curriculum_iu;

-- =========================================================
-- Audit actor architecture migration
--
-- Goal:
-- 1. user_account contains business/SRS actors only.
-- 2. audit_log keeps immutable historical actor identity.
-- 3. Legacy "system" account is preserved only as an
--    audit_actor_snapshot row, then removed from user_account.
-- 4. Background audit events fall back to the configured Admin
--    in application code, not to a technical SYSTEM login.
--
-- IMPORTANT:
-- - This migration NEVER updates or deletes audit_log rows.
-- - This migration NEVER disables foreign-key checks.
-- - This migration does NOT modify the immutable audit_log
--   UPDATE/DELETE triggers.
-- =========================================================


-- ---------------------------------------------------------
-- 1. Persistent actor identity table for Audit Log
-- ---------------------------------------------------------

CREATE TABLE IF NOT EXISTS audit_actor_snapshot (
    user_id INT NOT NULL,
    username VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    actor_type VARCHAR(30) NOT NULL,
    captured_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;


-- ---------------------------------------------------------
-- 2. Safe reconciliation procedure
-- ---------------------------------------------------------

DROP PROCEDURE IF EXISTS migrate_audit_actor_snapshot;

DELIMITER $$

CREATE PROCEDURE migrate_audit_actor_snapshot()
BEGIN
    DECLARE v_admin_count INT DEFAULT 0;

    DECLARE v_system_count INT DEFAULT 0;
    DECLARE v_system_id INT DEFAULT NULL;
    DECLARE v_system_active BOOLEAN DEFAULT FALSE;

    DECLARE v_business_ref_count BIGINT DEFAULT 0;
    DECLARE v_missing_snapshot_count BIGINT DEFAULT 0;

    DECLARE v_old_fk_count INT DEFAULT 0;
    DECLARE v_old_fk_name VARCHAR(128) DEFAULT NULL;

    DECLARE v_snapshot_fk_count INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;


    -- -----------------------------------------------------
    -- Precondition: exactly one active Administrator named
    -- "admin" must exist because the application uses it as
    -- fallback for scheduler/background audit events.
    -- -----------------------------------------------------

    SELECT COUNT(*)
      INTO v_admin_count
    FROM user_account
    WHERE LOWER(TRIM(username)) = 'admin'
      AND role = 'ADMIN'
      AND is_active = 1;

    IF v_admin_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Audit migration stopped: expected exactly one active ADMIN account named admin.';
    END IF;


    -- -----------------------------------------------------
    -- Find the legacy technical account if it still exists.
    -- Running this script again after migration is allowed.
    -- -----------------------------------------------------

    SELECT COUNT(*), MIN(id)
      INTO v_system_count, v_system_id
    FROM user_account
    WHERE LOWER(TRIM(username)) = 'system';

    IF v_system_count > 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Audit migration stopped: multiple user_account rows named system exist.';
    END IF;

    IF v_system_count = 1 THEN

        SELECT is_active
          INTO v_system_active
        FROM user_account
        WHERE id = v_system_id;

        IF COALESCE(v_system_active, FALSE) = TRUE THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT =
                    'Audit migration stopped: legacy system account is still active.';
        END IF;


        -- -------------------------------------------------
        -- A technical system account must not own any
        -- business data. audit_log is intentionally excluded.
        -- -------------------------------------------------

        SELECT
              (SELECT COUNT(*)
                 FROM approval_request
                WHERE requested_by = v_system_id
                   OR reviewed_by = v_system_id)

            + (SELECT COUNT(*)
                 FROM notification
                WHERE user_id = v_system_id)

            + (SELECT COUNT(*)
                 FROM source_document
                WHERE uploaded_by = v_system_id)

            + (SELECT COUNT(*)
                 FROM student
                WHERE user_id = v_system_id)

            + (SELECT COUNT(*)
                 FROM student_score
                WHERE recorded_by = v_system_id)

            + (SELECT COUNT(*)
                 FROM syllabus
                WHERE created_by = v_system_id
                   OR approved_by = v_system_id)

            + (SELECT COUNT(*)
                 FROM syllabus_deadline
                WHERE created_by = v_system_id
                   OR updated_by = v_system_id)

            + (SELECT COUNT(*)
                 FROM syllabus_deadline_escalation_log
                WHERE recipient_user_id = v_system_id)

            + (SELECT COUNT(*)
                 FROM syllabus_deadline_reminder_log
                WHERE recipient_user_id = v_system_id)

            + (SELECT COUNT(*)
                 FROM syllabus_import_history
                WHERE imported_by = v_system_id)

          INTO v_business_ref_count;

        IF v_business_ref_count <> 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT =
                    'Audit migration stopped: legacy system account still owns business records.';
        END IF;

    END IF;


    -- -----------------------------------------------------
    -- Snapshot all current business users.
    --
    -- captured_at is intentionally not overwritten on an
    -- existing snapshot.
    -- -----------------------------------------------------

    START TRANSACTION;

    INSERT INTO audit_actor_snapshot (
        user_id,
        username,
        role,
        actor_type,
        captured_at
    )
    SELECT
        ua.id,
        ua.username,
        ua.role,
        CASE
            WHEN LOWER(TRIM(ua.username)) = 'system'
                THEN 'LEGACY_SYSTEM'
            ELSE 'BUSINESS_USER'
        END,
        NOW()
    FROM user_account ua
    ON DUPLICATE KEY UPDATE
        username = VALUES(username),
        role = VALUES(role),
        actor_type =
            CASE
                WHEN audit_actor_snapshot.actor_type = 'LEGACY_SYSTEM'
                    THEN 'LEGACY_SYSTEM'
                ELSE VALUES(actor_type)
            END;

    IF v_system_count = 1 THEN
        UPDATE audit_actor_snapshot
        SET actor_type = 'LEGACY_SYSTEM'
        WHERE user_id = v_system_id;
    END IF;


    -- -----------------------------------------------------
    -- Every existing immutable Audit Log row must have an
    -- actor snapshot BEFORE the foreign key is switched.
    -- -----------------------------------------------------

    SELECT COUNT(*)
      INTO v_missing_snapshot_count
    FROM audit_log a
    LEFT JOIN audit_actor_snapshot s
        ON s.user_id = a.changed_by
    WHERE s.user_id IS NULL;

    IF v_missing_snapshot_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Audit migration stopped: audit_log contains actors without snapshots.';
    END IF;

    COMMIT;


    -- -----------------------------------------------------
    -- 3. Switch audit_log.changed_by:
    --
    -- old:
    -- audit_log -> user_account
    --
    -- new:
    -- audit_log -> audit_actor_snapshot
    --
    -- This is idempotent and also works when the migration
    -- has already been applied.
    -- -----------------------------------------------------

    SELECT COUNT(*), MIN(CONSTRAINT_NAME)
      INTO v_old_fk_count, v_old_fk_name
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'audit_log'
      AND COLUMN_NAME = 'changed_by'
      AND REFERENCED_TABLE_NAME = 'user_account';

    IF v_old_fk_count > 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Audit migration stopped: multiple user_account foreign keys exist on audit_log.changed_by.';
    END IF;


    SELECT COUNT(*)
      INTO v_snapshot_fk_count
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'audit_log'
      AND COLUMN_NAME = 'changed_by'
      AND REFERENCED_TABLE_NAME = 'audit_actor_snapshot'
      AND REFERENCED_COLUMN_NAME = 'user_id';

    IF v_snapshot_fk_count > 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Audit migration stopped: multiple snapshot foreign keys exist on audit_log.changed_by.';
    END IF;


    IF v_snapshot_fk_count = 0 THEN

        IF v_old_fk_count = 1 THEN

            SET @audit_fk_ddl = CONCAT(
                'ALTER TABLE audit_log ',
                'DROP FOREIGN KEY `',
                REPLACE(v_old_fk_name, '`', '``'),
                '`, ',
                'ADD CONSTRAINT `fk_audit_actor_snapshot` ',
                'FOREIGN KEY (`changed_by`) ',
                'REFERENCES `audit_actor_snapshot` (`user_id`) ',
                'ON UPDATE NO ACTION ',
                'ON DELETE NO ACTION'
            );

        ELSE

            SET @audit_fk_ddl =
                'ALTER TABLE audit_log
                 ADD CONSTRAINT `fk_audit_actor_snapshot`
                 FOREIGN KEY (`changed_by`)
                 REFERENCES `audit_actor_snapshot` (`user_id`)
                 ON UPDATE NO ACTION
                 ON DELETE NO ACTION';

        END IF;

        PREPARE audit_fk_stmt FROM @audit_fk_ddl;
        EXECUTE audit_fk_stmt;
        DEALLOCATE PREPARE audit_fk_stmt;

    ELSEIF v_old_fk_count = 1 THEN

        SET @audit_fk_ddl = CONCAT(
            'ALTER TABLE audit_log DROP FOREIGN KEY `',
            REPLACE(v_old_fk_name, '`', '``'),
            '`'
        );

        PREPARE audit_fk_stmt FROM @audit_fk_ddl;
        EXECUTE audit_fk_stmt;
        DEALLOCATE PREPARE audit_fk_stmt;

    END IF;


    -- -----------------------------------------------------
    -- Verify again after FK migration.
    -- -----------------------------------------------------

    SELECT COUNT(*)
      INTO v_missing_snapshot_count
    FROM audit_log a
    LEFT JOIN audit_actor_snapshot s
        ON s.user_id = a.changed_by
    WHERE s.user_id IS NULL;

    IF v_missing_snapshot_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Audit migration stopped: actor snapshots became inconsistent after FK migration.';
    END IF;


    -- -----------------------------------------------------
    -- 4. Remove the legacy technical login account.
    --
    -- Password-reset tokens are transient authentication
    -- data, not immutable academic/audit history.
    -- -----------------------------------------------------

    IF v_system_count = 1 THEN

        START TRANSACTION;

        DELETE FROM password_reset_token
        WHERE user_id = v_system_id;

        DELETE FROM user_account
        WHERE id = v_system_id
          AND LOWER(TRIM(username)) = 'system'
          AND is_active = 0;

        IF ROW_COUNT() <> 1 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT =
                    'Audit migration stopped: legacy system account could not be deleted safely.';
        END IF;

        COMMIT;

    END IF;

END$$

DELIMITER ;

CALL migrate_audit_actor_snapshot();

DROP PROCEDURE IF EXISTS migrate_audit_actor_snapshot;


-- ---------------------------------------------------------
-- 5. Keep live business-user snapshots synchronized.
--
-- There is intentionally NO DELETE trigger:
-- actor identity must survive account deletion.
-- ---------------------------------------------------------

DROP TRIGGER IF EXISTS trg_user_account_audit_actor_insert;

DELIMITER $$

CREATE TRIGGER trg_user_account_audit_actor_insert
AFTER INSERT ON user_account
FOR EACH ROW
BEGIN
    INSERT INTO audit_actor_snapshot (
        user_id,
        username,
        role,
        actor_type,
        captured_at
    )
    VALUES (
        NEW.id,
        NEW.username,
        NEW.role,
        'BUSINESS_USER',
        NOW()
    )
    ON DUPLICATE KEY UPDATE
        username = NEW.username,
        role = NEW.role;
END$$

DELIMITER ;


DROP TRIGGER IF EXISTS trg_user_account_audit_actor_update;

DELIMITER $$

CREATE TRIGGER trg_user_account_audit_actor_update
AFTER UPDATE ON user_account
FOR EACH ROW
BEGIN
    UPDATE audit_actor_snapshot
    SET
        username = NEW.username,
        role = NEW.role
    WHERE user_id = NEW.id;
END$$

DELIMITER ;


-- ---------------------------------------------------------
-- 6. Verification output
-- ---------------------------------------------------------

SELECT
    kcu.CONSTRAINT_NAME,
    kcu.TABLE_NAME,
    kcu.COLUMN_NAME,
    kcu.REFERENCED_TABLE_NAME,
    kcu.REFERENCED_COLUMN_NAME,
    rc.DELETE_RULE,
    rc.UPDATE_RULE
FROM information_schema.KEY_COLUMN_USAGE kcu
JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
    ON rc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
   AND rc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
WHERE kcu.CONSTRAINT_SCHEMA = DATABASE()
  AND kcu.TABLE_NAME = 'audit_log'
  AND kcu.COLUMN_NAME = 'changed_by';


SELECT COUNT(*) AS audit_rows_without_snapshot
FROM audit_log a
LEFT JOIN audit_actor_snapshot s
    ON s.user_id = a.changed_by
WHERE s.user_id IS NULL;


SELECT COUNT(*) AS system_accounts_remaining
FROM user_account
WHERE LOWER(TRIM(username)) = 'system';


SELECT COUNT(*) AS active_admin_accounts
FROM user_account
WHERE LOWER(TRIM(username)) = 'admin'
  AND role = 'ADMIN'
  AND is_active = 1;


SELECT
    user_id,
    username,
    role,
    actor_type,
    captured_at
FROM audit_actor_snapshot
WHERE actor_type = 'LEGACY_SYSTEM'
ORDER BY user_id;


SELECT
    TRIGGER_NAME,
    ACTION_TIMING,
    EVENT_MANIPULATION,
    EVENT_OBJECT_TABLE
FROM information_schema.TRIGGERS
WHERE TRIGGER_SCHEMA = DATABASE()
  AND TRIGGER_NAME IN (
      'trg_user_account_audit_actor_insert',
      'trg_user_account_audit_actor_update'
  )
ORDER BY TRIGGER_NAME;