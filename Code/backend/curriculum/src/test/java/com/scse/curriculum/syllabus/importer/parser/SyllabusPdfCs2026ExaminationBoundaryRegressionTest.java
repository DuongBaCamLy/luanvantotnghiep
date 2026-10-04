package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import com.scse.curriculum.syllabus.source.entity.SourceDocument;
import com.scse.curriculum.testsupport.Cs2026DatabaseFixture;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional(readOnly = true)
class SyllabusPdfCs2026ExaminationBoundaryRegressionTest
        extends Cs2026DatabaseFixture {

    @Autowired
    private SyllabusPdfParser parser;

    private SyllabusPdfParser.ParsedPdfBatch loadBatch()
            throws Exception {

        SourceDocument source =
                sourceDocumentRepository.findById(48L)
                        .orElseThrow(() ->
                                new AssertionError(
                                        "CS2026 source document 48 is missing"));

        return parser.parsePdfBatchWithMetadata(
                new ByteArrayInputStream(
                        source.getContent()));
    }

    @Test
    void realCs2026It120SeparatesExaminationRequirements()
            throws Exception {

        SyllabusPdfParser.ParsedPdfBatch batch =
                loadBatch();

        assertEquals(517, batch.pageCount());
        assertEquals(55, batch.sections().size());

        SyllabusImportData data =
                batch.sections()
                        .stream()
                        .map(SyllabusPdfParser.ParsedSyllabusSection::data)
                        .filter(item ->
                                item != null
                                        && "IT120IU".equalsIgnoreCase(
                                        item.getSourceCourseCode()))
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                "Multiple-choice questions, short-answer questions",
                data.getExamForms());

        assertNotNull(data.getExamRequirements());

        assertTrue(
                data.getExamRequirements()
                        .startsWith("Attendance:"));

        assertTrue(
                data.getExamRequirements()
                        .contains("more than 50/100"));

        assertTrue(
                data.getExamRequirements()
                        .contains("pass this course"));

        assertFalse(
                data.getExamForms()
                        .contains("Attendance:"));

        assertFalse(
                data.getExamForms()
                        .contains("Study and"));

        assertFalse(
                data.getExamRequirements()
                        .contains("Startup Owner"));

        assertFalse(
                data.getExamRequirements()
                        .contains("Sustainable Development Practice"));
    }

    @Test
    void realCs2026En012UsesActualPercentPassNote()
            throws Exception {

        SyllabusPdfParser.ParsedPdfBatch batch =
                loadBatch();

        SyllabusImportData data =
                batch.sections()
                        .stream()
                        .map(SyllabusPdfParser.ParsedSyllabusSection::data)
                        .filter(item ->
                                item != null
                                        && "EN012IU".equalsIgnoreCase(
                                        item.getSourceCourseCode()))
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                "%Pass: Target that % of students having scores greater than 50 out of 100.",
                data.getAssessmentPassNote());

        assertFalse(
                data.getAssessmentPassNote()
                        .contains("Criterion Criterion"));

        assertFalse(
                data.getAssessmentPassNote()
                        .contains("Final exam (40%)"));
    }

    @Test
    void assessmentMatrixPassCriteriaIsNotAHeading()
            throws Exception {

        String text = """
                Assessment plan
                Assessment Type CLO1 CLO2 CLO3 CLO4
                Ongoing assessment (30%) 60% Pass
                Criteria Criterion 4&
                Pass Criteria 2 Criteria Criteria Criterion
                Final exam (40%) Pass Pass Pass
                Note: %Pass: Target that % of students having scores greater than 50 out of 100.
                5. Rubrics
                """;

        Method method =
                SyllabusPdfParser.class
                        .getDeclaredMethod(
                                "parseAssessmentPassNote",
                                String.class,
                                String.class);

        method.setAccessible(true);

        String result =
                (String) method.invoke(
                        parser,
                        text,
                        text.replaceAll("\\s+", " ").trim());

        assertEquals(
                "%Pass: Target that % of students having scores greater than 50 out of 100.",
                result);
    }

    @Test
    void attendanceFirstInterleavedLayoutIsSeparated() {

        String text = """
                Examination forms Multiple-choice questions, short-answer questions
                Attendance: A minimum attendance of 80 percent is compulsory
                for the class sessions. Students will be assessed on the basis of
                Study and
                their class participation. Questions and comments are strongly
                examination
                encouraged.
                requirements
                Assignments/Examination: Students must have more than 50/100
                points overall to pass this course.
                1. Blank, S., & Dorf, B. The Startup Owner's Manual. K&S Ranch, 2020.
                Reading list
                """;

        SyllabusImportData data =
                SyllabusImportData.builder()
                        .build();

        parser.parseExaminationFields(text, data);

        assertEquals(
                "Multiple-choice questions, short-answer questions",
                data.getExamForms());

        assertNotNull(data.getExamRequirements());

        assertTrue(
                data.getExamRequirements()
                        .startsWith("Attendance:"));

        assertTrue(
                data.getExamRequirements()
                        .contains("pass this course"));

        assertFalse(
                data.getExamRequirements()
                        .contains("Startup Owner"));
    }
}