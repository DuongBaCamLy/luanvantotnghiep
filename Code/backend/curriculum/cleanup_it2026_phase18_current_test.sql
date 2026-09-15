SET NAMES utf8mb4;
USE curriculum_iu;

DROP PROCEDURE IF EXISTS cleanup_it2026_phase18_current_test;
DELIMITER $$

CREATE PROCEDURE cleanup_it2026_phase18_current_test()
BEGIN
    DECLARE v_target_count INT DEFAULT 0;
    DECLARE v_non_draft_count INT DEFAULT 0;
    DECLARE v_approval_count INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    DROP TEMPORARY TABLE IF EXISTS tmp_it2026_test_syllabus;
    CREATE TEMPORARY TABLE tmp_it2026_test_syllabus (
        syllabus_id INT PRIMARY KEY
    ) ENGINE=InnoDB;

    /*
     * Admin program-document imports store cohort.name as academic_year.
     * Using academic_year='IT2026' also captures an orphan duplicate syllabus
     * that may no longer be reachable through course_program after a repeated
     * course code overwrote the mapping.
     */
    INSERT INTO tmp_it2026_test_syllabus (syllabus_id)
    SELECT s.id
    FROM syllabus s
    WHERE UPPER(REPLACE(TRIM(s.academic_year), ' ', '')) = 'IT2026'
      AND s.status = 'DRAFT';

    SELECT COUNT(*) INTO v_target_count
    FROM tmp_it2026_test_syllabus;

    IF v_target_count <> 65 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cleanup stopped: expected exactly 65 current IT2026 DRAFT test syllabus rows.';
    END IF;

    SELECT COUNT(*) INTO v_non_draft_count
    FROM syllabus s
    JOIN tmp_it2026_test_syllabus t ON t.syllabus_id = s.id
    WHERE s.status <> 'DRAFT';

    IF v_non_draft_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cleanup stopped: a target syllabus is no longer DRAFT.';
    END IF;

    SELECT COUNT(*) INTO v_approval_count
    FROM approval_request ar
    JOIN tmp_it2026_test_syllabus t ON t.syllabus_id = ar.syllabus_id;

    IF v_approval_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cleanup stopped: a target syllabus has approval workflow records.';
    END IF;

    START TRANSACTION;

    UPDATE class_section cs
    JOIN tmp_it2026_test_syllabus t ON t.syllabus_id = cs.syllabus_id
    SET cs.syllabus_id = NULL;

    DELETE h
    FROM syllabus_import_history h
    JOIN tmp_it2026_test_syllabus t ON t.syllabus_id = h.syllabus_id;

    DELETE ss
    FROM syllabus_source_snapshot ss
    JOIN tmp_it2026_test_syllabus t ON t.syllabus_id = ss.syllabus_id;

    UPDATE course_program cp
    JOIN tmp_it2026_test_syllabus t ON t.syllabus_id = cp.syllabus_id
    SET cp.syllabus_id = NULL
    WHERE cp.cohort_id = 13;

    DELETE s
    FROM syllabus s
    JOIN tmp_it2026_test_syllabus t ON t.syllabus_id = s.id;

    COMMIT;

    SELECT
        v_target_count AS deleted_it2026_test_syllabus_rows,
        (SELECT COUNT(*) FROM syllabus
         WHERE UPPER(REPLACE(TRIM(academic_year), ' ', ''))='IT2026'
           AND status='DRAFT') AS remaining_it2026_drafts,
        (SELECT COUNT(*) FROM course_program
         WHERE cohort_id=13 AND syllabus_id IS NOT NULL) AS remaining_it2026_links;

    SELECT
        c.id AS cohort_id,
        c.name AS cohort,
        COUNT(DISTINCT s.id) AS linked_syllabus_count
    FROM cohort c
    LEFT JOIN course_program cp ON cp.cohort_id=c.id
    LEFT JOIN syllabus s ON s.id=cp.syllabus_id
    WHERE c.id IN (1,12,13)
    GROUP BY c.id,c.name
    ORDER BY c.id;
END$$
DELIMITER ;

CALL cleanup_it2026_phase18_current_test();
DROP PROCEDURE IF EXISTS cleanup_it2026_phase18_current_test;
