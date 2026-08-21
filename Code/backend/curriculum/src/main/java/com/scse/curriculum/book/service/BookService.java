package com.scse.curriculum.book.service;

import com.scse.curriculum.book.dto.BookRequest;
import com.scse.curriculum.book.dto.BookResponse;

import java.util.List;

public interface BookService {

    BookResponse create(BookRequest request);

    BookResponse getById(Integer id);

    List<BookResponse> getAll();

    BookResponse update(
            Integer id,
            BookRequest request);

    void delete(Integer id);
}