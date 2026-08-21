package com.scse.curriculum.program.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ProgramResponse {

    private Integer id;

    private String code;

    private String name;

    private String nameVn;

    private Integer majorId;
    private String majorCode;

    private Integer programTypeId;
    private String programTypeCode;

    private Integer departmentId;
    private String departmentCode;

    private String accreditationBody;

    private Integer totalCredits;

    private Integer durationYears;

    private LocalDate validFrom;

    private LocalDate validTo;

    private Boolean isActive;

    private LocalDateTime createdAt;
}