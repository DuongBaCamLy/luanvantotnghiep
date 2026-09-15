package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.syllabus.source.entity.SourceDocument;
import com.scse.curriculum.testsupport.Cs2026DatabaseFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SyllabusPdfFullProgramRegressionTest extends Cs2026DatabaseFixture {

    @Autowired
    private SyllabusPdfParser parser;


    @Test
    void reparsesRealCs2026PdfAndRecoversIt064AndIt089() throws Exception {

        SourceDocument source =
                sourceDocumentRepository.findById(48L)
                        .orElseThrow(() ->
                                new AssertionError(
                                        "Source document 48 not found"
                                )
                        );

        assertEquals(
                "application/pdf",
                source.getContentType()
        );

        assertNotNull(
                source.getContent()
        );

        assertTrue(
                source.getContent().length > 0
        );

        SyllabusPdfParser.ParsedPdfBatch batch =
                parser.parsePdfBatchWithMetadata(
                        new ByteArrayInputStream(
                                source.getContent()
                        )
                );

        System.out.println(
                "PDF pages = " + batch.pageCount()
        );

        SyllabusPdfParser.ParsedSyllabusSection it064 =
                findCourse(
                        batch,
                        "IT064IU"
                );

        SyllabusPdfParser.ParsedSyllabusSection it089 =
                findCourse(
                        batch,
                        "IT089IU"
                );

                

        printResult(
                "IT064IU",
                it064
        );

        printResult(
                "IT089IU",
                it089
        );

        /*
         * IT064IU source:
         *
         * CLO1: ... CLO5:
         *
         * This is the regression case for
         * colon-separated CLO declarations.
         */
        assertEquals(
                5,
                it064.data()
                        .getClos()
                        .size(),
                "IT064IU must recover all 5 CLOs"
        );

        assertEquals(
                7,
                it064.data()
                        .getCloPloMappings()
                        .size(),
                "IT064IU must recover all 7 matrix entries"
        );

        /*
         * IT089IU source:
         *
         * 0. Learning Outcomes Matrix
         *
         * This is the regression case for
         * non-standard section numbering.
         */
        assertEquals(
                5,
                it089.data()
                        .getClos()
                        .size(),
                "IT089IU must contain all 5 CLOs"
        );

        assertEquals(
                6,
                it089.data()
                        .getCloPloMappings()
                        .size(),
                "IT089IU must recover all 6 matrix entries"
        );

        assertTrue(
                it064.issues()
                        .stream()
                        .noneMatch(issue ->
                                "ERROR".equalsIgnoreCase(
                                        issue.getSeverity()
                                )
                        ),
                () -> "IT064IU parser errors: "
                        + it064.issues()
        );

        assertTrue(
                it089.issues()
                        .stream()
                        .noneMatch(issue ->
                                "ERROR".equalsIgnoreCase(
                                        issue.getSeverity()
                                )
                        ),
                () -> "IT089IU parser errors: "
                        + it089.issues()
        );
    }

    private SyllabusPdfParser.ParsedSyllabusSection findCourse(
            SyllabusPdfParser.ParsedPdfBatch batch,
            String courseCode) {

        return batch.sections()
                .stream()
                .filter(section ->
                        section.data() != null
                                && courseCode.equalsIgnoreCase(
                                section.data()
                                        .getSourceCourseCode()
                        )
                )
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError(
                                "Course not detected: "
                                        + courseCode
                        )
                );
    }

    private void printResult(
            String courseCode,
            SyllabusPdfParser.ParsedSyllabusSection section) {

        System.out.println(
                courseCode
                        + " pages="
                        + section.startPage()
                        + "-"
                        + section.endPage()
        );

        System.out.println(
                courseCode
                        + " CLO count="
                        + section.data()
                        .getClos()
                        .size()
        );

        System.out.println(
                courseCode
                        + " mappings="
                        + section.data()
                        .getCloPloMappings()
                        .stream()
                        .map(mapping ->
                                mapping.getCloCode()
                                        + "->"
                                        + mapping.getPloCode()
                                        + "="
                                        + mapping.getValue()
                        )
                        .toList()
        );
    }
}