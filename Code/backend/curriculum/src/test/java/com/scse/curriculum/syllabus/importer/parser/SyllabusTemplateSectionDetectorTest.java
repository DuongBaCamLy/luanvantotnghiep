package com.scse.curriculum.syllabus.importer.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import com.scse.curriculum.syllabus.importer.dto.AssessmentImportData;
import com.scse.curriculum.syllabus.importer.dto.CloImportData;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import com.scse.curriculum.syllabus.importer.dto.TopicImportData;

class SyllabusTemplateSectionDetectorTest {

    @Test
    void pdfProfileFollowsActualSourceSectionOrder() {
        SyllabusImportData data = SyllabusImportData.builder()
                .sourceCourseCode("IT999")
                .sourceCourseName("Template Variant")
                .objectives("Objective")
                .clos(List.of(
                        CloImportData.builder()
                                .code("CLO1")
                                .description("Outcome")
                                .orderIndex(1)
                                .build()))
                .assessments(List.of(
                        AssessmentImportData.builder()
                                .name("Final examination")
                                .assessmentType("FINAL_EXAM")
                                .weightPercent(100f)
                                .orderIndex(1)
                                .build()))
                .readings(List.of(
                        SyllabusImportData.ReadingItem.builder()
                                .title("Reference")
                                .author("Author")
                                .build()))
                .build();

        String source = """
                COURSE SYLLABUS

                General information
                Course Code: IT999

                Reading list
                [1] Reference

                Course objectives
                Objective

                Assessment plan
                Final examination (100%)

                Course learning outcomes
                CLO1 Outcome
                """;

        List<SyllabusImportData.TemplateSection> sections =
                SyllabusTemplateSectionDetector.fromPdfText(
                        source,
                        data);

        assertThat(sections)
                .extracting(SyllabusImportData.TemplateSection::getKey)
                .containsSubsequence(
                        "general",
                        "readings",
                        "requirements",
                        "assessment",
                        "clo");
    }
    @Test
    void pdfProfilePreservesRecognizedReadingFieldsEvenWhenValuesWereNotParsed() {
        SyllabusImportData data =
                SyllabusImportData.builder()
                        .build();

        String source = """
                COURSE SYLLABUS

                Reading List
                Title | Author | Publisher | Year | Usage Type
                """;

        List<SyllabusImportData.TemplateSection> sections =
                SyllabusTemplateSectionDetector.fromPdfText(
                        source,
                        data);

        SyllabusImportData.TemplateSection readings =
                sections.stream()
                        .filter(section ->
                                "readings".equals(section.getKey()))
                        .findFirst()
                        .orElseThrow();

        assertThat(readings.getFields())
                .extracting(SyllabusImportData.TemplateField::getKey)
                .contains(
                        "title",
                        "author",
                        "publisher",
                        "year",
                        "usageType");
    }
        @Test
    void pdfReadingFieldEvidenceDoesNotLeakFromOtherSections() {
        SyllabusImportData data =
                SyllabusImportData.builder()
                        .build();

        String source = """
                COURSE SYLLABUS

                Reading List
                Title

                Assessment Plan
                Author | Publisher | Year
                """;

        List<SyllabusImportData.TemplateSection> sections =
                SyllabusTemplateSectionDetector.fromPdfText(
                        source,
                        data);

        SyllabusImportData.TemplateSection readings =
                sections.stream()
                        .filter(section ->
                                "readings".equals(section.getKey()))
                        .findFirst()
                        .orElseThrow();

        assertThat(readings.getFields())
                .extracting(SyllabusImportData.TemplateField::getKey)
                .contains("title")
                .doesNotContain(
                        "author",
                        "publisher",
                        "year");
    }    
    @Test
    void pdfProfilePreservesRecognizedSectionEvenWhenParserExtractedNoFields() {
        SyllabusImportData data =
                SyllabusImportData.builder()
                        .build();

        String source = """
                COURSE SYLLABUS

                Course learning outcomes
                CLO Code | Description | Competency Level
                """;

        List<SyllabusImportData.TemplateSection> sections =
                SyllabusTemplateSectionDetector.fromPdfText(
                        source,
                        data);

        assertThat(sections)
                .extracting(SyllabusImportData.TemplateSection::getKey)
                .contains("clo");
    }
    
