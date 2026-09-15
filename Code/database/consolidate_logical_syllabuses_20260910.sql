-- Read ONE_SYLLABUS_PREMIGRATION_AUDIT_20260910.md first; prepare schema separately.
-- Default is rollback rehearsal; SET @logical_syllabus_commit=1 explicitly to commit.
-- No disabling FK checks. Run mysql WITHOUT --force.
DROP PROCEDURE IF EXISTS consolidate_logical_syllabuses_20260910;
DELIMITER $$
CREATE PROCEDURE consolidate_logical_syllabuses_20260910()
BEGIN
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 CREATE TEMPORARY TABLE merge_chain(old_id INT PRIMARY KEY, survivor_id INT, course_id INT, old_version INT, new_version INT, new_status VARCHAR(32));
 INSERT INTO merge_chain VALUES (3047,3051,27,1,2,'SUBMITTED'),(3046,3053,36,1,2,'SUBMITTED'),(3054,3055,53,2,3,'APPROVED'),(3038,3052,116,1,2,'SUBMITTED');
 CREATE TEMPORARY TABLE preserved_syllabus LIKE syllabus;
 CREATE TEMPORARY TABLE merge_audit(action VARCHAR(100),affected_rows INT);
 SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
 START TRANSACTION;
 SELECT id FROM syllabus ORDER BY id FOR UPDATE;
 SELECT id FROM course_program ORDER BY id FOR UPDATE;
 SELECT id FROM class_section ORDER BY id FOR UPDATE;
 SELECT id FROM approval_request ORDER BY id FOR UPDATE;
 IF DATABASE()<>'curriculum_iu' OR @@foreign_key_checks<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Wrong database or disabled FK checks'; END IF;
 IF (SELECT COUNT(*) FROM syllabus)<>60 OR (SELECT COUNT(*) FROM syllabus_revision_snapshot)<>0
 OR (SELECT COUNT(*) FROM approval_request)<>5 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unexpected baseline row counts'; END IF;
 IF (SELECT COUNT(*) FROM merge_chain m JOIN syllabus o ON o.id=m.old_id JOIN syllabus s ON s.id=m.survivor_id
 WHERE o.course_id=m.course_id AND s.course_id=m.course_id AND o.program=s.program
 AND o.academic_year=s.academic_year AND (o.semester <=> s.semester)
 AND o.status='ARCHIVED' AND BINARY s.status=BINARY m.new_status
 AND o.version_number=m.old_version AND s.version_number=m.new_version)<>4 THEN
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Reviewed workflow chain identity changed'; END IF;
 IF NOT EXISTS (SELECT 1 FROM syllabus WHERE id=3013 AND course_id=53 AND program='CS-2021' AND academic_year='CS2026' AND semester='Semester 2' AND version_number=1 AND status='DRAFT')
 OR NOT EXISTS (SELECT 1 FROM course_program WHERE id=246 AND syllabus_id=3013)
 OR NOT EXISTS (SELECT 1 FROM course_program WHERE id=196 AND syllabus_id=3055)
 OR NOT EXISTS (SELECT 1 FROM class_section WHERE id=11 AND syllabus_id=3055)
 OR NOT EXISTS (SELECT 1 FROM course_program WHERE id=297 AND syllabus_id=3052 AND semester_suggest IS NULL AND term_code='ELECTIVE') THEN
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Preserved curriculum identity guard failed'; END IF;
 IF EXISTS (SELECT 1 FROM information_schema.KEY_COLUMN_USAGE WHERE CONSTRAINT_SCHEMA=DATABASE()
 AND REFERENCED_TABLE_NAME='syllabus' AND TABLE_NAME NOT IN ('approval_request','assessment_component','class_section','clo','course_program','syllabus_book','syllabus_import_history','syllabus_source_snapshot','topic','syllabus_revision_snapshot')) THEN
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unexpected syllabus FK; review dependency graph'; END IF;
 IF EXISTS (SELECT 1 FROM syllabus_source_snapshot WHERE syllabus_id IN (3038,3046,3047,3054))
 OR EXISTS (SELECT 1 FROM student_score x JOIN assessment_component a ON a.id=x.assessment_component_id WHERE a.syllabus_id IN (3038,3046,3047,3054)) THEN
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Source snapshot or student score requires explicit reparenting'; END IF;
 IF EXISTS (SELECT 1 FROM assessment_clo x JOIN assessment_component a ON a.id=x.assessment_component_id JOIN clo c ON c.id=x.clo_id
 WHERE (a.syllabus_id IN (3038,3046,3047,3054)) <> (c.syllabus_id IN (3038,3046,3047,3054)))
 OR EXISTS (SELECT 1 FROM topic_clo x JOIN topic t ON t.id=x.topic_id JOIN clo c ON c.id=x.clo_id
 WHERE (t.syllabus_id IN (3038,3046,3047,3054)) <> (c.syllabus_id IN (3038,3046,3047,3054))) THEN
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Cross-syllabus detail mapping found'; END IF;
 INSERT INTO preserved_syllabus SELECT * FROM syllabus WHERE id NOT IN (3038,3046,3047,3054);
 -- Snapshot BOTH predecessor and survivor before changing any child links.
 INSERT INTO syllabus_revision_snapshot(syllabus_id,original_syllabus_id,version_number,version_label,event_type,captured_at,actor,content)
 SELECT COALESCE(m.survivor_id,s.id),s.id,s.version_number,s.version_label,'MIGRATED',s.updated_at,'Migration',
