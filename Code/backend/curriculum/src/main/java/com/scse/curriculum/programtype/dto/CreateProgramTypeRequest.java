package com.scse.curriculum.programtype.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateProgramTypeRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String name;
}