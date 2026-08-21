package com.scse.curriculum.assessment.controller;

import com.scse.curriculum.assessment.dto.*;
import com.scse.curriculum.assessment.service.AssessmentComponentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assessment-components")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD', 'INSTRUCTOR')")
public class AssessmentComponentController {

    private final AssessmentComponentService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public AssessmentComponentResponse create(
            @RequestBody CreateAssessmentComponentRequest request) {

        return service.create(request);
    }

    @GetMapping
    public List<AssessmentComponentResponse> getAll() {

        return service.getAll();
    }

    @GetMapping("/{id}")
    public AssessmentComponentResponse getById(
            @PathVariable Integer id) {

        return service.getById(id);
    }

    @GetMapping("/syllabus/{syllabusId}")
    public List<AssessmentComponentResponse> getBySyllabus(
            @PathVariable Integer syllabusId) {

        return service.getBySyllabus(syllabusId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public AssessmentComponentResponse update(
            @PathVariable Integer id,
            @RequestBody CreateAssessmentComponentRequest request) {

        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public void delete(
            @PathVariable Integer id) {

        service.delete(id);
    }
}