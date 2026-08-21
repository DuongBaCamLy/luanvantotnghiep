package com.scse.curriculum.curriculum.dto;

import com.scse.curriculum.cohort.dto.CohortResponse;
import com.scse.curriculum.program.dto.CloneProgramResponse;
import com.scse.curriculum.program.dto.ProgramResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateCurriculumResponse {
    private ProgramResponse program;
    private CohortResponse cohort;
    private CloneProgramResponse clone;
    private String message;
}
