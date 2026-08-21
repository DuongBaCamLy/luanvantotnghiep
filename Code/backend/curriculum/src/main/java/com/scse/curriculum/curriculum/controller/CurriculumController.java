package com.scse.curriculum.curriculum.controller;

import com.scse.curriculum.curriculum.dto.CreateCurriculumRequest;
import com.scse.curriculum.curriculum.dto.CreateCurriculumResponse;
import com.scse.curriculum.curriculum.service.CurriculumCreationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/curricula")
@RequiredArgsConstructor
public class CurriculumController {

    private final CurriculumCreationService service;

    @PostMapping
@PreAuthorize("hasRole('ADMIN')")
    public CreateCurriculumResponse create(@Valid @RequestBody CreateCurriculumRequest request) {
        return service.create(request);
    }
}
