-- Phase 1.4: import-resilient text capacity
-- Purpose:
--   Programme dossiers can contain descriptive workload fields and bibliography
--   metadata longer than VARCHAR(255/500). These are imported document text,
--   so persist them as TEXT instead of failing the whole syllabus transaction.
--
-- No rows are deleted. Existing values are preserved.

ALTER TABLE syllabus
    MODIFY COLUMN workload_total   TEXT NULL,
    MODIFY COLUMN workload_contact TEXT NULL,
    MODIFY COLUMN workload_private TEXT NULL;

ALTER TABLE book
    MODIFY COLUMN title     TEXT NOT NULL,
    MODIFY COLUMN author    TEXT NULL,
    MODIFY COLUMN publisher TEXT NULL,
    MODIFY COLUMN url       TEXT NULL;
