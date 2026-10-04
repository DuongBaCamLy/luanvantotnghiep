package com.scse.curriculum.syllabus.importer.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.scse.curriculum.syllabus.importer.dto.AssessmentImportData;
import com.scse.curriculum.syllabus.importer.dto.CloImportData;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import com.scse.curriculum.syllabus.importer.dto.TopicImportData;

/**
 * Resolves review-only provenance for fields extracted from the uploaded
 * SOURCE syllabus.
 *
 * This resolver never reads targetTemplateSections and never creates target
 * values. It only reports whether canonical source data was parsed, absent,
 * or structurally present but unresolved.
 */
final class SyllabusSourceProvenanceResolver {

    private SyllabusSourceProvenanceResolver() {
    }

    static Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
            resolve(
                    SyllabusImportData data) {

        Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
                result =
                        new LinkedHashMap<>();

        resolveGeneral(data, result);
        resolveWorkloadCredit(data, result);
        resolveRequirements(data, result);
        resolveClos(data, result);
        resolveContent(data, result);
        resolveTopicClo(data, result);
        resolveCloPlo(data, result);
        resolvePlannedActivities(data, result);
        resolveAssessment(data, result);
        resolveAssessmentClo(data, result);
        resolveExamination(data, result);
        resolveRubrics(data, result);
        resolveReadings(data, result);

        return result;
    }

    private static void resolveGeneral(
            SyllabusImportData data,
            Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
                    result) {

        Map<String, String> labels =
                sourceFieldLabels(data, "general");

        Map<String, SyllabusImportData.SourceFieldProvenance> section =
                new LinkedHashMap<>();

        put(section, labels, "courseCode",
                data == null ? null : data.getSourceCourseCode());

        put(section, labels, "courseName",
                data == null ? null : data.getSourceCourseName());

        put(section, labels, "courseDesignation",
                data == null ? null : data.getCourseDesignation());

        put(section, labels, "courseTypes",
                data == null ? null : data.getCourseTypes());

        put(section, labels, "semester",
                data == null ? null : data.getSemester());

        put(section, labels, "personResponsible",
                data == null ? null : data.getPersonResponsible());

        put(section, labels, "language",
                data == null ? null : data.getLanguage());

        put(section, labels, "relation",
                data == null ? null : data.getRelation());

        put(section, labels, "teachingMethods",
                data == null ? null : data.getTeachingMethods());

        put(section, labels, "major",
                data == null ? null : data.getMajor());

        result.put("general", section);
    }

    private static void resolveWorkloadCredit(
            SyllabusImportData data,
            Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
                    result) {

        Map<String, String> labels =
                sourceFieldLabels(data, "workloadCredit");

        Map<String, SyllabusImportData.SourceFieldProvenance> section =
                new LinkedHashMap<>();

        put(section, labels, "workloadTotal",
                data == null ? null : data.getWorkloadTotal());

        put(section, labels, "workloadContact",
                data == null ? null : data.getWorkloadContact());

        put(section, labels, "workloadPrivate",
                data == null ? null : data.getWorkloadPrivate());

        put(section, labels, "workloadStudentResponsibility",
                data == null ? null : data.getWorkloadStudentResponsibility());

        put(section, labels, "creditPoints",
                data == null ? null : data.getCreditPoints());

        put(section, labels, "lectureCredits",
                data == null ? null : data.getLectureCredits());

        put(section, labels, "laboratoryCredits",
                data == null ? null : data.getLaboratoryCredits());

        result.put("workloadCredit", section);
    }

    private static void resolveRequirements(
            SyllabusImportData data,
            Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
                    result) {

        Map<String, String> labels =
                sourceFieldLabels(data, "requirements");

        Map<String, SyllabusImportData.SourceFieldProvenance> section =
                new LinkedHashMap<>();

        put(section, labels, "prerequisites",
                data == null ? null : data.getPrerequisites());

        put(section, labels, "objectives",
                data == null ? null : data.getObjectives());

        result.put("requirements", section);
    }

