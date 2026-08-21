package com.scse.curriculum.programtype.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProgramTypeResponse {

    private Integer id;

    private String code;

    private String name;
}