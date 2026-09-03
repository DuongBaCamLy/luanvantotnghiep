package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.common.security.XssInputValidator;
import com.scse.curriculum.syllabus.importer.dto.CloImportData;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;

class SyllabusPdfParserSanitizationTest {

    private final SyllabusPdfParser parser = new SyllabusPdfParser();
    private final XssInputValidator validator = new XssInputValidator();

    @Test
    void importedTeachingMethodsRemainPlainTextAfterEveryXssSignature() {
        String extracted = "Discussion expression(alert), &lt;script, <math and javascript:alert(1)";
        SyllabusImportData data = SyllabusImportData.builder()
                .teachingMethods(extracted)
                .examForms("Written exam expression(alert)")
                .clos(List.of(CloImportData.builder().code("CLO1").description("&lt;script incomplete").build()))
                .build();

        parser.sanitizeImportedData(data);
        assertDoesNotThrow(() -> validator.validate(data));
    }

    @Test
    void preservesEverySemesterListedByTheSource() {
        assertEquals("Semester 1, 3", parser.normalizeSemester("1,3 the course is taught"));
        assertEquals("Semester 5, 7", parser.normalizeSemester("5, 7 which the course is taught"));
        assertEquals("Semester 7", parser.normalizeSemester("7"));
        assertEquals(null, parser.normalizeSemester("the course is taught"));
    }
}
