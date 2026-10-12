package com.scse.curriculum.syllabus.comparison.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scse.curriculum.syllabus.comparison.config.OpenAiSemanticProperties;
import com.scse.curriculum.syllabus.comparison.dto.SemanticComparisonRequest;
import com.scse.curriculum.syllabus.comparison.dto.SemanticProviderStatusResponse;
import com.scse.curriculum.syllabus.comparison.model.SemanticChangeType;
import com.scse.curriculum.syllabus.comparison.model.SemanticSectionType;
import com.scse.curriculum.syllabus.comparison.service.AiSemanticComparisonService;
import com.scse.curriculum.syllabus.comparison.service.BasicSemanticComparisonService;
import com.scse.curriculum.syllabus.comparison.service.SemanticProviderStatusService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class GroqSemanticHttpTest {

    @Test
    void configuredClientProbesAndAnalyzesThroughExistingResponsesContract() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AtomicReference<JsonNode> sent = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/openai/v1/models", exchange -> {
            byte[] body = "{\"data\":[{\"id\":\"openai/gpt-oss-20b\"}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/openai/v1/responses", exchange -> {
            sent.set(mapper.readTree(exchange.getRequestBody()));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            JsonNode result = mapper.readTree("""
                    {"results":[{"itemId":"objective","classification":"MEANINGFUL_CHANGE",
                    "changeNature":"COGNITIVE_LEVEL_INCREASE","significance":"HIGH",
                    "oldMeaning":"Conceptual understanding of databases.",
                    "newMeaning":"Ability to design relational databases.",
                    "summary":"The expected competency changes from understanding to design."}]}
                    """);
            var response = mapper.createObjectNode().put("status", "completed");
            response.putArray("output").addObject().put("type", "message")
                    .putArray("content").addObject().put("type", "output_text")
                    .put("text", mapper.writeValueAsString(result));
            byte[] body = mapper.writeValueAsBytes(response);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            OpenAiSemanticProperties properties = new OpenAiSemanticProperties();
            properties.setEnabled(true);
            properties.setApiKey("test-key");
            properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/openai/v1");
            OpenAiSemanticClient client = new OpenAiSemanticClient(mapper, properties);
            assertThat(new SemanticProviderStatusService(properties, client).getStatus().getStatus())
                    .isEqualTo(SemanticProviderStatusResponse.Status.READY);
            var request = SemanticComparisonRequest.builder().itemId("objective")
                    .sectionType(SemanticSectionType.COURSE_OBJECTIVE).fieldName("objectives")
                    .oldText("Understand basic database concepts.")
                    .newText("Design relational databases using SQL.").build();
            var results = new AiSemanticComparisonService(new BasicSemanticComparisonService(), client, properties)
                    .compare(List.of(request));
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().getClassification()).isEqualTo(SemanticChangeType.MEANINGFUL_CHANGE);
            assertThat(results.getFirst().getOldText()).isEqualTo(request.getOldText());
            assertThat(results.getFirst().getNewText()).isEqualTo(request.getNewText());
            assertThat(results.getFirst().isRequiresAi()).isFalse();
            assertThat(authorization.get()).isEqualTo("Bearer test-key");
            assertThat(sent.get().path("model").asText()).isEqualTo("openai/gpt-oss-20b");
            assertThat(sent.get().path("text").path("format").path("type").asText()).isEqualTo("json_schema");
            assertThat(sent.get().path("text").path("format").path("strict").asBoolean()).isTrue();
            assertThat(sent.get().path("input").get(1).path("content").asText())
                    .contains("objective", request.getOldText(), request.getNewText());
        } finally {
            server.stop(0);
        }
    }
}
