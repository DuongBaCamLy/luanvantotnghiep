-- =============================================================================
-- FR-01.7: Audit trail bat bien + ghi dung nguoi thuc hien
-- Chay 1 lan tren database curriculum_iu (sau khi da backup).
-- =============================================================================

-- 1) Tai khoan SYSTEM du phong
--    Dung khi mot ban ghi audit_log duoc tao ma khong co actor dang nhap thuc su
--    (vi du: job chay theo scheduler nhu FR-05.6/FR-05.7 deadline reminder/escalation).
--    Tai khoan nay khong the dang nhap (is_active = 0, password_hash khong hop le).
--    Xem AuditLogAsyncProcessor.resolveActorId() / getSystemUserId().
INSERT INTO user_account (username, email, password_hash, role, is_active, created_at)
SELECT 'system', 'system@internal.local', '!DISABLED!', 'ADMIN', 0, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM user_account WHERE username = 'system'
);

-- 2) Trigger chan UPDATE/DELETE truc tiep tren audit_log ngay tai tang database.
--    Day la lop bao ve thu hai (defense-in-depth), doc lap voi tang service/controller
--    (da bo update/delete o do). Dam bao ngay ca khi co code/cong cu nao truy cap DB
--    truc tiep (vd: DBA chay SQL tay, script migrate khac) cung khong the sua/xoa log.
DELIMITER $$

DROP TRIGGER IF EXISTS trg_audit_log_no_update $$
CREATE TRIGGER trg_audit_log_no_update
BEFORE UPDATE ON audit_log
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'audit_log la bat bien: khong duoc phep UPDATE.';
END $$

DROP TRIGGER IF EXISTS trg_audit_log_no_delete $$
CREATE TRIGGER trg_audit_log_no_delete
BEFORE DELETE ON audit_log
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'audit_log la bat bien: khong duoc phep DELETE.';
END $$

DELIMITER ;
