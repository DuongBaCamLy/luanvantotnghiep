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
public class SyllabusCreateContextResponse {
    private CourseContext course;
    private SyllabusResponse latestSyllabus;
    private SyllabusResponse latestApprovedSyllabus;

    /**
     * Priority-2 fallback data (Curriculum/Course context) used to prefill a
     * new syllabus when no prior syllabus exists for the course.
     */
    @Builder.Default
    private List<CourseProgramSummary> coursePrograms = new ArrayList<>();

    @Builder.Default
    private List<PloSummary> plos = new ArrayList<>();

    private DefaultsContext defaults;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseContext {
        private Integer id;
        private String courseCode;
        private String name;
        private String nameVn;
        private Integer creditTheory;
        private Integer creditLab;
        private String courseLevel;
        private String description;
        private String departmentName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseProgramSummary {
        private Integer id;
        private Integer programId;
        private String programCode;
        private String programName;
        private Integer cohortId;
        private String cohortName;
        private Integer courseTypeId;
        private String courseTypeName;
        private Integer semesterSuggest;
        private Boolean required;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PloSummary {
        private Integer id;
        private Integer programId;
        private String programCode;
        private String code;
        private String description;
    }

    /**
     * Priority-3 fallback values used only when neither a prior syllabus nor
     * curriculum data supplies a value for the field.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefaultsContext {
        private String language;
        private String teachingMethods;
        private String semester;
        private String courseTypes;
    }
}
