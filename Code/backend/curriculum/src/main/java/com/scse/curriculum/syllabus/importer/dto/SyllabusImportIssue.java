package com.scse.curriculum.syllabus.importer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SyllabusImportIssue {
    private String severity;
    private String section;
    private Integer row;
    private String field;
    private String message;
}
