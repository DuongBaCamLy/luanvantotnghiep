-- MySQL 8: append the new source without changing existing enum ordinals.
-- Existing nullable/default behavior is preserved. No syllabus rows are changed.
ALTER TABLE curriculum_iu.syllabus
    MODIFY COLUMN source_type
    ENUM('CLONE','IMPORT_DOCX','IMPORT_PDF','MANUAL','IMPORT_XLSX') NULL DEFAULT NULL;
