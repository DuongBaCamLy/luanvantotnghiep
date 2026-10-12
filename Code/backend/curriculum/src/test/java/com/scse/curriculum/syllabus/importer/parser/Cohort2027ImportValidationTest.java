package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import com.scse.curriculum.syllabus.importer.dto.TopicImportData;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class Cohort2027ImportValidationTest {
    private SyllabusImportData data(String name, Integer week) {
        var data = new SyllabusImportData();
        data.setTopics(List.of(TopicImportData.builder().name(name).weekNumber(week).build()));
        return data;
    }
    @Test void keepsLongSourceContentWithoutTruncatingOrCreatingAnOversizedTopic() {
        var input = data("x".repeat(512), 1);
        assertDoesNotThrow(() -> Cohort2027ImportValidation.validate(input));
        assertEquals(512, input.getTopics().get(0).getName().length());
        assertFalse(Cohort2027ImportValidation.canPersistCanonicalTopic(input.getTopics().get(0)));
    }
    @Test void preservesIndependentContentWithoutInventingAWeek() {
        var input = data("Verified content", null);
        assertDoesNotThrow(() -> Cohort2027ImportValidation.validate(input));
        assertNull(input.getTopics().get(0).getWeekNumber());
        assertFalse(Cohort2027ImportValidation.canPersistCanonicalTopic(input.getTopics().get(0)));
    }
    @Test void rejectsInvalidWeekEvenWhenIndependentStorageIsAvailable() {
        assertThrows(IllegalArgumentException.class, () -> Cohort2027ImportValidation.validate(data("Content", 0)));
    }
    @Test void requiresSourceHoursAndTypeBeforePersistingCanonicalRows() {
        var topic = data("Verified content", 7).getTopics().get(0);
        assertFalse(Cohort2027ImportValidation.canPersistCanonicalTopic(topic));
        topic.setTeachingHours(2); topic.setLabHours(0); topic.setSelfStudyHours(4); topic.setTopicType("LECTURE");
        assertTrue(Cohort2027ImportValidation.canPersistCanonicalTopic(topic));
        topic.setLabHours(null);
        assertFalse(Cohort2027ImportValidation.canPersistCanonicalTopic(topic));
    }
    @Test void acceptsVerifiedWeekAndStorageBoundaryWithoutEditingSource() {
        var data = data("x".repeat(255), 7);
        assertDoesNotThrow(() -> Cohort2027ImportValidation.validate(data));
        assertEquals(7, data.getTopics().get(0).getWeekNumber());
        assertEquals(255, data.getTopics().get(0).getName().length());
    }
    @Test void blocksCloMappingOutsideTheSourceSyllabus() {
        var input = data("Source Content", null);
        input.setClos(List.of(com.scse.curriculum.syllabus.importer.dto.CloImportData.builder().code("CLO1").description("Source outcome").build()));
        input.setCloPloMappings(List.of(SyllabusImportData.CloPloMappingItem.builder().cloCode("CLO99").ploCode("PLO1").value("4").build()));
        assertThrows(IllegalArgumentException.class, () -> Cohort2027ImportValidation.validate(input));
    }
    @Test void numericSourceMappingIsNotTreatedAsMissing() {
        var input = data("Source Content", null);
        input.setClos(List.of(com.scse.curriculum.syllabus.importer.dto.CloImportData.builder().code("CLO1").description("Source outcome").build()));
        input.setCloPloMappings(List.of(SyllabusImportData.CloPloMappingItem.builder().cloCode("CLO1").ploCode("PLO7").value("4").build()));
        assertDoesNotThrow(() -> Cohort2027ImportValidation.validate(input));
        assertEquals("4", input.getCloPloMappings().get(0).getValue());
    }
}
