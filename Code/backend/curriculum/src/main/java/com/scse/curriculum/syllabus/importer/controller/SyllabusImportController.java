package com.scse.curriculum.syllabus.importer.controller;

import com.scse.curriculum.syllabus.importer.dto.SyllabusImportPreviewResponse;
import com.scse.curriculum.syllabus.importer.dto.BulkSyllabusImportPreviewResponse;
import com.scse.curriculum.syllabus.dto.SyllabusResponse;
import com.scse.curriculum.syllabus.importer.dto.ConfirmSyllabusImportRequest;
import com.scse.curriculum.syllabus.importer.dto.CloPloReconciliationResponse;
import com.scse.curriculum.syllabus.importer.dto.BulkConfirmSyllabusImportRequest;
import jakarta.validation.Valid;
import com.scse.curriculum.syllabus.importer.service.SyllabusImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/syllabus-import")
@RequiredArgsConstructor
public class SyllabusImportController {


    private final SyllabusImportService service;



    @PostMapping(
        value="/preview",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public SyllabusImportPreviewResponse preview(
            @RequestPart("file")
            MultipartFile file
    ){

        return service.preview(file);

    }

    @PostMapping(value = "/preview-bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public BulkSyllabusImportPreviewResponse previewBulk(
            @RequestPart("file") MultipartFile file,
            @RequestParam Integer programId,
            @RequestParam Integer cohortId) {
        return service.previewBulk(file, programId, cohortId);
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public SyllabusResponse confirm(@Valid @RequestBody ConfirmSyllabusImportRequest request) {
        return service.confirm(request);
    }

    @PostMapping("/confirm-bulk-item")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public SyllabusResponse confirmBulkItem(
            @Valid @RequestBody BulkConfirmSyllabusImportRequest request) {
        return service.confirmBulkItem(request);
    }

    @PostMapping("/reconcile-clo-plo")
    @PreAuthorize("hasRole('ADMIN')")
    public CloPloReconciliationResponse reconcileCloPloMappings(
            @RequestParam Integer programId,
            @RequestParam Integer cohortId) {
        return service.reconcileCloPloMappings(programId, cohortId);
    }
}
