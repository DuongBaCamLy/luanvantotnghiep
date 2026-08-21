package com.scse.curriculum.program.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CurriculumTimelineResponse {

    private Integer programId;

    private String programCode;

    private String programName;

    private Integer cohortId;

    private String cohortName;

    private Integer entryYear;

    private Integer previousCohortId;

    private String previousCohortName;

    private Integer previousEntryYear;

    private boolean baseline;

    private int totalCourses;

    private int changeCount;

    private List<CourseSummaryResponse> currentCourses;

    private List<CourseChangeResponse> addedCourses;

    private List<CourseChangeResponse> removedCourses;

    private List<CourseChangeResponse> changedCourses;

    @Getter
    @Builder
    public static class CourseSummaryResponse {

        private Integer courseId;

        private String courseCode;

        private String courseName;

        private String courseType;

        private Boolean required;

        private Integer yearSuggest;

        private Integer semesterSuggest;

        private String termCode;

        private Integer syllabusId;

        private String syllabusVersionLabel;
    }

    @Getter
    @Builder
    public static class CourseChangeResponse {

        private Integer courseId;

        private String courseCode;

        private String courseName;

        private String changeType;

        private List<FieldChangeResponse> changes;
    }

    @Getter
    @Builder
    public static class FieldChangeResponse {

        private String field;

        private String label;

        private String oldValue;

        private String newValue;
    }
}