    private static void resolveClos(
            SyllabusImportData data,
            Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
                    result) {

        Map<String, String> labels =
                sourceFieldLabels(data, "clo");

        List<CloImportData> items =
                data == null ? null : data.getClos();

        Map<String, SyllabusImportData.SourceFieldProvenance> section =
                new LinkedHashMap<>();

        put(section, labels, "code",
                anyParsed(items, CloImportData::getCode));

        put(section, labels, "description",
                anyParsed(items, CloImportData::getDescription));

        put(section, labels, "descriptionVn",
                anyParsed(items, CloImportData::getDescriptionVn));

        put(section, labels, "competencyLevel",
                anyParsed(items, CloImportData::getCompetencyLevel));

        put(section, labels, "bloomLevel",
                anyParsed(items, CloImportData::getBloomLevel));

        put(section, labels, "orderIndex",
                anyParsed(items, CloImportData::getOrderIndex));

        result.put("clo", section);
    }

    private static void resolveContent(
            SyllabusImportData data,
            Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
                    result) {

        Map<String, String> labels =
                sourceFieldLabels(data, "content");

        List<TopicImportData> items =
                data == null ? null : data.getTopics();

        Map<String, SyllabusImportData.SourceFieldProvenance> section =
                new LinkedHashMap<>();

        put(section, labels, "contentNote",
                data == null ? null : data.getContentNote());

        put(section, labels, "name",
                anyParsed(items, TopicImportData::getName));

        put(section, labels, "nameVn",
                anyParsed(items, TopicImportData::getNameVn));

        put(section, labels, "weekNumber",
                anyParsed(items, TopicImportData::getWeekNumber));

        put(section, labels, "orderInWeek",
                anyParsed(items, TopicImportData::getOrderInWeek));

        put(section, labels, "teachingHours",
                anyParsed(items, TopicImportData::getTeachingHours));

        put(section, labels, "labHours",
                anyParsed(items, TopicImportData::getLabHours));

        put(section, labels, "selfStudyHours",
                anyParsed(items, TopicImportData::getSelfStudyHours));

        put(section, labels, "topicType",
                anyParsed(items, TopicImportData::getTopicType));

        put(section, labels, "teachingMethod",
                anyParsed(items, TopicImportData::getTeachingMethod));

        put(section, labels, "learningActivity",
                anyParsed(items, TopicImportData::getLearningActivity));

        put(section, labels, "resources",
                anyParsed(items, TopicImportData::getResources));

        put(section, labels, "contentWeight",
                anyParsed(items, TopicImportData::getContentWeight));

        put(
                section,
                labels,
                "contentLevel",
                anyParsed(
                        items,
                        item ->
                                hasParsedValue(item.getContentLevel())
                                        ? item.getContentLevel()
                                        : item.getTeachingLevel()));

        result.put("content", section);
    }

    private static void resolveTopicClo(
            SyllabusImportData data,
            Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
                    result) {

        Map<String, String> labels =
                sourceFieldLabels(data, "topicClo");

        List<SyllabusImportData.TopicCloMappingItem> items =
                data == null ? null : data.getTopicCloMappings();

        Map<String, SyllabusImportData.SourceFieldProvenance> section =
                new LinkedHashMap<>();

        put(
                section,
                labels,
                "cloCode",
                anyParsed(
                        items,
                        SyllabusImportData.TopicCloMappingItem::getCloCode));

        result.put("topicClo", section);
    }

    private static void resolveCloPlo(
            SyllabusImportData data,
            Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
                    result) {

        Map<String, String> labels =
                sourceFieldLabels(data, "cloPlo");

        List<SyllabusImportData.CloPloMappingItem> items =
                data == null ? null : data.getCloPloMappings();

        Map<String, SyllabusImportData.SourceFieldProvenance> section =
                new LinkedHashMap<>();

        put(
                section,
                labels,
                "cloCode",
                anyParsed(
                        items,
                        SyllabusImportData.CloPloMappingItem::getCloCode));

        put(
                section,
                labels,
                "ploCode",
                anyParsed(
                        items,
                        SyllabusImportData.CloPloMappingItem::getPloCode));

        /*
         * Source detector calls the matrix contribution cell "level".
         * The canonical import DTO preserves that original value as "value".
         */
        put(
                section,
                labels,
                "level",
                anyParsed(
                        items,
                        SyllabusImportData.CloPloMappingItem::getValue));

        put(
                section,
                labels,
                "contributionWeight",
                anyParsed(
                        items,
                        SyllabusImportData.CloPloMappingItem::getContributionWeight));

        result.put("cloPlo", section);
    }

