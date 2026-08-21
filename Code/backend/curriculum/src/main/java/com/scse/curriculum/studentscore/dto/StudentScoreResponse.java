package com.scse.curriculum.studentscore.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StudentScoreResponse {

    private Integer id;
    private Integer enrollmentId;
    private Integer assessmentComponentId;
    private String assessmentComponentName;
    private Float rawScore;
    private Float finalScore;
    private Boolean isAbsent;
    private String remark;
    private LocalDateTime recordedAt;
    private Integer recordedById;
}