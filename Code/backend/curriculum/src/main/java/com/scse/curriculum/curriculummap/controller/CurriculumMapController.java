package com.scse.curriculum.curriculummap.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scse.curriculum.curriculummap.dto.CurriculumMapResponse;
import com.scse.curriculum.curriculummap.service.CurriculumMapService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/curriculum-map")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD', 'INSTRUCTOR')")
public class CurriculumMapController {

    private final CurriculumMapService curriculumMapService;

    @GetMapping
public CurriculumMapResponse generate(
        @RequestParam Integer programId,
        @RequestParam(required = false) Integer cohortId,
        @RequestParam(required = false) String semester,
        @RequestParam(required = false) String status
) {
    return curriculumMapService.generate(
            programId,
            cohortId,
            semester,
            status
    );
}
}
