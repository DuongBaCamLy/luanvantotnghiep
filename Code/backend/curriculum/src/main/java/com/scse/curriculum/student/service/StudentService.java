package com.scse.curriculum.student.service;

import com.scse.curriculum.student.dto.CreateStudentRequest;
import com.scse.curriculum.student.dto.StudentResponse;

import java.util.List;

public interface StudentService {

    StudentResponse create(CreateStudentRequest request);

    StudentResponse update(Integer id, CreateStudentRequest request);

    void delete(Integer id);

    List<StudentResponse> getAll();

    StudentResponse getById(Integer id);

    StudentResponse getByStudentCode(String studentCode);

    List<StudentResponse> search(String keyword);
}