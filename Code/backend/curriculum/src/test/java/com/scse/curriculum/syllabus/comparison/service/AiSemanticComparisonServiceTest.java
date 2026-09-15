package com.scse.curriculum.syllabus.comparison.service;

import com.scse.curriculum.syllabus.comparison.client.OpenAiSemanticClient;
import com.scse.curriculum.syllabus.comparison.config.OpenAiSemanticProperties;
import com.scse.curriculum.syllabus.comparison.dto.SemanticComparisonRequest;
import com.scse.curriculum.syllabus.comparison.dto.SemanticDiffResult;
import com.scse.curriculum.syllabus.comparison.exception.SemanticAiException;
import com.scse.curriculum.syllabus.comparison.model.SemanticChangeNature;
import com.scse.curriculum.syllabus.comparison.model.SemanticChangeType;
import com.scse.curriculum.syllabus.comparison.model.SemanticSectionType;
import com.scse.curriculum.syllabus.comparison.model.SemanticSignificance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class AiSemanticComparisonServiceTest {

    private BasicSemanticComparisonService basicService;

    private OpenAiSemanticClient openAiClient;

    private OpenAiSemanticProperties properties;

    private AiSemanticComparisonService service;

    @BeforeEach
    void setUp() {

        basicService =
                new BasicSemanticComparisonService();

        openAiClient =
                mock(OpenAiSemanticClient.class);

        properties =
                new OpenAiSemanticProperties();

        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setMaxBatchSize(20);

        service =
                new AiSemanticComparisonService(
                        basicService,
                        openAiClient,
                        properties
                );
    }

    @Test
    void deterministicDifferencesShouldNotCallOpenAi() {

        List<SemanticComparisonRequest> requests =
                List.of(
                        request(
                                "objective",
                                SemanticSectionType.COURSE_OBJECTIVE,
                                "objectives",
                                "Understand programming.",
                                "Understand programming"
                        ),
                        request(
                                "CLO1.description",
                                SemanticSectionType.CLO,
                                "description",
                                "Apply algorithms",
                                "  APPLY   ALGORITHMS "
                        )
                );

        List<SemanticDiffResult> results =
                service.compare(requests);

        assertEquals(
                2,
                results.size()
        );

        assertEquals(
                SemanticChangeType.NO_MEANINGFUL_CHANGE,
                results.get(0).getClassification()
        );

        assertEquals(
                SemanticChangeType.NO_MEANINGFUL_CHANGE,
                results.get(1).getClassification()
        );

        verifyNoInteractions(
                openAiClient
        );
    }

    @Test
    void onlyUnresolvedItemsShouldBeSentToOpenAi() {

        SemanticComparisonRequest trivial =
                request(
                        "objective",
                        SemanticSectionType.COURSE_OBJECTIVE,
                        "objectives",
                        "Understand programming.",
                        "Understand programming"
                );

        SemanticComparisonRequest semantic =
                request(
                        "CLO1.description",
                        SemanticSectionType.CLO,
                        "description",
                        "Understand database concepts.",
                        "Design and implement relational databases."
                );

        when(
                openAiClient.compare(anyList())
        ).thenReturn(
                List.of(
                        aiResult(
                                semantic,
                                SemanticChangeType.MEANINGFUL_CHANGE,
                                SemanticChangeNature.COGNITIVE_LEVEL_INCREASE
                        )
                )
        );

        List<SemanticDiffResult> results =
                service.compare(
                        List.of(
                                trivial,
                                semantic
                        )
                );

        assertEquals(
                2,
                results.size()
        );

        assertEquals(
                SemanticChangeType.NO_MEANINGFUL_CHANGE,
                results.get(0).getClassification()
        );

        assertEquals(
                SemanticChangeType.MEANINGFUL_CHANGE,
                results.get(1).getClassification()
        );

        ArgumentCaptor<List<SemanticComparisonRequest>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(
                openAiClient,
                times(1)
        ).compare(
                captor.capture()
        );

        List<SemanticComparisonRequest> sent =
                captor.getValue();

        assertEquals(
                1,
                sent.size()
        );

        assertEquals(
                "CLO1.description",
                sent.getFirst().getItemId()
        );
    }

    @Test
    void resultsShouldPreserveOriginalRequestOrder() {

        SemanticComparisonRequest first =
                request(
                        "objective",
                        SemanticSectionType.COURSE_OBJECTIVE,
                        "objectives",
                        "Understand databases.",
                        "Design databases."
                );

        SemanticComparisonRequest second =
                request(
                        "topic.1.name",
                        SemanticSectionType.TOPIC,
                        "name",
                        "Introduction.",
                        "Introduction"
                );

        SemanticComparisonRequest third =
                request(
                        "CLO1.description",
                        SemanticSectionType.CLO,
                        "description",
                        "Describe algorithms.",
                        "Analyze algorithms."
                );

        /*
         * Deliberately return AI results in reverse order.
         *
         * Service must restore original request order.
         */
        when(
                openAiClient.compare(anyList())
        ).thenReturn(
                List.of(
                        aiResult(
                                third,
                                SemanticChangeType.MEANINGFUL_CHANGE,
                                SemanticChangeNature.COGNITIVE_LEVEL_INCREASE
                        ),
                        aiResult(
                                first,
                                SemanticChangeType.MEANINGFUL_CHANGE,
                                SemanticChangeNature.COGNITIVE_LEVEL_INCREASE
                        )
                )
        );

        List<SemanticDiffResult> results =
                service.compare(
                        List.of(
                                first,
                                second,
                                third
                        )
                );

        assertEquals(
                3,
                results.size()
        );

        assertEquals(
                "objective",
                results.get(0).getItemId()
        );

        assertEquals(
                "topic.1.name",
                results.get(1).getItemId()
        );

        assertEquals(
                "CLO1.description",
                results.get(2).getItemId()
        );
    }

    @Test
    void unresolvedItemsShouldBeSplitIntoConfiguredBatches() {

        properties.setMaxBatchSize(2);

        List<SemanticComparisonRequest> requests =
                List.of(
                        semanticRequest("item1"),
                        semanticRequest("item2"),
                        semanticRequest("item3"),
                        semanticRequest("item4"),
                        semanticRequest("item5")
                );

        when(
                openAiClient.compare(anyList())
        ).thenAnswer(invocation -> {

            List<SemanticComparisonRequest> batch =
                    invocation.getArgument(0);

            List<SemanticDiffResult> results =
                    new ArrayList<>();

            for (SemanticComparisonRequest request : batch) {

                results.add(
                        aiResult(
                                request,
                                SemanticChangeType.MEANINGFUL_CHANGE,
                                SemanticChangeNature.MODIFIED
                        )
                );
            }

            return results;
        });

        List<SemanticDiffResult> results =
                service.compare(requests);

        assertEquals(
                5,
                results.size()
        );

        ArgumentCaptor<List<SemanticComparisonRequest>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(
                openAiClient,
                times(3)
        ).compare(
                captor.capture()
        );

        List<List<SemanticComparisonRequest>> batches =
                captor.getAllValues();

        assertEquals(
                3,
                batches.size()
        );

        assertEquals(
                2,
                batches.get(0).size()
        );

        assertEquals(
                2,
                batches.get(1).size()
        );

        assertEquals(
                1,
                batches.get(2).size()
        );
    }

    @Test
    void duplicateInputItemIdShouldBeRejected() {

        SemanticComparisonRequest first =
                semanticRequest(
                        "duplicate"
                );

        SemanticComparisonRequest second =
                semanticRequest(
                        "duplicate"
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.compare(
                                List.of(
                                        first,
                                        second
                                )
                        )
                );

        assertTrue(
                exception.getMessage().contains(
                        "Duplicate semantic itemId"
                )
        );

        verifyNoInteractions(
                openAiClient
        );
    }

    @Test
    void missingAiResultShouldBeRejected() {

        SemanticComparisonRequest first =
                semanticRequest(
                        "item1"
                );

        SemanticComparisonRequest second =
                semanticRequest(
                        "item2"
                );

        when(
                openAiClient.compare(anyList())
        ).thenReturn(
                List.of(
                        aiResult(
                                first,
                                SemanticChangeType.MEANINGFUL_CHANGE,
                                SemanticChangeNature.MODIFIED
                        )
                )
        );

        SemanticAiException exception =
                assertThrows(
                        SemanticAiException.class,
                        () -> service.compare(
                                List.of(
                                        first,
                                        second
                                )
                        )
                );

        assertTrue(
                exception.getMessage().contains(
                        "missing itemIds"
                )
        );
    }

    @Test
    void aiFailureShouldPropagateInsteadOfBecomingNoChange() {

        SemanticComparisonRequest request =
                semanticRequest(
                        "objective"
                );

        when(
                openAiClient.compare(anyList())
        ).thenThrow(
                new SemanticAiException(
                        "OpenAI unavailable"
                )
        );

        SemanticAiException exception =
                assertThrows(
                        SemanticAiException.class,
                        () -> service.compare(
                                List.of(request)
                        )
                );

        assertEquals(
                "OpenAI unavailable",
                exception.getMessage()
        );
    }

    @Test
    void emptyInputShouldReturnEmptyListWithoutCallingAi() {

        List<SemanticDiffResult> result =
                service.compare(
                        List.of()
                );

        assertNotNull(result);

        assertTrue(
                result.isEmpty()
        );

        verifyNoInteractions(
                openAiClient
        );
    }

    private SemanticComparisonRequest semanticRequest(
            String itemId) {

        return request(
                itemId,
                SemanticSectionType.CLO,
                "description",
                "Understand " + itemId,
                "Design and implement " + itemId
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

    private SemanticDiffResult aiResult(
            SemanticComparisonRequest request,
            SemanticChangeType type,
            SemanticChangeNature nature) {

        return SemanticDiffResult.builder()
                .itemId(request.getItemId())
                .sectionType(request.getSectionType())
                .fieldName(request.getFieldName())
                .oldText(request.getOldText())
                .newText(request.getNewText())
                .classification(type)
                .changeNature(nature)
                .significance(
                        type == SemanticChangeType.MEANINGFUL_CHANGE
                                ? SemanticSignificance.MEDIUM
                                : SemanticSignificance.LOW
                )
                .requiresAi(false)
                .oldMeaning("Old academic meaning")
                .newMeaning("New academic meaning")
                .summary("Semantic test result")
                .build();
    }
}