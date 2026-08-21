package com.scse.curriculum.syllabusbook.dto;

import com.scse.curriculum.syllabusbook.entity.UsageType;
import lombok.Data;

@Data
public class SyllabusBookRequest {

    private Integer syllabusId;

    private Integer bookId;

    private UsageType usageType;

    private Integer orderIndex;
}