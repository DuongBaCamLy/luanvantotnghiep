package com.scse.curriculum.plo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePloRequest {

    @NotNull
    private Integer programId;

    @NotBlank
    private String code;

    @NotBlank
    private String description;

    private String descriptionVn;

    private String category;

    private Integer versionNumber;
}