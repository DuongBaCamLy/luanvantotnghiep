package com.scse.curriculum.enrollment.dto;

import com.scse.curriculum.enrollment.entity.EnrollmentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class EnrollmentResponse {

    private Integer id;
    private Integer studentId;
    private String studentCode;
    private String studentName;
    private Integer classSectionId;
    private String courseCode;
    private String courseName;
    private LocalDate enrolledAt;
    private EnrollmentStatus status;
}