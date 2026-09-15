package com.scse.curriculum.importer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportIssue;
import com.scse.curriculum.syllabus.importer.parser.SyllabusPdfParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Full-form regression coverage for the real IT116 syllabus supplied by the user.
 *
 * <p>The test intentionally exercises the parser without Spring or a database: import preview
 * must be a deterministic file-to-DTO operation. The JSON view also locks the API property names
 * consumed by the editable React form.</p>
 */
class SyllabusImportPdfFullFormTest {

    private static final String FIXTURE = "/syllabus-import/CS-IT116.pdf";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void suppliedOfficialPdfRemainsSourceFaithful() throws Exception {
        String sourcePdf = System.getProperty("sourcePdf");
        org.junit.jupiter.api.Assumptions.assumeTrue(
                sourcePdf != null && Files.isRegularFile(Path.of(sourcePdf)),
                "Run with -DsourcePdf=<official syllabus PDF>");

        List<SyllabusImportIssue> issues = new ArrayList<>();
        SyllabusImportData data;
        try (InputStream input = Files.newInputStream(Path.of(sourcePdf))) {
            data = new SyllabusPdfParser().parsePdf(input, issues);
        }

        assertAll("official PDF source fidelity",
                () -> assertEquals("IT089", data.getSourceCourseCode()),
                () -> assertEquals("Computer Architecture", data.getSourceCourseName()),
                () -> assertEquals("195", data.getWorkloadTotal()),
                () -> assertEquals("45 (lecture) + 30 (laboratory)", data.getWorkloadContact()),
                () -> assertEquals("120", data.getWorkloadPrivate()),
                () -> assertTrue(data.getClos().stream().allMatch(clo -> clo.getBloomLevel() == null)),
                () -> assertTrue(data.getTopics().stream().allMatch(topic ->
                        topic.getSelfStudyHours() == null || topic.getSelfStudyHours() == 0)),
                () -> assertTrue(data.getTopicCloMappings().size() >= 8),
                () -> assertTrue(data.getTopicCloMappings().stream().allMatch(mapping ->
                        data.getTopics().stream().anyMatch(topic ->
                                topic.getWeekNumber().equals(mapping.getTopicIndex())))),
                () -> assertTrue(data.getAssessmentCloMappings().size() >= 8),
                () -> assertTrue(data.getTopics().stream().allMatch(topic ->
                        topic.getOrderInWeek() == null || topic.getOrderInWeek() >= 1)));
    }

