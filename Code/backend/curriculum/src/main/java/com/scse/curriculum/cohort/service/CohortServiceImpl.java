package com.scse.curriculum.cohort.service;

import com.scse.curriculum.auth.security.CurrentUserService;
import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.approval.repository.ApprovalRequestRepository;
import com.scse.curriculum.approval.entity.ApprovalStatus;
import com.scse.curriculum.cohort.dto.CreateCohortRequest;
import com.scse.curriculum.cohort.dto.CohortResponse;
import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.program.repository.ProgramRepository;
import com.scse.curriculum.user.entity.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CohortServiceImpl
        implements CohortService {

    private final CohortRepository repository;

    private final ProgramRepository programRepository;

    private final ApprovalRequestRepository approvalRequestRepository;

    private final ClassSectionRepository classSectionRepository;

    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public CohortResponse create(
            CreateCohortRequest request) {

        if (repository.existsByProgramIdAndEntryYear(
                request.getProgramId(),
                request.getEntryYear())) {

            throw new IllegalStateException(
                    "Cohort already exists");
        }

        Program program = programRepository
                .findById(request.getProgramId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Program not found"));

        Cohort cohort = Cohort.builder()
                .program(program)
                .entryYear(request.getEntryYear())
                .name(Cohort.canonicalName(
                        program.getCode(),
                        request.getEntryYear()))
                .description(request.getDescription())
                .isActive(true)
                .archivedAt(null)
                .archivedBy(null)
                .build();

        return map(repository.save(cohort));
    }

    /*
     * Compatibility endpoint.
     *
     * Keep returning all Cohorts because existing screens currently consume
     * this API and perform their own active filtering.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CohortResponse> getAll() {

        return repository.findAll()
                .stream()
                .map(this::map)
                .toList();
    }

    /*
     * Dedicated canonical data source for Curriculum Archive.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CohortResponse> getArchived() {

        return repository.findArchived()
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CohortResponse getById(
            Integer id) {

        Cohort cohort = repository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cohort not found"));

        return map(cohort);
    }

    @Override
    @Transactional(readOnly = true)
    public CohortResponse getByName(
            String name) {

        Cohort cohort = repository
                .findByName(normalizeLookupName(name))
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cohort not found"));

        return map(cohort);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CohortResponse> getByProgram(
            Integer programId) {

        return repository
                .findByProgram_IdOrderByEntryYearDesc(programId)
                .stream()
                .map(this::map)
                .toList();
    }

    /*
     * Archive the existing Cohort record.
     *
     * Important:
     * - no Cohort copy
     * - no Program copy
     * - no curriculum data deletion
     * - archive actor is resolved from the authenticated UserAccount
     *
     * An already inactive legacy Cohort is returned unchanged. This avoids
     * inventing archive metadata for records such as CS2027 that were inactive
     * before archived_at / archived_by existed.
     */
    @Override
    @Transactional
    public CohortResponse archive(
            Integer id) {

        Cohort cohort = repository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cohort not found"));

        if (Boolean.FALSE.equals(cohort.getIsActive())) {
            return map(cohort);
        }

        long pendingApprovalCount =
                approvalRequestRepository
                        .countByCohortIdAndStatus(
                                id,
                                ApprovalStatus.PENDING);

        if (pendingApprovalCount > 0) {
            throw new IllegalStateException(
                    "Cannot archive a Cohort while syllabus approval requests are pending.");
        }

        long activeAssignmentCount =
                classSectionRepository
                        .countActiveByCohortId(id);

        if (activeAssignmentCount > 0) {
            throw new IllegalStateException(
                    "Cannot archive a Cohort while active teaching assignments exist.");
        }


        UserAccount actor =
                currentUserService.getCurrentUser();

        cohort.setIsActive(false);
        cohort.setArchivedAt(LocalDateTime.now());
        cohort.setArchivedBy(actor);

        return map(repository.save(cohort));
    }

    /*
     * Restore the SAME Cohort identity.
     *
     * Archive metadata describes the current archived state, therefore it is
     * cleared when the Cohort becomes active again.
     */
    @Override
    @Transactional
    public CohortResponse reactivate(
            Integer id) {

        Cohort cohort = repository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cohort not found"));

        cohort.setIsActive(true);
        cohort.setArchivedAt(null);
        cohort.setArchivedBy(null);

        return map(repository.save(cohort));
    }

    private CohortResponse map(
            Cohort cohort) {

        UserAccount archivedBy =
                cohort.getArchivedBy();

        return CohortResponse.builder()
                .id(cohort.getId())
                .programId(
                        cohort.getProgram().getId())
                .programCode(
                        cohort.getProgram().getCode())
                .programName(
                        cohort.getProgram().getName())
                .entryYear(
                        cohort.getEntryYear())
                .name(
                        Cohort.canonicalName(
                                cohort.getProgram().getCode(),
                                cohort.getEntryYear()))
                .description(
                        cohort.getDescription())
                .isActive(
                        cohort.getIsActive())
                .archivedAt(
                        cohort.getArchivedAt())
                .archivedByUserId(
                        archivedBy == null
                                ? null
                                : archivedBy.getId())
                .archivedByFullName(
                        archivedBy == null
                                ? null
                                : archivedBy.getFullName())
                .archivedByUsername(
                        archivedBy == null
                                ? null
                                : archivedBy.getUsername())
                .build();
    }

    private String normalizeLookupName(
            String name) {

        return name == null
                ? ""
                : name.trim()
                        .toUpperCase()
                        .replaceAll("\\s+", "");
    }
}
