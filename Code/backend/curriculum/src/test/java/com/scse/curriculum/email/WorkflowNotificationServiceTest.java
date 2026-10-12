package com.scse.curriculum.email;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
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

    @InjectMocks
    private WorkflowNotificationService service;

    private Syllabus syllabus;
    private UserAccount creator;

    @BeforeEach
    void setUp() {
        creator = user(
                1,
                "faculty",
                "faculty@iu.edu.vn",
                UserRole.INSTRUCTOR);

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
    void submittedCreatesInAppNotificationForEachUser() {
        UserAccount first = user(
                2,
                "head-a",
                "head@iu.edu.vn",
                UserRole.DEPT_HEAD);

        UserAccount duplicate = user(
                3,
                "head-b",
                "HEAD@iu.edu.vn",
                UserRole.DEPT_HEAD);

        service.notifySubmitted(
                syllabus,
                List.of(first, duplicate));

        verify(notificationService).createNotification(
                eq(first.getId()),
                any(),
                any(),
                eq("SYLLABUS_SUBMITTED"));

        verify(notificationService).createNotification(
                eq(duplicate.getId()),
                any(),
                any(),
                eq("SYLLABUS_SUBMITTED"));

        verify(notificationService).createNotification(
                eq(creator.getId()),
                any(),
                any(),
                eq("SYLLABUS_SUBMISSION_CONFIRMED"));
    }

    @Test
    void departmentHeadApprovalNotifiesDeanSecretaryAndCreator() {
        UserAccount reviewer = user(
                4,
                "dept-head",
                "dept@iu.edu.vn",
                UserRole.DEPT_HEAD);

        UserAccount deanSecretary = user(
                5,
                "dean-secretary",
                "secretary@iu.edu.vn",
                UserRole.DEAN_SECRETARY);

        service.notifyDepartmentHeadApproved(
                syllabus,
                reviewer,
                List.of(deanSecretary));

        verify(notificationService).createNotification(
                eq(deanSecretary.getId()),
                any(),
                any(),
                eq("SYLLABUS_WAITING_FOR_FINAL_REVIEW"));

        verify(notificationService).createNotification(
                eq(creator.getId()),
                any(),
                any(),
                eq("SYLLABUS_DEPT_HEAD_APPROVED"));
    }

    @Test
    void deanSecretaryForwardNotifiesDeanAndCreator() {
        UserAccount reviewer = user(
                6,
                "dean-secretary",
                "secretary@iu.edu.vn",
                UserRole.DEAN_SECRETARY);

        UserAccount dean = user(
                7,
                "dean",
                "dean@iu.edu.vn",
                UserRole.DEAN);

        service.notifyDeanSecretaryForwarded(
                syllabus,
                reviewer,
                List.of(dean));

        verify(notificationService).createNotification(
                eq(dean.getId()),
                any(),
                any(),
                eq("SYLLABUS_WAITING_FOR_DEAN"));

        verify(notificationService).createNotification(
                eq(creator.getId()),
                any(),
                any(),
                eq("SYLLABUS_FORWARDED_TO_DEAN"));
    }

    @Test
    void finalQueueNotifiesBothRolesOnceAndSkipsInactiveAccounts() {
        UserAccount head = user(4, "head", "head@iu.edu.vn", UserRole.DEPT_HEAD);
        UserAccount dean = user(7, "dean", "dean@iu.edu.vn", UserRole.DEAN);
        UserAccount secretary = user(8, "secretary", "secretary@iu.edu.vn", UserRole.DEAN_SECRETARY);
        UserAccount inactive = user(9, "inactive", "inactive@iu.edu.vn", UserRole.DEAN);
        inactive.setIsActive(false);

        service.notifyDepartmentHeadApproved(syllabus, head, List.of(dean, secretary, dean, inactive));

        for (UserAccount recipient : List.of(dean, secretary)) {
            verify(notificationService).createNotification(eq(recipient.getId()), any(), any(),
                    eq("SYLLABUS_WAITING_FOR_FINAL_REVIEW"));
        }
        verify(notificationService, never()).createNotification(eq(inactive.getId()), any(), any(), any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"DEAN,Trưởng khoa", "DEAN_SECRETARY,Thư ký khoa"})
    void finalApprovalAndRejectionIdentifyActualReviewerRole(UserRole role, String label) {
        UserAccount reviewer = user(7, "reviewer", "reviewer@iu.edu.vn", role);
        UserAccount head = user(4, "head", "head@iu.edu.vn", UserRole.DEPT_HEAD);

        service.notifyFullyApproved(syllabus, reviewer, List.of(head));
        service.notifyRevisionRequested(syllabus, reviewer, "Required revision",
                com.scse.curriculum.approval.entity.ApprovalStep.STEP3_DEAN, List.of(head));

        verify(notificationService).createNotification(eq(creator.getId()), any(), contains(label),
                eq("SYLLABUS_APPROVED"));
        verify(notificationService).createNotification(eq(creator.getId()), any(), contains(label),
                eq("SYLLABUS_REVISION_REQUESTED"));
        verify(notificationService).createNotification(eq(head.getId()), any(), contains(label),
                eq("SYLLABUS_DEAN_REVISION_REQUESTED"));
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
