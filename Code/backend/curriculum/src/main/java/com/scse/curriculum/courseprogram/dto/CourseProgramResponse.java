package com.scse.curriculum.courseprogram.dto;

import com.scse.curriculum.courseprogram.entity.CurriculumTerm;
    
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseProgramResponse {

    private Integer id;
    private CurriculumTerm termCode;
    private Integer courseId;
    private String courseCode;
    private String courseName;

    private Integer programId;
    private String programCode;
    private String programName;

    private Integer majorId;
    private String majorCode;
    private String majorName;

    private Integer cohortId;
    private String cohortName;

    private Integer courseTypeId;
    private String courseTypeName;

    private Integer creditTheory;
    private Integer creditLab;
    private Integer totalCredits;

    private Integer syllabusId;
    private Integer syllabusVersionNumber;
    private String syllabusVersionLabel;
    private String syllabusStatus;
    private Boolean syllabusCurrent;
    private Boolean hasSyllabus;

    private Integer semesterSuggest;
    private Integer yearSuggest;
    private Boolean required;
}