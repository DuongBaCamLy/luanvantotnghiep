package com.scse.curriculum.course.service;

import com.scse.curriculum.course.dto.CourseResponse;
import com.scse.curriculum.course.dto.CreateCourseRequest;

import java.util.List;

public interface CourseService {

    CourseResponse create(
            CreateCourseRequest request);

    List<CourseResponse> getAll();

    CourseResponse getById(
            Integer id);

    CourseResponse getByCourseCode(
            String courseCode);
}