package com.scse.curriculum.course.controller;

import com.scse.curriculum.course.dto.CourseResponse;
import com.scse.curriculum.course.dto.CreateCourseRequest;
import com.scse.curriculum.course.service.CourseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService service;

    @GetMapping
    public List<CourseResponse> getAll() {

        return service.getAll();
    }

    @GetMapping("/{id}")
    public CourseResponse getById(
            @PathVariable Integer id) {

        return service.getById(id);
    }

    @GetMapping("/code/{courseCode}")
    public CourseResponse getByCourseCode(
            @PathVariable String courseCode) {

        return service.getByCourseCode(courseCode);
    }

    @PostMapping
@PreAuthorize("hasRole('ADMIN')")
public CourseResponse create(
            @Valid
            @RequestBody
            CreateCourseRequest request) {

        return service.create(request);
    }
}