package com.scse.curriculum.major.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MajorResponse {

    private Integer id;

    private String code;

    private String name;

    private String nameVn;
}