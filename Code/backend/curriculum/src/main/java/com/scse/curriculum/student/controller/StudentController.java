package com.scse.curriculum.student.controller;

import com.scse.curriculum.student.dto.CreateStudentRequest;
import com.scse.curriculum.student.dto.StudentResponse;
import com.scse.curriculum.student.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPT_HEAD', 'DEAN', 'INSTRUCTOR')")
    public List<StudentResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPT_HEAD', 'DEAN', 'INSTRUCTOR')")
    public StudentResponse getById(@PathVariable Integer id) {
        return service.getById(id);
    }

    @GetMapping("/code/{studentCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPT_HEAD', 'DEAN', 'INSTRUCTOR')")
    public StudentResponse getByStudentCode(@PathVariable String studentCode) {
        return service.getByStudentCode(studentCode);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPT_HEAD', 'DEAN', 'INSTRUCTOR')")
    public List<StudentResponse> search(@RequestParam String q) {
        return service.search(q);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public StudentResponse create(@Valid @RequestBody CreateStudentRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public StudentResponse update(@PathVariable Integer id,
                                  @Valid @RequestBody CreateStudentRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Integer id) {
        service.delete(id);
        return "Student deleted successfully.";
    }
}