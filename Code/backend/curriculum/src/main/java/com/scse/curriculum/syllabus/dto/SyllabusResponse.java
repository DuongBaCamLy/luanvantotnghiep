package com.scse.curriculum.syllabus.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SyllabusResponse {

    private Integer id;

    private Integer courseId;

    private String courseCode;

    private String courseName;

    private Integer versionNumber;

    private String versionLabel;

    private Integer cohortId;

    private String cohortName;

    private String academicYear;

    // ===== General Information =====
private String courseDesignation;

private String courseTypes;

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
// ===============================

    private String status;

    private Boolean isCurrent;

    private Integer createdById;

    private String createdByUsername;

    private Integer approvedById;

    private String approvedByUsername;

    private LocalDateTime submittedAt;

    private LocalDateTime approvedAt;

    private String changeSummary;

    private String notes;

    private List<CreateSyllabusRequest.CloDTO> clos;

    private List<CreateSyllabusRequest.TopicDTO> topics;

    private List<CreateSyllabusRequest.AssessmentDTO> assessments;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}