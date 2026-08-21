package com.scse.curriculum.courseprogram.dto;

import com.scse.curriculum.courseprogram.entity.CurriculumTerm;

import lombok.Data;

@Data
public class CreateCourseProgramRequest {

    private CurriculumTerm termCode;
    private Integer courseId;

    private Integer programId;

    private Integer cohortId;

    private Integer courseTypeId;

    private Integer semesterSuggest;

    private Integer yearSuggest;

    private Boolean required;
}
