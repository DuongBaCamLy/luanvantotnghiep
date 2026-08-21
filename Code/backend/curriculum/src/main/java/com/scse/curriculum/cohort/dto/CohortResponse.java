package com.scse.curriculum.cohort.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CohortResponse {

    private Integer id;

    private Integer programId;

    private String programCode;

    private String programName;

    private Integer entryYear;

    private String name;

    private String description;

    private Boolean isActive;
}