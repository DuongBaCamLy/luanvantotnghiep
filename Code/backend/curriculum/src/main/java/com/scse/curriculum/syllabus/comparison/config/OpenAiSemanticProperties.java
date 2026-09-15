package com.scse.curriculum.syllabus.comparison.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Locale;

@Data
@Component
@ConfigurationProperties(prefix = "app.openai.semantic")
public class OpenAiSemanticProperties {

    /**
     * AI semantic analysis can be disabled without affecting
     * normal structural syllabus comparison.
     */
    private boolean enabled = false;

    /**
     * Secret provider key.
     *
     * This value must remain backend-only and must never be returned
     * by provider-status endpoints.
     */
    private String apiKey;

    /**
     * The project currently uses Groq's OpenAI-compatible Responses API.
     *
     * Keep this configurable so a future provider/model change does not
     * require rewriting business logic.
     */
    private String model = "openai/gpt-oss-20b";

    /**
     * Legacy property prefix remains app.openai.semantic for compatibility,
     * while the default runtime provider is Groq.
     */
    private String baseUrl = "https://api.groq.com/openai/v1";

    private int timeoutSeconds = 30;

    /**
     * Upper bound per semantic AI batch.
     */
    private int maxBatchSize = 20;

    private int maxOutputTokens = 4000;

    /**
     * Configuration readiness only.
     *
     * This does NOT claim that the external provider is reachable.
     * Live reachability/model availability is checked separately through
     * the provider probe.
     */
    public boolean isConfigured() {
        return enabled
                && notBlank(apiKey)
                && notBlank(model)
                && notBlank(baseUrl);
    }

    /**
     * Human-readable provider name derived from the configured endpoint.
     * No secret value is exposed.
     */
    public String providerName() {
        if (!notBlank(baseUrl)) {
            return "UNKNOWN";
        }

        try {
            String host =
                    URI.create(baseUrl.trim())
                            .getHost();

            if (host == null) {
                return "CUSTOM";
            }

            String normalized =
                    host.toLowerCase(Locale.ROOT);

            if (normalized.equals("api.groq.com")
                    || normalized.endsWith(".groq.com")) {
                return "GROQ";
            }

            if (normalized.equals("api.openai.com")
                    || normalized.endsWith(".openai.com")) {
                return "OPENAI";
            }

            return "CUSTOM";
        } catch (IllegalArgumentException exception) {
            return "CUSTOM";
        }
    }

    private boolean notBlank(String value) {
        return value != null
                && !value.isBlank();
    }
}
