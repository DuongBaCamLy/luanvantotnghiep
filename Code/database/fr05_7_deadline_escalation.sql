-- ============================================================================
-- FR-05.7 - Tự động escalation khi quá hạn nộp đề cương
-- MySQL 8.x
--
-- Mục tiêu:
--   1) Bổ sung các mốc escalation sau deadline (CSV, ví dụ 0,1,3,7,14).
--   2) Lưu audit log bất biến cho từng người nhận/phạm vi/mốc/revision.
--   3) Khóa UNIQUE chống gửi trùng khi nhiều scheduler instance chạy đồng thời.
--
-- Script có thể chạy lặp lại an toàn trên cùng database.
-- ============================================================================

SET @current_schema = DATABASE();

-- 1. Bổ sung cấu hình escalation_days cho deadline đã có từ FR-05.6.
SELECT COUNT(*) INTO @has_escalation_days
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @current_schema
  AND TABLE_NAME = 'syllabus_deadline'
  AND COLUMN_NAME = 'escalation_days';

SET @ddl = IF(
    @has_escalation_days = 0,
    'ALTER TABLE syllabus_deadline ADD COLUMN escalation_days VARCHAR(255) NOT NULL DEFAULT ''0,1,3,7,14'' AFTER reminder_days',
    'SELECT ''syllabus_deadline.escalation_days already exists'''
);
PREPARE fr057_stmt FROM @ddl;
EXECUTE fr057_stmt;
DEALLOCATE PREPARE fr057_stmt;

-- Chuẩn hóa dữ liệu cũ/phát sinh trước migration.
UPDATE syllabus_deadline
SET escalation_days = '0,1,3,7,14'
WHERE escalation_days IS NULL OR TRIM(escalation_days) = '';

-- 2. Audit log escalation.
CREATE TABLE IF NOT EXISTS syllabus_deadline_escalation_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    deadline_id BIGINT NOT NULL,
    deadline_revision INT NOT NULL,
    recipient_user_id INT NOT NULL,
    recipient_role VARCHAR(40) NOT NULL,
    scope_key VARCHAR(100) NOT NULL,
    department_id INT NULL,
    department_code VARCHAR(50) NULL,
    department_name VARCHAR(255) NULL,
    -- -1 = gửi thủ công; 0..365 = mốc tự động sau hạn.
    escalation_day INT NOT NULL,
    actual_days_overdue INT NOT NULL,
    overdue_instructor_count INT NOT NULL,
    missing_course_count INT NOT NULL,
    instructor_names TEXT NOT NULL,
    missing_course_codes TEXT NOT NULL,
    notification_id INT NULL,
    email_queued BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_deadline_escalation_delivery
        UNIQUE (
            deadline_id,
            deadline_revision,
            recipient_user_id,
            escalation_day,
            scope_key
        ),
    CONSTRAINT fk_deadline_escalation_deadline
        FOREIGN KEY (deadline_id) REFERENCES syllabus_deadline(id)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fk_deadline_escalation_recipient
        FOREIGN KEY (recipient_user_id) REFERENCES user_account(id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_deadline_escalation_notification
        FOREIGN KEY (notification_id) REFERENCES notification(id)
        ON UPDATE RESTRICT ON DELETE SET NULL,
    CONSTRAINT chk_deadline_escalation_day
        CHECK (escalation_day BETWEEN -1 AND 365),
    CONSTRAINT chk_deadline_escalation_actual_days
        CHECK (actual_days_overdue >= 0),
    CONSTRAINT chk_deadline_escalation_instructor_count
        CHECK (overdue_instructor_count >= 0),
    CONSTRAINT chk_deadline_escalation_course_count
        CHECK (missing_course_count >= 0),

    INDEX idx_deadline_escalation_log_deadline (deadline_id, created_at),
    INDEX idx_deadline_escalation_log_recipient (recipient_user_id, created_at),
    INDEX idx_deadline_escalation_log_scope (scope_key, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Kiểm tra nhanh sau migration.
SELECT
    d.id,
    d.academic_year,
    d.semester,
    d.deadline_at,
    d.reminder_days,
    d.escalation_days,
    d.is_active,
    d.revision
FROM syllabus_deadline d
ORDER BY d.deadline_at DESC;
