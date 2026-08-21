package com.scse.curriculum.deadline.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scse.curriculum.deadline.entity.SyllabusDeadline;
import com.scse.curriculum.deadline.repository.SyllabusDeadlineEscalationLogRepository;
import com.scse.curriculum.email.DeadlineEscalationEmailMessage;
import com.scse.curriculum.email.EmailOutboxService;
import com.scse.curriculum.notification.entity.Notification;
import com.scse.curriculum.notification.service.NotificationService;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;

@ExtendWith(MockitoExtension.class)
class DeadlineEscalationDeliveryServiceTest {

    @Mock
    private SyllabusDeadlineEscalationLogRepository escalationLogRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailOutboxService emailOutboxService;

    @InjectMocks
    private DeadlineEscalationDeliveryService service;

    private SyllabusDeadline deadline;
    private DeadlineEscalationTarget target;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.of(2026, 7, 24, 9, 15);
        deadline = SyllabusDeadline.builder()
                .id(10L)
                .academicYear("2026-2027")
                .semester(1)
                .deadlineAt(LocalDateTime.of(2026, 7, 20, 8, 0))
                .reminderDays("7,3,1,0")
                .escalationDays("0,1,3,7,14")
                .active(true)
                .revision(2)
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
    }

    @Test
    void shouldStopImmediatelyWhenUniqueClaimAlreadyExists() {
        when(escalationLogRepository.tryClaimDelivery(
                10L, 2, 501, "DEPT_HEAD", "DEPARTMENT:1",
                1, "CS", "Computer Science", 3, 4, 1, 1,
                "Alice Nguyen", "IT013IU", now))
                .thenReturn(0);

        boolean delivered = service.deliver(deadline, target, 3, 4, now);

        assertThat(delivered).isFalse();
        verify(notificationService, never())
                .createNotification(any(), any(), any(), any());
        verify(emailOutboxService, never())
                .enqueueDeadlineEscalation(any(), any());
        verify(escalationLogRepository, never())
                .markDelivered(anyLong(), anyInt(), anyInt(), anyInt(), anyString(), anyInt(), anyBoolean());
    }

    @Test
    void shouldCreateNotificationQueueEmailAndFinalizeAuditLogInOneDelivery() {
        when(escalationLogRepository.tryClaimDelivery(
                10L, 2, 501, "DEPT_HEAD", "DEPARTMENT:1",
                1, "CS", "Computer Science", 3, 4, 1, 1,
                "Alice Nguyen", "IT013IU", now))
                .thenReturn(1);
        when(notificationService.createNotification(
                eq(501),
                any(String.class),
                any(String.class),
                eq("SYLLABUS_DEADLINE_ESCALATED")))
                .thenReturn(Notification.builder().id(901).build());
        when(emailOutboxService.enqueueDeadlineEscalation(
                eq(target.recipient()),
                any(DeadlineEscalationEmailMessage.class)))
                .thenReturn(true);

        boolean delivered = service.deliver(deadline, target, 3, 4, now);

        assertThat(delivered).isTrue();
        verify(escalationLogRepository).markDelivered(
                10L,
                2,
                501,
                3,
                "DEPARTMENT:1",
                901,
                true);
    }
}
