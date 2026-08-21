package com.scse.curriculum.syllabus.pdf;

import java.time.LocalDateTime;
import java.util.List;

public record SyllabusPdfDocument(
        Integer syllabusId,
        String status,
        String courseCode,
        String courseName,
        String courseNameVn,
        String departmentCode,
        String departmentName,
        String academicYear,
        String semester,
        Integer versionNumber,
        String versionLabel,
        boolean currentVersion,
        String courseDesignation,
        String courseTypes,
        String language,
        String relation,
        String teachingMethods,
        String workloadTotal,
        String workloadContact,
        String workloadPrivate,
        String prerequisites,
        String objectives,
        String examForms,
        String examRequirements,
        String rubrics,
        String major,
        Integer creditTheory,
        Integer creditLab,
        String responsiblePersons,
        String createdBy,
        String approvedBy,
        LocalDateTime submittedAt,
        LocalDateTime approvedAt,
        LocalDateTime updatedAt,
        String changeSummary,
        String notes,
        List<CloRow> clos,
        List<PloColumn> plos,
        List<CloPloCell> cloPloCells,
        List<TopicRow> topics,
        List<AssessmentRow> assessments,
        List<BookRow> books) {

    public record CloRow(
            Integer id,
            String code,
            String competencyLevel,
            String bloomLevel,
            String description,
            String descriptionVn,
            Integer orderIndex) {
    }

    public record PloColumn(
            Integer id,
            String code,
            String description) {
    }

    public record CloPloCell(
            Integer cloId,
            Integer ploId,
            String level,
            Float weight,
            String notes) {
    }

    public record TopicRow(
            Integer id,
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
            String notes,
            List<String> cloCodes) {
    }

    public record AssessmentRow(
            Integer id,
            String name,
            String nameVn,
            String assessmentType,
            Float weightPercent,
            Float minScore,
            Float maxScore,
            Integer orderIndex,
            List<AssessmentCloRow> cloContributions) {
    }

    public record AssessmentCloRow(
            String cloCode,
            Float contributionPercent) {
    }

    public record BookRow(
            Integer id,
            String usageType,
            Integer orderIndex,
            String title,
            String author,
            String publisher,
            Integer year,
            String edition,
            String isbn,
            String url,
            String bookType) {
    }
}
