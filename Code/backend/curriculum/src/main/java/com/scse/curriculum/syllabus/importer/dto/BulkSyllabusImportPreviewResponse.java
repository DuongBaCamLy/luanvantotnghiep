package com.scse.curriculum.syllabus.importer.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BulkSyllabusImportPreviewResponse {
    private String fileName;
    private int pageCount;
    private int syllabusCount;
    private Long sourceDocumentId;
    private String sourceType;
    @Builder.Default
    private List<BulkSyllabusImportItem> items = new ArrayList<>();
}
