-- Apply only after the guarded consolidation commits. MySQL DDL commits implicitly.
-- The single ALTER atomically replaces the obsolete course/revision unique key.
DELIMITER $$
CREATE PROCEDURE enforce_logical_syllabus_identity()
BEGIN
  IF DATABASE() <> 'curriculum_iu' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unexpected database';
  END IF;
  IF (SELECT COUNT(*) FROM syllabus) <> 56 OR (SELECT COUNT(*) FROM syllabus_revision_snapshot) <> 8 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Consolidation must finish first';
  END IF;
  IF EXISTS (SELECT 1 FROM syllabus GROUP BY course_id,program,academic_year,semester HAVING COUNT(*)>1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate logical identity remains';
  END IF;
  IF EXISTS (
    SELECT 1 FROM syllabus s
    LEFT JOIN program p ON BINARY p.code=BINARY s.program
    LEFT JOIN cohort c ON BINARY c.name=BINARY s.academic_year AND c.program_id=p.id
    WHERE p.id IS NULL OR c.id IS NULL
      OR (s.semester IS NOT NULL AND s.semester NOT REGEXP '^Semester [1-8]$')
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Noncanonical curriculum identity';
  END IF;
  ALTER TABLE syllabus
    ADD COLUMN semester_identity VARCHAR(20) GENERATED ALWAYS AS (COALESCE(semester,'')) STORED,
    ADD CONSTRAINT ck_syllabus_logical_identity CHECK (
      course_id IS NOT NULL AND program IS NOT NULL AND CHAR_LENGTH(TRIM(program))>0
      AND academic_year IS NOT NULL AND academic_year REGEXP '^[A-Za-z]+[0-9]{4}$'
      AND (semester IS NULL OR semester REGEXP '^Semester [1-8]$')),
    ADD CONSTRAINT uq_syllabus_logical_identity UNIQUE (course_id,program,academic_year,semester_identity),
    DROP INDEX uq_syllabus_version;
END$$
DELIMITER ;
CALL enforce_logical_syllabus_identity();
DROP PROCEDURE enforce_logical_syllabus_identity;
