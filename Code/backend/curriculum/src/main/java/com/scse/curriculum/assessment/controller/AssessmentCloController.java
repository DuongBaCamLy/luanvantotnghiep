package com.scse.curriculum.assessment.controller;

import com.scse.curriculum.assessment.dto.AssessmentCloRequest;
import com.scse.curriculum.assessment.dto.AssessmentCloResponse;
import com.scse.curriculum.assessment.service.AssessmentCloService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assessment-clos")
@RequiredArgsConstructor
public class AssessmentCloController {

    private final AssessmentCloService assessmentCloService;

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public AssessmentCloResponse create(
            @Valid @RequestBody AssessmentCloRequest request) {

        return assessmentCloService.create(request);
    }

    @GetMapping("/assessment/{assessmentId}")
    public List<AssessmentCloResponse> getByAssessmentComponent(
            @PathVariable Integer assessmentId) {

        return assessmentCloService
                .getByAssessmentComponent(assessmentId);
    }

    @GetMapping("/clo/{cloId}")
    public List<AssessmentCloResponse> getByClo(
            @PathVariable Integer cloId) {

        return assessmentCloService
                .getByClo(cloId);
    }

    @DeleteMapping("/{assessmentId}/{cloId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Integer assessmentId,
            @PathVariable Integer cloId) {

        assessmentCloService.delete(
                assessmentId,
                cloId);

        return ResponseEntity.noContent().build();
    }
}