package com.scse.curriculum.syllabusbook.service;

import com.scse.curriculum.syllabusbook.dto.SyllabusBookRequest;
import com.scse.curriculum.syllabusbook.dto.SyllabusBookResponse;

import java.util.List;

public interface SyllabusBookService {

    SyllabusBookResponse create(
            SyllabusBookRequest request);

    List<SyllabusBookResponse> getBySyllabus(
            Integer syllabusId);

    List<SyllabusBookResponse> getByBook(
            Integer bookId);

    void delete(
            Integer syllabusId,
            Integer bookId);
}