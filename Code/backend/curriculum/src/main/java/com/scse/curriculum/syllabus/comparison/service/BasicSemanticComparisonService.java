package com.scse.curriculum.syllabus.comparison.service;

import com.scse.curriculum.syllabus.comparison.dto.SemanticComparisonRequest;
import com.scse.curriculum.syllabus.comparison.dto.SemanticDiffResult;
import com.scse.curriculum.syllabus.comparison.model.SemanticChangeNature;
import com.scse.curriculum.syllabus.comparison.model.SemanticChangeType;
import com.scse.curriculum.syllabus.comparison.model.SemanticSignificance;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Performs deterministic preprocessing before any AI request.
 *
 * This service MUST NOT attempt to guess semantic meaning.
 *
 * It only resolves cases that can be classified safely without AI:
 *
 * - both values empty
 * - added value
 * - removed value
 * - exact normalized equality
 * - punctuation/case/whitespace-only differences
 *
 * Everything else is delegated to the semantic AI layer.
 */
@Service
public class BasicSemanticComparisonService {

    /**
     * Tries to resolve a comparison without AI.
     *
     * @return a result when comparison can be decided deterministically;
     *         Optional.empty() when semantic analysis is required.
     */
    public Optional<SemanticDiffResult> resolveWithoutAi(
            SemanticComparisonRequest request) {

        validateRequest(request);

        String oldText = request.getOldText();
        String newText = request.getNewText();

        boolean oldBlank = isBlank(oldText);
        boolean newBlank = isBlank(newText);

        /*
         * Both values absent.
         */
        if (oldBlank && newBlank) {

            return Optional.of(
                    result(
                            request,
                            SemanticChangeType.NO_MEANINGFUL_CHANGE,
                            SemanticChangeNature.SAME_MEANING,
                            SemanticSignificance.LOW,
                            "Both values are empty."
                    )
            );
        }

        /*
         * Academic content was added.
         *
         * No AI is required to determine that a new value exists.
         */
        if (oldBlank) {

            return Optional.of(
                    result(
                            request,
                            SemanticChangeType.MEANINGFUL_CHANGE,
                            SemanticChangeNature.ADDED,
                            SemanticSignificance.MEDIUM,
                            "Academic content was added."
                    )
            );
        }

        /*
         * Academic content was removed.
         */
        if (newBlank) {

            return Optional.of(
                    result(
                            request,
                            SemanticChangeType.MEANINGFUL_CHANGE,
                            SemanticChangeNature.REMOVED,
                            SemanticSignificance.MEDIUM,
                            "Academic content was removed."
                    )
            );
        }

        String normalizedOld = normalizeText(oldText);
        String normalizedNew = normalizeText(newText);

        /*
         * Case / whitespace / Unicode-normalization differences only.
         */
        if (Objects.equals(normalizedOld, normalizedNew)) {

            return Optional.of(
                    noMeaningfulChange(
                            request,
                            "Only formatting, capitalization or whitespace changed."
                    )
            );
        }

        String punctuationInsensitiveOld =
                normalizeIgnoringPunctuation(oldText);

        String punctuationInsensitiveNew =
                normalizeIgnoringPunctuation(newText);

        /*
         * Examples:
         *
         * "Understand programming."
         * "Understand programming"
         *
         * These should not consume an OpenAI API request.
         */
        if (Objects.equals(
                punctuationInsensitiveOld,
                punctuationInsensitiveNew)) {

            return Optional.of(
                    noMeaningfulChange(
                            request,
                            "Only punctuation or superficial formatting changed."
                    )
            );
        }

        /*
         * IMPORTANT:
         *
         * Do NOT classify the remaining values here.
         *
         * Different strings are not automatically different academic meanings.
         */
        return Optional.empty();
    }

    /**
     * Creates an explicit pending result for candidates that require AI.
     *
     * This is useful for an aggregator that wants to preserve every
     * candidate before calling OpenAI.
     */
    public SemanticDiffResult requiresAi(
            SemanticComparisonRequest request) {

        validateRequest(request);

        return SemanticDiffResult.builder()
                .itemId(request.getItemId())
                .sectionType(request.getSectionType())
                .fieldName(request.getFieldName())
                .oldText(request.getOldText())
                .newText(request.getNewText())
                .classification(null)
                .changeNature(null)
                .significance(null)
                .requiresAi(true)
                .oldMeaning(null)
                .newMeaning(null)
                .summary("Semantic interpretation is required.")
                .build();
    }

    /**
     * Canonical representation for safe exact comparison.
     *
     * IMPORTANT:
     * Do not remove symbols here because technical terms such as
     * C++ must retain their identity.
     */
    String normalizeText(String value) {

        if (value == null) {
            return "";
        }

        return Normalizer
                .normalize(value, Normalizer.Form.NFKC)
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Used only for deciding whether the difference consists solely
     * of punctuation.
     *
     * Unicode punctuation is removed, but mathematical/programming
     * symbols such as '+' are preserved.
     *
     * Therefore:
     *
     * "C++" does NOT become "C".
     */
    String normalizeIgnoringPunctuation(String value) {

        String normalized = normalizeText(value);

        return normalized
                .replaceAll("\\p{P}+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private SemanticDiffResult noMeaningfulChange(
            SemanticComparisonRequest request,
            String summary) {

        return result(
                request,
                SemanticChangeType.NO_MEANINGFUL_CHANGE,
                SemanticChangeNature.SAME_MEANING,
                SemanticSignificance.LOW,
                summary
        );
    }

    private SemanticDiffResult result(
            SemanticComparisonRequest request,
            SemanticChangeType classification,
            SemanticChangeNature changeNature,
            SemanticSignificance significance,
            String summary) {

        return SemanticDiffResult.builder()
                .itemId(request.getItemId())
                .sectionType(request.getSectionType())
                .fieldName(request.getFieldName())
                .oldText(request.getOldText())
                .newText(request.getNewText())
                .classification(classification)
                .changeNature(changeNature)
                .significance(significance)
                .requiresAi(false)
                .oldMeaning(null)
                .newMeaning(null)
                .summary(summary)
                .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void validateRequest(
            SemanticComparisonRequest request) {

        Objects.requireNonNull(
                request,
                "Semantic comparison request must not be null"
        );

        Objects.requireNonNull(
                request.getSectionType(),
                "Semantic section type must not be null"
        );

        if (request.getItemId() == null
                || request.getItemId().isBlank()) {

            throw new IllegalArgumentException(
                    "Semantic comparison itemId must not be blank"
            );
        }

        if (request.getFieldName() == null
                || request.getFieldName().isBlank()) {

            throw new IllegalArgumentException(
                    "Semantic comparison fieldName must not be blank"
            );
        }
    }
}