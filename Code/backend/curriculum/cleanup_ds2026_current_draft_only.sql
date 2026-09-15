SET NAMES utf8mb4;
USE curriculum_iu;

DROP PROCEDURE IF EXISTS cleanup_ds2026_current_test_drafts;
DELIMITER $$

CREATE PROCEDURE cleanup_ds2026_current_test_drafts()
BEGIN
    DECLARE v_cohort_id INT DEFAULT NULL;
    DECLARE v_target_count INT DEFAULT 0;
    DECLARE v_approval_count INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SELECT id INTO v_cohort_id
    FROM cohort
    WHERE UPPER(REPLACE(TRIM(name), ' ', '')) = 'DS2026'
    LIMIT 1;

    IF v_cohort_id IS NULL OR v_cohort_id <> 14 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cleanup stopped: expected DS2026 cohort id 14.';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS tmp_ds2026_draft_syllabus;
    CREATE TEMPORARY TABLE tmp_ds2026_draft_syllabus (
        syllabus_id INT PRIMARY KEY
    ) ENGINE=InnoDB;

    INSERT INTO tmp_ds2026_draft_syllabus (syllabus_id)
    SELECT DISTINCT s.id
    FROM syllabus s
    JOIN course_program cp
      ON cp.syllabus_id = s.id
     AND cp.cohort_id = v_cohort_id
    WHERE s.status = 'DRAFT';

    SELECT COUNT(*) INTO v_target_count
    FROM tmp_ds2026_draft_syllabus;

    -- Latest verified UI state before this script: 35 saved DS2026 drafts.
    IF v_target_count <> 35 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cleanup stopped: expected exactly 35 linked DS2026 DRAFT syllabuses.';
    END IF;

    SELECT COUNT(*) INTO v_approval_count
    FROM approval_request ar
    JOIN tmp_ds2026_draft_syllabus t
      ON t.syllabus_id = ar.syllabus_id;

    IF v_approval_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cleanup stopped: DS2026 DRAFT data has approval_request records.';
    END IF;

    START TRANSACTION;

    UPDATE class_section cs
    JOIN tmp_ds2026_draft_syllabus t
      ON t.syllabus_id = cs.syllabus_id
    SET cs.syllabus_id = NULL;

    DELETE h
    FROM syllabus_import_history h
    JOIN tmp_ds2026_draft_syllabus t
      ON t.syllabus_id = h.syllabus_id;

    DELETE ss
    FROM syllabus_source_snapshot ss
    JOIN tmp_ds2026_draft_syllabus t
      ON t.syllabus_id = ss.syllabus_id;

    UPDATE course_program cp
    JOIN tmp_ds2026_draft_syllabus t
      ON t.syllabus_id = cp.syllabus_id
    SET cp.syllabus_id = NULL
    WHERE cp.cohort_id = v_cohort_id;

    DELETE s
    FROM syllabus s
    JOIN tmp_ds2026_draft_syllabus t
      ON t.syllabus_id = s.id;

    COMMIT;

    SELECT
        v_target_count AS deleted_ds2026_draft_count,
        (SELECT COUNT(*) FROM course_program
         WHERE cohort_id=v_cohort_id AND syllabus_id IS NOT NULL)
             AS remaining_ds2026_links;
END$$
DELIMITER ;

CALL cleanup_ds2026_current_test_drafts();
DROP PROCEDURE IF EXISTS cleanup_ds2026_current_test_drafts;
