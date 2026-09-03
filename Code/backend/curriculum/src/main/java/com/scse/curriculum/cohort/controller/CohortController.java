package com.scse.curriculum.cohort.controller;

import com.scse.curriculum.cohort.dto.CreateCohortRequest;
import com.scse.curriculum.cohort.dto.CohortResponse;
import com.scse.curriculum.cohort.service.CohortService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cohorts")
@RequiredArgsConstructor
public class CohortController {

    private final CohortService service;

    @PostMapping
@PreAuthorize("hasRole('ADMIN')")
public CohortResponse create(
            @Valid
            @RequestBody CreateCohortRequest request) {

        return service.create(request);
    }

    @GetMapping
    public List<CohortResponse> getAll() {

        return service.getAll();
    }

    @GetMapping("/program/{programId}")
    public List<CohortResponse> getByProgram(
            @PathVariable Integer programId) {

        return service.getByProgram(programId);
    }

    @GetMapping("/{id}")
    public CohortResponse getById(
            @PathVariable Integer id) {

        return service.getById(id);
    }

    @GetMapping("/name/{name}")
    public CohortResponse getByName(
            @PathVariable String name) {

        return service.getByName(name);
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public CohortResponse archive(@PathVariable Integer id) {
        return service.archive(id);
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public CohortResponse reactivate(@PathVariable Integer id) {
        return service.reactivate(id);
    }

}
