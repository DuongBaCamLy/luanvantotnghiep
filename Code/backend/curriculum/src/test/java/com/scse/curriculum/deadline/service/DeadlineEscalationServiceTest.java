package com.scse.curriculum.deadline.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scse.curriculum.deadline.dto.DeadlineEscalationDispatchResponse;
import com.scse.curriculum.deadline.entity.SyllabusDeadline;
import com.scse.curriculum.deadline.repository.SyllabusDeadlineEscalationLogRepository;
import com.scse.curriculum.deadline.repository.SyllabusDeadlineRepository;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;

@ExtendWith(MockitoExtension.class)
class DeadlineEscalationServiceTest {

    @Mock
    private SyllabusDeadlineRepository deadlineRepository;

    @Mock
    private SyllabusDeadlineEscalationLogRepository escalationLogRepository;

    @Mock
    private DeadlineEscalationTargetResolver targetResolver;

    @Mock
    private DeadlineEscalationDeliveryService deliveryService;

    @Mock
    private DeadlineEscalationProperties properties;

    @Mock
    private SyllabusDeadlineService deadlineService;

    @InjectMocks
    private DeadlineEscalationService service;

    private SyllabusDeadline deadline;
    private DeadlineEscalationTarget target;

    @BeforeEach
    void setUp() {
        deadline = SyllabusDeadline.builder()
                .id(10L)
                .academicYear("2026-2027")
                .semester(1)
                .deadlineAt(LocalDateTime.of(2026, 7, 20, 8, 0))
                .reminderDays("7,3,1,0")
                .escalationDays("0,1,3,7,14")
                .active(true)
                .revision(3)
                .build();

        UserAccount head = UserAccount.builder()
                .id(501)
                .username("head.cs")
                .email("head.cs@iu.edu.vn")
                .passwordHash("test")
                .role(UserRole.DEPT_HEAD)
                .isActive(true)
                .build();

        target = new DeadlineEscalationTarget(
                head,
                UserRole.DEPT_HEAD,
                "DEPARTMENT:1",
                1,
                "CS",
                "Computer Science",
                List.of(new DeadlineEscalationTarget.OverdueInstructor(
                        11,
                        "Alice Nguyen",
                        "alice@iu.edu.vn",
                        1,
                        "CS",
                        "Computer Science",
                        List.of(new DeadlineEscalationTarget.MissingCourse(
                                101,
                                "IT013IU",
                                "Algorithms")))));

        when(deadlineRepository.findById(10L)).thenReturn(Optional.of(deadline));
    }

    @Test
    void shouldNotEscalateBeforeDeadlineEvenWhenForced() {
        LocalDateTime current = LocalDateTime.of(2026, 7, 20, 7, 59);

        DeadlineEscalationDispatchResponse result =
                service.dispatchDeadline(10L, true, current);

        assertThat(result.notificationsQueued()).isZero();
        assertThat(result.escalationDue()).isFalse();
        assertThat(result.message()).contains("chưa quá hạn");
        verify(targetResolver, never()).resolve(any(), any());
        verify(deliveryService, never()).deliver(any(), any(), anyInt(), anyInt(), any());
    }

    @Test
    void shouldCatchUpTheNearestPastMilestoneAfterRestart() {
        LocalDateTime current = LocalDateTime.of(2026, 7, 22, 10, 0);
        when(targetResolver.resolve("2026-2027", 1))
                .thenReturn(resolution(target));
        when(deliveryService.deliver(
                eq(deadline),
                eq(target),
                eq(1),
                eq(2),
                eq(current)))
                .thenReturn(true);

        DeadlineEscalationDispatchResponse result =
                service.dispatchDeadline(10L, false, current);

        assertThat(result.daysOverdue()).isEqualTo(2);
        assertThat(result.escalationDue()).isTrue();
        assertThat(result.escalationDay()).isEqualTo(1);
        assertThat(result.notificationsQueued()).isEqualTo(1);
        assertThat(result.failedDeliveries()).isZero();
    }

    @Test
    void shouldUseAnIndependentIdempotencyKeyForManualEscalation() {
        LocalDateTime current = LocalDateTime.of(2026, 7, 23, 10, 0);
        when(targetResolver.resolve("2026-2027", 1))
                .thenReturn(resolution(target));
        when(deliveryService.deliver(
                eq(deadline),
                eq(target),
                eq(-1),
                eq(3),
                eq(current)))
                .thenReturn(true);

        DeadlineEscalationDispatchResponse result =
                service.dispatchDeadline(10L, true, current);

        assertThat(result.forced()).isTrue();
        assertThat(result.notificationsQueued()).isEqualTo(1);
        verify(deliveryService).deliver(deadline, target, -1, 3, current);
    }

    @Test
    void shouldIsolateRecipientFailureAndContinueOtherDeliveries() {
        LocalDateTime current = LocalDateTime.of(2026, 7, 23, 10, 0);
        DeadlineEscalationTarget deanTarget = new DeadlineEscalationTarget(
                UserAccount.builder()
                        .id(601)
                        .username("dean")
                        .email("dean@iu.edu.vn")
                        .passwordHash("test")
                        .role(UserRole.DEAN)
                        .isActive(true)
                        .build(),
                UserRole.DEAN,
                "FACULTY",
                null,
                null,
                "SCSE",
                target.overdueInstructors());

        when(targetResolver.resolve("2026-2027", 1))
                .thenReturn(resolution(target, deanTarget));
        when(deliveryService.deliver(deadline, target, 3, 3, current))
                .thenThrow(new IllegalStateException("SMTP/outbox error"));
        when(deliveryService.deliver(deadline, deanTarget, 3, 3, current))
                .thenReturn(true);

        DeadlineEscalationDispatchResponse result =
                service.dispatchDeadline(10L, false, current);

        assertThat(result.notificationsQueued()).isEqualTo(1);
        assertThat(result.failedDeliveries()).isEqualTo(1);
        assertThat(result.message()).contains("1 người nhận lỗi");
        verify(deliveryService).deliver(deadline, deanTarget, 3, 3, current);
    }

    @Test
    void shouldSkipInactiveDeadline() {
        deadline.setActive(false);
        LocalDateTime current = LocalDateTime.of(2026, 7, 30, 10, 0);

        DeadlineEscalationDispatchResponse result =
                service.dispatchDeadline(10L, false, current);

        assertThat(result.notificationsQueued()).isZero();
        assertThat(result.message()).contains("đang tắt");
        verify(targetResolver, never()).resolve(any(), any());
    }

    private DeadlineEscalationTargetResolver.Resolution resolution(
            DeadlineEscalationTarget... targets) {
        return new DeadlineEscalationTargetResolver.Resolution(
                List.of(targets),
                List.of(new DeadlineEscalationTargetResolver.DepartmentResolution(
                        1,
                        "CS",
                        "Computer Science",
                        target.overdueInstructors(),
                        List.of(target.recipient()))),
                0,
                false,
                1,
                1);
    }
}
