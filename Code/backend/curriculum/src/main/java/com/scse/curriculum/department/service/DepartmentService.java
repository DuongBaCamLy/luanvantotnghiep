package com.scse.curriculum.department.service;

import com.scse.curriculum.department.dto.CreateDepartmentRequest;
import com.scse.curriculum.department.dto.DepartmentResponse;

import java.util.List;

public interface DepartmentService {

    DepartmentResponse create(CreateDepartmentRequest request);

    List<DepartmentResponse> getAll();

    DepartmentResponse getById(Integer id);

    DepartmentResponse getByCode(String code);
}