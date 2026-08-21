package com.scse.curriculum.program.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProgramCreditValidationResponse {

    private Integer programId;
    private String programCode;

    private Integer cohortId;
    private String cohortName;

    private Integer expectedTotalCredits;
    private Integer actualTotalCredits;
    private Integer requiredCredits;
    private Integer electiveCredits;
    private Integer difference;

    private Boolean valid;
    private String message;

    private List<CourseCreditItem> courses;

    @Data
    @Builder
    public static class CourseCreditItem {
        private Integer courseProgramId;
        private Integer courseId;
        private String courseCode;
        private String courseName;
        private Integer creditTheory;
        private Integer creditLab;
        private Integer totalCredits;
        private String courseTypeName;
        private Integer semesterSuggest;
        private Integer yearSuggest;
        private Boolean required;
    }
}
