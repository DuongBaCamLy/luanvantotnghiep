-- =============================================================================
-- Migrate ClassSection teaching assignments
--
-- Legacy:
--   class_section.instructor_id -> instructor.id
--
-- Target:
--   class_section.instructor_user_id -> user_account.id
--
-- Safety principles:
--   1. Never assign user id 0.
--   2. Never create fake UserAccount rows.
--   3. Never delete ClassSection rows.
--   4. Never silently ignore unresolved assignments.
--   5. Preserve already-valid instructor_user_id values.
--   6. Prefer legacy user_account.instructor_id mapping.
--   7. Use normalized email only as a fallback.
-- =============================================================================

DROP PROCEDURE IF EXISTS migrate_class_section_instructor_user;

DELIMITER $$

CREATE PROCEDURE migrate_class_section_instructor_user()
BEGIN
    DECLARE v_count INT DEFAULT 0;
    DECLARE v_has_legacy_cs_column INT DEFAULT 0;
    DECLARE v_has_target_column INT DEFAULT 0;
    DECLARE v_has_user_legacy_column INT DEFAULT 0;
    DECLARE v_has_instructor_table INT DEFAULT 0;
    DECLARE v_target_nullable VARCHAR(3);
    DECLARE v_fk_count INT DEFAULT 0;
    DECLARE v_fk_valid_count INT DEFAULT 0;
    DECLARE v_index_count INT DEFAULT 0;
    DECLARE v_unique_count INT DEFAULT 0;

    -- -------------------------------------------------------------------------
    -- 1. Basic schema inspection
    -- -------------------------------------------------------------------------

    SELECT COUNT(*)
    INTO v_count
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'class_section';

    IF v_count = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Migration aborted: class_section table does not exist.';
    END IF;

    SELECT COUNT(*)
    INTO v_count
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'user_account';

    IF v_count = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Migration aborted: user_account table does not exist.';
    END IF;

    SELECT COUNT(*)
    INTO v_has_legacy_cs_column
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'class_section'
      AND column_name = 'instructor_id';

    SELECT COUNT(*)
    INTO v_has_target_column
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'class_section'
      AND column_name = 'instructor_user_id';

    SELECT COUNT(*)
    INTO v_has_user_legacy_column
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'user_account'
      AND column_name = 'instructor_id';

    SELECT COUNT(*)
    INTO v_has_instructor_table
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'instructor';

    -- -------------------------------------------------------------------------
    -- 2. Add target column as NULLABLE first.
    --
    -- This is deliberately NOT added as NOT NULL because existing rows need
    -- to be backfilled before the constraint can safely be enforced.
    -- -------------------------------------------------------------------------

    IF v_has_target_column = 0 THEN
        ALTER TABLE class_section
            ADD COLUMN instructor_user_id INT NULL
            AFTER syllabus_id;

        SET v_has_target_column = 1;
    END IF;

    -- A partially migrated database may already have the target column as
    -- NOT NULL. Temporarily allow NULL so invalid legacy 0 values can be
    -- repaired safely before final validation.
    SELECT is_nullable
    INTO v_target_nullable
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'class_section'
      AND column_name = 'instructor_user_id'
    LIMIT 1;

    IF v_target_nullable = 'NO' THEN
        ALTER TABLE class_section
            MODIFY COLUMN instructor_user_id INT NULL;
    END IF;

    -- -------------------------------------------------------------------------
    -- 3. Normalize the bad Hibernate-generated value 0.
    --
    -- 0 is not a valid mapped account and must be treated as unresolved.
    -- -------------------------------------------------------------------------

    UPDATE class_section
    SET instructor_user_id = NULL
    WHERE instructor_user_id = 0;

    -- -------------------------------------------------------------------------
    -- 4. Validate already-populated target values.
    --
    -- Existing non-null mappings are preserved, but only if they point to a
    -- real UserAccount whose role is INSTRUCTOR.
    -- -------------------------------------------------------------------------

    SELECT COUNT(*)
    INTO v_count
    FROM class_section cs
    LEFT JOIN user_account ua
        ON ua.id = cs.instructor_user_id
    WHERE cs.instructor_user_id IS NOT NULL
      AND (
          ua.id IS NULL
          OR ua.role <> 'INSTRUCTOR'
      );

    IF v_count > 0 THEN

        SELECT
            cs.id AS class_section_id,
            cs.instructor_user_id,
            ua.username,
            ua.email,
            ua.role
        FROM class_section cs
        LEFT JOIN user_account ua
            ON ua.id = cs.instructor_user_id
        WHERE cs.instructor_user_id IS NOT NULL
          AND (
              ua.id IS NULL
              OR ua.role <> 'INSTRUCTOR'
          );

        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Migration aborted: existing instructor_user_id points to a missing or non-INSTRUCTOR account.';
    END IF;

    -- -------------------------------------------------------------------------
    -- 4b. Detect conflicts between an already-populated target value and the
    -- legacy instructor identity.
    --
    -- A valid target UserAccount is not enough if the legacy relationship
    -- uniquely identifies a different INSTRUCTOR account. Fail rather than
    -- silently choosing one side.
    -- -------------------------------------------------------------------------

    IF v_has_legacy_cs_column = 1
       AND v_has_user_legacy_column = 1 THEN

        -- Direct legacy relationship has highest priority.
        SELECT COUNT(*)
        INTO v_count
        FROM class_section cs
        JOIN (
            SELECT
                instructor_id,
                MIN(id) AS user_id
            FROM user_account
            WHERE role = 'INSTRUCTOR'
              AND instructor_id IS NOT NULL
            GROUP BY instructor_id
            HAVING COUNT(*) = 1
        ) mapped_user
            ON mapped_user.instructor_id = cs.instructor_id
        WHERE cs.instructor_user_id IS NOT NULL
          AND cs.instructor_user_id <> mapped_user.user_id;

        IF v_count > 0 THEN

            SELECT
                cs.id AS class_section_id,
                cs.instructor_id AS legacy_instructor_id,
                cs.instructor_user_id AS existing_instructor_user_id,
                mapped_user.user_id AS expected_instructor_user_id
            FROM class_section cs
            JOIN (
                SELECT
                    instructor_id,
                    MIN(id) AS user_id
                FROM user_account
                WHERE role = 'INSTRUCTOR'
                  AND instructor_id IS NOT NULL
                GROUP BY instructor_id
                HAVING COUNT(*) = 1
            ) mapped_user
                ON mapped_user.instructor_id = cs.instructor_id
            WHERE cs.instructor_user_id IS NOT NULL
              AND cs.instructor_user_id <> mapped_user.user_id
            ORDER BY cs.id;

            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT =
                    'Migration aborted: existing instructor_user_id conflicts with legacy direct mapping.';
        END IF;

    END IF;

    IF v_has_legacy_cs_column = 1
       AND v_has_instructor_table = 1 THEN

        -- Email is only the fallback identity. It is checked only when no
        -- eligible direct legacy UserAccount mapping exists.
        IF v_has_user_legacy_column = 1 THEN

            SELECT COUNT(*)
            INTO v_count
            FROM class_section cs
            JOIN instructor i
                ON i.id = cs.instructor_id
            JOIN (
                SELECT
                    LOWER(TRIM(email)) AS normalized_email,
                    MIN(id) AS user_id
                FROM user_account
                WHERE role = 'INSTRUCTOR'
                  AND email IS NOT NULL
                  AND TRIM(email) <> ''
                GROUP BY LOWER(TRIM(email))
                HAVING COUNT(*) = 1
            ) mapped_email
                ON mapped_email.normalized_email
                   = LOWER(TRIM(i.email))
            WHERE cs.instructor_user_id IS NOT NULL
              AND i.email IS NOT NULL
              AND TRIM(i.email) <> ''
              AND NOT EXISTS (
                  SELECT 1
                  FROM user_account direct_ua
                  WHERE direct_ua.role = 'INSTRUCTOR'
                    AND direct_ua.instructor_id = cs.instructor_id
              )
              AND cs.instructor_user_id <> mapped_email.user_id;

        ELSE

            SELECT COUNT(*)
            INTO v_count
            FROM class_section cs
            JOIN instructor i
                ON i.id = cs.instructor_id
            JOIN (
                SELECT
                    LOWER(TRIM(email)) AS normalized_email,
                    MIN(id) AS user_id
                FROM user_account
                WHERE role = 'INSTRUCTOR'
                  AND email IS NOT NULL
                  AND TRIM(email) <> ''
                GROUP BY LOWER(TRIM(email))
                HAVING COUNT(*) = 1
            ) mapped_email
                ON mapped_email.normalized_email
                   = LOWER(TRIM(i.email))
            WHERE cs.instructor_user_id IS NOT NULL
              AND i.email IS NOT NULL
              AND TRIM(i.email) <> ''
              AND cs.instructor_user_id <> mapped_email.user_id;

        END IF;

        IF v_count > 0 THEN

            SELECT
                cs.id AS class_section_id,
                cs.instructor_id AS legacy_instructor_id,
                i.email AS legacy_instructor_email,
                cs.instructor_user_id AS existing_instructor_user_id
            FROM class_section cs
            JOIN instructor i
                ON i.id = cs.instructor_id
            WHERE cs.instructor_user_id IS NOT NULL
            ORDER BY cs.id;

            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT =
                    'Migration aborted: existing instructor_user_id conflicts with legacy email mapping.';
        END IF;

    END IF;

    -- -------------------------------------------------------------------------
    -- 5. Backfill using the strongest legacy relation:
    --
    --   class_section.instructor_id
    --        =
    --   user_account.instructor_id
    --
    -- Only INSTRUCTOR accounts are eligible.
    -- -------------------------------------------------------------------------

    IF v_has_legacy_cs_column = 1
       AND v_has_user_legacy_column = 1 THEN

        -- Ambiguous direct mapping is unsafe.
        SELECT COUNT(*)
        INTO v_count
        FROM (
            SELECT cs.id
            FROM class_section cs
            JOIN user_account ua
                ON ua.instructor_id = cs.instructor_id
               AND ua.role = 'INSTRUCTOR'
            WHERE cs.instructor_user_id IS NULL
            GROUP BY cs.id
            HAVING COUNT(ua.id) > 1
        ) ambiguous_direct;

        IF v_count > 0 THEN

            SELECT
                cs.id AS class_section_id,
                cs.instructor_id,
                COUNT(ua.id) AS matching_instructor_accounts
            FROM class_section cs
            JOIN user_account ua
                ON ua.instructor_id = cs.instructor_id
               AND ua.role = 'INSTRUCTOR'
            WHERE cs.instructor_user_id IS NULL
            GROUP BY cs.id, cs.instructor_id
            HAVING COUNT(ua.id) > 1;

            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT =
                    'Migration aborted: ambiguous user_account.instructor_id mapping.';
        END IF;

        UPDATE class_section cs
        JOIN (
            SELECT
                instructor_id,
                MIN(id) AS user_id
            FROM user_account
            WHERE role = 'INSTRUCTOR'
              AND instructor_id IS NOT NULL
            GROUP BY instructor_id
            HAVING COUNT(*) = 1
        ) mapped_user
            ON mapped_user.instructor_id = cs.instructor_id
        SET cs.instructor_user_id = mapped_user.user_id
        WHERE cs.instructor_user_id IS NULL;

    END IF;

    -- -------------------------------------------------------------------------
    -- 6. Fallback mapping by normalized email.
    --
    -- Used only for rows still unresolved after the direct relationship.
    --
    -- instructor.email == user_account.email
    -- LOWER(TRIM(...)) is used for legacy formatting differences.
    -- -------------------------------------------------------------------------

    IF v_has_legacy_cs_column = 1
       AND v_has_instructor_table = 1 THEN

        SELECT COUNT(*)
        INTO v_count
        FROM (
            SELECT cs.id
            FROM class_section cs
            JOIN instructor i
                ON i.id = cs.instructor_id
            JOIN user_account ua
                ON LOWER(TRIM(ua.email))
                   = LOWER(TRIM(i.email))
               AND ua.role = 'INSTRUCTOR'
            WHERE cs.instructor_user_id IS NULL
              AND i.email IS NOT NULL
              AND TRIM(i.email) <> ''
            GROUP BY cs.id
            HAVING COUNT(ua.id) > 1
        ) ambiguous_email;

        IF v_count > 0 THEN

            SELECT
                cs.id AS class_section_id,
                cs.instructor_id,
                i.email AS legacy_instructor_email,
                COUNT(ua.id) AS matching_instructor_accounts
            FROM class_section cs
            JOIN instructor i
                ON i.id = cs.instructor_id
            JOIN user_account ua
                ON LOWER(TRIM(ua.email))
                   = LOWER(TRIM(i.email))
               AND ua.role = 'INSTRUCTOR'
            WHERE cs.instructor_user_id IS NULL
              AND i.email IS NOT NULL
              AND TRIM(i.email) <> ''
            GROUP BY
                cs.id,
                cs.instructor_id,
                i.email
            HAVING COUNT(ua.id) > 1;

            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT =
                    'Migration aborted: ambiguous normalized-email instructor mapping.';
        END IF;

        UPDATE class_section cs
        JOIN instructor i
            ON i.id = cs.instructor_id
        JOIN (
            SELECT
                LOWER(TRIM(email)) AS normalized_email,
                MIN(id) AS user_id
            FROM user_account
            WHERE role = 'INSTRUCTOR'
              AND email IS NOT NULL
              AND TRIM(email) <> ''
            GROUP BY LOWER(TRIM(email))
            HAVING COUNT(*) = 1
        ) mapped_email
            ON mapped_email.normalized_email
               = LOWER(TRIM(i.email))
        SET cs.instructor_user_id = mapped_email.user_id
        WHERE cs.instructor_user_id IS NULL;

    END IF;

    -- -------------------------------------------------------------------------
    -- 7. Fail if any ClassSection is still unresolved.
    --
    -- The migration MUST NOT delete those assignments or invent an account.
    -- -------------------------------------------------------------------------

    SELECT COUNT(*)
    INTO v_count
    FROM class_section
    WHERE instructor_user_id IS NULL;

    IF v_count > 0 THEN

        IF v_has_legacy_cs_column = 1
           AND v_has_instructor_table = 1 THEN

            SELECT
                cs.id AS class_section_id,
                cs.instructor_id AS legacy_instructor_id,
                i.staff_code,
                i.full_name,
                i.email
            FROM class_section cs
            LEFT JOIN instructor i
                ON i.id = cs.instructor_id
            WHERE cs.instructor_user_id IS NULL
            ORDER BY cs.id;

        ELSE

            SELECT
                cs.id AS class_section_id,
                cs.instructor_user_id
            FROM class_section cs
            WHERE cs.instructor_user_id IS NULL
            ORDER BY cs.id;

        END IF;

        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Migration aborted: one or more ClassSection rows could not be mapped to an INSTRUCTOR UserAccount.';
    END IF;

       -- -------------------------------------------------------------------------
    -- 8. Replace legacy ClassSection business unique keys.
    --
    -- Older databases may still have:
    --
    --   uq_class_section
    --     (course_id, semester, academic_year, group_number)
    --
    -- Newer legacy databases may have:
    --
    --   uq_class_section_context
    --     (... instructor_id ...)
    --
    -- The target constraint must use instructor_user_id.
    --
    -- Validate all required columns BEFORE dropping any existing index because
    -- MySQL DDL performs implicit commits.
    -- -------------------------------------------------------------------------

    SELECT COUNT(DISTINCT column_name)
    INTO v_count
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'class_section'
      AND column_name IN (
          'course_id',
          'program_id',
          'cohort_id',
          'instructor_user_id',
          'semester',
          'academic_year',
          'group_number'
      );

    IF v_count <> 7 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Migration aborted: class_section curriculum-context columns are incomplete.';
    END IF;

    -- Fail before any unique-key DDL if mapped rows would collide under
    -- the target ClassSection business key.
    --
    -- MySQL UNIQUE permits multiple rows when any key component is NULL,
    -- so only fully populated target keys are checked here.
    SELECT COUNT(*)
    INTO v_count
    FROM (
        SELECT
            course_id,
            program_id,
            cohort_id,
            instructor_user_id,
            semester,
            academic_year,
            group_number
        FROM class_section
        WHERE course_id IS NOT NULL
          AND program_id IS NOT NULL
          AND cohort_id IS NOT NULL
          AND instructor_user_id IS NOT NULL
          AND semester IS NOT NULL
          AND academic_year IS NOT NULL
          AND group_number IS NOT NULL
        GROUP BY
            course_id,
            program_id,
            cohort_id,
            instructor_user_id,
            semester,
            academic_year,
            group_number
        HAVING COUNT(*) > 1
    ) duplicate_target_context;

    IF v_count > 0 THEN

        SELECT
            course_id,
            program_id,
            cohort_id,
            instructor_user_id,
            semester,
            academic_year,
            group_number,
            COUNT(*) AS duplicate_count
        FROM class_section
        WHERE course_id IS NOT NULL
          AND program_id IS NOT NULL
          AND cohort_id IS NOT NULL
          AND instructor_user_id IS NOT NULL
          AND semester IS NOT NULL
          AND academic_year IS NOT NULL
          AND group_number IS NOT NULL
        GROUP BY
            course_id,
            program_id,
            cohort_id,
            instructor_user_id,
            semester,
            academic_year,
            group_number
        HAVING COUNT(*) > 1
        ORDER BY
            course_id,
            program_id,
            cohort_id,
            instructor_user_id;

        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Migration aborted: mapped ClassSection rows collide under the target unique key.';
    END IF;

    -- Remove the oldest legacy business unique key when it exists.
    SELECT COUNT(*)
    INTO v_unique_count
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'class_section'
      AND index_name = 'uq_class_section';

    IF v_unique_count > 0 THEN
        ALTER TABLE class_section
            DROP INDEX uq_class_section;
    END IF;

    -- Remove the newer legacy context key because it contains instructor_id.
    SELECT COUNT(*)
    INTO v_unique_count
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'class_section'
      AND index_name = 'uq_class_section_context';

    IF v_unique_count > 0 THEN
        ALTER TABLE class_section
            DROP INDEX uq_class_section_context;
    END IF;

    ALTER TABLE class_section
        ADD CONSTRAINT uq_class_section_context
        UNIQUE (
            course_id,
            program_id,
            cohort_id,
            instructor_user_id,
            semester,
            academic_year,
            group_number
        );

    -- -------------------------------------------------------------------------
    -- 9. Ensure an index exists with instructor_user_id as the leading column.
    -- -------------------------------------------------------------------------

    SELECT COUNT(*)
    INTO v_index_count
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'class_section'
      AND column_name = 'instructor_user_id'
      AND seq_in_index = 1;

    IF v_index_count = 0 THEN
        CREATE INDEX idx_cs_instructor_user
            ON class_section (instructor_user_id);
    END IF;

    -- -------------------------------------------------------------------------
    -- 10. Ensure target FK is either absent or valid.
    -- -------------------------------------------------------------------------

    SELECT COUNT(*)
    INTO v_fk_count
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE()
      AND table_name = 'class_section'
      AND column_name = 'instructor_user_id'
      AND referenced_table_name IS NOT NULL;

    SELECT COUNT(*)
    INTO v_fk_valid_count
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE()
      AND table_name = 'class_section'
      AND column_name = 'instructor_user_id'
      AND referenced_table_name = 'user_account'
      AND referenced_column_name = 'id';

    IF v_fk_count > 0
       AND v_fk_valid_count <> v_fk_count THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Migration aborted: instructor_user_id has an unexpected foreign key.';
    END IF;

    IF v_fk_count = 0 THEN
        ALTER TABLE class_section
            ADD CONSTRAINT fk_cs_instructor_user
            FOREIGN KEY (instructor_user_id)
            REFERENCES user_account(id);
    END IF;

    -- -------------------------------------------------------------------------
    -- 11. All rows are valid now; enforce NOT NULL.
    -- -------------------------------------------------------------------------

    ALTER TABLE class_section
        MODIFY COLUMN instructor_user_id INT NOT NULL;

    -- -------------------------------------------------------------------------
    -- 12. Remove the legacy ClassSection instructor relation.
    --
    -- user_account.instructor_id is intentionally NOT removed here.
    -- It belongs to the legacy Instructor profile mapping and was also useful
    -- for the data migration above.
    -- -------------------------------------------------------------------------

    IF v_has_legacy_cs_column = 1 THEN
        -- ---------------------------------------------------------------------
        -- Preflight legacy FK/index objects BEFORE any DROP.
        --
        -- MySQL DDL performs implicit commits, so unexpected schema objects
        -- must be detected before changing the legacy relationship.
        -- ---------------------------------------------------------------------

        -- fk_cs_instructor is safe to drop only when it is exactly the
        -- single-column instructor_id -> instructor.id legacy relationship.
        SELECT COUNT(*)
        INTO v_fk_count
        FROM information_schema.key_column_usage
        WHERE table_schema = DATABASE()
          AND table_name = 'class_section'
          AND constraint_name = 'fk_cs_instructor'
          AND referenced_table_name IS NOT NULL;

        SELECT COUNT(*)
        INTO v_fk_valid_count
        FROM information_schema.key_column_usage
        WHERE table_schema = DATABASE()
          AND table_name = 'class_section'
          AND constraint_name = 'fk_cs_instructor'
          AND column_name = 'instructor_id'
          AND referenced_table_name = 'instructor'
          AND referenced_column_name = 'id';

        IF v_fk_count > 0
           AND (
               v_fk_count <> 1
               OR v_fk_valid_count <> 1
           ) THEN

            SELECT
                constraint_name,
                column_name,
                referenced_table_name,
                referenced_column_name
            FROM information_schema.key_column_usage
            WHERE table_schema = DATABASE()
              AND table_name = 'class_section'
              AND constraint_name = 'fk_cs_instructor';

            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT =
                    'Migration aborted: fk_cs_instructor has an unexpected shape.';
        END IF;

        -- No differently named FK may still depend on instructor_id.
        SELECT COUNT(*)
        INTO v_count
        FROM information_schema.key_column_usage
        WHERE table_schema = DATABASE()
          AND table_name = 'class_section'
          AND column_name = 'instructor_id'
          AND referenced_table_name IS NOT NULL
          AND constraint_name <> 'fk_cs_instructor';

        IF v_count > 0 THEN

            SELECT
                constraint_name,
                column_name,
                referenced_table_name,
                referenced_column_name
            FROM information_schema.key_column_usage
            WHERE table_schema = DATABASE()
              AND table_name = 'class_section'
              AND column_name = 'instructor_id'
              AND referenced_table_name IS NOT NULL
              AND constraint_name <> 'fk_cs_instructor';

            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT =
                    'Migration aborted: unexpected foreign key depends on legacy instructor_id.';
        END IF;

        -- idx_cs_instructor is safe to drop only when it is exactly the
        -- expected one-column legacy instructor_id index.
        SELECT COUNT(*)
        INTO v_index_count
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'class_section'
          AND index_name = 'idx_cs_instructor';

        SELECT COUNT(*)
        INTO v_count
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'class_section'
          AND index_name = 'idx_cs_instructor'
          AND column_name = 'instructor_id'
          AND seq_in_index = 1;

        IF v_index_count > 0
           AND (
               v_index_count <> 1
               OR v_count <> 1
           ) THEN

            SELECT
                index_name,
                column_name,
                seq_in_index,
                non_unique
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'class_section'
              AND index_name = 'idx_cs_instructor'
            ORDER BY seq_in_index;

            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT =
                    'Migration aborted: idx_cs_instructor has an unexpected shape.';
        END IF;

        -- No differently named index may still depend on instructor_id.
        SELECT COUNT(DISTINCT index_name)
        INTO v_count
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'class_section'
          AND column_name = 'instructor_id'
          AND index_name <> 'idx_cs_instructor';

        IF v_count > 0 THEN

            SELECT DISTINCT
                index_name,
                column_name,
                seq_in_index,
                non_unique
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'class_section'
              AND column_name = 'instructor_id'
              AND index_name <> 'idx_cs_instructor'
            ORDER BY index_name, seq_in_index;

            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT =
                    'Migration aborted: unexpected index depends on legacy instructor_id.';
        END IF;

                -- Drop fk_cs_instructor only when it is actually the legacy
        -- instructor_id -> instructor.id relationship.
        --
        -- Do not rely on the constraint name alone because a partially
        -- repaired database could reuse a familiar constraint name for a
        -- different column.
        SELECT COUNT(*)
        INTO v_count
        FROM information_schema.key_column_usage
        WHERE table_schema = DATABASE()
          AND table_name = 'class_section'
          AND constraint_name = 'fk_cs_instructor'
          AND column_name = 'instructor_id'
          AND referenced_table_name = 'instructor'
          AND referenced_column_name = 'id';

        IF v_count > 0 THEN
            ALTER TABLE class_section
                DROP FOREIGN KEY fk_cs_instructor;
        END IF;

        -- Stop rather than silently dropping an unexpected additional FK.
        SELECT COUNT(*)
        INTO v_count
        FROM information_schema.key_column_usage
        WHERE table_schema = DATABASE()
          AND table_name = 'class_section'
          AND column_name = 'instructor_id'
          AND referenced_table_name IS NOT NULL;

        IF v_count > 0 THEN

            SELECT
                constraint_name,
                referenced_table_name,
                referenced_column_name
            FROM information_schema.key_column_usage
            WHERE table_schema = DATABASE()
              AND table_name = 'class_section'
              AND column_name = 'instructor_id'
              AND referenced_table_name IS NOT NULL;

            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT =
                    'Migration aborted: unexpected legacy instructor_id foreign key remains.';
        END IF;

        -- Remove the known legacy index if it still exists.
        SELECT COUNT(*)
        INTO v_count
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'class_section'
          AND index_name = 'idx_cs_instructor';

        IF v_count > 0 THEN
            ALTER TABLE class_section
                DROP INDEX idx_cs_instructor;
        END IF;

        -- Verify no unknown index still depends on instructor_id.
        SELECT COUNT(DISTINCT index_name)
        INTO v_count
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'class_section'
          AND column_name = 'instructor_id';

        IF v_count > 0 THEN

            SELECT DISTINCT
                index_name,
                non_unique
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'class_section'
              AND column_name = 'instructor_id';

            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT =
                    'Migration aborted: unexpected index still depends on legacy instructor_id.';
        END IF;

        ALTER TABLE class_section
            DROP COLUMN instructor_id;

    END IF;

END$$

DELIMITER ;

CALL migrate_class_section_instructor_user();

DROP PROCEDURE IF EXISTS migrate_class_section_instructor_user;
