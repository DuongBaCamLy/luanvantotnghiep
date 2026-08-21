package com.scse.curriculum.cloplomapping.controller;

import com.scse.curriculum.cloplomapping.dto.CreateCloPloMappingRequest;
import com.scse.curriculum.cloplomapping.dto.CloPloMappingResponse;
import com.scse.curriculum.cloplomapping.service.CloPloMappingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clo-plo-mappings")
@RequiredArgsConstructor
public class CloPloMappingController {

    private final CloPloMappingService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public CloPloMappingResponse create(
            @Valid @RequestBody
            CreateCloPloMappingRequest request) {

        return service.create(request);
    }

    @GetMapping
    public List<CloPloMappingResponse> getAll() {

        return service.getAll();
    }

    @GetMapping("/{id}")
    public CloPloMappingResponse getById(
            @PathVariable Integer id) {

        return service.getById(id);
    }

    @GetMapping("/clo/{cloId}")
    public List<CloPloMappingResponse> getByClo(
            @PathVariable Integer cloId) {

        return service.getByClo(cloId);
    }

    @GetMapping("/plo/{ploId}")
    public List<CloPloMappingResponse> getByPlo(
            @PathVariable Integer ploId) {

        return service.getByPlo(ploId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public void delete(
            @PathVariable Integer id) {

        service.delete(id);
    }
}