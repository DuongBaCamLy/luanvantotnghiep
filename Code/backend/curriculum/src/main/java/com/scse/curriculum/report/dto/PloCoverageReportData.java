package com.scse.curriculum.report.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PloCoverageReportData {
    private Integer programId;
    private String programCode;
    private String programName;
    private String programNameVn;
    private Integer cohortId;
    private String cohortName;
    private String academicYear;
    private String semester;
    private Integer courseTypeId;
    private String courseTypeCode;
    private String courseTypeName;
    private String courseTypeNameVn;
    private String scopeKey;
    private String dataSource;
    private Summary summary = new Summary();
    private List<PloCoverageRow> plos = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    @Data
    public static class Summary {
        private Integer totalPlos = 0;
        private Integer coveredPlos = 0;
        private Integer uncoveredPlos = 0;
        private Double coveragePercentage = 0d;
        private Integer totalCourses = 0;
        private Integer contributingCourses = 0;
        private Integer totalClos = 0;
        private Integer mappedClos = 0;
        private Integer unmappedClos = 0;
    }

    @Data
    public static class PloCoverageRow {
        private Integer ploId;
        private String ploCode;
        private String description;
        private String descriptionVn;
        private String category;
        private Boolean covered = false;
        private Integer contributingCourseCount = 0;
        private Integer contributingCloCount = 0;
        private Integer introductionCount = 0;
        private Integer developmentCount = 0;
        private Integer achievementCount = 0;
        private List<CourseContribution> courses = new ArrayList<>();
    }

    @Data
    public static class CourseContribution {
        private Integer courseId;
        private String courseCode;
        private String courseName;
        private String courseNameVn;
        private String courseTypeName;
        private String courseTypeNameVn;
        private String level;
        private Integer mappingCount = 0;
        private List<String> cloCodes = new ArrayList<>();
    }
}
