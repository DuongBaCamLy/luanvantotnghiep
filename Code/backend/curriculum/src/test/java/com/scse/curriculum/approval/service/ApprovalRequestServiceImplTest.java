package com.scse.curriculum.approval.service;

import static org.mockito.Mockito.times;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import com.scse.curriculum.approval.dto.ReviewApprovalRequest;
import com.scse.curriculum.approval.entity.ApprovalRequest;
import com.scse.curriculum.approval.entity.ApprovalStatus;
import com.scse.curriculum.approval.entity.ApprovalStep;
import com.scse.curriculum.approval.repository.ApprovalRequestRepository;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.email.WorkflowNotificationService;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.syllabus.service.SyllabusAccessService;
import com.scse.curriculum.syllabus.service.SyllabusService;
import com.scse.curriculum.syllabus.dto.SyllabusResponse;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class ApprovalRequestServiceImplTest {

    @Mock
    private ApprovalRequestRepository repository;
    @Mock
    private SyllabusRepository syllabusRepository;
    @Mock
    private UserAccountRepository userRepository;
    @Mock
    private WorkflowNotificationService workflowNotificationService;
    @Mock
    private SyllabusAccessService syllabusAccessService;
    @Mock
    private SyllabusService syllabusService;

    @InjectMocks
    private ApprovalRequestServiceImpl service;

    private Course course;
    private UserAccount creator;
    private UserAccount deptHead;
    private UserAccount dean;

    @BeforeEach
    void setUp() {
        course = Course.builder()
                .id(10)
                .courseCode("IT001IU")
                .name("Introduction to Computing")
                .build();
        creator = user(1, "faculty", UserRole.INSTRUCTOR);
        deptHead = user(2, "dept-head", UserRole.DEPT_HEAD);
        dean = user(3, "dean", UserRole.DEAN);
    }

    @Test
    void departmentHeadApprovalCreatesDeanStepAndQueuesNotifications() {
        Syllabus syllabus = syllabus(SyllabusStatus.SUBMITTED);

        ApprovalRequest approval = approval(
                20,
                syllabus,
                ApprovalStep.STEP1_DEPT_HEAD,
                creator);

        ReviewApprovalRequest request
                = review(ApprovalStatus.APPROVED, "Đạt");

        when(repository.findById(20))
                .thenReturn(Optional.of(approval));

        when(syllabusAccessService.currentUser())
                .thenReturn(deptHead);

        when(userRepository.findByRoleAndIsActiveTrue(UserRole.DEAN))
                .thenReturn(List.of(dean));

        when(repository.save(any(ApprovalRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.review(20, request);

        /*
     * Kiểm tra trạng thái syllabus sau khi Trưởng bộ môn duyệt.
     * Syllabus chưa APPROVED cuối cùng mà chuyển sang chờ Trưởng khoa.
         */
        assertThat(syllabus.getStatus())
                .isEqualTo(SyllabusStatus.UNDER_REVIEW);

        verify(syllabusRepository).save(syllabus);

        /*
     * Workflow phải lưu hai ApprovalRequest:
     * 1. Cập nhật yêu cầu của Trưởng bộ môn thành APPROVED.
     * 2. Tạo yêu cầu PENDING mới dành cho Trưởng khoa.
         */
        ArgumentCaptor<ApprovalRequest> requestCaptor
                = ArgumentCaptor.forClass(ApprovalRequest.class);

        verify(repository, times(2))
                .save(requestCaptor.capture());

        List<ApprovalRequest> savedRequests
                = requestCaptor.getAllValues();

        assertThat(savedRequests).hasSize(2);

        /*
     * Lần lưu thứ nhất: hoàn tất bước Trưởng bộ môn.
         */
        ApprovalRequest savedDepartmentHeadRequest
                = savedRequests.get(0);

        assertThat(savedDepartmentHeadRequest)
                .isSameAs(approval);

        assertThat(savedDepartmentHeadRequest.getStep())
                .isEqualTo(ApprovalStep.STEP1_DEPT_HEAD);

        assertThat(savedDepartmentHeadRequest.getStatus())
                .isEqualTo(ApprovalStatus.APPROVED);

        assertThat(savedDepartmentHeadRequest.getReviewedBy())
                .isEqualTo(deptHead);

        assertThat(savedDepartmentHeadRequest.getComment())
                .isEqualTo("Đạt");

        assertThat(savedDepartmentHeadRequest.getResolvedAt())
                .isNotNull();

        /*
     * Lần lưu thứ hai: tạo bước duyệt dành cho Trưởng khoa.
         */
        ApprovalRequest savedDeanRequest
                = savedRequests.get(1);

        assertThat(savedDeanRequest)
                .isNotSameAs(approval);

        assertThat(savedDeanRequest.getSyllabus())
                .isSameAs(syllabus);

        assertThat(savedDeanRequest.getStep())
                .isEqualTo(ApprovalStep.STEP3_DEAN);

        assertThat(savedDeanRequest.getStatus())
                .isEqualTo(ApprovalStatus.PENDING);

        assertThat(savedDeanRequest.getRequestedBy())
                .isEqualTo(deptHead);

        assertThat(savedDeanRequest.getReviewedBy())
                .isNull();

        assertThat(savedDeanRequest.getComment())
                .isNull();

        assertThat(savedDeanRequest.getResolvedAt())
                .isNull();

        /*
     * Kiểm tra email được tạo cho Dean và Faculty.
         */
        verify(workflowNotificationService)
                .notifyDepartmentHeadApproved(
                        eq(syllabus),
                        eq(deptHead),
                        eq(List.of(dean)));
    }

    @Test
    void departmentHeadCannotApproveWhenNoActiveDeanExists() {
        Syllabus syllabus = syllabus(SyllabusStatus.SUBMITTED);
        ApprovalRequest approval = approval(
                21,
                syllabus,
                ApprovalStep.STEP1_DEPT_HEAD,
                creator);

        when(repository.findById(21)).thenReturn(Optional.of(approval));
        when(syllabusAccessService.currentUser()).thenReturn(deptHead);
        when(userRepository.findByRoleAndIsActiveTrue(UserRole.DEAN))
                .thenReturn(List.of());

        assertThatThrownBy(()
                -> service.review(21, review(ApprovalStatus.APPROVED, "")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Chưa có tài khoản Trưởng khoa");
    }

    @Test
void deanRevisionNotifiesCreatorAndForwardingDepartmentHead() {
    Syllabus syllabus = syllabus(SyllabusStatus.UNDER_REVIEW);

    ApprovalRequest approval = approval(
            22,
            syllabus,
            ApprovalStep.STEP3_DEAN,
            deptHead);

    ReviewApprovalRequest request = review(
           ApprovalStatus.REJECTED,
            "Bổ sung mapping CLO-PLO");

    when(repository.findById(22))
            .thenReturn(Optional.of(approval));

    when(syllabusAccessService.currentUser())
            .thenReturn(dean);

    when(repository.save(any(ApprovalRequest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    when(syllabusService.createRevisionDraft(
            eq(100),
            eq("Bổ sung mapping CLO-PLO")))
            .thenReturn(
                    SyllabusResponse.builder()
                            .id(101)
                            .versionNumber(3)
                            .versionLabel("v3.0")
                            .status(SyllabusStatus.DRAFT.name())
                            .build()
            );

    var response = service.review(22, request);

    assertThat(syllabus.getStatus())
        .isEqualTo(SyllabusStatus.REJECTED);
    verify(workflowNotificationService)
            .notifyRevisionRequested(
                    eq(syllabus),
                    eq(dean),
                    eq("Bổ sung mapping CLO-PLO"),
                    eq(ApprovalStep.STEP3_DEAN),
                    eq(List.of(deptHead)));

    verify(syllabusService)
            .createRevisionDraft(
                    eq(100),
                    eq("Bổ sung mapping CLO-PLO"));

    assertThat(response.getRevisionDraftId())
            .isEqualTo(101);

    assertThat(response.getRevisionDraftVersionLabel())
            .isEqualTo("v3.0");
}
    private Syllabus syllabus(SyllabusStatus status) {
        return Syllabus.builder()
                .id(100)
                .course(course)
                .createdBy(creator)
                .versionNumber(2)
                .versionLabel("v2.0")
                .academicYear("2026-2027")
                .semester("1")
                .status(status)
                .isCurrent(false)
                .build();
    }

    private ApprovalRequest approval(
            int id,
            Syllabus syllabus,
            ApprovalStep step,
            UserAccount requester) {
        return ApprovalRequest.builder()
                .id(id)
                .syllabus(syllabus)
                .step(step)
                .status(ApprovalStatus.PENDING)
                .requestedBy(requester)
                .build();
    }

    private ReviewApprovalRequest review(
            ApprovalStatus status,
            String comment) {
        ReviewApprovalRequest request = new ReviewApprovalRequest();
        request.setStatus(status);
        request.setComment(comment);
        return request;
    }

    private UserAccount user(int id, String username, UserRole role) {
        return UserAccount.builder()
                .id(id)
                .username(username)
                .email(username + "@iu.edu.vn")
                .role(role)
                .isActive(true)
                .build();
    }
}
