package com.scse.curriculum.syllabus.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionValidationResponse {

    private Integer syllabusId;
    private boolean valid;
    private String message;
    private int errorCount;

    private Double assessmentTotalWeight;
    private Double workloadTotal;
    private Double workloadContact;
    private Double workloadPrivate;
    private Double topicContactHours;
    private Double topicPrivateHours;

    @Builder.Default
    private List<SubmissionValidationIssue> issues = new ArrayList<>();
}
