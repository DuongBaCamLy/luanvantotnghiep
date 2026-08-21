package com.scse.curriculum.courserelationship.controller;

import com.scse.curriculum.courserelationship.dto.CourseRelationshipResponse;
import com.scse.curriculum.courserelationship.dto.CreateCourseRelationshipRequest;
import com.scse.curriculum.courserelationship.service.CourseRelationshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/course-relationships")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD', 'INSTRUCTOR')")
public class CourseRelationshipController {

    private final CourseRelationshipService service;

    @GetMapping
    public List<CourseRelationshipResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public CourseRelationshipResponse getById(@PathVariable Integer id) {
        return service.getById(id);
    }

    @GetMapping("/course/{courseId}")
    public List<CourseRelationshipResponse> getByCourseId(@PathVariable Integer courseId) {
        return service.getByCourseId(courseId);
    }

    @GetMapping("/search")
    public List<CourseRelationshipResponse> search(@RequestParam String q) {
        return service.search(q);
    }

    @PostMapping
@PreAuthorize("hasRole('ADMIN')")
    public CourseRelationshipResponse create(@Valid @RequestBody CreateCourseRelationshipRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
    public CourseRelationshipResponse update(@PathVariable Integer id,
                                             @Valid @RequestBody CreateCourseRelationshipRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Integer id) {
        service.delete(id);
        return "Course relationship deleted successfully.";
    }
}