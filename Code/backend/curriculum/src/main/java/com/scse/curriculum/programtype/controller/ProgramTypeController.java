package com.scse.curriculum.programtype.controller;

import com.scse.curriculum.programtype.dto.CreateProgramTypeRequest;
import com.scse.curriculum.programtype.dto.ProgramTypeResponse;
import com.scse.curriculum.programtype.service.ProgramTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/program-types")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD', 'INSTRUCTOR')")
public class ProgramTypeController {

    private final ProgramTypeService service;

    @GetMapping
    public List<ProgramTypeResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ProgramTypeResponse getById(
            @PathVariable Integer id) {

        return service.getById(id);
    }

    @GetMapping("/code/{code}")
    public ProgramTypeResponse getByCode(
            @PathVariable String code) {

        return service.getByCode(code);
    }

    @PostMapping
@PreAuthorize("hasRole('ADMIN')")
    public ProgramTypeResponse create(
            @Valid
            @RequestBody
            CreateProgramTypeRequest request) {

        return service.create(request);
    }
}