    private static void resolvePlannedActivities(
            SyllabusImportData data,
            Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
                    result) {

        Map<String, String> labels =
                sourceFieldLabels(data, "plannedActivities");

        List<SyllabusImportData.WeeklyActivityItem> items =
                data == null ? null : data.getWeeklyActivities();

        Map<String, SyllabusImportData.SourceFieldProvenance> section =
                new LinkedHashMap<>();

        put(section, labels, "week",
                anyParsed(items, SyllabusImportData.WeeklyActivityItem::getWeek));

        put(section, labels, "topic",
                anyParsed(items, SyllabusImportData.WeeklyActivityItem::getTopic));

        put(section, labels, "clo",
                anyParsed(items, SyllabusImportData.WeeklyActivityItem::getClo));

        put(section, labels, "assessments",
                anyParsed(items, SyllabusImportData.WeeklyActivityItem::getAssessments));

        put(section, labels, "learningActivities",
                anyParsed(items, SyllabusImportData.WeeklyActivityItem::getLearningActivities));

        put(section, labels, "resources",
                anyParsed(items, SyllabusImportData.WeeklyActivityItem::getResources));

        result.put("plannedActivities", section);
    }

    private static void resolveAssessment(
            SyllabusImportData data,
            Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
                    result) {

        Map<String, String> labels =
                sourceFieldLabels(data, "assessment");

        List<AssessmentImportData> items =
                data == null ? null : data.getAssessments();

        Map<String, SyllabusImportData.SourceFieldProvenance> section =
                new LinkedHashMap<>();

        put(section, labels, "assessmentPassNote",
                data == null ? null : data.getAssessmentPassNote());

        put(section, labels, "name",
                anyParsed(items, AssessmentImportData::getName));

        put(section, labels, "nameVn",
                anyParsed(items, AssessmentImportData::getNameVn));

        put(section, labels, "assessmentType",
                anyParsed(items, AssessmentImportData::getAssessmentType));

        put(section, labels, "weightPercent",
                anyParsed(items, AssessmentImportData::getWeightPercent));

        put(section, labels, "minScore",
                anyParsed(items, AssessmentImportData::getMinScore));

        put(section, labels, "maxScore",
                anyParsed(items, AssessmentImportData::getMaxScore));

        put(section, labels, "orderIndex",
                anyParsed(items, AssessmentImportData::getOrderIndex));

        result.put("assessment", section);
    }

    private static void resolveAssessmentClo(
            SyllabusImportData data,
            Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
                    result) {

        Map<String, String> labels =
                sourceFieldLabels(data, "assessmentClo");

        List<SyllabusImportData.AssessmentCloMappingItem> items =
                data == null ? null : data.getAssessmentCloMappings();

        Map<String, SyllabusImportData.SourceFieldProvenance> section =
                new LinkedHashMap<>();

        put(
                section,
                labels,
                "cloCode",
                anyParsed(
                        items,
                        SyllabusImportData.AssessmentCloMappingItem::getCloCode));

        put(
                section,
                labels,
                "contributionPercent",
                anyParsed(
                        items,
                        SyllabusImportData.AssessmentCloMappingItem::getPercentage));

        result.put("assessmentClo", section);
    }

    private static void resolveExamination(
            SyllabusImportData data,
            Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
                    result) {

        Map<String, String> labels =
                sourceFieldLabels(data, "examination");

        Map<String, SyllabusImportData.SourceFieldProvenance> section =
                new LinkedHashMap<>();

        put(section, labels, "examForms",
                data == null ? null : data.getExamForms());

        put(section, labels, "examRequirements",
                data == null ? null : data.getExamRequirements());

        result.put("examination", section);
    }

    private static void resolveRubrics(
            SyllabusImportData data,
            Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
                    result) {

        Map<String, String> labels =
                sourceFieldLabels(data, "rubrics");

        List<SyllabusImportData.RubricItem> rubrics =
                data == null ? null : data.getRubricItems();

        List<SyllabusImportData.RubricCriteriaItem> criteria =
                rubricCriteria(rubrics);

        Map<String, SyllabusImportData.SourceFieldProvenance> section =
                new LinkedHashMap<>();

        put(section, labels, "type",
                anyParsed(rubrics, SyllabusImportData.RubricItem::getType));

        put(section, labels, "title",
                anyParsed(rubrics, SyllabusImportData.RubricItem::getTitle));

        put(section, labels, "criterion",
                anyParsed(criteria, SyllabusImportData.RubricCriteriaItem::getCriterion));

        put(section, labels, "level1",
                anyParsed(criteria, SyllabusImportData.RubricCriteriaItem::getLevel1));

        put(section, labels, "level2",
                anyParsed(criteria, SyllabusImportData.RubricCriteriaItem::getLevel2));

        put(section, labels, "level3",
                anyParsed(criteria, SyllabusImportData.RubricCriteriaItem::getLevel3));

        put(section, labels, "level4",
                anyParsed(criteria, SyllabusImportData.RubricCriteriaItem::getLevel4));

        result.put("rubrics", section);
    }

