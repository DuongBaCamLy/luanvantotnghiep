package com.scse.curriculum.syllabus.importer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConfirmSyllabusImportRequest {
    @NotNull @Valid
    private SyllabusImportData data;

    /** MERGE is deliberately the default so a partial import cannot erase other sections. */
    private String importMode = "MERGE";
}
