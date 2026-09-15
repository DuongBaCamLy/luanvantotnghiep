package com.scse.curriculum.syllabus.comparison.controller;

import com.scse.curriculum.syllabus.comparison.dto.SemanticProviderStatusResponse;
import com.scse.curriculum.syllabus.comparison.service.SemanticProviderStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provider diagnostics for the syllabus semantic-comparison feature.
 *
 * This endpoint never returns the provider API key.
 */
@RestController
@RequestMapping(
        "/api/syllabuses/diff/semantic")
@RequiredArgsConstructor
@PreAuthorize(
        "hasAnyRole('DEPT_HEAD','DEAN','ADMIN','INSTRUCTOR')")
public class SemanticProviderStatusController {

    private final SemanticProviderStatusService service;

    @GetMapping("/provider-status")
    public SemanticProviderStatusResponse getStatus() {
        return service.getStatus();
    }
}
