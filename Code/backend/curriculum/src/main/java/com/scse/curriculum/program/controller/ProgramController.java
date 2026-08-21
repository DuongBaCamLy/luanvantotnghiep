package com.scse.curriculum.program.controller;

import com.scse.curriculum.program.dto.CloneProgramRequest;
import com.scse.curriculum.program.dto.CloneProgramResponse;
import com.scse.curriculum.program.dto.CreateProgramRequest;
import com.scse.curriculum.program.dto.CurriculumTimelineResponse;
import com.scse.curriculum.program.dto.ProgramCreditValidationResponse;
import com.scse.curriculum.program.dto.ProgramResponse;
import com.scse.curriculum.program.dto.UpdateProgramRequest;
import com.scse.curriculum.program.dto.ProgramArchiveValidationResponse;
import com.scse.curriculum.program.service.ProgramService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/programs")
@RequiredArgsConstructor
public class ProgramController {

    private final ProgramService service;

    @GetMapping
    public List<ProgramResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ProgramResponse getById(
            @PathVariable Integer id) {

        return service.getById(id);
    }

    @GetMapping("/code/{code}")
    public ProgramResponse getByCode(
            @PathVariable String code) {

        return service.getByCode(code);
    }

    @PostMapping
@PreAuthorize("hasRole('ADMIN')")
    public ProgramResponse create(
            @Valid
            @RequestBody
            CreateProgramRequest request) {

        return service.create(request);
    }

    @PutMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
    public ProgramResponse update(@PathVariable Integer id, @Valid @RequestBody UpdateProgramRequest request) {
        return service.update(id, request);
    }

   @GetMapping("/{id}/archive-validation")
@PreAuthorize("hasRole('ADMIN')")
    public ProgramArchiveValidationResponse validateArchive(@PathVariable Integer id) {
        return service.validateArchive(id);
    }

   @PostMapping("/{id}/archive")
@PreAuthorize("hasRole('ADMIN')")
    public ProgramResponse archive(@PathVariable Integer id) {
        return service.archive(id);
    }

    @PostMapping("/{id}/reactivate")
@PreAuthorize("hasRole('ADMIN')")
    public ProgramResponse reactivate(@PathVariable Integer id) {
        return service.reactivate(id);
    }

    @GetMapping("/{id}/diff")
    public com.scse.curriculum.program.dto.ProgramDiffResponse getDiff(
            @PathVariable Integer id,
            @RequestParam Integer oldCohortId,
            @RequestParam Integer newCohortId) {

        return service.getDiff(id, oldCohortId, newCohortId);
    }
/**
 * FR-06.7:
 * Timeline lịch sử cập nhật CTĐT qua từng cohort.
 */
@GetMapping("/{id}/curriculum-timeline")
@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD')")
public List<CurriculumTimelineResponse>
        getCurriculumTimeline(
                @PathVariable Integer id) {

    return service.getCurriculumTimeline(id);
}
    @GetMapping("/{id}/credit-validation")
    public ProgramCreditValidationResponse validateCredits(
            @PathVariable Integer id,
            @RequestParam Integer cohortId) {

        return service.validateCredits(id, cohortId);
    }

    @PostMapping("/{id}/clone-cohort")
@PreAuthorize("hasRole('ADMIN')")
    public CloneProgramResponse cloneToCohort(
            @PathVariable Integer id,
            @Valid @RequestBody CloneProgramRequest request) {

        return service.cloneToCohort(id, request);
    }

}
