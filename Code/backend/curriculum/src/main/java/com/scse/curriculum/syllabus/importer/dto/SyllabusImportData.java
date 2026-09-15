package com.scse.curriculum.syllabus.importer.dto;


import lombok.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;



@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyllabusImportData {


    /*
     * Source information
     */

    private String sourceCourseCode;

    private String sourceCourseName;
    /**
 * Canonical source-field provenance collected during import.
 *
 * Outer key = canonical template section key.
 * Inner key = canonical field key inside that section.
 *
 * A nested map is required because field keys such as "name",
 * "orderIndex", "resources", and "cloCode" can legitimately occur
 * in more than one section.
 *
 * PARSED
 *   Source exposes the value and the parser extracted it successfully.
 *
 * ABSENT_IN_SOURCE
 *   The source does not provide a value for this canonical field.
 *
 * UNRESOLVED
 *   The source appears to provide the field/value, but the parser could
 *   not extract it reliably.
 */
@Builder.Default
private Map<String, Map<String, SourceFieldProvenance>> sourceProvenance =
        new LinkedHashMap<>();

    /**
     * Ordered comparison shape recognized from the uploaded template.
     *
     * The comparison UI must follow this structure instead of assuming a
     * fixed template. Parsers should populate sections in source order and
     * include only fields that are actually present/recognized.
     */
    @Builder.Default
    private List<TemplateSection> templateSections =
            new ArrayList<>();



    /*
     * General information
     */

    private String courseDesignation;

    private String courseTypes;

    private String semester;

    private String personResponsible;

    private String language;

    private String relation;

    private String teachingMethods;

    /** Free-text note immediately above the content table (weight/level legend). */
    private String contentNote;



    /*
     * Workload
     */

    private String workloadTotal;

    private String workloadContact;

    private String workloadPrivate;

    private String workloadStudentResponsibility;



    /*
     * Credit points
     */

    private String creditPoints;

    private String lectureCredits;

    private String laboratoryCredits;



    /*
     * Requirement
     */

    private String prerequisites;

    private String objectives;

    private String examForms;

    private String examRequirements;

    /** Pass-target note printed below the assessment plan. */
    private String assessmentPassNote;

    /** ISO-8601 revision date when the source exposes one (yyyy-MM-dd). */
    private String dateRevised;



    /*
     * Major / Program
     */

    private String major;



    /*
     * Rubrics
     */

    @Builder.Default
    private List<RubricItem> rubricItems =
            new ArrayList<>();



    /*
     * Child data
     */


    /**
     * Course Learning Outcomes
     */
    @Builder.Default
    private List<CloImportData> clos =
            new ArrayList<>();



    /**
     * Course Content / Topics
     */
    @Builder.Default
    private List<TopicImportData> topics =
            new ArrayList<>();



    /**
     * Planned learning activities
     */
    @Builder.Default
    private List<WeeklyActivityItem> weeklyActivities =
            new ArrayList<>();



    /**
     * Assessment Plan
     */
    @Builder.Default
    private List<AssessmentImportData> assessments =
            new ArrayList<>();





    /*
     * Reading list
     */

    @Builder.Default
    private List<ReadingItem> readings =
            new ArrayList<>();






    /*
     * Mapping
     */


    /**
     * CLO - PLO Matrix
     */
    @Builder.Default
    private List<CloPloMappingItem> cloPloMappings =
            new ArrayList<>();



    /**
     * Topic - CLO Mapping
     */
    @Builder.Default
    private List<TopicCloMappingItem> topicCloMappings =
            new ArrayList<>();



    /**
     * Assessment - CLO Mapping
     */
    @Builder.Default
    private List<AssessmentCloMappingItem> assessmentCloMappings =
            new ArrayList<>();







    /*
     * Inner DTO
     */


    /**
     * Missing-state contract for one canonical source field.
     */
    public enum SourceFieldState {
        PARSED,
        ABSENT_IN_SOURCE,
        UNRESOLVED
    }


    /**
     * Provenance metadata for one canonical field.
     *
     * sourceLabel preserves the heading/label actually recognized from
     * the uploaded source when available.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceFieldProvenance {

        @Builder.Default
        private SourceFieldState state =
                SourceFieldState.UNRESOLVED;

        private String sourceLabel;
    }
    /**
     * One field inside an uploaded-template comparison section.
     *
     * key   = canonical comparison field key
     * label = source/canonical label shown in the comparison UI
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateField {

        private String key;

        private String label;
    }



    /**
     * One comparison section recognized from the uploaded template.
     *
     * The list order in SyllabusImportData.templateSections is the source
     * template order. The fields list preserves the order recognized inside
     * that section.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateSection {

        private String key;

        private String label;

        @Builder.Default
        private List<TemplateField> fields =
                new ArrayList<>();
    }



    /**
     * Reading List
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadingItem {


        private String title;

        private String author;

        private String publisher;

        private Integer year;

        private String type;

    }







    /**
     * CLO-PLO Matrix
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CloPloMappingItem {


        private String cloCode;


        private String ploCode;



        /**
         * Original matrix value
         *
         * Example:
         * X
         * 1
         * 2
         * 3
         */
        private String value;



        /**
         * Numeric contribution weight
         *
         * Used when database requires percentage/weight
         */
        private Float contributionWeight;

    }







    /**
     * Topic CLO Mapping
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicCloMappingItem {


        private Integer topicIndex;

        private String cloCode;

    }







    /**
     * Assessment CLO Mapping
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssessmentCloMappingItem {


        private Integer assessmentIndex;

        private String cloCode;

        private Double percentage;

    }







    /**
     * Planned Learning Activities
     *
     * PDF section:
     * Week
     * Topic
     * CLO
     * Assessments
     * Learning activities
     * Resources
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyActivityItem {


        private Integer week;


        private String topic;


        private String clo;


        private String assessments;


        private String learningActivities;


        private String resources;

    }









    /**
     * Rubric
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RubricItem {


        /**
         * Grading checklist
         * Holistic rubric
         * Analytic rubric
         */
        private String type;


        private String title;



        @Builder.Default
        private List<RubricCriteriaItem> criteria =
                new ArrayList<>();

    }








    /**
     * Rubric criteria
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RubricCriteriaItem {


        private String criterion;


        private String level1;


        private String level2;


        private String level3;


        private String level4;

    }

}