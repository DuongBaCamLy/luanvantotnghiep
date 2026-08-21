package com.scse.curriculum.curriculum.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CurriculumCloneSourceRequest {

    @NotNull
    private Integer programId;

    @NotNull
    private Integer cohortId;
}
