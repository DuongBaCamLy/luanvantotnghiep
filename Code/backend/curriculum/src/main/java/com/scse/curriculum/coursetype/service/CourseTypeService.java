package com.scse.curriculum.coursetype.service;

import com.scse.curriculum.coursetype.dto.CourseTypeResponse;

import java.util.List;

public interface CourseTypeService {

    List<CourseTypeResponse> getAll();

    CourseTypeResponse getById(
            Integer id);

    CourseTypeResponse getByCode(
            String code);
}