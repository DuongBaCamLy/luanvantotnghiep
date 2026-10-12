package com.scse.curriculum.syllabus.template;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Ds2027RubricNotesTest {
    private final ObjectMapper json = new ObjectMapper();
    private JsonNode resource() throws Exception {
        try (var input = getClass().getResourceAsStream("/syllabus-import/verified-ds2026-rubric-grids.json")) {
            return json.readTree(input);
        }
    }
    private ObjectNode oldNotes(JsonNode resource, JsonNode course) {
        ObjectNode notes = json.createObjectNode();
        notes.putObject("sourceImportEvidence").put("sourceSha256", resource.path("sourceSha256").asText())
                .put("courseCode", course.path("sourceCode").asText()).put("startPage", course.path("startPage").asInt())
                .put("endPage", course.path("endPage").asInt());
        notes.putObject("sourceSupplementalSections").set("11", course.path("legacySection11"));
        ((ObjectNode) notes.path("sourceSupplementalSections")).put("10", "Unchanged assessment evidence");
        notes.put("assignmentDescription", "Instructor assignment text");
        return notes;
    }
    @Test void allOldRubricBlocksResolveWithoutChangingOtherSectionsOrPersistedInput() throws Exception {
        JsonNode resource = resource();
        assertEquals(42, resource.path("courses").size());
        for (JsonNode course : resource.path("courses")) {
            var before = oldNotes(resource, course);String saved = before.toString();
            String resolved = Ds2027RubricNotes.forDisplay(saved, course.path("sourceCode").asText(), "NEW_2027");
            var expected = before.deepCopy();
            ((ObjectNode) expected.path("sourceSupplementalSections")).set("11", course.path("section11"));
            assertEquals(expected, json.readTree(resolved));
            assertEquals(saved, before.toString());
            assertEquals(resolved, Ds2027RubricNotes.forDisplay(resolved, course.path("sourceCode").asText(), "NEW_2027"));
            for (var block : course.path("section11").get(0).path("blocks")) if ("table".equals(block.path("kind").asText())) {
                int columns = block.path("widths").size();assertTrue(columns >= 1);
                int[] occupied = new int[columns];
                for (var row : block.path("rows")) {
                    int col = 0;
                    for (var cell : row) {
                        while(col < columns && occupied[col] > 0) col++;
                        int cs = cell.path("colSpan").asInt(), rs = cell.path("rowSpan").asInt();
                        assertTrue(cs > 0 && rs > 0 && col + cs <= columns);
                        for(int c = col; c < col + cs; c++){assertEquals(0, occupied[c]);occupied[c] = rs;}
                        col += cs;
                    }
                    assertTrue(java.util.Arrays.stream(occupied).allMatch(value -> value > 0), "Missing source grid cells");
                    for(int c = 0; c < columns; c++) occupied[c]--;
                }
                assertTrue(java.util.Arrays.stream(occupied).allMatch(value -> value == 0));
            }
        }
    }
    @Test void wrongSourceCourseRangeProfileOrEditedSectionIsNeverReplaced() throws Exception {
        JsonNode resource = resource(), course = resource.path("courses").get(0);
        var notes = oldNotes(resource, course);String code = course.path("sourceCode").asText();
        assertEquals(notes.toString(), Ds2027RubricNotes.forDisplay(notes.toString(), code, "LEGACY_2026"));
        assertEquals(notes.toString(), Ds2027RubricNotes.forDisplay(notes.toString(), "UNRELATED", "NEW_2027"));
        for (String field : new String[]{"sourceSha256", "startPage", "courseCode"}) {
            var changed = notes.deepCopy();((ObjectNode) changed.path("sourceImportEvidence")).put(field, "wrong");
            assertEquals(changed.toString(), Ds2027RubricNotes.forDisplay(changed.toString(), code, "NEW_2027"));
        }
        ((ObjectNode) notes.path("sourceSupplementalSections")).put("11", "Instructor edited rubric");
        assertEquals(notes.toString(), Ds2027RubricNotes.forDisplay(notes.toString(), code, "NEW_2027"));
        assertEquals("invalid json", Ds2027RubricNotes.forDisplay("invalid json", code, "NEW_2027"));
    }
    @Test void picturedIt094KeepsScoresAndDescriptionsInSeparateCellsAndExcludesSignature() throws Exception {
        var tables = Ds2027RubricNotes.section(resource().path("sourceSha256").asText(), "IT094", 448, 456);
        String text = json.writeValueAsString(tables);
        assertTrue(text.contains("Max."));assertTrue(text.contains("Score"));assertTrue(text.contains("Comments"));
        assertTrue(text.contains("Capstone"));assertTrue(text.contains("Milestone"));assertTrue(text.contains("Benchmark"));
        assertTrue(text.contains("Issue/ problem to be considered critically is stated without clarification or description."));
        assertFalse(text.contains("Date revised"));assertFalse(text.contains("Nguyen Van Sinh"));
        assertFalse(text.contains("402 / 513"));
        assertNull(Ds2027RubricNotes.section("CS hash", "IT094", 448, 456));
        assertNull(Ds2027RubricNotes.section(resource().path("sourceSha256").asText(), "IT094", 447, 456));
    }
    @Test void peMergedCloCellsKeepTheirSourceSpansInsteadOfLosingReferences() throws Exception {
        var section = json.valueToTree(Ds2027RubricNotes.section(resource().path("sourceSha256").asText(), "PE021IU", 116, 126));
        boolean found = false;
        for (var block : section.get(0).path("blocks")) for (var row : block.path("rows")) for (var cell : row)
            if (cell.path("text").asText().equals("CLO 1, 2")) {
                assertEquals(3, cell.path("rowSpan").asInt());found = true;
            }
        assertTrue(found);
    }
}
