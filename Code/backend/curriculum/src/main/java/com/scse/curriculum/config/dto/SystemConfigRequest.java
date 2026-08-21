package com.scse.curriculum.config.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class SystemConfigRequest {
    @NotBlank
    private String key;

    @NotBlank
    private String value;

    private String description;
}
