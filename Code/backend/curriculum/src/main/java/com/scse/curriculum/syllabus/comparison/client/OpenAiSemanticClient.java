package com.scse.curriculum.syllabus.comparison.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.scse.curriculum.syllabus.comparison.config.OpenAiSemanticProperties;
import com.scse.curriculum.syllabus.comparison.dto.SemanticComparisonRequest;
import com.scse.curriculum.syllabus.comparison.dto.SemanticDiffResult;
import com.scse.curriculum.syllabus.comparison.exception.SemanticAiException;
import com.scse.curriculum.syllabus.comparison.model.SemanticChangeNature;
import com.scse.curriculum.syllabus.comparison.model.SemanticChangeType;
import com.scse.curriculum.syllabus.comparison.model.SemanticSignificance;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiSemanticClient {

    private static final String SYSTEM_PROMPT = """
            You are an academic curriculum and syllabus change analyst.

            Compare ONLY the supplied OLD and NEW syllabus texts.

            Your task is to determine whether the academic meaning changed.

            Ignore differences that are only:
            - punctuation
            - capitalization
            - whitespace
            - formatting
            - minor grammar correction
            - superficial paraphrasing
            - synonym substitution where academic meaning remains the same

            Consider a change meaningful when it changes:
            - academic knowledge or content
            - learning scope
            - expected student competency
            - expected learning outcome
            - teaching approach
            - learning activity
            - assessment expectation
            - cognitive level

            For learning objectives and CLOs, pay special attention to
            cognitive intent.

            For example, a change from:
            understand / describe

            to:
            apply / analyze / design / implement / evaluate

            may represent a cognitive-level increase when supported by
            the supplied text.

            Do not invent information.
            Do not use external curriculum facts.
            Do not judge whether the syllabus is good or bad.
            Only analyze semantic differences between OLD and NEW.

            Preserve every itemId exactly.
            Return one result for every input item.

            You are comparing academic syllabus content by semantic meaning,
not by lexical or surface-text difference.

A wording difference alone is NOT a meaningful academic change.

Classification rules:

NO_MEANINGFUL_CHANGE:
Use only when the texts are effectively identical and differ only
in punctuation, capitalization, whitespace, formatting, or other
trivial surface differences.

MINOR_REWORDING:
Use when the wording is different but the academic meaning is
substantially equivalent. This includes paraphrasing, grammar fixes,
word-order changes, synonyms, stylistic rewriting, or clearer wording
that does not change the expected knowledge, skill, competency,
scope, cognitive demand, teaching approach, learning activity,
or assessment expectation.

For MINOR_REWORDING:
changeNature must normally be SAME_MEANING
and significance should normally be LOW.

MEANINGFUL_CHANGE:
Use only when the academic meaning materially changes, such as:
- different expected knowledge or skill
- broader or narrower learning scope
- added or removed academic requirement
- different competency expectation
- cognitive-level increase or decrease
- materially different teaching approach
- materially different learning activity
- materially different examination expectation

IMPORTANT RULE FOR TYPOS AND MALFORMED WORDING:

A typo, accidental word insertion, accidental word deletion,
grammatical mistake, awkward wording, or malformed phrase is NOT
a meaningful academic change by itself.

If OLD and NEW still communicate substantially the same academic
knowledge, skill, competency, learning outcome, teaching method,
learning activity, or assessment expectation, classify the result as:

classification = MINOR_REWORDING
changeNature = SAME_MEANING
significance = LOW

Do not invent a new academic interpretation from an awkward or
grammatically incorrect phrase.

For example:

OLD:
"Manage marketing strategy and financial statements in an enterprise."

NEW:
"Manage marketing strategy and financial statements in outcomes an enterprise."

The phrase "in outcomes an enterprise" is malformed wording and does
not establish a clear new academic requirement. Unless surrounding
text provides clear evidence of a different academic intent, this
should be MINOR_REWORDING, SAME_MEANING, LOW.

A MEANINGFUL_CHANGE requires a coherent and identifiable change in
academic intent. Lexical differences alone are insufficient evidence.

For oldMeaning and newMeaning:

Do NOT simply repeat or copy oldText and newText.

Express the interpreted academic meaning concisely in normalized
language.

When OLD and NEW have the same academic meaning, oldMeaning and
newMeaning should describe essentially the same academic intent,
even when the original wording differs.

Do NOT classify text as MEANINGFUL_CHANGE merely because words,
phrases, sentence order, grammar, or phrasing changed.

Compare only the academic meaning of oldText and newText.
Do not infer a change from Program, Cohort, academic year, or other
context not contained in the compared text.
            """;

    private final ObjectMapper objectMapper;
    private final OpenAiSemanticProperties properties;
    private final RestClient restClient;

    public OpenAiSemanticClient(
            ObjectMapper objectMapper,
            OpenAiSemanticProperties properties) {

        this.objectMapper = objectMapper;
        this.properties = properties;

        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        Duration timeout =
                Duration.ofSeconds(properties.getTimeoutSeconds());

        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    public List<SemanticDiffResult> compare(
            List<SemanticComparisonRequest> requests) {

        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        if (!properties.isConfigured()) {
            throw new SemanticAiException(
                    "OpenAI semantic analysis is disabled or not configured."
            );
        }

        if (requests.size() > properties.getMaxBatchSize()) {
            throw new IllegalArgumentException(
                    "Semantic comparison batch exceeds maximum size of "
                            + properties.getMaxBatchSize()
            );
        }

        ObjectNode requestBody =
                buildRequestBody(requests);

        try {

            JsonNode response = restClient.post()
                    .uri("/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers ->
                            headers.setBearerAuth(
                                    properties.getApiKey()
                            )
                    )
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            return parseResponse(
                    response,
                    requests
            );

        } catch (SemanticAiException exception) {

            throw exception;

        } catch (RestClientException exception) {

            throw new SemanticAiException(
                    "OpenAI semantic analysis request failed.",
                    exception
            );

        } catch (Exception exception) {

            throw new SemanticAiException(
                    "Unable to process OpenAI semantic analysis response.",
                    exception
            );
        }
    }

    private ObjectNode buildRequestBody(
            List<SemanticComparisonRequest> requests) {

        ObjectNode root =
                objectMapper.createObjectNode();

        root.put(
                "model",
                properties.getModel()
        );

        root.put(
                "max_output_tokens",
                properties.getMaxOutputTokens()
        );

        ArrayNode input =
                root.putArray("input");

        ObjectNode systemMessage =
                input.addObject();

        systemMessage.put(
                "role",
                "system"
        );

        systemMessage.put(
                "content",
                SYSTEM_PROMPT
        );

        ObjectNode userMessage =
                input.addObject();

        userMessage.put(
                "role",
                "user"
        );

        userMessage.put(
                "content",
                buildUserPayload(requests)
        );

        ObjectNode text =
                root.putObject("text");

        ObjectNode format =
                text.putObject("format");

        format.put(
                "type",
                "json_schema"
        );

        format.put(
                "name",
                "syllabus_semantic_comparison"
        );

        format.put(
                "strict",
                true
        );

        format.set(
                "schema",
                buildResponseSchema()
        );

        return root;
    }

    private String buildUserPayload(
            List<SemanticComparisonRequest> requests) {

        ArrayNode items =
                objectMapper.createArrayNode();

        for (SemanticComparisonRequest request : requests) {

            ObjectNode item =
                    items.addObject();

            item.put(
                    "itemId",
                    request.getItemId()
            );

            item.put(
                    "sectionType",
                    request.getSectionType().name()
            );

            item.put(
                    "fieldName",
                    request.getFieldName()
            );

            item.put(
                    "oldText",
                    safeText(request.getOldText())
            );

            item.put(
                    "newText",
                    safeText(request.getNewText())
            );
        }

        ObjectNode payload =
                objectMapper.createObjectNode();

        payload.set(
                "items",
                items
        );

        try {

            return objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(payload);

        } catch (Exception exception) {

            throw new SemanticAiException(
                    "Unable to serialize semantic comparison request.",
                    exception
            );
        }
    }

    private ObjectNode buildResponseSchema() {

        ObjectNode root =
                objectMapper.createObjectNode();

        root.put(
                "type",
                "object"
        );

        root.put(
                "additionalProperties",
                false
        );

        ObjectNode propertiesNode =
                root.putObject("properties");

        ObjectNode results =
                propertiesNode.putObject("results");

        results.put(
                "type",
                "array"
        );

        ObjectNode item =
                results.putObject("items");

        item.put(
                "type",
                "object"
        );

        item.put(
                "additionalProperties",
                false
        );

        ObjectNode itemProperties =
                item.putObject("properties");

        itemProperties
                .putObject("itemId")
                .put("type", "string");

        enumProperty(
                itemProperties,
                "classification",
                SemanticChangeType.values()
        );

        enumProperty(
                itemProperties,
                "changeNature",
                SemanticChangeNature.values()
        );

        enumProperty(
                itemProperties,
                "significance",
                SemanticSignificance.values()
        );

        itemProperties
                .putObject("oldMeaning")
                .put("type", "string");

        itemProperties
                .putObject("newMeaning")
                .put("type", "string");

        itemProperties
                .putObject("summary")
                .put("type", "string");

        ArrayNode required =
                item.putArray("required");

        required.add("itemId");
        required.add("classification");
        required.add("changeNature");
        required.add("significance");
        required.add("oldMeaning");
        required.add("newMeaning");
        required.add("summary");

        ArrayNode rootRequired =
                root.putArray("required");

        rootRequired.add("results");

        return root;
    }

    private void enumProperty(
            ObjectNode parent,
            String propertyName,
            Enum<?>[] values) {

        ObjectNode property =
                parent.putObject(propertyName);

        property.put(
                "type",
                "string"
        );

        ArrayNode enumValues =
                property.putArray("enum");

        for (Enum<?> value : values) {
            enumValues.add(value.name());
        }
    }

    /**
     * Lightweight provider verification used by the comparison status UI.
     *
     * It does NOT run a semantic inference request and therefore does not
     * consume a syllabus-comparison completion. It only asks the configured
     * OpenAI-compatible provider for its model list.
     */
    public ProviderProbe probeProvider() {

        if (!properties.isConfigured()) {
            return new ProviderProbe(
                    false,
                    false,
                    "AI semantic provider is not fully configured."
            );
        }

        try {

            JsonNode response =
                    restClient.get()
                            .uri("/models")
                            .headers(headers ->
                                    headers.setBearerAuth(
                                            properties.getApiKey()
                                    )
                            )
                            .retrieve()
                            .body(JsonNode.class);

            if (response == null) {
                return new ProviderProbe(
                        true,
                        false,
                        "Provider responded, but the model list was empty."
                );
            }

            JsonNode models =
                    response.path("data");

            if (!models.isArray()) {
                return new ProviderProbe(
                        true,
                        false,
                        "Provider responded, but no compatible model list was returned."
                );
            }

            String configuredModel =
                    properties.getModel() == null
                            ? ""
                            : properties.getModel().trim();

            boolean modelAvailable = false;

            for (JsonNode model : models) {
                String id =
                        model.path("id").asText("");

                if (configuredModel.equals(id)) {
                    modelAvailable = true;
                    break;
                }
            }

            if (!modelAvailable) {
                return new ProviderProbe(
                        true,
                        false,
                        "Provider is reachable, but the configured semantic model is not available."
                );
            }

            return new ProviderProbe(
                    true,
                    true,
                    "Provider is reachable and the configured semantic model is available."
            );

        } catch (RestClientException exception) {

            return new ProviderProbe(
                    false,
                    false,
                    "Unable to reach or authenticate with the configured AI provider."
            );

        } catch (Exception exception) {

            return new ProviderProbe(
                    false,
                    false,
                    "Unable to verify the configured AI provider."
            );
        }
    }


    public record ProviderProbe(
            boolean reachable,
            boolean modelAvailable,
            String message) {
    }


    List<SemanticDiffResult> parseResponse(
            JsonNode response,
            List<SemanticComparisonRequest> requests) {

        if (response == null) {
            throw new SemanticAiException(
                    "OpenAI returned an empty response."
            );
        }

        String status =
                response.path("status").asText();

        if (!"completed".equalsIgnoreCase(status)) {

            throw new SemanticAiException(
                    "OpenAI response was not completed. Status: "
                            + status
            );
        }

        String outputText =
                extractOutputText(response);

        JsonNode structured;

        try {

            structured =
                    objectMapper.readTree(outputText);

        } catch (Exception exception) {

            throw new SemanticAiException(
                    "OpenAI returned invalid structured JSON.",
                    exception
            );
        }

        JsonNode resultsNode =
                structured.path("results");

        if (!resultsNode.isArray()) {

            throw new SemanticAiException(
                    "OpenAI semantic response does not contain results."
            );
        }

        Map<String, SemanticComparisonRequest> requestById =
                new HashMap<>();

        for (SemanticComparisonRequest request : requests) {

            if (requestById.put(
                    request.getItemId(),
                    request
            ) != null) {

                throw new IllegalArgumentException(
                        "Duplicate semantic itemId: "
                                + request.getItemId()
                );
            }
        }

        List<SemanticDiffResult> results =
                new ArrayList<>();

        for (JsonNode resultNode : resultsNode) {

            String itemId =
                    resultNode.path("itemId").asText();

            SemanticComparisonRequest original =
                    requestById.remove(itemId);

            if (original == null) {

                throw new SemanticAiException(
                        "OpenAI returned an unknown or duplicate itemId: "
                                + itemId
                );
            }

            results.add(
                    SemanticDiffResult.builder()
                            .itemId(itemId)
                            .sectionType(original.getSectionType())
                            .fieldName(original.getFieldName())
                            .oldText(original.getOldText())
                            .newText(original.getNewText())
                            .classification(
                                    parseEnum(
                                            SemanticChangeType.class,
                                            resultNode.path(
                                                    "classification"
                                            ).asText(),
                                            "classification"
                                    )
                            )
                            .changeNature(
                                    parseEnum(
                                            SemanticChangeNature.class,
                                            resultNode.path(
                                                    "changeNature"
                                            ).asText(),
                                            "changeNature"
                                    )
                            )
                            .significance(
                                    parseEnum(
                                            SemanticSignificance.class,
                                            resultNode.path(
                                                    "significance"
                                            ).asText(),
                                            "significance"
                                    )
                            )
                            .requiresAi(false)
                            .oldMeaning(
                                    resultNode.path(
                                            "oldMeaning"
                                    ).asText()
                            )
                            .newMeaning(
                                    resultNode.path(
                                            "newMeaning"
                                    ).asText()
                            )
                            .summary(
                                    resultNode.path(
                                            "summary"
                                    ).asText()
                            )
                            .build()
            );
        }

        if (!requestById.isEmpty()) {

            throw new SemanticAiException(
                    "OpenAI response is missing semantic results for: "
                            + requestById.keySet()
            );
        }

        return results;
    }

    private String extractOutputText(
            JsonNode response) {

        JsonNode output =
                response.path("output");

        if (!output.isArray()) {

            throw new SemanticAiException(
                    "OpenAI response does not contain output."
            );
        }

        for (JsonNode outputItem : output) {

            if (!"message".equals(
                    outputItem.path("type").asText())) {

                continue;
            }

            JsonNode content =
                    outputItem.path("content");

            if (!content.isArray()) {
                continue;
            }

            for (JsonNode contentItem : content) {

                String type =
                        contentItem.path("type").asText();

                if ("refusal".equals(type)) {

                    throw new SemanticAiException(
                            "OpenAI refused the semantic analysis request."
                    );
                }

                if ("output_text".equals(type)) {

                    String text =
                            contentItem.path("text").asText();

                    if (!text.isBlank()) {
                        return text;
                    }
                }
            }
        }

        throw new SemanticAiException(
                "OpenAI response contains no output_text."
        );
    }

    private <E extends Enum<E>> E parseEnum(
            Class<E> enumClass,
            String value,
            String fieldName) {

        try {

            return Enum.valueOf(
                    enumClass,
                    value
            );

        } catch (Exception exception) {

            throw new SemanticAiException(
                    "Invalid OpenAI value for "
                            + fieldName
                            + ": "
                            + value
            );
        }
    }

    private String safeText(
            String value) {

        return value == null
                ? ""
                : value;
    }
}