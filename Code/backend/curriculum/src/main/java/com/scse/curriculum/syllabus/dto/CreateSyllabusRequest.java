package com.scse.curriculum.syllabus.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class CreateSyllabusRequest {
    private Integer classSectionId;
    @NotNull
    private Integer courseId;

    private Integer courseProgramId;
    
    private Integer versionNumber;

    private String versionLabel;

    @NotBlank(message = "Please select the academic year / applicable cohort")
    private String academicYear;

    private String courseDesignation;

private String courseTypes;

@NotBlank(message = "Please select the semester")
private String semester;

private String language;

private String relation;

private String teachingMethods;

private String workloadTotal;

private String workloadContact;

private String workloadPrivate;

private String prerequisites;

private String objectives;

private String examForms;

private String examRequirements;

private String rubrics;

private String major;

    /**
     * Giữ lại để tương thích payload cũ; backend không tin giá trị này.
     * Người tạo luôn được lấy từ JWT/SecurityContext.
     */
    private Integer createdBy;

    private String changeSummary;

    private String notes;

    private Integer sourceSyllabusId;

    private List<CloDTO> clos;

    private List<TopicDTO> topics;

    private List<AssessmentDTO> assessments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CloDTO {

        private String code;

        private String description;

        private String descriptionVn;

        private String competencyLevel;

        private String bloomLevel;

        private Integer orderIndex;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicDTO {

        private Integer weekNumber;

        private Integer orderInWeek;

        private String name;

        private String nameVn;

        private Integer teachingHours;

        private Integer labHours;

        private Integer selfStudyHours;

        private String topicType;

        private String teachingMethod;

        private String learningActivity;

        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssessmentDTO {

        private String name;

        private String nameVn;

        private String assessmentType;

        private Float weightPercent;

        private Float minScore;

        private Float maxScore;

        private Integer orderIndex;
    }
}