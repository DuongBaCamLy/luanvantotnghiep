package com.scse.curriculum.syllabus.importer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;

class SyllabusImportRubricsPersistenceTest {

    @Test
    void updateGeneralInformationPersistsImportedRubricsAsRendererCompatibleJson()
            throws Exception {

        ObjectMapper objectMapper =
                new ObjectMapper();

        SyllabusImportServiceImpl service =
                mock(
                        SyllabusImportServiceImpl.class,
                        CALLS_REAL_METHODS);

        Field objectMapperField =
                SyllabusImportServiceImpl.class
                        .getDeclaredField(
                                "objectMapper");

        objectMapperField.setAccessible(true);
        objectMapperField.set(
                service,
                objectMapper);

        SyllabusImportData.RubricCriteriaItem criterion =
                SyllabusImportData.RubricCriteriaItem.builder()
                        .criterion("Technical quality")
                        .level1("Excellent")
                        .level2("Good")
                        .level3("Developing")
                        .level4("Insufficient")
                        .build();

        SyllabusImportData.RubricItem rubric =
                SyllabusImportData.RubricItem.builder()
                        .type("ANALYTIC")
                        .title("Technical rubric")
                        .criteria(
                                List.of(
                                        criterion))
                        .build();

        SyllabusImportData data =
                SyllabusImportData.builder()
                        .rubricItems(
                                List.of(
                                        rubric))
                        .build();

        Syllabus syllabus =
                Syllabus.builder()
                        .build();

        Method method =
                SyllabusImportServiceImpl.class
                        .getDeclaredMethod(
                                "updateGeneralInformation",
                                Syllabus.class,
                                SyllabusImportData.class);

        method.setAccessible(true);

        method.invoke(
                service,
                syllabus,
                data);

        assertThat(syllabus.getRubrics())
                .isNotBlank();

        JsonNode root =
                objectMapper.readTree(
                        syllabus.getRubrics());

        JsonNode rubrics =
                root.path("rubrics");

        assertThat(rubrics.isArray())
                .isTrue();

        assertThat(rubrics)
                .hasSize(1);

        JsonNode importedRubric =
                rubrics.get(0);

        assertThat(importedRubric.path("type").asText())
                .isEqualTo("ANALYTIC");

        assertThat(importedRubric.path("title").asText())
                .isEqualTo("Technical rubric");

        assertThat(importedRubric.path("scaleLabels").isArray())
                .isTrue();

        assertThat(importedRubric.path("scaleLabels"))
                .hasSize(4);

        JsonNode criteria =
                importedRubric.path("criteria");

        assertThat(criteria.isArray())
                .isTrue();

        assertThat(criteria)
                .hasSize(1);

        assertThat(criteria.get(0).path("criterion").asText())
                .isEqualTo("Technical quality");

        assertThat(criteria.get(0).path("levels"))
                .hasSize(4);

        assertThat(criteria.get(0).path("levels").get(0).asText())
                .isEqualTo("Excellent");

        assertThat(criteria.get(0).path("levels").get(3).asText())
                .isEqualTo("Insufficient");
    }
    @Test
void emptyImportedRubricsDoNotOverwriteExistingRubrics()
        throws Exception {

    ObjectMapper objectMapper =
            new ObjectMapper();

    SyllabusImportServiceImpl service =
            mock(
                    SyllabusImportServiceImpl.class,
                    CALLS_REAL_METHODS);

    Field objectMapperField =
            SyllabusImportServiceImpl.class
                    .getDeclaredField(
                            "objectMapper");

    objectMapperField.setAccessible(true);
    objectMapperField.set(
            service,
            objectMapper);

    String existingRubrics =
            """
            {
              "rubrics": [
                {
                  "title": "Existing rubric"
                }
              ]
            }
            """;

    Syllabus syllabus =
            Syllabus.builder()
                    .rubrics(existingRubrics)
                    .build();

    SyllabusImportData data =
            SyllabusImportData.builder()
                    .rubricItems(List.of())
                    .build();

    Method method =
            SyllabusImportServiceImpl.class
                    .getDeclaredMethod(
                            "updateGeneralInformation",
                            Syllabus.class,
                            SyllabusImportData.class);

    method.setAccessible(true);

    method.invoke(
            service,
            syllabus,
            data);

    assertThat(syllabus.getRubrics())
            .isEqualTo(existingRubrics);
}

}