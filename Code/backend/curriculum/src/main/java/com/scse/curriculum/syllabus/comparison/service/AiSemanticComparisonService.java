package com.scse.curriculum.syllabus.comparison.service;

import com.scse.curriculum.syllabus.comparison.client.OpenAiSemanticClient;
import com.scse.curriculum.syllabus.comparison.config.OpenAiSemanticProperties;
import com.scse.curriculum.syllabus.comparison.dto.SemanticComparisonRequest;
import com.scse.curriculum.syllabus.comparison.dto.SemanticDiffResult;
import com.scse.curriculum.syllabus.comparison.exception.SemanticAiException;
import com.scse.curriculum.syllabus.comparison.model.SemanticChangeNature;
import com.scse.curriculum.syllabus.comparison.model.SemanticChangeType;
import com.scse.curriculum.syllabus.comparison.model.SemanticSignificance;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Final semantic comparison orchestrator.
 *
 * Responsibilities:
 *
 * 1. Run deterministic comparison first.
 * 2. Avoid OpenAI requests for trivial differences.
 * 3. Collect only unresolved narrative fields.
 * 4. Send unresolved items to OpenAI in bounded batches.
 * 5. Preserve original request order.
 *
 * IMPORTANT:
 *
 * This service does NOT replace SyllabusDiffService.
 *
 * SyllabusDiffService:
 *     structural / exact comparison
 *
 * AiSemanticComparisonService:
 *     academic meaning interpretation
 */
