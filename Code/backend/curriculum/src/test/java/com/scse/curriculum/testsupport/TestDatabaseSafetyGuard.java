package com.scse.curriculum.testsupport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

/** Test-classpath only: reject unsafe overrides before any datasource is created. */
public final class TestDatabaseSafetyGuard implements EnvironmentPostProcessor, Ordered {
    public static final String TEST_URL = "jdbc:mysql://localhost:3306/curriculum_iu_test"
            + "?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8";

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.acceptsProfiles(Profiles.of("test"))
                || !TEST_URL.equals(environment.getProperty("spring.datasource.url"))) {
            throw new IllegalStateException("Tests require profile test and the dedicated curriculum_iu_test datasource.");
        }
        for (String property : new String[] {
                "spring.datasource.hikari.jdbc-url", "spring.datasource.jndi-name",
                "spring.datasource.hikari.connection-init-sql",
                "spring.datasource.hikari.catalog", "spring.datasource.hikari.schema",
                "spring.jpa.properties.hibernate.default_catalog",
                "spring.jpa.properties.hibernate.default_schema",
                "spring.jpa.properties.hibernate.connection.url",
                "spring.jpa.properties.jakarta.persistence.jdbc.url"}) {
            if (environment.containsProperty(property)) {
                throw new IllegalStateException("Unsafe test datasource override: " + property);
            }
        }
        if (!"never".equals(environment.getProperty("spring.sql.init.mode"))) {
            throw new IllegalStateException("Tests must use explicit fixtures, not development SQL initialization.");
        }
        System.out.println("TEST DATABASE SAFETY: profile=test, datasource=" + TEST_URL);
    }
}
