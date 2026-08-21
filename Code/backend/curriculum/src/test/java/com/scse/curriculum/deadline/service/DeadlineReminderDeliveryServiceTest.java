package com.scse.curriculum.deadline.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scse.curriculum.deadline.entity.SyllabusDeadline;
import com.scse.curriculum.deadline.repository.SyllabusDeadlineReminderLogRepository;
import com.scse.curriculum.email.DeadlineEmailMessage;
import com.scse.curriculum.email.EmailOutboxService;
import com.scse.curriculum.notification.entity.Notification;
import com.scse.curriculum.notification.service.NotificationService;
import com.scse.curriculum.user.entity.UserAccount;

@ExtendWith(MockitoExtension.class)
class DeadlineReminderDeliveryServiceTest {

    @Mock
    private SyllabusDeadlineReminderLogRepository reminderLogRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private EmailOutboxService emailOutboxService;

    @InjectMocks
    private DeadlineReminderDeliveryService service;

    private SyllabusDeadline deadline;
    private UserAccount user;
    private DeadlineReminderTarget target;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        deadline = SyllabusDeadline.builder()
                .id(30L)
                .academicYear("2026-2027")
                .semester(2)
                .deadlineAt(LocalDateTime.of(2027, 1, 15, 17, 0))
                .revision(3)
                .active(true)
                .build();
        user = UserAccount.builder()
                .id(700)
                .username("faculty.deadline")
                .email("faculty.deadline@iu.edu.vn")
                .instructorId(70)
                .isActive(true)
                .build();
        target = new DeadlineReminderTarget(
                user,
                70,
                "Faculty Deadline",
                List.of(
                        new DeadlineReminderTarget.MissingCourse(
                                1,
                                "IT201",
                                "Data Structures"),
                        new DeadlineReminderTarget.MissingCourse(
                                2,
                                "IT202",
                                "Databases")));
        now = LocalDateTime.of(2027, 1, 8, 8, 0);
    }

    @Test
    void duplicateClaimSkipsNotificationAndEmail() {
        when(reminderLogRepository.tryClaimDelivery(
                30L,
                3,
                700,
                7,
                2,
                "IT201, IT202",
                now))
                .thenReturn(0);

        boolean delivered = service.deliver(
                deadline,
                target,
                7,
                7,
                now);

        assertThat(delivered).isFalse();
        verify(notificationService, never()).createNotification(
                any(),
                any(),
                any(),
                any());
        verify(emailOutboxService, never()).enqueueDeadlineReminder(
                any(),
                any());
        verify(reminderLogRepository, never()).markDelivered(
                any(),
                any(),
                any(),
                any(),
                any(),
                anyBoolean());
    }

    @Test
    void successfulClaimCreatesInAppNotificationQueuesEmailAndCompletesLog() {
        when(reminderLogRepository.tryClaimDelivery(
                30L,
                3,
                700,
                7,
                2,
                "IT201, IT202",
                now))
                .thenReturn(1);
        when(notificationService.createNotification(
                eq(700),
                eq("Nhắc deadline nộp đề cương"),
                contains("IT201, IT202"),
                eq("SYLLABUS_DEADLINE_REMINDER")))
                .thenReturn(Notification.builder().id(900).build());
        when(emailOutboxService.enqueueDeadlineReminder(
                eq(user),
                any(DeadlineEmailMessage.class)))
                .thenReturn(true);

        boolean delivered = service.deliver(
                deadline,
                target,
                7,
                7,
                now);

        assertThat(delivered).isTrue();

        ArgumentCaptor<DeadlineEmailMessage> emailCaptor =
                ArgumentCaptor.forClass(DeadlineEmailMessage.class);
        verify(emailOutboxService).enqueueDeadlineReminder(
                eq(user),
                emailCaptor.capture());
        assertThat(emailCaptor.getValue().daysRemaining()).isEqualTo(7);
        assertThat(emailCaptor.getValue().missingCourses()).hasSize(2);

        verify(reminderLogRepository).markDelivered(
                30L,
                3,
                700,
                7,
                900,
                true);
    }

    @Test
    void dueTodayUsesUrgentEventType() {
        when(reminderLogRepository.tryClaimDelivery(
                30L,
                3,
                700,
                0,
                2,
                "IT201, IT202",
                now))
                .thenReturn(1);
        when(notificationService.createNotification(
                eq(700),
                eq("Deadline nộp đề cương là hôm nay"),
                contains("đến hạn hôm nay"),
                eq("SYLLABUS_DEADLINE_DUE_TODAY")))
                .thenReturn(Notification.builder().id(901).build());
        when(emailOutboxService.enqueueDeadlineReminder(
                eq(user),
                any(DeadlineEmailMessage.class)))
                .thenReturn(false);

        boolean delivered = service.deliver(
                deadline,
                target,
                0,
                0,
                now);

        assertThat(delivered).isTrue();
        verify(reminderLogRepository).markDelivered(
                30L,
                3,
                700,
                0,
                901,
                false);
    }
}
