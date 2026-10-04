package com.scse.curriculum.syllabus.importer.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.scse.curriculum.syllabus.importer.dto.AssessmentImportData;
import com.scse.curriculum.syllabus.importer.dto.CloImportData;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import com.scse.curriculum.syllabus.importer.dto.TopicImportData;

class SyllabusSourceProvenanceResolverChildFieldTest {

    @Test
    void childFieldsDistinguishParsedUnresolvedAndAbsent() {

        SyllabusImportData data =
                SyllabusImportData.builder()
                        .clos(
                                List.of(
                                        CloImportData.builder()
                                                .code("CLO1")
                                                .description("Explain core concepts")
                                                .descriptionVn(null)
                                                .build()))
                        .topics(
                                List.of(
                                        TopicImportData.builder()
                                                .name("Introduction")
                                                .nameVn(null)
                                                .contentLevel("I")
                                                .build()))
                        .cloPloMappings(
                                List.of(
                                        SyllabusImportData.CloPloMappingItem.builder()
                                                .cloCode("CLO1")
                                                .ploCode("PLO1")
                                                .build()))
                        .templateSections(
                                List.of(
                                        SyllabusImportData.TemplateSection.builder()
                                                .key("clo")
                                                .label("Course Learning Outcomes")
                                                .fields(
                                                        List.of(
                                                                SyllabusImportData.TemplateField.builder()
                                                                        .key("code")
                                                                        .label("CLO Code")
                                                                        .build(),
                                                                SyllabusImportData.TemplateField.builder()
                                                                        .key("description")
                                                                        .label("Description")
                                                                        .build(),
                                                                SyllabusImportData.TemplateField.builder()
                                                                        .key("descriptionVn")
                                                                        .label("Vietnamese Description")
                                                                        .build()))
                                                .build(),
                                        SyllabusImportData.TemplateSection.builder()
                                                .key("content")
                                                .label("Course Content")
                                                .fields(
                                                        List.of(
                                                                SyllabusImportData.TemplateField.builder()
                                                                        .key("name")
                                                                        .label("Topic")
                                                                        .build(),
                                                                SyllabusImportData.TemplateField.builder()
                                                                        .key("contentLevel")
                                                                        .label("Content Level")
                                                                        .build()))
                                                .build(),
                                        SyllabusImportData.TemplateSection.builder()
                                                .key("cloPlo")
                                                .label("CLO-PLO Matrix")
                                                .fields(
                                                        List.of(
                                                                SyllabusImportData.TemplateField.builder()
                                                                        .key("cloCode")
                                                                        .label("CLO")
                                                                        .build(),
                                                                SyllabusImportData.TemplateField.builder()
                                                                        .key("ploCode")
                                                                        .label("PLO")
                                                                        .build()))
                                                .build()))
                        .build();

        Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
                provenance =
                        SyllabusSourceProvenanceResolver.resolve(data);

        assertThat(
                provenance.get("clo")
                        .get("code")
                        .getState())
                .isEqualTo(
                        SyllabusImportData.SourceFieldState.PARSED);

        assertThat(
                provenance.get("clo")
                        .get("descriptionVn")
                        .getState())
                .isEqualTo(
                        SyllabusImportData.SourceFieldState.UNRESOLVED);

        assertThat(
                provenance.get("clo")
                        .get("descriptionVn")
                        .getSourceLabel())
                .isEqualTo(
                        "Vietnamese Description");

        assertThat(
                provenance.get("content")
                        .get("nameVn")
                        .getState())
                .isEqualTo(
                        SyllabusImportData.SourceFieldState.ABSENT_IN_SOURCE);

        assertThat(
                provenance.get("content")
                        .get("contentLevel")
                        .getState())
                .isEqualTo(
                        SyllabusImportData.SourceFieldState.PARSED);

        assertThat(
                provenance.get("cloPlo")
                        .get("ploCode")
                        .getState())
                .isEqualTo(
                        SyllabusImportData.SourceFieldState.PARSED);
    }

