package com.scse.curriculum.syllabus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionValidationIssue {

    private String code;
    private String section;
    private Integer tabId;
    private String field;
    private String message;
}