        @Test
    void pdfProfilePreservesRecognizedCloFieldsEvenWhenValuesWereNotParsed() {
        SyllabusImportData data =
                SyllabusImportData.builder()
                        .build();

        String source = """
                COURSE SYLLABUS

                Course learning outcomes
                CLO Code | Description | Competency Level
                """;

        List<SyllabusImportData.TemplateSection> sections =
                SyllabusTemplateSectionDetector.fromPdfText(
                        source,
                        data);

        SyllabusImportData.TemplateSection clo =
                sections.stream()
                        .filter(section ->
                                "clo".equals(section.getKey()))
                        .findFirst()
                        .orElseThrow();

        assertThat(clo.getFields())
                .extracting(SyllabusImportData.TemplateField::getKey)
                .contains(
                        "code",
                        "description",
                        "competencyLevel");
    }
    

        @Test
    void pdfCloFieldEvidenceDoesNotLeakFromOtherSections() {
        SyllabusImportData data =
                SyllabusImportData.builder()
                        .build();

        String source = """
                COURSE SYLLABUS

                Course learning outcomes
                CLO Code

                Assessment plan
                Description | Weight (%)
                """;

        List<SyllabusImportData.TemplateSection> sections =
                SyllabusTemplateSectionDetector.fromPdfText(
                        source,
                        data);

        SyllabusImportData.TemplateSection clo =
                sections.stream()
                        .filter(section ->
                                "clo".equals(section.getKey()))
                        .findFirst()
                        .orElseThrow();

        assertThat(clo.getFields())
                .extracting(SyllabusImportData.TemplateField::getKey)
                .contains("code")
                .doesNotContain("description");
    }
    @Test
    void pdfProfileDoesNotInventAbsentPlannedActivities() {
        SyllabusImportData data = SyllabusImportData.builder()
                .sourceCourseCode("IT999")
                .sourceCourseName("Minimal Template")
                .objectives("Objective")
                .build();

        String source = """
                COURSE SYLLABUS
                General information
                Course Code: IT999
                Course objectives
                Objective
                """;

        List<SyllabusImportData.TemplateSection> sections =
                SyllabusTemplateSectionDetector.fromPdfText(
                        source,
                        data);

        assertThat(sections)
                .extracting(SyllabusImportData.TemplateSection::getKey)
                .contains("general", "requirements")
                .doesNotContain(
                        "plannedActivities",
                        "assessment",
                        "readings",
                        "cloPlo",
                        "assessmentClo");
    }
    @Test
    void pdfProfilePreservesRecognizedContentFieldsEvenWhenValuesWereNotParsed() {
        SyllabusImportData data =
                SyllabusImportData.builder()
                        .build();

        String source = """
                COURSE SYLLABUS

                Course Content
                Topic | Week | Teaching Method | Learning Activity
                """;

        List<SyllabusImportData.TemplateSection> sections =
                SyllabusTemplateSectionDetector.fromPdfText(
                        source,
                        data);

        SyllabusImportData.TemplateSection content =
                sections.stream()
                        .filter(section ->
                                "content".equals(section.getKey()))
                        .findFirst()
                        .orElseThrow();

        assertThat(content.getFields())
                .extracting(SyllabusImportData.TemplateField::getKey)
                .contains(
                        "name",
                        "weekNumber",
                        "teachingMethod",
                        "learningActivity");
    }
        @Test
    void pdfContentFieldEvidenceDoesNotLeakFromOtherSections() {
        SyllabusImportData data =
                SyllabusImportData.builder()
                        .build();

        String source = """
                COURSE SYLLABUS

                Course Content
                Topic

                Assessment Plan
                Week | Teaching Method
                """;

        List<SyllabusImportData.TemplateSection> sections =
                SyllabusTemplateSectionDetector.fromPdfText(
                        source,
                        data);

        SyllabusImportData.TemplateSection content =
                sections.stream()
                        .filter(section ->
                                "content".equals(section.getKey()))
                        .findFirst()
                        .orElseThrow();

        assertThat(content.getFields())
                .extracting(SyllabusImportData.TemplateField::getKey)
                .contains("name")
                .doesNotContain(
                        "weekNumber",
                        "teachingMethod");
    }
    @Test
    void docxProfileFollowsReorderedHeadings() throws Exception {
        SyllabusImportData data = SyllabusImportData.builder()
                .sourceCourseCode("IT999")
                .sourceCourseName("DOCX Variant")
                .objectives("Objective")
                .clos(List.of(
                        CloImportData.builder()
                                .code("CLO1")
                                .description("Outcome")
                                .orderIndex(1)
                                .build()))
                .topics(List.of(
                        TopicImportData.builder()
                                .weekNumber(1)
                                .orderInWeek(1)
                                .name("Topic A")
                                .build()))
                .build();

        try (XWPFDocument document =
                     new XWPFDocument()) {

            document.createParagraph()
                    .createRun()
                    .setText("Course Content");

            document.createParagraph()
                    .createRun()
                    .setText("Course Learning Outcomes");

            document.createParagraph()
                    .createRun()
                    .setText("Course Objectives");

            List<SyllabusImportData.TemplateSection> sections =
                    SyllabusTemplateSectionDetector.fromDocx(
                            document.getBodyElements(),
                            data);

            assertThat(sections)
                    .extracting(SyllabusImportData.TemplateSection::getKey)
                    .containsSubsequence(
                            "content",
                            "clo",
                            "requirements");
        }
    }


