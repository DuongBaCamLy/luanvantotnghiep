package com.scse.curriculum.curriculum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCurriculumCohortRequest {

    @NotNull
    private Integer entryYear;

    @NotBlank
    private String name;

    private String description;
}
