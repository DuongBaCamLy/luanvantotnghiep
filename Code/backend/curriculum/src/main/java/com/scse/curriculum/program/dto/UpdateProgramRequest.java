package com.scse.curriculum.program.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/** Editable curriculum metadata. The program code is intentionally immutable. */
@Data
public class UpdateProgramRequest {

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

    @NotNull
    @Min(1)
    private Integer totalCredits;

    @NotNull
    @Min(1)
    private Integer durationYears;

    @NotNull
    private LocalDate validFrom;

    private LocalDate validTo;
}
