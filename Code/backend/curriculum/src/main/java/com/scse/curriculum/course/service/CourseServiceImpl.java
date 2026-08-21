package com.scse.curriculum.course.service;

import com.scse.curriculum.course.dto.CourseResponse;
import com.scse.curriculum.course.dto.CreateCourseRequest;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.department.entity.Department;
import com.scse.curriculum.department.repository.DepartmentRepository;
import com.scse.curriculum.course.repository.CourseRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional
public class CourseServiceImpl implements CourseService {

    private final CourseRepository repository;
    private final DepartmentRepository departmentRepository;

    @Override
    public CourseResponse create(
            CreateCourseRequest request) {

        if (repository.existsByCourseCode(
                request.getCourseCode())) {

            throw new RuntimeException(
                    "Course code already exists");
        }

        Department department =
                departmentRepository.findById(
                                request.getDepartmentId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Department not found"));

        Integer creditTheory = request.getCreditTheory();
        if (creditTheory == null) {
            creditTheory = 3;
        }

        Integer creditLab = request.getCreditLab();
        if (creditLab == null) {
            creditLab = 0;
        }

        Course course = Course.builder()
                .courseCode(request.getCourseCode())
                .name(request.getName())
                .nameVn(request.getNameVn())
                .department(department)
                .creditTheory(creditTheory)
                .creditLab(creditLab)
                .courseLevel(
                        request.getCourseLevel())
                .description(
                        request.getDescription())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        return map(
                repository.save(course));
    }

    @Override
    public List<CourseResponse> getAll() {

        return repository.findAll()
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public CourseResponse getById(
            Integer id) {

        Course course = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Course not found"));

        return map(course);
    }

    @Override
    public CourseResponse getByCourseCode(
            String courseCode) {

        Course course =
                repository.findByCourseCode(courseCode)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Course not found"));

        return map(course);
    }

    private CourseResponse map(
            Course course) {

        return CourseResponse.builder()
                .id(course.getId())
                .courseCode(course.getCourseCode())
                .name(course.getName())
                .nameVn(course.getNameVn())

                .departmentId(
                        course.getDepartment().getId())

                .departmentCode(
                        course.getDepartment().getCode())

                .departmentName(
                        course.getDepartment().getName())

                .creditTheory(
                        course.getCreditTheory())

                .creditLab(
                        course.getCreditLab())

                .totalCredits(
                        course.getCreditTheory()
                                + course.getCreditLab())

                .courseLevel(
                        course.getCourseLevel())

                .description(
                        course.getDescription())

                .isActive(
                        course.getIsActive())

                .createdAt(
                        course.getCreatedAt())
                .build();
    }
}