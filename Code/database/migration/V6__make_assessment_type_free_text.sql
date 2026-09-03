ALTER TABLE assessment_component
    MODIFY COLUMN assessment_type VARCHAR(100) NOT NULL
    COMMENT 'Loại hình đánh giá nhập theo syllabus nguồn';
