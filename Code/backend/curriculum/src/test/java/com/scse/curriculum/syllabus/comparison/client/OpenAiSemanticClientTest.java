package com.scse.curriculum.syllabus.comparison.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiSemanticClientTest {

    private ObjectMapper objectMapper;
    private OpenAiSemanticClient client;

    @BeforeEach
    void setUp() {

        objectMapper = new ObjectMapper();

        OpenAiSemanticProperties properties =
                new OpenAiSemanticProperties();

        /*
         * No real HTTP request is made in these tests.
         * Values only allow the client to be instantiated normally.
         */
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setModel("gpt-5.6-terra");
        properties.setBaseUrl("https://api.openai.com/v1");
        properties.setTimeoutSeconds(5);
        properties.setMaxBatchSize(20);
        properties.setMaxOutputTokens(4000);

        client = new OpenAiSemanticClient(
                objectMapper,
                properties
        );
    }

    @Test
    void validStructuredResponseShouldBeParsed() throws Exception {

        SemanticComparisonRequest request =
                request(
                        "objective",
                        SemanticSectionType.COURSE_OBJECTIVE,
                        "objectives",
                        "Understand basic database concepts.",
                        "Design and implement relational databases using SQL."
                );

        JsonNode response = responseWithOutput("""
                {
                  "results": [
                    {
                      "itemId": "objective",
                      "classification": "MEANINGFUL_CHANGE",
                      "changeNature": "COGNITIVE_LEVEL_INCREASE",
                      "significance": "HIGH",
                      "oldMeaning": "Conceptual understanding of basic database concepts.",
                      "newMeaning": "Practical ability to design and implement relational databases using SQL.",
                      "summary": "The expected competency increased from understanding to design and implementation."
                    }
                  ]
                }
                """);

        List<SemanticDiffResult> results =
                client.parseResponse(
                        response,
                        List.of(request)
                );

        assertEquals(
                1,
                results.size()
        );

        SemanticDiffResult result =
                results.getFirst();

        assertEquals(
                "objective",
                result.getItemId()
        );

        assertEquals(
                SemanticSectionType.COURSE_OBJECTIVE,
                result.getSectionType()
        );

        assertEquals(
                SemanticChangeType.MEANINGFUL_CHANGE,
                result.getClassification()
        );

        assertEquals(
                SemanticChangeNature.COGNITIVE_LEVEL_INCREASE,
                result.getChangeNature()
        );

        assertEquals(
                SemanticSignificance.HIGH,
                result.getSignificance()
        );

        assertFalse(
                result.isRequiresAi()
        );

        /*
         * Original texts must come from our own request,
         * never from text invented or modified by the model.
         */
        assertEquals(
                request.getOldText(),
                result.getOldText()
        );

        assertEquals(
                request.getNewText(),
                result.getNewText()
        );

        assertFalse(
                result.getSummary().isBlank()
        );
    }

    @Test
    void sameMeaningResponseShouldBeParsed() throws Exception {

        SemanticComparisonRequest request =
                request(
                        "CLO1.description",
                        SemanticSectionType.CLO,
                        "description",
                        "Understand fundamental object-oriented programming concepts.",
                        "Gain an understanding of the fundamental principles of object-oriented programming."
                );

        JsonNode response = responseWithOutput("""
                {
                  "results": [
                    {
                      "itemId": "CLO1.description",
                      "classification": "NO_MEANINGFUL_CHANGE",
                      "changeNature": "SAME_MEANING",
                      "significance": "LOW",
                      "oldMeaning": "Understand fundamental object-oriented programming concepts.",
                      "newMeaning": "Understand fundamental object-oriented programming principles.",
                      "summary": "The wording changed, but the intended academic meaning remains equivalent."
                    }
                  ]
                }
                """);

        List<SemanticDiffResult> results =
                client.parseResponse(
                        response,
                        List.of(request)
                );

        SemanticDiffResult result =
                results.getFirst();

        assertEquals(
                SemanticChangeType.NO_MEANINGFUL_CHANGE,
                result.getClassification()
        );

        assertEquals(
                SemanticChangeNature.SAME_MEANING,
                result.getChangeNature()
        );

        assertEquals(
                SemanticSignificance.LOW,
                result.getSignificance()
        );
    }

    @Test
    void missingResultShouldBeRejected() throws Exception {

        SemanticComparisonRequest first =
                request(
                        "objective",
                        SemanticSectionType.COURSE_OBJECTIVE,
                        "objectives",
                        "Old objective",
                        "New objective"
                );

        SemanticComparisonRequest second =
                request(
                        "CLO1.description",
                        SemanticSectionType.CLO,
                        "description",
                        "Old CLO",
                        "New CLO"
                );

        /*
         * OpenAI returns only the first item.
         * This is invalid because every requested item must
         * have exactly one response.
         */
        JsonNode response = responseWithOutput("""
                {
                  "results": [
                    {
                      "itemId": "objective",
                      "classification": "MEANINGFUL_CHANGE",
                      "changeNature": "MODIFIED",
                      "significance": "MEDIUM",
                      "oldMeaning": "Old meaning",
                      "newMeaning": "New meaning",
                      "summary": "Meaning changed."
                    }
                  ]
                }
                """);

        SemanticAiException exception =
                assertThrows(
                        SemanticAiException.class,
                        () -> client.parseResponse(
                                response,
                                List.of(first, second)
                        )
                );

        assertTrue(
                exception.getMessage().contains(
                        "missing semantic results"
                )
        );
    }

    @Test
    void unknownItemIdShouldBeRejected() throws Exception {

        SemanticComparisonRequest request =
                request(
                        "objective",
                        SemanticSectionType.COURSE_OBJECTIVE,
                        "objectives",
                        "Old",
                        "New"
                );

        JsonNode response = responseWithOutput("""
                {
                  "results": [
                    {
                      "itemId": "UNKNOWN_ITEM",
                      "classification": "MEANINGFUL_CHANGE",
                      "changeNature": "MODIFIED",
                      "significance": "MEDIUM",
                      "oldMeaning": "Old",
                      "newMeaning": "New",
                      "summary": "Changed."
                    }
                  ]
                }
                """);

        SemanticAiException exception =
                assertThrows(
                        SemanticAiException.class,
                        () -> client.parseResponse(
                                response,
                                List.of(request)
                        )
                );

        assertTrue(
                exception.getMessage().contains(
                        "unknown or duplicate itemId"
                )
        );
    }

    @Test
    void duplicateResponseItemIdShouldBeRejected() throws Exception {

        SemanticComparisonRequest request =
                request(
                        "objective",
                        SemanticSectionType.COURSE_OBJECTIVE,
                        "objectives",
                        "Old",
                        "New"
                );

        JsonNode response = responseWithOutput("""
                {
                  "results": [
                    {
                      "itemId": "objective",
                      "classification": "MEANINGFUL_CHANGE",
                      "changeNature": "MODIFIED",
                      "significance": "MEDIUM",
                      "oldMeaning": "Old",
                      "newMeaning": "New",
                      "summary": "Changed."
                    },
                    {
                      "itemId": "objective",
                      "classification": "NO_MEANINGFUL_CHANGE",
                      "changeNature": "SAME_MEANING",
                      "significance": "LOW",
                      "oldMeaning": "Old",
                      "newMeaning": "New",
                      "summary": "Same."
                    }
                  ]
                }
                """);

        assertThrows(
                SemanticAiException.class,
                () -> client.parseResponse(
                        response,
                        List.of(request)
                )
        );
    }

    @Test
    void refusalShouldNotBeTreatedAsNoChange() throws Exception {

        SemanticComparisonRequest request =
                request(
                        "objective",
                        SemanticSectionType.COURSE_OBJECTIVE,
                        "objectives",
                        "Old objective",
                        "New objective"
                );

        JsonNode response =
                objectMapper.readTree("""
                        {
                          "status": "completed",
                          "output": [
                            {
                              "type": "message",
                              "role": "assistant",
                              "content": [
                                {
                                  "type": "refusal",
                                  "refusal": "Unable to perform the request."
                                }
                              ]
                            }
                          ]
                        }
                        """);

        SemanticAiException exception =
                assertThrows(
                        SemanticAiException.class,
                        () -> client.parseResponse(
                                response,
                                List.of(request)
                        )
                );

        assertTrue(
                exception.getMessage().contains(
                        "refused"
                )
        );
    }

    @Test
    void incompleteResponseShouldBeRejected() throws Exception {

        SemanticComparisonRequest request =
                request(
                        "objective",
                        SemanticSectionType.COURSE_OBJECTIVE,
                        "objectives",
                        "Old",
                        "New"
                );

        JsonNode response =
                objectMapper.readTree("""
                        {
                          "status": "incomplete",
                          "output": []
                        }
                        """);

        SemanticAiException exception =
                assertThrows(
                        SemanticAiException.class,
                        () -> client.parseResponse(
                                response,
                                List.of(request)
                        )
                );

        assertTrue(
                exception.getMessage().contains(
                        "not completed"
                )
        );
    }

    @Test
    void invalidJsonOutputShouldBeRejected() throws Exception {

        SemanticComparisonRequest request =
                request(
                        "objective",
                        SemanticSectionType.COURSE_OBJECTIVE,
                        "objectives",
                        "Old",
                        "New"
                );

        JsonNode response =
                objectMapper.readTree("""
                        {
                          "status": "completed",
                          "output": [
                            {
                              "type": "message",
                              "role": "assistant",
                              "content": [
                                {
                                  "type": "output_text",
                                  "text": "THIS IS NOT JSON"
                                }
                              ]
                            }
                          ]
                        }
                        """);

        SemanticAiException exception =
                assertThrows(
                        SemanticAiException.class,
                        () -> client.parseResponse(
                                response,
                                List.of(request)
                        )
                );

        assertTrue(
                exception.getMessage().contains(
                        "invalid structured JSON"
                )
        );
    }

    @Test
    void duplicateRequestItemIdShouldBeRejected() throws Exception {

        SemanticComparisonRequest first =
                request(
                        "objective",
                        SemanticSectionType.COURSE_OBJECTIVE,
                        "objectives",
                        "Old A",
                        "New A"
                );

        SemanticComparisonRequest second =
                request(
                        "objective",
                        SemanticSectionType.CLO,
                        "description",
                        "Old B",
                        "New B"
                );

        JsonNode response = responseWithOutput("""
                {
                  "results": [
                    {
                      "itemId": "objective",
                      "classification": "MEANINGFUL_CHANGE",
                      "changeNature": "MODIFIED",
                      "significance": "MEDIUM",
                      "oldMeaning": "Old",
                      "newMeaning": "New",
                      "summary": "Changed."
                    }
                  ]
                }
                """);

        assertThrows(
                IllegalArgumentException.class,
                () -> client.parseResponse(
                        response,
                        List.of(first, second)
                )
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

    /**
     * Builds a simplified but realistic Responses API structure:
     *
     * response
     *   -> output[]
     *      -> message
     *         -> content[]
     *            -> output_text
     */
    private JsonNode responseWithOutput(
            String structuredJson) throws Exception {

        String encoded =
                objectMapper.writeValueAsString(
                        structuredJson
                );

        return objectMapper.readTree(
                """
                {
                  "id": "resp_test",
                  "object": "response",
                  "status": "completed",
                  "output": [
                    {
                      "id": "msg_test",
                      "type": "message",
                      "role": "assistant",
                      "status": "completed",
                      "content": [
                        {
                          "type": "output_text",
                          "text": %s
                        }
                      ]
                    }
                  ]
                }
                """.formatted(encoded)
        );
    }
}