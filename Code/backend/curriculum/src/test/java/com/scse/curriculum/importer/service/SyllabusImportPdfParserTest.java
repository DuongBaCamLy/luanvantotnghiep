package com.scse.curriculum.syllabus.importer.service;

import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportIssue;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SyllabusImportPdfParserTest {

    @Test
    void parsesRealIt116PdfWithoutMissingTopicWeeks() throws Exception {
        List<SyllabusImportIssue> issues = new ArrayList<>();
        try (InputStream in = getClass().getResourceAsStream("/syllabus-import/CS-IT116.pdf")) {
            assertNotNull(in, "Regression PDF must be available in test resources");

            SyllabusImportData data = SyllabusImportServiceImpl.parsePdf(in, issues);

            assertEquals("IT116", data.getSourceCourseCode());
            assertEquals("C/C++ Programming", data.getSourceCourseName());
            assertEquals(3, data.getClos().size());
            assertEquals(15, data.getTopics().size());
            assertEquals(4, data.getAssessments().size());
            assertEquals(1, data.getReadingList().size());

            assertTrue(data.getTopics().stream().allMatch(t -> t.getWeekNumber() != null),
                    () -> "Topics without week: " + data.getTopics().stream()
                            .filter(t -> t.getWeekNumber() == null)
                            .map(t -> t.getName())
                            .collect(Collectors.toList()));

            assertEquals(Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
                    data.getTopics().stream().map(t -> t.getWeekNumber()).collect(Collectors.toSet()));

            assertEquals(27, data.getTopicCloMappings().size(),
                    "Weeks 1-3 map one CLO each and weeks 4-15 map two CLOs each");
            assertTrue(data.getCloPloMappings().stream()
                            .allMatch(mapping -> mapping.getContributionWeight() != null),
                    "Every imported CLO-PLO mapping must have a database-safe contribution weight");

            assertTrue(issues.stream().noneMatch(i -> "ERROR".equalsIgnoreCase(i.getSeverity())),
                    () -> "Unexpected parser errors: " + issues);
        }
    }
}
