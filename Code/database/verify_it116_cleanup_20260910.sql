SELECT s.id,s.version_number,s.version_label,s.academic_year,s.semester,s.status FROM syllabus s JOIN course c ON c.id=s.course_id WHERE c.course_code='IT116IU';
SELECT cp.id,h.name,cp.syllabus_id FROM course_program cp JOIN cohort h ON h.id=cp.cohort_id WHERE cp.id IN (196,246);
SELECT id,syllabus_id FROM class_section WHERE id=11;
SELECT 'approval_request' AS child_table,COUNT(*) AS invalid_references FROM approval_request WHERE syllabus_id IN (3048,3049,3050)
UNION ALL
SELECT 'assessment_component' AS child_table,COUNT(*) AS invalid_references FROM assessment_component WHERE syllabus_id IN (3048,3049,3050)
UNION ALL
SELECT 'class_section' AS child_table,COUNT(*) AS invalid_references FROM class_section WHERE syllabus_id IN (3048,3049,3050)
UNION ALL
SELECT 'clo' AS child_table,COUNT(*) AS invalid_references FROM clo WHERE syllabus_id IN (3048,3049,3050)
UNION ALL
SELECT 'course_program' AS child_table,COUNT(*) AS invalid_references FROM course_program WHERE syllabus_id IN (3048,3049,3050)
UNION ALL
SELECT 'syllabus_book' AS child_table,COUNT(*) AS invalid_references FROM syllabus_book WHERE syllabus_id IN (3048,3049,3050)
UNION ALL
SELECT 'syllabus_import_history' AS child_table,COUNT(*) AS invalid_references FROM syllabus_import_history WHERE syllabus_id IN (3048,3049,3050)
UNION ALL
SELECT 'syllabus_source_snapshot' AS child_table,COUNT(*) AS invalid_references FROM syllabus_source_snapshot WHERE syllabus_id IN (3048,3049,3050)
UNION ALL
SELECT 'topic' AS child_table,COUNT(*) AS invalid_references FROM topic WHERE syllabus_id IN (3048,3049,3050);
