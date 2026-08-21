package com.scse.curriculum.assessment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssessmentCloResponse {

    private Integer assessmentComponentId;

    private String assessmentName;

    private Integer cloId;

    private String cloCode;

    private Float contributionPercent;
}