package com.scse.curriculum.coursetype.service;

import com.scse.curriculum.coursetype.dto.CourseTypeResponse;
import com.scse.curriculum.coursetype.entity.CourseType;
import com.scse.curriculum.coursetype.repository.CourseTypeRepository;
import com.scse.curriculum.common.exception.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseTypeServiceImpl
        implements CourseTypeService {

    private final CourseTypeRepository repository;

    @Override
    public List<CourseTypeResponse> getAll() {

        return repository.findAll()
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public CourseTypeResponse getById(
            Integer id) {

        CourseType courseType =
                repository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Course Type not found"));

        return map(courseType);
    }

    @Override
    public CourseTypeResponse getByCode(
            String code) {

        CourseType courseType =
                repository.findByCode(code)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Course Type not found"));

        return map(courseType);
    }

    private CourseTypeResponse map(
            CourseType courseType) {

        return CourseTypeResponse.builder()
                .id(courseType.getId())
                .code(courseType.getCode())
                .name(courseType.getName())
                .nameVn(courseType.getNameVn())
                .build();
    }
}