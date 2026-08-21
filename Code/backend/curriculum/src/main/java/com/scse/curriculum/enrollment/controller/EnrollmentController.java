package com.scse.curriculum.enrollment.controller;

import com.scse.curriculum.enrollment.dto.CreateEnrollmentRequest;
import com.scse.curriculum.enrollment.dto.EnrollmentResponse;
import com.scse.curriculum.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD', 'INSTRUCTOR')")
public class EnrollmentController {

    private final EnrollmentService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD', 'INSTRUCTOR')")
    public List<EnrollmentResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD', 'INSTRUCTOR')")
    public EnrollmentResponse getById(@PathVariable Integer id) {
        return service.getById(id);
    }

    @GetMapping("/student/{studentId}")
@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD', 'INSTRUCTOR')")
    public List<EnrollmentResponse> getByStudentId(@PathVariable Integer studentId) {
        return service.getByStudentId(studentId);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD', 'INSTRUCTOR')")
    public List<EnrollmentResponse> search(@RequestParam String q) {
        return service.search(q);
    }

    @PostMapping
@PreAuthorize("hasRole('ADMIN')")
    public EnrollmentResponse create(@Valid @RequestBody CreateEnrollmentRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
    public EnrollmentResponse update(@PathVariable Integer id,
                                     @Valid @RequestBody CreateEnrollmentRequest request) {
        return service.update(id, request);
    }

   @DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Integer id) {
        service.delete(id);
        return "Enrollment deleted successfully.";
    }
}