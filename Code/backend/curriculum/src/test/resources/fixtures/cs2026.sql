-- Minimal fixed regression fixture, never a copy of the live database.
-- Idempotent within a test context; Hibernate recreates the dedicated test schema.
INSERT IGNORE INTO department (id, code, name) VALUES (1, 'SCSE', 'SCSE test fixture');
INSERT IGNORE INTO program (id, code, name, department_id, is_active)
VALUES (1, 'CS', 'CS test fixture', 1, true);
INSERT IGNORE INTO cohort (id, program_id, entry_year, name, is_active)
VALUES (12, 1, 2026, 'CS2026', true);
INSERT IGNORE INTO course (id, course_code, name, name_vn, department_id, is_active)
VALUES (64, 'IT064IU', 'IT064 regression fixture', 'IT064 fixture', 1, true),
       (89, 'IT089IU', 'IT089 regression fixture', 'IT089 fixture', 1, true);
INSERT IGNORE INTO syllabus (id, course_id, status, is_current, version_number)
VALUES (64, 64, 'APPROVED', true, 1), (89, 89, 'APPROVED', true, 1);
INSERT IGNORE INTO course_program (id, course_id, program_id, cohort_id, syllabus_id)
VALUES (64, 64, 1, 12, 64), (89, 89, 1, 12, 89);
INSERT IGNORE INTO plo (id, program_id, code, description, is_active)
VALUES (1, 1, 'PLO1', 'PLO1 fixture', true), (2, 1, 'PLO2', 'PLO2 fixture', true),
       (3, 1, 'PLO3', 'PLO3 fixture', true), (5, 1, 'PLO5', 'PLO5 fixture', true);
INSERT IGNORE INTO clo (id, syllabus_id, code, description, order_index)
VALUES (641, 64, 'CLO1', 'CLO1 fixture', 1), (642, 64, 'CLO2', 'CLO2 fixture', 2),
       (643, 64, 'CLO3', 'CLO3 fixture', 3), (644, 64, 'CLO4', 'CLO4 fixture', 4),
       (645, 64, 'CLO5', 'CLO5 fixture', 5),
       (891, 89, 'CLO1', 'CLO1 fixture', 1), (892, 89, 'CLO2', 'CLO2 fixture', 2),
       (893, 89, 'CLO3', 'CLO3 fixture', 3), (894, 89, 'CLO4', 'CLO4 fixture', 4),
       (895, 89, 'CLO5', 'CLO5 fixture', 5);
INSERT IGNORE INTO clo_plo_mapping (clo_id, plo_id, level, contribution_weight)
VALUES (641, 1, 'I', 1), (641, 3, 'I', 1), (642, 1, 'I', 1), (642, 3, 'I', 1),
       (643, 1, 'I', 1), (644, 1, 'I', 1), (645, 5, 'I', 1),
       (891, 1, 'I', 1), (892, 1, 'I', 1), (893, 2, 'I', 1), (893, 5, 'I', 1),
       (894, 1, 'I', 1), (895, 1, 'I', 1);
