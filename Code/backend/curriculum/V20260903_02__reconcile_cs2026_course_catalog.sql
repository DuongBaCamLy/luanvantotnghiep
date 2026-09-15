SET NAMES utf8mb4;
USE curriculum_iu;

-- ============================================================================
-- SAFE CS2026 catalog reconciliation (Phase 1.3C)
--
-- Guarantees:
--   * INSERT only. No UPDATE / DELETE of existing Course, CourseProgram,
--     Syllabus, Program, Cohort, or Department rows.
--   * Uses the active SRS course_type taxonomy:
--       GENERAL / COMPULSORY / ELECTIVE
--     (legacy FOUNDATION / CORE / THESIS / INTERNSHIP are migrated separately).
--   * Resolves the program THROUGH cohort CS2026 instead of assuming a program
--     code such as "CS" or "CS-2021".
--   * Stops and rolls back on ambiguity or missing required reference data.
--   * Idempotent: safe to run again after a successful run.
-- ============================================================================

DROP PROCEDURE IF EXISTS reconcile_cs2026_curriculum_catalog;
DELIMITER $$

CREATE PROCEDURE reconcile_cs2026_curriculum_catalog()
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

    -- ------------------------------------------------------------------------
    -- 1) Resolve the exact CS2026 cohort first. Do not assume Program.code.
    -- ------------------------------------------------------------------------
    SELECT COUNT(*), MIN(id)
      INTO v_cohort_count, v_cohort_id
    FROM cohort
    WHERE UPPER(REPLACE(TRIM(name), ' ', '')) = 'CS2026';

    IF v_cohort_count <> 1 OR v_cohort_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Phase 1.3C stopped: expected exactly one CS2026 cohort.';
    END IF;

    SET v_program_id = (
        SELECT c.program_id
        FROM cohort c
        WHERE c.id = v_cohort_id
        LIMIT 1
    );

    IF v_program_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Phase 1.3C stopped: CS2026 cohort has no program.';
    END IF;

    SET v_program_department_id = (
        SELECT p.department_id
        FROM program p
        WHERE p.id = v_program_id
        LIMIT 1
    );

    IF v_program_department_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Phase 1.3C stopped: CS2026 program has no owning department.';
    END IF;

    -- ------------------------------------------------------------------------
    -- 2) Require the active SRS taxonomy. Do not recreate legacy types.
    -- ------------------------------------------------------------------------
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
            SET MESSAGE_TEXT = 'Phase 1.3C stopped: GENERAL/COMPULSORY/ELECTIVE course types are required. Run migrate_course_type_to_srs_3_groups.sql first if needed.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM course_type
        WHERE UPPER(TRIM(code)) IN ('FOUNDATION','CORE','THESIS','INTERNSHIP')
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Phase 1.3C stopped: legacy course types still exist. Run migrate_course_type_to_srs_3_groups.sql before reconciliation.';
    END IF;

    -- ------------------------------------------------------------------------
    -- 3) Resolve departments from already-known catalog courses.
    -- ------------------------------------------------------------------------
    SET v_it_department_id = COALESCE(
        (SELECT department_id FROM course WHERE UPPER(TRIM(course_code)) IN ('IT064IU','IT064') LIMIT 1),
        (SELECT department_id FROM course WHERE UPPER(TRIM(course_code)) IN ('IT069IU','IT069') LIMIT 1),
        v_program_department_id
    );

    SET v_math_department_id = COALESCE(
        (SELECT department_id FROM course WHERE UPPER(TRIM(course_code)) IN ('MA001IU','MA001') LIMIT 1),
        (SELECT department_id FROM course WHERE UPPER(TRIM(course_code)) IN ('MA003IU','MA003') LIMIT 1),
        v_program_department_id
    );

    SET v_general_department_id = COALESCE(
        (SELECT department_id FROM course WHERE UPPER(TRIM(course_code)) IN ('PE019IU','PE019') LIMIT 1),
        (SELECT department_id FROM course WHERE UPPER(TRIM(course_code)) IN ('PE018IU','PE018') LIMIT 1),
        v_program_department_id
    );

    -- ------------------------------------------------------------------------
    -- 4) Official CS2026 rows that were missing/incomplete in current Catalog.
    --    Credits/placement come from the PROGRAM CURRICULUM TABLE, not stale
    --    individual syllabus metadata.
    -- ------------------------------------------------------------------------
    DROP TEMPORARY TABLE IF EXISTS tmp_cs2026_catalog_reconcile;
    CREATE TEMPORARY TABLE tmp_cs2026_catalog_reconcile (
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

    INSERT INTO tmp_cs2026_catalog_reconcile
        (course_code, base_code, course_name, course_name_vn,
         credit_theory, credit_lab, owner_group, course_type_code,
         semester_suggest, year_suggest, term_code, is_required)
    VALUES
        ('PE021IU', 'PE021', 'General Law', 'Pháp luật đại cương', 3, 0,
         'GENERAL', 'GENERAL', 2, 1, 'HK2', TRUE),
        ('MA033IU', 'MA033', 'Linear Algebra', 'Đại số tuyến tính', 3, 0,
         'MATH', 'GENERAL', 2, 1, 'HK2', TRUE),
        ('MA036IU', 'MA036', 'Probability and Statistics', 'Xác suất và thống kê', 3, 0,
         'MATH', 'GENERAL', 5, 3, 'HK5', TRUE),

        ('IT175IU', 'IT175', 'User Experience Design', 'Thiết kế trải nghiệm người dùng', 3, 1,
         'IT', 'ELECTIVE', NULL, NULL, 'ELECTIVE', FALSE),
        ('IT164IU', 'IT164', 'Cloud Computing', 'Điện toán đám mây', 3, 1,
         'IT', 'ELECTIVE', NULL, NULL, 'ELECTIVE', FALSE),
        ('IT165IU', 'IT165', 'Security Technology and Implementation', 'Công nghệ và Triển khai bảo mật', 3, 1,
         'IT', 'ELECTIVE', NULL, NULL, 'ELECTIVE', FALSE),
        ('IT166IU', 'IT166', 'Software Quality Verification and Validation', 'Kiểm tra chất lượng phần mềm', 3, 1,
         'IT', 'ELECTIVE', NULL, NULL, 'ELECTIVE', FALSE),
        ('IT167IU', 'IT167', 'Game Application Development', 'Phát triển ứng dụng game', 3, 1,
         'IT', 'ELECTIVE', NULL, NULL, 'ELECTIVE', FALSE),
        ('IT150IU', 'IT150', 'Blockchain', 'Chuỗi khối', 3, 1,
         'IT', 'ELECTIVE', NULL, NULL, 'ELECTIVE', FALSE),
        ('IT156IU', 'IT156', 'Development & Operation (DevOps)', 'Phát triển và vận hành liên tục', 3, 1,
         'IT', 'ELECTIVE', NULL, NULL, 'ELECTIVE', FALSE),
        ('IT138IU', 'IT138', 'Data Science and Visualization', 'Trực quan hóa dữ liệu', 3, 1,
         'IT', 'ELECTIVE', NULL, NULL, 'ELECTIVE', FALSE),

        ('IT177IU', 'IT177', 'Internship', 'Thực tập công nghiệp', 0, 7,
         'IT', 'COMPULSORY', 7, 4, 'HK7', TRUE);

    -- ------------------------------------------------------------------------
    -- 5) Abort on catalog ambiguity instead of guessing.
    --    Supported existing forms are canonical "XX999IU" or base "XX999".
    -- ------------------------------------------------------------------------
    IF EXISTS (
        SELECT 1
        FROM tmp_cs2026_catalog_reconcile t
        JOIN course c
          ON (UPPER(REPLACE(REPLACE(TRIM(c.course_code), '-', ''), ' ', '')) COLLATE utf8mb4_unicode_ci
              = t.course_code COLLATE utf8mb4_unicode_ci
          OR UPPER(REPLACE(REPLACE(TRIM(c.course_code), '-', ''), ' ', '')) COLLATE utf8mb4_unicode_ci
              = t.base_code COLLATE utf8mb4_unicode_ci)
        GROUP BY t.course_code
        HAVING COUNT(DISTINCT c.id) > 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Phase 1.3C stopped: duplicate canonical/base course codes already exist. No data was changed.';
    END IF;

    START TRANSACTION;

    -- ------------------------------------------------------------------------
    -- 6) Add only genuinely missing Course rows.
    -- ------------------------------------------------------------------------
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
    FROM tmp_cs2026_catalog_reconcile t
    WHERE NOT EXISTS (
        SELECT 1
        FROM course c
        WHERE (UPPER(REPLACE(REPLACE(TRIM(c.course_code), '-', ''), ' ', '')) COLLATE utf8mb4_unicode_ci
               = t.course_code COLLATE utf8mb4_unicode_ci
           OR UPPER(REPLACE(REPLACE(TRIM(c.course_code), '-', ''), ' ', '')) COLLATE utf8mb4_unicode_ci
               = t.base_code COLLATE utf8mb4_unicode_ci)
    );

    -- ------------------------------------------------------------------------
    -- 7) Add only missing exact CS2026 CourseProgram rows.
    --    Existing mappings are never changed.
    -- ------------------------------------------------------------------------
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
    FROM tmp_cs2026_catalog_reconcile t
    JOIN course c
      ON (UPPER(REPLACE(REPLACE(TRIM(c.course_code), '-', ''), ' ', '')) COLLATE utf8mb4_unicode_ci
          = t.course_code COLLATE utf8mb4_unicode_ci
      OR UPPER(REPLACE(REPLACE(TRIM(c.course_code), '-', ''), ' ', '')) COLLATE utf8mb4_unicode_ci
          = t.base_code COLLATE utf8mb4_unicode_ci)
    WHERE NOT EXISTS (
        SELECT 1
        FROM course_program cp
        WHERE cp.course_id = c.id
          AND cp.program_id = v_program_id
          AND cp.cohort_id = v_cohort_id
    );

    COMMIT;

    -- ------------------------------------------------------------------------
    -- 8) Return a compact verification result to Workbench.
    -- ------------------------------------------------------------------------
    SELECT
        t.course_code AS expected_code,
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
    FROM tmp_cs2026_catalog_reconcile t
    LEFT JOIN course c
      ON (UPPER(REPLACE(REPLACE(TRIM(c.course_code), '-', ''), ' ', '')) COLLATE utf8mb4_unicode_ci
          = t.course_code COLLATE utf8mb4_unicode_ci
      OR UPPER(REPLACE(REPLACE(TRIM(c.course_code), '-', ''), ' ', '')) COLLATE utf8mb4_unicode_ci
          = t.base_code COLLATE utf8mb4_unicode_ci)
    LEFT JOIN course_program cp
      ON cp.course_id = c.id
     AND cp.program_id = v_program_id
     AND cp.cohort_id = v_cohort_id
    LEFT JOIN course_type ct ON ct.id = cp.course_type_id
    ORDER BY t.course_code;

    DROP TEMPORARY TABLE IF EXISTS tmp_cs2026_catalog_reconcile;
END$$

DELIMITER ;

CALL reconcile_cs2026_curriculum_catalog();
DROP PROCEDURE IF EXISTS reconcile_cs2026_curriculum_catalog;
