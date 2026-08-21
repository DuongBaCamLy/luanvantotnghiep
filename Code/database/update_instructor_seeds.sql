USE curriculum_iu;

-- Temporarily disable foreign key checks to allow truncating tables with constraints
SET FOREIGN_KEY_CHECKS = 0;

-- 1. Insert SCSE sub-departments (major-equivalents under School of CS & Eng)
INSERT INTO department (id, code, name, name_vn, is_active) VALUES
(7, 'CS', 'Computer Science Department', 'Khoa học máy tính', TRUE),
(8, 'IT', 'Information Technology Department', 'Công nghệ thông tin', TRUE),
(9, 'DS', 'Data Science Department', 'Khoa học dữ liệu', TRUE),
(10, 'CE', 'Computer Engineering Department', 'Kỹ thuật máy tính', TRUE)
ON DUPLICATE KEY UPDATE name_vn = VALUES(name_vn);

-- 2. Clear instructor table to avoid conflicts
UPDATE user_account SET instructor_id = NULL;
DELETE FROM instructor;

-- 3. Seed 13 instructors matching the screenshot
INSERT INTO instructor (id, staff_code, full_name, email, degree, academic_rank, department_id, is_active) VALUES
(1, 'GV001', 'Nguyễn Văn An', 'nvan@hcmiu.edu.vn', 'PhD', 'Associate Professor', 7, TRUE),
(2, 'GV002', 'Trần Thị Bình', 'ttbinh@hcmiu.edu.vn', 'PhD', 'Lecturer', 7, TRUE),
(3, 'GV003', 'Lê Quốc Cường', 'lqcuong@hcmiu.edu.vn', 'PhD', 'Lecturer', 8, TRUE),
(4, 'GV004', 'Phạm Minh Dũng', 'pmdung@hcmiu.edu.vn', 'PhD', 'Lecturer', 9, TRUE),
(5, 'GV005', 'Hoàng Thị Mai', 'htmai@hcmiu.edu.vn', 'PhD', 'Lecturer', 7, TRUE),
(6, 'GV006', 'Đỗ Hữu Phong', 'dhphong@hcmiu.edu.vn', 'MSc', 'Lecturer', 8, TRUE),
(7, 'GV007', 'Vũ Khánh Linh', 'vklinh@hcmiu.edu.vn', 'PhD', 'Lecturer', 9, TRUE),
(8, 'GV008', 'Bùi Quang Huy', 'bqhuy@hcmiu.edu.vn', 'MSc', 'Lecturer', 8, TRUE),
(9, 'GV009', 'Nguyễn Thị Hà', 'ntha@hcmiu.edu.vn', 'PhD', 'Lecturer', 10, TRUE),
(10, 'GV010', 'Phạm Văn Hải', 'pvhai@hcmiu.edu.vn', 'MSc', 'Lecturer', 10, TRUE),
(11, 'GV011', 'Đặng Minh Hùng', 'dmhung@hcmiu.edu.vn', 'MSc', 'Lecturer', 7, TRUE),
(12, 'GV012', 'Lê Thị Hương', 'lthuong@hcmiu.edu.vn', 'MSc', 'Lecturer', 9, TRUE),
(13, 'GV013', 'Trần Văn Khang', 'tvkhang@hcmiu.edu.vn', 'PhD', 'Lecturer', 8, TRUE);

-- 4. Associate existing users with the new instructors
-- dean.scse -> GV001 (Dean Nguyễn Văn An)
-- depthead1 -> GV002 (Dept Head Trần Thị Bình)
-- instructor1 -> GV005 (Giảng viên Hoàng Thị Mai)
UPDATE user_account SET instructor_id = 1 WHERE username = 'dean.scse';
UPDATE user_account SET instructor_id = 2 WHERE username = 'depthead1';
UPDATE user_account SET instructor_id = 5 WHERE username = 'instructor1';

-- 5. Delete other instructor user_accounts (except admin and core accounts) to prevent duplicates, then insert them
DELETE FROM user_account WHERE username IN ('lqcuong', 'pmdung', 'dhphong', 'vklinh', 'bqhuy', 'ntha', 'pvhai', 'dmhung', 'lthuong', 'tvkhang');

-- 6. Insert new user accounts for the other 10 instructors
-- Default password hash: BCrypt of 'admin123'
INSERT INTO user_account (username, email, password_hash, role, instructor_id, is_active) VALUES
('lqcuong', 'lqcuong@hcmiu.edu.vn', '$2a$10$j39d0N4D13c.r/oWw9oG6uG0uI7Hl5zC2nKsnJc/sW/K4eU680/1a', 'DEPT_HEAD', 3, TRUE),
('pmdung', 'pmdung@hcmiu.edu.vn', '$2a$10$j39d0N4D13c.r/oWw9oG6uG0uI7Hl5zC2nKsnJc/sW/K4eU680/1a', 'DEPT_HEAD', 4, TRUE),
('dhphong', 'dhphong@hcmiu.edu.vn', '$2a$10$j39d0N4D13c.r/oWw9oG6uG0uI7Hl5zC2nKsnJc/sW/K4eU680/1a', 'INSTRUCTOR', 6, TRUE),
('vklinh', 'vklinh@hcmiu.edu.vn', '$2a$10$j39d0N4D13c.r/oWw9oG6uG0uI7Hl5zC2nKsnJc/sW/K4eU680/1a', 'INSTRUCTOR', 7, TRUE),
('bqhuy', 'bqhuy@hcmiu.edu.vn', '$2a$10$j39d0N4D13c.r/oWw9oG6uG0uI7Hl5zC2nKsnJc/sW/K4eU680/1a', 'INSTRUCTOR', 8, TRUE),
('ntha', 'ntha@hcmiu.edu.vn', '$2a$10$j39d0N4D13c.r/oWw9oG6uG0uI7Hl5zC2nKsnJc/sW/K4eU680/1a', 'DEPT_HEAD', 9, TRUE),
('pvhai', 'pvhai@hcmiu.edu.vn', '$2a$10$j39d0N4D13c.r/oWw9oG6uG0uI7Hl5zC2nKsnJc/sW/K4eU680/1a', 'INSTRUCTOR', 10, TRUE),
('dmhung', 'dmhung@hcmiu.edu.vn', '$2a$10$j39d0N4D13c.r/oWw9oG6uG0uI7Hl5zC2nKsnJc/sW/K4eU680/1a', 'INSTRUCTOR', 11, TRUE),
('lthuong', 'lthuong@hcmiu.edu.vn', '$2a$10$j39d0N4D13c.r/oWw9oG6uG0uI7Hl5zC2nKsnJc/sW/K4eU680/1a', 'INSTRUCTOR', 12, TRUE),
('tvkhang', 'tvkhang@hcmiu.edu.vn', '$2a$10$j39d0N4D13c.r/oWw9oG6uG0uI7Hl5zC2nKsnJc/sW/K4eU680/1a', 'INSTRUCTOR', 13, TRUE);

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;
