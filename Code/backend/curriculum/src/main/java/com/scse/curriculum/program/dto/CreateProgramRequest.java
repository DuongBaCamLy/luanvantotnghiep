package com.scse.curriculum.program.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateProgramRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    private String nameVn;

    @NotNull
    private Integer majorId;

    @NotNull
    private Integer programTypeId;

    @NotNull
    private Integer departmentId;

    private String accreditationBody;

    private Integer totalCredits;

    private Integer durationYears;

    private LocalDate validFrom;

    private LocalDate validTo;
}