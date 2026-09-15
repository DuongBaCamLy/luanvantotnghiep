package com.scse.curriculum.syllabus.comparison.model;

/**
 * High-level semantic classification.
 *
 * This answers:
 * "Did the academic meaning actually change?"
 */
public enum SemanticChangeType {

    /**
     * Text may differ, but academic meaning is effectively unchanged.
     */
    NO_MEANINGFUL_CHANGE,

    /**
     * Small wording/grammar/paraphrasing change with little or no
     * academic impact.
     */
    MINOR_REWORDING,

    /**
     * Learning meaning, competency, content scope or expectation
     * changed materially.
     */
    MEANINGFUL_CHANGE
}