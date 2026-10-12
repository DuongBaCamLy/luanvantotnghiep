package com.scse.curriculum.syllabus.importer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import com.scse.curriculum.syllabus.importer.dto.TopicImportData;
import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.topic.entity.Topic;
import com.scse.curriculum.topicclo.entity.TopicClo;
import com.scse.curriculum.topicclo.repository.TopicCloRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Cohort2027ContentPersistenceTest {
    private SyllabusImportServiceImpl service() throws Exception {
        var service = mock(SyllabusImportServiceImpl.class, CALLS_REAL_METHODS);
        var field = SyllabusImportServiceImpl.class.getDeclaredField("objectMapper");
        field.setAccessible(true); field.set(service, new ObjectMapper());
        return service;
    }
    private void importTopics(SyllabusImportServiceImpl service, Syllabus syllabus, SyllabusImportData data, boolean modern) throws Exception {
        var method = SyllabusImportServiceImpl.class.getDeclaredMethod("importTopics", Syllabus.class, SyllabusImportData.class, boolean.class);
        method.setAccessible(true); method.invoke(service, syllabus, data, modern);
    }
    @Test void unmatchedContentRemainsVerbatimInNotesAndCreatesNoInventedTopic() throws Exception {
        var service = service();
        var data = new SyllabusImportData();
        data.setTopics(List.of(TopicImportData.builder().name("Double Integrals in General Regions and Applications")
                .contentWeight("2").contentLevel("T,U").build()));
        var syllabus = Syllabus.builder().build();
        importTopics(service, syllabus, data, true);
        assertTrue(syllabus.getTopics().isEmpty());
        var method = SyllabusImportServiceImpl.class.getDeclaredMethod("buildStandardFormNotes", SyllabusImportData.class, boolean.class);
        method.setAccessible(true);
        var notes = new ObjectMapper().readTree((String) method.invoke(service, data, true));
        var row = notes.path("sourceContentRows").get(0);
        assertEquals(data.getTopics().get(0).getName(), row.path("name").asText());
        assertEquals("2", row.path("weight").asText());
        assertEquals("T,U", row.path("level").asText());
        assertTrue(row.path("relatedCloCodes").isEmpty());
        assertTrue(row.path("weekNumber").isNull()); assertTrue(row.path("teachingHours").isNull());
    }
    @Test void weekWithoutSourceHoursDoesNotGetDefaultThreeHours() throws Exception {
        var data = new SyllabusImportData();
        data.setTopics(List.of(TopicImportData.builder().name("Source Content").weekNumber(7).build()));
        var syllabus = Syllabus.builder().build();
        importTopics(service(), syllabus, data, true);
        assertTrue(syllabus.getTopics().isEmpty());
    }
    @Test void fullySpecifiedSourceTopicKeepsExactHoursAndType() throws Exception {
        var data = new SyllabusImportData();
        data.setTopics(List.of(TopicImportData.builder().name("Source Lab").weekNumber(7).teachingHours(0)
                .labHours(2).selfStudyHours(4).topicType("LAB").build()));
        var syllabus = Syllabus.builder().build();
        importTopics(service(), syllabus, data, true);
        assertEquals(1, syllabus.getTopics().size());
        var topic = syllabus.getTopics().get(0);
        assertEquals(7, topic.getWeekNumber()); assertEquals(0, topic.getTeachingHours());
        assertEquals(2, topic.getLabHours()); assertEquals(4, topic.getSelfStudyHours());
        assertEquals("LAB", topic.getTopicType().name());
    }
    @Test void legacyImportBehaviorIsPreserved() throws Exception {
        var data = new SyllabusImportData();
        data.setTopics(List.of(TopicImportData.builder().name("Legacy Content").build()));
        var syllabus = Syllabus.builder().build();
        importTopics(service(), syllabus, data, false);
        assertEquals(1, syllabus.getTopics().get(0).getWeekNumber());
        assertEquals(3, syllabus.getTopics().get(0).getTeachingHours());
    }
    @Test void skippedContentDoesNotShiftMappingToAnotherTopic() throws Exception {
        var service = service();
        var repository = mock(TopicCloRepository.class);
        var field = SyllabusImportServiceImpl.class.getDeclaredField("topicCloRepository");
        field.setAccessible(true); field.set(service, repository);
        var independent = TopicImportData.builder().name("Independent Content").build();
        var canonical = TopicImportData.builder().name("Verified Lab").weekNumber(7).teachingHours(0)
                .labHours(2).selfStudyHours(4).topicType("LAB").contentLevel("T").build();
        var data = new SyllabusImportData(); data.setTopics(List.of(independent, canonical));
        data.setTopicCloMappings(List.of(
                SyllabusImportData.TopicCloMappingItem.builder().topicIndex(1).cloCode("CLO1").build(),
                SyllabusImportData.TopicCloMappingItem.builder().topicIndex(2).cloCode("CLO2").build()));
        var syllabus = Syllabus.builder().build();
        syllabus.getClos().add(Clo.builder().id(101).code("CLO1").build());
        syllabus.getClos().add(Clo.builder().id(102).code("CLO2").build());
        importTopics(service, syllabus, data, true);
        syllabus.getTopics().get(0).setId(201);
        var method = SyllabusImportServiceImpl.class.getDeclaredMethod("persistImportedTopicCloMappings", Syllabus.class, SyllabusImportData.class, boolean.class);
        method.setAccessible(true); method.invoke(service, syllabus, data, true);
        verify(repository).saveAll(argThat(rows -> {
            var iterator = rows.iterator();
            if (!iterator.hasNext()) return false;
            TopicClo row = iterator.next();
            return !iterator.hasNext() && row.getTopic().getId().equals(201) && row.getClo().getCode().equals("CLO2");
        }));
    }
}
