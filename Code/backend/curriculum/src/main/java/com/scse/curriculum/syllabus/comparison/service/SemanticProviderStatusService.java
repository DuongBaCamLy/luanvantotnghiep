package com.scse.curriculum.syllabus.comparison.service;

import com.scse.curriculum.syllabus.comparison.client.OpenAiSemanticClient;
import com.scse.curriculum.syllabus.comparison.config.OpenAiSemanticProperties;
import com.scse.curriculum.syllabus.comparison.dto.SemanticProviderStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Exposes a safe, live semantic-provider status for the comparison UI.
 *
 * The provider probe uses the provider's model-list endpoint and never sends
 * syllabus text.
 */
@Service
@RequiredArgsConstructor
public class SemanticProviderStatusService {

    private final OpenAiSemanticProperties properties;

    private final OpenAiSemanticClient semanticClient;

    public SemanticProviderStatusResponse getStatus() {

        String provider =
                properties.providerName();

        String model =
                trimToNull(
                        properties.getModel());

        String baseUrl =
                trimToNull(
                        properties.getBaseUrl());

        if (!properties.isEnabled()) {
            return SemanticProviderStatusResponse.builder()
                    .provider(provider)
                    .status(
                            SemanticProviderStatusResponse.Status.DISABLED)
                    .enabled(false)
                    .configured(false)
                    .reachable(null)
                    .modelAvailable(null)
                    .model(model)
                    .baseUrl(baseUrl)
                    .message(
                            "AI semantic analysis is disabled by configuration.")
                    .build();
        }

        if (!properties.isConfigured()) {
            return SemanticProviderStatusResponse.builder()
                    .provider(provider)
                    .status(
                            SemanticProviderStatusResponse.Status.NOT_CONFIGURED)
                    .enabled(true)
                    .configured(false)
                    .reachable(null)
                    .modelAvailable(null)
                    .model(model)
                    .baseUrl(baseUrl)
                    .message(
                            "AI semantic analysis is enabled, but the provider configuration is incomplete.")
                    .build();
        }

        OpenAiSemanticClient.ProviderProbe probe =
                semanticClient.probeProvider();

        if (!probe.reachable()) {
            return SemanticProviderStatusResponse.builder()
                    .provider(provider)
                    .status(
                            SemanticProviderStatusResponse.Status.UNREACHABLE)
                    .enabled(true)
                    .configured(true)
                    .reachable(false)
                    .modelAvailable(false)
                    .model(model)
                    .baseUrl(baseUrl)
                    .message(
                            probe.message())
                    .build();
        }

        if (!probe.modelAvailable()) {
            return SemanticProviderStatusResponse.builder()
                    .provider(provider)
                    .status(
                            SemanticProviderStatusResponse.Status.MODEL_UNAVAILABLE)
                    .enabled(true)
                    .configured(true)
                    .reachable(true)
                    .modelAvailable(false)
                    .model(model)
                    .baseUrl(baseUrl)
                    .message(
                            probe.message())
                    .build();
        }

        return SemanticProviderStatusResponse.builder()
                .provider(provider)
                .status(
                        SemanticProviderStatusResponse.Status.READY)
                .enabled(true)
                .configured(true)
                .reachable(true)
                .modelAvailable(true)
                .model(model)
                .baseUrl(baseUrl)
                .message(
                        probe.message())
                .build();
    }

    private String trimToNull(
            String value) {

        if (value == null) {
            return null;
        }

        String trimmed =
                value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }
}