        @Test
    void docxProfilePreservesRecognizedSectionEvenWhenParserExtractedNoFields()
            throws Exception {

        SyllabusImportData data =
                SyllabusImportData.builder()
                        .build();

        try (XWPFDocument document =
                     new XWPFDocument()) {

            document.createParagraph()
                    .createRun()
                    .setText("Course Learning Outcomes");

            List<SyllabusImportData.TemplateSection> sections =
                    SyllabusTemplateSectionDetector.fromDocx(
                            document.getBodyElements(),
                            data);

            assertThat(sections)
                    .extracting(SyllabusImportData.TemplateSection::getKey)
                    .contains("clo");
        }
    }
    
        @Test
    void docxProfilePreservesRecognizedContentSectionEvenWhenParserExtractedNoFields()
            throws Exception {

        SyllabusImportData data =
                SyllabusImportData.builder()
                        .build();

        try (XWPFDocument document =
                     new XWPFDocument()) {

            document.createParagraph()
                    .createRun()
                    .setText("Course Content");

            List<SyllabusImportData.TemplateSection> sections =
                    SyllabusTemplateSectionDetector.fromDocx(
                            document.getBodyElements(),
                            data);

            assertThat(sections)
                    .extracting(SyllabusImportData.TemplateSection::getKey)
                    .contains("content");
        }
    }
        @Test
    void docxProfilePreservesAllRecognizedSectionsEvenWhenParserExtractedNoFields()
            throws Exception {

        SyllabusImportData data =
                SyllabusImportData.builder()
                        .build();

        try (XWPFDocument document =
                     new XWPFDocument()) {

            document.createParagraph()
                    .createRun()
                    .setText("Planned Learning Activities");

            document.createParagraph()
                    .createRun()
                    .setText("Assessment Plan");

            document.createParagraph()
                    .createRun()
                    .setText("Examination Forms");

            document.createParagraph()
                    .createRun()
                    .setText("Reading List");

            document.createParagraph()
                    .createRun()
                    .setText("Date Revised");

            List<SyllabusImportData.TemplateSection> sections =
                    SyllabusTemplateSectionDetector.fromDocx(
                            document.getBodyElements(),
                            data);

            assertThat(sections)
                    .extracting(SyllabusImportData.TemplateSection::getKey)
                    .containsExactly(
                            "plannedActivities",
                            "assessment",
                            "examination",
                            "readings",
                            "revision");
        }
    }
        @Test
    void pdfProfilePreservesRecognizedAssessmentFieldsEvenWhenValuesWereNotParsed() {
        SyllabusImportData data =
                SyllabusImportData.builder()
                        .build();

        String source = """
                COURSE SYLLABUS

                Assessment plan
                Assessment Name | Assessment Type | Weight (%)
                """;

        List<SyllabusImportData.TemplateSection> sections =
                SyllabusTemplateSectionDetector.fromPdfText(
                        source,
                        data);

        SyllabusImportData.TemplateSection assessment =
                sections.stream()
                        .filter(section ->
                                "assessment".equals(section.getKey()))
                        .findFirst()
                        .orElseThrow();

        assertThat(assessment.getFields())
                .extracting(SyllabusImportData.TemplateField::getKey)
                .contains(
                        "name",
                        "assessmentType",
                        "weightPercent");
    }
    
