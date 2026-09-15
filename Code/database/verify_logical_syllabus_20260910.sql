-- Read-only results plus rollback-only enforcement probes. No durable row changes.
SELECT course_id,program,academic_year,semester,COUNT(*) AS duplicates
FROM syllabus GROUP BY course_id,program,academic_year,semester HAVING COUNT(*)>1;
SELECT id,course_id,program,academic_year,semester,version_number,version_label,status
FROM syllabus WHERE course_id=53 ORDER BY academic_year;
SELECT syllabus_id,original_syllabus_id,version_number,event_type,JSON_VALID(content) AS valid_content
FROM syllabus_revision_snapshot ORDER BY syllabus_id,version_number;
DELIMITER $$
CREATE PROCEDURE verify_logical_syllabus_guards()
BEGIN
  DECLARE duplicate_blocked BOOLEAN DEFAULT FALSE;
  DECLARE snapshot_update_blocked BOOLEAN DEFAULT FALSE;
  DECLARE snapshot_delete_blocked BOOLEAN DEFAULT FALSE;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  BEGIN
    DECLARE CONTINUE HANDLER FOR 1062 SET duplicate_blocked=TRUE;
    UPDATE syllabus SET academic_year='CS2021' WHERE id=3013;
  END;
  BEGIN
    DECLARE CONTINUE HANDLER FOR SQLSTATE '45000' SET snapshot_update_blocked=TRUE;
    UPDATE syllabus_revision_snapshot SET actor=actor WHERE syllabus_id=3055;
  END;
  BEGIN
    DECLARE CONTINUE HANDLER FOR SQLSTATE '45000' SET snapshot_delete_blocked=TRUE;
    DELETE FROM syllabus_revision_snapshot WHERE syllabus_id=3055;
  END;
  ROLLBACK;
  IF NOT duplicate_blocked OR NOT snapshot_update_blocked OR NOT snapshot_delete_blocked THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='A database enforcement probe failed';
  END IF;
  SELECT duplicate_blocked,snapshot_update_blocked,snapshot_delete_blocked,'All probes rolled back' AS result;
END$$
DELIMITER ;
CALL verify_logical_syllabus_guards();
DROP PROCEDURE verify_logical_syllabus_guards;
