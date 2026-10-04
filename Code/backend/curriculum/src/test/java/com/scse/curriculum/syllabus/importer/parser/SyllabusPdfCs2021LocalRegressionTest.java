package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyllabusPdfCs2021LocalRegressionTest {

    private final SyllabusPdfParser parser =
            new SyllabusPdfParser();

    @Test
    void inspectRealCs2021Pdf() throws Exception {

        String sourcePath =
        System.getProperty("cs2021Pdf");

org.junit.jupiter.api.Assumptions.assumeTrue(
        sourcePath != null
                && !sourcePath.isBlank(),
        "Local CS2021 regression requires "
                + "-Dcs2021Pdf=<absolute PDF path>"
);

        Path pdf =
                Path.of(sourcePath);

        assertTrue(
                Files.isRegularFile(pdf),
                "CS2021 PDF not found: " + pdf
        );

        System.out.println();
        System.out.println(
                "========== CS2021 PDF REGRESSION =========="
        );

        System.out.println(
                "FILE   : " + pdf.toAbsolutePath()
        );

        System.out.println(
                "BYTES  : " + Files.size(pdf)
        );

        SyllabusPdfParser.ParsedPdfBatch batch;

        try (InputStream input =
                     Files.newInputStream(pdf)) {

            batch =
                    parser.parsePdfBatchWithMetadata(
                            input
                    );
        }

        System.out.println(
                "PAGES  : " + batch.pageCount()
        );

        System.out.println(
                "SECTIONS: " + batch.sections().size()
        );

        /*
         * The official CS2021 programme specification supplied
         * for this regression contains 374 PDF pages.
         *
         * Do not assert the syllabus count yet.
         * First capture what the current boundary detector sees.
         */
        assertEquals(
                374,
                batch.pageCount(),
                "Unexpected CS2021 programme PDF"
        );

        assertTrue(
                batch.sections().size() > 0,
                "Parser detected no syllabus sections"
        );

        int index = 1;

        for (SyllabusPdfParser.ParsedSyllabusSection section
                : batch.sections()) {

            SyllabusImportData data =
                    section.data();

            String courseCode =
                    data == null
                            ? null
                            : data.getSourceCourseCode();

            String courseName =
                    data == null
                            ? null
                            : data.getSourceCourseName();

            int issueCount =
                    section.issues() == null
                            ? 0
                            : section.issues().size();

            System.out.printf(
                    "%02d | pages=%d-%d | code=%s | name=%s | issues=%d%n",
                    index,
                    section.startPage(),
                    section.endPage(),
                    courseCode,
                    courseName,
                    issueCount
            );
if ("PE015IU".equalsIgnoreCase(courseCode)
        || "PE016IU".equalsIgnoreCase(courseCode)
        || "PE017IU".equalsIgnoreCase(courseCode)
        || "PE019IU".equalsIgnoreCase(courseCode)
        || "PH012".equalsIgnoreCase(courseCode)
        || "PH012IU".equalsIgnoreCase(courseCode)) {

    System.out.println(
            "  AUDIT designation="
                    + data.getCourseDesignation()
    );

    System.out.println(
            "  AUDIT language="
                    + data.getLanguage()
    );

    System.out.println(
            "  AUDIT teachingMethods="
                    + data.getTeachingMethods()
    );

    System.out.println(
            "  AUDIT objectives="
                    + data.getObjectives()
    );

    System.out.println(
            "  AUDIT examForms="
                    + data.getExamForms()
    );

    System.out.println(
            "  AUDIT examRequirements="
                    + data.getExamRequirements()
    );

    System.out.printf(
            "  AUDIT counts clos=%d topics=%d weekly=%d assessments=%d readings=%d%n",
            data.getClos() == null ? 0 : data.getClos().size(),
            data.getTopics() == null ? 0 : data.getTopics().size(),
            data.getWeeklyActivities() == null ? 0 : data.getWeeklyActivities().size(),
            data.getAssessments() == null ? 0 : data.getAssessments().size(),
            data.getReadings() == null ? 0 : data.getReadings().size()
    );

    if (section.issues() != null) {
        for (var issue : section.issues()) {
            System.out.printf(
                    "  ISSUE severity=%s | section=%s | field=%s | message=%s%n",
                    issue.getSeverity(),
                    issue.getSection(),
                    issue.getField(),
                    issue.getMessage()
            );
        }
    }
}

String semanticCode =
        courseCode == null
                ? ""
                : courseCode.trim()
                        .toUpperCase(java.util.Locale.ROOT);

if ("CHE011IU".equals(semanticCode)) {
    semanticCode = "CH011IU";
} else if (!semanticCode.endsWith("IU")
        && semanticCode.matches(
                "^(IT|PH|CH|EN|MA|PE)\\d+$")) {
    semanticCode = semanticCode + "IU";
}

long distinctTopicClo =
        data.getTopicCloMappings() == null
                ? 0
                : data.getTopicCloMappings()
                        .stream()
                        .filter(m -> m.getTopicIndex() != null)
                        .filter(m -> m.getCloCode() != null
                                && !m.getCloCode().isBlank())
                        .map(m ->
                                m.getTopicIndex()
                                        + "|"
                                        + m.getCloCode()
                                                .replaceAll("[^A-Za-z0-9]", "")
                                                .toUpperCase(java.util.Locale.ROOT))
                        .distinct()
                        .count();

long distinctCloPlo =
        data.getCloPloMappings() == null
                ? 0
                : data.getCloPloMappings()
                        .stream()
                        .filter(m -> m.getCloCode() != null
                                && !m.getCloCode().isBlank())
                        .filter(m -> m.getPloCode() != null
                                && !m.getPloCode().isBlank())
                        .map(m ->
                                m.getCloCode()
                                                .replaceAll("[^A-Za-z0-9]", "")
                                                .toUpperCase(java.util.Locale.ROOT)
                                        + "|"
                                        + m.getPloCode()
                                                .replaceAll("[^A-Za-z0-9]", "")
                                                .toUpperCase(java.util.Locale.ROOT))
                        .distinct()
                        .count();

long nonBlankReadings =
        data.getReadings() == null
                ? 0
                : data.getReadings()
                        .stream()
                        .filter(r -> r.getTitle() != null
                                && !r.getTitle().isBlank())
                        .count();

System.out.printf(
        "SEMANTIC_SHAPE | code=%s | raw=%s"
                + " | clo=%d"
                + " | topic=%d"
                + " | weekly=%d"
                + " | assessment=%d"
                + " | reading=%d"
                + " | cloPlo=%d"
                + " | topicClo=%d"
                + " | assessmentClo=%d"
                + " | objectiveLen=%d"
                + " | examFormsLen=%d"
                + " | examReqLen=%d"
+ " | topicCloDistinct=%d"
+ " | cloPloDistinct=%d"
+ " | readingPersistable=%d%n",
        semanticCode,
        courseCode,
        data.getClos() == null
                ? 0 : data.getClos().size(),
        data.getTopics() == null
                ? 0 : data.getTopics().size(),
        data.getWeeklyActivities() == null
                ? 0 : data.getWeeklyActivities().size(),
        data.getAssessments() == null
                ? 0 : data.getAssessments().size(),
        data.getReadings() == null
                ? 0 : data.getReadings().size(),
        data.getCloPloMappings() == null
                ? 0 : data.getCloPloMappings().size(),
        data.getTopicCloMappings() == null
                ? 0 : data.getTopicCloMappings().size(),
        data.getAssessmentCloMappings() == null
                ? 0 : data.getAssessmentCloMappings().size(),
        data.getObjectives() == null
                ? 0 : data.getObjectives().length(),
        data.getExamForms() == null
                ? 0 : data.getExamForms().length(),
        data.getExamRequirements() == null
        ? 0 : data.getExamRequirements().length(),
distinctTopicClo,
distinctCloPlo,
nonBlankReadings
);

java.util.Set<String> expectedPersistedTopicClo =
        new java.util.LinkedHashSet<>();

if (data.getTopicCloMappings() != null
        && data.getTopics() != null
        && data.getClos() != null) {

    for (var mapping : data.getTopicCloMappings()) {

        Integer sourceIndex =
                mapping.getTopicIndex();

        if (sourceIndex == null
                || sourceIndex < 1) {
            continue;
        }

        int topicPosition = -1;

        for (int i = 0;
                i < data.getTopics().size();
                i++) {

            if (java.util.Objects.equals(
                    data.getTopics()
                            .get(i)
                            .getWeekNumber(),
                    sourceIndex)) {

                topicPosition = i;
                break;
            }
        }

        if (topicPosition < 0
                && sourceIndex <= data.getTopics().size()) {

            topicPosition =
                    sourceIndex - 1;
        }

        if (topicPosition < 0) {
            continue;
        }

        String cloKey =
                mapping.getCloCode() == null
                        ? ""
                        : mapping.getCloCode()
                                .replaceAll(
                                        "[^A-Za-z0-9]",
                                        "")
                                .toUpperCase(
                                        java.util.Locale.ROOT);

        if (cloKey.isBlank()) {
            continue;
        }

        boolean cloExists =
                data.getClos()
                        .stream()
                        .anyMatch(clo -> {

                            String candidate =
                                    clo.getCode() == null
                                            ? ""
                                            : clo.getCode()
                                                    .replaceAll(
                                                            "[^A-Za-z0-9]",
                                                            "")
                                                    .toUpperCase(
                                                            java.util.Locale.ROOT);

                            return candidate.equals(cloKey);
                        });

        if (!cloExists) {
            continue;
        }

        expectedPersistedTopicClo.add(
                topicPosition
                        + "|"
                        + cloKey);
    }
}

System.out.printf(
        "TOPIC_CLO_PERSIST | code=%s | expected=%d%n",
        semanticCode,
        expectedPersistedTopicClo.size()
);

java.util.Set<String> weeklyAuditCodes =
        java.util.Set.of(
                "MA026IU",
                "PH013IU",
                "PH014IU",
                "PH015IU",
                "CH011IU",
                "IT153IU",
                "IT134IU",
                "IT160IU",
                "PE008IU"
        );

if (weeklyAuditCodes.contains(semanticCode)) {

    String weeks =
            data.getWeeklyActivities() == null
                    ? ""
                    : data.getWeeklyActivities()
                            .stream()
                            .map(item ->
                                    String.valueOf(
                                            item.getWeek()))
                            .collect(
                                    java.util.stream.Collectors
                                            .joining(","));

    System.out.printf(
            "WEEK_AUDIT | code=%s | count=%d | weeks=%s%n",
            semanticCode,
            data.getWeeklyActivities() == null
                    ? 0
                    : data.getWeeklyActivities().size(),
            weeks
    );
}

boolean legacyVietnamesePolitical =
        "PE015IU".equalsIgnoreCase(courseCode)
                || "PE016IU".equalsIgnoreCase(courseCode)
                || "PE017IU".equalsIgnoreCase(courseCode)
                || "PE019IU".equalsIgnoreCase(courseCode);

if (legacyVietnamesePolitical) {

    assertTrue(
            data.getObjectives() != null
                    && !data.getObjectives().isBlank(),
            courseCode + " must preserve source course objectives"
    );

    assertTrue(
            data.getClos() != null
                    && !data.getClos().isEmpty(),
            courseCode + " must preserve source learning outcomes"
    );


    assertTrue(
            data.getAssessments() != null
                    && !data.getAssessments().isEmpty(),
            courseCode + " must preserve source assessments"
    );

    assertTrue(
            data.getReadings() != null
                    && !data.getReadings().isEmpty(),
            courseCode + " must preserve source learning materials"
    );
}

if ("PH012".equalsIgnoreCase(courseCode)
        || "PH012IU".equalsIgnoreCase(courseCode)) {

    assertNotNull(
            data.getTopics(),
            "PH012 content topics must exist"
    );

   SyllabusImportData ph012 =
        batch.sections()
                .stream()
                .map(
                        SyllabusPdfParser
                                .ParsedSyllabusSection::data)
                .filter(candidate ->
                        candidate != null
                                && candidate.getSourceCourseCode() != null)
                .filter(candidate ->
                        "PH012".equalsIgnoreCase(
                                candidate.getSourceCourseCode())
                                || "PH012IU".equalsIgnoreCase(
                                candidate.getSourceCourseCode()))
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError(
                                "PH012 source syllabus was not found"));
assertTrue(
        ph012.getTopics() == null
                || ph012.getTopics().isEmpty(),
        "PH012 has no separate structured Course Content rows");

assertEquals(
        7,
        ph012.getWeeklyActivities().size(),
        "PH012 source Planned learning activities contains 7 rows");

    assertEquals(
            7,
            data.getWeeklyActivities().size(),
            "PH012 planned activities regression"
    );
}
            index++;
        }

        System.out.println(
                "==========================================="
        );
    }
}