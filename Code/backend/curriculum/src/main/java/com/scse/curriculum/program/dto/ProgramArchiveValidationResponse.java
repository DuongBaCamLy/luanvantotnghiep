package com.scse.curriculum.program.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProgramArchiveValidationResponse {
    private Integer programId;
    private boolean canArchive;
    private List<String> violations;
}
