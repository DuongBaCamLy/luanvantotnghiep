package com.scse.curriculum.syllabus.comparison.service;

import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.clo.repository.CloRepository;

import com.scse.curriculum.syllabus.comparison.config.OpenAiSemanticProperties;
import com.scse.curriculum.syllabus.comparison.dto.SemanticComparisonRequest;
import com.scse.curriculum.syllabus.comparison.dto.SemanticDiffResult;
import com.scse.curriculum.syllabus.comparison.dto.SemanticSyllabusDiffResponse;
import com.scse.curriculum.syllabus.comparison.exception.SemanticAiException;
import com.scse.curriculum.syllabus.comparison.model.SemanticAnalysisStatus;
import com.scse.curriculum.syllabus.comparison.model.SemanticChangeType;
import com.scse.curriculum.syllabus.comparison.model.SemanticSectionType;
import com.scse.curriculum.syllabus.comparison.model.SemanticSignificance;

import com.scse.curriculum.syllabus.entity.Syllabus;

import com.scse.curriculum.topic.entity.Topic;
import com.scse.curriculum.topic.repository.TopicRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Builds academic semantic-comparison candidates from two
 * versions of the SAME course.
 *
 * This service does not decide access authorization.
 * SyllabusServiceImpl remains responsible for that.
 */
@Service
@RequiredArgsConstructor
public class SyllabusSemanticComparisonService {

    private final CloRepository cloRepository;

    private final TopicRepository topicRepository;

    private final SemanticComparisonService semanticComparisonService;

    private final BasicSemanticComparisonService basicComparisonService;

    private final OpenAiSemanticProperties properties;

    @Transactional(readOnly = true)
    public SemanticSyllabusDiffResponse compare(
            Syllabus oldSyllabus,
            Syllabus newSyllabus) {

        validateSyllabuses(
                oldSyllabus,
                newSyllabus
        );

        List<SemanticComparisonRequest> requests =
                collectCandidates(
                        oldSyllabus,
                        newSyllabus
                );

        /*
         * Nothing semantic exists to compare.
         */
        if (requests.isEmpty()) {

            return buildResponse(
                    oldSyllabus,
                    newSyllabus,
                    SemanticAnalysisStatus.SUCCESS,
                    List.of(),
                    false,
                    "No semantic academic text was available for comparison."
            );
        }

        /*
         * AI intentionally disabled or key not configured.
         *
         * We still return deterministic results such as
         * punctuation-only / added / removed changes.
         */
        if (!properties.isConfigured()) {

            List<SemanticDiffResult> fallback =
                    buildFallbackResults(
                            requests
                    );

            return buildResponse(
                    oldSyllabus,
                    newSyllabus,
                    SemanticAnalysisStatus.DISABLED,
                    fallback,
                    containsUnresolved(fallback),
                    "AI-assisted semantic analysis is disabled. "
                            + "Deterministic comparison results are still available."
            );
        }

        try {

            List<SemanticDiffResult> results =
                    semanticComparisonService.compare(
                            requests
                    );

            return buildResponse(
                    oldSyllabus,
                    newSyllabus,
                    SemanticAnalysisStatus.SUCCESS,
                    results,
                    false,
                    createSuccessSummary(results)
            );

        } catch (SemanticAiException exception) {

            /*
             * IMPORTANT:
             *
             * Do not interpret AI failure as
             * NO_MEANINGFUL_CHANGE.
             */
            List<SemanticDiffResult> fallback =
                    buildFallbackResults(
                            requests
                    );

            boolean unresolved =
                    containsUnresolved(fallback);

            SemanticAnalysisStatus status =
                    unresolved
                            ? SemanticAnalysisStatus.PARTIAL
                            : SemanticAnalysisStatus.UNAVAILABLE;

            return buildResponse(
                    oldSyllabus,
                    newSyllabus,
                    status,
                    fallback,
                    unresolved,
                    "AI-assisted semantic analysis is currently unavailable. "
                            + "Deterministic results are still valid."
            );
        }
    }

