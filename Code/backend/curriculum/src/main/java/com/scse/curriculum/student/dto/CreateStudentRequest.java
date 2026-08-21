package com.scse.curriculum.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateStudentRequest {

    @NotBlank
    private String studentCode;

    @NotBlank
    private String fullName;

    @Email
    @NotBlank
    private String email;

    @NotNull
    private Integer cohortId;

    private Integer userId;

    private Boolean isActive;
}