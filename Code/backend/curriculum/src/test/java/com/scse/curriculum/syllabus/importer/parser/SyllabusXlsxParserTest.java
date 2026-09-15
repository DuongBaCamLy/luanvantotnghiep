package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportIssue;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SyllabusXlsxParserTest {
    private final SyllabusXlsxParser parser = new SyllabusXlsxParser();
    private final List<SyllabusImportIssue> issues = new ArrayList<>();

    private XSSFWorkbook template() throws Exception {
        try (var input = Files.newInputStream(Path.of("../../templates/syllabus-import-template.xlsx"))) {
            return new XSSFWorkbook(input);
        }
    }

    private SyllabusImportData parse(XSSFWorkbook workbook) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        workbook.write(bytes);
        return parser.parse(new ByteArrayInputStream(bytes.toByteArray()), issues);
    }

    @Test
    void supportsOnlyXlsxExtensionOrMime() {
        assertThat(parser.supports("syllabus.XLSX", null)).isTrue();
        assertThat(parser.supports(null, SyllabusXlsxParser.MIME)).isTrue();
        assertThat(parser.supports("syllabus.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")).isFalse();
        assertThat(parser.supports("syllabus.pdf", "application/pdf")).isFalse();
        assertThat(parser.supports("syllabus.xls", "application/vnd.ms-excel")).isFalse();
    }

    @Test
    void readsActualFiveSheetTemplateWithoutFabricatingIdentityOrMappings() throws Exception {
        try (var workbook = template()) {
            var data = parse(workbook);
            assertThat(workbook.getNumberOfSheets()).isEqualTo(5);
            assertThat(data.getCourseDesignation()).isEqualTo("Core");
            assertThat(data.getCourseTypes()).isEqualTo("Required");
            assertThat(data.getSemester()).isEqualTo("HK1");
            assertThat(data.getLanguage()).isEqualTo("English");
            assertThat(data.getWorkloadTotal()).isEqualTo("135");
            assertThat(data.getWorkloadContact()).isEqualTo("45");
            assertThat(data.getWorkloadPrivate()).isEqualTo("90");
            assertThat(data.getObjectives()).isEqualTo("Provide foundational knowledge");
            assertThat(data.getRubricItems().getFirst().getTitle()).isEqualTo("Course rubric");
            assertThat(data.getClos()).singleElement().satisfies(clo -> {
                assertThat(clo.getCode()).isEqualTo("CLO1");
                assertThat(clo.getDescription()).isEqualTo("Explain core concepts");
                assertThat(clo.getDescriptionVn()).isEqualTo("Giải thích khái niệm cốt lõi");
                assertThat(clo.getBloomLevel()).isEqualTo("UNDERSTAND");
                assertThat(clo.getCompetencyLevel()).isEqualTo("KNOWLEDGE");
                assertThat(clo.getOrderIndex()).isEqualTo(1);
            });
            assertThat(data.getTopics()).singleElement().satisfies(topic -> {
                assertThat(topic.getName()).isEqualTo("Introduction");
                assertThat(topic.getTeachingHours()).isEqualTo(3);
                assertThat(topic.getLabHours()).isZero();
                assertThat(topic.getSelfStudyHours()).isEqualTo(6);
                assertThat(topic.getTopicType()).isEqualTo("LECTURE");
                assertThat(topic.getLearningActivity()).isEqualTo("Discussion");
            });
            assertThat(data.getAssessments()).singleElement().satisfies(item -> {
                assertThat(item.getName()).isEqualTo("Final Exam");
                assertThat(item.getWeightPercent()).isEqualTo(100f);
                assertThat(item.getMinScore()).isZero();
                assertThat(item.getMaxScore()).isEqualTo(10f);
            });
            assertThat(data.getReadings()).singleElement().satisfies(item -> {
                assertThat(item.getTitle()).isEqualTo("Introduction to Computing");
                assertThat(item.getYear()).isEqualTo(2026);
                assertThat(item.getType()).isEqualTo("TEXTBOOK");
            });
            assertThat(data.getWeeklyActivities()).isEmpty();
            assertThat(data.getCloPloMappings()).isEmpty();
            assertThat(data.getTopicCloMappings()).isEmpty();
            assertThat(data.getAssessmentCloMappings()).isEmpty();
            assertThat(data.getSourceCourseCode()).isNullOrEmpty();
            assertThat(issues).anySatisfy(issue -> {
                assertThat(issue.getSeverity()).isEqualTo("ERROR");
                assertThat(issue.getField()).isEqualTo("course code");
            });
        }
    }

    @Test
    void handlesBlankOptionalCellsAndReorderedCaseInsensitiveHeaders() throws Exception {
        try (var workbook = template()) {
            workbook.setSheetName(1, " clo ");
            var sheet = workbook.getSheetAt(1);
            replaceText(sheet.getRow(0).getCell(0), " DESCRIPTION ");
            replaceText(sheet.getRow(0).getCell(1), " code ");
            replaceText(sheet.getRow(1).getCell(0), "Description");
            replaceText(sheet.getRow(1).getCell(1), "clo-1");
            workbook.getSheet("Reading List").getRow(1).getCell(3).setBlank();
            var data = parse(workbook);
            assertThat(data.getClos().getFirst().getCode()).isEqualTo("CLO1");
            assertThat(data.getClos().getFirst().getDescription()).isEqualTo("Description");
            assertThat(data.getReadings().getFirst().getYear()).isNull();
        }
    }

    @Test
    void numericAndCachedFormulaValuesAreNormalized() throws Exception {
        try (var workbook = template()) {
            var value = workbook.getSheet("General Info").getRow(7).getCell(1);
            value.setCellType(CellType.NUMERIC);
            value.setCellFormula("45+90");
            workbook.getCreationHelper().createFormulaEvaluator().evaluateFormulaCell(value);
            assertThat(parse(workbook).getWorkloadTotal()).isEqualTo("135");
        }
    }

    @Test
    void invalidNumericAndDuplicateCloProduceLocatedErrors() throws Exception {
        try (var workbook = template()) {
            var sheet = workbook.getSheet("CLO");
            var row = sheet.createRow(2);
            row.createCell(0).setCellValue("clo1");
            row.createCell(1).setCellValue("Conflicting description");
            workbook.getSheet("Assessments").getRow(1).getCell(3).setCellValue(-10);
            var data = parse(workbook);
            assertThat(data.getClos()).hasSize(1);
            assertThat(issues).anySatisfy(issue -> {
                assertThat(issue.getSection()).isEqualTo("CLO");
                assertThat(issue.getRow()).isEqualTo(3);
                assertThat(issue.getMessage()).contains("Duplicate");
            });
            assertThat(issues).anySatisfy(issue -> {
                assertThat(issue.getSection()).isEqualTo("Assessments");
                assertThat(issue.getField()).isEqualTo("Weight Percent");
                assertThat(issue.getSeverity()).isEqualTo("ERROR");
            });
        }
    }

    @Test
    void malformedWorkbookProducesControlledErrorAndClosesInput() throws Exception {
        boolean[] closed = {false};
        var input = new ByteArrayInputStream(new byte[]{1, 2, 3}) {
            @Override public void close() { closed[0] = true; }
        };
        parser.parse(input, issues);
        assertThat(closed[0]).isTrue();
        assertThat(issues).singleElement().satisfies(issue -> {
            assertThat(issue.getSeverity()).isEqualTo("ERROR");
            assertThat(issue.getMessage()).startsWith("Cannot read this XLSX workbook");
        });
    }

    @Test
    void missingMandatoryHeadersAreReported() throws Exception {
        try (var workbook = template()) {
            replaceText(workbook.getSheet("General Info").getRow(0).getCell(1), "Unsupported");
            parse(workbook);
            assertThat(issues).anySatisfy(issue -> {
                assertThat(issue.getSeverity()).isEqualTo("ERROR");
                assertThat(issue.getMessage()).contains("Missing table headers");
            });
        }
    }

    @Test
    void completedIdentityProducesValidPreviewData() throws Exception {
        try (var workbook = template()) {
            for (var row : workbook.getSheet("General Info")) {
                if ("course code".equals(row.getCell(0).toString())) replaceText(row.getCell(1), "it116iu");
                if ("course name".equals(row.getCell(0).toString())) replaceText(row.getCell(1), "Introduction to Computing");
            }
            var data = parse(workbook);
            assertThat(data.getSourceCourseCode()).isEqualTo("IT116IU");
            assertThat(data.getSourceCourseName()).isEqualTo("Introduction to Computing");
            assertThat(issues).noneMatch(issue -> "ERROR".equals(issue.getSeverity()));
        }
    }

    private void replaceText(org.apache.poi.ss.usermodel.Cell cell, String value) {
        // The repository template uses inline strings; remove the old inline value first.
        cell.setBlank();
        cell.setCellValue(value);
    }
}
