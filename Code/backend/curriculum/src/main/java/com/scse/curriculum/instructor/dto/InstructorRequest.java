package com.scse.curriculum.instructor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InstructorRequest {

    @NotBlank(message = "Staff code is required")
    private String staffCode;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    private String degree;

    private String academicRank;

    @NotNull(message = "Department is required")
    private Integer departmentId;

    private Boolean isActive;

    private com.scse.curriculum.user.entity.UserRole role;

}