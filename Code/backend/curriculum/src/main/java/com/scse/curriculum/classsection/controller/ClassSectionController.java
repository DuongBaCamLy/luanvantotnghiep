package com.scse.curriculum.classsection.controller;

import com.scse.curriculum.classsection.dto.ClassSectionResponse;
import com.scse.curriculum.classsection.dto.CreateClassSectionRequest;
import com.scse.curriculum.classsection.service.ClassSectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/class-sections")
@RequiredArgsConstructor
public class ClassSectionController {

    private final ClassSectionService service;

    @GetMapping
@PreAuthorize("hasRole('ADMIN')")
    public List<ClassSectionResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/my-assignments")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public List<ClassSectionResponse> getMyActiveAssignments() {
        return service.getMyActiveAssignments();
    }

    @GetMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
    public ClassSectionResponse getById(@PathVariable Integer id) {
        return service.getById(id);
    }

    @GetMapping("/course/{courseId}")
@PreAuthorize("hasRole('ADMIN')")
    public List<ClassSectionResponse> getByCourseId(@PathVariable Integer courseId) {
        return service.getByCourseId(courseId);
    }

    @GetMapping("/search")
@PreAuthorize("hasRole('ADMIN')")
    public List<ClassSectionResponse> search(@RequestParam String q) {
        return service.search(q);
    }

    @PostMapping
@PreAuthorize("hasRole('ADMIN')")
    public ClassSectionResponse create(@Valid @RequestBody CreateClassSectionRequest request) {
        return service.create(request);
    }

@PutMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
    public ClassSectionResponse update(
            @PathVariable Integer id,
            @Valid @RequestBody CreateClassSectionRequest request) {
        return service.update(id, request);
    }

   @DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Integer id) {
        service.delete(id);
        return "Class section deleted successfully.";
    }
}
