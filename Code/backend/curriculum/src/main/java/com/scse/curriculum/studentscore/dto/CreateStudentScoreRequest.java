package com.scse.curriculum.studentscore.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateStudentScoreRequest {

    @NotNull
    private Integer enrollmentId;

    @NotNull
    private Integer assessmentComponentId;

    private Float rawScore;

    private Float finalScore;

    private Boolean isAbsent;

    private String remark;

    private Integer recordedById;
}