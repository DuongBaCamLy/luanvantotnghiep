package com.scse.curriculum.syllabus.comparison.service;

import com.scse.curriculum.syllabus.comparison.dto.SemanticComparisonRequest;
import com.scse.curriculum.syllabus.comparison.dto.SemanticDiffResult;

import java.util.List;

/**
 * Contract for final semantic comparison.
 *
 * A future implementation will combine:
 *
 * deterministic normalization
 *          +
 * OpenAI semantic analysis
 */
public interface SemanticComparisonService {

    List<SemanticDiffResult> compare(
            List<SemanticComparisonRequest> requests);
}