package com.scse.curriculum.syllabus.importer.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;

final class SyllabusSourceProvenanceResolver {

    private SyllabusSourceProvenanceResolver() {
    }

    static Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
            resolve(
                    SyllabusImportData data) {

        Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
                result =
                        new LinkedHashMap<>();

        Map<String, SyllabusImportData.SourceFieldProvenance>
                general =
                        new LinkedHashMap<>();

        Map<String, String> sourceLabels =
                sourceFieldLabels(
                        data,
                        "general");

        general.put(
        "courseCode",
        resolveField(
                sourceLabels,
                "courseCode",
                data == null
                        ? null
                        : data.getSourceCourseCode()));

general.put(
        "courseName",
        resolveField(
                sourceLabels,
                "courseName",
                data == null
                        ? null
                        : data.getSourceCourseName()));

general.put(
        "courseDesignation",
        resolveField(
                sourceLabels,
                "courseDesignation",
                data == null
                        ? null
                        : data.getCourseDesignation()));

general.put(
        "courseTypes",
        resolveField(
                sourceLabels,
                "courseTypes",
                data == null
                        ? null
                        : data.getCourseTypes()));

general.put(
        "semester",
        resolveField(
                sourceLabels,
                "semester",
                data == null
                        ? null
                        : data.getSemester()));

general.put(
        "personResponsible",
        resolveField(
                sourceLabels,
                "personResponsible",
                data == null
                        ? null
                        : data.getPersonResponsible()));

general.put(
        "language",
        resolveField(
                sourceLabels,
                "language",
                data == null
                        ? null
                        : data.getLanguage()));

general.put(
        "relation",
        resolveField(
                sourceLabels,
                "relation",
                data == null
                        ? null
                        : data.getRelation()));

general.put(
        "teachingMethods",
        resolveField(
                sourceLabels,
                "teachingMethods",
                data == null
                        ? null
                        : data.getTeachingMethods()));

general.put(
        "major",
        resolveField(
                sourceLabels,
                "major",
                data == null
                        ? null
                        : data.getMajor()));

        result.put(
                "general",
                general);
Map<String, SyllabusImportData.SourceFieldProvenance>
        workloadCredit =
                new LinkedHashMap<>();

Map<String, String> workloadCreditSourceLabels =
        sourceFieldLabels(
                data,
                "workloadCredit");

workloadCredit.put(
        "workloadTotal",
        resolveField(
                workloadCreditSourceLabels,
                "workloadTotal",
                data == null
                        ? null
                        : data.getWorkloadTotal()));

workloadCredit.put(
        "workloadContact",
        resolveField(
                workloadCreditSourceLabels,
                "workloadContact",
                data == null
                        ? null
                        : data.getWorkloadContact()));

workloadCredit.put(
        "workloadPrivate",
        resolveField(
                workloadCreditSourceLabels,
                "workloadPrivate",
                data == null
                        ? null
                        : data.getWorkloadPrivate()));

workloadCredit.put(
        "workloadStudentResponsibility",
        resolveField(
                workloadCreditSourceLabels,
                "workloadStudentResponsibility",
                data == null
                        ? null
                        : data.getWorkloadStudentResponsibility()));

workloadCredit.put(
        "creditPoints",
        resolveField(
                workloadCreditSourceLabels,
                "creditPoints",
                data == null
                        ? null
                        : data.getCreditPoints()));

workloadCredit.put(
        "lectureCredits",
        resolveField(
                workloadCreditSourceLabels,
                "lectureCredits",
                data == null
                        ? null
                        : data.getLectureCredits()));

workloadCredit.put(
        "laboratoryCredits",
        resolveField(
                workloadCreditSourceLabels,
                "laboratoryCredits",
                data == null
                        ? null
                        : data.getLaboratoryCredits()));

result.put(
        "workloadCredit",
        workloadCredit);
        Map<String, SyllabusImportData.SourceFieldProvenance>
        requirements =
                new LinkedHashMap<>();

Map<String, String> requirementsSourceLabels =
        sourceFieldLabels(
                data,
                "requirements");

requirements.put(
        "prerequisites",
        resolveField(
                requirementsSourceLabels,
                "prerequisites",
                data == null
                        ? null
                        : data.getPrerequisites()));

requirements.put(
        "objectives",
        resolveField(
                requirementsSourceLabels,
                "objectives",
                data == null
                        ? null
                        : data.getObjectives()));

result.put(
        "requirements",
        requirements);
        Map<String, SyllabusImportData.SourceFieldProvenance>
        content =
                new LinkedHashMap<>();

Map<String, String> contentSourceLabels =
        sourceFieldLabels(
                data,
                "content");

content.put(
        "contentNote",
        resolveField(
                contentSourceLabels,
                "contentNote",
                data == null
                        ? null
                        : data.getContentNote()));

result.put(
        "content",
        content);
        Map<String, SyllabusImportData.SourceFieldProvenance>
        assessment =
                new LinkedHashMap<>();

Map<String, String> assessmentSourceLabels =
        sourceFieldLabels(
                data,
                "assessment");

assessment.put(
        "assessmentPassNote",
        resolveField(
                assessmentSourceLabels,
                "assessmentPassNote",
                data == null
                        ? null
                        : data.getAssessmentPassNote()));

result.put(
        "assessment",
        assessment);
        return result;
    }


