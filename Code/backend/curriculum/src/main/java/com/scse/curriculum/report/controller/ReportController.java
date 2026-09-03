package com.scse.curriculum.report.controller;

import com.scse.curriculum.report.dto.PloCoverageReportData;
import com.scse.curriculum.report.service.ReportService;
import com.scse.curriculum.syllabus.pdf.SyllabusPdfMode;
import com.scse.curriculum.syllabus.pdf.SyllabusPdfResult;
import com.scse.curriculum.syllabus.pdf.SyllabusPdfService;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("@phaseRoleGuard.isAdmin(authentication)")
public class ReportController {

    private final ReportService reportService;
    private final SyllabusPdfService syllabusPdfService;

    public ReportController(
            ReportService reportService,
            SyllabusPdfService syllabusPdfService) {
        this.reportService = reportService;
        this.syllabusPdfService = syllabusPdfService;
    }

    @GetMapping("/clo-plo-matrix/excel")
    public ResponseEntity<byte[]> exportCloPloMatrixExcel(
            @RequestParam long programId,
            @RequestParam(required = false) Integer cohortId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) Integer courseTypeId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        byte[] data = reportService.generateCloPloMatrixExcel(programId, cohortId, semester, search, status);
        String filename = matrixFilename("xlsx", programId, cohortId, academicYear, semester, courseTypeId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(data);
    }

    @GetMapping("/clo-plo-matrix/pdf")
    public ResponseEntity<byte[]> exportCloPloMatrixPdf(
            @RequestParam long programId,
            @RequestParam(required = false) Integer cohortId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) Integer courseTypeId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        byte[] data = reportService.generateCloPloMatrixPdf(programId, cohortId, semester, search, status);
        String filename = matrixFilename("pdf", programId, cohortId, academicYear, semester, courseTypeId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(data);
    }

    @GetMapping("/syllabus-list/excel")
    public ResponseEntity<byte[]> exportSyllabusListExcel(
            @RequestParam String academicYear,
            @RequestParam String semester,
            @RequestParam(required = false) Integer programId,
            @RequestParam(required = false) Integer cohortId,
            @RequestParam(defaultValue = "ALL") String status) {
        byte[] data = reportService.generateSyllabusListExcel(academicYear, semester, programId, cohortId, status);
        String filename = "Syllabus_List_" + sanitizeFilenamePart(academicYear, "year") + "_HK" + sanitizeFilenamePart(semester, "term") + ".xlsx";
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff").body(data);
    }

    @GetMapping("/syllabus-list/pdf")
    public ResponseEntity<byte[]> exportSyllabusListPdf(
            @RequestParam String academicYear,
            @RequestParam String semester,
            @RequestParam(required = false) Integer programId,
            @RequestParam(required = false) Integer cohortId,
            @RequestParam(defaultValue = "ALL") String status) {
        byte[] data = reportService.generateSyllabusListPdf(academicYear, semester, programId, cohortId, status);
        String filename = "Syllabus_List_" + sanitizeFilenamePart(academicYear, "year") + "_HK" + sanitizeFilenamePart(semester, "term") + ".pdf";
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff").body(data);
    }

    @GetMapping("/plo-coverage")
    public PloCoverageReportData getPloCoverageReport(
            @RequestParam long programId,
            @RequestParam Integer cohortId,
            @RequestParam String academicYear,
            @RequestParam String semester,
            @RequestParam(required = false) Integer courseTypeId) {
        return reportService.getPloCoverageReport(
                programId, cohortId, academicYear, semester, courseTypeId);
    }

    @GetMapping("/plo-coverage/excel")
    public ResponseEntity<byte[]> exportPloCoverageExcel(
            @RequestParam long programId,
            @RequestParam Integer cohortId,
            @RequestParam String academicYear,
            @RequestParam String semester,
            @RequestParam(required = false) Integer courseTypeId) {
        byte[] data = reportService.generatePloCoverageExcel(
                programId, cohortId, academicYear, semester, courseTypeId);
        String filename = coverageFilename("xlsx", programId, cohortId, academicYear, semester, courseTypeId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(data);
    }

    @GetMapping("/plo-coverage/pdf")
    public ResponseEntity<byte[]> exportPloCoveragePdf(
            @RequestParam long programId,
            @RequestParam Integer cohortId,
            @RequestParam String academicYear,
            @RequestParam String semester,
            @RequestParam(required = false) Integer courseTypeId) {
        byte[] data = reportService.generatePloCoveragePdf(
                programId, cohortId, academicYear, semester, courseTypeId);
        String filename = coverageFilename("pdf", programId, cohortId, academicYear, semester, courseTypeId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(data);
    }

    /** FR-07.4: Export one complete syllabus using the SCSE PDF template. */
    @GetMapping(value = "/syllabuses/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportSyllabusPdf(@PathVariable Integer id) {
        SyllabusPdfResult result = syllabusPdfService.generate(id, SyllabusPdfMode.EXPORT);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(result.filename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(result.content());
    }

    private static String matrixFilename(
            String extension,
            long programId,
            Integer cohortId,
            String academicYear,
            String semester,
            Integer courseTypeId) {
        return "CLO_PLO_Matrix_P" + programId
                + "_C" + (cohortId == null ? "unknown" : cohortId)
                + "." + extension;
    }


    private static String coverageFilename(
            String extension,
            long programId,
            Integer cohortId,
            String academicYear,
            String semester,
            Integer courseTypeId) {
        return "PLO_Coverage_P" + programId
                + "_C" + (cohortId == null ? "unknown" : cohortId)
                + "_" + sanitizeFilenamePart(academicYear, "unknown-year")
                + "_HK" + sanitizeFilenamePart(semester, "unknown-semester")
                + "_" + (courseTypeId == null ? "ALL" : "GROUP-" + courseTypeId)
                + "." + extension;
    }

    private static String sanitizeFilenamePart(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String sanitized = value.trim().replaceAll("[^0-9A-Za-z-]", "_");
        return sanitized.isBlank() ? fallback : sanitized;
    }

}
