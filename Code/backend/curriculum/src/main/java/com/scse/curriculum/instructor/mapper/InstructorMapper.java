package com.scse.curriculum.instructor.mapper;

import com.scse.curriculum.department.entity.Department;
import com.scse.curriculum.department.repository.DepartmentRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.instructor.dto.InstructorRequest;
import com.scse.curriculum.instructor.dto.InstructorResponse;
import com.scse.curriculum.instructor.entity.Instructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InstructorMapper {

    private final DepartmentRepository departmentRepository;

    public Instructor toEntity(InstructorRequest request) {

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Department not found with id: "
                                + request.getDepartmentId()));

        Boolean isActive = request.getIsActive();
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }

        return Instructor.builder()
                .staffCode(request.getStaffCode())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .degree(request.getDegree())
                .academicRank(request.getAcademicRank())
                .department(department)
            .isActive(isActive)
                .build();
    }

    public InstructorResponse toResponse(Instructor instructor) {

        return InstructorResponse.builder()
                .id(instructor.getId())
                .staffCode(instructor.getStaffCode())
                .fullName(instructor.getFullName())
                .email(instructor.getEmail())
                .degree(instructor.getDegree())
                .academicRank(instructor.getAcademicRank())
                .departmentId(instructor.getDepartment().getId())
                .departmentCode(instructor.getDepartment().getCode())
                .departmentName(instructor.getDepartment().getName())
                .departmentNameVn(instructor.getDepartment().getNameVn())
                .isActive(instructor.getIsActive())
                .createdAt(instructor.getCreatedAt())
                .build();
    }

    public void updateEntity(Instructor instructor, InstructorRequest request) {

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Department not found with id: "
                                + request.getDepartmentId()));

        instructor.setStaffCode(request.getStaffCode());
        instructor.setFullName(request.getFullName());
        instructor.setEmail(request.getEmail());
        instructor.setDegree(request.getDegree());
        instructor.setAcademicRank(request.getAcademicRank());
        instructor.setDepartment(department);

        if (request.getIsActive() != null) {
            instructor.setIsActive(request.getIsActive());
        }
    }

}