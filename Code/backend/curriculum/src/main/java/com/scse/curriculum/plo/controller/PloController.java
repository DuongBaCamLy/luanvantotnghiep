package com.scse.curriculum.plo.controller;

import com.scse.curriculum.plo.dto.CreatePloRequest;
import com.scse.curriculum.plo.dto.PloResponse;
import com.scse.curriculum.plo.service.PloService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plos")
@RequiredArgsConstructor
public class PloController {

    private final PloService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")
    public PloResponse create(
            @Valid
            @RequestBody CreatePloRequest request) {

        return service.create(request);
    }

    @GetMapping
    public List<PloResponse> getAll() {

        return service.getAll();
    }

    @GetMapping("/{id}")
    public PloResponse getById(
            @PathVariable Integer id) {

        return service.getById(id);
    }

    @GetMapping("/code/{code}")
    public List<PloResponse> getByCode(
            @PathVariable String code) {

        return service.getByCode(code);
    }

    @GetMapping("/program/{programId}")
    public List<PloResponse> getByProgram(
            @PathVariable Integer programId) {

        return service.getByProgram(programId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")
    public PloResponse update(
            @PathVariable Integer id,
            @Valid @RequestBody CreatePloRequest request) {

        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")
    public void delete(
            @PathVariable Integer id) {

        service.delete(id);
    }
}
