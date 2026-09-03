package com.scse.curriculum.syllabus.importer.dto;


import lombok.*;



@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralInformation {



    private String courseCode;



    private String courseName;



    private String semester;



    private String language;



    private String relation;



    private String teachingMethods;



    private String workloadTotal;



    private String workloadContact;



    private String workloadPrivate;



    private String prerequisites;



    private String objectives;



    private String courseDesignation;



    private String courseTypes;



    private String major;


}