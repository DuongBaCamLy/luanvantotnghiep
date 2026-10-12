package com.scse.curriculum.approval.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import com.scse.curriculum.approval.dto.ApprovalHistoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scse.curriculum.approval.dto.ApprovalResponse;
import com.scse.curriculum.approval.dto.ReviewApprovalRequest;
import com.scse.curriculum.approval.entity.ApprovalRequest;
import com.scse.curriculum.approval.entity.ApprovalStatus;
import com.scse.curriculum.approval.entity.ApprovalStep;
import com.scse.curriculum.approval.repository.ApprovalRequestRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.email.WorkflowNotificationService;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.syllabus.service.SyllabusAccessService;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.classsection.entity.ClassSection;
import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.user.repository.UserAccountRepository;
import com.scse.curriculum.syllabus.dto.SyllabusResponse;
import com.scse.curriculum.syllabus.service.SyllabusService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApprovalRequestServiceImpl
                implements ApprovalRequestService {

        private final ApprovalRequestRepository repository;

        private final SyllabusRepository syllabusRepository;

        private final UserAccountRepository userRepository;

        private final EntityManager entityManager;
        private final WorkflowNotificationService workflowNotificationService;

        private final SyllabusAccessService syllabusAccessService;
        private final SyllabusService syllabusService;
        private final CourseProgramRepository courseProgramRepository;

        private final ClassSectionRepository classSectionRepository;
        private final com.scse.curriculum.syllabus.history.SyllabusHistoryService syllabusHistoryService;

        @Override
@Transactional
public ApprovalResponse review(
        Integer id,
        ReviewApprovalRequest request) {

    ApprovalRequest approval = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Approval request not found"));

    if (approval.getStatus() != ApprovalStatus.PENDING) {
        throw new IllegalStateException(
                "Yêu cầu phê duyệt này đã được xử lý.");
    }

    if (request == null
            || (request.getStatus() != ApprovalStatus.APPROVED
            && request.getStatus() != ApprovalStatus.REJECTED)) {
        throw new IllegalArgumentException(
                "Vui lòng chọn APPROVED hoặc REJECTED.");
    }

    syllabusAccessService.assertCanReview(approval);

    UserAccount reviewer = syllabusAccessService.currentUser();
    if (approval.getStep() == ApprovalStep.STEP2_PROG_COORDINATOR) {
        throw new IllegalStateException(
                "Bước Điều phối chương trình là dữ liệu lịch sử.");
    }

    boolean isRejected =
            request.getStatus() == ApprovalStatus.REJECTED;

    if (isRejected && (request.getComment() == null
            || request.getComment().isBlank())) {
        throw new IllegalArgumentException(
                "Khi từ chối phải nhập nhận xét.");
    }

    Syllabus syllabus = approval.getSyllabus();

    // Lock workflow to prevent concurrent decisions.
    syllabusRepository.lockWorkflow(syllabus.getId());

    // Refresh after locking to avoid approving an outdated request.
    entityManager.refresh(
            syllabus, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(
            approval, LockModeType.PESSIMISTIC_WRITE);

    if (approval.getStatus() != ApprovalStatus.PENDING) {
        throw new IllegalStateException(
                "Yêu cầu phê duyệt này đã được xử lý.");
    }

    syllabusAccessService.assertCanReview(approval);
    ApprovalStep currentStep = approval.getStep();

    if (approval.getSyllabusVersionNumber() == null || !java.util.Objects.equals(
            approval.getSyllabusVersionNumber(),
            syllabus.getVersionNumber())) {
        throw new IllegalStateException(
                "This approval belongs to an earlier syllabus revision.");
    }

    boolean isDepartmentReview =
            currentStep == ApprovalStep.STEP1_DEPT_HEAD;

    boolean isFinalReview =
            currentStep == ApprovalStep.STEP3_DEAN
            || currentStep == ApprovalStep.STEP2_DEAN_SECRETARY;

    if (!isDepartmentReview && !isFinalReview) {
        throw new IllegalStateException(
                "Bước phê duyệt không hợp lệ.");
    }

    if (isDepartmentReview
            && syllabus.getStatus() != SyllabusStatus.SUBMITTED) {
        throw new IllegalStateException(
                "Đề cương không còn chờ Trưởng bộ môn duyệt.");
    }

    if (isFinalReview
            && syllabus.getStatus() != SyllabusStatus.UNDER_REVIEW) {
        throw new IllegalStateException(
                "Đề cương không còn chờ phê duyệt cuối.");
    }

    // Find all eligible final reviewers before advancing STEP 1.
    List<UserAccount> finalReviewers = new ArrayList<>();

    if (isDepartmentReview && !isRejected) {
        finalReviewers.addAll(
                userRepository.findByRoleAndIsActiveTrue(
                        UserRole.DEAN));

        finalReviewers.addAll(
                userRepository.findByRoleAndIsActiveTrue(
                        UserRole.DEAN_SECRETARY));

        if (finalReviewers.isEmpty()) {
            throw new IllegalStateException(
                    "No active Dean or Dean Secretary account "
                    + "is available for final review.");
        }
    }

    approval.setReviewedBy(reviewer);
    approval.setStatus(request.getStatus());
    approval.setComment(request.getComment());
    approval.setResolvedAt(LocalDateTime.now());

    ApprovalRequest savedApproval = repository.save(approval);

    // Rejection by Department Head or final reviewer.
    if (isRejected) {
        syllabus.setStatus(SyllabusStatus.REJECTED);
        syllabus.setUpdatedAt(LocalDateTime.now());

        syllabusRepository.saveAndFlush(syllabus);

        ApprovalResponse response = map(savedApproval);

        syllabusHistoryService.capture(
                syllabus,
                "REJECTED",
                reviewer.getUsername());

        List<UserAccount> departmentHeads = isFinalReview
                ? findDepartmentHeadReviewersForVersion(syllabus)
                : List.of();

        workflowNotificationService.notifyRevisionRequested(
                syllabus,
                reviewer,
                request.getComment().trim(),
                currentStep,
                departmentHeads);

        return response;
    }

    // STEP 1: Department Head -> shared final approval queue.
    if (isDepartmentReview) {
        syllabus.setStatus(SyllabusStatus.UNDER_REVIEW);
        syllabus.setUpdatedAt(LocalDateTime.now());

        syllabusRepository.save(syllabus);

        ApprovalRequest finalRequest = ApprovalRequest.builder()
                .syllabus(syllabus)
                .syllabusVersionNumber(syllabus.getVersionNumber())
                .step(ApprovalStep.STEP3_DEAN)
                .status(ApprovalStatus.PENDING)
                .requestedBy(reviewer)
                .reviewedBy(null)
                .comment(null)
                .createdAt(LocalDateTime.now())
                .resolvedAt(null)
                .build();

        repository.save(finalRequest);

        syllabusHistoryService.capture(
                syllabus,
                "DEPT_HEAD_APPROVED",
                reviewer.getUsername());

        workflowNotificationService.notifyDepartmentHeadApproved(
                syllabus,
                reviewer,
                finalReviewers);

        return map(savedApproval);
    }

    // Final approval: Dean OR Dean Secretary.
    // Legacy pending STEP2_DEAN_SECRETARY is also accepted.
    syllabus.setStatus(SyllabusStatus.APPROVED);
    syllabus.setApprovedBy(reviewer);
    syllabus.setApprovedAt(LocalDateTime.now());
    syllabus.setIsCurrent(true);
    syllabus.setUpdatedAt(LocalDateTime.now());

    syllabusRepository.save(syllabus);

    syllabusHistoryService.capture(
            syllabus,
            "APPROVED",
            reviewer.getUsername());

    List<UserAccount> departmentHeads =
            findDepartmentHeadReviewersForVersion(syllabus);

    workflowNotificationService.notifyFullyApproved(
            syllabus,
            reviewer,
            departmentHeads);

    return map(savedApproval);
}
        private List<UserAccount> findDepartmentHeadReviewersForVersion(
                        Syllabus syllabus) {

                if (syllabus == null
                                || syllabus.getId() == null
                                || syllabus.getVersionNumber() == null) {

                        return List.of();
                }

                return repository
                                .findBySyllabusIdOrderByCreatedAtDesc(
                                                syllabus.getId())
                                .stream()
                                .filter(request -> request.getStep() == ApprovalStep.STEP1_DEPT_HEAD)
                                .filter(request -> request.getStatus() == ApprovalStatus.APPROVED)
                                .filter(request -> java.util.Objects.equals(
                                                request.getSyllabusVersionNumber(),
                                                syllabus.getVersionNumber()))
                                .map(ApprovalRequest::getReviewedBy)
                                .filter(java.util.Objects::nonNull)
                                .limit(1)
                                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public ApprovalResponse getById(
                        Integer id) {

                ApprovalRequest approval = repository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Approval request not found"));

                syllabusAccessService.assertCanAccessApproval(approval);
                return map(approval);
        }

        @Override
        public List<ApprovalResponse> getAll() {

                return repository.findAll()
                                .stream()
                                .map(this::map)
                                .toList();
        }

        @Override
@Transactional(readOnly = true)
public List<ApprovalResponse> getPendingByStep(
        ApprovalStep step) {

    syllabusAccessService.assertCanOpenPendingStep(step);

    UserAccount currentUser =
            syllabusAccessService.currentUser();

    List<ApprovalRequest> pending =
            currentUser.getRole() == UserRole.DEPT_HEAD
                    ? repository.findPendingByManagedMajor(
                            step,
                            ApprovalStatus.PENDING,
                            syllabusAccessService.currentManagedMajorId(currentUser))
                    : repository.findByStepAndStatusOrderByCreatedAtAsc(
                            step,
                            ApprovalStatus.PENDING);

    // Shared queue for Dean and Dean Secretary.
    if (step == ApprovalStep.STEP3_DEAN) {

        List<ApprovalRequest> merged = new ArrayList<>(pending);

        // Include old pending requests without changing database history.
        merged.addAll(
                repository.findByStepAndStatusOrderByCreatedAtAsc(
                        ApprovalStep.STEP2_DEAN_SECRETARY,
                        ApprovalStatus.PENDING));

        return merged.stream()
                .filter(item -> item.getSyllabusVersionNumber() != null)
                .filter(item -> item.getSyllabus() != null)
                .filter(item ->
                        item.getSyllabus().getStatus()
                                == SyllabusStatus.UNDER_REVIEW)
                .filter(item ->
                        java.util.Objects.equals(
                                item.getSyllabusVersionNumber(),
                                item.getSyllabus().getVersionNumber()))
                .sorted(Comparator.comparing(
                        ApprovalRequest::getCreatedAt,
                        Comparator.nullsLast(
                                Comparator.naturalOrder())))
                .map(this::map)
                .toList();
    }

    if (currentUser.getRole() == UserRole.DEPT_HEAD) {
        return pending.stream()
                .filter(item -> item.getSyllabusVersionNumber() != null)
                .filter(item -> item.getSyllabus() != null)
                .filter(item -> item.getSyllabus().getStatus() == SyllabusStatus.SUBMITTED)
                .filter(item -> java.util.Objects.equals(
                        item.getSyllabusVersionNumber(), item.getSyllabus().getVersionNumber()))
                .filter(item ->
                        syllabusAccessService.canView(
                                item.getSyllabus(),
                                currentUser))
                .map(this::map)
                .toList();
    }

    return pending.stream()
            .map(this::map)
            .toList();
}

        @Override
        @Transactional(readOnly = true)
        public List<ApprovalHistoryResponse> getApprovalHistory(
                        Integer syllabusId) {

                Syllabus syllabus = syllabusRepository.findByIdWithRelations(syllabusId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Syllabus not found"));

                syllabusAccessService.assertCanViewHistory(syllabus);

                return repository
                                .findBySyllabusIdOrderByCreatedAtDesc(syllabusId)
                                .stream()
                                .map(approval ->

                                ApprovalHistoryResponse.builder()
                                                .versionNumber(approval.getSyllabusVersionNumber())
                                                .versionLabel(com.scse.curriculum.syllabus.entity.SyllabusVersion
                                                                .format(approval.getSyllabusVersionNumber()))

                                                .id(
                                                                approval.getId())

                                                .step(
                                                                approval.getStep() == null
                                                                                ? null
                                                                                : approval.getStep().name())

                                                .status(
                                                                approval.getStatus() == null
                                                                                ? null
                                                                                : approval.getStatus().name())

                                                .requestedByUsername(
                                                                approval.getRequestedBy() == null
                                                                                ? null
                                                                                : approval.getRequestedBy()
                                                                                                .getUsername())

                                                .reviewedByUsername(
                                                                approval.getReviewedBy() == null
                                                                                ? null
                                                                                : approval.getReviewedBy()
                                                                                                .getUsername())

                                                .comment(
                                                                approval.getComment())

                                                .createdAt(
                                                                approval.getCreatedAt())

                                                .resolvedAt(
                                                                approval.getResolvedAt())

                                                .build()

                                )
                                .toList();

        }

        @Override
        @Transactional(readOnly = true)
        public List<ApprovalResponse> getBySyllabus(
                        Integer syllabusId) {

                Syllabus syllabus = syllabusRepository.findByIdWithRelations(syllabusId)
                                .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found"));
                syllabusAccessService.assertCanView(syllabus);

                return repository.findBySyllabusId(syllabusId)
                                .stream()
                                .map(this::map)
                                .toList();
        }

        private ApprovalResponse map(
                        ApprovalRequest approval) {
                Syllabus syllabus = approval.getSyllabus();
                CourseProgram courseProgram = courseProgramRepository
                                .findBySyllabus_Id(syllabus.getId()).stream().findFirst().orElse(null);
                ClassSection classSection = classSectionRepository
                                .findBySyllabusId(syllabus.getId()).stream().findFirst().orElse(null);
                return ApprovalResponse.builder()
                                .id(approval.getId())

                                .syllabusId(
                                                approval.getSyllabus().getId())

                                .courseCode(
                                                approval.getSyllabus()
                                                                .getCourse()
                                                                .getCourseCode())

                                .courseName(
                                                approval.getSyllabus()
                                                                .getCourse()
                                                                .getName())

                                .programCode(courseProgram != null && courseProgram.getProgram() != null
                                                ? courseProgram.getProgram().getCode()
                                                : classSection != null && classSection.getProgram() != null
                                                                ? classSection.getProgram().getCode()
                                                                : null)
                                .programName(courseProgram != null && courseProgram.getProgram() != null
                                                ? courseProgram.getProgram().getName()
                                                : classSection != null && classSection.getProgram() != null
                                                                ? classSection.getProgram().getName()
                                                                : null)
                                .cohortName(courseProgram != null && courseProgram.getCohort() != null
                                                ? courseProgram.getCohort().getName()
                                                : classSection != null && classSection.getCohort() != null
                                                                ? classSection.getCohort().getName()
                                                                : null)
                                .semester(syllabus.getSemester())
                                .instructorUsername(syllabus.getCreatedBy() == null
                                                ? null
                                                : syllabus.getCreatedBy().getUsername())
                                .departmentCode(syllabus.getCourse().getDepartment() == null
                                                ? null
                                                : syllabus.getCourse().getDepartment().getCode())
                                .departmentName(syllabus.getCourse().getDepartment() == null
                                                ? null
                                                : syllabus.getCourse().getDepartment().getName())

                                .versionNumber(
                                                approval.getSyllabusVersionNumber())

                                .versionLabel(
                                                com.scse.curriculum.syllabus.entity.SyllabusVersion
                                                                .format(approval.getSyllabusVersionNumber()))

                                .syllabusStatus(
                                                approval.getSyllabus()
                                                                .getStatus())

                                .step(approval.getStep())

                                .status(approval.getStatus())

                                .requestedById(
                                                approval.getRequestedBy().getId())

                                .requestedByUsername(
                                                approval.getRequestedBy()
                                                                .getUsername())

                                .reviewedById(
                                                approval.getReviewedBy() == null
                                                                ? null
                                                                : approval.getReviewedBy().getId())

                                .reviewedByUsername(
                                                approval.getReviewedBy() == null
                                                                ? null
                                                                : approval.getReviewedBy()
                                                                                .getUsername())

                                .comment(approval.getComment())

                                .submittedAt(
                                                approval.getCreatedAt())

                                .createdAt(approval.getCreatedAt())

                                .resolvedAt(approval.getResolvedAt())

                                .revisionDraftId(null)
                                .revisionDraftVersionLabel(null)

                                .build();
        }
}
