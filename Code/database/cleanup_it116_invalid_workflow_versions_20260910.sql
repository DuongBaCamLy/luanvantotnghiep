-- One-time IT116IU repair. Run using mysql WITHOUT --force, in curriculum_iu.
-- Default is a complete rollback rehearsal. SET @cleanup_commit = 1 before sourcing to commit.
-- No migrations, FK disabling, shared-record deletion, or file operations.
DELIMITER $$
CREATE PROCEDURE cleanup_it116_20260910()
BEGIN
 DECLARE n INT;
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 CREATE TEMPORARY TABLE cleanup_audit (action VARCHAR(100), affected_rows INT);
 CREATE TEMPORARY TABLE expected_fk (t VARCHAR(64), c VARCHAR(64), p VARCHAR(64));
 INSERT INTO expected_fk VALUES
 ('approval_request','syllabus_id','syllabus'),
 ('assessment_component','syllabus_id','syllabus'),
 ('class_section','syllabus_id','syllabus'),
 ('clo','syllabus_id','syllabus'),
 ('course_program','syllabus_id','syllabus'),
 ('syllabus_book','syllabus_id','syllabus'),
 ('syllabus_import_history','syllabus_id','syllabus'),
 ('syllabus_source_snapshot','syllabus_id','syllabus'),
 ('topic','syllabus_id','syllabus'),
 ('assessment_clo','clo_id','clo'),
 ('assessment_clo','assessment_component_id','assessment_component'),
 ('student_score','assessment_component_id','assessment_component'),
 ('topic_clo','topic_id','topic'),
 ('topic_clo','clo_id','clo'),
 ('clo_plo_mapping','clo_id','clo');
 SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
 START TRANSACTION;
 -- Lock the course's complete version range and preserved links before checking.
 SELECT id FROM course WHERE course_code='IT116IU' FOR UPDATE;
 SELECT id FROM syllabus WHERE course_id=53 FOR UPDATE;
 SELECT id FROM course_program WHERE course_id=53 FOR UPDATE;
 SELECT id FROM class_section WHERE course_id=53 FOR UPDATE;
 IF DATABASE() <> 'curriculum_iu' OR @@foreign_key_checks <> 1 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Wrong database or FK checks disabled';
 END IF;
 IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
   AND TABLE_TYPE='BASE TABLE' AND ENGINE <> 'InnoDB') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Nontransactional table found';
 END IF;
 IF EXISTS (SELECT 1 FROM information_schema.KEY_COLUMN_USAGE k
   WHERE k.CONSTRAINT_SCHEMA=DATABASE() AND k.REFERENCED_TABLE_NAME IN ('syllabus','clo','topic','assessment_component')
   AND NOT EXISTS (SELECT 1 FROM expected_fk e WHERE e.t=k.TABLE_NAME AND e.c=k.COLUMN_NAME AND e.p=k.REFERENCED_TABLE_NAME))
 OR (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE CONSTRAINT_SCHEMA=DATABASE()
   AND REFERENCED_TABLE_NAME IN ('syllabus','clo','topic','assessment_component')) <> 15 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='FK graph changed; review cleanup';
 END IF;
 IF EXISTS (SELECT 1 FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE()
   AND EVENT_OBJECT_TABLE IN ('syllabus','course_program','class_section','approval_request','assessment_component',
   'assessment_clo','student_score','clo','clo_plo_mapping','topic','topic_clo','syllabus_book','syllabus_import_history','syllabus_source_snapshot')) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unexpected trigger on cleanup tables';
 END IF;
 IF (SELECT COUNT(*) FROM syllabus s JOIN course c ON c.id=s.course_id WHERE s.id=3013 AND c.id=53
   AND c.course_code='IT116IU' AND s.academic_year='CS2026' AND s.semester='Semester 2'
   AND s.status='DRAFT' AND s.version_number=1 AND s.version_label='Version 1' AND s.program='CS-2021') <> 1
 OR (SELECT COUNT(*) FROM syllabus WHERE course_id=53) <> 4
 OR (SELECT COUNT(*) FROM syllabus WHERE course_id=53 AND academic_year='2026-2027' AND semester='HK1'
   AND ((id=3048 AND version_number=2 AND status='DRAFT') OR (id=3049 AND version_number=3 AND status='ARCHIVED')
   OR (id=3050 AND version_number=4 AND status='SUBMITTED'))) <> 3 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Syllabus identity guard failed';
 END IF;
 IF (SELECT COUNT(*) FROM course_program cp JOIN cohort h ON h.id=cp.cohort_id JOIN program p ON p.id=cp.program_id
   WHERE cp.course_id=53 AND p.code='CS-2021' AND cp.semester_suggest=2
   AND ((cp.id=246 AND cp.syllabus_id=3013 AND h.name='CS2026') OR (cp.id=196 AND cp.syllabus_id=3050 AND h.name='CS2021'))) <> 2
 OR (SELECT COUNT(*) FROM course_program WHERE syllabus_id IN (3048,3049,3050)) <> 1
 OR (SELECT COUNT(*) FROM class_section WHERE id=11 AND course_id=53 AND syllabus_id=3050) <> 1
 OR (SELECT COUNT(*) FROM class_section WHERE syllabus_id IN (3048,3049,3050)) <> 1 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Preserved assignment guard failed';
 END IF;
 -- Abort on cross-syllabus links rather than altering another syllabus's content.
 IF EXISTS (SELECT 1 FROM assessment_clo x JOIN assessment_component a ON a.id=x.assessment_component_id JOIN clo c ON c.id=x.clo_id
   WHERE (a.syllabus_id IN (3048,3049,3050)) <> (c.syllabus_id IN (3048,3049,3050)))
 OR EXISTS (SELECT 1 FROM topic_clo x JOIN topic t ON t.id=x.topic_id JOIN clo c ON c.id=x.clo_id
   WHERE (t.syllabus_id IN (3048,3049,3050)) <> (c.syllabus_id IN (3048,3049,3050))) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Cross-syllabus child link found';
 END IF;
 UPDATE course_program SET syllabus_id=NULL WHERE id=196;
 INSERT INTO cleanup_audit VALUES ('unlink course_program 196', ROW_COUNT());
 UPDATE class_section SET syllabus_id=NULL WHERE id=11;
 INSERT INTO cleanup_audit VALUES ('unlink class_section 11', ROW_COUNT());
 DELETE x FROM student_score x JOIN assessment_component p ON p.id=x.assessment_component_id WHERE p.syllabus_id IN (3048,3049,3050);
 INSERT INTO cleanup_audit VALUES ('delete student_score', ROW_COUNT());
 DELETE x FROM assessment_clo x JOIN assessment_component p ON p.id=x.assessment_component_id WHERE p.syllabus_id IN (3048,3049,3050);
 INSERT INTO cleanup_audit VALUES ('delete assessment_clo', ROW_COUNT());
 DELETE x FROM topic_clo x JOIN topic p ON p.id=x.topic_id WHERE p.syllabus_id IN (3048,3049,3050);
 INSERT INTO cleanup_audit VALUES ('delete topic_clo', ROW_COUNT());
 DELETE x FROM clo_plo_mapping x JOIN clo p ON p.id=x.clo_id WHERE p.syllabus_id IN (3048,3049,3050);
 INSERT INTO cleanup_audit VALUES ('delete clo_plo_mapping', ROW_COUNT());
 DELETE FROM approval_request WHERE syllabus_id IN (3048,3049,3050);
 INSERT INTO cleanup_audit VALUES ('delete approval_request', ROW_COUNT());
 DELETE FROM syllabus_import_history WHERE syllabus_id IN (3048,3049,3050);
 INSERT INTO cleanup_audit VALUES ('delete syllabus_import_history', ROW_COUNT());
 DELETE FROM syllabus_source_snapshot WHERE syllabus_id IN (3048,3049,3050);
 INSERT INTO cleanup_audit VALUES ('delete syllabus_source_snapshot', ROW_COUNT());
 DELETE FROM syllabus_book WHERE syllabus_id IN (3048,3049,3050);
 INSERT INTO cleanup_audit VALUES ('delete syllabus_book', ROW_COUNT());
 DELETE FROM assessment_component WHERE syllabus_id IN (3048,3049,3050);
 INSERT INTO cleanup_audit VALUES ('delete assessment_component', ROW_COUNT());
 DELETE FROM topic WHERE syllabus_id IN (3048,3049,3050);
 INSERT INTO cleanup_audit VALUES ('delete topic', ROW_COUNT());
 DELETE FROM clo WHERE syllabus_id IN (3048,3049,3050);
 INSERT INTO cleanup_audit VALUES ('delete clo', ROW_COUNT());
 DELETE FROM syllabus WHERE id IN (3048,3049,3050);
 INSERT INTO cleanup_audit VALUES ('delete syllabus', ROW_COUNT());
 UPDATE syllabus SET version_label='v1.0', updated_at=updated_at WHERE id=3013;
 INSERT INTO cleanup_audit VALUES ('normalize 3013 label', ROW_COUNT());
 IF EXISTS (SELECT 1 FROM approval_request WHERE syllabus_id IN (3048,3049,3050)) OR
 EXISTS (SELECT 1 FROM assessment_component WHERE syllabus_id IN (3048,3049,3050)) OR
 EXISTS (SELECT 1 FROM class_section WHERE syllabus_id IN (3048,3049,3050)) OR
 EXISTS (SELECT 1 FROM clo WHERE syllabus_id IN (3048,3049,3050)) OR
 EXISTS (SELECT 1 FROM course_program WHERE syllabus_id IN (3048,3049,3050)) OR
 EXISTS (SELECT 1 FROM syllabus_book WHERE syllabus_id IN (3048,3049,3050)) OR
 EXISTS (SELECT 1 FROM syllabus_import_history WHERE syllabus_id IN (3048,3049,3050)) OR
 EXISTS (SELECT 1 FROM syllabus_source_snapshot WHERE syllabus_id IN (3048,3049,3050)) OR
 EXISTS (SELECT 1 FROM topic WHERE syllabus_id IN (3048,3049,3050)) THEN
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Dangling syllabus reference'; END IF;
 IF (SELECT COUNT(*) FROM syllabus WHERE course_id=53) <> 1
 OR (SELECT COUNT(*) FROM syllabus WHERE id=3013 AND course_id=53 AND version_number=1 AND version_label='v1.0'
 AND academic_year='CS2026' AND semester='Semester 2' AND status='DRAFT' AND program='CS-2021') <> 1
 OR EXISTS (SELECT 1 FROM syllabus WHERE id IN (3048,3049,3050))
 OR (SELECT COUNT(*) FROM course_program WHERE id=196 AND syllabus_id IS NULL) <> 1
 OR (SELECT COUNT(*) FROM course_program WHERE id=246 AND syllabus_id=3013) <> 1
 OR (SELECT COUNT(*) FROM class_section WHERE id=11 AND syllabus_id IS NULL) <> 1 THEN
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Final invariant failed'; END IF;
 SELECT * FROM cleanup_audit;
 IF COALESCE(@cleanup_commit,0)=1 THEN
  COMMIT;
  SELECT 'COMMITTED: all invariants passed' AS result;
 ELSE
  ROLLBACK;
  SELECT 'REHEARSAL: all invariants passed; changes rolled back' AS result;
 END IF;
 DROP TEMPORARY TABLE cleanup_audit;
 DROP TEMPORARY TABLE expected_fk;
END$$
DELIMITER ;
CALL cleanup_it116_20260910();
DROP PROCEDURE cleanup_it116_20260910;
