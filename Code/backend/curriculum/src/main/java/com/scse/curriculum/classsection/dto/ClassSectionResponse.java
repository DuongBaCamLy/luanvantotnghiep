package com.scse.curriculum.classsection.dto;

import com.scse.curriculum.classsection.entity.SectionType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassSectionResponse {

    private Integer id;

    private Integer courseId;
    private String courseCode;
    private String courseName;

    private Integer programId;
    private String programCode;
    private String programName;

    private Integer cohortId;
    private String cohortName;

    private Integer syllabusId;
    private Integer syllabusVersionNumber;
    private String syllabusStatus;

    /**
     * Account-based instructor assignment.
     */
    private Integer instructorUserId;
    private String instructorFullName;
    private String instructorUsername;

    private Integer semester;
    private String academicYear;

    private Integer groupNumber;
    private Integer labGroup;

    private Integer maxStudents;

    private String room;
    private String schedule;

    private SectionType sectionType;

    private Boolean isActive;

    private Boolean readyForSyllabusCreation;
}