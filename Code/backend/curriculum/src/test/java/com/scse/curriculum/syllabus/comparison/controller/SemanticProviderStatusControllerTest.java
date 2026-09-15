package com.scse.curriculum.syllabus.comparison.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scse.curriculum.syllabus.comparison.dto.SemanticProviderStatusResponse;
import com.scse.curriculum.syllabus.comparison.service.SemanticProviderStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Pure controller unit test.
 *
 * Do not use @WebMvcTest here because the application's security filter
 * chain pulls JwtAuthFilter/JwtService into the slice context. This test only
 * needs to verify that the controller delegates correctly and that the
 * response DTO serializes without exposing any secret API-key property.
 */
class SemanticProviderStatusControllerTest {

    private SemanticProviderStatusService service;

    private SemanticProviderStatusController controller;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        service =
                Mockito.mock(
                        SemanticProviderStatusService.class);

        controller =
                new SemanticProviderStatusController(
                        service);

        objectMapper =
                new ObjectMapper();
    }

    @Test
    void providerStatusReturnsSafeGroqDiagnostics()
            throws Exception {

        SemanticProviderStatusResponse response =
                SemanticProviderStatusResponse.builder()
                        .provider("GROQ")
                        .status(
                                SemanticProviderStatusResponse.Status.READY)
                        .enabled(true)
                        .configured(true)
                        .reachable(true)
                        .modelAvailable(true)
                        .model(
                                "openai/gpt-oss-20b")
                        .baseUrl(
                                "https://api.groq.com/openai/v1")
                        .message(
                                "Provider is reachable and the configured semantic model is available.")
                        .build();

        when(service.getStatus())
                .thenReturn(response);

        SemanticProviderStatusResponse result =
                controller.getStatus();

        assertThat(result)
                .isSameAs(response);

        assertThat(result.getProvider())
                .isEqualTo("GROQ");

        assertThat(result.getStatus())
                .isEqualTo(
                        SemanticProviderStatusResponse.Status.READY);

        assertThat(result.isEnabled())
                .isTrue();

        assertThat(result.isConfigured())
                .isTrue();

        assertThat(result.getReachable())
                .isTrue();

        assertThat(result.getModelAvailable())
                .isTrue();

        assertThat(result.getModel())
                .isEqualTo(
                        "openai/gpt-oss-20b");

        assertThat(result.getBaseUrl())
                .isEqualTo(
                        "https://api.groq.com/openai/v1");

        String json =
                objectMapper.writeValueAsString(
                        result);

        assertThat(json)
                .contains(
                        "\"provider\":\"GROQ\"",
                        "\"status\":\"READY\"",
                        "\"model\":\"openai/gpt-oss-20b\"")
                .doesNotContain(
                        "apiKey",
                        "secret",
                        "GROQ_API_KEY");
    }
}