@Service
@RequiredArgsConstructor
public class AiSemanticComparisonService
        implements SemanticComparisonService {

    private final BasicSemanticComparisonService basicComparisonService;

    private final OpenAiSemanticClient openAiSemanticClient;

    private final OpenAiSemanticProperties properties;

    @Override
    public List<SemanticDiffResult> compare(
            List<SemanticComparisonRequest> requests) {

        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        /*
         * LinkedHashMap is intentional:
         * it gives stable insertion behavior while itemId remains
         * the semantic identity.
         */
        Map<String, SemanticDiffResult> resultByItemId =
                new LinkedHashMap<>();

        List<SemanticComparisonRequest> aiCandidates =
                new ArrayList<>();

        Set<String> seenItemIds =
                new HashSet<>();

        /*
         * =====================================================
         * STEP 1
         * Deterministic preprocessing
         * =====================================================
         */
        for (SemanticComparisonRequest request : requests) {

            Objects.requireNonNull(
                    request,
                    "Semantic comparison request must not be null"
            );

            /*
             * resolveWithoutAi() also validates:
             *
             * itemId
             * sectionType
             * fieldName
             */
            Optional<SemanticDiffResult> basicResult =
                    basicComparisonService.resolveWithoutAi(
                            request
                    );

            if (!seenItemIds.add(request.getItemId())) {

                throw new IllegalArgumentException(
                        "Duplicate semantic itemId: "
                                + request.getItemId()
                );
            }

            if (basicResult.isPresent()) {

                resultByItemId.put(
                        request.getItemId(),
                        basicResult.get()
                );

            } else {

                /*
                 * Genuine narrative difference.
                 *
                 * Do NOT guess.
                 * Let OpenAI analyze academic meaning.
                 */
                aiCandidates.add(request);
            }
        }

        /*
         * =====================================================
         * STEP 2
         * AI analysis
         * =====================================================
         */
        if (!aiCandidates.isEmpty()) {

            int batchSize =
                    properties.getMaxBatchSize();

            if (batchSize <= 0) {
                throw new IllegalStateException(
                        "OpenAI semantic maxBatchSize must be greater than zero."
                );
            }

            for (
                    int start = 0;
                    start < aiCandidates.size();
                    start += batchSize
            ) {

                int end =
                        Math.min(
                                start + batchSize,
                                aiCandidates.size()
                        );

                /*
                 * Copy the sub-list so the OpenAI client receives
                 * an independent bounded batch.
                 */
                List<SemanticComparisonRequest> batch =
                        new ArrayList<>(
                                aiCandidates.subList(
                                        start,
                                        end
                                )
                        );

                List<SemanticDiffResult> aiResults =
                        openAiSemanticClient.compare(
                                batch
                        );

                validateAndMergeAiResults(
                        batch,
                        aiResults,
                        resultByItemId
                );
            }
        }

        /*
         * =====================================================
         * STEP 3
         * Restore exact original order
         * =====================================================
         */
        List<SemanticDiffResult> orderedResults =
                new ArrayList<>(
                        requests.size()
                );

        for (SemanticComparisonRequest request : requests) {

            SemanticDiffResult result =
                    resultByItemId.get(
                            request.getItemId()
                    );

            if (result == null) {

                /*
                 * This must never silently happen.
                 *
                 * Missing AI output is NOT the same as:
                 * NO_MEANINGFUL_CHANGE.
                 */
                throw new SemanticAiException(
                        "Missing semantic comparison result for itemId: "
                                + request.getItemId()
                );
            }

            orderedResults.add(result);
        }

        return List.copyOf(
                orderedResults
        );
    }

    private void validateAndMergeAiResults(
            List<SemanticComparisonRequest> batch,
            List<SemanticDiffResult> aiResults,
            Map<String, SemanticDiffResult> resultByItemId) {

        if (aiResults == null) {

            throw new SemanticAiException(
                    "OpenAI semantic client returned null results."
            );
        }

        Map<String, SemanticComparisonRequest> expected =
                new LinkedHashMap<>();

        for (SemanticComparisonRequest request : batch) {

            expected.put(
                    request.getItemId(),
                    request
            );
        }

        Set<String> returnedIds =
                new HashSet<>();

        for (SemanticDiffResult result : aiResults) {

            if (result == null) {

                throw new SemanticAiException(
                        "OpenAI semantic client returned a null result item."
                );
            }

            String itemId =
                    result.getItemId();

            if (itemId == null
                    || itemId.isBlank()) {

                throw new SemanticAiException(
                        "OpenAI semantic result has a blank itemId."
                );
            }

            if (!expected.containsKey(itemId)) {

                throw new SemanticAiException(
                        "OpenAI semantic result returned unexpected itemId: "
                                + itemId
                );
            }

            if (!returnedIds.add(itemId)) {

                throw new SemanticAiException(
                        "OpenAI semantic result returned duplicate itemId: "
                                + itemId
                );
            }

            if (resultByItemId.containsKey(itemId)) {

    throw new SemanticAiException(
            "Semantic result already exists for itemId: "
                    + itemId
    );
}

SemanticComparisonRequest originalRequest =
        expected.get(itemId);

SemanticDiffResult normalizedResult =
        normalizeAiResult(
                originalRequest,
                result
        );

resultByItemId.put(
        itemId,
        normalizedResult
);
        }

        /*
         * Every candidate sent to OpenAI must return exactly
         * one semantic result.
         */
        if (returnedIds.size()
                != expected.size()) {

            Set<String> missing =
                    new HashSet<>(
                            expected.keySet()
                    );

            missing.removeAll(
                    returnedIds
            );

            throw new SemanticAiException(
                    "OpenAI semantic result is missing itemIds: "
                            + missing
            );
        }
    }

private SemanticDiffResult normalizeAiResult(
        SemanticComparisonRequest request,
        SemanticDiffResult result) {

    /*
     * Every item reaching the AI stage has already failed the
     * deterministic "trivial difference" checks.
     *
     * Therefore an AI result of NO_MEANINGFUL_CHANGE here means:
     *
     * - the raw wording genuinely differs
     * - but the academic meaning remains equivalent
     *
     * According to our semantic contract, that is
     * MINOR_REWORDING rather than NO_MEANINGFUL_CHANGE.
     */
    if (result.getClassification()
            != SemanticChangeType.NO_MEANINGFUL_CHANGE) {

        return result;
    }

    return SemanticDiffResult.builder()
            .itemId(result.getItemId())
            .sectionType(request.getSectionType())
            .fieldName(request.getFieldName())
            .oldText(request.getOldText())
            .newText(request.getNewText())
            .classification(
                    SemanticChangeType.MINOR_REWORDING
            )
            .changeNature(
                    SemanticChangeNature.SAME_MEANING
            )
            .significance(
                    SemanticSignificance.LOW
            )
            .requiresAi(false)
            .oldMeaning(
                    result.getOldMeaning()
            )
            .newMeaning(
                    result.getNewMeaning()
            )
            .summary(
                    "Wording differs, but the academic meaning "
                            + "is substantially equivalent; "
                            + "classified as a minor rewording."
            )
            .build();
}
}