    private static void resolveReadings(
            SyllabusImportData data,
            Map<String, Map<String, SyllabusImportData.SourceFieldProvenance>>
                    result) {

        Map<String, String> labels =
                sourceFieldLabels(data, "readings");

        List<SyllabusImportData.ReadingItem> items =
                data == null ? null : data.getReadings();

        Map<String, SyllabusImportData.SourceFieldProvenance> section =
                new LinkedHashMap<>();

        put(section, labels, "title",
                anyParsed(items, SyllabusImportData.ReadingItem::getTitle));

        put(section, labels, "author",
                anyParsed(items, SyllabusImportData.ReadingItem::getAuthor));

        put(section, labels, "publisher",
                anyParsed(items, SyllabusImportData.ReadingItem::getPublisher));

        put(section, labels, "year",
                anyParsed(items, SyllabusImportData.ReadingItem::getYear));

        /*
         * SOURCE structural key = usageType.
         * Canonical target/import DTO property = type.
         */
        putAlias(
                section,
                "type",
                labels,
                "usageType",
                anyParsed(
                        items,
                        SyllabusImportData.ReadingItem::getType));

        result.put("readings", section);
    }

    private static void put(
            Map<String, SyllabusImportData.SourceFieldProvenance> section,
            Map<String, String> sourceLabels,
            String fieldKey,
            Object parsedValue) {

        section.put(
                fieldKey,
                resolveField(
                        sourceLabels,
                        fieldKey,
                        parsedValue));
    }

    private static void putAlias(
            Map<String, SyllabusImportData.SourceFieldProvenance> section,
            String resultFieldKey,
            Map<String, String> sourceLabels,
            String sourceFieldKey,
            Object parsedValue) {

        section.put(
                resultFieldKey,
                resolveField(
                        sourceLabels,
                        sourceFieldKey,
                        parsedValue));
    }

    private static SyllabusImportData.SourceFieldProvenance resolveField(
            Map<String, String> sourceLabels,
            String fieldKey,
            Object parsedValue) {

        boolean parsed =
                hasParsedValue(parsedValue);

        boolean evidencedInSource =
                sourceLabels.containsKey(fieldKey);

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

        if (evidencedInSource) {
            return SyllabusImportData.SourceFieldProvenance.builder()
                    .state(
                            SyllabusImportData.SourceFieldState.UNRESOLVED)
                    .sourceLabel(
                            sourceLabels.get(fieldKey))
                    .build();
        }

        return SyllabusImportData.SourceFieldProvenance.builder()
                .state(
                        SyllabusImportData.SourceFieldState.ABSENT_IN_SOURCE)
                .sourceLabel(null)
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
                    || !sectionKey.equals(section.getKey())) {
                continue;
            }

            List<SyllabusImportData.TemplateField> fields =
                    section.getFields();

            if (fields == null) {
                continue;
            }

            for (SyllabusImportData.TemplateField field : fields) {

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

    private static <T> Object anyParsed(
            List<T> items,
            Function<T, ?> extractor) {

        if (items == null
                || extractor == null) {
            return null;
        }

        for (T item : items) {

            if (item == null) {
                continue;
            }

            Object value =
                    extractor.apply(item);

            if (hasParsedValue(value)) {
                /*
                 * Provenance only needs to know that at least one canonical
                 * row value was parsed. Do not leak an arbitrary child row
                 * into resolver state.
                 */
                return Boolean.TRUE;
            }
        }

        return null;
    }

    private static List<SyllabusImportData.RubricCriteriaItem> rubricCriteria(
            List<SyllabusImportData.RubricItem> rubrics) {

        List<SyllabusImportData.RubricCriteriaItem> result =
                new ArrayList<>();

        if (rubrics == null) {
            return result;
        }

        for (SyllabusImportData.RubricItem rubric : rubrics) {

            if (rubric == null
                    || rubric.getCriteria() == null) {
                continue;
            }

            for (SyllabusImportData.RubricCriteriaItem criterion :
                    rubric.getCriteria()) {

                if (criterion != null) {
                    result.add(criterion);
                }
            }
        }

        return result;
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