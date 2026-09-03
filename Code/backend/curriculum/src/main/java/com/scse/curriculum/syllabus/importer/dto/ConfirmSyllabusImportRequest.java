package com.scse.curriculum.syllabus.importer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConfirmSyllabusImportRequest {


    @NotNull
    @Valid
    private SyllabusImportData data;


    @NotNull
    private Integer courseId;

    @NotNull
    private Integer programId;

    @NotNull
    private Integer cohortId;

    private Integer courseProgramId;

    /** Required for Instructor imports; identifies the authenticated user's assignment. */
    private Integer assignmentId;


    private String importMode="CREATE";


    private String originalFileName;


    private String originalFileType;

}
