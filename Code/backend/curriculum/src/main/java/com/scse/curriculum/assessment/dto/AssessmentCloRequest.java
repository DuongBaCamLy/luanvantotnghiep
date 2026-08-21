package com.scse.curriculum.assessment.dto;

import lombok.Data;

@Data
public class AssessmentCloRequest {

    private Integer assessmentComponentId;

    private Integer cloId;

    private Float contributionPercent;
}