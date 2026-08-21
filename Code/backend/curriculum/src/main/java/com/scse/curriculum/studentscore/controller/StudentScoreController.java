package com.scse.curriculum.studentscore.controller;

import com.scse.curriculum.studentscore.dto.CreateStudentScoreRequest;
import com.scse.curriculum.studentscore.dto.StudentScoreResponse;
import com.scse.curriculum.studentscore.service.StudentScoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student-scores")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD', 'INSTRUCTOR')")
public class StudentScoreController {

    private final StudentScoreService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD', 'INSTRUCTOR')")
    public List<StudentScoreResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD', 'INSTRUCTOR')")
    public StudentScoreResponse getById(@PathVariable Integer id) {
        return service.getById(id);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD', 'INSTRUCTOR')")
    public List<StudentScoreResponse> search(@RequestParam String q) {
        return service.search(q);
    }

    @PostMapping
@PreAuthorize("hasRole('ADMIN')")
    public StudentScoreResponse create(@Valid @RequestBody CreateStudentScoreRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public StudentScoreResponse update(@PathVariable Integer id,
                                       @Valid @RequestBody CreateStudentScoreRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Integer id) {
        service.delete(id);
        return "Student score deleted successfully.";
    }
}