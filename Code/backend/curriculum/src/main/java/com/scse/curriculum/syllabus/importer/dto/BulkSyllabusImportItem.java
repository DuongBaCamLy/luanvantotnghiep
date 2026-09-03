package com.scse.curriculum.syllabus.importer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BulkSyllabusImportItem {
    private int startPage;
    private int endPage;
    private Long sourceSnapshotId;
    private String sourceType;
    private SyllabusImportPreviewResponse preview;
}
