SET NAMES utf8mb4;
USE curriculum_iu;

SET @cohort_2026_id = (
    SELECT id
    FROM cohort
    WHERE UPPER(REPLACE(TRIM(name), ' ', '')) = 'CS2026'
    ORDER BY id
    LIMIT 1
);
SET @program_id = (
    SELECT program_id FROM cohort WHERE id = @cohort_2026_id LIMIT 1
);
SET @cohort_2021_id = (
    SELECT id
    FROM cohort
    WHERE program_id = @program_id
      AND UPPER(REPLACE(TRIM(name), ' ', '')) = 'CS2021'
    ORDER BY id
    LIMIT 1
);

-- A) Reference context. Expect one CS2026 row and a non-null program id.
SELECT @program_id AS program_id,
       @cohort_2021_id AS cohort_2021_id,
       @cohort_2026_id AS cohort_2026_id;

-- B) CS2021 chemistry alias. Expected: canonical CH011IU/CH011 only;
-- Phase 1.3B must NOT create CHE011IU.
SELECT c.id, c.course_code, c.name, c.name_vn,
       c.credit_theory, c.credit_lab
FROM course c
WHERE UPPER(REPLACE(REPLACE(TRIM(c.course_code), '-', ''), ' ', ''))
      IN ('CH011IU','CH011','CHE011IU','CHE011')
ORDER BY c.id;

-- C) All 12 official CS2026 mappings needed by import.
SELECT
    c.course_code,
    c.name,
    c.credit_theory,
    c.credit_lab,
    ct.code AS course_type,
    cp.semester_suggest,
    cp.year_suggest,
    cp.term_code,
    cp.is_required
FROM course_program cp
JOIN course c ON c.id = cp.course_id
LEFT JOIN course_type ct ON ct.id = cp.course_type_id
WHERE cp.program_id = @program_id
  AND cp.cohort_id = @cohort_2026_id
  AND UPPER(REPLACE(REPLACE(TRIM(c.course_code), '-', ''), ' ', '')) IN (
      'PE021IU','PE021','MA033IU','MA033','MA036IU','MA036',
      'IT175IU','IT175','IT164IU','IT164','IT165IU','IT165',
      'IT166IU','IT166','IT167IU','IT167','IT150IU','IT150',
      'IT156IU','IT156','IT138IU','IT138','IT177IU','IT177'
  )
ORDER BY c.course_code;

-- Expected: 12 exact CS2026 mappings.
SELECT COUNT(*) AS reconciled_cs2026_mapping_count
FROM course_program cp
JOIN course c ON c.id = cp.course_id
WHERE cp.program_id = @program_id
  AND cp.cohort_id = @cohort_2026_id
  AND UPPER(REPLACE(REPLACE(TRIM(c.course_code), '-', ''), ' ', '')) IN (
      'PE021IU','PE021','MA033IU','MA033','MA036IU','MA036',
      'IT175IU','IT175','IT164IU','IT164','IT165IU','IT165',
      'IT166IU','IT166','IT167IU','IT167','IT150IU','IT150',
      'IT156IU','IT156','IT138IU','IT138','IT177IU','IT177'
  );

-- D) Taxonomy sanity. Expected active SRS groups: GENERAL / COMPULSORY / ELECTIVE.
SELECT id, code, name, name_vn
FROM course_type
ORDER BY id;

-- E) Dangerous canonical/base duplicates among the 12 reconciled codes.
-- Expected: zero rows.
SELECT base_code, COUNT(*) AS duplicate_count,
       GROUP_CONCAT(CONCAT(id, ':', course_code) ORDER BY id SEPARATOR ', ') AS rows_found
FROM (
    SELECT id,
           course_code,
           CASE
             WHEN RIGHT(UPPER(REPLACE(REPLACE(TRIM(course_code), '-', ''), ' ', '')), 2) = 'IU'
             THEN LEFT(
                    UPPER(REPLACE(REPLACE(TRIM(course_code), '-', ''), ' ', '')),
                    LENGTH(UPPER(REPLACE(REPLACE(TRIM(course_code), '-', ''), ' ', ''))) - 2
                  )
             ELSE UPPER(REPLACE(REPLACE(TRIM(course_code), '-', ''), ' ', ''))
           END AS base_code
    FROM course
) q
WHERE base_code IN ('PE021','MA033','MA036','IT175','IT164','IT165','IT166','IT167','IT150','IT156','IT138','IT177')
GROUP BY base_code
HAVING COUNT(*) > 1
ORDER BY base_code;


-- F) SRS taxonomy exactness.
-- Expected: active_count = 3, legacy_count = 0.
SELECT
    SUM(UPPER(TRIM(code)) IN ('GENERAL','COMPULSORY','ELECTIVE')) AS active_count,
    SUM(UPPER(TRIM(code)) IN ('FOUNDATION','CORE','THESIS','INTERNSHIP')) AS legacy_count
FROM course_type;
