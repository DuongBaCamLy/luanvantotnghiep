package com.scse.curriculum.cohort.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCohortRequest {

    @NotNull
    private Integer programId;

    @NotNull
    private Integer entryYear;

    @NotBlank
    private String name;

    private String description;
}