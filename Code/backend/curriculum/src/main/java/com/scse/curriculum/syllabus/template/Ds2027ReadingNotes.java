package com.scse.curriculum.syllabus.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;

/** Section 12 source rows, bound to the original source and unchanged imported reading text. */
public final class Ds2027ReadingNotes {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String HASH = "fd279731316e301fce93cd25c9231959b1f73631f62c7d38e883d87bfaba1f32";
    private static final JsonNode RESOURCE = load();
    private Ds2027ReadingNotes() {}

    private static JsonNode load() {
        try (var input = Ds2027ReadingNotes.class.getResourceAsStream("/syllabus-import/verified-ds2026-reading-rows.json")) {
            if (input == null) throw new IllegalStateException("Missing DS reading-list evidence");
            JsonNode resource = JSON.readTree(input);
            if (!HASH.equals(resource.path("sourceSha256").asText())) throw new IllegalStateException("Invalid DS reading-list source");
            return resource;
        } catch (IOException error) { throw new IllegalStateException("Cannot load DS reading-list evidence", error); }
    }

    public static String forDisplay(String notes, String courseCode, String profile) {
        if (notes == null || !"NEW_2027".equals(profile)) return notes;
        try {
            JsonNode root = JSON.readTree(notes), evidence = root.path("sourceImportEvidence");
            if (!(root instanceof ObjectNode) || !HASH.equals(evidence.path("sourceSha256").asText())) return notes;
            String sourceCode = evidence.path("courseCode").asText();
            if (!normalize(sourceCode).equals(normalize(courseCode))) return notes;
            JsonNode sections = root.path("sourceSupplementalSections");
            if (!sections.isMissingNode() && !(sections instanceof ObjectNode)) return notes;
            if (sections.has("12")) return notes; // Preserve saved rows and any subsequent instructor edits.
            for (JsonNode course : RESOURCE.path("courses")) {
                if (!sourceCode.equals(course.path("sourceCode").asText())
                        || evidence.path("startPage").asInt() != course.path("startPage").asInt()
                        || evidence.path("endPage").asInt() != course.path("endPage").asInt()
                        || !root.path("sourceReadingList").equals(course.path("legacyReadingList"))) continue;
                if (sections.isMissingNode()) sections = ((ObjectNode) root).putObject("sourceSupplementalSections");
                ((ObjectNode) sections).set("12", course.path("section12").deepCopy());
                return JSON.writeValueAsString(root);
            }
        } catch (IOException | IllegalArgumentException error) { return notes; }
        return notes;
    }
    private static String normalize(String code) {
        String value = code == null ? "" : code.toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return value.endsWith("IU") ? value.substring(0, value.length() - 2) : value;
    }
}
