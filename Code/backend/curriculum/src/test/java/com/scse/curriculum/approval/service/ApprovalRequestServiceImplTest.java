package com.scse.curriculum.approval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scse.curriculum.approval.dto.ReviewApprovalRequest;
import com.scse.curriculum.approval.entity.ApprovalRequest;
import com.scse.curriculum.approval.entity.ApprovalStatus;
import com.scse.curriculum.approval.entity.ApprovalStep;
import com.scse.curriculum.approval.repository.ApprovalRequestRepository;
import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.email.WorkflowNotificationService;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.syllabus.history.SyllabusHistoryService;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.syllabus.service.SyllabusAccessService;
import com.scse.curriculum.syllabus.service.SyllabusService;
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
    private EntityManager entityManager;

    @Mock
    private WorkflowNotificationService workflowNotificationService;

    @Mock
    private SyllabusAccessService syllabusAccessService;

    @Mock
    private SyllabusService syllabusService;

    @Mock
    private CourseProgramRepository courseProgramRepository;

    @Mock
    private ClassSectionRepository classSectionRepository;

    @Mock
    private SyllabusHistoryService syllabusHistoryService;

    @InjectMocks
    private ApprovalRequestServiceImpl service;

    private Course course;
    private UserAccount creator;
    private UserAccount deptHead;
    private UserAccount deanSecretary;
    private UserAccount dean;

    @BeforeEach
    void setUp() {
        course = Course.builder()
                .id(10)
                .courseCode("IT001IU")
                .name("Introduction to Computing")
                .build();

        creator = user(
                1,
                "faculty",
                UserRole.INSTRUCTOR);

        deptHead = user(
                2,
                "dept-head",
                UserRole.DEPT_HEAD);

        deanSecretary = user(
                3,
                "dean-secretary",
                UserRole.DEAN_SECRETARY);

        dean = user(
                4,
                "dean",
                UserRole.DEAN);
    }

    @Test
    void departmentHeadApprovalCreatesSharedFinalStepWithOnlySecretaryActive() {
        Syllabus syllabus =
                syllabus(SyllabusStatus.SUBMITTED);

        ApprovalRequest approval = approval(
                20,
                syllabus,
                ApprovalStep.STEP1_DEPT_HEAD,
                creator);

        when(repository.findById(20))
                .thenReturn(Optional.of(approval));

        when(syllabusAccessService.currentUser())
                .thenReturn(deptHead);

        when(userRepository.findByRoleAndIsActiveTrue(UserRole.DEAN)).thenReturn(List.of());
        when(userRepository.findByRoleAndIsActiveTrue(
                UserRole.DEAN_SECRETARY))
                .thenReturn(List.of(deanSecretary));

        when(repository.save(any(ApprovalRequest.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        stubMappingRepositories();

        service.review(
                20,
                review(
                        ApprovalStatus.APPROVED,
                        "Đạt"));

        assertThat(syllabus.getStatus())
                .isEqualTo(SyllabusStatus.UNDER_REVIEW);

        ArgumentCaptor<ApprovalRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        ApprovalRequest.class);

        verify(repository, times(2))
                .save(requestCaptor.capture());

        List<ApprovalRequest> savedRequests =
                requestCaptor.getAllValues();

        assertThat(savedRequests)
                .hasSize(2);

        ApprovalRequest savedDepartmentHeadRequest =
                savedRequests.get(0);

        assertThat(savedDepartmentHeadRequest)
                .isSameAs(approval);

        assertThat(savedDepartmentHeadRequest.getStep())
                .isEqualTo(
                        ApprovalStep.STEP1_DEPT_HEAD);

        assertThat(savedDepartmentHeadRequest.getStatus())
                .isEqualTo(
                        ApprovalStatus.APPROVED);

        assertThat(savedDepartmentHeadRequest.getReviewedBy())
                .isEqualTo(deptHead);

        assertThat(savedDepartmentHeadRequest.getResolvedAt())
                .isNotNull();

        ApprovalRequest secretaryRequest =
                savedRequests.get(1);

        assertThat(secretaryRequest)
                .isNotSameAs(approval);

        assertThat(secretaryRequest.getSyllabus())
                .isSameAs(syllabus);

        assertThat(secretaryRequest.getStep())
                .isEqualTo(
                        ApprovalStep.STEP3_DEAN);

        assertThat(secretaryRequest.getStatus())
                .isEqualTo(
                        ApprovalStatus.PENDING);

        assertThat(secretaryRequest.getRequestedBy())
                .isEqualTo(deptHead);

        assertThat(secretaryRequest.getReviewedBy())
                .isNull();

        assertThat(secretaryRequest.getComment())
                .isNull();

        assertThat(secretaryRequest.getResolvedAt())
                .isNull();

        verify(workflowNotificationService)
                .notifyDepartmentHeadApproved(
                        eq(syllabus),
                        eq(deptHead),
                        eq(List.of(deanSecretary)));

        verify(userRepository)
                .findByRoleAndIsActiveTrue(
                        UserRole.DEAN);
    }

    @Test
    void departmentHeadCannotAdvanceWithoutAnyActiveFinalReviewer() {
        Syllabus syllabus =
                syllabus(SyllabusStatus.SUBMITTED);

        ApprovalRequest approval = approval(
                21,
                syllabus,
                ApprovalStep.STEP1_DEPT_HEAD,
                creator);

        when(repository.findById(21))
                .thenReturn(Optional.of(approval));

        when(syllabusAccessService.currentUser())
                .thenReturn(deptHead);

        when(userRepository.findByRoleAndIsActiveTrue(UserRole.DEAN)).thenReturn(List.of());
        when(userRepository.findByRoleAndIsActiveTrue(
                UserRole.DEAN_SECRETARY))
                .thenReturn(List.of());

        assertThatThrownBy(() ->
                service.review(
                        21,
                        review(
                                ApprovalStatus.APPROVED,
                                "")))
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessageContaining(
                        "No active Dean or Dean Secretary account");

        assertThat(syllabus.getStatus())
                .isEqualTo(SyllabusStatus.SUBMITTED);

        assertThat(approval.getStatus())
                .isEqualTo(ApprovalStatus.PENDING);

        verify(repository, never())
                .save(any(ApprovalRequest.class));

        verify(syllabusRepository, never())
                .save(any(Syllabus.class));
    }

    @Test
    void deanSecretaryFinalizesLegacyRequestWithoutCreatingAnotherStep() {
        Syllabus syllabus = syllabus(SyllabusStatus.UNDER_REVIEW);
        ApprovalRequest approval = approval(22, syllabus, ApprovalStep.STEP2_DEAN_SECRETARY, deptHead);
        when(repository.findById(22)).thenReturn(Optional.of(approval));
        when(syllabusAccessService.currentUser()).thenReturn(deanSecretary);
        when(repository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubMappingRepositories();

        service.review(22, review(ApprovalStatus.APPROVED, "Final approved"));

        assertThat(syllabus.getStatus()).isEqualTo(SyllabusStatus.APPROVED);
        assertThat(syllabus.getApprovedBy()).isSameAs(deanSecretary);
        assertThat(syllabus.getApprovedAt()).isNotNull();
        assertThat(syllabus.getIsCurrent()).isTrue();
        assertThat(approval.getReviewedBy()).isSameAs(deanSecretary);
        assertThat(approval.getResolvedAt()).isNotNull();
        verify(repository).save(approval);
        verify(userRepository, never()).findByRoleAndIsActiveTrue(any(UserRole.class));
        verify(workflowNotificationService).notifyFullyApproved(syllabus, deanSecretary, List.of());
        verify(workflowNotificationService, never()).notifyDeanSecretaryForwarded(any(), any(), any());
        verify(syllabusHistoryService).capture(syllabus, "APPROVED", deanSecretary.getUsername());
    }

    @Test
    void deanSecretaryCanRejectLegacyRequestWithComment() {
        Syllabus syllabus = syllabus(SyllabusStatus.UNDER_REVIEW);
        ApprovalRequest approval = approval(23, syllabus, ApprovalStep.STEP2_DEAN_SECRETARY, deptHead);
        when(repository.findById(23)).thenReturn(Optional.of(approval));
        when(syllabusAccessService.currentUser()).thenReturn(deanSecretary);
        when(repository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubMappingRepositories();

        service.review(23, review(ApprovalStatus.REJECTED, "Required revision"));

        assertThat(approval.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(approval.getReviewedBy()).isSameAs(deanSecretary);
        assertThat(syllabus.getStatus()).isEqualTo(SyllabusStatus.REJECTED);
        verify(workflowNotificationService).notifyRevisionRequested(
                syllabus, deanSecretary, "Required revision", ApprovalStep.STEP2_DEAN_SECRETARY, List.of());
        verify(syllabusHistoryService).capture(syllabus, "REJECTED", deanSecretary.getUsername());
    }

    @Test
    void deanSecretaryCanApproveSharedFinalStepWithoutActiveDean() {
        Syllabus syllabus = syllabus(SyllabusStatus.UNDER_REVIEW);
        ApprovalRequest approval = approval(24, syllabus, ApprovalStep.STEP3_DEAN, deptHead);
        when(repository.findById(24)).thenReturn(Optional.of(approval));
        when(syllabusAccessService.currentUser()).thenReturn(deanSecretary);
        when(repository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubMappingRepositories();

        service.review(24, review(ApprovalStatus.APPROVED, ""));

        assertThat(approval.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(syllabus.getApprovedBy()).isSameAs(deanSecretary);
        verify(userRepository, never()).findByRoleAndIsActiveTrue(any(UserRole.class));
        verify(repository).save(approval);
    }

    @Test
    void deanApprovalFinalizesSyllabusAndNotifiesDepartmentHead() {
        Syllabus syllabus =
                syllabus(SyllabusStatus.UNDER_REVIEW);

        ApprovalRequest deanApproval = approval(
                25,
                syllabus,
                ApprovalStep.STEP3_DEAN,
                deanSecretary);

        ApprovalRequest departmentHeadApproval =
                completedDepartmentHeadApproval(
                        15,
                        syllabus);

        when(repository.findById(25))
                .thenReturn(
                        Optional.of(deanApproval));

        when(syllabusAccessService.currentUser())
                .thenReturn(dean);

        when(repository.save(any(ApprovalRequest.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        when(repository
                .findBySyllabusIdOrderByCreatedAtDesc(
                        syllabus.getId()))
                .thenReturn(
                        List.of(
                                deanApproval,
                                departmentHeadApproval));

        stubMappingRepositories();

        service.review(
                25,
                review(
                        ApprovalStatus.APPROVED,
                        "Approved"));

        assertThat(syllabus.getStatus())
                .isEqualTo(
                        SyllabusStatus.APPROVED);

        assertThat(syllabus.getApprovedBy())
                .isEqualTo(dean);

        assertThat(syllabus.getApprovedAt())
                .isNotNull();

        assertThat(syllabus.getIsCurrent())
                .isTrue();

        assertThat(deanApproval.getStatus())
                .isEqualTo(
                        ApprovalStatus.APPROVED);

        assertThat(deanApproval.getReviewedBy())
                .isEqualTo(dean);

        verify(workflowNotificationService)
                .notifyFullyApproved(
                        eq(syllabus),
                        eq(dean),
                        eq(List.of(deptHead)));
    }

    @Test
    void deanRevisionNotifiesCreatorAndOriginalDepartmentHead() {
        Syllabus syllabus =
                syllabus(SyllabusStatus.UNDER_REVIEW);

        ApprovalRequest deanApproval = approval(
                26,
                syllabus,
                ApprovalStep.STEP3_DEAN,
                deanSecretary);

        ApprovalRequest departmentHeadApproval =
                completedDepartmentHeadApproval(
                        16,
                        syllabus);

        when(repository.findById(26))
                .thenReturn(
                        Optional.of(deanApproval));

        when(syllabusAccessService.currentUser())
                .thenReturn(dean);

        when(repository.save(any(ApprovalRequest.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        when(repository
                .findBySyllabusIdOrderByCreatedAtDesc(
                        syllabus.getId()))
                .thenReturn(
                        List.of(
                                deanApproval,
                                departmentHeadApproval));

        stubMappingRepositories();

        var response = service.review(
                26,
                review(
                        ApprovalStatus.REJECTED,
                        "Bổ sung mapping CLO-PLO"));

        assertThat(syllabus.getStatus())
                .isEqualTo(
                        SyllabusStatus.REJECTED);

        verify(workflowNotificationService)
                .notifyRevisionRequested(
                        eq(syllabus),
                        eq(dean),
                        eq("Bổ sung mapping CLO-PLO"),
                        eq(ApprovalStep.STEP3_DEAN),
                        eq(List.of(deptHead)));

        assertThat(response.getRevisionDraftId())
                .isNull();

        assertThat(
                response.getRevisionDraftVersionLabel())
                .isNull();

        verify(syllabusService, never())
                .createRevisionDraftFromRejected(
                        100);
    }

    @Test
    void legacyProgramCoordinatorStepIsNotActionable() {
        Syllabus syllabus =
                syllabus(SyllabusStatus.UNDER_REVIEW);

        ApprovalRequest approval = approval(
                27,
                syllabus,
                ApprovalStep.STEP2_PROG_COORDINATOR,
                deptHead);

        when(repository.findById(27))
                .thenReturn(Optional.of(approval));

        when(syllabusAccessService.currentUser())
                .thenReturn(dean);

        assertThatThrownBy(() ->
                service.review(
                        27,
                        review(
                                ApprovalStatus.APPROVED,
                                "")))
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessageContaining(
                        "dữ liệu lịch sử");

        assertThat(approval.getStatus())
                .isEqualTo(ApprovalStatus.PENDING);

        verify(repository, never())
                .save(any(ApprovalRequest.class));
    }


    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void departmentHeadAdvancesWithDeanOnlyOrBothFinalRoles(boolean includeSecretary) {
        Syllabus syllabus = syllabus(SyllabusStatus.SUBMITTED);
        ApprovalRequest approval = approval(30, syllabus, ApprovalStep.STEP1_DEPT_HEAD, creator);
        when(repository.findById(30)).thenReturn(Optional.of(approval));
        when(syllabusAccessService.currentUser()).thenReturn(deptHead);
        when(userRepository.findByRoleAndIsActiveTrue(UserRole.DEAN)).thenReturn(List.of(dean));
        when(userRepository.findByRoleAndIsActiveTrue(UserRole.DEAN_SECRETARY))
                .thenReturn(includeSecretary ? List.of(deanSecretary) : List.of());
        when(repository.save(any(ApprovalRequest.class))).thenAnswer(i -> i.getArgument(0));
        stubMappingRepositories();

        service.review(30, review(ApprovalStatus.APPROVED, ""));

        ArgumentCaptor<ApprovalRequest> saved = ArgumentCaptor.forClass(ApprovalRequest.class);
        verify(repository, times(2)).save(saved.capture());
        ApprovalRequest next = saved.getAllValues().get(1);
        assertThat(next.getStep()).isEqualTo(ApprovalStep.STEP3_DEAN);
        assertThat(next.getStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(next.getSyllabusVersionNumber()).isEqualTo(syllabus.getVersionNumber());
        verify(workflowNotificationService).notifyDepartmentHeadApproved(
                syllabus, deptHead, includeSecretary ? List.of(dean, deanSecretary) : List.of(dean));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = UserRole.class, names = {"DEAN", "DEAN_SECRETARY"})
    void finalRejectRequiresNonBlankComment(UserRole role) {
        ApprovalRequest approval = approval(31, syllabus(SyllabusStatus.UNDER_REVIEW),
                ApprovalStep.STEP3_DEAN, deptHead);
        when(repository.findById(31)).thenReturn(Optional.of(approval));
        when(syllabusAccessService.currentUser()).thenReturn(role == UserRole.DEAN ? dean : deanSecretary);

        for (String comment : new String[] {null, "", "  "}) {
            assertThatThrownBy(() -> service.review(31, review(ApprovalStatus.REJECTED, comment)))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("nhận xét");
        }
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsRequestResolvedWhileWaitingForWorkflowLock() {
        Syllabus syllabus = syllabus(SyllabusStatus.UNDER_REVIEW);
        ApprovalRequest approval = approval(32, syllabus, ApprovalStep.STEP3_DEAN, deptHead);
        when(repository.findById(32)).thenReturn(Optional.of(approval));
        when(syllabusAccessService.currentUser()).thenReturn(dean);
        org.mockito.Mockito.doAnswer(i -> {
            if (i.getArgument(0) == approval) approval.setStatus(ApprovalStatus.APPROVED);
            return null;
        }).when(entityManager).refresh(any(), eq(LockModeType.PESSIMISTIC_WRITE));

        assertThatThrownBy(() -> service.review(32, review(ApprovalStatus.REJECTED, "Revision")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("đã được xử lý");
        verify(repository, never()).save(any());
        verify(syllabusHistoryService, never()).capture(any(), any(), any());
    }

    @Test
    void rejectsRevisionChangedWhileWaitingForLock() {
        Syllabus syllabus = syllabus(SyllabusStatus.UNDER_REVIEW);
        ApprovalRequest approval = approval(33, syllabus, ApprovalStep.STEP3_DEAN, deptHead);
        when(repository.findById(33)).thenReturn(Optional.of(approval));
        when(syllabusAccessService.currentUser()).thenReturn(dean);
        org.mockito.Mockito.doAnswer(i -> {
            if (i.getArgument(0) == syllabus) syllabus.setVersionNumber(3);
            return null;
        }).when(entityManager).refresh(any(), eq(LockModeType.PESSIMISTIC_WRITE));

        assertThatThrownBy(() -> service.review(33, review(ApprovalStatus.APPROVED, "")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("earlier syllabus revision");
        verify(repository, never()).save(any());
    }

    @Test
    void cannotReviewAnAlreadyResolvedRequest() {
        ApprovalRequest approval = approval(34, syllabus(SyllabusStatus.APPROVED),
                ApprovalStep.STEP3_DEAN, deptHead);
        approval.setStatus(ApprovalStatus.APPROVED);
        when(repository.findById(34)).thenReturn(Optional.of(approval));

        assertThatThrownBy(() -> service.review(34, review(ApprovalStatus.APPROVED, "")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("đã được xử lý");
        verify(entityManager, never()).refresh(any(), any(LockModeType.class));
        verify(repository, never()).save(any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = SyllabusStatus.class,
            names = {"DRAFT", "SUBMITTED", "APPROVED", "REJECTED"})
    void finalReviewRejectsInvalidSyllabusState(SyllabusStatus status) {
        ApprovalRequest approval = approval(35, syllabus(status), ApprovalStep.STEP3_DEAN, deptHead);
        when(repository.findById(35)).thenReturn(Optional.of(approval));
        when(syllabusAccessService.currentUser()).thenReturn(dean);

        assertThatThrownBy(() -> service.review(35, review(ApprovalStatus.APPROVED, "")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("không còn chờ phê duyệt cuối");
        verify(repository, never()).save(any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = UserRole.class, names = {"DEAN", "DEAN_SECRETARY", "ADMIN"})
    void sharedQueueIncludesLegacyAndExcludesOutdatedOrResolvedSyllabuses(UserRole role) {
        Syllabus syllabus = syllabus(SyllabusStatus.UNDER_REVIEW);
        ApprovalRequest current = approval(36, syllabus, ApprovalStep.STEP3_DEAN, deptHead);
        ApprovalRequest legacy = approval(37, syllabus, ApprovalStep.STEP2_DEAN_SECRETARY, deptHead);
        ApprovalRequest outdated = approval(38, syllabus, ApprovalStep.STEP3_DEAN, deptHead);
        outdated.setSyllabusVersionNumber(1);
        ApprovalRequest finished = approval(39, syllabus(SyllabusStatus.APPROVED),
                ApprovalStep.STEP3_DEAN, deptHead);
        when(syllabusAccessService.currentUser()).thenReturn(user(50, "reviewer", role));
        when(repository.findByStepAndStatusOrderByCreatedAtAsc(ApprovalStep.STEP3_DEAN, ApprovalStatus.PENDING))
                .thenReturn(List.of(current, outdated, finished));
        when(repository.findByStepAndStatusOrderByCreatedAtAsc(ApprovalStep.STEP2_DEAN_SECRETARY, ApprovalStatus.PENDING))
                .thenReturn(List.of(legacy));
        stubMappingRepositories();

        assertThat(service.getPendingByStep(ApprovalStep.STEP3_DEAN))
                .extracting(com.scse.curriculum.approval.dto.ApprovalResponse::getId)
                .containsExactly(36, 37);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = UserRole.class, names = {"DEAN", "DEAN_SECRETARY"})
    void eitherFinalRoleCanRejectSharedRequestAndPreserveRevisionHistory(UserRole role) {
        Syllabus syllabus = syllabus(SyllabusStatus.UNDER_REVIEW);
        UserAccount reviewer = role == UserRole.DEAN ? dean : deanSecretary;
        ApprovalRequest approval = approval(41, syllabus, ApprovalStep.STEP3_DEAN, deptHead);
        when(repository.findById(41)).thenReturn(Optional.of(approval));
        when(syllabusAccessService.currentUser()).thenReturn(reviewer);
        when(repository.save(any(ApprovalRequest.class))).thenAnswer(i -> i.getArgument(0));
        stubMappingRepositories();

        service.review(41, review(ApprovalStatus.REJECTED, "Revise CLO mappings"));

        assertThat(syllabus.getStatus()).isEqualTo(SyllabusStatus.REJECTED);
        assertThat(syllabus.getVersionNumber()).isEqualTo(2);
        assertThat(approval.getReviewedBy()).isSameAs(reviewer);
        assertThat(approval.getComment()).isEqualTo("Revise CLO mappings");
        verify(syllabusHistoryService).capture(syllabus, "REJECTED", reviewer.getUsername());
        verify(workflowNotificationService).notifyRevisionRequested(
                syllabus, reviewer, "Revise CLO mappings", ApprovalStep.STEP3_DEAN, List.of());
        verify(repository).save(approval);
    }

    @Test
    void reviewRechecksAuthorizationAfterRefresh() {
        ApprovalRequest approval = approval(40, syllabus(SyllabusStatus.UNDER_REVIEW),
                ApprovalStep.STEP3_DEAN, deptHead);
        when(repository.findById(40)).thenReturn(Optional.of(approval));
        when(syllabusAccessService.currentUser()).thenReturn(dean);
        org.mockito.Mockito.doNothing().doThrow(
                new com.scse.curriculum.common.exception.ForbiddenOperationException("Changed review scope"))
                .when(syllabusAccessService).assertCanReview(approval);

        assertThatThrownBy(() -> service.review(40, review(ApprovalStatus.APPROVED, "")))
                .hasMessageContaining("Changed review scope");
        verify(repository, never()).save(any());
    }

    private ApprovalRequest completedDepartmentHeadApproval(
            int id,
            Syllabus syllabus) {

        ApprovalRequest approval = approval(
                id,
                syllabus,
                ApprovalStep.STEP1_DEPT_HEAD,
                creator);

        approval.setStatus(
                ApprovalStatus.APPROVED);

        approval.setReviewedBy(
                deptHead);

        return approval;
    }

    private void stubMappingRepositories() {
        when(courseProgramRepository
                .findBySyllabus_Id(100))
                .thenReturn(List.of());

        when(classSectionRepository
                .findBySyllabusId(100))
                .thenReturn(List.of());
    }

    private Syllabus syllabus(
            SyllabusStatus status) {

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
                .syllabusVersionNumber(
                        syllabus.getVersionNumber())
                .step(step)
                .status(
                        ApprovalStatus.PENDING)
                .requestedBy(requester)
                .build();
    }

    private ReviewApprovalRequest review(
            ApprovalStatus status,
            String comment) {

        ReviewApprovalRequest request =
                new ReviewApprovalRequest();

        request.setStatus(status);
        request.setComment(comment);

        return request;
    }

    private UserAccount user(
            int id,
            String username,
            UserRole role) {

        return UserAccount.builder()
                .id(id)
                .username(username)
                .email(
                        username + "@iu.edu.vn")
                .role(role)
                .isActive(true)
                .build();
    }
}
