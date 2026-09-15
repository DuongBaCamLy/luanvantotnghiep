SET NAMES utf8mb4;
USE curriculum_iu;

-- ============================================================================
-- Phase 1.7 - IT2026 CourseProgram reconciliation
--
-- INSERT ONLY:
--   * No Course rows are created or modified.
--   * No existing CourseProgram rows are updated/deleted.
--   * Only official IT2026 curriculum rows currently missing from the selected
--     cohort are added.
--
-- The following appendix-only / legacy syllabus codes are intentionally NOT
-- mapped because they are not listed in the official IT2026 curriculum tables:
--   IT165IU  Security Technology and Implementation
--   IT155IU  legacy duplicate of canonical IT163IU Optimization and Applications
--   IT173IU  Big Data Analytics
-- ============================================================================

DROP PROCEDURE IF EXISTS reconcile_it2026_course_program;
DELIMITER $$

CREATE PROCEDURE reconcile_it2026_course_program()
BEGIN
    DECLARE v_program_id INT DEFAULT NULL;
    DECLARE v_cohort_id INT DEFAULT NULL;
    DECLARE v_cohort_count INT DEFAULT 0;

    DECLARE v_general_type_id INT DEFAULT NULL;
    DECLARE v_compulsory_type_id INT DEFAULT NULL;
    DECLARE v_elective_type_id INT DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SELECT COUNT(*), MIN(id)
      INTO v_cohort_count, v_cohort_id
    FROM cohort
    WHERE UPPER(REPLACE(TRIM(name), ' ', '')) = 'IT2026';

    IF v_cohort_count <> 1 OR v_cohort_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Phase 1.7 stopped: expected exactly one IT2026 cohort.';
    END IF;

    SELECT program_id
      INTO v_program_id
    FROM cohort
    WHERE id = v_cohort_id
    LIMIT 1;

    IF v_program_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Phase 1.7 stopped: IT2026 cohort has no Program.';
    END IF;

    SET v_general_type_id = (
        SELECT id FROM course_type WHERE UPPER(TRIM(code)) = 'GENERAL' LIMIT 1
    );
    SET v_compulsory_type_id = (
        SELECT id FROM course_type WHERE UPPER(TRIM(code)) = 'COMPULSORY' LIMIT 1
    );
    SET v_elective_type_id = (
        SELECT id FROM course_type WHERE UPPER(TRIM(code)) = 'ELECTIVE' LIMIT 1
    );

    IF v_general_type_id IS NULL
       OR v_compulsory_type_id IS NULL
       OR v_elective_type_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Phase 1.7 stopped: GENERAL/COMPULSORY/ELECTIVE course types are required.';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS tmp_it2026_reconcile;
    CREATE TEMPORARY TABLE tmp_it2026_reconcile (
        course_code       VARCHAR(50) NOT NULL PRIMARY KEY,
        base_code         VARCHAR(50) NOT NULL,
        course_type_code  VARCHAR(20) NOT NULL,
        semester_suggest  INT NULL,
        year_suggest      INT NULL,
        term_code         VARCHAR(20) NULL,
        is_required       BOOLEAN NOT NULL
    ) ENGINE=InnoDB
      DEFAULT CHARACTER SET utf8mb4
      COLLATE utf8mb4_unicode_ci;

    INSERT INTO tmp_it2026_reconcile
        (course_code, base_code, course_type_code,
         semester_suggest, year_suggest, term_code, is_required)
    VALUES
        ('MA036IU', 'MA036', 'GENERAL',     4, 2, 'HK4', TRUE),
        ('IT159IU', 'IT159', 'COMPULSORY',  7, 4, 'HK7', TRUE),

        ('IT163IU', 'IT163', 'ELECTIVE', NULL, NULL, 'ELECTIVE', FALSE),
        ('IT024IU', 'IT024', 'ELECTIVE', NULL, NULL, 'ELECTIVE', FALSE),
        ('IT090IU', 'IT090', 'ELECTIVE', NULL, NULL, 'ELECTIVE', FALSE),
        ('IT092IU', 'IT092', 'ELECTIVE', NULL, NULL, 'ELECTIVE', FALSE),
        ('IT114IU', 'IT114', 'ELECTIVE', NULL, NULL, 'ELECTIVE', FALSE),
        ('IT138IU', 'IT138', 'ELECTIVE', NULL, NULL, 'ELECTIVE', FALSE),
        ('IT144IU', 'IT144', 'ELECTIVE', NULL, NULL, 'ELECTIVE', FALSE),
        ('IT145IU', 'IT145', 'ELECTIVE', NULL, NULL, 'ELECTIVE', FALSE),
        ('IT164IU', 'IT164', 'ELECTIVE', NULL, NULL, 'ELECTIVE', FALSE),
        ('IT150IU', 'IT150', 'ELECTIVE', NULL, NULL, 'ELECTIVE', FALSE),
        ('IT157IU', 'IT157', 'ELECTIVE', NULL, NULL, 'ELECTIVE', FALSE),
        ('IT158IU', 'IT158', 'ELECTIVE', NULL, NULL, 'ELECTIVE', FALSE),
        ('IT166IU', 'IT166', 'ELECTIVE', NULL, NULL, 'ELECTIVE', FALSE),
        ('IT167IU', 'IT167', 'ELECTIVE', NULL, NULL, 'ELECTIVE', FALSE);

    -- Every official missing mapping must already have exactly one Catalog Course.
    IF EXISTS (
        SELECT 1
        FROM tmp_it2026_reconcile t
        LEFT JOIN course c
          ON (
              UPPER(REPLACE(REPLACE(TRIM(c.course_code), '-', ''), ' ', ''))
                  COLLATE utf8mb4_unicode_ci
                  = t.course_code COLLATE utf8mb4_unicode_ci
              OR
              UPPER(REPLACE(REPLACE(TRIM(c.course_code), '-', ''), ' ', ''))
                  COLLATE utf8mb4_unicode_ci
                  = t.base_code COLLATE utf8mb4_unicode_ci
          )
        GROUP BY t.course_code
        HAVING COUNT(DISTINCT c.id) <> 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Phase 1.7 stopped: one or more official IT2026 courses are missing/ambiguous in Course Catalog.';
    END IF;

    START TRANSACTION;

    INSERT INTO course_program
        (course_id, program_id, cohort_id, course_type_id, syllabus_id,
         semester_suggest, year_suggest, term_code, is_required)
    SELECT
        c.id,
        v_program_id,
        v_cohort_id,
        CASE t.course_type_code
            WHEN 'GENERAL' THEN v_general_type_id
            WHEN 'ELECTIVE' THEN v_elective_type_id
            ELSE v_compulsory_type_id
        END,
        NULL,
        t.semester_suggest,
        t.year_suggest,
        t.term_code,
        t.is_required
    FROM tmp_it2026_reconcile t
    JOIN course c
      ON (
          UPPER(REPLACE(REPLACE(TRIM(c.course_code), '-', ''), ' ', ''))
              COLLATE utf8mb4_unicode_ci
              = t.course_code COLLATE utf8mb4_unicode_ci
          OR
          UPPER(REPLACE(REPLACE(TRIM(c.course_code), '-', ''), ' ', ''))
              COLLATE utf8mb4_unicode_ci
              = t.base_code COLLATE utf8mb4_unicode_ci
      )
    WHERE NOT EXISTS (
        SELECT 1
        FROM course_program cp
        WHERE cp.course_id = c.id
          AND cp.program_id = v_program_id
          AND cp.cohort_id = v_cohort_id
    );

    COMMIT;

    SELECT
        t.course_code AS expected_code,
        c.course_code AS catalog_code,
        cp.id AS course_program_id,
        ct.code AS course_type,
        cp.semester_suggest,
        cp.year_suggest,
        cp.term_code,
        cp.is_required
    FROM tmp_it2026_reconcile t
    JOIN course c
      ON (
          UPPER(REPLACE(REPLACE(TRIM(c.course_code), '-', ''), ' ', ''))
              COLLATE utf8mb4_unicode_ci
              = t.course_code COLLATE utf8mb4_unicode_ci
          OR
          UPPER(REPLACE(REPLACE(TRIM(c.course_code), '-', ''), ' ', ''))
              COLLATE utf8mb4_unicode_ci
              = t.base_code COLLATE utf8mb4_unicode_ci
      )
    JOIN course_program cp
      ON cp.course_id = c.id
     AND cp.program_id = v_program_id
     AND cp.cohort_id = v_cohort_id
    JOIN course_type ct ON ct.id = cp.course_type_id
    ORDER BY t.course_code;
END$$
DELIMITER ;

CALL reconcile_it2026_course_program();
DROP PROCEDURE IF EXISTS reconcile_it2026_course_program;
