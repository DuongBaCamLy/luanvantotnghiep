package com.scse.curriculum.courseprogram.controller;

import com.scse.curriculum.courseprogram.dto.*;
import com.scse.curriculum.courseprogram.service.CourseProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/course-programs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD', 'INSTRUCTOR')")
public class CourseProgramController {

    private final CourseProgramService service;

    @PostMapping
@PreAuthorize("hasRole('ADMIN')")
    public CourseProgramResponse create(
            @RequestBody CreateCourseProgramRequest request) {

        return service.create(request);
    }

    @GetMapping
    public List<CourseProgramResponse> getAll() {

        return service.getAll();
    }

    @GetMapping("/{id}")
    public CourseProgramResponse getById(
            @PathVariable Integer id) {

        return service.getById(id);
    }

    @GetMapping("/program/{programId}")
    public List<CourseProgramResponse> getByProgram(
            @PathVariable Integer programId) {

        return service.getByProgram(programId);
    }

    @GetMapping("/cohort/{cohortId}")
    public List<CourseProgramResponse> getByCohort(
            @PathVariable Integer cohortId) {

        return service.getByCohort(cohortId);
    }

    @GetMapping("/curriculum")
    public List<CourseProgramResponse> getCurriculum(
            @RequestParam Integer programId,
            @RequestParam Integer cohortId) {

        return service.getByProgramAndCohort(
                programId,
                cohortId);
    }

    @PutMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
    public CourseProgramResponse update(
            @PathVariable Integer id,
            @RequestBody UpdateCourseProgramRequest request) {
        
        return service.update(id, request);
    }

    @PutMapping("/{courseProgramId}/syllabus/{syllabusId}")
@PreAuthorize("hasRole('ADMIN')")
public CourseProgramResponse assignSyllabus(
        @PathVariable Integer courseProgramId,
        @PathVariable Integer syllabusId) {

    return service.assignSyllabus(
            courseProgramId,
            syllabusId);
}

    @DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
    public void delete(
            @PathVariable Integer id) {

        service.delete(id);
    }
}