package com.scse.curriculum.assessment.service;

import com.scse.curriculum.assessment.dto.AssessmentCloRequest;
import com.scse.curriculum.assessment.dto.AssessmentCloResponse;

import java.util.List;

public interface AssessmentCloService {

    AssessmentCloResponse create(
            AssessmentCloRequest request);

    List<AssessmentCloResponse> getByAssessmentComponent(
            Integer assessmentComponentId);

    List<AssessmentCloResponse> getByClo(
            Integer cloId);

    void delete(
            Integer assessmentComponentId,
            Integer cloId);
}