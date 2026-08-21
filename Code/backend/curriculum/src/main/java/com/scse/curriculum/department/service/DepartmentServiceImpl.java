package com.scse.curriculum.department.service;

import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.department.dto.CreateDepartmentRequest;
import com.scse.curriculum.department.dto.DepartmentResponse;
import com.scse.curriculum.department.entity.Department;
import com.scse.curriculum.department.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository repository;

    @Override
    public DepartmentResponse create(
            CreateDepartmentRequest request) {

        if (repository.existsByCode(request.getCode())) {
            throw new ResourceNotFoundException("Department code already exists");
        }

        Department department = Department.builder()
                .code(request.getCode())
                .name(request.getName())
                .nameVn(request.getNameVn())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        department = repository.save(department);

        return map(department);
    }

    @Override
    public List<DepartmentResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public DepartmentResponse getById(Integer id) {

        Department department = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Department not found"));

        return map(department);
    }

    @Override
    public DepartmentResponse getByCode(String code) {

        Department department = repository.findByCode(code)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Department not found"));

        return map(department);
    }

    private DepartmentResponse map(Department department) {

        return DepartmentResponse.builder()
                .id(department.getId())
                .code(department.getCode())
                .name(department.getName())
                .nameVn(department.getNameVn())
                .isActive(department.getIsActive())
                .createdAt(department.getCreatedAt())
                .build();
    }
}