    @Test
    void mapsEverySectionOfRealIt116PdfIntoEditableFormPayload() throws Exception {
        List<SyllabusImportIssue> issues = new ArrayList<>();
        JsonNode root;

        try (InputStream input = getClass().getResourceAsStream(FIXTURE)) {
            assertNotNull(input, "Real IT116 regression fixture is missing");
            SyllabusImportData data = new SyllabusPdfParser().parsePdf(input, issues);
            root = objectMapper.valueToTree(data);
        }

        assertAll("general information",
                () -> assertEquals("IT116", text(root, "sourceCourseCode")),
                () -> assertEquals("C/C++ Programming", text(root, "sourceCourseName")),
                () -> assertEquals("Learning the basics of programming", text(root, "courseDesignation")),
                () -> assertEquals("Semester 2", text(root, "semester")),
                () -> assertEquals("MSc. Le Thanh Son", text(root, "personResponsible")),
                () -> assertEquals("English", text(root, "language")),
                () -> assertEquals("Compulsory (CS, NE, CE)", text(root, "relation")),
                () -> assertEquals("Lecture", text(root, "teachingMethods")),
                () -> assertEquals("195", text(root, "workloadTotal")),
                () -> assertEquals("45 (lecture) + 30 (laboratory)", text(root, "workloadContact")),
                () -> assertEquals("120", text(root, "workloadPrivate")),
                () -> assertEquals("4", text(root, "creditPoints")),
                () -> assertEquals("3", text(root, "lectureCredits")),
                () -> assertEquals("1", text(root, "laboratoryCredits")),
                () -> assertEquals("None", text(root, "prerequisites")),
                () -> assertTrue(text(root, "objectives").contains("foundations for further studies in IT")),
                () -> assertTrue(text(root, "objectives").contains("dynamic data types")),
                () -> assertEquals("Short-answer questions, Programming exercises", text(root, "examForms")),
                () -> assertTrue(text(root, "examRequirements").contains("minimum attendance of 80 percent")),
                () -> assertTrue(text(root, "examRequirements").contains("more than 50/100 points overall")),
                () -> assertTrue(text(root, "contentNote").contains("Weight: lecture session (3 hours)")),
                () -> assertTrue(text(root, "contentNote")
                        .contains("Teaching levels: I (Introduce); T (Teach); U (Utilize)")),
                () -> assertEquals(
                        "%Pass: Target that % of students having scores greater than 50 out of 100.",
                        text(root, "assessmentPassNote")),
                () -> assertEquals("2022-02-15", text(root, "dateRevised"))
        );

        assertClos(root.path("clos"));
        assertContentAndWeeklyPlan(root.path("topics"), root.path("weeklyActivities"));
        assertCloSloMatrix(root.path("cloPloMappings"));
        assertAssessmentPlan(root.path("assessments"), root.path("assessmentCloMappings"));
        assertReading(root.path("readings"));
        assertRubrics(root.path("rubricItems"));

        assertTrue(issues.stream().noneMatch(issue -> "ERROR".equalsIgnoreCase(issue.getSeverity())),
                () -> "Unexpected parser errors: " + issues);
    }



private static void assertClos(JsonNode clos) {
        assertEquals(3, clos.size());
        Map<String, JsonNode> byCode = byTextProperty(clos, "code");

        assertAll("CLO descriptions and competency",
                () -> assertEquals(
                        "Understand programming languages and applications, how applications work",
                        text(byCode.get("CLO1"), "description")),
                () -> assertEquals(
                        "Understand basic data structure and control flow of C programming language",
                        text(byCode.get("CLO2"), "description")),
                () -> assertEquals("Able to write applications using C", text(byCode.get("CLO3"), "description")),
                () -> assertEquals("KNOWLEDGE", text(byCode.get("CLO1"), "competencyLevel").toUpperCase(Locale.ROOT)),
                () -> assertEquals("SKILL", text(byCode.get("CLO2"), "competencyLevel").toUpperCase(Locale.ROOT)),
                () -> assertEquals("SKILL", text(byCode.get("CLO3"), "competencyLevel").toUpperCase(Locale.ROOT)),
                () -> assertEquals(List.of(1.0, 2.0, 3.0), numbers(clos, "orderIndex"))
        );
    }

    private static void assertContentAndWeeklyPlan(JsonNode topics, JsonNode weekly) {
        List<String> names = List.of(
                "Introduction to Computer and Programming Language",
                "Introduction to C Programming Language",
                "C Basic Data Types",
                "Control Flow: Branching statements",
                "Control Flow: Iteration",
                "Functions",
                "Array",
                "Pointers",
                "String",
                "File Processing",
                "Dynamic Memory Allocation",
                "Struct, Union",
                "Bitwise Operation",
                "Linked list, Stack, Queue",
                "Binary tree"
        );

        assertAll("15 content rows",
                () -> assertEquals(15, topics.size()),
                () -> assertEquals(names, values(topics, "name")),
                () -> assertTrue(StreamSupport.stream(topics.spliterator(), false)
                        .allMatch(topic -> "1".equals(text(topic, "contentWeight")))),
                () -> assertEquals("I", text(topics.get(0), "contentLevel")),
                () -> assertEquals("I, T", text(topics.get(1), "contentLevel")),
                () -> assertTrue(StreamSupport.stream(topics.spliterator(), false)
                        .skip(2)
                        .allMatch(topic -> "T, U".equals(text(topic, "contentLevel"))))
        );

        assertEquals(15, weekly.size(), "Every displayed week must have all six PDF columns");
        for (int index = 0; index < 15; index++) {
            int week = index + 1;
            JsonNode row = weekly.get(index);
            String expectedTopic = names.get(index);
            String expectedClo = week <= 3 ? "1" : "2, 3";
            String expectedAssessment = week <= 3
                    ? "Quiz"
                    : week <= 8 ? "Quiz, Lab, Midterm" : "Quiz, Lab, Final";
            String expectedActivity = week <= 3 ? "Lecture" : "Lecture, Discussion, In-class Exercise";

            assertAll("week " + week,
                    () -> assertEquals(week, row.path("week").asInt()),
                    () -> assertEquals(expectedTopic, text(row, "topic")),
                    () -> assertEquals(expectedClo, text(row, "clo")),
                    () -> assertEquals(expectedAssessment, text(row, "assessments")),
                    () -> assertEquals(expectedActivity, text(row, "learningActivities")),
                    () -> assertEquals("1", text(row, "resources"))
            );
        }
    }

