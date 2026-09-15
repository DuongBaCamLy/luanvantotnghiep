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
 * FR-01.7: verify that AuditLogAsyncProcessor records
 * the correct actor.
 *
 * Authenticated requests must be attributed to the real
 * authenticated user.
 *
 * When an action is generated without an authenticated actor
 * (for example a scheduler/background job), the system falls
 * back to the single active Administrator account defined by
 * the SRS model.
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
        ReflectionTestUtils.setField(
                processor,
                "fallbackUsername",
                "admin");
    }

    @Test
    void handleAuditLogEvent_shouldResolveRealActor_byUsername() {

        UserAccount instructor =
                UserAccount.builder()
                        .id(42)
                        .username("nvminh")
                        .role(UserRole.INSTRUCTOR)
                        .isActive(true)
                        .build();

        when(userAccountRepository
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(
                        "nvminh",
                        "nvminh"))
                .thenReturn(
                        Optional.of(instructor));

        AuditLogEvent event =
                new AuditLogEvent(
                        this,
                        "syllabus",
                        10,
                        "UPDATE",
                        "{}",
                        "{}",
                        "nvminh",
                        LocalDateTime.now(),
                        "127.0.0.1",
                        "JUnit");

        processor.handleAuditLogEvent(event);

        verify(jdbcTemplate)
                .update(
                        any(String.class),
                        eq("syllabus"),
                        eq(10),
                        eq("UPDATE"),
                        eq("{}"),
                        eq("{}"),
                        eq(42),
                        any(LocalDateTime.class),
                        eq("127.0.0.1"),
                        eq("JUnit"));
    }

    @Test
    void handleAuditLogEvent_shouldFallbackToAdmin_whenNoActor() {

        UserAccount admin =
                UserAccount.builder()
                        .id(1)
                        .username("admin")
                        .role(UserRole.ADMIN)
                        .isActive(true)
                        .build();

        when(userAccountRepository
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(
                        "admin",
                        "admin"))
                .thenReturn(
                        Optional.of(admin));

        AuditLogEvent event =
                new AuditLogEvent(
                        this,
                        "deadline",
                        1,
                        "STATUS_CHANGE",
                        null,
                        "{}",
                        null,
                        LocalDateTime.now(),
                        "System",
                        "System");

        processor.handleAuditLogEvent(event);

        verify(jdbcTemplate)
                .update(
                        any(String.class),
                        eq("deadline"),
                        eq(1),
                        eq("STATUS_CHANGE"),
                        isNull(),
                        eq("{}"),
                        eq(1),
                        any(LocalDateTime.class),
                        eq("System"),
                        eq("System"));
    }

    @Test
    void handleAuditLogEvent_shouldSkipInsert_whenFallbackAdminMissing() {

        when(userAccountRepository
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(
                        "admin",
                        "admin"))
                .thenReturn(
                        Optional.empty());

        AuditLogEvent event =
                new AuditLogEvent(
                        this,
                        "deadline",
                        1,
                        "STATUS_CHANGE",
                        null,
                        "{}",
                        null,
                        LocalDateTime.now(),
                        "System",
                        "System");

        processor.handleAuditLogEvent(event);

        verify(
                jdbcTemplate,
                never())
                .update(
                        any(String.class),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any());
    }

    @Test
    void handleAuditLogEvent_shouldCacheAdminId_acrossMultipleCalls() {

        UserAccount admin =
                UserAccount.builder()
                        .id(1)
                        .username("admin")
                        .role(UserRole.ADMIN)
                        .isActive(true)
                        .build();

        when(userAccountRepository
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(
                        "admin",
                        "admin"))
                .thenReturn(
                        Optional.of(admin));

        AuditLogEvent event1 =
                new AuditLogEvent(
                        this,
                        "a",
                        1,
                        "CREATE",
                        null,
                        "{}",
                        null,
                        LocalDateTime.now(),
                        "System",
                        "System");

        AuditLogEvent event2 =
                new AuditLogEvent(
                        this,
                        "b",
                        2,
                        "CREATE",
                        null,
                        "{}",
                        null,
                        LocalDateTime.now(),
                        "System",
                        "System");

        processor.handleAuditLogEvent(event1);
        processor.handleAuditLogEvent(event2);

        verify(
                userAccountRepository,
                times(1))
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(
                        "admin",
                        "admin");
    }
}