    private static SyllabusImportData.SourceFieldProvenance resolveField(
        Map<String, String> sourceLabels,
        String fieldKey,
        Object parsedValue) {

    boolean parsed =
            hasParsedValue(
                    parsedValue);

    boolean evidencedInSource =
            sourceLabels.containsKey(
                    fieldKey);

    /*
     * A successfully parsed value is authoritative evidence that
     * the source contained this field, even when template detection
     * failed to preserve the corresponding source label.
     */
    if (parsed) {
        return SyllabusImportData.SourceFieldProvenance.builder()
                .state(
                        SyllabusImportData.SourceFieldState.PARSED)
                .sourceLabel(
                        evidencedInSource
                                ? sourceLabels.get(fieldKey)
                                : null)
                .build();
    }

    if (!evidencedInSource) {
        return SyllabusImportData.SourceFieldProvenance.builder()
                .state(
                        SyllabusImportData.SourceFieldState
                                .ABSENT_IN_SOURCE)
                .sourceLabel(null)
                .build();
    }

    return SyllabusImportData.SourceFieldProvenance.builder()
            .state(
                    SyllabusImportData.SourceFieldState.UNRESOLVED)
            .sourceLabel(
                    sourceLabels.get(
                            fieldKey))
            .build();
}
    private static Map<String, String> sourceFieldLabels(
            SyllabusImportData data,
            String sectionKey) {

        Map<String, String> labels =
                new LinkedHashMap<>();

        if (data == null
                || data.getTemplateSections() == null) {
            return labels;
        }

        for (SyllabusImportData.TemplateSection section :
                data.getTemplateSections()) {

            if (section == null
                    || !sectionKey.equals(
                            section.getKey())) {
                continue;
            }

            List<SyllabusImportData.TemplateField> fields =
                    section.getFields();

            if (fields == null) {
                continue;
            }

            for (SyllabusImportData.TemplateField field :
                    fields) {

                if (field == null
                        || field.getKey() == null
                        || field.getKey().isBlank()) {
                    continue;
                }

                labels.putIfAbsent(
                        field.getKey(),
                        field.getLabel());
            }
        }

        return labels;
    }


    private static boolean hasParsedValue(
            Object value) {

        if (value == null) {
            return false;
        }

        if (value instanceof String text) {
            return !text.isBlank();
        }

        if (value instanceof List<?> list) {
            return !list.isEmpty();
        }

        if (value instanceof Map<?, ?> map) {
            return !map.isEmpty();
        }

        return true;
    }
}