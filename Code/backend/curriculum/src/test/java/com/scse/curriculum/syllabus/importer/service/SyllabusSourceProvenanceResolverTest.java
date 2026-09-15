package com.scse.curriculum.syllabus.importer.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;

class SyllabusSourceProvenanceResolverTest {
    @Test
void assessmentSectionCoversSourceBackedCanonicalField() {

    SyllabusImportData data =
            SyllabusImportData.builder()
                    .assessmentPassNote("Minimum overall score is 50%")
                    .templateSections(List.of())
                    .build();

    Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
            provenance =
                    SyllabusSourceProvenanceResolver.resolve(
                            data);

    Map<String, SyllabusImportData.SourceFieldProvenance> assessment =
            provenance.get("assessment");

    assertThat(assessment)
            .containsOnlyKeys(
                    "assessmentPassNote");

    assertThat(assessment.get("assessmentPassNote").getState())
            .isEqualTo(
                    SyllabusImportData.SourceFieldState.PARSED);
}
    @Test
void contentSectionCoversSourceBackedCanonicalField() {

    SyllabusImportData data =
            SyllabusImportData.builder()
                    .contentNote("Topics may be adjusted during the semester")
                    .templateSections(List.of())
                    .build();

    Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
            provenance =
                    SyllabusSourceProvenanceResolver.resolve(
                            data);

    Map<String, SyllabusImportData.SourceFieldProvenance> content =
            provenance.get("content");

    assertThat(content)
            .containsOnlyKeys(
                    "contentNote");

    assertThat(content.get("contentNote").getState())
            .isEqualTo(
                    SyllabusImportData.SourceFieldState.PARSED);
}
    @Test
void requirementsSectionCoversAllSourceBackedCanonicalFields() {

    SyllabusImportData data =
            SyllabusImportData.builder()
                    .prerequisites("Data Structures")
                    .objectives("Understand software architecture")
                    .templateSections(List.of())
                    .build();

    Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
            provenance =
                    SyllabusSourceProvenanceResolver.resolve(
                            data);

    Map<String, SyllabusImportData.SourceFieldProvenance> requirements =
            provenance.get("requirements");

    assertThat(requirements)
            .containsOnlyKeys(
                    "prerequisites",
                    "objectives");

    assertThat(requirements.values())
            .extracting(
                    SyllabusImportData.SourceFieldProvenance::getState)
            .containsOnly(
                    SyllabusImportData.SourceFieldState.PARSED);
}
    @Test
void workloadCreditSectionCoversAllSourceBackedCanonicalFields() {

    SyllabusImportData data =
            SyllabusImportData.builder()
                    .workloadTotal("150")
                    .workloadContact("60")
                    .workloadPrivate("90")
                    .workloadStudentResponsibility("Self-study and assignments")
                    .creditPoints("6")
                    .lectureCredits("4")
                    .laboratoryCredits("2")
                    .templateSections(List.of())
                    .build();

    Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
            provenance =
                    SyllabusSourceProvenanceResolver.resolve(
                            data);

    Map<String, SyllabusImportData.SourceFieldProvenance> workloadCredit =
            provenance.get("workloadCredit");

    assertThat(workloadCredit)
            .containsOnlyKeys(
                    "workloadTotal",
                    "workloadContact",
                    "workloadPrivate",
                    "workloadStudentResponsibility",
                    "creditPoints",
                    "lectureCredits",
                    "laboratoryCredits");

    assertThat(workloadCredit.values())
            .extracting(
                    SyllabusImportData.SourceFieldProvenance::getState)
            .containsOnly(
                    SyllabusImportData.SourceFieldState.PARSED);
}
    @Test
void generalSectionCoversAllSourceBackedCanonicalFields() {

    SyllabusImportData data =
            SyllabusImportData.builder()
                    .sourceCourseCode("IT999IU")
                    .sourceCourseName("Software Engineering")
                    .courseDesignation("Required")
                    .courseTypes("Core")
                    .semester("Semester 5")
                    .personResponsible("Dr. Nguyen")
                    .language("English")
                    .relation("Compulsory")
                    .teachingMethods("Lecture")
                    .major("Computer Science")
                    .templateSections(List.of())
                    .build();

    Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
            provenance =
                    SyllabusSourceProvenanceResolver.resolve(
                            data);

    Map<String, SyllabusImportData.SourceFieldProvenance> general =
            provenance.get("general");

    assertThat(general)
            .containsOnlyKeys(
                    "courseCode",
                    "courseName",
                    "courseDesignation",
                    "courseTypes",
                    "semester",
                    "personResponsible",
                    "language",
                    "relation",
                    "teachingMethods",
                    "major");

    assertThat(general.values())
            .extracting(
                    SyllabusImportData.SourceFieldProvenance::getState)
            .containsOnly(
                    SyllabusImportData.SourceFieldState.PARSED);
}
@Test
void parsedValueIsParsedEvenWhenTemplateFieldEvidenceIsMissing() {

    SyllabusImportData data =
            SyllabusImportData.builder()
                    .language("English")
                    .templateSections(
                            List.of(
                                    SyllabusImportData.TemplateSection.builder()
                                            .key("general")
                                            .label("General Information")
                                            .fields(List.of())
                                            .build()))
                    .build();

    Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
            provenance =
                    SyllabusSourceProvenanceResolver.resolve(
                            data);

    SyllabusImportData.SourceFieldProvenance language =
            provenance.get("general")
                    .get("language");

    assertThat(language.getState())
            .isEqualTo(
                    SyllabusImportData.SourceFieldState.PARSED);

    assertThat(language.getSourceLabel())
            .isNull();
}
    @Test
    void resolvesParsedUnresolvedAndAbsentStatesFromSourceEvidence() {

        SyllabusImportData data =
                SyllabusImportData.builder()
                        .sourceCourseCode("IT999IU")
                        .sourceCourseName(null)
                        .language(null)
                        .templateSections(
                                List.of(
                                        SyllabusImportData.TemplateSection.builder()
                                                .key("general")
                                                .label("General Information")
                                                .fields(
                                                        List.of(
                                                                SyllabusImportData.TemplateField.builder()
                                                                        .key("courseCode")
                                                                        .label("Course Code")
                                                                        .build(),
                                                                SyllabusImportData.TemplateField.builder()
                                                                        .key("courseName")
                                                                        .label("Course Name")
                                                                        .build()))
                                                .build()))
                        .build();

        Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
                provenance =
                        SyllabusSourceProvenanceResolver.resolve(
                                data);

        SyllabusImportData.SourceFieldProvenance courseCode =
                provenance.get("general")
                        .get("courseCode");

        assertThat(courseCode.getState())
                .isEqualTo(
                        SyllabusImportData.SourceFieldState.PARSED);

        assertThat(courseCode.getSourceLabel())
                .isEqualTo("Course Code");


        SyllabusImportData.SourceFieldProvenance courseName =
                provenance.get("general")
                        .get("courseName");

        assertThat(courseName.getState())
                .isEqualTo(
                        SyllabusImportData.SourceFieldState.UNRESOLVED);

        assertThat(courseName.getSourceLabel())
                .isEqualTo("Course Name");


        SyllabusImportData.SourceFieldProvenance language =
                provenance.get("general")
                        .get("language");

        assertThat(language.getState())
                .isEqualTo(
                        SyllabusImportData.SourceFieldState.ABSENT_IN_SOURCE);

        assertThat(language.getSourceLabel())
                .isNull();
    }
}