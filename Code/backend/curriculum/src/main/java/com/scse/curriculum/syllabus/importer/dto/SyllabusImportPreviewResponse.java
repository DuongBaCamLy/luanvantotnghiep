package com.scse.curriculum.syllabus.importer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SyllabusImportPreviewResponse {
    private String fileName;
    private String fileType;
    private boolean valid;
    private int errorCount;
    private int warningCount;
    private SyllabusImportData data;
    @Builder.Default private List<SyllabusImportIssue> issues = new ArrayList<>();
}
