package com.scse.curriculum.department.controller;

import com.scse.curriculum.department.dto.CreateDepartmentRequest;
import com.scse.curriculum.department.dto.DepartmentResponse;
import com.scse.curriculum.department.service.DepartmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService service;

    @GetMapping
    public List<DepartmentResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public DepartmentResponse getById(
            @PathVariable Integer id) {

        return service.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentResponse create(
            @Valid
            @RequestBody CreateDepartmentRequest request) {

        return service.create(request);
    }

    @GetMapping("/code/{code}")
public DepartmentResponse getByCode(
        @PathVariable String code) {

    return service.getByCode(code);
}
}