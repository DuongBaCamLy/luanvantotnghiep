package com.scse.curriculum.report.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
public class CurriculumChangeReportData {
    private String formatVersion;
    private OffsetDateTime generatedAt;
    private String scopeKey;

    private Integer programId;
    private String programCode;
    private String programName;
    private String programNameVn;

    private CohortSnapshot oldCohort;
    private CohortSnapshot newCohort;
    private Summary summary;
    private List<MetadataChange> metadataChanges;
    private List<CourseChange> courseChanges;
    private List<String> warnings;

    @Data
    @Builder
    public static class CohortSnapshot {
        private Integer id;
        private String name;
        private Integer entryYear;
        private String description;
        private Boolean active;
        private Integer courseCount;
        private Integer totalCredits;
    }

    @Data
    @Builder
    public static class Summary {
        private Integer oldCourseCount;
        private Integer newCourseCount;
        private Integer addedCourses;
        private Integer removedCourses;
        private Integer modifiedCourses;
        private Integer unchangedCourses;
        private Integer oldTotalCredits;
        private Integer newTotalCredits;
        private Integer creditDifference;
        private Integer metadataChangeCount;
    }

    @Data
    @Builder
    public static class MetadataChange {
        private String field;
        private String label;
        private String oldValue;
        private String newValue;
    }

    @Data
    @Builder
    public static class CourseChange {
        private String changeType;
        private Integer courseId;
        private String courseCode;
        private String courseName;
        private String courseNameVn;
        private CourseSnapshot oldValue;
        private CourseSnapshot newValue;
        private List<FieldChange> changes;
    }

    @Data
    @Builder
    public static class CourseSnapshot {
        private Integer courseProgramId;
        private Integer creditTheory;
        private Integer creditLab;
        private Integer totalCredits;
        private Integer semesterSuggest;
        private Integer yearSuggest;
        private String termCode;
        private Boolean required;
        private Integer courseTypeId;
        private String courseTypeCode;
        private String courseTypeName;
        private String courseTypeNameVn;
    }

    @Data
    @Builder
    public static class FieldChange {
        private String field;
        private String label;
        private String oldValue;
        private String newValue;
    }
}
