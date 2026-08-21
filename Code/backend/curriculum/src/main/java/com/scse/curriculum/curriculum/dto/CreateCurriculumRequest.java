package com.scse.curriculum.curriculum.dto;

import com.scse.curriculum.program.dto.CreateProgramRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCurriculumRequest {

    @Valid
    @NotNull
    private CreateProgramRequest program;

    @Valid
    @NotNull
    private CreateCurriculumCohortRequest cohort;

    @Valid
    private CurriculumCloneSourceRequest cloneSource;
}
