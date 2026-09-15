package com.scse.curriculum.syllabus.comparison.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scse.curriculum.syllabus.comparison.client.OpenAiSemanticClient;
import com.scse.curriculum.syllabus.comparison.config.OpenAiSemanticProperties;
import com.scse.curriculum.syllabus.comparison.dto.SemanticProviderStatusResponse;

class SemanticProviderStatusServiceTest {

    private OpenAiSemanticProperties properties;

    private OpenAiSemanticClient semanticClient;

    private SemanticProviderStatusService service;

    @BeforeEach
    void setUp() {
        properties =
                new OpenAiSemanticProperties();

        semanticClient =
                mock(OpenAiSemanticClient.class);

        service =
                new SemanticProviderStatusService(
                        properties,
                        semanticClient);
    }

    @Test
    void disabledConfigurationDoesNotProbeExternalProvider() {
        properties.setEnabled(false);
        properties.setApiKey("secret");
        properties.setModel(
                "openai/gpt-oss-20b");
        properties.setBaseUrl(
                "https://api.groq.com/openai/v1");

        SemanticProviderStatusResponse response =
                service.getStatus();

        assertThat(response.getProvider())
                .isEqualTo("GROQ");

        assertThat(response.getStatus())
                .isEqualTo(
                        SemanticProviderStatusResponse.Status.DISABLED);

        assertThat(response.isEnabled())
                .isFalse();

        assertThat(response.isConfigured())
                .isFalse();

        assertThat(response.getReachable())
                .isNull();

        verifyNoInteractions(
                semanticClient);
    }

    @Test
    void enabledButMissingKeyIsNotConfiguredAndDoesNotProbe() {
        properties.setEnabled(true);
        properties.setApiKey(" ");
        properties.setModel(
                "openai/gpt-oss-20b");
        properties.setBaseUrl(
                "https://api.groq.com/openai/v1");

        SemanticProviderStatusResponse response =
                service.getStatus();

        assertThat(response.getStatus())
                .isEqualTo(
                        SemanticProviderStatusResponse.Status.NOT_CONFIGURED);

        assertThat(response.isConfigured())
                .isFalse();

        verifyNoInteractions(
                semanticClient);
    }

    @Test
    void reachableProviderWithConfiguredModelIsReady() {
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setModel(
                "openai/gpt-oss-20b");
        properties.setBaseUrl(
                "https://api.groq.com/openai/v1");

        when(semanticClient.probeProvider())
                .thenReturn(
                        new OpenAiSemanticClient.ProviderProbe(
                                true,
                                true,
                                "Provider is reachable and the configured semantic model is available."));

        SemanticProviderStatusResponse response =
                service.getStatus();

        assertThat(response.getProvider())
                .isEqualTo("GROQ");

        assertThat(response.getStatus())
                .isEqualTo(
                        SemanticProviderStatusResponse.Status.READY);

        assertThat(response.isEnabled())
                .isTrue();

        assertThat(response.isConfigured())
                .isTrue();

        assertThat(response.getReachable())
                .isTrue();

        assertThat(response.getModelAvailable())
                .isTrue();

        assertThat(response.getModel())
                .isEqualTo(
                        "openai/gpt-oss-20b");

        assertThat(response.getBaseUrl())
                .isEqualTo(
                        "https://api.groq.com/openai/v1");
    }

    @Test
    void reachableProviderWithoutConfiguredModelIsReportedPrecisely() {
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setModel(
                "openai/gpt-oss-20b");
        properties.setBaseUrl(
                "https://api.groq.com/openai/v1");

        when(semanticClient.probeProvider())
                .thenReturn(
                        new OpenAiSemanticClient.ProviderProbe(
                                true,
                                false,
                                "Provider is reachable, but the configured semantic model is not available."));

        SemanticProviderStatusResponse response =
                service.getStatus();

        assertThat(response.getStatus())
                .isEqualTo(
                        SemanticProviderStatusResponse.Status.MODEL_UNAVAILABLE);

        assertThat(response.getReachable())
                .isTrue();

        assertThat(response.getModelAvailable())
                .isFalse();
    }

    @Test
    void providerConnectivityFailureIsNotReportedAsReady() {
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setModel(
                "openai/gpt-oss-20b");
        properties.setBaseUrl(
                "https://api.groq.com/openai/v1");

        when(semanticClient.probeProvider())
                .thenReturn(
                        new OpenAiSemanticClient.ProviderProbe(
                                false,
                                false,
                                "Unable to reach or authenticate with the configured AI provider."));

        SemanticProviderStatusResponse response =
                service.getStatus();

        assertThat(response.getStatus())
                .isEqualTo(
                        SemanticProviderStatusResponse.Status.UNREACHABLE);

        assertThat(response.getReachable())
                .isFalse();

        assertThat(response.getModelAvailable())
                .isFalse();
    }
}
