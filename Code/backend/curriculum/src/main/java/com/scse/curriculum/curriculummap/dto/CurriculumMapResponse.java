package com.scse.curriculum.curriculummap.dto;

import java.util.List;

import com.scse.curriculum.courserelationship.entity.RelationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurriculumMapResponse {
private Integer programId;
private String programCode;

private Integer majorId;
private String majorCode;

private Integer cohortId;
private String cohortName;
    private String academicYear;
    private String major;
    private List<SemesterGroup> semesters;
    private List<RelationEdge> relations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SemesterGroup {
        private String semester;
        private List<CourseNode> courses;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseNode {
        private Integer courseId;
        private String courseCode;
        private String courseName;
        private String courseNameVn;
        private Integer creditTheory;
        private Integer creditLab;
        private String semester;
        private String academicYear;
        private String major;
        private String courseTypes;
        private String syllabusVersion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelationEdge {
        private Integer fromCourseId;
        private String fromCourseCode;
        private String fromCourseName;

        private Integer toCourseId;
        private String toCourseCode;
        private String toCourseName;

        private RelationType relationType;
    }
}