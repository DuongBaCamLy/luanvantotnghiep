package com.scse.curriculum.cloplomapping.dto;

import com.scse.curriculum.cloplomapping.entity.ContributionLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CloPloMappingResponse {

    private Integer id;

    private Integer cloId;
    private String cloCode;

    private Integer ploId;
    private String ploCode;

    private ContributionLevel level;

    private Float contributionWeight;

    private String notes;
}