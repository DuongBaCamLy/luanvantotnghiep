-- FR-03.1 / FR-05.6 compatibility
-- Cho phép phân công giảng dạy tồn tại trước khi Faculty tạo syllabus Draft.

ALTER TABLE class_section
    MODIFY COLUMN syllabus_id INT NULL
    COMMENT 'Đề cương sử dụng học kỳ này; có thể NULL trước khi Faculty tạo Draft';
