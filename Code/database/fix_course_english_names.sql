SET NAMES utf8mb4;
USE curriculum_iu;

START TRANSACTION;

-- 1. EE117IU
UPDATE course
SET
    name = 'Digital System Design Lab',
    name_vn = 'Thực hành Thiết kế Hệ thống Số'
WHERE course_code = 'EE117IU';

-- 2. EE121IU
UPDATE course
SET
    name = 'Concepts in VLSI Design',
    name_vn = 'Khái niệm Thiết kế VLSI'
WHERE course_code = 'EE121IU';

-- 3. IT076IU
UPDATE course
SET name = 'Software Engineering'
WHERE course_code = 'IT076IU';

-- 4. IT091IU
UPDATE course
SET name = 'Computer Networks'
WHERE course_code = 'IT091IU';

-- 5. IT129IU
UPDATE course
SET name = 'Micro-processing Systems Laboratory'
WHERE course_code = 'IT129IU';

-- 6. IT135IU
UPDATE course
SET name = 'Introduction to Data Science'
WHERE course_code = 'IT135IU';

-- 7. IT161IU
UPDATE course
SET name = 'Big Data Technology'
WHERE course_code = 'IT161IU';

-- 8. IT163IU
UPDATE course
SET name = 'Optimization and Applications'
WHERE course_code = 'IT163IU';

-- 9. PE016IU
UPDATE course
SET name = 'Political economics of Marxism and Leninism'
WHERE course_code = 'PE016IU';

-- 10. PE017IU
UPDATE course
SET name = 'Scientific socialism'
WHERE course_code = 'PE017IU';

COMMIT;