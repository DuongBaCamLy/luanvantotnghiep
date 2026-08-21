package com.scse.curriculum.clo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CloResponse {

    private Integer id;

    private Integer syllabusId;

    private String code;

    private String description;

    private String descriptionVn;

    private String competencyLevel;

    private String bloomLevel;

    private Integer orderIndex;
}