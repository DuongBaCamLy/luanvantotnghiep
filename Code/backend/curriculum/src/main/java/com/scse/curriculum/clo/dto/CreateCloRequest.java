package com.scse.curriculum.clo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCloRequest {

    @NotNull
    private Integer syllabusId;

    @NotBlank
    private String code;

    @NotBlank
    private String description;

    private String descriptionVn;

    private String competencyLevel;

    private String bloomLevel;

    private Integer orderIndex;
}