package com.scse.curriculum.student.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StudentResponse {

    private Integer id;
    private String studentCode;
    private String fullName;
    private String email;
    private Integer cohortId;
    private String cohortName;
    private Integer userId;
    private Boolean isActive;
    private LocalDateTime createdAt;
}