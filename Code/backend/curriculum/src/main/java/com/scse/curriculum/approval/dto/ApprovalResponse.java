package com.scse.curriculum.approval.dto;

import java.time.LocalDateTime;

import com.scse.curriculum.approval.entity.ApprovalStatus;
import com.scse.curriculum.approval.entity.ApprovalStep;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApprovalResponse {

    private Integer id;

    private Integer syllabusId;

    private String courseCode;

    private String courseName;

    private String programCode;

    private String programName;

    private String cohortName;

    private String semester;

    private String instructorUsername;

    private String departmentCode;

    private String departmentName;

    private Integer versionNumber;

    private String versionLabel;

    private SyllabusStatus syllabusStatus;

    private ApprovalStep step;

    private ApprovalStatus status;

    private Integer requestedById;

    private String requestedByUsername;

    private Integer reviewedById;

    private String reviewedByUsername;

    private String comment;

    private LocalDateTime submittedAt;

    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    private Integer revisionDraftId;

    private String revisionDraftVersionLabel;
}
