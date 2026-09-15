package com.scse.curriculum.syllabus.comparison.dto;

import com.scse.curriculum.syllabus.comparison.model.SemanticSectionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One academic text comparison candidate.
 *
 * itemId must remain stable so batched AI responses can be mapped
 * back to the correct syllabus field/CLO/topic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticComparisonRequest {

    /**
     * Stable identity inside one syllabus comparison.
     *
     * Examples:
     * objective
     * CLO1.description
     * topic.3.1.name
     * topic.3.1.teachingMethod
     */
    private String itemId;

    /**
     * Academic section being compared.
     */
    private SemanticSectionType sectionType;

    /**
     * Original entity/property name.
     *
     * Examples:
     * objectives
     * description
     * name
     * teachingMethod
     */
    private String fieldName;

    /**
     * Previous/baseline syllabus value.
     */
    private String oldText;

    /**
     * New/target syllabus value.
     */
    private String newText;
}