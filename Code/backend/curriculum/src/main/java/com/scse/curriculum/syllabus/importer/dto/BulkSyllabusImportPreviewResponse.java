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

    /**
     * Template profile selected from the TARGET cohort.
     *
     * SOURCE_TEMPLATE keeps the existing behaviour and follows
     * the structure detected from the uploaded source document.
     *
     * NEW_2027 uses the new 2027 target-template contract while
     * preserving the uploaded document as semantic source data.
     */
    private String targetTemplateProfile;

    /**
     * Ordered structure of the TARGET syllabus template.
     *
     * Empty for SOURCE_TEMPLATE so existing cohorts keep the current
     * source-driven behaviour.
     *
     * NEW_2027 contains the target form independently from
     * SyllabusImportData.templateSections, which still represents
     * the uploaded SOURCE document.
     */
    @Builder.Default
    private List<SyllabusImportData.TemplateSection> targetTemplateSections =
            new ArrayList<>();

    @Builder.Default
    private List<BulkSyllabusImportItem> items = new ArrayList<>();
}
