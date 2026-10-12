package com.scse.curriculum.syllabus.comparison.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class GroqLocalConfigurationTest {

    @TempDir
    Path directory;

    @Test
    void missingOptionalLocalFilePreservesExistingDisabledDefault() {
        context(directory.resolve("absent.properties"))
                .withPropertyValues("GROQ_API_KEY=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    OpenAiSemanticProperties properties = context.getBean(OpenAiSemanticProperties.class);
                    assertThat(properties.isEnabled()).isFalse();
                    assertThat(properties.isConfigured()).isFalse();
                });
    }

    @Test
    void localConfigurationEnablesAiButMissingKeyCannotBeReady() throws IOException {
        context(localConfiguration())
                .withPropertyValues("GROQ_API_KEY=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    OpenAiSemanticProperties properties = context.getBean(OpenAiSemanticProperties.class);
                    assertThat(properties.isEnabled()).isTrue();
                    assertThat(properties.isConfigured()).isFalse();
                });
    }

    @Test
    void backendEnvironmentKeyMakesLocalConfigurationReady() throws IOException {
        context(localConfiguration())
                .withPropertyValues("GROQ_API_KEY=test-key")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    OpenAiSemanticProperties properties = context.getBean(OpenAiSemanticProperties.class);
                    assertThat(properties.isEnabled()).isTrue();
                    assertThat(properties.isConfigured()).isTrue();
                    assertThat(properties.getApiKey()).isEqualTo("test-key");
                    assertThat(properties.providerName()).isEqualTo("GROQ");
                    assertThat(properties.getModel()).isEqualTo("openai/gpt-oss-20b");
                    assertThat(properties.getBaseUrl()).isEqualTo("https://api.groq.com/openai/v1");
                });
    }

    @Test
    void explicitEnvironmentDisableStillTakesPrecedence() throws IOException {
        context(localConfiguration())
                .withPropertyValues("GROQ_API_KEY=test-key", "GROQ_SEMANTIC_ENABLED=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    OpenAiSemanticProperties properties = context.getBean(OpenAiSemanticProperties.class);
                    assertThat(properties.isEnabled()).isFalse();
                    assertThat(properties.isConfigured()).isFalse();
                });
    }

    @Test
    void existingEnvironmentOnlyConfigurationStillWorksWithoutLocalFile() {
        context(directory.resolve("absent.properties"))
                .withPropertyValues("GROQ_API_KEY=test-key", "GROQ_SEMANTIC_ENABLED=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(OpenAiSemanticProperties.class).isConfigured()).isTrue();
                });
    }

    private Path localConfiguration() throws IOException {
        Path file = directory.resolve("groq-local.properties");
        Files.copy(Path.of("groq-local.properties.example"), file);
        return file;
    }

    private ApplicationContextRunner context(Path localFile) {
        // Bind only the real semantic settings. Isolate the import location so
        // an actual developer key/configuration cannot affect these tests.
        Path baseFile = directory.resolve("application.properties");
        Properties application = new Properties();
        Properties semantic = new Properties();
        try (var input = getClass().getResourceAsStream("/application.properties")) {
            application.load(input);
            assertThat(application.getProperty("spring.config.import"))
                    .isEqualTo("optional:file:./groq-local.properties");
            application.stringPropertyNames().stream()
                    .filter(name -> name.startsWith("app.openai.semantic."))
                    .forEach(name -> semantic.setProperty(name, application.getProperty(name)));
            semantic.setProperty("spring.config.import", "optional:" + localFile.toUri());
            try (var output = Files.newOutputStream(baseFile)) {
                semantic.store(output, "Isolated real Groq settings; no credentials");
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return new ApplicationContextRunner()
                .withPropertyValues("spring.config.location=" + baseFile.toUri())
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(SemanticConfiguration.class);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(OpenAiSemanticProperties.class)
    static class SemanticConfiguration {
    }
}
