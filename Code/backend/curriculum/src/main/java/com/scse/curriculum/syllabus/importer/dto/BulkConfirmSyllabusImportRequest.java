package com.scse.curriculum.syllabus.importer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Confirms one item extracted from a programme PDF. Unlike the single-file
 * flow, the target course is resolved by the server from the extracted course
 * code inside the selected curriculum scope.
 */
@Data
public class BulkConfirmSyllabusImportRequest {

    @NotNull
    @Valid
    private SyllabusImportData data;

    @NotNull
    private Integer programId;

    @NotNull
    private Integer cohortId;

    /** Required for Instructor imports; ignored for unrestricted roles. */
    private Integer assignmentId;
    private Long sourceSnapshotId;

    private String importMode = "CREATE";
    private String originalFileName;
    private String originalFileType;
}
