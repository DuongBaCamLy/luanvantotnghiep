package com.scse.curriculum.course.dto;

import com.scse.curriculum.course.entity.CourseLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CourseResponse {

    private Integer id;

    private String courseCode;

    private String name;

    private String nameVn;

    private Integer departmentId;

    private String departmentCode;

    private String departmentName;

    private Integer creditTheory;

    private Integer creditLab;

    private Integer totalCredits;

    private CourseLevel courseLevel;

    private String description;

    private Boolean isActive;

    private LocalDateTime createdAt;
}