    /**
     * Convert the two syllabus versions into semantic text candidates.
     */
    List<SemanticComparisonRequest> collectCandidates(
            Syllabus oldSyllabus,
            Syllabus newSyllabus) {

        List<SemanticComparisonRequest> requests =
                new ArrayList<>();

        /*
         * =====================================================
         * GENERAL ACADEMIC TEXT
         * =====================================================
         */

        addRequest(
                requests,
                "general.objectives",
                SemanticSectionType.COURSE_OBJECTIVE,
                "objectives",
                oldSyllabus.getObjectives(),
                newSyllabus.getObjectives()
        );

        addRequest(
                requests,
                "general.teachingMethods",
                SemanticSectionType.TEACHING_METHOD,
                "teachingMethods",
                oldSyllabus.getTeachingMethods(),
                newSyllabus.getTeachingMethods()
        );

        addRequest(
                requests,
                "general.examRequirements",
                SemanticSectionType.EXAM_REQUIREMENT,
                "examRequirements",
                oldSyllabus.getExamRequirements(),
                newSyllabus.getExamRequirements()
        );

        /*
         * =====================================================
         * CLO
         * =====================================================
         */

        List<Clo> oldClos =
                sortedClos(
                        cloRepository.findBySyllabusId(
                                oldSyllabus.getId()
                        )
                );

        List<Clo> newClos =
                sortedClos(
                        cloRepository.findBySyllabusId(
                                newSyllabus.getId()
                        )
                );

        List<ItemPair<Clo>> cloPairs =
                matchClos(
                        oldClos,
                        newClos
                );

        for (ItemPair<Clo> pair : cloPairs) {

            Clo oldClo =
                    pair.oldItem();

            Clo newClo =
                    pair.newItem();

            String identity =
                    cloIdentity(
                            oldClo,
                            newClo
                    );

            addRequest(
                    requests,
                    "clo." + identity + ".description",
                    SemanticSectionType.CLO,
                    "description",
                    oldClo == null
                            ? null
                            : oldClo.getDescription(),
                    newClo == null
                            ? null
                            : newClo.getDescription()
            );
        }

        /*
         * =====================================================
         * TOPICS
         * =====================================================
         */

        List<Topic> oldTopics =
                sortedTopics(
                        topicRepository
                                .findBySyllabusIdOrderByWeekNumberAscOrderInWeekAsc(
                                        oldSyllabus.getId()
                                )
                );

        List<Topic> newTopics =
                sortedTopics(
                        topicRepository
                                .findBySyllabusIdOrderByWeekNumberAscOrderInWeekAsc(
                                        newSyllabus.getId()
                                )
                );

        List<ItemPair<Topic>> topicPairs =
                matchTopics(
                        oldTopics,
                        newTopics
                );

        for (ItemPair<Topic> pair : topicPairs) {

            Topic oldTopic =
                    pair.oldItem();

            Topic newTopic =
                    pair.newItem();

            String identity =
                    topicIdentity(
                            oldTopic,
                            newTopic
                    );

            /*
             * Topic/content itself.
             */
            addRequest(
                    requests,
                    "topic." + identity + ".name",
                    SemanticSectionType.TOPIC,
                    "name",
                    oldTopic == null
                            ? null
                            : oldTopic.getName(),
                    newTopic == null
                            ? null
                            : newTopic.getName()
            );

            /*
             * How the topic is taught.
             */
            addRequest(
                    requests,
                    "topic." + identity + ".teachingMethod",
                    SemanticSectionType.TEACHING_METHOD,
                    "teachingMethod",
                    oldTopic == null
                            ? null
                            : oldTopic.getTeachingMethod(),
                    newTopic == null
                            ? null
                            : newTopic.getTeachingMethod()
            );

            /*
             * What students do while learning the topic.
             */
            addRequest(
                    requests,
                    "topic." + identity + ".learningActivity",
                    SemanticSectionType.LEARNING_ACTIVITY,
                    "learningActivity",
                    oldTopic == null
                            ? null
                            : oldTopic.getLearningActivity(),
                    newTopic == null
                            ? null
                            : newTopic.getLearningActivity()
            );
        }

        return List.copyOf(
                requests
        );
    }

    private void addRequest(
            List<SemanticComparisonRequest> requests,
            String itemId,
            SemanticSectionType sectionType,
            String fieldName,
            String oldText,
            String newText) {

        /*
         * Both entirely absent:
         * there is no useful comparison candidate.
         */
        if (isBlank(oldText)
                && isBlank(newText)) {
            return;
        }

        requests.add(
                SemanticComparisonRequest.builder()
                        .itemId(itemId)
                        .sectionType(sectionType)
                        .fieldName(fieldName)
                        .oldText(oldText)
                        .newText(newText)
                        .build()
        );
    }

    /*
     * =========================================================
     * CLO MATCHING
     * =========================================================
     *
     * Existing SyllabusDiffService primarily matches CLOs by
     * code and falls back to order.
     *
     * We preserve the same semantic identity rule here.
     */

