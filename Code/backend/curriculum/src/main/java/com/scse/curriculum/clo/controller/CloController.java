package com.scse.curriculum.clo.controller;

import com.scse.curriculum.clo.dto.CloResponse;
import com.scse.curriculum.clo.dto.CreateCloRequest;
import com.scse.curriculum.clo.service.CloService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clos")
@RequiredArgsConstructor
public class CloController {

    private final CloService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public CloResponse create(
            @Valid @RequestBody CreateCloRequest request) {

        return service.create(request);
    }

    @GetMapping
    public List<CloResponse> getAll() {

        return service.getAll();
    }

    @GetMapping("/{id}")
    public CloResponse getById(
            @PathVariable Integer id) {

        return service.getById(id);
    }

    @GetMapping("/syllabus/{syllabusId}")
    public List<CloResponse> getBySyllabus(
            @PathVariable Integer syllabusId) {

        return service.getBySyllabus(syllabusId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public CloResponse update(
            @PathVariable Integer id,
            @Valid @RequestBody CreateCloRequest request) {

        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id) {

        service.delete(id);

        return ResponseEntity.noContent().build();
    }
}