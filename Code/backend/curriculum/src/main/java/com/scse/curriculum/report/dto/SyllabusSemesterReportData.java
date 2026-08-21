package com.scse.curriculum.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyllabusSemesterReportData {
    private String academicYear;
    private String semester;
    private Integer programId;
    private String programCode;
    private Integer cohortId;
    private String cohortName;
    private String status;
    @Builder.Default private List<Row> rows = new ArrayList<>();

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Row {
        private Integer syllabusId;
        private Integer courseId;
        private String courseCode;
        private String courseName;
        private String courseNameVn;
        private Integer versionNumber;
        private String versionLabel;
        private String status;
        private String instructorUsername;
        private String instructorFullName;
        private String academicYear;
        private String semester;
        private String cohortNames;
        private LocalDateTime createdAt;
        private LocalDateTime submittedAt;
        private LocalDateTime approvedAt;
        private String finalReviewerUsername;
        private String finalReviewerFullName;
        private String finalApprovalStep;
        private String finalApprovalStatus;
        private LocalDateTime finalReviewedAt;
        private String finalComment;
    }
}