    private List<ItemPair<Clo>> matchClos(
            List<Clo> oldItems,
            List<Clo> newItems) {

        return matchItems(
                oldItems,
                newItems,
                this::cloCodeKey,
                this::cloOrderKey
        );
    }

    private String cloCodeKey(
            Clo clo) {

        return normalizeKey(
                clo == null
                        ? null
                        : clo.getCode()
        );
    }

    private String cloOrderKey(
            Clo clo) {

        if (clo == null
                || clo.getOrderIndex() == null) {
            return null;
        }

        return "order:"
                + clo.getOrderIndex();
    }

    /*
     * =========================================================
     * TOPIC MATCHING
     * =========================================================
     *
     * Existing SyllabusDiffService uses:
     *
     * week + order
     *
     * then topic name as fallback.
     */

    private List<ItemPair<Topic>> matchTopics(
            List<Topic> oldItems,
            List<Topic> newItems) {

        return matchItems(
                oldItems,
                newItems,
                this::topicPositionKey,
                this::topicNameKey
        );
    }

    private String topicPositionKey(
            Topic topic) {

        if (topic == null
                || topic.getWeekNumber() == null
                || topic.getOrderInWeek() == null) {

            return null;
        }

        return "week:"
                + topic.getWeekNumber()
                + "|order:"
                + topic.getOrderInWeek();
    }

    private String topicNameKey(
            Topic topic) {

        if (topic == null) {
            return null;
        }

        return normalizeKey(
                topic.getName()
        );
    }

    /*
     * =========================================================
     * GENERIC MATCHER
     * =========================================================
     */

    private <T> List<ItemPair<T>> matchItems(
            List<T> oldItems,
            List<T> newItems,
            java.util.function.Function<T, String> primaryKey,
            java.util.function.Function<T, String> secondaryKey) {

        List<T> oldSafe =
                oldItems == null
                        ? List.of()
                        : oldItems;

        List<T> newSafe =
                newItems == null
                        ? List.of()
                        : newItems;

        boolean[] oldMatched =
                new boolean[oldSafe.size()];

        boolean[] newMatched =
                new boolean[newSafe.size()];

        List<ItemPair<T>> pairs =
                new ArrayList<>();

        /*
         * First pass:
         * primary identity.
         */
        pairByUniqueKey(
                oldSafe,
                newSafe,
                oldMatched,
                newMatched,
                primaryKey,
                pairs
        );

        /*
         * Second pass:
         * fallback identity.
         */
        pairByUniqueKey(
                oldSafe,
                newSafe,
                oldMatched,
                newMatched,
                secondaryKey,
                pairs
        );

        /*
         * Removed items.
         */
        for (int index = 0;
             index < oldSafe.size();
             index++) {

            if (!oldMatched[index]) {

                pairs.add(
                        new ItemPair<>(
                                oldSafe.get(index),
                                null
                        )
                );
            }
        }

        /*
         * Added items.
         */
        for (int index = 0;
             index < newSafe.size();
             index++) {

            if (!newMatched[index]) {

                pairs.add(
                        new ItemPair<>(
                                null,
                                newSafe.get(index)
                        )
                );
            }
        }

        return pairs;
    }

    private <T> void pairByUniqueKey(
            List<T> oldItems,
            List<T> newItems,
            boolean[] oldMatched,
            boolean[] newMatched,
            java.util.function.Function<T, String> keyExtractor,
            List<ItemPair<T>> pairs) {

        Map<String, List<Integer>> oldIndexes =
                indexesByKey(
                        oldItems,
                        oldMatched,
                        keyExtractor
                );

        Map<String, List<Integer>> newIndexes =
                indexesByKey(
                        newItems,
                        newMatched,
                        keyExtractor
                );

        for (Map.Entry<String, List<Integer>> entry
                : oldIndexes.entrySet()) {

            String key =
                    entry.getKey();

            List<Integer> oldMatches =
                    entry.getValue();

            List<Integer> newMatches =
                    newIndexes.get(key);

            /*
             * Only pair automatically when identity is unique
             * on BOTH sides.
             *
             * Ambiguous duplicate data must not be paired
             * arbitrarily.
             */
            if (oldMatches.size() != 1
                    || newMatches == null
                    || newMatches.size() != 1) {

                continue;
            }

            int oldIndex =
                    oldMatches.getFirst();

            int newIndex =
                    newMatches.getFirst();

            oldMatched[oldIndex] =
                    true;

            newMatched[newIndex] =
                    true;

            pairs.add(
                    new ItemPair<>(
                            oldItems.get(oldIndex),
                            newItems.get(newIndex)
                    )
            );
        }
    }

