package com.scse.curriculum.syllabus.comparison.dto;

import com.scse.curriculum.syllabus.comparison.model.SemanticChangeNature;
import com.scse.curriculum.syllabus.comparison.model.SemanticChangeType;
import com.scse.curriculum.syllabus.comparison.model.SemanticSectionType;
import com.scse.curriculum.syllabus.comparison.model.SemanticSignificance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Semantic interpretation for one syllabus text field.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticDiffResult {

    private String itemId;

    private SemanticSectionType sectionType;

    private String fieldName;

    private String oldText;

    private String newText;

    /**
     * Final semantic classification.
     *
     * Null is allowed temporarily when requiresAi == true.
     */
    private SemanticChangeType classification;

    /**
     * Nature of the academic change.
     */
    private SemanticChangeNature changeNature;

    /**
     * Academic significance.
     *
     * May be null while waiting for AI analysis.
     */
    private SemanticSignificance significance;

    /**
     * true means deterministic comparison cannot safely decide
     * whether academic meaning changed.
     */
    private boolean requiresAi;

    /**
     * Semantic interpretation of the old text.
     * Normally populated by AI only.
     */
    private String oldMeaning;

    /**
     * Semantic interpretation of the new text.
     * Normally populated by AI only.
     */
    private String newMeaning;

    /**
     * Short explanation suitable for the comparison UI.
     */
    private String summary;
}