package com.scse.curriculum.assessment.dto;

import lombok.Data;

@Data
public class CreateAssessmentComponentRequest {

    private Integer syllabusId;

    private String name;

    private String nameVn;

    private String assessmentType;

    private Float weightPercent;

    private Float minScore;

    private Float maxScore;

    private Integer orderIndex;
}
