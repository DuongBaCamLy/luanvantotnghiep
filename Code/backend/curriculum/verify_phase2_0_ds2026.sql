SET NAMES utf8mb4;
USE curriculum_iu;

SET @ds2026_cohort_id := (
    SELECT id
    FROM cohort
    WHERE UPPER(REPLACE(TRIM(name), ' ', '')) = 'DS2026'
    LIMIT 1
);

SET @ds2026_program_id := (
    SELECT program_id
    FROM cohort
    WHERE id = @ds2026_cohort_id
);

SELECT
    @ds2026_program_id AS program_id,
    @ds2026_cohort_id AS cohort_id;

SELECT
    c.course_code,
    c.name,
    c.credit_theory,
    c.credit_lab,
    cp.id AS course_program_id,
    ct.code AS course_type,
    cp.semester_suggest,
    cp.year_suggest,
    cp.term_code,
    cp.is_required
FROM course c
JOIN course_program cp
  ON cp.course_id = c.id
 AND cp.program_id = @ds2026_program_id
 AND cp.cohort_id = @ds2026_cohort_id
LEFT JOIN course_type ct
  ON ct.id = cp.course_type_id
WHERE UPPER(REPLACE(REPLACE(TRIM(c.course_code), '-', ''), ' ', '')) IN (
    'PE021IU','PE021',
    'IT178IU','IT178',
    'IT176IU','IT176',
    'IT172IU','IT172',
    'IT173IU','IT173',
    'IT169IU','IT169',
    'IT076IU','IT076',
    'IT170IU','IT170',
    'IT093IU','IT093',
    'IT164IU','IT164',
    'IT153IU','IT153'
)
ORDER BY c.course_code;

SELECT COUNT(*) AS reconciled_ds2026_mapping_count
FROM course c
JOIN course_program cp
  ON cp.course_id = c.id
 AND cp.program_id = @ds2026_program_id
 AND cp.cohort_id = @ds2026_cohort_id
WHERE UPPER(REPLACE(REPLACE(TRIM(c.course_code), '-', ''), ' ', '')) IN (
    'PE021IU','PE021',
    'IT178IU','IT178',
    'IT176IU','IT176',
    'IT172IU','IT172',
    'IT173IU','IT173',
    'IT169IU','IT169',
    'IT076IU','IT076',
    'IT170IU','IT170',
    'IT093IU','IT093',
    'IT164IU','IT164',
    'IT153IU','IT153'
);

-- Expected: 11