JSON_OBJECT('syllabus',JSON_OBJECT('id',s.`id`,'course_id',s.`course_id`,'version_number',s.`version_number`,'version_label',s.`version_label`,'academic_year',s.`academic_year`,'status',s.`status`,'is_current',s.`is_current`,'created_by',s.`created_by`,'approved_by',s.`approved_by`,'submitted_at',s.`submitted_at`,'approved_at',s.`approved_at`,'change_summary',s.`change_summary`,'notes',s.`notes`,'created_at',s.`created_at`,'updated_at',s.`updated_at`,'course_designation',s.`course_designation`,'course_types',s.`course_types`,'semester',s.`semester`,'language',s.`language`,'relation',s.`relation`,'teaching_methods',s.`teaching_methods`,'workload_total',s.`workload_total`,'workload_contact',s.`workload_contact`,'workload_private',s.`workload_private`,'prerequisites',s.`prerequisites`,'objectives',s.`objectives`,'exam_forms',s.`exam_forms`,'exam_requirements',s.`exam_requirements`,'rubrics',s.`rubrics`,'major',s.`major`,'course_code_snapshot',s.`course_code_snapshot`,'course_name_snapshot',s.`course_name_snapshot`,'final_approval_date',s.`final_approval_date`,'import_status',s.`import_status`,'original_file_name',s.`original_file_name`,'original_file_type',s.`original_file_type`,'program',s.`program`,'source_type',s.`source_type`,'lock_version',s.`lock_version`),
'clo',COALESCE((SELECT JSON_ARRAYAGG(JSON_OBJECT('id',x.`id`,'syllabus_id',x.`syllabus_id`,'code',x.`code`,'description',x.`description`,'description_vn',x.`description_vn`,'competency_level',x.`competency_level`,'bloom_level',x.`bloom_level`,'order_index',x.`order_index`)) FROM `clo` x  WHERE x.syllabus_id=s.id),JSON_ARRAY()),
'topic',COALESCE((SELECT JSON_ARRAYAGG(JSON_OBJECT('id',x.`id`,'syllabus_id',x.`syllabus_id`,'week_number',x.`week_number`,'order_in_week',x.`order_in_week`,'name',x.`name`,'name_vn',x.`name_vn`,'teaching_hours',x.`teaching_hours`,'lab_hours',x.`lab_hours`,'self_study_hours',x.`self_study_hours`,'topic_type',x.`topic_type`,'teaching_method',x.`teaching_method`,'learning_activity',x.`learning_activity`,'notes',x.`notes`,'assessments',x.`assessments`,'resources',x.`resources`)) FROM `topic` x  WHERE x.syllabus_id=s.id),JSON_ARRAY()),
'assessment_component',COALESCE((SELECT JSON_ARRAYAGG(JSON_OBJECT('id',x.`id`,'syllabus_id',x.`syllabus_id`,'name',x.`name`,'name_vn',x.`name_vn`,'assessment_type',x.`assessment_type`,'weight_percent',x.`weight_percent`,'min_score',x.`min_score`,'max_score',x.`max_score`,'order_index',x.`order_index`)) FROM `assessment_component` x  WHERE x.syllabus_id=s.id),JSON_ARRAY()),
'syllabus_book',COALESCE((SELECT JSON_ARRAYAGG(JSON_OBJECT('syllabus_id',x.`syllabus_id`,'book_id',x.`book_id`,'usage_type',x.`usage_type`,'order_index',x.`order_index`)) FROM `syllabus_book` x  WHERE x.syllabus_id=s.id),JSON_ARRAY()),
'syllabus_import_history',COALESCE((SELECT JSON_ARRAYAGG(JSON_OBJECT('id',x.`id`,'created_at',x.`created_at`,'error_message',x.`error_message`,'import_status',x.`import_status`,'original_file_name',x.`original_file_name`,'original_file_type',x.`original_file_type`,'updated_at',x.`updated_at`,'imported_by',x.`imported_by`,'syllabus_id',x.`syllabus_id`)) FROM `syllabus_import_history` x  WHERE x.syllabus_id=s.id),JSON_ARRAY()),
'clo_plo_mapping',COALESCE((SELECT JSON_ARRAYAGG(JSON_OBJECT('id',x.`id`,'clo_id',x.`clo_id`,'plo_id',x.`plo_id`,'level',x.`level`,'contribution_weight',x.`contribution_weight`,'notes',x.`notes`)) FROM `clo_plo_mapping` x JOIN clo p ON p.id=x.clo_id WHERE p.syllabus_id=s.id),JSON_ARRAY()),
'topic_clo',COALESCE((SELECT JSON_ARRAYAGG(JSON_OBJECT('topic_id',x.`topic_id`,'clo_id',x.`clo_id`,'teaching_level',x.`teaching_level`)) FROM `topic_clo` x JOIN topic p ON p.id=x.topic_id WHERE p.syllabus_id=s.id),JSON_ARRAY()),
'assessment_clo',COALESCE((SELECT JSON_ARRAYAGG(JSON_OBJECT('assessment_component_id',x.`assessment_component_id`,'clo_id',x.`clo_id`,'contribution_percent',x.`contribution_percent`)) FROM `assessment_clo` x JOIN assessment_component p ON p.id=x.assessment_component_id WHERE p.syllabus_id=s.id),JSON_ARRAY()),
'student_score',COALESCE((SELECT JSON_ARRAYAGG(JSON_OBJECT('id',x.`id`,'enrollment_id',x.`enrollment_id`,'assessment_component_id',x.`assessment_component_id`,'raw_score',x.`raw_score`,'final_score',x.`final_score`,'is_absent',x.`is_absent`,'remark',x.`remark`,'recorded_at',x.`recorded_at`,'recorded_by',x.`recorded_by`)) FROM `student_score` x JOIN assessment_component p ON p.id=x.assessment_component_id WHERE p.syllabus_id=s.id),JSON_ARRAY()),
'book',COALESCE((SELECT JSON_ARRAYAGG(JSON_OBJECT('id',x.`id`,'title',x.`title`,'author',x.`author`,'publisher',x.`publisher`,'year',x.`year`,'edition',x.`edition`,'isbn',x.`isbn`,'url',x.`url`,'book_type',x.`book_type`)) FROM `book` x JOIN syllabus_book b ON b.book_id=x.id WHERE b.syllabus_id=s.id),JSON_ARRAY()),
'plo',COALESCE((SELECT JSON_ARRAYAGG(JSON_OBJECT('id',x.`id`,'program_id',x.`program_id`,'code',x.`code`,'description',x.`description`,'description_vn',x.`description_vn`,'category',x.`category`,'version_number',x.`version_number`,'is_active',x.`is_active`,'created_at',x.`created_at`)) FROM `plo` x  WHERE x.id IN (SELECT m.plo_id FROM clo_plo_mapping m JOIN clo c ON c.id=m.clo_id WHERE c.syllabus_id=s.id)),JSON_ARRAY()),
'course',COALESCE((SELECT JSON_ARRAYAGG(JSON_OBJECT('id',x.`id`,'course_code',x.`course_code`,'name',x.`name`,'name_vn',x.`name_vn`,'department_id',x.`department_id`,'credit_theory',x.`credit_theory`,'credit_lab',x.`credit_lab`,'course_level',x.`course_level`,'description',x.`description`,'is_active',x.`is_active`,'created_at',x.`created_at`)) FROM `course` x  WHERE x.id=s.course_id),JSON_ARRAY()))
 FROM syllabus s LEFT JOIN merge_chain m ON m.old_id=s.id
 WHERE s.id IN (3038,3046,3047,3054,3051,3052,3053,3055);
 INSERT INTO merge_audit VALUES ('preserved revision snapshots',ROW_COUNT());
 IF (SELECT COUNT(*) FROM syllabus_revision_snapshot)<>8 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing history snapshots'; END IF;
 UPDATE approval_request a JOIN merge_chain m ON m.old_id=a.syllabus_id SET a.syllabus_id=m.survivor_id;
 INSERT INTO merge_audit VALUES ('reparent approval history',ROW_COUNT());
 UPDATE syllabus_import_history a JOIN merge_chain m ON m.old_id=a.syllabus_id SET a.syllabus_id=m.survivor_id;
 INSERT INTO merge_audit VALUES ('reparent import history',ROW_COUNT());
 UPDATE course_program a JOIN merge_chain m ON m.old_id=a.syllabus_id SET a.syllabus_id=m.survivor_id;
 INSERT INTO merge_audit VALUES ('relink course_program',ROW_COUNT());
 UPDATE class_section a JOIN merge_chain m ON m.old_id=a.syllabus_id SET a.syllabus_id=m.survivor_id;
 INSERT INTO merge_audit VALUES ('relink class_section',ROW_COUNT());
 DELETE x FROM assessment_clo x JOIN assessment_component p ON p.id=x.assessment_component_id WHERE p.syllabus_id IN (3038,3046,3047,3054);
 INSERT INTO merge_audit VALUES ('delete snapshotted assessment_clo',ROW_COUNT());
 DELETE x FROM topic_clo x JOIN topic p ON p.id=x.topic_id WHERE p.syllabus_id IN (3038,3046,3047,3054);
 INSERT INTO merge_audit VALUES ('delete snapshotted topic_clo',ROW_COUNT());
 DELETE x FROM clo_plo_mapping x JOIN clo p ON p.id=x.clo_id WHERE p.syllabus_id IN (3038,3046,3047,3054);
 INSERT INTO merge_audit VALUES ('delete snapshotted clo_plo_mapping',ROW_COUNT());
 DELETE FROM syllabus_book WHERE syllabus_id IN (3038,3046,3047,3054);
 INSERT INTO merge_audit VALUES ('delete snapshotted syllabus_book',ROW_COUNT());
 DELETE FROM assessment_component WHERE syllabus_id IN (3038,3046,3047,3054);
 INSERT INTO merge_audit VALUES ('delete snapshotted assessment_component',ROW_COUNT());
 DELETE FROM topic WHERE syllabus_id IN (3038,3046,3047,3054);
 INSERT INTO merge_audit VALUES ('delete snapshotted topic',ROW_COUNT());
 DELETE FROM clo WHERE syllabus_id IN (3038,3046,3047,3054);
 INSERT INTO merge_audit VALUES ('delete snapshotted clo',ROW_COUNT());
 DELETE FROM syllabus WHERE id IN (3038,3046,3047,3054);
 INSERT INTO merge_audit VALUES ('delete obsolete syllabus',ROW_COUNT());
 IF (SELECT COUNT(*) FROM syllabus)<>56 OR EXISTS (SELECT 1 FROM syllabus GROUP BY course_id,program,academic_year,semester HAVING COUNT(*)>1) THEN
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate logical identity remains'; END IF;
 IF EXISTS (SELECT 1 FROM preserved_syllabus b LEFT JOIN syllabus s ON s.id=b.id WHERE s.id IS NULL OR
NOT (s.`id` <=> b.`id`) OR
NOT (s.`course_id` <=> b.`course_id`) OR
NOT (s.`version_number` <=> b.`version_number`) OR
NOT (s.`version_label` <=> b.`version_label`) OR
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
NOT (s.`source_type` <=> b.`source_type`) OR
NOT (s.`lock_version` <=> b.`lock_version`)) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Survivor/unrelated data changed'; END IF;
 IF (SELECT COUNT(*) FROM approval_request)<>5 OR EXISTS (SELECT 1 FROM approval_request WHERE syllabus_version_number IS NULL)
 OR (SELECT COUNT(*) FROM syllabus WHERE course_id=53)<>2
 OR NOT EXISTS (SELECT 1 FROM syllabus WHERE id=3055 AND academic_year='CS2021' AND semester='Semester 2' AND version_number=3 AND status='APPROVED')
 OR NOT EXISTS (SELECT 1 FROM syllabus WHERE id=3013 AND academic_year='CS2026' AND semester='Semester 2' AND version_number=1 AND status='DRAFT') THEN
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Approval or IT116IU invariant failed'; END IF;
 SELECT * FROM merge_audit;
 IF COALESCE(@logical_syllabus_commit,0)=1 THEN COMMIT; SELECT 'COMMITTED' AS result;
 ELSE ROLLBACK; SELECT 'REHEARSAL: all invariants passed; rolled back' AS result; END IF;
 DROP TEMPORARY TABLE preserved_syllabus;
 DROP TEMPORARY TABLE merge_chain;
 DROP TEMPORARY TABLE merge_audit;
END$$
DELIMITER ;
CALL consolidate_logical_syllabuses_20260910();
DROP PROCEDURE consolidate_logical_syllabuses_20260910;
