package com.scse.curriculum.syllabus.controller;

import com.scse.curriculum.syllabus.dto.CloneSyllabusRequest;
import com.scse.curriculum.syllabus.dto.CreateSyllabusRequest;
import com.scse.curriculum.syllabus.dto.SyllabusDiffResponse;
import com.scse.curriculum.syllabus.dto.SyllabusResponse;
import com.scse.curriculum.syllabus.dto.SyllabusCreateContextResponse;
import com.scse.curriculum.syllabus.dto.SubmissionValidationResponse;
import com.scse.curriculum.syllabus.service.SyllabusService;
import com.scse.curriculum.syllabus.pdf.SyllabusPdfMode;
import com.scse.curriculum.syllabus.pdf.SyllabusPdfResult;
import com.scse.curriculum.syllabus.pdf.SyllabusPdfService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/syllabuses")
public class SyllabusController {

    private final SyllabusService service;
    private final SyllabusPdfService syllabusPdfService;

    public SyllabusController(
            SyllabusService service,
            SyllabusPdfService syllabusPdfService) {
        this.service = service;
        this.syllabusPdfService = syllabusPdfService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public SyllabusResponse create(
            @Valid @RequestBody CreateSyllabusRequest request) {

        return service.create(request);
    }

    @GetMapping
    public List<SyllabusResponse> getAll() {

        return service.getAll();
    }

    @GetMapping("/create-context")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public SyllabusCreateContextResponse createContext(@RequestParam Integer courseId) {
        return service.getCreateContext(courseId);
    }

    @GetMapping("/{id}")
    public SyllabusResponse getById(
            @PathVariable Integer id) {

        return service.getById(id);
    }

    @GetMapping("/course/{courseId}")
    public List<SyllabusResponse> getByCourse(
            @PathVariable Integer courseId) {

        return service.getByCourse(courseId);
    }

    @GetMapping("/status/{status}")
    public List<SyllabusResponse> getByStatus(
            @PathVariable String status) {

        return service.getByStatus(status);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public SyllabusResponse update(
            @PathVariable Integer id,
            @Valid @RequestBody CreateSyllabusRequest request) {

        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id) {

        service.delete(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/submission-validation")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public SubmissionValidationResponse validateForSubmit(
            @PathVariable Integer id) {

        return service.validateForSubmit(id);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public SyllabusResponse submit(
            @PathVariable Integer id) {

        return service.submit(id);
    }


    @GetMapping("/{id}/diff")
    @PreAuthorize("hasAnyRole('DEPT_HEAD', 'DEAN', 'ADMIN', 'INSTRUCTOR')")
    public SyllabusDiffResponse getDiff(
            @PathVariable Integer id,
            @RequestParam Integer compareWith) {

        return service.getDiff(compareWith, id);
    }

    @PostMapping("/{id}/clone")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public SyllabusResponse clone(
            @PathVariable Integer id,
            @RequestBody CloneSyllabusRequest request) {

        return service.clone(id, request);
    }
    @GetMapping(value = "/{id}/pdf/preview", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN', 'DEPT_HEAD', 'DEAN')")
    public ResponseEntity<byte[]> previewPdf(@PathVariable Integer id) {
        SyllabusPdfResult result = syllabusPdfService.generate(id, SyllabusPdfMode.PREVIEW);
        return pdfResponse(result, false);
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN', 'DEPT_HEAD', 'DEAN')")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Integer id) {
        SyllabusPdfResult result = syllabusPdfService.generate(id, SyllabusPdfMode.EXPORT);
        return pdfResponse(result, true);
    }

    private ResponseEntity<byte[]> pdfResponse(SyllabusPdfResult result, boolean attachment) {
        ContentDisposition disposition = (attachment
                ? ContentDisposition.attachment()
                : ContentDisposition.inline())
                .filename(result.filename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(result.content());
    }

}