SET NAMES utf8mb4;

USE curriculum_iu;

START TRANSACTION;

-- ============================================================
-- 1. INSTRUCTORS
-- Chỉ sửa tên hiển thị.
-- KHÔNG thay đổi department_id, email, role hoặc tài khoản.
-- ============================================================

UPDATE instructor SET full_name = 'Nguyễn Văn An'
WHERE staff_code = 'GV001';

UPDATE instructor SET full_name = 'Trần Thị Bình'
WHERE staff_code = 'GV002';

UPDATE instructor SET full_name = 'Lê Quốc Cường'
WHERE staff_code = 'GV003';

UPDATE instructor SET full_name = 'Phạm Minh Dũng'
WHERE staff_code = 'GV004';

UPDATE instructor SET full_name = 'Hoàng Thị Mai'
WHERE staff_code = 'GV005';

UPDATE instructor SET full_name = 'Đỗ Hữu Phong'
WHERE staff_code = 'GV006';

UPDATE instructor SET full_name = 'Vũ Khánh Linh'
WHERE staff_code = 'GV007';

UPDATE instructor SET full_name = 'Bùi Quang Huy'
WHERE staff_code = 'GV008';

UPDATE instructor SET full_name = 'Nguyễn Thị Hà'
WHERE staff_code = 'GV009';

UPDATE instructor SET full_name = 'Phạm Văn Hải'
WHERE staff_code = 'GV010';

UPDATE instructor SET full_name = 'Đặng Minh Hùng'
WHERE staff_code = 'GV011';

UPDATE instructor SET full_name = 'Lê Thị Hương'
WHERE staff_code = 'GV012';

UPDATE instructor SET full_name = 'Trần Văn Khang'
WHERE staff_code = 'GV013';


-- ============================================================
-- 2. DEPARTMENTS
-- ============================================================

UPDATE department
SET name_vn = 'Khoa Khoa học và Kỹ thuật Máy tính'
WHERE code = 'SCSE';

UPDATE department
SET name_vn = 'Bộ môn Toán'
WHERE code = 'MATH';

UPDATE department
SET name_vn = 'Bộ môn Vật lý'
WHERE code = 'PHYS';

UPDATE department
SET name_vn = 'Bộ môn Hóa học'
WHERE code = 'CHEM';

UPDATE department
SET name_vn = 'Trung tâm Ngoại ngữ'
WHERE code = 'LANG';

UPDATE department
SET name_vn = 'Khoa Chính trị - Hành chính'
WHERE code = 'PE';

UPDATE department
SET name_vn = 'Khoa học máy tính'
WHERE code = 'TEST';

UPDATE department
SET name_vn = 'Công nghệ thông tin'
WHERE code = 'IT';

UPDATE department
SET name_vn = 'Khoa học dữ liệu'
WHERE code = 'DS';

UPDATE department
SET name_vn = 'Kỹ thuật máy tính'
WHERE code = 'CE';


-- ============================================================
-- 3. PROGRAMS
-- ============================================================

UPDATE program
SET name_vn = 'Cử nhân Khoa học Máy tính'
WHERE code = 'CS-2021';

UPDATE program
SET name_vn = 'Kỹ sư Công nghệ Thông tin'
WHERE code = 'IT-2021';

UPDATE program
SET name_vn = 'Cử nhân Khoa học Dữ liệu'
WHERE code = 'DS-2021';

UPDATE program
SET name_vn = 'Kỹ sư Kỹ thuật Phần mềm'
WHERE code = 'SE-2025';


-- ============================================================
-- 4. COURSE TYPES
-- ============================================================

UPDATE course_type
SET name_vn = 'Giáo dục đại cương'
WHERE code = 'GENERAL';

UPDATE course_type
SET name_vn = 'Cơ sở ngành'
WHERE code = 'FOUNDATION';

UPDATE course_type
SET name_vn = 'Chuyên ngành bắt buộc'
WHERE code = 'CORE';

UPDATE course_type
SET name_vn = 'Chuyên ngành tự chọn'
WHERE code = 'ELECTIVE';

UPDATE course_type
SET name_vn = 'Luận văn/Đồ án tốt nghiệp'
WHERE code = 'THESIS';

UPDATE course_type
SET name_vn = 'Thực tập'
WHERE code = 'INTERNSHIP';


-- ============================================================
-- 5. MAJORS
-- ============================================================

UPDATE major
SET name_vn = 'Khoa học Máy tính'
WHERE code = 'CS';

UPDATE major
SET name_vn = 'Công nghệ Thông tin'
WHERE code = 'IT';

UPDATE major
SET name_vn = 'Khoa học Dữ liệu'
WHERE code = 'DS';

UPDATE major
SET name_vn = 'Kỹ thuật Máy tính'
WHERE code = 'CE';

UPDATE major
SET name_vn = 'Kỹ thuật Phần mềm'
WHERE code = 'SE';


COMMIT;