package com.scse.curriculum.courseprogram.dto;

import com.scse.curriculum.courseprogram.entity.CurriculumTerm;

import lombok.Data;

@Data
public class UpdateCourseProgramRequest {
    private Integer semesterSuggest;
    private Integer courseTypeId;
    private Boolean isRequired;
    private CurriculumTerm termCode;
}
