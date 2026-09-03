-- Existing assignments remain nullable so an Administrator can repair their
-- curriculum context through Edit Teaching Assignment. All new/updated rows
-- are required by API validation to supply both IDs.
ALTER TABLE class_section
  ADD COLUMN program_id INT NULL AFTER course_id,
  ADD COLUMN cohort_id INT NULL AFTER program_id,
  ADD INDEX idx_cs_program (program_id),
  ADD INDEX idx_cs_cohort (cohort_id),
  ADD CONSTRAINT fk_cs_program FOREIGN KEY (program_id) REFERENCES program(id),
  ADD CONSTRAINT fk_cs_cohort FOREIGN KEY (cohort_id) REFERENCES cohort(id);