    private static void assertCloSloMatrix(JsonNode mappings) {
        assertEquals(3, mappings.size(), "The source matrix contains exactly three marked cells");
        Set<String> cells = StreamSupport.stream(mappings.spliterator(), false)
                .map(mapping -> text(mapping, "cloCode") + ":" + text(mapping, "ploCode") + ":"
                        + text(mapping, "value").toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        assertEquals(Set.of("CLO1:PLO1:x", "CLO2:PLO2:xxx", "CLO3:PLO2:xxx"), cells,
                "Only the three marked source cells may be imported");
    }

    private static void assertAssessmentPlan(JsonNode assessments, JsonNode mappings) {
        assertAll("assessment rows",
                () -> assertEquals(4, assessments.size()),
                () -> assertEquals(
                        List.of("Quiz / Assignment", "Labs", "Midterm examination", "Final examination"),
                        values(assessments, "name")),
                () -> assertEquals(List.of(10.0, 20.0, 30.0, 40.0), numbers(assessments, "weightPercent"))
        );

        Map<String, Double> expected = Map.ofEntries(
                Map.entry("1:CLO1", 50.0), Map.entry("1:CLO2", 10.0), Map.entry("1:CLO3", 10.0),
                Map.entry("2:CLO1", 10.0), Map.entry("2:CLO2", 30.0), Map.entry("2:CLO3", 30.0),
                Map.entry("3:CLO1", 30.0), Map.entry("3:CLO2", 30.0), Map.entry("3:CLO3", 30.0),
                Map.entry("4:CLO1", 10.0), Map.entry("4:CLO2", 30.0), Map.entry("4:CLO3", 30.0)
        );
        Map<String, Double> actual = new LinkedHashMap<>();
        mappings.forEach(mapping -> actual.put(
                mapping.path("assessmentIndex").asInt() + ":" + text(mapping, "cloCode"),
                mapping.path("percentage").asDouble()));
        assertEquals(12, mappings.size(), "The 4 x 3 source assessment matrix has 12 cells");
        assertEquals(expected, actual, "All 12 assessment-CLO percentages must come from the PDF matrix");
    }

    private static void assertReading(JsonNode readings) {
        assertEquals(1, readings.size());
        JsonNode reading = readings.get(0);
        assertAll("reading list",
        () -> assertEquals("C How to Program 8th", text(reading, "title")),
        () -> assertEquals("Paul Deitel", text(reading, "author")),
        () -> assertEquals(2016, reading.path("year").asInt()),
        () -> assertTrue(
                text(reading, "publisher").isBlank(),
                "PDF page number must not be imported as publisher")
);
    }

    private static void assertRubrics(JsonNode rubrics) {
        assertEquals(4, rubrics.size(), "Checklist, holistic, critical-thinking and oral rubrics are distinct groups");
        Map<String, JsonNode> byType = byTextProperty(rubrics, "type");

        JsonNode checklistGroup = required(byType, "GRADING_CHECKLIST");
        JsonNode holisticGroup = required(byType, "HOLISTIC");
        JsonNode analyticGroup = required(byType, "ANALYTIC");
        JsonNode oralGroup = required(byType, "ORAL_COMMUNICATION");
        JsonNode checklist = checklistGroup.path("criteria");
        JsonNode holistic = holisticGroup.path("criteria");
        JsonNode analytic = analyticGroup.path("criteria");
        JsonNode oral = oralGroup.path("criteria");

        assertAll("rubric criteria",
                () -> assertTrue(text(checklistGroup, "title").contains("Grading checklist")),
                () -> assertTrue(checklist.size() >= 9),
                () -> assertTrue(hasCriterion(checklist, "Abstract clearly identifies purpose")),
                () -> assertTrue(hasCriterion(checklist, "Quality of Layout and Graphics")),
                () -> assertTrue(hasAnyLevelContaining(checklist, "10")),
                () -> assertTrue(text(holisticGroup, "title").contains("Holistic rubric")),
                () -> assertTrue(holistic.size() >= 6),
                () -> assertTrue(hasCriterion(holistic, "5")),
                () -> assertTrue(hasAnyLevelContaining(holistic, "complete understanding of the problem")),
                () -> assertTrue(text(analyticGroup, "title").contains("Critical thinking")),
                () -> assertEquals(5, analytic.size()),
                () -> assertTrue(hasCriterion(analytic, "Explanation of issues")),
                () -> assertTrue(allHaveFourLevels(analytic)),
                () -> assertTrue(text(oralGroup, "title").contains("Oral communication")),
                () -> assertEquals(5, oral.size()),
                () -> assertTrue(hasCriterion(oral, "Organization")),
                () -> assertTrue(hasCriterion(oral, "Central Message")),
                () -> assertTrue(allHaveFourLevels(oral))
        );
    }

    private static JsonNode required(Map<String, JsonNode> values, String key) {
        JsonNode value = values.get(key);
        assertNotNull(value, "Missing rubric type " + key);
        return value;
    }

    private static boolean hasCriterion(JsonNode criteria, String expectedPart) {
        String needle = expectedPart.toLowerCase(Locale.ROOT);
        return StreamSupport.stream(criteria.spliterator(), false)
                .map(item -> text(item, "criterion").toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains(needle));
    }

    private static boolean hasAnyLevelContaining(JsonNode criteria, String expectedPart) {
        String needle = expectedPart.toLowerCase(Locale.ROOT);
        return StreamSupport.stream(criteria.spliterator(), false)
                .flatMap(item -> StreamSupport.stream(item.spliterator(), false))
                .map(value -> value.asText("").toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains(needle));
    }

    private static boolean allHaveFourLevels(JsonNode criteria) {
        return StreamSupport.stream(criteria.spliterator(), false)
                .allMatch(item -> !text(item, "level1").isBlank()
                        && !text(item, "level2").isBlank()
                        && !text(item, "level3").isBlank()
                        && !text(item, "level4").isBlank());
    }

    private static Map<String, JsonNode> byTextProperty(JsonNode array, String property) {
        Map<String, JsonNode> result = new LinkedHashMap<>();
        array.forEach(item -> result.put(text(item, property), item));
        return result;
    }

    private static List<String> values(JsonNode array, String property) {
        return StreamSupport.stream(array.spliterator(), false)
                .map(item -> text(item, property))
                .toList();
    }

    private static List<Double> numbers(JsonNode array, String property) {
        return StreamSupport.stream(array.spliterator(), false)
                .map(item -> item.path(property).asDouble())
                .toList();
    }

    private static String text(JsonNode node, String property) {
        assertNotNull(node, "Missing object containing property " + property);
        return node.path(property).asText("").replaceAll("\\s+", " ").trim();
    }
}
