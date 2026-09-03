package com.scse.curriculum.book.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.scse.curriculum.book.dto.BookRequest;
import com.scse.curriculum.book.dto.BookResponse;
import com.scse.curriculum.book.service.BookService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD', 'INSTRUCTOR')")
public class BookController {

    private final BookService bookService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public BookResponse create(
            @RequestBody BookRequest request) {

        return bookService.create(request);
    }

    @GetMapping
    public List<BookResponse> getAll() {

        return bookService.getAll();
    }

    @GetMapping("/{id}")
    public BookResponse getById(
            @PathVariable Integer id) {

        return bookService.getById(id);
    }

    @PutMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
    public BookResponse update(
            @PathVariable Integer id,
            @RequestBody BookRequest request) {

        return bookService.update(
                id,
                request);
    }

    @DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
    public void delete(
            @PathVariable Integer id) {

        bookService.delete(id);
    }
}
