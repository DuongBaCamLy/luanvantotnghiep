package com.scse.curriculum.courserelationship.service;

import com.scse.curriculum.courserelationship.dto.CourseRelationshipResponse;
import com.scse.curriculum.courserelationship.dto.CreateCourseRelationshipRequest;

import java.util.List;

public interface CourseRelationshipService {

    CourseRelationshipResponse create(CreateCourseRelationshipRequest request);

    CourseRelationshipResponse update(Integer id, CreateCourseRelationshipRequest request);

    void delete(Integer id);

    List<CourseRelationshipResponse> getAll();

    CourseRelationshipResponse getById(Integer id);

    List<CourseRelationshipResponse> getByCourseId(Integer courseId);

    List<CourseRelationshipResponse> search(String keyword);
}