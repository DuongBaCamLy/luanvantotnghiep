package com.scse.curriculum.courserelationship.dto;

import com.scse.curriculum.courserelationship.entity.RelationType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseRelationshipResponse {

    private Integer id;
    private Integer courseId;
    private String courseCode;
    private String courseName;
    private Integer relatedCourseId;
    private String relatedCourseCode;
    private String relatedCourseName;
    private RelationType relationType;
}