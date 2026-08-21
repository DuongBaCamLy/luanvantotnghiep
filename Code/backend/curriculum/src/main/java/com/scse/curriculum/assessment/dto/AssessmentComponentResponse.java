package com.scse.curriculum.assessment.dto;

import com.scse.curriculum.assessment.entity.AssessmentType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssessmentComponentResponse {

    private Integer id;

    private Integer syllabusId;

    private String courseCode;

    private String name;

    private String nameVn;

    private AssessmentType assessmentType;

    private Float weightPercent;

    private Float minScore;

    private Float maxScore;

    private Integer orderIndex;
}