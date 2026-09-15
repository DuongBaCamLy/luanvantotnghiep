package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.common.security.XssInputValidator;
import com.scse.curriculum.syllabus.importer.dto.CloImportData;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyllabusPdfParserSanitizationTest {

    private final SyllabusPdfParser parser =
            new SyllabusPdfParser();

    private final XssInputValidator validator =
            new XssInputValidator();

    @Test
    void separatesLegacyCs2021ExaminationRequirements() {

        String extracted = """
                Examination forms Short-answer questions, Programming exercises
                Study and examination Attendance: A minimum attendance of 80 percent is compulsory for the class
                requirements sessions. Students will be assessed on requirements the basis of their class participation.
                Questions and comments are strongly encouraged.
                Assignments/Examination: Students must have more than 50/100 points overall to pass this course.
                Reading list 1. Paul Deitel, C How to Program
                """;

        SyllabusImportData data =
                SyllabusImportData.builder()
                        .build();

        parser.parseExaminationFields(
                extracted,
                data
        );

        assertEquals(
                "Short-answer questions, Programming exercises",
                data.getExamForms()
        );

        assertNotNull(
                data.getExamRequirements()
        );

        assertTrue(
                data.getExamRequirements()
                        .startsWith("Attendance:")
        );

        assertTrue(
                data.getExamRequirements()
                        .contains(
                                "compulsory for the class sessions"
                        )
        );

        assertTrue(
                data.getExamRequirements()
                        .contains(
                                "on the basis of their class participation"
                        )
        );

        assertTrue(
                data.getExamRequirements()
                        .contains(
                                "more than 50/100 points overall"
                        )
        );

        assertFalse(
                data.getExamRequirements()
                        .contains(
                                "class requirements sessions"
                        )
        );

        assertFalse(
                data.getExamRequirements()
                        .contains(
                                "requirements the basis"
                        )
        );
    }

    @Test
    void importedTeachingMethodsRemainPlainTextAfterEveryXssSignature() {

        String extracted =
                "Discussion expression(alert), &lt;script, <math and javascript:alert(1)";

        SyllabusImportData data =
                SyllabusImportData.builder()
                        .teachingMethods(extracted)
                        .examForms("Written exam expression(alert)")
                        .clos(
                                List.of(
                                        CloImportData.builder()
                                                .code("CLO1")
                                                .description("&lt;script incomplete")
                                                .build()
                                )
                        )
                        .build();

        parser.sanitizeImportedData(data);

        assertDoesNotThrow(
                () -> validator.validate(data)
        );
    }

    @Test
    void preservesEverySemesterListedByTheSource() {

        assertEquals(
                "Semester 1, 3",
                parser.normalizeSemester(
                        "1,3 the course is taught"
                )
        );

        assertEquals(
                "Semester 5, 7",
                parser.normalizeSemester(
                        "5, 7 which the course is taught"
                )
        );

        assertEquals(
                "Semester 7",
                parser.normalizeSemester("7")
        );

        assertEquals(
                null,
                parser.normalizeSemester(
                        "the course is taught"
                )
        );
    }

    @Test
    void separatesInterleavedExaminationRequirementsFromExamForms() {

        String extracted = """
                Examination forms Short-answer questions, Programming exercises
                Study and Attendance: A minimum attendance of 80 percent is
                examination compulsory forthe class sessions.Students will be assessed on
                requirements the basis of their class participation. Questions and comments are strongly encouraged.
                Assignments/Examination: Students must have more than 50/100 points overall to pass this course.
                Reading list 1. Paul Deitel, C How to Program
                """;

        SyllabusImportData data =
                SyllabusImportData.builder()
                        .build();

        parser.parseExaminationFields(
                extracted,
                data
        );

        assertEquals(
                "Short-answer questions, Programming exercises",
                data.getExamForms()
        );

        assertNotNull(
                data.getExamRequirements()
        );

        assertTrue(
                data.getExamRequirements()
                        .startsWith("Attendance:")
        );

        assertTrue(
                data.getExamRequirements()
                        .contains(
                                "is compulsory for the class sessions"
                        )
        );

        assertTrue(
                data.getExamRequirements()
                        .contains(
                                "sessions. Students will"
                        )
        );

        assertTrue(
                data.getExamRequirements()
                        .contains(
                                "on the basis of their class participation"
                        )
        );

        assertTrue(
                data.getExamRequirements()
                        .contains(
                                "Assignments/Examination:"
                        )
        );

        assertTrue(
                data.getExamRequirements()
                        .contains(
                                "more than 50/100 points overall"
                        )
        );

        assertFalse(
                data.getExamForms()
                        .contains(
                                "Attendance:"
                        )
        );

        assertFalse(
                data.getExamRequirements()
                        .contains(
                                "examination compulsory"
                        )
        );

        assertFalse(
                data.getExamRequirements()
                        .contains(
                                "requirements the basis"
                        )
        );

        assertFalse(
                data.getExamRequirements()
                        .contains(
                                "sessions.Students"
                        )
        );
    }

    @Test
    void removesPdfPageCountersWithoutDestroyingAcademicNumbers() {

        String extracted = """
                Course objectives This course covers pointers, bit operators,
                file processing, dynamic data types.44
                44 / 517
                Course learning outcomes
                CLO 1. Understand programming concepts.
                Assignments/Examination: Students must have more than 50/100 points overall.
                Date revised: June 10, 2025
                """;

        String cleaned =
                parser.stripPdfPageArtifacts(
                        extracted);

        assertTrue(
                cleaned.contains(
                        "dynamic data types."));

        assertFalse(
                cleaned.contains(
                        "types.44"));

        assertFalse(
                cleaned.contains(
                        "44 / 517"));

        assertTrue(
                cleaned.contains(
                        "50/100"));

        assertTrue(
                cleaned.contains(
                        "2025"));
    }

    @Test
    void courseObjectivesStopBeforeCloHeadingAndPageMetadata() {

        String extracted = """
                Course objectives This course concentrates on learning the basics
                of programming languages. The course covers pointers,
                bit operators, file processing, dynamic data types.44
                44 / 517
                Course learning outcomes
                CLO 1. Understand programming languages and applications.
                """;

        String cleaned =
                parser.stripPdfPageArtifacts(
                        extracted);

        assertEquals(
                "This course concentrates on learning the basics of programming languages. "
                        + "The course covers pointers, bit operators, file processing, dynamic data types.",
                parser.parseObjectives(
                        cleaned));
    }

    @Test
    void removesOnlyLeakedWorkloadWrapperSuffix() {

        assertEquals(
                "45 (lecture) + 30 (laboratory)",
                parser.cleanWorkloadContact(
                        "45 (lecture) + 30 (laboratory) study hours)"));

        assertEquals(
                "45 (lecture) + 30 (laboratory)",
                parser.cleanWorkloadContact(
                        "45 (lecture) + 30 (laboratory)"));

        assertEquals(
                "120",
                parser.cleanWorkloadContact(
                        "120"));
    }
    @Test
void readingPageNumberMustNotBecomePublisher() throws Exception {

    SyllabusImportData data =
            SyllabusImportData.builder()
                    .build();

    /*
     * Real Program Document extraction can leave the physical PDF page
     * number after the bibliography year:
     *
     * Paul Deitel, C How to Program 8th, 2016
     * 45
     *
     * After line collapsing this must not be interpreted as publisher "45".
     */
    String extracted = """
            Reading list
            1. Paul Deitel, C How to Program 8th, 2016
            45
            Date revised:
            """;

    java.lang.reflect.Method method =
            SyllabusPdfParser.class.getDeclaredMethod(
                    "parseReadingList",
                    String.class,
                    SyllabusImportData.class);

    method.setAccessible(true);
    method.invoke(
            parser,
            extracted,
            data);

    assertEquals(
            1,
            data.getReadings().size());

    SyllabusImportData.ReadingItem reading =
            data.getReadings().getFirst();

    assertEquals(
            "Paul Deitel",
            reading.getAuthor());

    assertEquals(
            "C How to Program 8th",
            reading.getTitle());

    assertEquals(
            2016,
            reading.getYear());

    assertTrue(
            reading.getPublisher() == null
                    || reading.getPublisher().isBlank(),
            "PDF page number must not become publisher: "
                    + reading.getPublisher());
}
}