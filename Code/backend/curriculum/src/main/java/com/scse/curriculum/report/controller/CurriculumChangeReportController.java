package com.scse.curriculum.report.controller;

import com.scse.curriculum.report.dto.CurriculumChangeReportData;
import com.scse.curriculum.report.service.CurriculumChangeReportService;
import com.scse.curriculum.syllabus.pdf.SyllabusPdfFontProvider;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/reports/curriculum-change")
@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD')")
public class CurriculumChangeReportController {
    private final CurriculumChangeReportService service;
    private final SyllabusPdfFontProvider fonts;

    public CurriculumChangeReportController(CurriculumChangeReportService service, SyllabusPdfFontProvider fonts) {
        this.service = service;
        this.fonts = fonts;
    }

    @GetMapping
    public CurriculumChangeReportData get(@RequestParam Integer programId,
                                          @RequestParam Integer oldCohortId,
                                          @RequestParam Integer newCohortId) {
        return service.build(programId, oldCohortId, newCohortId);
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> excel(@RequestParam Integer programId,
                                        @RequestParam Integer oldCohortId,
                                        @RequestParam Integer newCohortId) {
        CurriculumChangeReportData data = service.build(programId, oldCohortId, newCohortId);
        byte[] content = com.scse.curriculum.report.service.CurriculumChangeReportExporter.excel(data);
        return download(content, filename(data, "xlsx"), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf(@RequestParam Integer programId,
                                      @RequestParam Integer oldCohortId,
                                      @RequestParam Integer newCohortId) {
        CurriculumChangeReportData data = service.build(programId, oldCohortId, newCohortId);
        byte[] content = com.scse.curriculum.report.service.CurriculumChangeReportExporter.pdf(data, fonts);
        return download(content, filename(data, "pdf"), MediaType.APPLICATION_PDF_VALUE);
    }

    private static ResponseEntity<byte[]> download(byte[] content, String filename, String mediaType) {
        ContentDisposition disposition = ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(mediaType))
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff").body(content);
    }

    private static String filename(CurriculumChangeReportData data, String extension) {
        return "Curriculum_Change_" + safe(data.getProgramCode()) + "_"
                + safe(data.getOldCohort().getName()) + "_to_" + safe(data.getNewCohort().getName()) + "." + extension;
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) return "unknown";
        return value.trim().replaceAll("[^0-9A-Za-z-]", "_");
    }
}
