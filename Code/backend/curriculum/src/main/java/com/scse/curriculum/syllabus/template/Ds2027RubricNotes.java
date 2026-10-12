package com.scse.curriculum.syllabus.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;

/** Source-bound section 11 presentation; never changes entities or edited rubric values. */
public final class Ds2027RubricNotes {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String HASH = "fd279731316e301fce93cd25c9231959b1f73631f62c7d38e883d87bfaba1f32";
    private static final JsonNode RESOURCE = load();

    private Ds2027RubricNotes() {}

    private static JsonNode load() {
        try (var input = Ds2027RubricNotes.class.getResourceAsStream("/syllabus-import/verified-ds2026-rubric-grids.json")) {
            if (input == null) throw new IllegalStateException("Missing DS source rubric evidence");
            JsonNode resource = JSON.readTree(input);
            if (!HASH.equals(resource.path("sourceSha256").asText())) throw new IllegalStateException("Invalid DS source rubric evidence");
            return resource;
        } catch (IOException error) { throw new IllegalStateException("Cannot read DS source rubric evidence", error); }
    }

    private static JsonNode proof(String hash, String sourceCode, int start, int end) {
        if (!HASH.equals(hash)) return null;
        for (JsonNode course : RESOURCE.path("courses"))
            if (course.path("sourceCode").asText().equals(sourceCode)
                    && course.path("startPage").asInt() == start && course.path("endPage").asInt() == end) return course;
        return null;
    }

    public static Object section(String hash, String sourceCode, int start, int end) {
        JsonNode course = proof(hash, sourceCode, start, end);
        return course == null ? null : JSON.convertValue(course.path("section11"), Object.class);
    }

    /** Replace only the exact old importer output, after existing access checks have succeeded. */
    public static String forDisplay(String notes, String courseCode, String targetTemplateProfile) {
        if (notes == null || !"NEW_2027".equals(targetTemplateProfile)) return notes;
        try {
            JsonNode root = JSON.readTree(notes), evidence = root.path("sourceImportEvidence");
            String sourceCode = evidence.path("courseCode").asText();
            // Same IU suffix normalization already used by import identity checks; no course equivalence inference.
            if (!normalize(sourceCode).equals(normalize(courseCode))) return notes;
            JsonNode course = proof(evidence.path("sourceSha256").asText(), sourceCode,
                    evidence.path("startPage").asInt(), evidence.path("endPage").asInt());
            JsonNode sections = root.path("sourceSupplementalSections");
            if (course == null || !(sections instanceof ObjectNode)
                    || !sections.path("11").equals(course.path("legacySection11"))) return notes;
            ((ObjectNode) sections).set("11", course.path("section11").deepCopy());
            return JSON.writeValueAsString(root);
        } catch (IOException | IllegalArgumentException error) { return notes; }
    }

    private static String normalize(String code) {
        String value = code == null ? "" : code.toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return value.endsWith("IU") ? value.substring(0, value.length() - 2) : value;
    }
}
