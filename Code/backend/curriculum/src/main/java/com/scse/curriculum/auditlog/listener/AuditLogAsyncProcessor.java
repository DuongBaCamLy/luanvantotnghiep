package com.scse.curriculum.auditlog.listener;
import com.scse.curriculum.user.entity.UserRole;
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

    @Value("${app.audit.fallback-username:admin}")
private String fallbackUsername;

private volatile Integer cachedFallbackUserId;

    @Async
    @EventListener
    public void handleAuditLogEvent(AuditLogEvent event) {
        try {
            Integer changedById = resolveActorId(event.getChangedByUsername());
            if (changedById == null) {
    log.error(
            "Unable to resolve an audit actor "
                    + "(table={}, record={}). "
                    + "The configured fallback Administrator '{}' "
                    + "is missing, inactive, or invalid. "
                    + "The audit event will not be written.",
            event.getTableName(),
            event.getRecordId(),
            fallbackUsername);

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

    private Integer resolveActorId(String username) {

    if (username != null
            && !username.isBlank()) {

        return userAccountRepository
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(
                        username,
                        username)
                .map(UserAccount::getId)
                .orElseGet(() -> {

                    log.warn(
                            "Audit actor '{}' was not found. "
                                    + "Falling back to Administrator '{}'.",
                            username,
                            fallbackUsername);

                    return getFallbackAdminId();
                });
    }

    /*
     * SRS model:
     * background/system jobs are attributed to the single
     * Administrator account because SYSTEM is not a business actor.
     */
    return getFallbackAdminId();
}

private Integer getFallbackAdminId() {

    Integer id =
            cachedFallbackUserId;

    if (id != null) {
        return id;
    }

    id = userAccountRepository
            .findByUsernameIgnoreCaseOrEmailIgnoreCase(
                    fallbackUsername,
                    fallbackUsername)
            .filter(user ->
                    user.getRole()
                            == UserRole.ADMIN)
            .filter(user ->
                    Boolean.TRUE.equals(
                            user.getIsActive()))
            .map(UserAccount::getId)
            .orElse(null);

    if (id == null) {

        log.error(
                "Fallback Administrator '{}' was not found "
                        + "or is inactive. Audit event will not be written.",
                fallbackUsername);

        return null;
    }

    cachedFallbackUserId = id;

    return id;
}

}
