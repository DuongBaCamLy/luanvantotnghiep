package com.scse.curriculum.auditlog.listener;

import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.user.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FR-01.7: kiem tra AuditLogAsyncProcessor ghi dung actor.
 *
 * Bug cu: khi changedById (luon null do bug o listener) -> fallback CUNG ve 1 (Admin).
 * => moi thao tac cua moi nguoi dung deu bi ghi nham thanh do Admin thuc hien.
 *
 * Sau khi fix: actor duoc tra dung theo username; chi khi thuc su khong co actor
 * (job he thong) moi dung tai khoan SYSTEM rieng - khong con mao danh Admin.
 */
@ExtendWith(MockitoExtension.class)
class AuditLogAsyncProcessorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private AuditLogAsyncProcessor processor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(processor, "systemUsername", "system");
    }

    @Test
    void handleAuditLogEvent_shouldResolveRealActor_byUsername() {
        UserAccount instructor = UserAccount.builder().id(42).username("nvminh").role(UserRole.INSTRUCTOR).build();
        when(userAccountRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("nvminh", "nvminh"))
                .thenReturn(Optional.of(instructor));

        AuditLogEvent event = new AuditLogEvent(this, "syllabus", 10, "UPDATE",
                "{}", "{}", "nvminh", LocalDateTime.now(), "127.0.0.1", "JUnit");

        processor.handleAuditLogEvent(event);

        // FR-01.7: actor ghi vao DB phai la 42 (nvminh), TUYET DOI khong duoc la 1 (Admin cu)
        verify(jdbcTemplate).update(any(String.class),
                eq("syllabus"), eq(10), eq("UPDATE"), eq("{}"), eq("{}"),
                eq(42), any(LocalDateTime.class), eq("127.0.0.1"), eq("JUnit"));
    }

    @Test
    void handleAuditLogEvent_shouldFallbackToSystemAccount_whenNoActor_notAdmin() {
        UserAccount systemUser = UserAccount.builder().id(999).username("system").role(UserRole.ADMIN).isActive(false).build();
        when(userAccountRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("system", "system"))
                .thenReturn(Optional.of(systemUser));

        AuditLogEvent event = new AuditLogEvent(this, "deadline", 1, "STATUS_CHANGE",
                null, "{}", null, LocalDateTime.now(), "System", "System");

        processor.handleAuditLogEvent(event);

        // Khong con fallback cung ve id=1 (Admin) nhu code cu; phai la tai khoan SYSTEM rieng (999)
        verify(jdbcTemplate).update(any(String.class),
                eq("deadline"), eq(1), eq("STATUS_CHANGE"), isNull(), eq("{}"),
                eq(999), any(LocalDateTime.class), eq("System"), eq("System"));
    }

    @Test
    void handleAuditLogEvent_shouldSkipInsert_whenNoActorAndNoSystemAccountConfigured() {
        when(userAccountRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("system", "system"))
                .thenReturn(Optional.empty());

        AuditLogEvent event = new AuditLogEvent(this, "deadline", 1, "STATUS_CHANGE",
                null, "{}", null, LocalDateTime.now(), "System", "System");

        processor.handleAuditLogEvent(event);

        // Khong ghi lieu voi actor sai/gia mao; bo qua va log loi (xem AuditLogAsyncProcessor)
        verify(jdbcTemplate, never()).update(any(String.class), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void handleAuditLogEvent_shouldCacheSystemUserId_acrossMultipleCalls() {
        UserAccount systemUser = UserAccount.builder().id(999).username("system").role(UserRole.ADMIN).build();
        when(userAccountRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("system", "system"))
                .thenReturn(Optional.of(systemUser));

        AuditLogEvent event1 = new AuditLogEvent(this, "a", 1, "CREATE", null, "{}", null, LocalDateTime.now(), "s", "s");
        AuditLogEvent event2 = new AuditLogEvent(this, "b", 2, "CREATE", null, "{}", null, LocalDateTime.now(), "s", "s");

        processor.handleAuditLogEvent(event1);
        processor.handleAuditLogEvent(event2);

        // Chi tra DB 1 lan cho system user id, lan sau dung cache
        verify(userAccountRepository, times(1)).findByUsernameIgnoreCaseOrEmailIgnoreCase("system", "system");
    }
}
