package com.scse.curriculum.coursetype.controller;

import com.scse.curriculum.coursetype.dto.CourseTypeResponse;
import com.scse.curriculum.coursetype.service.CourseTypeService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/course-types")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD', 'INSTRUCTOR')")
public class CourseTypeController {

    private final CourseTypeService service;

    @GetMapping
    public List<CourseTypeResponse> getAll() {

        return service.getAll();
    }

    @GetMapping("/{id}")
    public CourseTypeResponse getById(
            @PathVariable Integer id) {

        return service.getById(id);
    }

    @GetMapping("/code/{code}")
    public CourseTypeResponse getByCode(
            @PathVariable String code) {

        return service.getByCode(code);
    }
}