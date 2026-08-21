package com.scse.curriculum.department.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DepartmentResponse {

    private Integer id;

    private String code;

    private String name;

    private String nameVn;

    private Boolean isActive;

    private LocalDateTime createdAt;
}