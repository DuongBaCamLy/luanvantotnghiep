package com.scse.curriculum.syllabus.importer.dto;


import lombok.*;

import java.util.ArrayList;
import java.util.List;



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
