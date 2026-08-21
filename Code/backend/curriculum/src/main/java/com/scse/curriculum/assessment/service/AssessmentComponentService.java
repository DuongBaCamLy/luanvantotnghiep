package com.scse.curriculum.assessment.service;

import com.scse.curriculum.assessment.dto.AssessmentComponentResponse;
import com.scse.curriculum.assessment.dto.CreateAssessmentComponentRequest;

import java.util.List;

public interface AssessmentComponentService {

    AssessmentComponentResponse create(CreateAssessmentComponentRequest request);

    List<AssessmentComponentResponse> getAll();

    AssessmentComponentResponse getById(Integer id);

    List<AssessmentComponentResponse> getBySyllabus(Integer syllabusId);

    AssessmentComponentResponse update(Integer id, CreateAssessmentComponentRequest request);

    void delete(Integer id);
}