SET NAMES utf8mb4;
USE curriculum_iu;

START TRANSACTION;

-- ============================================================
-- SRS COURSE GROUP TAXONOMY
-- FR-02.3 / FR-06.5
--
-- 1. COMPULSORY
-- 2. ELECTIVE
-- 3. GENERAL
-- ============================================================


-- ------------------------------------------------------------
-- 1. Tạo COMPULSORY nếu chưa có
-- ------------------------------------------------------------

INSERT INTO course_type (
    code,
    name,
    name_vn
)
SELECT
    'COMPULSORY',
    'Compulsory',
    'Môn bắt buộc'
WHERE NOT EXISTS (
    SELECT 1
    FROM course_type
    WHERE code = 'COMPULSORY'
);


-- ------------------------------------------------------------
-- 2. Chuẩn hóa GENERAL
-- ------------------------------------------------------------

UPDATE course_type
SET
    name = 'General',
    name_vn = 'Giáo dục đại cương'
WHERE code = 'GENERAL';


-- ------------------------------------------------------------
-- 3. Chuẩn hóa ELECTIVE
-- ------------------------------------------------------------

UPDATE course_type
SET
    name = 'Elective',
    name_vn = 'Môn tự chọn'
WHERE code = 'ELECTIVE';


-- ------------------------------------------------------------
-- 4. Chuẩn hóa COMPULSORY
-- ------------------------------------------------------------

UPDATE course_type
SET
    name = 'Compulsory',
    name_vn = 'Môn bắt buộc'
WHERE code = 'COMPULSORY';


-- ------------------------------------------------------------
-- 5. Gom FOUNDATION / CORE / THESIS / INTERNSHIP
--    thành COMPULSORY
-- ------------------------------------------------------------

SET @compulsory_id = (
    SELECT id
    FROM course_type
    WHERE code = 'COMPULSORY'
    LIMIT 1
);

UPDATE course_program cp
JOIN course_type ct
    ON ct.id = cp.course_type_id
SET cp.course_type_id = @compulsory_id
WHERE ct.code IN (
    'FOUNDATION',
    'CORE',
    'THESIS',
    'INTERNSHIP'
);


-- ------------------------------------------------------------
-- 6. Xóa taxonomy cũ không còn thuộc SRS
-- ------------------------------------------------------------

DELETE FROM course_type
WHERE code IN (
    'FOUNDATION',
    'CORE',
    'THESIS',
    'INTERNSHIP'
);


COMMIT;