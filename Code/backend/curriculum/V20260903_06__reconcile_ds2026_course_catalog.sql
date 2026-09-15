SET NAMES utf8mb4;
USE curriculum_iu;

-- ============================================================================
-- Phase 2.0 - SAFE DS2026 Course Catalog + CourseProgram reconciliation
--
-- Source of truth:
--   "Hồ sơ CTĐT Khóa 2026 - Khoa học dữ liệu - 14.08.2026 Full.pdf"
--
-- Guarantees:
--   * INSERT only for Course and CourseProgram.
--   * No UPDATE / DELETE of existing Course, CourseProgram, Syllabus, Program,
--     Cohort, Department, or CourseType rows.
--   * Existing Course rows are reused by canonical/base code equivalence.
--   * Missing Course rows are created only for official DS2026 curriculum codes.
--   * Missing exact DS2026 CourseProgram rows are created only once.
--   * Stops on catalog ambiguity instead of guessing.
--   * Resolves Program THROUGH cohort DS2026 instead of assuming Program.code.
--   * Uses active taxonomy GENERAL / COMPULSORY / ELECTIVE.
-- ============================================================================

DROP PROCEDURE IF EXISTS reconcile_ds2026_curriculum_catalog;
DELIMITER $$

CREATE PROCEDURE reconcile_ds2026_curriculum_catalog()
BEGIN
    DECLARE v_program_id INT DEFAULT NULL;
    DECLARE v_cohort_id INT DEFAULT NULL;
    DECLARE v_cohort_count INT DEFAULT 0;

    DECLARE v_program_department_id INT DEFAULT NULL;
    DECLARE v_it_department_id INT DEFAULT NULL;
    DECLARE v_math_department_id INT DEFAULT NULL;
    DECLARE v_general_department_id INT DEFAULT NULL;

    DECLARE v_general_type_id INT DEFAULT NULL;
    DECLARE v_compulsory_type_id INT DEFAULT NULL;
    DECLARE v_elective_type_id INT DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    -- 1) Resolve exact DS2026 cohort.
    SELECT COUNT(*), MIN(id)
      INTO v_cohort_count, v_cohort_id
    FROM cohort
    WHERE UPPER(REPLACE(TRIM(name), ' ', '')) = 'DS2026';

    IF v_cohort_count <> 1 OR v_cohort_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Phase 2.0 stopped: expected exactly one DS2026 cohort.';
    END IF;

    IF v_cohort_id <> 14 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Phase 2.0 stopped: current verified DS2026 cohort id should be 14.';
    END IF;

    SET v_program_id = (
        SELECT program_id
        FROM cohort
        WHERE id = v_cohort_id
        LIMIT 1
    );

    IF v_program_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Phase 2.0 stopped: DS2026 cohort has no Program.';
    END IF;

    SET v_program_department_id = (
        SELECT department_id
        FROM program
        WHERE id = v_program_id
        LIMIT 1
    );

    IF v_program_department_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Phase 2.0 stopped: DS2026 Program has no owning Department.';
    END IF;

    -- 2) Active taxonomy only.
    SET v_general_type_id = (
        SELECT id FROM course_type
        WHERE UPPER(TRIM(code)) = 'GENERAL'
        LIMIT 1
    );
    SET v_compulsory_type_id = (
        SELECT id FROM course_type
        WHERE UPPER(TRIM(code)) = 'COMPULSORY'
        LIMIT 1
    );
    SET v_elective_type_id = (
        SELECT id FROM course_type
        WHERE UPPER(TRIM(code)) = 'ELECTIVE'
        LIMIT 1
    );

    IF v_general_type_id IS NULL
       OR v_compulsory_type_id IS NULL
       OR v_elective_type_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Phase 2.0 stopped: GENERAL/COMPULSORY/ELECTIVE CourseTypes are required.';
    END IF;

    -- 3) Resolve Departments from known catalog courses.
    SET v_it_department_id = COALESCE(
        (SELECT department_id FROM course
         WHERE UPPER(TRIM(course_code)) IN ('IT135IU','IT135')
         LIMIT 1),
        (SELECT department_id FROM course
         WHERE UPPER(TRIM(course_code)) IN ('IT140IU','IT140')
         LIMIT 1),
        (SELECT department_id FROM course
         WHERE UPPER(TRIM(course_code)) IN ('IT069IU','IT069')
         LIMIT 1),
        v_program_department_id
    );

    SET v_math_department_id = COALESCE(
        (SELECT department_id FROM course
         WHERE UPPER(TRIM(course_code)) IN ('MA033IU','MA033')
         LIMIT 1),
        (SELECT department_id FROM course
         WHERE UPPER(TRIM(course_code)) IN ('MA001IU','MA001')
         LIMIT 1),
        v_program_department_id
    );

    SET v_general_department_id = COALESCE(
        (SELECT department_id FROM course
         WHERE UPPER(TRIM(course_code)) IN ('PE021IU','PE021')
         LIMIT 1),
        (SELECT department_id FROM course
         WHERE UPPER(TRIM(course_code)) IN ('PE019IU','PE019')
         LIMIT 1),
        v_program_department_id
    );

    -- 4) ONLY the eleven red DS2026 official curriculum courses.
    DROP TEMPORARY TABLE IF EXISTS tmp_ds2026_reconcile;
    CREATE TEMPORARY TABLE tmp_ds2026_reconcile (
        course_code       VARCHAR(50)  NOT NULL PRIMARY KEY,
        base_code         VARCHAR(50)  NOT NULL,
        course_name       VARCHAR(255) NOT NULL,
        course_name_vn    VARCHAR(255) NOT NULL,
        credit_theory     INT          NOT NULL,
        credit_lab        INT          NOT NULL,
        owner_group       VARCHAR(20)  NOT NULL,
        course_type_code  VARCHAR(20)  NOT NULL,
        semester_suggest  INT          NULL,
        year_suggest      INT          NULL,
        term_code         VARCHAR(20)  NULL,
        is_required       BOOLEAN      NOT NULL
    ) ENGINE=InnoDB
      DEFAULT CHARACTER SET utf8mb4
      COLLATE utf8mb4_unicode_ci;

    INSERT INTO tmp_ds2026_reconcile
        (course_code, base_code, course_name, course_name_vn,
         credit_theory, credit_lab, owner_group, course_type_code,
         semester_suggest, year_suggest, term_code, is_required)
    VALUES
        -- General education / compulsory in the DS2026 programme.
        ('PE021IU', 'PE021',
         'General Law', 'Pháp luật đại cương',
         3, 0, 'GENERAL', 'GENERAL',
         2, 1, 'HK2', TRUE),

        ('IT178IU', 'IT178',
         'Probability and Statistics', 'Xác suất và thống kê',
         3, 0, 'MATH', 'GENERAL',
         2, 1, 'HK2', TRUE),

        ('IT176IU', 'IT176',
         'Algorithmic Statistics', 'Thuật toán thống kê',
         3, 0, 'IT', 'GENERAL',
         4, 2, 'HK4', TRUE),

        -- DS specialized compulsory courses.
        ('IT172IU', 'IT172',
         'Machine Learning', 'Học máy',
         3, 1, 'IT', 'COMPULSORY',
         5, 3, 'HK5', TRUE),

        ('IT173IU', 'IT173',
         'Big Data Analytics', 'Phân tích dữ liệu lớn',
         3, 1, 'IT', 'COMPULSORY',
         6, 3, 'HK6', TRUE),

        -- DS elective pool. No fixed semester is imposed by the official
        -- curriculum; therefore semester/year remain NULL.
        ('IT169IU', 'IT169',
         'Time Series Analysis', 'Phân tích chuỗi thời gian',
         3, 1, 'IT', 'ELECTIVE',
         NULL, NULL, 'ELECTIVE', FALSE),

        ('IT076IU', 'IT076',
         'Software Engineering', 'Công nghệ phần mềm',
         3, 1, 'IT', 'ELECTIVE',
         NULL, NULL, 'ELECTIVE', FALSE),

        ('IT170IU', 'IT170',
         'Natural Language Processing', 'Xử lý ngôn ngữ tự nhiên',
         3, 1, 'IT', 'ELECTIVE',
         NULL, NULL, 'ELECTIVE', FALSE),

        ('IT093IU', 'IT093',
         'Web Application Development', 'Phát triển ứng dụng web',
         3, 1, 'IT', 'ELECTIVE',
         NULL, NULL, 'ELECTIVE', FALSE),

        ('IT164IU', 'IT164',
         'Cloud Computing', 'Điện toán đám mây',
         3, 1, 'IT', 'ELECTIVE',
         NULL, NULL, 'ELECTIVE', FALSE),

        ('IT153IU', 'IT153',
         'Discrete Mathematics', 'Toán rời rạc',
         3, 0, 'IT', 'ELECTIVE',
         NULL, NULL, 'ELECTIVE', FALSE);

    -- 5) Existing canonical/base duplicates are an ambiguity: abort.
    IF EXISTS (
        SELECT 1
        FROM tmp_ds2026_reconcile t
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
        GROUP BY t.course_code
        HAVING COUNT(DISTINCT c.id) > 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Phase 2.0 stopped: duplicate canonical/base course codes exist. No data was changed.';
    END IF;

    START TRANSACTION;

    -- 6) Create only genuinely missing Course Catalog rows.
    INSERT INTO course
        (course_code, name, name_vn, department_id,
         credit_theory, credit_lab, course_level,
         description, is_active, created_at)
    SELECT
        t.course_code,
        t.course_name,
        t.course_name_vn,
        CASE t.owner_group
            WHEN 'MATH' THEN v_math_department_id
            WHEN 'GENERAL' THEN v_general_department_id
            ELSE v_it_department_id
        END,
        t.credit_theory,
        t.credit_lab,
        'UNDERGRADUATE',
        NULL,
        TRUE,
        NOW()
    FROM tmp_ds2026_reconcile t
    WHERE NOT EXISTS (
        SELECT 1
        FROM course c
        WHERE (
            UPPER(REPLACE(REPLACE(TRIM(c.course_code), '-', ''), ' ', ''))
                COLLATE utf8mb4_unicode_ci
                = t.course_code COLLATE utf8mb4_unicode_ci
            OR
            UPPER(REPLACE(REPLACE(TRIM(c.course_code), '-', ''), ' ', ''))
                COLLATE utf8mb4_unicode_ci
                = t.base_code COLLATE utf8mb4_unicode_ci
        )
    );

    -- 7) Create only missing exact DS2026 CourseProgram rows.
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
    FROM tmp_ds2026_reconcile t
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

    -- 8) Verification result.
    SELECT
        t.course_code AS expected_code,
        c.id AS course_id,
        c.course_code AS catalog_code,
        c.name AS catalog_name,
        c.credit_theory,
        c.credit_lab,
        cp.id AS course_program_id,
        ct.code AS course_type,
        cp.semester_suggest,
        cp.year_suggest,
        cp.term_code,
        cp.is_required
    FROM tmp_ds2026_reconcile t
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
    LEFT JOIN course_program cp
      ON cp.course_id = c.id
     AND cp.program_id = v_program_id
     AND cp.cohort_id = v_cohort_id
    LEFT JOIN course_type ct
      ON ct.id = cp.course_type_id
    ORDER BY t.course_code;

    SELECT COUNT(*) AS reconciled_ds2026_mapping_count
    FROM tmp_ds2026_reconcile t
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
     AND cp.cohort_id = v_cohort_id;

    DROP TEMPORARY TABLE IF EXISTS tmp_ds2026_reconcile;
END$$

DELIMITER ;

CALL reconcile_ds2026_curriculum_catalog();
DROP PROCEDURE IF EXISTS reconcile_ds2026_curriculum_catalog;
