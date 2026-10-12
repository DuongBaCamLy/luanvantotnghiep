package com.scse.curriculum.syllabus.template;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Ds2027ReadingNotesTest {
    private final ObjectMapper json = new ObjectMapper();
    private JsonNode resource() throws Exception {
        try (var input = getClass().getResourceAsStream("/syllabus-import/verified-ds2026-reading-rows.json")) { return json.readTree(input); }
    }
    private ObjectNode notes(JsonNode source, JsonNode course) {
        var notes = json.createObjectNode();
        notes.putObject("sourceImportEvidence").put("sourceSha256", source.path("sourceSha256").asText())
                .put("courseCode", course.path("sourceCode").asText()).put("startPage", course.path("startPage").asInt())
                .put("endPage", course.path("endPage").asInt());
        notes.set("sourceReadingList", course.path("legacyReadingList"));
        notes.putObject("sourceSupplementalSections").put("11", "Rubric content must remain unchanged");
        notes.putArray("readings").addObject().put("title", "Existing structured book");
        return notes;
    }
    @Test void verifiedReadingRowsOnlyAddSection12AndPreserveRawTextOtherSectionsAndIdempotency() throws Exception {
        var source = resource();int count = 0;
        assertEquals(37, source.path("courses").size());
        for (var course : source.path("courses")) {
            var saved = notes(source, course);var expected = saved.deepCopy();
            ((ObjectNode) expected.path("sourceSupplementalSections")).set("12", course.path("section12"));
            String result = Ds2027ReadingNotes.forDisplay(saved.toString(), course.path("sourceCode").asText(), "NEW_2027");
            assertEquals(expected, json.readTree(result));
            assertEquals(result, Ds2027ReadingNotes.forDisplay(result, course.path("sourceCode").asText(), "NEW_2027"));
            for (var table : course.path("section12")) for (var row : table.path("rows")) {
                assertEquals(2, row.size());assertFalse(row.get(1).asText().isBlank());count++;
            }
        }
        assertEquals(108, count);
    }
    @Test void shownIt056KeepsThreeCompleteReferencesWithNoInferredMetadata() throws Exception {
        var source = resource();var course = java.util.stream.StreamSupport.stream(source.path("courses").spliterator(), false)
                .filter(c -> c.path("sourceCode").asText().equals("IT056")).findFirst().orElseThrow();
        var result = json.readTree(Ds2027ReadingNotes.forDisplay(notes(source, course).toString(), "IT056IU", "NEW_2027"));
        var rows = result.path("sourceSupplementalSections").path("12").get(0).path("rows");
        assertEquals(3, rows.size());assertEquals("1.", rows.get(0).get(0).asText());
        assertEquals("Kathy Schwalbe, IT Project Management - 9th Edition, 2019", rows.get(0).get(1).asText());
        assertEquals("Stellman and Greene, Applied Software Project Management, O’Reilly Media, 2006.", rows.get(1).get(1).asText());
        assertTrue(rows.get(2).get(1).asText().endsWith("Providing Measureable Organizational Value 5th, 2016"));
        assertFalse(result.path("sourceSupplementalSections").path("12").toString().contains("REQUIRED"));
    }
    @Test void sourceRangeCourseProfileRawTextAndExistingRowsGuardsProtectEditsAndCs() throws Exception {
        var source = resource();var course = source.path("courses").get(0);var saved = notes(source, course);
        String code = course.path("sourceCode").asText();
        assertEquals(saved.toString(), Ds2027ReadingNotes.forDisplay(saved.toString(), code, "LEGACY_2026"));
        assertEquals(saved.toString(), Ds2027ReadingNotes.forDisplay(saved.toString(), "OTHER", "NEW_2027"));
        for (String field : new String[]{"sourceSha256", "startPage", "endPage", "courseCode"}) {
            var edited = saved.deepCopy();((ObjectNode) edited.path("sourceImportEvidence")).put(field, "different");
            assertEquals(edited.toString(), Ds2027ReadingNotes.forDisplay(edited.toString(), code, "NEW_2027"));
        }
        var edited = saved.deepCopy();edited.put("sourceReadingList", "Instructor-edited references");
        assertEquals(edited.toString(), Ds2027ReadingNotes.forDisplay(edited.toString(), code, "NEW_2027"));
        ((ObjectNode) saved.path("sourceSupplementalSections")).put("12", "Instructor-edited rows");
        assertEquals(saved.toString(), Ds2027ReadingNotes.forDisplay(saved.toString(), code, "NEW_2027"));
        assertEquals("bad json", Ds2027ReadingNotes.forDisplay("bad json", code, "NEW_2027"));
    }
}
