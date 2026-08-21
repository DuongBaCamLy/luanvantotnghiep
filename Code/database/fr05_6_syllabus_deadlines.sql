-- ============================================================
-- FR-05.6 - Deadline theo học kỳ và nhắc trước hạn
-- MySQL 8.x
-- Có thể chạy nhiều lần an toàn nhờ CREATE TABLE IF NOT EXISTS.
-- ============================================================

CREATE TABLE IF NOT EXISTS syllabus_deadline (
    id BIGINT NOT NULL AUTO_INCREMENT,
    academic_year VARCHAR(50) NOT NULL,
    semester INT NOT NULL,
    deadline_at DATETIME(6) NOT NULL,
    reminder_days VARCHAR(255) NOT NULL,
    escalation_days VARCHAR(255) NOT NULL DEFAULT '0,1,3,7,14',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    revision INT NOT NULL DEFAULT 1,
    created_by INT NULL,
    updated_by INT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_syllabus_deadline_term
        UNIQUE (academic_year, semester),
    CONSTRAINT fk_syllabus_deadline_created_by
        FOREIGN KEY (created_by) REFERENCES user_account(id)
        ON UPDATE RESTRICT ON DELETE SET NULL,
    CONSTRAINT fk_syllabus_deadline_updated_by
        FOREIGN KEY (updated_by) REFERENCES user_account(id)
        ON UPDATE RESTRICT ON DELETE SET NULL,
    CONSTRAINT chk_syllabus_deadline_semester
        CHECK (semester BETWEEN 1 AND 8),
    CONSTRAINT chk_syllabus_deadline_revision
        CHECK (revision >= 1),
    INDEX idx_syllabus_deadline_active_time (is_active, deadline_at),
    INDEX idx_syllabus_deadline_term (academic_year, semester)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS syllabus_deadline_reminder_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    deadline_id BIGINT NOT NULL,
    deadline_revision INT NOT NULL,
    recipient_user_id INT NOT NULL,
    days_before INT NOT NULL,
    missing_course_count INT NOT NULL,
    missing_course_codes TEXT NOT NULL,
    notification_id INT NULL,
    email_queued BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_deadline_reminder_delivery
        UNIQUE (
            deadline_id,
            deadline_revision,
            recipient_user_id,
            days_before
        ),
    CONSTRAINT fk_deadline_reminder_deadline
        FOREIGN KEY (deadline_id) REFERENCES syllabus_deadline(id)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fk_deadline_reminder_recipient
        FOREIGN KEY (recipient_user_id) REFERENCES user_account(id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_deadline_reminder_notification
        FOREIGN KEY (notification_id) REFERENCES notification(id)
        ON UPDATE RESTRICT ON DELETE SET NULL,
    -- -1 là lượt gửi thử thủ công; 0..60 là mốc scheduler.
    CONSTRAINT chk_deadline_reminder_days_before
        CHECK (days_before BETWEEN -1 AND 60),
    CONSTRAINT chk_deadline_reminder_missing_count
        CHECK (missing_course_count >= 0),
    INDEX idx_deadline_reminder_log_deadline (deadline_id, created_at),
    INDEX idx_deadline_reminder_log_recipient (recipient_user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Cấu hình SYLLABUS_DEADLINE cũ trong system_config không thể tự động migrate,
-- vì dữ liệu cũ không có năm học và học kỳ. Sau khi chạy migration, Admin tạo
-- cấu hình mới tại trang /admin/settings rồi có thể xóa key cũ nếu không dùng.
