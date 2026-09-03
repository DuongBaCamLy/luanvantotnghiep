package com.scse.curriculum.syllabus.importer.dto;

import java.util.List;

public record CloPloReconciliationResponse(
        int syllabusCount,
        int matrixEntryCount,
        int existingMappingCount,
        int insertedMappingCount,
        int unresolvedMappingCount,
        List<String> unresolvedMappings) {
}
