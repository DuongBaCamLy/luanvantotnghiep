package com.scse.curriculum.classsection.service;

import com.scse.curriculum.classsection.dto.ClassSectionResponse;
import com.scse.curriculum.classsection.dto.CreateClassSectionRequest;

import java.util.List;

public interface ClassSectionService {

    ClassSectionResponse create(CreateClassSectionRequest request);
    ClassSectionResponse update(Integer id, CreateClassSectionRequest request);
    void delete(Integer id);
    List<ClassSectionResponse> getAll();
    ClassSectionResponse getById(Integer id);
    List<ClassSectionResponse> getByCourseId(Integer courseId);
    List<ClassSectionResponse> search(String keyword);
    List<ClassSectionResponse> getMyActiveAssignments();
}
