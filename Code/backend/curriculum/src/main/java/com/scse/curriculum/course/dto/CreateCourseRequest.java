package com.scse.curriculum.course.dto;

import com.scse.curriculum.course.entity.CourseLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCourseRequest {

    @NotBlank
    private String courseCode;

    @NotBlank
    private String name;

    @NotBlank
    private String nameVn;

    @NotNull
    private Integer departmentId;

    private Integer creditTheory;

    private Integer creditLab;

    private CourseLevel courseLevel;

    private String description;
}