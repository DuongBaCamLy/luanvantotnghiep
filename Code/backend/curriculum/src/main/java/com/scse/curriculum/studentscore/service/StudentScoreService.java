package com.scse.curriculum.studentscore.service;

import com.scse.curriculum.studentscore.dto.CreateStudentScoreRequest;
import com.scse.curriculum.studentscore.dto.StudentScoreResponse;

import java.util.List;

public interface StudentScoreService {

    StudentScoreResponse create(CreateStudentScoreRequest request);

    StudentScoreResponse update(Integer id, CreateStudentScoreRequest request);

    void delete(Integer id);

    List<StudentScoreResponse> getAll();

    StudentScoreResponse getById(Integer id);

    List<StudentScoreResponse> search(String keyword);
}