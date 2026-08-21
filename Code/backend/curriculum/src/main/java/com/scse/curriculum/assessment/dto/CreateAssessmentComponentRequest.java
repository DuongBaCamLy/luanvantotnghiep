package com.scse.curriculum.assessment.dto;

import com.scse.curriculum.assessment.entity.AssessmentType;
import lombok.Data;

@Data
public class CreateAssessmentComponentRequest {

    private Integer syllabusId;

    private String name;

    private String nameVn;

    private AssessmentType assessmentType;

    private Float weightPercent;

    private Float minScore;

    private Float maxScore;

    private Integer orderIndex;
}