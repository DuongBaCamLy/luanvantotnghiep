package com.scse.curriculum.email;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.notification.service.NotificationService;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;

@ExtendWith(MockitoExtension.class)
class WorkflowNotificationServiceTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailOutboxService emailOutboxService;

    @InjectMocks
    private WorkflowNotificationService service;

    private Syllabus syllabus;
    private UserAccount creator;

    @BeforeEach
    void setUp() {
        creator = user(1, "faculty", "faculty@iu.edu.vn", UserRole.INSTRUCTOR);
        Course course = Course.builder()
                .id(10)
                .courseCode("IT001IU")
                .name("Introduction to Computing")
                .build();
        syllabus = Syllabus.builder()
                .id(100)
                .course(course)
                .createdBy(creator)
                .versionLabel("v2.0")
                .academicYear("2026-2027")
                .semester("1")
                .status(SyllabusStatus.SUBMITTED)
                .build();
    }

    @Test
    void submittedDeduplicatesRecipientsByEmailAcrossChannels() {
        UserAccount first = user(2, "head-a", "head@iu.edu.vn", UserRole.DEPT_HEAD);
        UserAccount duplicate = user(3, "head-b", "HEAD@iu.edu.vn", UserRole.DEPT_HEAD);

        service.notifySubmitted(syllabus, List.of(first, duplicate));

        verify(notificationService, times(1)).createNotification(
                eq(first.getId()),
                any(),
                any(),
                eq("SYLLABUS_SUBMITTED"));
        verify(emailOutboxService, times(1)).enqueue(eq(first), any());
        verify(notificationService, times(1)).createNotification(
                eq(creator.getId()),
                any(),
                any(),
                eq("SYLLABUS_SUBMISSION_CONFIRMED"));
        verify(emailOutboxService, times(1)).enqueue(eq(creator), any());
    }

    @Test
    void departmentHeadApprovalNotifiesDeanAndCreator() {
        UserAccount reviewer = user(4, "dept-head", "dept@iu.edu.vn", UserRole.DEPT_HEAD);
        UserAccount dean = user(5, "dean", "dean@iu.edu.vn", UserRole.DEAN);

        service.notifyDepartmentHeadApproved(
                syllabus,
                reviewer,
                List.of(dean));

        verify(notificationService).createNotification(
                eq(dean.getId()), any(), any(), eq("SYLLABUS_WAITING_FOR_DEAN"));
        verify(notificationService).createNotification(
                eq(creator.getId()), any(), any(), eq("SYLLABUS_DEPT_HEAD_APPROVED"));
        verify(emailOutboxService, times(2)).enqueue(any(), any());
    }

    private UserAccount user(
            int id,
            String username,
            String email,
            UserRole role) {
        return UserAccount.builder()
                .id(id)
                .username(username)
                .email(email)
                .role(role)
                .isActive(true)
                .build();
    }
}