        @Test
    void pdfAssessmentFieldEvidenceDoesNotLeakFromOtherSections() {
        SyllabusImportData data =
                SyllabusImportData.builder()
                        .build();

        String source = """
                COURSE SYLLABUS

                Assessment plan
                Assessment Name

                Course Content
                Topic | Weight (%)
                """;

        List<SyllabusImportData.TemplateSection> sections =
                SyllabusTemplateSectionDetector.fromPdfText(
                        source,
                        data);

        SyllabusImportData.TemplateSection assessment =
                sections.stream()
                        .filter(section ->
                                "assessment".equals(section.getKey()))
                        .findFirst()
                        .orElseThrow();

        assertThat(assessment.getFields())
                .extracting(SyllabusImportData.TemplateField::getKey)
                .contains("name")
                .doesNotContain("weightPercent");
    }
    
    @Test
    void xlsxProfileFollowsWorkbookSheetOrderAndHeaders() throws Exception {
        SyllabusImportData data =
                SyllabusImportData.builder()
                        .build();

        try (XSSFWorkbook workbook =
                     new XSSFWorkbook()) {

            var readingSheet =
                    workbook.createSheet(
                            "Reading List");

            var readingHeader =
                    readingSheet.createRow(0);

            readingHeader.createCell(0)
                    .setCellValue("Publisher");

            readingHeader.createCell(1)
                    .setCellValue("Title");

            readingHeader.createCell(2)
                    .setCellValue("Author");

            var generalSheet =
                    workbook.createSheet(
                            "General Info");

            var generalHeader =
                    generalSheet.createRow(0);

            generalHeader.createCell(0)
                    .setCellValue("Field");

            generalHeader.createCell(1)
                    .setCellValue("Value");

            var courseCode =
                    generalSheet.createRow(1);

            courseCode.createCell(0)
                    .setCellValue("Course Code");

            courseCode.createCell(1)
                    .setCellValue("IT999");

            var objective =
                    generalSheet.createRow(2);

            objective.createCell(0)
                    .setCellValue("Objectives");

            objective.createCell(1)
                    .setCellValue("Objective");

            var cloSheet =
                    workbook.createSheet("CLO");

            var cloHeader =
                    cloSheet.createRow(0);

            cloHeader.createCell(0)
                    .setCellValue("Description");

            cloHeader.createCell(1)
                    .setCellValue("Code");

            List<SyllabusImportData.TemplateSection> sections =
                    SyllabusTemplateSectionDetector.fromXlsx(
                            workbook,
                            data);

            assertThat(sections)
                    .extracting(SyllabusImportData.TemplateSection::getKey)
                    .containsExactly(
                            "readings",
                            "general",
                            "requirements",
                            "clo");

            SyllabusImportData.TemplateSection reading =
                    sections.getFirst();

            assertThat(reading.getFields())
                    .extracting(SyllabusImportData.TemplateField::getKey)
                    .containsExactly(
                            "publisher",
                            "title",
                            "author");

            SyllabusImportData.TemplateSection clo =
                    sections.getLast();

            assertThat(clo.getFields())
                    .extracting(SyllabusImportData.TemplateField::getKey)
                    .containsExactly(
                            "description",
                            "code");
        }
    }
}
