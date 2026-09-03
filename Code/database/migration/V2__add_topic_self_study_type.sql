-- Keep the database enum aligned with the canonical Syllabus Form.
-- Existing topic values and the LECTURE default are preserved.
ALTER TABLE topic
    MODIFY COLUMN topic_type ENUM(
        'LECTURE',
        'LAB',
        'SEMINAR',
        'EXAM',
        'PROJECT',
        'SELF_STUDY'
    ) NOT NULL DEFAULT 'LECTURE';
