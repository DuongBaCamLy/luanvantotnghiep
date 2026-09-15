package com.scse.curriculum.syllabus.comparison;

import com.scse.curriculum.syllabus.comparison.dto.SemanticComparisonRequest;
import com.scse.curriculum.syllabus.comparison.dto.SemanticDiffResult;
import com.scse.curriculum.syllabus.comparison.model.SemanticChangeNature;
import com.scse.curriculum.syllabus.comparison.model.SemanticChangeType;
import com.scse.curriculum.syllabus.comparison.model.SemanticSectionType;
import com.scse.curriculum.syllabus.comparison.service.BasicSemanticComparisonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BasicSemanticComparisonServiceTest {

    private BasicSemanticComparisonService service;

    @BeforeEach
    void setUp() {
        service = new BasicSemanticComparisonService();
    }

    @Test
    void punctuationOnlyDifferenceShouldNotRequireAi() {

        SemanticComparisonRequest request = request(
                "objective",
                SemanticSectionType.COURSE_OBJECTIVE,
                "objectives",
                "Understand fundamental programming concepts.",
                "Understand fundamental programming concepts"
        );

        Optional<SemanticDiffResult> result =
                service.resolveWithoutAi(request);

        assertTrue(result.isPresent());

        assertEquals(
                SemanticChangeType.NO_MEANINGFUL_CHANGE,
                result.get().getClassification()
        );

        assertEquals(
                SemanticChangeNature.SAME_MEANING,
                result.get().getChangeNature()
        );

        assertFalse(result.get().isRequiresAi());
    }

    @Test
    void caseAndWhitespaceOnlyDifferenceShouldNotRequireAi() {

        SemanticComparisonRequest request = request(
                "CLO1.description",
                SemanticSectionType.CLO,
                "description",
                "Understand   fundamental programming concepts",
                "  UNDERSTAND fundamental programming concepts  "
        );

        Optional<SemanticDiffResult> result =
                service.resolveWithoutAi(request);

        assertTrue(result.isPresent());

        assertEquals(
                SemanticChangeType.NO_MEANINGFUL_CHANGE,
                result.get().getClassification()
        );

        assertFalse(result.get().isRequiresAi());
    }

    @Test
    void addedTextShouldBeDetectedWithoutAi() {

        SemanticComparisonRequest request = request(
                "objective",
                SemanticSectionType.COURSE_OBJECTIVE,
                "objectives",
                null,
                "Understand fundamental programming concepts."
        );

        Optional<SemanticDiffResult> result =
                service.resolveWithoutAi(request);

        assertTrue(result.isPresent());

        assertEquals(
                SemanticChangeType.MEANINGFUL_CHANGE,
                result.get().getClassification()
        );

        assertEquals(
                SemanticChangeNature.ADDED,
                result.get().getChangeNature()
        );

        assertFalse(result.get().isRequiresAi());
    }

    @Test
    void removedTextShouldBeDetectedWithoutAi() {

        SemanticComparisonRequest request = request(
                "objective",
                SemanticSectionType.COURSE_OBJECTIVE,
                "objectives",
                "Understand fundamental programming concepts.",
                null
        );

        Optional<SemanticDiffResult> result =
                service.resolveWithoutAi(request);

        assertTrue(result.isPresent());

        assertEquals(
                SemanticChangeType.MEANINGFUL_CHANGE,
                result.get().getClassification()
        );

        assertEquals(
                SemanticChangeNature.REMOVED,
                result.get().getChangeNature()
        );

        assertFalse(result.get().isRequiresAi());
    }

    @Test
    void bothBlankShouldBeNoMeaningfulChange() {

        SemanticComparisonRequest request = request(
                "objective",
                SemanticSectionType.COURSE_OBJECTIVE,
                "objectives",
                "   ",
                null
        );

        Optional<SemanticDiffResult> result =
                service.resolveWithoutAi(request);

        assertTrue(result.isPresent());

        assertEquals(
                SemanticChangeType.NO_MEANINGFUL_CHANGE,
                result.get().getClassification()
        );

        assertFalse(result.get().isRequiresAi());
    }

    @Test
    void genuinelyDifferentNarrativeTextShouldRequireAi() {

        SemanticComparisonRequest request = request(
                "objective",
                SemanticSectionType.COURSE_OBJECTIVE,
                "objectives",
                "Understand basic database concepts.",
                "Design and implement relational databases using SQL."
        );

        Optional<SemanticDiffResult> result =
                service.resolveWithoutAi(request);

        assertTrue(result.isEmpty());

        SemanticDiffResult pending =
                service.requiresAi(request);

        assertTrue(pending.isRequiresAi());
        assertNull(pending.getClassification());
        assertNull(pending.getSignificance());
    }

    @Test
    void paraphraseMustNotBeGuessedByBasicService() {

        SemanticComparisonRequest request = request(
                "CLO1.description",
                SemanticSectionType.CLO,
                "description",
                "Understand fundamental concepts of object-oriented programming.",
                "Gain an understanding of the fundamental principles of object-oriented programming."
        );

        /*
         * Even if humans may consider these equivalent,
         * deterministic preprocessing must NOT guess.
         */
        Optional<SemanticDiffResult> result =
                service.resolveWithoutAi(request);

        assertTrue(result.isEmpty());
    }

    @Test
    void programmingSymbolsMustBePreserved() {

        SemanticComparisonRequest request = request(
                "topic.1.1.name",
                SemanticSectionType.TOPIC,
                "name",
                "Introduction to C++.",
                "Introduction to C."
        );

        /*
         * Removing punctuation must not accidentally remove
         * the '+' symbols and make C++ equal to C.
         */
        Optional<SemanticDiffResult> result =
                service.resolveWithoutAi(request);

        assertTrue(result.isEmpty());
    }

    @Test
    void blankItemIdShouldBeRejected() {

        SemanticComparisonRequest request =
                SemanticComparisonRequest.builder()
                        .itemId(" ")
                        .sectionType(SemanticSectionType.CLO)
                        .fieldName("description")
                        .oldText("A")
                        .newText("B")
                        .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.resolveWithoutAi(request)
        );
    }

    private SemanticComparisonRequest request(
            String itemId,
            SemanticSectionType sectionType,
            String fieldName,
            String oldText,
            String newText) {

        return SemanticComparisonRequest.builder()
                .itemId(itemId)
                .sectionType(sectionType)
                .fieldName(fieldName)
                .oldText(oldText)
                .newText(newText)
                .build();
    }
}