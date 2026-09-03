package com.scse.curriculum.syllabus.dto;


import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;



@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyllabusResponse {



    private Integer id;



    /*
     * =====================================================
     * COURSE INFORMATION
     * =====================================================
     */


    private Integer courseId;


    private String courseCode;


    private String courseName;


    private String courseNameVn;

    private Integer creditTheory;

private Integer creditLab;





    /*
     * =====================================================
     * PROGRAM / COHORT
     * =====================================================
     */


    private Integer cohortId;

    private String cohortName;

    private Integer courseProgramId;

    private Integer programId;

    private String programCode;

    private String programName;

    private String program;


    private String major;


    private String semester;



    private String academicYear;





    /*
     * =====================================================
     * VERSION
     * =====================================================
     */


    private Integer versionNumber;


    private String versionLabel;






    /*
     * =====================================================
     * GENERAL INFORMATION
     *
     * Syllabus structure
     * =====================================================
     */


    private String courseDesignation;


    private String courseTypes;


    private String language;


    private String relation;


    private String teachingMethods;




    /*
     * =====================================================
     * WORKLOAD
     * =====================================================
     */


    private String workloadTotal;


    private String workloadContact;


    private String workloadPrivate;





    /*
     * =====================================================
     * COURSE REQUIREMENTS
     * =====================================================
     */


    private String prerequisites;


    private String objectives;


    private String examForms;


    private String examRequirements;


    private String rubrics;







    /*
     * =====================================================
     * SNAPSHOT METADATA
     *
     * Phase 1
     * =====================================================
     */


    private String courseCodeSnapshot;


    private String courseNameSnapshot;



    private String sourceType;


    private String originalFileName;


    private String originalFileType;


    private String importStatus;






    /*
     * =====================================================
     * WORKFLOW
     * =====================================================
     */


    private String status;


    private String responsibleInstructors;

    private Boolean isCurrent;


private Integer createdById;

private String createdByUsername;


private Integer approvedById;

private String approvedByUsername;


private LocalDateTime submittedAt;

private LocalDateTime approvedAt;


private String changeSummary;

private String notes;


    private String createdBy;



    private String approvedBy;



    private LocalDateTime finalApprovalDate;



    private LocalDateTime createdAt;



    private LocalDateTime updatedAt;







    /*
     * =====================================================
     * CHILD ENTITIES
     *
     * Used by:
     * - Editor
     * - Import
     * - PDF
     * - Clone
     *
     * =====================================================
     */


    @Builder.Default
private List<CreateSyllabusRequest.CloDTO> clos =
        new ArrayList<>();


@Builder.Default
private List<CreateSyllabusRequest.TopicDTO> topics =
        new ArrayList<>();


@Builder.Default
private List<CreateSyllabusRequest.AssessmentDTO> assessments =
        new ArrayList<>();








    /*
     * =====================================================
     * CLO DTO
     * =====================================================
     */


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CloResponse {


        private Integer id;


        private String code;


        private String description;


        private String descriptionVn;


        private String competencyLevel;


        private String bloomLevel;


        private Integer orderIndex;

    }








    /*
     * =====================================================
     * TOPIC DTO
     * =====================================================
     */


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicResponse {


        private Integer id;


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








    /*
     * =====================================================
     * ASSESSMENT DTO
     * =====================================================
     */


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssessmentResponse {


        private Integer id;


        private String name;


        private String nameVn;


        private String assessmentType;


        private Float weightPercent;


        private Float minScore;


        private Float maxScore;


        private Integer orderIndex;

    }


}
