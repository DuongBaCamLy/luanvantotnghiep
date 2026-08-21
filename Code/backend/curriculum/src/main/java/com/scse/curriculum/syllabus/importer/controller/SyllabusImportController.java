package com.scse.curriculum.syllabus.importer.controller;

import com.scse.curriculum.syllabus.dto.SyllabusResponse;
import com.scse.curriculum.syllabus.importer.dto.ConfirmSyllabusImportRequest;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportPreviewResponse;
import com.scse.curriculum.syllabus.importer.service.SyllabusImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/syllabuses/{syllabusId}/import")
@RequiredArgsConstructor
public class SyllabusImportController {
    private final SyllabusImportService service;

    @PostMapping(value="/preview", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public SyllabusImportPreviewResponse preview(@PathVariable Integer syllabusId,
                                                  @RequestPart("file") MultipartFile file) {
        return service.preview(syllabusId, file);
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public SyllabusResponse confirm(@PathVariable Integer syllabusId,
                                     @Valid @RequestBody ConfirmSyllabusImportRequest request) {
        return service.confirm(syllabusId, request);
    }
}
