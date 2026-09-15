package com.scse.curriculum.testsupport;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestDatabaseSafetyGuardTest {
    private final TestDatabaseSafetyGuard guard = new TestDatabaseSafetyGuard();

    private MockEnvironment safeEnvironment() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.url", TestDatabaseSafetyGuard.TEST_URL)
                .withProperty("spring.sql.init.mode", "never");
        environment.setActiveProfiles("test");
        return environment;
    }

    @Test
    void acceptsOnlyDedicatedTestDatasource() {
        assertThatCode(() -> guard.postProcessEnvironment(safeEnvironment(), null)).doesNotThrowAnyException();
    }

    @Test
    void rejectsDevelopmentUrlOverrideBeforeConnecting() {
        MockEnvironment environment = safeEnvironment()
                .withProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/curriculum_iu");
        assertThatThrownBy(() -> guard.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsMissingProfileAndMissingUrlInsteadOfFallingBack() {
        assertThatThrownBy(() -> guard.postProcessEnvironment(new MockEnvironment(), null))
                .isInstanceOf(IllegalStateException.class);
        MockEnvironment environment = safeEnvironment();
        environment.setActiveProfiles("development");
        assertThatThrownBy(() -> guard.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsPoolUrlOverrideAndAutomaticSqlInitialization() {
        assertThatThrownBy(() -> guard.postProcessEnvironment(safeEnvironment()
                .withProperty("spring.datasource.hikari.jdbc-url", "jdbc:mysql://localhost:3306/curriculum_iu"), null))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> guard.postProcessEnvironment(safeEnvironment()
                .withProperty("spring.sql.init.mode", "always"), null))
                .isInstanceOf(IllegalStateException.class);
    }
}
