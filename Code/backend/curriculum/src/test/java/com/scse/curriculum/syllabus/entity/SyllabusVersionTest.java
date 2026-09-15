package com.scse.curriculum.syllabus.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;

class SyllabusVersionTest {
    @ParameterizedTest
    @ValueSource(strings = {"Version 1", "Version 1.0", "1", "v1", "v1.0"})
    @NullSource
    void legacyResponseIsCanonicalWithoutChangingStoredHistory(String label) {
        Syllabus syllabus = Syllabus.builder().versionNumber(1).versionLabel(label).build();
        assertThat(syllabus.getVersionLabel()).isEqualTo("v1.0");
        assertThat(ReflectionTestUtils.getField(syllabus, "versionLabel")).isEqualTo(label);
        assertThat(syllabus.getUpdatedAt()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Version 1", "1", "v1", "v1.1", "v2.0", "", "v01.0"})
    void persistenceRejectsMalformedMinorOrConflictingLabels(String label) {
        Syllabus syllabus = Syllabus.builder().versionNumber(1).versionLabel(label).build();
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(syllabus, "initializeAuditTimestamps"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(syllabus, "refreshUpdatedAt"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void missingLabelIsInitializedAndInvalidNumbersRejected() {
        Syllabus syllabus = Syllabus.builder().versionNumber(1).build();
        ReflectionTestUtils.invokeMethod(syllabus, "initializeAuditTimestamps");
        assertThat(ReflectionTestUtils.getField(syllabus, "versionLabel")).isEqualTo("v1.0");
        assertThatThrownBy(() -> SyllabusVersion.format(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SyllabusVersion.format(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SyllabusVersion.format(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
