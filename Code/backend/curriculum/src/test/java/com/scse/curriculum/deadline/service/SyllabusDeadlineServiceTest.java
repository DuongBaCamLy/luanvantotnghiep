package com.scse.curriculum.deadline.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

import com.scse.curriculum.auth.security.CurrentUserService;
import com.scse.curriculum.deadline.dto.DeadlineDispatchResponse;
import com.scse.curriculum.deadline.entity.SyllabusDeadline;
import com.scse.curriculum.deadline.repository.SyllabusDeadlineReminderLogRepository;
import com.scse.curriculum.deadline.repository.SyllabusDeadlineRepository;
import com.scse.curriculum.user.entity.UserAccount;

@ExtendWith(MockitoExtension.class)
class SyllabusDeadlineServiceTest {

    @Mock
    private SyllabusDeadlineRepository deadlineRepository;
    @Mock
    private SyllabusDeadlineReminderLogRepository reminderLogRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private DeadlineReminderTargetResolver targetResolver;
    @Mock
    private DeadlineReminderDeliveryService deliveryService;
    @Mock
    private DeadlineReminderProperties properties;

    @InjectMocks
    private SyllabusDeadlineService service;

    private SyllabusDeadline deadline;
    private DeadlineReminderTarget firstTarget;
    private DeadlineReminderTarget secondTarget;

    @BeforeEach
    void setUp() {
        deadline = SyllabusDeadline.builder()
                .id(10L)
                .academicYear("2026-2027")
                .semester(1)
                .deadlineAt(LocalDateTime.of(2026, 8, 10, 23, 59))
                .reminderDays("14,7,3,1,0")
                .active(true)
                .revision(2)
                .build();

        firstTarget = target(
                101,
                1001,
                "faculty.one",
                "GV Một",
                new DeadlineReminderTarget.MissingCourse(
                        11,
                        "IT101",
                        "Introduction to IT"));
        secondTarget = target(
                102,
                1002,
                "faculty.two",
                "GV Hai",
                new DeadlineReminderTarget.MissingCourse(
                        12,
                        "IT102",
                        "Programming Fundamentals"));

        when(deadlineRepository.findById(10L))
                .thenReturn(Optional.of(deadline));
    }

    @Test
    void schedulerSendsAtExactConfiguredMilestone() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 3, 8, 0);
        when(targetResolver.resolve("2026-2027", 1))
                .thenReturn(new DeadlineReminderTargetResolver.Resolution(
                        List.of(firstTarget),
                        0));
        when(deliveryService.deliver(
                deadline,
                firstTarget,
                7,
                7,
                now))
                .thenReturn(true);

        DeadlineDispatchResponse result = service.dispatchDeadline(
                10L,
                false,
                now);

        assertThat(result.reminderDue()).isTrue();
        assertThat(result.reminderDay()).isEqualTo(7);
        assertThat(result.daysRemaining()).isEqualTo(7);
        assertThat(result.notificationsQueued()).isEqualTo(1);
        assertThat(result.failedDeliveries()).isZero();

        verify(deliveryService).deliver(
                deadline,
                firstTarget,
                7,
                7,
                now);
    }

    @Test
    void schedulerCatchesUpNearestMissedMilestoneAfterDowntime() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 4, 8, 0);
        when(targetResolver.resolve("2026-2027", 1))
                .thenReturn(new DeadlineReminderTargetResolver.Resolution(
                        List.of(firstTarget),
                        0));
        when(deliveryService.deliver(
                deadline,
                firstTarget,
                7,
                6,
                now))
                .thenReturn(true);

        DeadlineDispatchResponse result = service.dispatchDeadline(
                10L,
                false,
                now);

        assertThat(result.reminderDay()).isEqualTo(7);
        assertThat(result.daysRemaining()).isEqualTo(6);
        assertThat(result.notificationsQueued()).isEqualTo(1);

        verify(deliveryService).deliver(
                deadline,
                firstTarget,
                7,
                6,
                now);
    }

    @Test
    void forcedTestUsesDedicatedKeyEvenOnSchedulerMilestone() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 3, 8, 0);
        when(targetResolver.resolve("2026-2027", 1))
                .thenReturn(new DeadlineReminderTargetResolver.Resolution(
                        List.of(firstTarget),
                        0));
        when(deliveryService.deliver(
                deadline,
                firstTarget,
                -1,
                7,
                now))
                .thenReturn(true);

        DeadlineDispatchResponse result = service.dispatchDeadline(
                10L,
                true,
                now);

        assertThat(result.forced()).isTrue();
        assertThat(result.reminderDue()).isTrue();
        assertThat(result.reminderDay()).isEqualTo(7);
        assertThat(result.notificationsQueued()).isEqualTo(1);

        verify(deliveryService).deliver(
                deadline,
                firstTarget,
                -1,
                7,
                now);
        verify(deliveryService, never()).deliver(
                deadline,
                firstTarget,
                7,
                7,
                now);
    }

    @Test
    void overdueDeadlineIsNotSentEvenWhenForced() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 11, 8, 0);

        DeadlineDispatchResponse result = service.dispatchDeadline(
                10L,
                true,
                now);

        assertThat(result.notificationsQueued()).isZero();
        assertThat(result.recipientCount()).isZero();
        assertThat(result.message()).contains("FR-05.7");
        verifyNoInteractions(targetResolver, deliveryService);
    }

    @Test
    void oneRecipientFailureDoesNotAbortRemainingRecipients() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 9, 8, 0);
        when(targetResolver.resolve("2026-2027", 1))
                .thenReturn(new DeadlineReminderTargetResolver.Resolution(
                        List.of(firstTarget, secondTarget),
                        0));
        when(deliveryService.deliver(
                deadline,
                firstTarget,
                1,
                1,
                now))
                .thenThrow(new IllegalStateException("SMTP/outbox failure"));
        when(deliveryService.deliver(
                deadline,
                secondTarget,
                1,
                1,
                now))
                .thenReturn(true);

        DeadlineDispatchResponse result = service.dispatchDeadline(
                10L,
                false,
                now);

        assertThat(result.recipientCount()).isEqualTo(2);
        assertThat(result.notificationsQueued()).isEqualTo(1);
        assertThat(result.failedDeliveries()).isEqualTo(1);
        assertThat(result.message()).contains("sẽ được thử lại");

        verify(deliveryService).deliver(
                deadline,
                secondTarget,
                1,
                1,
                now);
    }

    private DeadlineReminderTarget target(
            int userId,
            int instructorId,
            String username,
            String instructorName,
            DeadlineReminderTarget.MissingCourse missingCourse) {
        UserAccount user = UserAccount.builder()
                .id(userId)
                .username(username)
                .email(username + "@iu.edu.vn")
                .instructorId(instructorId)
                .isActive(true)
                .build();
        return new DeadlineReminderTarget(
                user,
                instructorId,
                instructorName,
                List.of(missingCourse));
    }
}
