package com.scse.curriculum.syllabus.comparison.model;

public enum SemanticAnalysisStatus {

    SUCCESS,

    /**
     * Some deterministic results are available,
     * but one or more items could not be analyzed by AI.
     */
    PARTIAL,

    /**
     * AI feature is intentionally disabled.
     */
    DISABLED,

    /**
     * AI was configured but unavailable / failed.
     */
    UNAVAILABLE
}