package com.scse.curriculum.department.service;

import com.scse.curriculum.department.dto.CreateDepartmentRequest;
import com.scse.curriculum.department.dto.DepartmentResponse;
import com.scse.curriculum.department.dto.DepartmentHeadCandidateResponse;
import com.scse.curriculum.department.entity.Department;
import java.util.Optional;

import java.util.List;

public interface DepartmentService {

    DepartmentResponse create(CreateDepartmentRequest request);

    List<DepartmentResponse> getAll();

    DepartmentResponse getById(Integer id);

    DepartmentResponse getByCode(String code);

    List<DepartmentHeadCandidateResponse> getActiveHeadCandidates();

    DepartmentResponse assignHead(Integer departmentId, Integer userAccountId);
}
