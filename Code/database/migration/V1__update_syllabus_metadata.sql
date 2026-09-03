-- =====================================================
-- PHASE 1
-- Syllabus Metadata Enhancement
-- =====================================================


ALTER TABLE syllabus

ADD COLUMN course_code_snapshot VARCHAR(100),

ADD COLUMN course_name_snapshot VARCHAR(255),

ADD COLUMN program VARCHAR(255),

ADD COLUMN source_type VARCHAR(50),

ADD COLUMN original_file_name VARCHAR(255),

ADD COLUMN original_file_type VARCHAR(50),

ADD COLUMN import_status VARCHAR(50) DEFAULT 'NONE',

ADD COLUMN final_approval_date TIMESTAMP;


-- =====================================================
-- Index phục vụ Admin List API
-- =====================================================


CREATE INDEX idx_syllabus_course_code_snapshot
ON syllabus(course_code_snapshot);


CREATE INDEX idx_syllabus_program
ON syllabus(program);


CREATE INDEX idx_syllabus_semester
ON syllabus(semester);


CREATE INDEX idx_syllabus_status
ON syllabus(status);


CREATE INDEX idx_syllabus_created_by
ON syllabus(created_by);


-- =====================================================
-- Backfill dữ liệu hiện tại
-- =====================================================


UPDATE syllabus s

INNER JOIN course c
ON s.course_id = c.id

SET

s.course_code_snapshot = c.code,

s.course_name_snapshot = c.name

WHERE

s.course_code_snapshot IS NULL;


-- =====================================================
-- Đồng bộ approval date
-- =====================================================


UPDATE syllabus

SET final_approval_date = approved_at

WHERE approved_at IS NOT NULL;


-- =====================================================
-- Default source
-- =====================================================


UPDATE syllabus

SET source_type = 'MANUAL'

WHERE source_type IS NULL;


UPDATE syllabus

SET import_status = 'NONE'

WHERE import_status IS NULL;