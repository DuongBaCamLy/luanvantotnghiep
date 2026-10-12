package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;

/** Preserve Content independently; only complete source rows can become canonical topics. */
public final class Cohort2027ImportValidation {
    private Cohort2027ImportValidation() {}

    public static void validate(SyllabusImportData data) {
        var safety = data.getSourceProvenance() == null ? null
                : data.getSourceProvenance().getOrDefault("importSafety", java.util.Map.of()).get("sourceValidation");
        if (safety != null && safety.getState() == SyllabusImportData.SourceFieldState.UNRESOLVED)
            throw new IllegalArgumentException("NEW_2027 source validation is unresolved: " + safety.getSourceLabel());
        java.util.Set<String> cloCodes = new java.util.HashSet<>();
        for (var clo : data.getClos()) {
            String code = clo.getCode() == null ? "" : clo.getCode().replaceAll("\\s+", "").toUpperCase(java.util.Locale.ROOT);
            if (code.isBlank() || !cloCodes.add(code)) throw new IllegalArgumentException("NEW_2027 source CLO identity is missing or duplicated.");
        }
        for (var mapping : data.getCloPloMappings()) {
            String code = mapping.getCloCode() == null ? "" : mapping.getCloCode().replaceAll("\\s+", "").toUpperCase(java.util.Locale.ROOT);
            if (!cloCodes.contains(code)) throw new IllegalArgumentException("NEW_2027 CLO–PLO mapping refers to a CLO outside this syllabus.");
        }
        for (var mapping : data.getTopicCloMappings()) {
            String code = mapping.getCloCode() == null ? "" : mapping.getCloCode().replaceAll("\\s+", "").toUpperCase(java.util.Locale.ROOT);
            if (!cloCodes.contains(code) || mapping.getTopicIndex() == null || mapping.getTopicIndex() < 1
                    || mapping.getTopicIndex() > data.getTopics().size())
                throw new IllegalArgumentException("NEW_2027 Topic–CLO mapping refers to an unrelated CLO or content row.");
        }
        if (data.getTopics() == null) return;
        for (var topic : data.getTopics()) {
            String name = topic.getName();
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("NEW_2027 content row has no source name.");
            }
            if (topic.getWeekNumber() != null && topic.getWeekNumber() < 1) {
                throw new IllegalArgumentException("NEW_2027 topic has an invalid source week.");
            }
        }
    }

    public static boolean canPersistCanonicalTopic(com.scse.curriculum.syllabus.importer.dto.TopicImportData topic) {
        if (topic.getName() == null || topic.getName().length() > 255
                || topic.getWeekNumber() == null || topic.getWeekNumber() < 1
                || topic.getTeachingHours() == null || topic.getTeachingHours() < 0
                || topic.getLabHours() == null || topic.getLabHours() < 0
                || topic.getSelfStudyHours() == null || topic.getSelfStudyHours() < 0
                || topic.getTopicType() == null) return false;
        try {
            com.scse.curriculum.topic.entity.TopicType.valueOf(topic.getTopicType().trim().toUpperCase(java.util.Locale.ROOT));
            return true;
        } catch (IllegalArgumentException error) { return false; }
    }
}
