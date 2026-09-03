package com.scse.curriculum.cohort.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CreateCohortRequest {

    @NotNull
    private Integer programId;

    @NotNull
    @Min(2000)
    @Max(2100)
    private Integer entryYear;

    /**
     * Kept temporarily for backwards-compatible clients. The service ignores
     * this display value and derives it from Program code + entryYear.
     */
    private String name;

    private String description;
}
