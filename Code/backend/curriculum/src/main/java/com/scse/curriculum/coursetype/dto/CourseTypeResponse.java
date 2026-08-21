package com.scse.curriculum.coursetype.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseTypeResponse {

    private Integer id;

    private String code;

    private String name;

    private String nameVn;
}