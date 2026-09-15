-- Phase 1.3E
-- Keep official CS2021 curriculum mapping on CH011IU.
-- Add CHE011IU as a separate catalog code from the detailed syllabus.
-- INSERT-only, idempotent. No UPDATE / DELETE.

START TRANSACTION;

INSERT INTO course (
    course_code,
    name,
    name_vn,
    department_id,
    credit_theory,
    credit_lab,
    course_level,
    description,
    is_active,
    created_at
)
SELECT
    'CHE011IU',
    'Chemistry for Engineers',
    ch.name_vn,
    ch.department_id,
    ch.credit_theory,
    ch.credit_lab,
    ch.course_level,
    ch.description,
    ch.is_active,
    CURRENT_TIMESTAMP
FROM course ch
WHERE UPPER(TRIM(ch.course_code)) = 'CH011IU'
  AND NOT EXISTS (
      SELECT 1
      FROM course existing
      WHERE UPPER(TRIM(existing.course_code)) = 'CHE011IU'
  );

-- Record the verified relationship without duplicating the CS2021 curriculum row.
INSERT INTO course_relationship (
    course_id,
    related_course_id,
    relation_type
)
SELECT
    che.id,
    ch.id,
    'EQUIVALENT'
FROM course che
JOIN course ch
  ON UPPER(TRIM(ch.course_code)) = 'CH011IU'
WHERE UPPER(TRIM(che.course_code)) = 'CHE011IU'
  AND NOT EXISTS (
      SELECT 1
      FROM course_relationship cr
      WHERE cr.course_id = che.id
        AND cr.related_course_id = ch.id
        AND cr.relation_type = 'EQUIVALENT'
  );

COMMIT;

SELECT
    id,
    course_code,
    name,
    name_vn,
    department_id,
    credit_theory,
    credit_lab,
    course_level,
    is_active
FROM course
WHERE UPPER(TRIM(course_code)) IN ('CH011IU', 'CHE011IU')
ORDER BY course_code;

SELECT
    c1.course_code AS source_code,
    cr.relation_type,
    c2.course_code AS canonical_code
FROM course_relationship cr
JOIN course c1 ON c1.id = cr.course_id
JOIN course c2 ON c2.id = cr.related_course_id
WHERE UPPER(TRIM(c1.course_code)) = 'CHE011IU'
  AND UPPER(TRIM(c2.course_code)) = 'CH011IU'
  AND cr.relation_type = 'EQUIVALENT';
