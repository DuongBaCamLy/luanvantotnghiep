package com.scse.curriculum.major.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateMajorRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    @NotBlank
    private String nameVn;
}