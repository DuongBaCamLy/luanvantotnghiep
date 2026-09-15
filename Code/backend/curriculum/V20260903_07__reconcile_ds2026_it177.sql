SET NAMES utf8mb4;
USE curriculum_iu;

DROP PROCEDURE IF EXISTS reconcile_ds2026_it177;
DELIMITER $$

CREATE PROCEDURE reconcile_ds2026_it177()
BEGIN
    DECLARE v_cohort_id INT DEFAULT NULL;
    DECLARE v_program_id INT DEFAULT NULL;
    DECLARE v_course_id INT DEFAULT NULL;
    DECLARE v_course_count INT DEFAULT 0;
    DECLARE v_type_id INT DEFAULT NULL;
    DECLARE v_exact_count INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SELECT id, program_id
      INTO v_cohort_id, v_program_id
    FROM cohort
    WHERE UPPER(REPLACE(TRIM(name), ' ', '')) = 'DS2026'
    LIMIT 1;

    IF v_cohort_id IS NULL OR v_program_id IS NULL OR v_cohort_id <> 14 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Phase 2.1 stopped: expected DS2026 cohort id 14.';
    END IF;

    SELECT COUNT(*), MIN(id)
      INTO v_course_count, v_course_id
    FROM course
    WHERE UPPER(REPLACE(REPLACE(TRIM(course_code), '-', ''), ' ', ''))
          COLLATE utf8mb4_unicode_ci
          IN ('IT177IU','IT177');

    IF v_course_count <> 1 OR v_course_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Phase 2.1 stopped: expected exactly one IT177/IT177IU Course row.';
    END IF;

    SELECT id
      INTO v_type_id
    FROM course_type
    WHERE UPPER(TRIM(code)) = 'COMPULSORY'
    LIMIT 1;

    IF v_type_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Phase 2.1 stopped: COMPULSORY CourseType not found.';
    END IF;

    SELECT COUNT(*)
      INTO v_exact_count
    FROM course_program
    WHERE course_id = v_course_id
      AND program_id = v_program_id
      AND cohort_id = v_cohort_id;

    IF v_exact_count = 0 THEN
        START TRANSACTION;

        INSERT INTO course_program
            (course_id, program_id, cohort_id, course_type_id, syllabus_id,
             semester_suggest, year_suggest, term_code, is_required)
        VALUES
            (v_course_id, v_program_id, v_cohort_id, v_type_id, NULL,
             7, 4, 'HK7', TRUE);

        COMMIT;
    ELSEIF v_exact_count > 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Phase 2.1 stopped: duplicate IT177IU DS2026 CourseProgram rows already exist.';
    END IF;

    SELECT
        cp.id AS course_program_id,
        c.course_code,
        c.name,
        co.name AS cohort,
        p.code AS program_code,
        ct.code AS course_type,
        cp.semester_suggest,
        cp.year_suggest,
        cp.term_code,
        cp.is_required,
        cp.syllabus_id
    FROM course_program cp
    JOIN course c ON c.id = cp.course_id
    JOIN cohort co ON co.id = cp.cohort_id
    JOIN program p ON p.id = cp.program_id
    LEFT JOIN course_type ct ON ct.id = cp.course_type_id
    WHERE cp.course_id = v_course_id
      AND cp.program_id = v_program_id
      AND cp.cohort_id = v_cohort_id;
END$$
DELIMITER ;

CALL reconcile_ds2026_it177();
DROP PROCEDURE IF EXISTS reconcile_ds2026_it177;
