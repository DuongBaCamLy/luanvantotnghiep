package com.scse.curriculum.syllabus.dto;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

/**
 * Canonical structural diff between two logical syllabuses.
 *
 * The response is organized by the same data groups used by the
 * standard syllabus form. Scalar fields are returned as section maps,
 * while repeating rows/matrices are returned as ListDiff values.
 *
 * Important:
 * - Raw Syllabus.notes JSON must never be exposed as one diff field.
 * - Relation-backed matrices/readings remain canonical in their
 *   dedicated diff collections to avoid double-counting JSON mirrors.
 */
@Data
@Builder
public class SyllabusDiffResponse {

    private Integer oldSyllabusId;
    private Integer newSyllabusId;
    private String oldVersionLabel;
    private String newVersionLabel;
    private boolean hasChanges;

    /*
     * ============================================================
     * CANONICAL SCALAR FORM SECTIONS
     * ============================================================
     */

    /**
     * General Information.
     *
     * Expected keys include:
     * courseName, courseNameVn, academicYear, courseDesignation,
     * courseTypes, semester, personResponsible, language, relation,
     * teachingMethods, major.
     */
    private Map<String, FieldDiff> generalInfoDiff;

    /**
     * Workload and Credit Points.
     *
     * Expected keys include:
     * workloadTotal, workloadContact, workloadPrivate,
     * workloadStudentResponsibility, creditPoints,
     * lectureCredits, laboratoryCredits.
     */
    private Map<String, FieldDiff> workloadCreditDiff;

    /**
     * Requirements and Course Objectives.
     *
     * Expected keys include:
     * prerequisites, objectives.
     */
    private Map<String, FieldDiff> requirementsDiff;

    /**
     * Content-level scalar information.
     *
     * Expected keys include:
     * contentNote.
     */
    private Map<String, FieldDiff> contentDiff;

    /**
     * Assessment-level scalar information that is not an assessment row.
     *
     * Expected keys include:
     * assessmentPassNote.
     */
    private Map<String, FieldDiff> assessmentInfoDiff;

    /**
     * Examination and Study Requirements.
     *
     * Expected keys include:
     * examForms, examRequirements, rubrics.
     */
    private Map<String, FieldDiff> examinationDiff;

    /**
     * Revision Information.
     *
     * Expected keys include:
     * dateRevised, internalNotes, changeSummary.
     */
    private Map<String, FieldDiff> revisionInfoDiff;

    /*
     * ============================================================
     * CANONICAL REPEATING / MATRIX FORM SECTIONS
     * ============================================================
     */

    private ListDiff<CloDiff> cloDiff;

    /**
     * Canonical CLO-PLO relation diff.
     *
     * Do not duplicate the same matrix from notes.cloPloMatrix.
     */
    private ListDiff<CloPloMappingDiff> cloPloDiff;

    private ListDiff<TopicDiff> topicDiff;

    /**
     * Canonical Topic-CLO relation diff.
     *
     * Do not duplicate the same relationship from supplemental JSON.
     */
    private ListDiff<TopicCloMappingDiff> topicCloDiff;

    /**
     * Planned Learning Activities stored in the standard syllabus
     * supplemental JSON.
     */
    private ListDiff<PlannedActivityDiff> plannedActivityDiff;

    private ListDiff<AssessmentDiff> assessmentDiff;

    /**
     * Canonical Assessment-CLO relation diff.
     *
     * Do not duplicate the same matrix from notes.assessmentCloMatrix.
     */
    private ListDiff<AssessmentCloMappingDiff> assessmentCloDiff;

    /**
     * Canonical reading-list relation diff.
     *
     * Relation-backed Book/SyllabusBook data is authoritative when it
     * exists; notes.readings is only a supplemental/legacy source.
     */
    private ListDiff<readingsDiff> readingsDiff;

    @Data
    @Builder
    public static class FieldDiff {
        private String oldValue;
        private String newValue;
    }

    @Data
    @Builder
    public static class ListDiff<T> {
        private List<T> added;
        private List<T> removed;
        private List<T> modified;
    }

    /*
     * ============================================================
     * CLO
     * ============================================================
     */

    @Data
    @Builder
    public static class CloDiff {
        private String code;
        private String description;
        private String descriptionVn;
        private String competencyLevel;
        private String bloomLevel;
        private Integer orderIndex;

        /**
         * Kept for backward compatibility with the current structural
         * comparison response. The canonical matrix itself is cloPloDiff.
         */
        private List<String> plos;

        private Map<String, FieldDiff> changes;
    }

    @Data
    @Builder
    public static class CloPloMappingDiff {
        private String cloCode;
        private String ploCode;
        private String level;
        private Float contributionWeight;
        private String notes;
        private Map<String, FieldDiff> changes;
    }

    /*
     * ============================================================
     * CONTENT / TOPICS
     * ============================================================
     */

    @Data
    @Builder
    public static class TopicDiff {
        private String name;
        private String nameVn;
        private Integer weekNumber;
        private Integer orderInWeek;

        private Integer teachingHours;
        private Integer labHours;
        private Integer selfStudyHours;

        private String topicType;
        private String teachingMethod;
        private String learningActivity;

        /**
         * Standard content-table supplemental values.
         *
         * Topic-CLO itself remains canonical in topicCloDiff, so there is
         * intentionally no duplicated "clo" field here.
         */
        private String assessments;
        private String resources;
        private String contentWeight;
        private String contentLevel;

        private Map<String, FieldDiff> changes;
    }

    @Data
    @Builder
    public static class TopicCloMappingDiff {
        private String topicName;
        private Integer weekNumber;
        private Integer orderInWeek;
        private String cloCode;
        private String teachingLevel;
        private Map<String, FieldDiff> changes;
    }

    /*
     * ============================================================
     * PLANNED LEARNING ACTIVITIES
     * ============================================================
     */

    @Data
    @Builder
    public static class PlannedActivityDiff {
        private Integer week;
        private String topic;
        private String clo;
        private String assessments;
        private String learningActivities;
        private String resources;
        private Map<String, FieldDiff> changes;
    }

    /*
     * ============================================================
     * ASSESSMENT PLAN
     * ============================================================
     */

    @Data
    @Builder
    public static class AssessmentDiff {
        private String name;
        private String nameVn;
        private String assessmentType;
        private Double weightPercent;
        private Double minScore;
        private Double maxScore;
        private Integer orderIndex;
        private Map<String, FieldDiff> changes;
    }

    @Data
    @Builder
    public static class AssessmentCloMappingDiff {
        private String assessmentName;
        private Integer orderIndex;
        private String cloCode;
        private Float contributionPercent;
        private Map<String, FieldDiff> changes;
    }

    /*
     * ============================================================
     * READING LIST
     * ============================================================
     */

    /**
     * Kept with the existing class name to avoid breaking the current
     * backend service and frontend contract during Task 2.
     */
    @Data
    @Builder
    public static class readingsDiff {
        private Integer bookId;
        private String title;
        private String author;
        private String publisher;
        private Integer year;
        private String edition;
        private String isbn;
        private String url;
        private String bookType;
        private String usageType;
        private Integer orderIndex;
        private Map<String, FieldDiff> changes;
    }
}