    private <T> Map<String, List<Integer>> indexesByKey(
            List<T> items,
            boolean[] alreadyMatched,
            java.util.function.Function<T, String> keyExtractor) {

        Map<String, List<Integer>> indexes =
                new LinkedHashMap<>();

        for (int index = 0;
             index < items.size();
             index++) {

            if (alreadyMatched[index]) {
                continue;
            }

            String key =
                    keyExtractor.apply(
                            items.get(index)
                    );

            if (key == null
                    || key.isBlank()) {
                continue;
            }

            indexes
                    .computeIfAbsent(
                            key,
                            ignored -> new ArrayList<>()
                    )
                    .add(index);
        }

        return indexes;
    }

    /*
     * =========================================================
     * FALLBACK
     * =========================================================
     */

    private List<SemanticDiffResult> buildFallbackResults(
            List<SemanticComparisonRequest> requests) {

        List<SemanticDiffResult> results =
                new ArrayList<>(
                        requests.size()
                );

        for (SemanticComparisonRequest request : requests) {

            results.add(
                    basicComparisonService
                            .resolveWithoutAi(request)
                            .orElseGet(
                                    () -> basicComparisonService
                                            .requiresAi(request)
                            )
            );
        }

        return List.copyOf(
                results
        );
    }

    /*
     * =========================================================
     * RESPONSE
     * =========================================================
     */

    private SemanticSyllabusDiffResponse buildResponse(
            Syllabus oldSyllabus,
            Syllabus newSyllabus,
            SemanticAnalysisStatus status,
            List<SemanticDiffResult> items,
            boolean unresolved,
            String summary) {

        boolean meaningful =
                items.stream()
                        .anyMatch(
                                item ->
                                        item.getClassification()
                                                == SemanticChangeType.MEANINGFUL_CHANGE
                        );

        SemanticSignificance significance =
                calculateOverallSignificance(
                        items
                );

        return SemanticSyllabusDiffResponse.builder()
                .oldSyllabusId(
                        oldSyllabus.getId()
                )
                .newSyllabusId(
                        newSyllabus.getId()
                )
                .oldVersionLabel(
                        oldSyllabus.getVersionLabel()
                )
                .newVersionLabel(
                        newSyllabus.getVersionLabel()
                )
                .courseId(
                        oldSyllabus
                                .getCourse()
                                .getId()
                )
                .courseCode(
                        oldSyllabus
                                .getCourse()
                                .getCourseCode()
                )
                .courseName(
                        oldSyllabus
                                .getCourse()
                                .getName()
                )
                .status(status)
                .hasMeaningfulChanges(
                        meaningful
                )
                .hasUnresolvedItems(
                        unresolved
                )
                .overallSignificance(
                        significance
                )
                .summary(summary)
                .items(items)
                .build();
    }

    private SemanticSignificance calculateOverallSignificance(
            List<SemanticDiffResult> results) {

        boolean hasHigh =
                results.stream()
                        .anyMatch(
                                result ->
                                        result.getSignificance()
                                                == SemanticSignificance.HIGH
                        );

        if (hasHigh) {
            return SemanticSignificance.HIGH;
        }

        boolean hasMedium =
                results.stream()
                        .anyMatch(
                                result ->
                                        result.getSignificance()
                                                == SemanticSignificance.MEDIUM
                        );

        if (hasMedium) {
            return SemanticSignificance.MEDIUM;
        }

        return SemanticSignificance.LOW;
    }

    private String createSuccessSummary(
            List<SemanticDiffResult> results) {

        long meaningful =
                results.stream()
                        .filter(
                                result ->
                                        result.getClassification()
                                                == SemanticChangeType.MEANINGFUL_CHANGE
                        )
                        .count();

        long rewording =
                results.stream()
                        .filter(
                                result ->
                                        result.getClassification()
                                                == SemanticChangeType.MINOR_REWORDING
                        )
                        .count();

        long unchanged =
                results.stream()
                        .filter(
                                result ->
                                        result.getClassification()
                                                == SemanticChangeType.NO_MEANINGFUL_CHANGE
                        )
                        .count();

        return "Semantic analysis completed: "
                + meaningful
                + " meaningful change(s), "
                + rewording
                + " minor rewording(s), "
                + unchanged
                + " no-meaningful-change item(s).";
    }

