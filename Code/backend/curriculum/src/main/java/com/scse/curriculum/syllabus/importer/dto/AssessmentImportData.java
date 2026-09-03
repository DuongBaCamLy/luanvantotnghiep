package com.scse.curriculum.syllabus.importer.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentImportData {


    private String name;


    private String nameVn;


    private String assessmentType;


    private Float weightPercent;


    private Float minScore;


    private Float maxScore;


    private Integer orderIndex;


}