SET NAMES utf8mb4;
USE curriculum_iu;

SET @it2026_cohort_id := (
    SELECT id FROM cohort
    WHERE UPPER(REPLACE(TRIM(name), ' ', '')) = 'IT2026'
    LIMIT 1
);

SET @it2026_program_id := (
    SELECT program_id FROM cohort WHERE id = @it2026_cohort_id
);

SELECT
    COUNT(*) AS reconciled_official_mapping_count
FROM course_program cp
JOIN course c ON c.id = cp.course_id
WHERE cp.program_id = @it2026_program_id
  AND cp.cohort_id = @it2026_cohort_id
  AND UPPER(REPLACE(REPLACE(TRIM(c.course_code), '-', ''), ' ', '')) IN (
      'MA036IU','MA036','IT159IU','IT159',
      'IT163IU','IT163','IT024IU','IT024','IT090IU','IT090',
      'IT092IU','IT092','IT114IU','IT114','IT138IU','IT138',
      'IT144IU','IT144','IT145IU','IT145','IT164IU','IT164',
      'IT150IU','IT150','IT157IU','IT157','IT158IU','IT158',
      'IT166IU','IT166','IT167IU','IT167'
  );

-- Expected: 16

SELECT
    c.course_code,
    c.name,
    CASE WHEN cp.id IS NULL THEN 'NOT_MAPPED' ELSE 'MAPPED' END AS it2026_scope
FROM course c
LEFT JOIN course_program cp
  ON cp.course_id = c.id
 AND cp.program_id = @it2026_program_id
 AND cp.cohort_id = @it2026_cohort_id
WHERE UPPER(REPLACE(REPLACE(TRIM(c.course_code), '-', ''), ' ', '')) IN (
    'IT165IU','IT165','IT155IU','IT155','IT173IU','IT173'
)
ORDER BY c.course_code;

-- IT165 should remain NOT_MAPPED if it exists in Catalog.
-- IT155 / IT173 may have no Catalog row at all; no row is also correct.
