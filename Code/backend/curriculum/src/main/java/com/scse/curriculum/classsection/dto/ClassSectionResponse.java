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
    private Integer syllabusId;
    private Integer syllabusVersionNumber;
    private String syllabusStatus;
    private Integer instructorId;
    private String instructorName;
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
