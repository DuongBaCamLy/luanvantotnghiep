package com.scse.curriculum.cloplomapping.dto;

import com.scse.curriculum.cloplomapping.entity.ContributionLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCloPloMappingRequest {

    @NotNull
    private Integer cloId;

    @NotNull
    private Integer ploId;

    @NotNull
    private ContributionLevel level;

    private Float contributionWeight;

    private String notes;
}