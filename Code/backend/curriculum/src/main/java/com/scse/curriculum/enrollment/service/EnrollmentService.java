package com.scse.curriculum.enrollment.service;

import com.scse.curriculum.enrollment.dto.CreateEnrollmentRequest;
import com.scse.curriculum.enrollment.dto.EnrollmentResponse;

import java.util.List;

public interface EnrollmentService {

    EnrollmentResponse create(CreateEnrollmentRequest request);

    EnrollmentResponse update(Integer id, CreateEnrollmentRequest request);

    void delete(Integer id);

    List<EnrollmentResponse> getAll();

    EnrollmentResponse getById(Integer id);

    List<EnrollmentResponse> getByStudentId(Integer studentId);

    List<EnrollmentResponse> search(String keyword);
}