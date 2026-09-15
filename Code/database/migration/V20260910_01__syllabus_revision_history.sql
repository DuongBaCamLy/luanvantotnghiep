-- Schema phase: MySQL DDL commits independently of the subsequent rehearsal/cleanup.
-- Review ONE_SYLLABUS_PREMIGRATION_AUDIT_20260910.md first. No syllabus content/status changes.
DELIMITER $$
CREATE PROCEDURE prepare_syllabus_revision_history_20260910()
BEGIN
 IF DATABASE() <> 'curriculum_iu' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Wrong database'; END IF;
 IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='approval_request' AND COLUMN_NAME='syllabus_version_number') THEN
  ALTER TABLE approval_request ADD COLUMN syllabus_version_number INT NULL;
  UPDATE approval_request a JOIN syllabus s ON s.id=a.syllabus_id SET a.syllabus_version_number=s.version_number;
  ALTER TABLE approval_request MODIFY syllabus_version_number INT NOT NULL;
 END IF;
 IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='syllabus' AND COLUMN_NAME='lock_version') THEN
  ALTER TABLE syllabus ADD COLUMN lock_version BIGINT NOT NULL DEFAULT 0;
 END IF;
END$$
DELIMITER ;
CALL prepare_syllabus_revision_history_20260910();
DROP PROCEDURE prepare_syllabus_revision_history_20260910;

CREATE TABLE IF NOT EXISTS syllabus_revision_snapshot (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 syllabus_id INT NOT NULL,
 original_syllabus_id INT NOT NULL,
 version_number INT NOT NULL,
 version_label VARCHAR(255) NOT NULL,
 event_type VARCHAR(255) NOT NULL,
 captured_at DATETIME(6) NOT NULL,
 actor VARCHAR(255) NULL,
 content LONGTEXT NOT NULL,
 UNIQUE KEY uq_syllabus_revision_event (syllabus_id,version_number,event_type),
 CONSTRAINT fk_revision_syllabus FOREIGN KEY (syllabus_id) REFERENCES syllabus(id),
 CONSTRAINT chk_revision_content CHECK (JSON_VALID(content))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELIMITER $$
CREATE TRIGGER syllabus_revision_no_update BEFORE UPDATE ON syllabus_revision_snapshot
FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Syllabus revision history is immutable'; END$$
CREATE TRIGGER syllabus_revision_no_delete BEFORE DELETE ON syllabus_revision_snapshot
FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Syllabus revision history is immutable'; END$$
DELIMITER ;