    @Test
    void activitiesAssessmentsReadingsRubricsAndExaminationAreResolved() {

        SyllabusImportData data =
                SyllabusImportData.builder()
                        .examForms("Written examination")
                        .weeklyActivities(
                                List.of(
                                        SyllabusImportData.WeeklyActivityItem.builder()
                                                .week(1)
                                                .topic("Introduction")
                                                .resources(null)
                                                .build()))
                        .assessments(
                                List.of(
                                        AssessmentImportData.builder()
                                                .name("Final examination")
                                                .weightPercent(50f)
                                                .build()))
                        .assessmentCloMappings(
                                List.of(
                                        SyllabusImportData.AssessmentCloMappingItem.builder()
                                                .cloCode("CLO1")
                                                .percentage(50d)
                                                .build()))
                        .readings(
                                List.of(
                                        SyllabusImportData.ReadingItem.builder()
                                                .title("Reference book")
                                                .author(null)
                                                .type("RECOMMENDED")
                                                .build()))
                        .rubricItems(
                                List.of(
                                        SyllabusImportData.RubricItem.builder()
                                                .title("Project rubric")
                                                .criteria(
                                                        List.of(
                                                                SyllabusImportData.RubricCriteriaItem.builder()
                                                                        .criterion("Correctness")
                                                                        .level1("Excellent")
                                                                        .build()))
                                                .build()))
                        .templateSections(
                                List.of(
                                        SyllabusImportData.TemplateSection.builder()
                                                .key("plannedActivities")
                                                .label("Planned Learning Activities")
                                                .fields(
                                                        List.of(
                                                                SyllabusImportData.TemplateField.builder()
                                                                        .key("week")
                                                                        .label("Week")
                                                                        .build(),
                                                                SyllabusImportData.TemplateField.builder()
                                                                        .key("topic")
                                                                        .label("Topic")
                                                                        .build(),
                                                                SyllabusImportData.TemplateField.builder()
                                                                        .key("resources")
                                                                        .label("Resources")
                                                                        .build()))
                                                .build(),
                                        SyllabusImportData.TemplateSection.builder()
                                                .key("assessment")
                                                .label("Assessment Plan")
                                                .fields(
                                                        List.of(
                                                                SyllabusImportData.TemplateField.builder()
                                                                        .key("name")
                                                                        .label("Assessment")
                                                                        .build(),
                                                                SyllabusImportData.TemplateField.builder()
                                                                        .key("weightPercent")
                                                                        .label("Weight (%)")
                                                                        .build()))
                                                .build(),
                                        SyllabusImportData.TemplateSection.builder()
                                                .key("assessmentClo")
                                                .label("Assessment-CLO")
                                                .fields(
                                                        List.of(
                                                                SyllabusImportData.TemplateField.builder()
                                                                        .key("cloCode")
                                                                        .label("CLO")
                                                                        .build(),
                                                                SyllabusImportData.TemplateField.builder()
                                                                        .key("contributionPercent")
                                                                        .label("Contribution (%)")
                                                                        .build()))
                                                .build(),
                                        SyllabusImportData.TemplateSection.builder()
                                                .key("examination")
                                                .label("Examination")
                                                .fields(
                                                        List.of(
                                                                SyllabusImportData.TemplateField.builder()
                                                                        .key("examForms")
                                                                        .label("Examination Forms")
                                                                        .build()))
                                                .build(),
                                        SyllabusImportData.TemplateSection.builder()
                                                .key("readings")
                                                .label("Reading List")
                                                .fields(
                                                        List.of(
                                                                SyllabusImportData.TemplateField.builder()
                                                                        .key("title")
                                                                        .label("Title")
                                                                        .build(),
                                                                SyllabusImportData.TemplateField.builder()
                                                                        .key("author")
                                                                        .label("Author")
                                                                        .build(),
                                                                SyllabusImportData.TemplateField.builder()
                                                                        .key("usageType")
                                                                        .label("Usage Type")
                                                                        .build()))
                                                .build()))
                        .build();

        Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
                provenance =
                        SyllabusSourceProvenanceResolver.resolve(data);

        assertThat(
                provenance.get("plannedActivities")
                        .get("week")
                        .getState())
                .isEqualTo(
                        SyllabusImportData.SourceFieldState.PARSED);

        assertThat(
                provenance.get("plannedActivities")
                        .get("resources")
                        .getState())
                .isEqualTo(
                        SyllabusImportData.SourceFieldState.UNRESOLVED);

        assertThat(
                provenance.get("assessment")
                        .get("weightPercent")
                        .getState())
                .isEqualTo(
                        SyllabusImportData.SourceFieldState.PARSED);

        assertThat(
                provenance.get("assessmentClo")
                        .get("contributionPercent")
                        .getState())
                .isEqualTo(
                        SyllabusImportData.SourceFieldState.PARSED);

        assertThat(
                provenance.get("examination")
                        .get("examForms")
                        .getState())
                .isEqualTo(
                        SyllabusImportData.SourceFieldState.PARSED);

        assertThat(
                provenance.get("readings")
                        .get("author")
                        .getState())
                .isEqualTo(
                        SyllabusImportData.SourceFieldState.UNRESOLVED);

        assertThat(
                provenance.get("readings")
                        .get("type")
                        .getState())
                .isEqualTo(
                        SyllabusImportData.SourceFieldState.PARSED);

        assertThat(
                provenance.get("rubrics")
                        .get("title")
                        .getState())
                .isEqualTo(
                        SyllabusImportData.SourceFieldState.PARSED);

        assertThat(
                provenance.get("rubrics")
                        .get("criterion")
                        .getState())
                .isEqualTo(
                        SyllabusImportData.SourceFieldState.PARSED);

        assertThat(
                provenance.get("rubrics")
                        .get("level1")
                        .getState())
                .isEqualTo(
                        SyllabusImportData.SourceFieldState.PARSED);
    }
}