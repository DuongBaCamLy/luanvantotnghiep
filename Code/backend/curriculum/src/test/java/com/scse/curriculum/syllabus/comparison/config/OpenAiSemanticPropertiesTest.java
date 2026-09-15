package com.scse.curriculum.syllabus.comparison.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpenAiSemanticPropertiesTest {

    @Test
    void defaultProviderMatchesProjectGroqConfiguration() {
        OpenAiSemanticProperties properties =
                new OpenAiSemanticProperties();

        assertThat(properties.providerName())
                .isEqualTo("GROQ");

        assertThat(properties.getModel())
                .isEqualTo(
                        "openai/gpt-oss-20b");

        assertThat(properties.getBaseUrl())
                .isEqualTo(
                        "https://api.groq.com/openai/v1");
    }

    @Test
    void configuredRequiresEnabledKeyModelAndBaseUrl() {
        OpenAiSemanticProperties properties =
                new OpenAiSemanticProperties();

        properties.setEnabled(true);
        properties.setApiKey("test-key");

        assertThat(properties.isConfigured())
                .isTrue();

        properties.setModel(" ");

        assertThat(properties.isConfigured())
                .isFalse();
    }

    @Test
    void providerNameDoesNotExposeOrDependOnApiKey() {
        OpenAiSemanticProperties properties =
                new OpenAiSemanticProperties();

        properties.setApiKey(
                "super-secret-key");

        assertThat(properties.providerName())
                .isEqualTo("GROQ");

        assertThat(properties.providerName())
                .doesNotContain(
                        "secret");
    }
}
