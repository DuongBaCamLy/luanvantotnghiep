package com.scse.curriculum.syllabus.importer.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import com.scse.curriculum.syllabus.importer.dto.TopicImportData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/** Reviewed image-table transcriptions apply only to the exact source PDF and section. */
public final class Cohort2027VerifiedSourceRecovery {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String REVIEW_RESOURCE = "/syllabus-import/verified-2026-reviewed-syllabi.json";

    static void applySourceTables(String hash,int start,int end,SyllabusImportData data)throws IOException {
        var proof=vectorCourse(hash,start,end,data.getSourceCourseCode());if(proof==null)return;
        // A source max-score/checklist column is not a NEW_2027 rubric achievement level.
        data.setRubricItems(new ArrayList<>());
        if(proof.has("readingListRaw")) {
            data.setReadings(new ArrayList<>());
            if(!proof.path("readingListRaw").asText().isBlank())data.getReadings().add(SyllabusImportData.ReadingItem.builder()
                    .title(proof.path("readingListRaw").asText()).type("RAW_SOURCE").build());
            data.getSourceProvenance().computeIfAbsent("readingList",k->new LinkedHashMap<>()).put("sourceCellRead",
                    SyllabusImportData.SourceFieldProvenance.builder().state(SyllabusImportData.SourceFieldState.PARSED)
                            .sourceLabel("Original source Reading list cell, including explicit empty cells; following section labels are excluded").build());
        }
        data.getSourceProvenance().computeIfAbsent("sourceVectorTables",k->new LinkedHashMap<>()).put("document",
                SyllabusImportData.SourceFieldProvenance.builder().state(SyllabusImportData.SourceFieldState.PARSED)
                        .sourceLabel(hash+":"+start+":"+end).build());
    }
    private static com.fasterxml.jackson.databind.JsonNode vectorCourse(String hash,int start,int end,String code)throws IOException {
        String resourceName=Cohort2027DsSourceRecovery.supports(hash)
                ? "/syllabus-import/verified-ds2026-source-tables.json" : "/syllabus-import/verified-2026-vector-tables.json";
        try(var in=Cohort2027VerifiedSourceRecovery.class.getResourceAsStream(resourceName)) {
            if(in==null)return null;var resource=MAPPER.readTree(in);
            if(!hash.equals(resource.path("sourceSha256").asText()))return null;
            for(var entry:resource.path("courses"))if(code.equals(entry.path("sourceCode").asText())
                    && start==entry.path("startPage").asInt() && end==entry.path("endPage").asInt())return entry;
            return null;
        }
    }
    public static java.util.Map<String,Object> vectorNotes(SyllabusImportData data) {
        var marker=data.getSourceProvenance().getOrDefault("sourceVectorTables",java.util.Map.of()).get("document");
        if(marker==null)return java.util.Map.of();
        try {
            var p=marker.getSourceLabel().split(":");if(p.length!=3)throw new IllegalArgumentException("Invalid source table evidence");
            var proof=vectorCourse(p[0],Integer.parseInt(p[1]),Integer.parseInt(p[2]),data.getSourceCourseCode());
            if(proof==null)throw new IllegalArgumentException("Source tables do not match this PDF/course/section");
            var sections = MAPPER.convertValue(proof.path("sections"),new com.fasterxml.jackson.core.type.TypeReference<LinkedHashMap<String,Object>>(){});
            Object rubric = com.scse.curriculum.syllabus.template.Ds2027RubricNotes.section(
                    p[0], data.getSourceCourseCode(), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
            if (rubric != null) sections.put("11", rubric);
            return sections;
        }catch(IOException|NumberFormatException error){throw new IllegalArgumentException("Cannot preserve original source tables",error);}
    }

    /** Full page-reviewed sources are independent of OCR and never overlay another course. */
    static boolean applyReviewed(String sha256, int startPage, int endPage, SyllabusImportData data) throws IOException {
        var proof = reviewedCourse(sha256, data.getSourceCourseCode(), startPage, endPage);
        if (proof == null) return false;
        var recovered = MAPPER.treeToValue(proof.path("data"), SyllabusImportData.class);
        try {
            for (var field : SyllabusImportData.class.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                field.set(data, field.get(recovered));
            }
        } catch (IllegalAccessException error) { throw new IOException("Cannot read reviewed source", error); }
        data.getSourceProvenance().computeIfAbsent("reviewedSource", ignored -> new LinkedHashMap<>())
                .put("document", SyllabusImportData.SourceFieldProvenance.builder()
                        .state(SyllabusImportData.SourceFieldState.PARSED)
                        .sourceLabel(sha256 + ":" + startPage + ":" + endPage).build());
        data.getSourceProvenance().computeIfAbsent("content", ignored -> new LinkedHashMap<>())
                .put("sourceRows", SyllabusImportData.SourceFieldProvenance.builder()
                        .state(SyllabusImportData.SourceFieldState.PARSED)
                        .sourceLabel("Direct visual source review, physical pages " + startPage + "–" + endPage).build());
        return true;
    }

    private static com.fasterxml.jackson.databind.JsonNode reviewedCourse(String hash, String code, int start, int end) throws IOException {
        String resourceName=Cohort2027DsSourceRecovery.supports(hash)
                ? Cohort2027DsSourceRecovery.REVIEW_RESOURCE : REVIEW_RESOURCE;
        try (var input = Cohort2027VerifiedSourceRecovery.class.getResourceAsStream(resourceName)) {
            if (input == null) return null;
            var resource = MAPPER.readTree(input);
            if (!hash.equals(resource.path("sourceSha256").asText())) return null;
            for (var course : resource.path("courses"))
                if (code != null && code.equals(course.path("courseCode").asText())
                        && start == course.path("startPage").asInt() && end == course.path("endPage").asInt()) return course;
            return null;
        }
    }

    /** A client-provided provenance marker cannot certify altered academic values. */
    public static java.util.Map<String, Object> reviewedNotes(SyllabusImportData data) {
        var marker = data.getSourceProvenance() == null ? null
                : data.getSourceProvenance().getOrDefault("reviewedSource", java.util.Map.of()).get("document");
        if (marker == null) return java.util.Map.of();
        try {
            var parts = marker.getSourceLabel().split(":");
            if (parts.length != 3) throw new IllegalArgumentException("Invalid reviewed source evidence");
            var proof = reviewedCourse(parts[0], data.getSourceCourseCode(), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            if (proof == null) throw new IllegalArgumentException("Reviewed source identity does not match this syllabus");
            var actual = MAPPER.valueToTree(data);
            var expected = MAPPER.valueToTree(MAPPER.treeToValue(proof.path("data"), SyllabusImportData.class));
            for (String key : java.util.List.of("sourceProvenance", "templateSections")) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) actual).remove(key);
                ((com.fasterxml.jackson.databind.node.ObjectNode) expected).remove(key);
            }
            if (!actual.equals(expected)) throw new IllegalArgumentException("Academic values differ from the reviewed source; re-verify the edited fields before confirmation");
            java.util.Map<String, Object> notes = MAPPER.convertValue(proof.path("notes"), new com.fasterxml.jackson.core.type.TypeReference<java.util.LinkedHashMap<String, Object>>() {});
            var evidence = new LinkedHashMap<String, Object>();
            evidence.put("sourceSha256", parts[0]);
            evidence.put("courseCode", data.getSourceCourseCode());
            evidence.put("startPage", Integer.parseInt(parts[1]));
            evidence.put("endPage", Integer.parseInt(parts[2]));
            evidence.put("fieldPages", MAPPER.convertValue(proof.path("fieldPages"), Object.class));
            evidence.put("reviewedPages", MAPPER.convertValue(proof.path("reviewedPages"), Object.class));
            evidence.put("method", proof.path("method").asText());
            notes.put("sourceImportEvidence", evidence);
            notes.put("sourceUnverifiedFields", MAPPER.convertValue(proof.path("unverifiedFields"), Object.class));
            notes.put("sourceMissingFields", MAPPER.convertValue(proof.path("missingFields"), Object.class));
            return notes;
        } catch (IOException | NumberFormatException error) {
            throw new IllegalArgumentException("Cannot validate reviewed academic source", error);
        }
    }

    static boolean apply(String sha256, int startPage, int endPage, SyllabusImportData data) throws IOException {
        try (var input = Cohort2027VerifiedSourceRecovery.class.getResourceAsStream(
                "/syllabus-import/verified-2026-image-content.json")) {
            if (input == null) return false;
            var evidence = new ObjectMapper().readTree(input);
            if (!sha256.equals(evidence.path("sourceSha256").asText())) return false;
            for (var course : evidence.path("courses")) {
                if (!data.getSourceCourseCode().equals(course.path("courseCode").asText())
                        || startPage != course.path("startPage").asInt()
                        || endPage != course.path("endPage").asInt()) continue;
                var rows = new ArrayList<TopicImportData>();
                for (var row : course.path("rows")) rows.add(TopicImportData.builder()
                        .name(row.path("name").asText()).contentWeight(row.path("weight").asText())
                        .contentLevel(row.path("level").asText()).teachingLevel(row.path("level").asText())
                        .orderInWeek(rows.size()+1).build());
                data.setTopics(rows);
                // These image rows do not identify any Content-to-week/CLO correspondence.
                data.setTopicCloMappings(new ArrayList<>());
                if (data.getSourceProvenance()==null) data.setSourceProvenance(new LinkedHashMap<>());
                data.getSourceProvenance().computeIfAbsent("content", ignored -> new LinkedHashMap<>())
                        .put("sourceRows", SyllabusImportData.SourceFieldProvenance.builder()
                                .state(SyllabusImportData.SourceFieldState.PARSED)
                                .sourceLabel("Visually verified source image table, physical page "
                                        +course.path("contentPage").asInt()+", exact PDF SHA-256 "+sha256).build());
                return true;
            }
            return false;
        }
    }
}
