-- Audited correction: MySQL ON UPDATE changed updated_at during the first cleanup.
-- Restore the exact timestamp from the fresh pre-cleanup affected-table backup.
SET time_zone = '+00:00';
DROP PROCEDURE IF EXISTS cleanup_it116_preserve_timestamp_20260910;
DELIMITER $$
CREATE PROCEDURE cleanup_it116_preserve_timestamp_20260910()
BEGIN
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 START TRANSACTION;
 SELECT id FROM syllabus WHERE id=3013 FOR UPDATE;
 IF (SELECT COUNT(*) FROM syllabus WHERE id=3013 AND course_id=53 AND version_number=1
 AND version_label='v1.0' AND academic_year='CS2026' AND semester='Semester 2'
 AND program='CS-2021' AND status='DRAFT' AND updated_at='2026-09-09 17:34:45') <> 1 THEN
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Timestamp correction guard failed'; END IF;
 UPDATE syllabus SET updated_at='2026-09-07 03:21:48' WHERE id=3013;
 SELECT ROW_COUNT() AS timestamp_restored_rows;
 COMMIT;
END$$
DELIMITER ;
CALL cleanup_it116_preserve_timestamp_20260910();
DROP PROCEDURE cleanup_it116_preserve_timestamp_20260910;
