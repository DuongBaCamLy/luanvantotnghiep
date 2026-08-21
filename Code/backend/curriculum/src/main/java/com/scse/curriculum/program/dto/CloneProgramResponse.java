package com.scse.curriculum.program.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CloneProgramResponse {

    private Integer programId;
    private String programCode;

    private Integer sourceCohortId;
    private String sourceCohortName;

    private Integer targetCohortId;
    private String targetCohortName;

    private Integer copiedCount;
    private Integer skippedCount;
    private Integer overwrittenCount;

    private List<Integer> createdCourseProgramIds;
    private String message;
}
