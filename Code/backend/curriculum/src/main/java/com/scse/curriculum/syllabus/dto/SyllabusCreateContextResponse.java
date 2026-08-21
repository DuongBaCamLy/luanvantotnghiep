package com.scse.curriculum.syllabus.dto;

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
}
