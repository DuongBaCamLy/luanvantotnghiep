package com.scse.curriculum.auditlog.listener;

import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * FR-01.7: Xử lý AuditLogEvent bất đồng bộ và ghi (INSERT thô, KHÔNG update/delete)
 * vào bảng audit_log.
 *
 * Chạy trên một thread riêng (@Async) sau khi transaction gốc đã hoàn tất việc flush
 * của Hibernate, nên có thể an toàn dùng UserAccountRepository để tra username -> id
 * mà không xung đột với Session đang được Hibernate quản lý ở request thread.
 *
 * FR-01.7 fix: trước đây khi không xác định được actor, code fallback CỨNG về
 * changedById = 1 (Admin) — khiến MỌI thao tác do lỗi ở listener (actor luôn null)
 * đều bị ghi nhận SAI thành do Admin thực hiện. Nay actor được tra đúng theo username
 * lấy từ SecurityContext; chỉ khi thực sự không có actor (job hệ thống chạy ngoài
 * request, ví dụ scheduler) mới gán cho một tài khoản SYSTEM chuyên dụng — không còn
 * mạo danh Admin hay bất kỳ người dùng thật nào.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAsyncProcessor {

    private final JdbcTemplate jdbcTemplate;
    private final UserAccountRepository userAccountRepository;

    @Value("${app.audit.system-username:system}")
    private String systemUsername;

    private volatile Integer cachedSystemUserId;

    @Async
    @EventListener
    public void handleAuditLogEvent(AuditLogEvent event) {
        try {
            Integer changedById = resolveActorId(event.getChangedByUsername());
            if (changedById == null) {
                // changed_by là NOT NULL ở DB (xem database/fr01_7_audit_log_immutability.sql).
                // Không có actor thật và cũng không tra được tài khoản SYSTEM dự phòng
                // (rất có thể do chưa chạy migration) -> log rõ để không âm thầm ghi sai actor.
                log.error("Không xác định được actor cho audit log (table={}, record={}). " +
                                "Hãy chắc chắn đã chạy database/fr01_7_audit_log_immutability.sql " +
                                "để tạo tài khoản SYSTEM dự phòng. Bỏ qua bản ghi này để tránh ghi sai actor.",
                        event.getTableName(), event.getRecordId());
                return;
            }

            String sql = "INSERT INTO audit_log (table_name, record_id, action, old_value, new_value, changed_by, changed_at, ip_address, user_agent) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(sql,
                    event.getTableName(),
                    event.getRecordId(),
                    event.getAction(),
                    event.getOldValueJson(),
                    event.getNewValueJson(),
                    changedById,
                    event.getChangedAt(),
                    event.getIpAddress(),
                    event.getUserAgent()
            );
            log.debug("Successfully saved audit log for table: {} record: {} actor: {}",
                    event.getTableName(), event.getRecordId(), changedById);
        } catch (Exception e) {
            log.error("Failed to save audit log for table: {} record: {}", event.getTableName(), event.getRecordId(), e);
        }
    }

    /**
     * Tra username (đăng nhập bằng username hoặc email đều được, giống CurrentUserService)
     * ra user_account.id. Nếu không có username (job hệ thống) hoặc không tìm thấy tài khoản
     * tương ứng, fallback về tài khoản SYSTEM chuyên dụng thay vì mạo danh Admin.
     */
    private Integer resolveActorId(String username) {
        if (username != null && !username.isBlank()) {
            return userAccountRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, username)
                    .map(UserAccount::getId)
                    .orElseGet(() -> {
                        log.warn("Audit actor username '{}' không tồn tại trong user_account, dùng tài khoản SYSTEM dự phòng.", username);
                        return getSystemUserId();
                    });
        }
        return getSystemUserId();
    }

    private Integer getSystemUserId() {
        Integer id = cachedSystemUserId;
        if (id == null) {
            id = userAccountRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(systemUsername, systemUsername)
                    .map(UserAccount::getId)
                    .orElse(null);
            if (id == null) {
                log.error("Không tìm thấy tài khoản SYSTEM ('{}') trong user_account. " +
                        "Hãy chạy database/fr01_7_audit_log_immutability.sql.", systemUsername);
            } else {
                cachedSystemUserId = id;
            }
        }
        return id;
    }
}
