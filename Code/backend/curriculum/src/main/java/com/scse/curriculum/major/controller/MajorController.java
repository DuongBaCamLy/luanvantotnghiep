package com.scse.curriculum.major.controller;

import com.scse.curriculum.major.dto.CreateMajorRequest;
import com.scse.curriculum.major.dto.MajorResponse;
import com.scse.curriculum.major.service.MajorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/majors")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD', 'INSTRUCTOR')")
public class MajorController {

    private final MajorService service;

    @GetMapping
    public List<MajorResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public MajorResponse getById(
            @PathVariable Integer id) {

        return service.getById(id);
    }

    @GetMapping("/code/{code}")
    public MajorResponse getByCode(
            @PathVariable String code) {

        return service.getByCode(code);
    }

    @PostMapping
@PreAuthorize("hasRole('ADMIN')")
    public MajorResponse create(
            @Valid
            @RequestBody
            CreateMajorRequest request) {

        return service.create(request);
    }
}