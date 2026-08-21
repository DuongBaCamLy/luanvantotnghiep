package com.scse.curriculum.syllabusbook.controller;

import com.scse.curriculum.syllabusbook.dto.SyllabusBookRequest;
import com.scse.curriculum.syllabusbook.dto.SyllabusBookResponse;
import com.scse.curriculum.syllabusbook.service.SyllabusBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/syllabus-books")
@RequiredArgsConstructor
@CrossOrigin
public class SyllabusBookController {

    private final SyllabusBookService syllabusBookService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<SyllabusBookResponse> create(
            @RequestBody SyllabusBookRequest request) {

        return ResponseEntity.ok(
                syllabusBookService.create(request));
    }

    @GetMapping("/syllabus/{syllabusId}")
    public ResponseEntity<List<SyllabusBookResponse>> getBySyllabus(
            @PathVariable Integer syllabusId) {

        return ResponseEntity.ok(
                syllabusBookService.getBySyllabus(syllabusId));
    }

    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<SyllabusBookResponse>> getByBook(
            @PathVariable Integer bookId) {

        return ResponseEntity.ok(
                syllabusBookService.getByBook(bookId));
    }

    @DeleteMapping("/{syllabusId}/{bookId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Void> delete(
            @PathVariable Integer syllabusId,
            @PathVariable Integer bookId) {

        syllabusBookService.delete(
                syllabusId,
                bookId);

        return ResponseEntity.noContent().build();
    }
}