package com.scse.curriculum.courserelationship.dto;

import com.scse.curriculum.courserelationship.entity.RelationType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCourseRelationshipRequest {

    @NotNull
    private Integer courseId;

    @NotNull
    private Integer relatedCourseId;

    @NotNull
    private RelationType relationType;
}