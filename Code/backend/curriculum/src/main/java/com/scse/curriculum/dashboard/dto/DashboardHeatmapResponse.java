package com.scse.curriculum.dashboard.dto;

import lombok.Data;

import java.util.List;

@Data
public class DashboardHeatmapResponse {
    private Integer programId;
    private String programCode;
    private String programName;
    private String programNameVn;
    private Integer cohortId;
    private String cohortName;
    private Integer cohortEntryYear;
    private String academicYear;
    private String semester;
    private Integer courseTypeId;
    private String courseTypeCode;
    private String courseTypeName;
    private String courseTypeNameVn;
    private String dataSource;
    /** Stable identity of the exact dataset scope used by heatmap and exports. */
    private String scopeKey;

    /** Kept for backward compatibility with the first heatmap response. */
    private List<String> plos;
    private List<PloColumn> ploDetails;
    private List<CourseCoverage> courseCoverages;
    private HeatmapSummary summary;
    private List<HeatmapWarning> warnings;

    @Data
    public static class PloColumn {
        private Integer id;
        private String code;
        private String description;
        private String descriptionVn;
        private String category;
        private Integer versionNumber;
        private Boolean covered;
        private Integer courseCount;
        private Integer cloCount;
    }

    @Data
    public static class CourseCoverage {
        private Integer courseId;
        private String courseCode;
        private String courseName;
        private String courseNameVn;
        private Integer courseTypeId;
        private String courseTypeCode;
        private String courseTypeName;
        private String courseTypeNameVn;
        private Integer syllabusId;
        private Integer syllabusVersion;
        private String syllabusVersionLabel;
        private String syllabusAcademicYear;
        private String syllabusSemester;
        private Boolean explicitCurriculumLink;
        private Boolean hasApprovedSyllabus;
        private Integer totalClos;
        private Integer mappedClos;
        private List<String> unmappedCloCodes;

        /** Kept for backward compatibility with the first heatmap response. */
        private List<String> coverageLevels;
        private List<CellCoverage> cells;
    }

    @Data
    public static class CellCoverage {
        private Integer ploId;
        private String ploCode;
        private String level;
        private Integer mappingCount;
        private List<String> cloCodes;
    }

    @Data
    public static class HeatmapSummary {
        private Integer totalPlos;
        private Integer coveredPlos;
        private Integer uncoveredPlos;
        private Double ploCoveragePercentage;
        private Integer totalCourses;
        private Integer coursesWithApprovedSyllabus;
        private Integer coursesWithoutApprovedSyllabus;
        private Integer totalClos;
        private Integer mappedClos;
        private Integer unmappedClos;
        private Double approvedSyllabusPercentage;
        private Double cloMappingPercentage;
        private Integer errorCount;
        private Integer warningCount;
        private Integer infoCount;
    }

    @Data
    public static class HeatmapWarning {
        private String severity;
        private String code;
        private String message;
        private List<String> references;
    }
}
