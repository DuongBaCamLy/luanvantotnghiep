package com.scse.curriculum.plo.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PloResponse {

    private Integer id;

    private Integer programId;

    private String programCode;

    private String programName;

    private String code;

    private String description;

    private String descriptionVn;

    private String category;

    private Integer versionNumber;

    private Boolean isActive;

    private LocalDateTime createdAt;
}