    private boolean containsUnresolved(
            List<SemanticDiffResult> items) {

        return items.stream()
                .anyMatch(
                        SemanticDiffResult::isRequiresAi
                );
    }

    /*
     * =========================================================
     * VALIDATION
     * =========================================================
     */

    private void validateSyllabuses(
            Syllabus oldSyllabus,
            Syllabus newSyllabus) {

        Objects.requireNonNull(
                oldSyllabus,
                "Old syllabus must not be null"
        );

        Objects.requireNonNull(
                newSyllabus,
                "New syllabus must not be null"
        );

        if (oldSyllabus.getId() == null
                || newSyllabus.getId() == null) {

            throw new IllegalArgumentException(
                    "Both syllabus IDs are required for semantic comparison."
            );
        }

        if (oldSyllabus.getCourse() == null
                || newSyllabus.getCourse() == null
                || oldSyllabus.getCourse().getId() == null
                || newSyllabus.getCourse().getId() == null) {

            throw new IllegalArgumentException(
                    "Both syllabuses must belong to a course."
            );
        }

        if (!Objects.equals(
                oldSyllabus.getCourse().getId(),
                newSyllabus.getCourse().getId())) {

            throw new IllegalArgumentException(
                    "Only versions of the same course can be semantically compared."
            );
        }
    }

    /*
     * =========================================================
     * IDENTITY / SORT
     * =========================================================
     */

    private List<Clo> sortedClos(
            List<Clo> values) {

        if (values == null) {
            return List.of();
        }

        return values.stream()
                .sorted(
                        Comparator
                                .comparing(
                                        Clo::getOrderIndex,
                                        Comparator.nullsLast(
                                                Integer::compareTo
                                        )
                                )
                                .thenComparing(
                                        clo -> normalizeKey(
                                                clo.getCode()
                                        ),
                                        Comparator.nullsLast(
                                                String::compareTo
                                        )
                                )
                )
                .toList();
    }

    private List<Topic> sortedTopics(
            List<Topic> values) {

        if (values == null) {
            return List.of();
        }

        return values.stream()
                .sorted(
                        Comparator
                                .comparing(
                                        Topic::getWeekNumber,
                                        Comparator.nullsLast(
                                                Integer::compareTo
                                        )
                                )
                                .thenComparing(
                                        Topic::getOrderInWeek,
                                        Comparator.nullsLast(
                                                Integer::compareTo
                                        )
                                )
                                .thenComparing(
                                        topic -> normalizeKey(
                                                topic.getName()
                                        ),
                                        Comparator.nullsLast(
                                                String::compareTo
                                        )
                                )
                )
                .toList();
    }

    private String cloIdentity(
            Clo oldClo,
            Clo newClo) {

        Clo source =
                newClo != null
                        ? newClo
                        : oldClo;

        String code =
                source == null
                        ? null
                        : normalizeKey(
                                source.getCode()
                        );

        if (code != null) {
            return safeItemIdPart(code);
        }

        Integer order =
                source == null
                        ? null
                        : source.getOrderIndex();

        return order == null
                ? "unknown"
                : "order-" + order;
    }

    private String topicIdentity(
            Topic oldTopic,
            Topic newTopic) {

        Topic source =
                newTopic != null
                        ? newTopic
                        : oldTopic;

        if (source == null) {
            return "unknown";
        }

        if (source.getWeekNumber() != null
                && source.getOrderInWeek() != null) {

            return "week-"
                    + source.getWeekNumber()
                    + "-order-"
                    + source.getOrderInWeek();
        }

        String name =
                normalizeKey(
                        source.getName()
                );

        return name == null
                ? "unknown"
                : safeItemIdPart(name);
    }

    private String normalizeKey(
            String value) {

        if (value == null) {
            return null;
        }

        String normalized =
                value
                        .trim()
                        .replaceAll(
                                "\\s+",
                                " "
                        )
                        .toLowerCase(
                                Locale.ROOT
                        );

        return normalized.isBlank()
                ? null
                : normalized;
    }

    private String safeItemIdPart(
            String value) {

        if (value == null
                || value.isBlank()) {

            return "unknown";
        }

        return value
                .toLowerCase(Locale.ROOT)
                .replaceAll(
                        "[^a-z0-9]+",
                        "-"
                )
                .replaceAll(
                        "^-+|-+$",
                        "");
    }

    private boolean isBlank(
            String value) {

        return value == null
                || value.isBlank();
    }

    private record ItemPair<T>(
            T oldItem,
            T newItem) {
    }
}