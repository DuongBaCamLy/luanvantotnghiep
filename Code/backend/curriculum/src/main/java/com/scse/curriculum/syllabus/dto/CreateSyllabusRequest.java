package com.scse.curriculum.syllabus.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
public class CreateSyllabusRequest {
    /** Teaching Assignment source-of-truth selected by the authenticated Instructor. */
    private Integer assignmentId;

    /** Test/source compatibility only; not part of the HTTP contract. */
    @Deprecated
    @JsonIgnore
    public Integer getClassSectionId() { return assignmentId; }

    /** Test/source compatibility only; not part of the HTTP contract. */
    @Deprecated
    @JsonIgnore
    public void setClassSectionId(Integer value) { this.assignmentId = value; }
    @NotNull
    private Integer courseId;

    private Integer courseProgramId;
    private Integer programId;
    private Integer cohortId;
    
    private Integer versionNumber;

    private String versionLabel;

    @NotBlank(message = "Please select the academic year / applicable cohort")
    private String academicYear;

    private String courseDesignation;

private String courseTypes;

@NotBlank(message = "Please select the semester")
private String semester;

/**
 * Snapshot metadata
 * Backend will fill when create
 */
private String program;


/**
 * Import information
 */
private String sourceType;

private String originalFileName;

private String originalFileType;

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

        /**
         * Stable identity used when an existing Draft is edited. It is optional
         * for create/import payloads and for backwards-compatible clients.
         */
        private Integer id;

        private String code;

        private String description;

        private String descriptionVn;

        private String competencyLevel;

        private String bloomLevel;

        private Integer orderIndex;

        public CloDTO(
                String code,
                String description,
                String descriptionVn,
                String competencyLevel,
                String bloomLevel,
                Integer orderIndex) {
            this(null, code, description, descriptionVn,
                    competencyLevel, bloomLevel, orderIndex);
        }
    }

    @Data
@NoArgsConstructor
@AllArgsConstructor
public static class TopicDTO {

    /** Stable identity for Draft updates; null for newly added topics. */
    private Integer id;


    /**
     * Planned learning activities
     * Week column in syllabus PDF
     */
    private Integer weekNumber;


    /**
     * Order of topic inside a week
     */
    private Integer orderInWeek;


    /**
     * Topic name
     */
    private String name;


    private String nameVn;


    /**
     * Hours
     */
    private Integer teachingHours;

    private Integer labHours;

    private Integer selfStudyHours;



    /**
     * Topic classification
     */
    private String topicType;


    /**
     * Teaching method
     *
     * Example:
     * Lecture
     * Discussion
     * Laboratory
     */
    private String teachingMethod;



    /**
     * Learning activities
     *
     * Example:
     * Lecture, Discussion, In-class Exercise
     */
    private String learningActivity;



    /**
     * Assessment related to this topic/week
     *
     * Example:
     * Quiz
     * Lab
     * Midterm
     * Final
     */
    private String assessments;



    /**
     * Learning resources
     *
     * Example:
     * Textbook 1
     * Reference 2
     */
    private String resources;



    private String notes;

    public TopicDTO(
            Integer weekNumber,
            Integer orderInWeek,
            String name,
            String nameVn,
            Integer teachingHours,
            Integer labHours,
            Integer selfStudyHours,
            String topicType,
            String teachingMethod,
            String learningActivity,
            String assessments,
            String resources,
            String notes) {
        this(null, weekNumber, orderInWeek, name, nameVn,
                teachingHours, labHours, selfStudyHours, topicType,
                teachingMethod, learningActivity, assessments, resources, notes);
    }
}
   @Data
@NoArgsConstructor
@AllArgsConstructor
public static class AssessmentDTO {

    /** Stable identity for Draft updates; null for newly added components. */
    private Integer id;


    /**
     * Assessment name
     */
    private String name;


    private String nameVn;



    /**
     * Assessment type
     *
     * QUIZ
     * LAB_REPORT
     * MIDTERM_EXAM
     * FINAL_EXAM
     */
    private String assessmentType;



    /**
     * Weight percentage
     */
    private Float weightPercent;



    /**
     * Score range
     */
    private Float minScore;


    private Float maxScore;



    /**
     * Display order
     */
    private Integer orderIndex;

    public AssessmentDTO(
            String name,
            String nameVn,
            String assessmentType,
            Float weightPercent,
            Float minScore,
            Float maxScore,
            Integer orderIndex) {
        this(null, name, nameVn, assessmentType,
                weightPercent, minScore, maxScore, orderIndex);
    }
}
}
