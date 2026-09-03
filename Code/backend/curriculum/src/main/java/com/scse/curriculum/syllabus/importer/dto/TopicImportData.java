package com.scse.curriculum.syllabus.importer.dto;


import lombok.*;



@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicImportData {



    private Integer weekNumber;



    private Integer orderInWeek;



    private String name;



    private String nameVn;

    /** Weight shown in the syllabus content table (kept as text for fidelity). */
    private String contentWeight;

    /** Original I/T/U teaching level shown in the content table. */
    private String contentLevel;

    /** Preferred API name for the original I/T/U teaching level. */
    private String teachingLevel;



    private Integer teachingHours;



    private Integer labHours;



    private Integer selfStudyHours;



    private String topicType;



    private String teachingMethod;



    private String learningActivity;



    private String resources;



    private String notes;


}
