CREATE TABLE IF NOT EXISTS source_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    file_size BIGINT NOT NULL,
    sha256 CHAR(64) NULL,
    uploaded_at DATETIME(6) NOT NULL,
    uploaded_by INT NOT NULL,
    program_id INT NOT NULL,
    cohort_id INT NOT NULL,
    content LONGBLOB NULL,
    CONSTRAINT fk_source_document_user FOREIGN KEY (uploaded_by) REFERENCES user_account(id),
    CONSTRAINT fk_source_document_program FOREIGN KEY (program_id) REFERENCES program(id),
    CONSTRAINT fk_source_document_cohort FOREIGN KEY (cohort_id) REFERENCES cohort(id),
    INDEX idx_source_document_scope (program_id, cohort_id),
    INDEX idx_source_document_sha256 (sha256)
);

CREATE TABLE IF NOT EXISTS syllabus_source_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_document_id BIGINT NOT NULL,
    syllabus_id INT NULL UNIQUE,
    course_code VARCHAR(50),
    start_boundary INT NOT NULL,
    end_boundary INT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    field_map LONGTEXT NULL,
    content LONGBLOB NOT NULL,
    CONSTRAINT fk_snapshot_document FOREIGN KEY (source_document_id) REFERENCES source_document(id),
    CONSTRAINT fk_snapshot_syllabus FOREIGN KEY (syllabus_id) REFERENCES syllabus(id),
    INDEX idx_snapshot_document (source_document_id),
    INDEX idx_snapshot_course_code (course_code)
);
