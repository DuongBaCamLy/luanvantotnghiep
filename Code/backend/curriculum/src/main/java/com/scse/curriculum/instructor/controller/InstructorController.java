package com.scse.curriculum.instructor.controller;

import com.scse.curriculum.instructor.dto.InstructorExportRequest;
import com.scse.curriculum.instructor.dto.InstructorRequest;
import com.scse.curriculum.instructor.dto.InstructorResponse;
import com.scse.curriculum.instructor.service.InstructorExcelExportService;
import com.scse.curriculum.instructor.service.InstructorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/instructors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InstructorController {

    private final InstructorService instructorService;
    private final InstructorExcelExportService instructorExcelExportService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InstructorResponse> createInstructor(
            @Valid @RequestBody InstructorRequest request) {

        InstructorResponse response =
                instructorService.createInstructor(request);

        return new ResponseEntity<>(
                response,
                HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<InstructorResponse>> getAllInstructors() {

        return ResponseEntity.ok(
                instructorService.getAllInstructors());
    }

    /**
     * Export the exact filtered set currently visible in the Admin UI.
     * The result is a real .xlsx workbook (not CSV) with print settings
     * already configured for A4 landscape / fit-to-width.
     */
    @PostMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportInstructors(
            @RequestBody(required = false)
            InstructorExportRequest request) {

        List<InstructorResponse> instructors =
                instructorService.getAllInstructors();

        if (
                request != null
                && request.getInstructorIds() != null
                && !request.getInstructorIds().isEmpty()
        ) {
            Set<Integer> requestedIds =
                    new HashSet<>(
                            request.getInstructorIds());

            instructors =
                    instructors
                            .stream()
                            .filter(item ->
                                    requestedIds.contains(
                                            item.getId()))
                            .toList();
        }

        byte[] workbook =
                instructorExcelExportService.export(
                        instructors,
                        request);

        String filename =
                "Instructor_List_SCSE_"
                        + LocalDateTime
                        .now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyyMMdd_HHmm"))
                        + ".xlsx";

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        headers.setContentDisposition(
                ContentDisposition
                        .attachment()
                        .filename(
                                filename,
                                StandardCharsets.UTF_8)
                        .build());

        headers.setContentLength(
                workbook.length);

        return new ResponseEntity<>(
                workbook,
                headers,
                HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InstructorResponse> getInstructorById(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                instructorService.getInstructorById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<InstructorResponse> getInstructorByUserId(
            @PathVariable Integer userId) {

        return ResponseEntity.ok(
                instructorService.getInstructorByUserId(userId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InstructorResponse> updateInstructor(
            @PathVariable Integer id,
            @Valid @RequestBody InstructorRequest request) {

        return ResponseEntity.ok(
                instructorService.updateInstructor(
                        id,
                        request));
    }

    /**
     * Legacy hard-delete endpoint.
     * Normal Admin UI uses profile activation/deactivation instead.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteInstructor(
            @PathVariable Integer id) {

        instructorService.deleteInstructor(id);

        return ResponseEntity.ok(
                "Instructor deleted successfully.");
    }
}