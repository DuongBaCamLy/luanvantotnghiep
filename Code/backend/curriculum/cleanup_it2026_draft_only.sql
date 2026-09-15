SET NAMES utf8mb4;
USE curriculum_iu;

DROP PROCEDURE IF EXISTS cleanup_it2026_test_drafts;
DELIMITER $$

CREATE PROCEDURE cleanup_it2026_test_drafts()
BEGIN
    DECLARE v_cohort_id INT DEFAULT NULL;
    DECLARE v_target_count INT DEFAULT 0;
    DECLARE v_cross_scope_count INT DEFAULT 0;
    DECLARE v_approval_count INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SELECT id
      INTO v_cohort_id
    FROM cohort
    WHERE UPPER(REPLACE(TRIM(name), ' ', '')) = 'IT2026'
    LIMIT 1;

    IF v_cohort_id IS NULL OR v_cohort_id <> 13 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cleanup stopped: expected IT2026 cohort id 13.';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS tmp_it2026_draft_syllabus;
    CREATE TEMPORARY TABLE tmp_it2026_draft_syllabus (
        syllabus_id INT PRIMARY KEY
    ) ENGINE=InnoDB;

    INSERT INTO tmp_it2026_draft_syllabus (syllabus_id)
    SELECT DISTINCT s.id
    FROM syllabus s
    JOIN course_program cp
      ON cp.syllabus_id = s.id
     AND cp.cohort_id = v_cohort_id
    WHERE s.status = 'DRAFT';

    SELECT COUNT(*) INTO v_target_count
    FROM tmp_it2026_draft_syllabus;

    -- This cleanup is intentionally locked to the exact test state the user verified.
    IF v_target_count <> 49 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cleanup stopped: expected exactly 49 IT2026 DRAFT syllabuses.';
    END IF;

    -- No target syllabus may be shared with another cohort.
    SELECT COUNT(*)
      INTO v_cross_scope_count
    FROM (
        SELECT cp.syllabus_id
        FROM course_program cp
        JOIN tmp_it2026_draft_syllabus t
          ON t.syllabus_id = cp.syllabus_id
        GROUP BY cp.syllabus_id
        HAVING COUNT(DISTINCT cp.cohort_id) > 1
    ) shared;

    IF v_cross_scope_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cleanup stopped: at least one IT2026 DRAFT syllabus is shared with another cohort.';
    END IF;

    -- DRAFT test syllabuses should not have entered approval workflow.
    SELECT COUNT(*)
      INTO v_approval_count
    FROM approval_request ar
    JOIN tmp_it2026_draft_syllabus t
      ON t.syllabus_id = ar.syllabus_id;

    IF v_approval_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cleanup stopped: one or more IT2026 DRAFT syllabuses have approval_request records.';
    END IF;

    START TRANSACTION;

    -- Preserve ClassSection rows, only detach their test syllabus pointer.
    UPDATE class_section cs
    JOIN tmp_it2026_draft_syllabus t
      ON t.syllabus_id = cs.syllabus_id
    SET cs.syllabus_id = NULL;

    -- Successful test imports have import-history records. These belong to the
    -- test syllabus itself, so remove them as part of the scoped test cleanup.
    DELETE h
    FROM syllabus_import_history h
    JOIN tmp_it2026_draft_syllabus t
      ON t.syllabus_id = h.syllabus_id;

    -- Technical source snapshots are owned by the test syllabus.
    DELETE ss
    FROM syllabus_source_snapshot ss
    JOIN tmp_it2026_draft_syllabus t
      ON t.syllabus_id = ss.syllabus_id;

    -- Preserve all curriculum mappings. Only detach syllabus_id.
    UPDATE course_program cp
    JOIN tmp_it2026_draft_syllabus t
      ON t.syllabus_id = cp.syllabus_id
    SET cp.syllabus_id = NULL
    WHERE cp.cohort_id = v_cohort_id;

    -- assessment_component / clo / syllabus_book / topic are DB CASCADE.
    DELETE s
    FROM syllabus s
    JOIN tmp_it2026_draft_syllabus t
      ON t.syllabus_id = s.id;

    COMMIT;

    SELECT
        v_target_count AS deleted_it2026_draft_count,
        (SELECT COUNT(*) FROM syllabus s
         JOIN course_program cp ON cp.syllabus_id = s.id
         WHERE cp.cohort_id = v_cohort_id) AS remaining_linked_it2026_syllabus,
        (SELECT COUNT(*) FROM course_program cp
         WHERE cp.cohort_id = v_cohort_id
           AND cp.syllabus_id IS NOT NULL) AS remaining_linked_it2026_course_program;

    SELECT
        c.id AS cohort_id,
        c.name AS cohort,
        COUNT(DISTINCT s.id) AS syllabus_count
    FROM cohort c
    LEFT JOIN course_program cp ON cp.cohort_id = c.id
    LEFT JOIN syllabus s ON s.id = cp.syllabus_id
    WHERE c.id IN (1, 12, 13)
    GROUP BY c.id, c.name
    ORDER BY c.id;
END$$
DELIMITER ;

CALL cleanup_it2026_test_drafts();
DROP PROCEDURE IF EXISTS cleanup_it2026_test_drafts;
