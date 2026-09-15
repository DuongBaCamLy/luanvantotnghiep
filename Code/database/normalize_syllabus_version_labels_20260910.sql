-- Whole workflow revisions only. Reviewed live manifest: 60 rows, 55 legacy labels.
-- Run in curriculum_iu using mysql WITHOUT --force. Default is rollback rehearsal.
-- SET @version_normalization_commit=1 before sourcing to commit.
-- No schema migrations, status changes, or timestamp advancement.
DROP PROCEDURE IF EXISTS normalize_syllabus_versions_20260910;
DELIMITER $$
CREATE PROCEDURE normalize_syllabus_versions_20260910()
BEGIN
 DECLARE changed_rows INT DEFAULT 0;
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 CREATE TEMPORARY TABLE expected_versions(id INT PRIMARY KEY, version_number INT, label_hex TEXT, status VARCHAR(32)) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
 CREATE TEMPORARY TABLE before_versions LIKE syllabus;
 INSERT INTO expected_versions VALUES
(2993,1,'56657273696F6E2031','DRAFT'),
(2994,1,'56657273696F6E2031','DRAFT'),
(2995,1,'56657273696F6E2031','DRAFT'),
(2996,1,'56657273696F6E2031','DRAFT'),
(2997,1,'56657273696F6E2031','DRAFT'),
(2998,1,'56657273696F6E2031','DRAFT'),
(2999,1,'56657273696F6E2031','DRAFT'),
(3000,1,'56657273696F6E2031','DRAFT'),
(3001,1,'56657273696F6E2031','DRAFT'),
(3002,1,'56657273696F6E2031','DRAFT'),
(3003,1,'56657273696F6E2031','DRAFT'),
(3004,1,'56657273696F6E2031','DRAFT'),
(3005,1,'56657273696F6E2031','DRAFT'),
(3006,1,'56657273696F6E2031','DRAFT'),
(3007,1,'56657273696F6E2031','DRAFT'),
(3008,1,'56657273696F6E2031','DRAFT'),
(3009,1,'56657273696F6E2031','DRAFT'),
(3010,1,'56657273696F6E2031','DRAFT'),
(3011,1,'56657273696F6E2031','DRAFT'),
(3012,1,'56657273696F6E2031','DRAFT'),
(3013,1,'76312E30','DRAFT'),
(3014,1,'56657273696F6E2031','DRAFT'),
(3015,1,'56657273696F6E2031','DRAFT'),
(3016,1,'56657273696F6E2031','DRAFT'),
(3017,1,'56657273696F6E2031','DRAFT'),
(3018,1,'56657273696F6E2031','DRAFT'),
(3019,1,'56657273696F6E2031','DRAFT'),
(3020,1,'56657273696F6E2031','DRAFT'),
(3021,1,'56657273696F6E2031','DRAFT'),
(3022,1,'56657273696F6E2031','DRAFT'),
(3023,1,'56657273696F6E2031','DRAFT'),
(3024,1,'56657273696F6E2031','DRAFT'),
(3025,1,'56657273696F6E2031','DRAFT'),
(3026,1,'56657273696F6E2031','DRAFT'),
(3027,1,'56657273696F6E2031','DRAFT'),
(3028,1,'56657273696F6E2031','DRAFT'),
(3029,1,'56657273696F6E2031','DRAFT'),
(3030,1,'56657273696F6E2031','DRAFT'),
(3031,1,'56657273696F6E2031','DRAFT'),
(3032,1,'56657273696F6E2031','DRAFT'),
(3033,1,'56657273696F6E2031','DRAFT'),
(3034,1,'56657273696F6E2031','DRAFT'),
(3035,1,'56657273696F6E2031','DRAFT'),
(3036,1,'56657273696F6E2031','DRAFT'),
(3037,1,'56657273696F6E2031','DRAFT'),
(3038,1,'56657273696F6E2031','ARCHIVED'),
(3039,1,'56657273696F6E2031','DRAFT'),
(3040,1,'56657273696F6E2031','DRAFT'),
(3041,1,'56657273696F6E2031','DRAFT'),
(3042,1,'56657273696F6E2031','DRAFT'),
(3043,1,'56657273696F6E2031','DRAFT'),
(3044,1,'56657273696F6E2031','DRAFT'),
(3045,1,'56657273696F6E2031','DRAFT'),
(3046,1,'56657273696F6E2031','ARCHIVED'),
(3047,1,'56657273696F6E2031','ARCHIVED'),
(3051,2,'76322E30','SUBMITTED'),
(3052,2,'76322E30','SUBMITTED'),
(3053,2,'76322E30','SUBMITTED'),
(3054,2,'56657273696F6E2032','ARCHIVED'),
(3055,3,'76332E30','APPROVED');
 SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
 START TRANSACTION;
 SELECT id FROM syllabus ORDER BY id FOR UPDATE;
 IF DATABASE() <> 'curriculum_iu'
 OR (SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='syllabus') <> 'InnoDB'
 OR EXISTS (SELECT 1 FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE() AND EVENT_OBJECT_TABLE='syllabus') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unexpected database, engine or syllabus trigger'; END IF;
 IF (SELECT COUNT(*) FROM syllabus) <> 60
 OR EXISTS (SELECT 1 FROM expected_versions e LEFT JOIN syllabus s ON s.id=e.id
 WHERE s.id IS NULL OR s.version_number <> e.version_number OR NOT (HEX(s.version_label) <=> e.label_hex) OR s.status <> e.status) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Live version manifest changed; review before normalizing'; END IF;
 -- Only recognized whole-revision legacy forms are safe. Any minor value/unknown text aborts.
 IF EXISTS (SELECT 1 FROM syllabus WHERE version_number < 1 OR version_number IS NULL
 OR (version_label IS NOT NULL AND NOT (
 BINARY version_label = BINARY CONCAT('v',version_number,'.0')
 OR BINARY version_label = BINARY CONCAT('Version ',version_number)
 OR BINARY version_label = BINARY CONCAT('Version ',version_number,'.0')
 OR BINARY version_label = BINARY CONCAT('v',version_number)
 OR BINARY version_label = BINARY CAST(version_number AS CHAR)))) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unknown or meaningful minor version found; do not overwrite'; END IF;
 INSERT INTO before_versions SELECT * FROM syllabus;
 UPDATE syllabus SET version_label=CONCAT('v',version_number,'.0'), updated_at=updated_at
 WHERE version_label IS NULL OR BINARY version_label <> BINARY CONCAT('v',version_number,'.0');
 SET changed_rows=ROW_COUNT();
 IF changed_rows <> 55 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unexpected normalized row count'; END IF;
 IF EXISTS (SELECT 1 FROM syllabus WHERE version_label IS NULL
 OR BINARY version_label <> BINARY CONCAT('v',version_number,'.0')) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Final canonical label verification failed'; END IF;
 -- Compare every other column including content, version number, identities and all timestamps.
 IF (SELECT COUNT(*) FROM syllabus) <> 60
 OR EXISTS (SELECT 1 FROM before_versions b LEFT JOIN syllabus s ON s.id=b.id WHERE s.id IS NULL OR
      NOT (s.`id` <=> b.`id`) OR
      NOT (s.`course_id` <=> b.`course_id`) OR
      NOT (s.`version_number` <=> b.`version_number`) OR
      NOT (s.`academic_year` <=> b.`academic_year`) OR
      NOT (s.`status` <=> b.`status`) OR
      NOT (s.`is_current` <=> b.`is_current`) OR
      NOT (s.`created_by` <=> b.`created_by`) OR
      NOT (s.`approved_by` <=> b.`approved_by`) OR
      NOT (s.`submitted_at` <=> b.`submitted_at`) OR
      NOT (s.`approved_at` <=> b.`approved_at`) OR
      NOT (s.`change_summary` <=> b.`change_summary`) OR
      NOT (s.`notes` <=> b.`notes`) OR
      NOT (s.`created_at` <=> b.`created_at`) OR
      NOT (s.`updated_at` <=> b.`updated_at`) OR
      NOT (s.`course_designation` <=> b.`course_designation`) OR
      NOT (s.`course_types` <=> b.`course_types`) OR
      NOT (s.`semester` <=> b.`semester`) OR
      NOT (s.`language` <=> b.`language`) OR
      NOT (s.`relation` <=> b.`relation`) OR
      NOT (s.`teaching_methods` <=> b.`teaching_methods`) OR
      NOT (s.`workload_total` <=> b.`workload_total`) OR
      NOT (s.`workload_contact` <=> b.`workload_contact`) OR
      NOT (s.`workload_private` <=> b.`workload_private`) OR
      NOT (s.`prerequisites` <=> b.`prerequisites`) OR
      NOT (s.`objectives` <=> b.`objectives`) OR
      NOT (s.`exam_forms` <=> b.`exam_forms`) OR
      NOT (s.`exam_requirements` <=> b.`exam_requirements`) OR
      NOT (s.`rubrics` <=> b.`rubrics`) OR
      NOT (s.`major` <=> b.`major`) OR
      NOT (s.`course_code_snapshot` <=> b.`course_code_snapshot`) OR
      NOT (s.`course_name_snapshot` <=> b.`course_name_snapshot`) OR
      NOT (s.`final_approval_date` <=> b.`final_approval_date`) OR
      NOT (s.`import_status` <=> b.`import_status`) OR
      NOT (s.`original_file_name` <=> b.`original_file_name`) OR
      NOT (s.`original_file_type` <=> b.`original_file_type`) OR
      NOT (s.`program` <=> b.`program`) OR
      NOT (s.`source_type` <=> b.`source_type`)) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='A non-label field changed; rolling back'; END IF;
 SELECT b.id,b.version_number,b.version_label AS before_label,s.version_label AS after_label,s.status
 FROM before_versions b JOIN syllabus s ON s.id=b.id
 WHERE NOT (BINARY b.version_label <=> BINARY s.version_label) ORDER BY b.id;
 SELECT changed_rows AS normalized_rows;
 IF COALESCE(@version_normalization_commit,0)=1 THEN
  COMMIT;
  SELECT 'COMMITTED: label-only verification passed' AS result;
 ELSE
  ROLLBACK;
  SELECT 'REHEARSAL: verified and rolled back' AS result;
 END IF;
 DROP TEMPORARY TABLE before_versions;
 DROP TEMPORARY TABLE expected_versions;
END$$
DELIMITER ;
CALL normalize_syllabus_versions_20260910();
DROP PROCEDURE normalize_syllabus_versions_20260910;
