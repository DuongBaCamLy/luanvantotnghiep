package com.scse.curriculum.enrollment.dto;

import com.scse.curriculum.enrollment.entity.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateEnrollmentRequest {

    @NotNull
    private Integer studentId;

    @NotNull
    private Integer classSectionId;

    private LocalDate enrolledAt;

    private EnrollmentStatus status;
}