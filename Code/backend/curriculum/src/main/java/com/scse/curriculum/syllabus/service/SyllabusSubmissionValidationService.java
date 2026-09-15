package com.scse.curriculum.syllabus.service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scse.curriculum.book.entity.Book;
import com.scse.curriculum.cloplomapping.entity.CloPloMapping;
import com.scse.curriculum.plo.entity.Plo;
import com.scse.curriculum.assessment.entity.AssessmentClo;
import com.scse.curriculum.assessment.entity.AssessmentComponent;
import com.scse.curriculum.assessment.repository.AssessmentCloRepository;
import com.scse.curriculum.assessment.repository.AssessmentComponentRepository;
import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.clo.repository.CloRepository;
import com.scse.curriculum.cloplomapping.repository.CloPloMappingRepository;
import com.scse.curriculum.syllabus.dto.SubmissionValidationIssue;
import com.scse.curriculum.syllabus.dto.SubmissionValidationResponse;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.exception.SyllabusSubmissionValidationException;
import com.scse.curriculum.syllabusbook.entity.SyllabusBook;
import com.scse.curriculum.syllabusbook.repository.SyllabusBookRepository;
import com.scse.curriculum.topic.entity.Topic;
import com.scse.curriculum.topic.repository.TopicRepository;
import com.scse.curriculum.topicclo.entity.TopicClo;
import com.scse.curriculum.topicclo.repository.TopicCloRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SyllabusSubmissionValidationService {

    private static final double EPSILON = 0.01d;
    private static final Pattern NUMBER_PATTERN = Pattern.compile(
            "[-+]?\\d+(?:[.,]\\d+)?");
    private static final Pattern CLO_CODE_PATTERN = Pattern.compile(
            "(?i)^CLO\\s*\\d+$");
    private static final Pattern SEMESTER_PATTERN = Pattern.compile(
            "(?i)^(?:(?:HK|SEMESTER)\\s*)?([1-8])$");

    private final CloRepository cloRepository;
    private final CloPloMappingRepository cloPloMappingRepository;

    private final TopicRepository topicRepository;
    private final TopicCloRepository topicCloRepository;

    private final AssessmentComponentRepository assessmentRepository;
    private final AssessmentCloRepository assessmentCloRepository;

    private final SyllabusBookRepository syllabusBookRepository;

    @Transactional(readOnly = true)
    public SubmissionValidationResponse validate(Syllabus syllabus) {
        ValidationCollector collector = new ValidationCollector(syllabus.getId());

        validateGeneralInfo(syllabus, collector);

        List<Clo> clos = cloRepository.findBySyllabusId(syllabus.getId());
        validateClos(clos, collector);

        List<Topic> topics = topicRepository
                .findBySyllabusIdOrderByWeekNumberAscOrderInWeekAsc(
                        syllabus.getId());

        List<TopicClo> topicCloMappings
                = topicCloRepository.findByTopic_Syllabus_Id(
                        syllabus.getId());

        validateTopics(
                topics,
                topicCloMappings,
                collector);

        List<AssessmentComponent> assessments
                = assessmentRepository.findBySyllabusId(
                        syllabus.getId());

        List<AssessmentClo> assessmentCloMappings
                = assessmentCloRepository
                        .findByAssessmentComponent_Syllabus_Id(
                                syllabus.getId());

        validateAssessments(
                assessments,
                assessmentCloMappings,
                collector);
        List<SyllabusBook> books = syllabusBookRepository
                .findBySyllabus_Id(syllabus.getId());
        validatereadings(books, collector);

        validateWorkload(syllabus, topics, collector);

        return collector.build();
    }

    public void validateOrThrow(Syllabus syllabus) {
        SubmissionValidationResponse result = validate(syllabus);
        if (!result.isValid()) {
            throw new SyllabusSubmissionValidationException(result);
        }
    }

    private void validateGeneralInfo(
            Syllabus syllabus,
            ValidationCollector collector) {

        if (syllabus.getCourse() == null) {
            collector.add(
                    "GENERAL_COURSE_REQUIRED",
                    "General Information",
                    1,
                    "course",
                    "The syllabus is not linked to a course.");
        } else {
            required(
                    syllabus.getCourse().getCourseCode(),
                    "GENERAL_COURSE_CODE_REQUIRED",
                    "courseCode",
                    "Course code is required.",
                    collector);
            required(
                    syllabus.getCourse().getName(),
                    "GENERAL_COURSE_NAME_REQUIRED",
                    "courseName",
                    "English course name is required.",
                    collector);
            required(
                    syllabus.getCourse().getNameVn(),
                    "GENERAL_COURSE_NAME_VN_REQUIRED",
                    "courseNameVn",
                    "Vietnamese course name is required.",
                    collector);

            Integer theory = syllabus.getCourse().getCreditTheory();
            Integer lab = syllabus.getCourse().getCreditLab();
            if (theory == null || lab == null || theory < 0 || lab < 0
                    || theory + lab <= 0) {
                collector.add(
                        "GENERAL_CREDITS_INVALID",
                        "General Information",
                        1,
                        "credits",
                        "Theory/lab credits must be valid and total credits must be greater than 0.");
            }
        }

        if (syllabus.getCreatedBy() == null) {
            collector.add(
                    "GENERAL_CREATOR_REQUIRED",
                    "General Information",
                    1,
                    "createdBy",
                    "The syllabus does not specify the preparing instructor.");
        }

        required(syllabus.getAcademicYear(),
                "GENERAL_ACADEMIC_YEAR_REQUIRED",
                "academicYear",
                "Academic year / applicable cohort is required.", collector);
        required(syllabus.getSemester(),
                "GENERAL_SEMESTER_REQUIRED",
                "semester",
                "Semester is required.", collector);
        validateSemesterFormat(syllabus.getSemester(), collector);
        required(syllabus.getVersionLabel(),
                "GENERAL_VERSION_LABEL_REQUIRED",
                "versionLabel",
                "Version label is required.", collector);
        /*
         * Program/CourseProgram is the curriculum identity. `major` is a
         * legacy display snapshot and must not duplicate that relationship
         * merely to make a syllabus submittable.
         */
        required(syllabus.getCourseDesignation(),
                "GENERAL_DESIGNATION_REQUIRED",
                "courseDesignation",
                "Course Designation is required.", collector);

        if (isEmptyCourseTypes(syllabus.getCourseTypes())) {
            collector.add(
                    "GENERAL_COURSE_TYPES_REQUIRED",
                    "General Information",
                    1,
                    "courseTypes",
                    "At least one course type must be selected.");
        }

        required(syllabus.getLanguage(),
                "GENERAL_LANGUAGE_REQUIRED",
                "language",
                "Language of instruction is required.", collector);
        required(syllabus.getRelation(),
                "GENERAL_RELATION_REQUIRED",
                "relation",
                "Curriculum relationship is required. Enter 'None' if not applicable.",
                collector);
        required(syllabus.getTeachingMethods(),
                "GENERAL_TEACHING_METHODS_REQUIRED",
                "teachingMethods",
                "Teaching methods are required.", collector);
        required(syllabus.getPrerequisites(),
                "GENERAL_PREREQUISITES_REQUIRED",
                "prerequisites",
                "Prerequisite information is required. Enter 'None' if not applicable.",
                collector);
        required(syllabus.getObjectives(),
                "GENERAL_OBJECTIVES_REQUIRED",
                "objectives",
                "Course objectives are required.", collector);
        required(syllabus.getExamForms(),
                "GENERAL_EXAM_FORMS_REQUIRED",
                "examForms",
                "Assessment / examination forms are required.", collector);
        required(syllabus.getExamRequirements(),
                "GENERAL_EXAM_REQUIREMENTS_REQUIRED",
                "examRequirements",
                "Learning and examination requirements are required.", collector);
    }

    private void validateSemesterFormat(
            String semester,
            ValidationCollector collector) {

        if (isBlank(semester)) {
            return;
        }

        String normalized = semester.trim();
        boolean summer = normalized.equalsIgnoreCase("SUMMER")
                || normalized.equalsIgnoreCase("HÈ")
                || normalized.equalsIgnoreCase("HE");

        if (!summer && !SEMESTER_PATTERN.matcher(normalized).matches()) {
            collector.add(
                    "GENERAL_SEMESTER_FORMAT_INVALID",
                    "General Information",
                    1,
                    "semester",
                    "Semester must be in the format Semester 1-8, or Summer.");
        }
    }

    private void validateClos(
        List<Clo> clos,
        ValidationCollector collector) {

    if (clos == null || clos.isEmpty()) {
        collector.add(
                "CLO_REQUIRED",
                "Course Learning Outcomes (CLO)",
                3,
                "clos",
                "The syllabus must contain at least one CLO.");
        return;
    }

    Set<String> seenCodes = new HashSet<>();

    for (int index = 0; index < clos.size(); index++) {
        Clo clo = clos.get(index);

        if (clo == null) {
            collector.add(
                    "CLO_INVALID",
                    "Course Learning Outcomes (CLO)",
                    3,
                    "clos[" + index + "]",
                    "CLO #" + (index + 1)
                            + " is invalid.");
            continue;
        }

        String label = isBlank(clo.getCode())
                ? "CLO #" + (index + 1)
                : clo.getCode().trim();

        if (isBlank(clo.getCode())) {
            collector.add(
                    "CLO_CODE_REQUIRED",
                    "Course Learning Outcomes (CLO)",
                    3,
                    "clos[" + index + "].code",
                    label + " does not have a CLO code.");
        } else {
            String normalizedCode = clo.getCode()
                    .replaceAll("\\s+", "")
                    .toUpperCase(Locale.ROOT);

            if (!seenCodes.add(normalizedCode)) {
                collector.add(
                        "CLO_CODE_DUPLICATED",
                        "Course Learning Outcomes (CLO)",
                        3,
                        "clos[" + index + "].code",
                        "CLO code '" + clo.getCode()
                                + "' is duplicated.");
            }

            if (!CLO_CODE_PATTERN
                    .matcher(clo.getCode().trim())
                    .matches()) {

                collector.add(
                        "CLO_CODE_FORMAT_INVALID",
                        "Course Learning Outcomes (CLO)",
                        3,
                        "clos[" + index + "].code",
                        "Code '" + clo.getCode()
                                + "' must follow the format "
                                + "CLO1, CLO2, ...");
            }
        }

        if (isBlank(clo.getDescription())) {
            collector.add(
                    "CLO_DESCRIPTION_REQUIRED",
                    "Course Learning Outcomes (CLO)",
                    3,
                    "clos[" + index + "].description",
                    label
                            + " does not have an outcome description.");
        }

        // Bloom taxonomy is an optional system enhancement. The official IU
        // syllabus template provides Competency level, not Bloom level, so a
        // source-faithful import must remain submittable with null Bloom data.

        if (clo.getCompetencyLevel() == null) {
            collector.add(
                    "CLO_COMPETENCY_REQUIRED",
                    "Course Learning Outcomes (CLO)",
                    3,
                    "clos[" + index
                            + "].competencyLevel",
                    label
                            + " does not have a competency type.");
        }

        List<CloPloMapping> mappings;

        if (clo.getId() == null) {
            mappings = List.of();
        } else {
            mappings = cloPloMappingRepository
                    .findByCloId(clo.getId());

            if (mappings == null) {
                mappings = List.of();
            }
        }

        validateCloPloMappings(
                clo,
                index,
                label,
                mappings,
                collector);
    }
}
private void validateCloPloMappings(
        Clo clo,
        int cloIndex,
        String cloLabel,
        List<CloPloMapping> mappings,
        ValidationCollector collector) {

    if (mappings.isEmpty()) {
        collector.add(
                "CLO_PLO_MAPPING_REQUIRED",
                "Mapping CLO–PLO",
                4,
                "clos[" + cloIndex + "].ploMappings",
                cloLabel
                        + " is not mapped "
                        + "to at least one PLO.");
        return;
    }

    Set<Integer> seenPloIds = new HashSet<>();

    for (int mappingIndex = 0;
            mappingIndex < mappings.size();
            mappingIndex++) {

        CloPloMapping mapping =
                mappings.get(mappingIndex);

        String fieldPrefix =
                "clos[" + cloIndex
                        + "].ploMappings["
                        + mappingIndex + "]";

        if (mapping == null) {
            collector.add(
                    "CLO_PLO_MAPPING_INVALID",
                    "Mapping CLO–PLO",
                    4,
                    fieldPrefix,
                    cloLabel
                            + " has an invalid CLO–PLO mapping "
                            + ".");
            continue;
        }

        Plo mappedPlo = mapping.getPlo();

        if (mappedPlo == null
                || mappedPlo.getId() == null) {

            collector.add(
                    "CLO_PLO_TARGET_REQUIRED",
                    "Mapping CLO–PLO",
                    4,
                    fieldPrefix + ".ploId",
                    cloLabel
                            + " has a mapping without a PLO.");
            continue;
        }

        if (!seenPloIds.add(mappedPlo.getId())) {
            collector.add(
                    "CLO_PLO_MAPPING_DUPLICATED",
                    "Mapping CLO–PLO",
                    4,
                    fieldPrefix + ".ploId",
                    cloLabel
                            + " has a duplicate mapping to "
                            + safePloLabel(mappedPlo)
                            + ".");
        }

        if (mapping.getLevel() == null) {
            collector.add(
                    "CLO_PLO_LEVEL_REQUIRED",
                    "Mapping CLO–PLO",
                    4,
                    fieldPrefix + ".level",
                    "Mapping from "
                            + cloLabel
                            + " to "
                            + safePloLabel(mappedPlo)
                            + " does not specify I, D, or A.");
        }

        Float contributionWeight =
                mapping.getContributionWeight();

        if (contributionWeight != null
                && (contributionWeight <= 0f
                || contributionWeight > 100f)) {

            collector.add(
                    "CLO_PLO_WEIGHT_INVALID",
                    "Mapping CLO–PLO",
                    4,
                    fieldPrefix
                            + ".contributionWeight",
                    "Contribution weight from "
                            + cloLabel
                            + " to "
                            + safePloLabel(mappedPlo)
                            + " must be greater than 0 and "
                            + "must not exceed 100%.");
        }
    }
}
    private void validateTopics(
            List<Topic> topics,
            List<TopicClo> topicCloMappings,
            ValidationCollector collector) {

        if (topics.isEmpty()) {
            collector.add(
                    "TOPIC_REQUIRED",
                    "Teaching Content",
                    5,
                    "topics",
                    "The syllabus must contain at least one teaching topic.");
            return;
        }
        if (topicCloMappings == null || topicCloMappings.isEmpty()) {
            collector.add(
                    "TOPIC_CLO_MAPPING_REQUIRED",
                    "Mapping Topic–CLO",
                    5,
                    "topicCloMappings",
                    "At least one Topic–CLO mapping from the official teaching plan is required.");
        }

        Map<Integer, List<TopicClo>> mappingsByTopic
                = topicCloMappings.stream()
                        .filter(mapping
                                -> mapping != null
                        && mapping.getTopic() != null
                        && mapping.getTopic().getId() != null)
                        .collect(Collectors.groupingBy(
                                mapping -> mapping.getTopic().getId()));

        Set<String> positions = new HashSet<>();
        for (int index = 0; index < topics.size(); index++) {
            Topic topic = topics.get(index);
            String label = "Topic #" + (index + 1);

            if (isBlank(topic.getName())) {
                collector.add(
                        "TOPIC_NAME_REQUIRED",
                        "Teaching Content",
                        5,
                        "topics[" + index + "].name",
                        label + " does not have a topic name.");
            }

            Integer week = topic.getWeekNumber();
            if (week == null || week < 1 || week > 52) {
                collector.add(
                        "TOPIC_WEEK_INVALID",
                        "Teaching Content",
                        5,
                        "topics[" + index + "].weekNumber",
                        label + " must have a week number from 1 to 52.");
            }

            Integer order = topic.getOrderInWeek();
            if (order == null || order < 1) {
                collector.add(
                        "TOPIC_ORDER_INVALID",
                        "Teaching Content",
                        5,
                        "topics[" + index + "].orderInWeek",
                        label + " must have an order within the week greater than 0.");
            }

            if (week != null && order != null) {
                String position = week + ":" + order;
                if (!positions.add(position)) {
                    collector.add(
                            "TOPIC_POSITION_DUPLICATED",
                            "Teaching Content",
                            5,
                            "topics[" + index + "]",
                            "Week " + week + " contains two topics with the same order " + order + ".");
                }
            }

            int teaching = nonNegativeHours(
                    topic.getTeachingHours(),
                    "teachingHours",
                    index,
                    label,
                    collector);
            int lab = nonNegativeHours(
                    topic.getLabHours(),
                    "labHours",
                    index,
                    label,
                    collector);
            int selfStudy = nonNegativeHours(
                    topic.getSelfStudyHours(),
                    "selfStudyHours",
                    index,
                    label,
                    collector);

            if (teaching + lab + selfStudy <= 0) {
                collector.add(
                        "TOPIC_WORKLOAD_REQUIRED",
                        "Teaching Content",
                        5,
                        "topics[" + index + "].hours",
                        label + " must have at least one teaching or self-study hour.");
            }

            if (topic.getTopicType() == null) {
                collector.add(
                        "TOPIC_TYPE_REQUIRED",
                        "Teaching Content",
                        5,
                        "topics[" + index + "].topicType",
                        label + " does not specify a topic type.");
            }
            List<TopicClo> mappings
                    = topic.getId() == null
                    ? List.of()
                    : mappingsByTopic.getOrDefault(
                            topic.getId(),
                            List.of());

            validateTopicCloMappings(
                    topic,
                    index,
                    label,
                    mappings,
                    collector);
        }
    }

    private void validateTopicCloMappings(
            Topic topic,
            int topicIndex,
            String topicLabel,
            List<TopicClo> mappings,
            ValidationCollector collector) {

        // Official syllabus tables may intentionally leave an exam/review week
        // without a CLO value. Validate mappings that exist, but do not invent
        // or require a mapping for every individual row.
        if (mappings.isEmpty()) return;

        Set<Integer> seenCloIds = new HashSet<>();

        for (int mappingIndex = 0;
                mappingIndex < mappings.size();
                mappingIndex++) {

            TopicClo mapping = mappings.get(mappingIndex);

            String fieldPrefix
                    = "topics[" + topicIndex
                    + "].cloMappings["
                    + mappingIndex + "]";

            if (mapping == null) {
                collector.add(
                        "TOPIC_CLO_MAPPING_INVALID",
                        "Mapping Topic–CLO",
                        5,
                        fieldPrefix,
                        topicLabel
                        + " has an invalid Topic–CLO mapping.");
                continue;
            }

            Clo mappedClo = mapping.getClo();

            if (mappedClo == null || mappedClo.getId() == null) {
                collector.add(
                        "TOPIC_CLO_TARGET_REQUIRED",
                        "Mapping Topic–CLO",
                        5,
                        fieldPrefix + ".cloId",
                        topicLabel
                        + " has a mapping without a CLO.");
                continue;
            }

            if (!seenCloIds.add(mappedClo.getId())) {
                collector.add(
                        "TOPIC_CLO_MAPPING_DUPLICATED",
                        "Mapping Topic–CLO",
                        5,
                        fieldPrefix + ".cloId",
                        topicLabel
                        + " has a duplicate mapping to "
                        + safeCloLabel(mappedClo) + ".");
            }

            if (!sameSyllabus(
                    topic.getSyllabus(),
                    mappedClo.getSyllabus())) {

                collector.add(
                        "TOPIC_CLO_CROSS_SYLLABUS",
                        "Mapping Topic–CLO",
                        5,
                        fieldPrefix + ".cloId",
                        topicLabel
                        + " is mapped to a CLO belonging to "
                        + "another syllabus.");
            }

            // The official planned-activities table maps Topic to CLO but has
            // no mapping-level field. Teaching level remains optional metadata.
        }
    }

    private int nonNegativeHours(
            Integer value,
            String field,
            int index,
            String label,
            ValidationCollector collector) {

        if (value == null || value < 0) {
            collector.add(
                    "TOPIC_HOURS_INVALID",
                    "Teaching Content",
                    5,
                    "topics[" + index + "]." + field,
                    label + " has invalid hours.");
            return 0;
        }
        return value;
    }

    private void validateAssessments(
            List<AssessmentComponent> assessments,
            List<AssessmentClo> assessmentCloMappings,
            ValidationCollector collector) {

        if (assessments.isEmpty()) {
            collector.add(
                    "ASSESSMENT_REQUIRED",
                    "Assessment Plan",
                    6,
                    "assessments",
                    "The syllabus must contain at least one assessment component.");
            collector.assessmentTotalWeight = 0d;
            return;
        }
        Map<Integer, List<AssessmentClo>> mappingsByAssessment
                = assessmentCloMappings.stream()
                        .filter(mapping
                                -> mapping != null
                        && mapping
                                .getAssessmentComponent()
                        != null
                        && mapping
                                .getAssessmentComponent()
                                .getId()
                        != null)
                        .collect(Collectors.groupingBy(
                                mapping -> mapping
                                        .getAssessmentComponent()
                                        .getId()));

        double total = 0d;
        for (int index = 0; index < assessments.size(); index++) {
            AssessmentComponent assessment = assessments.get(index);
            String label = isBlank(assessment.getName())
                    ? "Assessment component #" + (index + 1)
                    : assessment.getName().trim();

            if (isBlank(assessment.getName())) {
                collector.add(
                        "ASSESSMENT_NAME_REQUIRED",
                        "Assessment Plan",
                        6,
                        "assessments[" + index + "].name",
                        label + " does not have a name.");
            }

            if (assessment.getAssessmentType() == null) {
                collector.add(
                        "ASSESSMENT_TYPE_REQUIRED",
                        "Assessment Plan",
                        6,
                        "assessments[" + index + "].assessmentType",
                        label + " does not specify an assessment type.");
            }

            Float weight = assessment.getWeightPercent();
            if (weight == null || weight <= 0f || weight > 100f) {
                collector.add(
                        "ASSESSMENT_WEIGHT_INVALID",
                        "Assessment Plan",
                        6,
                        "assessments[" + index + "].weightPercent",
                        label + " must have a weight greater than 0 and must not exceed 100%.");
            } else {
                total += weight;
            }

            Float minScore = assessment.getMinScore();
            Float maxScore = assessment.getMaxScore();
            if (minScore == null || maxScore == null
                    || minScore < 0f
                    || maxScore <= minScore
                    || maxScore > 100f) {
                collector.add(
                        "ASSESSMENT_SCORE_RANGE_INVALID",
                        "Assessment Plan",
                        6,
                        "assessments[" + index + "].scoreRange",
                        label + " must have a valid score range: 0 ≤ minimum score < maximum score ≤ 100.");
            }
            List<AssessmentClo> mappings
                    = assessment.getId() == null
                    ? List.of()
                    : mappingsByAssessment.getOrDefault(
                            assessment.getId(),
                            List.of());

            validateAssessmentCloMappings(
                    assessment,
                    index,
                    label,
                    mappings,
                    collector);
        }

        collector.assessmentTotalWeight = round(total);
        if (!closeEnough(total, 100d)) {
            collector.add(
                    "ASSESSMENT_TOTAL_WEIGHT_INVALID",
                    "Assessment Plan",
                    6,
                    "assessmentTotalWeight",
                    "The total assessment weight must equal 100%. Current total: "
                    + formatNumber(total) + "%.");
        }
    }

    private void validateAssessmentCloMappings(
            AssessmentComponent assessment,
            int assessmentIndex,
            String assessmentLabel,
            List<AssessmentClo> mappings,
            ValidationCollector collector) {

        if (mappings.isEmpty()) {
            collector.add(
                    "ASSESSMENT_CLO_MAPPING_REQUIRED",
                    "Mapping Assessment–CLO",
                    6,
                    "assessments[" + assessmentIndex
                            + "].cloMappings",
                    assessmentLabel
                            + " is not mapped to at least one CLO.");
            return;
        }

        Set<Integer> seenCloIds = new HashSet<>();

        for (int mappingIndex = 0;
                mappingIndex < mappings.size();
                mappingIndex++) {

            AssessmentClo mapping = mappings.get(mappingIndex);

            String fieldPrefix =
                    "assessments[" + assessmentIndex
                            + "].cloMappings["
                            + mappingIndex + "]";

            if (mapping == null) {
                collector.add(
                        "ASSESSMENT_CLO_MAPPING_INVALID",
                        "Mapping Assessment–CLO",
                        6,
                        fieldPrefix,
                        assessmentLabel
                                + " has an invalid Assessment–CLO mapping "
                                + ".");
                continue;
            }

            Clo mappedClo = mapping.getClo();

            if (mappedClo == null || mappedClo.getId() == null) {
                collector.add(
                        "ASSESSMENT_CLO_TARGET_REQUIRED",
                        "Mapping Assessment–CLO",
                        6,
                        fieldPrefix + ".cloId",
                        assessmentLabel
                                + " has a mapping without a CLO.");
                continue;
            }

            if (!seenCloIds.add(mappedClo.getId())) {
                collector.add(
                        "ASSESSMENT_CLO_MAPPING_DUPLICATED",
                        "Mapping Assessment–CLO",
                        6,
                        fieldPrefix + ".cloId",
                        assessmentLabel
                                + " has a duplicate mapping to "
                                + safeCloLabel(mappedClo) + ".");
            }

            if (!sameSyllabus(
                    assessment.getSyllabus(),
                    mappedClo.getSyllabus())) {

                collector.add(
                        "ASSESSMENT_CLO_CROSS_SYLLABUS",
                        "Mapping Assessment–CLO",
                        6,
                        fieldPrefix + ".cloId",
                        assessmentLabel
                                + " is mapped to a CLO belonging to "
                                + "another syllabus.");
            }

            Float contribution = mapping.getContributionPercent();

            if (contribution == null
                    || contribution <= 0f
                    || contribution > 100f) {

                collector.add(
                        "ASSESSMENT_CLO_CONTRIBUTION_INVALID",
                        "Mapping Assessment–CLO",
                        6,
                        fieldPrefix + ".contributionPercent",
                        "Contribution percentage from "
                                + assessmentLabel
                                + " to "
                                + safeCloLabel(mappedClo)
                                + " must be greater than 0 and must not exceed 100%.");
            }
        }
    }

    private void validatereadings(
        List<SyllabusBook> books,
        ValidationCollector collector) {

    if (books == null || books.isEmpty()) {
        collector.add(
                "READING_LIST_REQUIRED",
                "Reading List",
                7,
                "readings",
                "The syllabus must contain at least "
                        + "one reading resource.");
        return;
    }

    for (int index = 0;
            index < books.size();
            index++) {

        SyllabusBook linkedBook = books.get(index);
        String fieldPrefix =
                "readings[" + index + "]";

        if (linkedBook == null) {
            collector.add(
                    "READING_ITEM_INVALID",
                    "Reading List",
                    7,
                    fieldPrefix,
                    "Resource #" + (index + 1)
                            + " is invalid.");
            continue;
        }

        Book book = linkedBook.getBook();

        if (book == null) {
            collector.add(
                    "READING_BOOK_REQUIRED",
                    "Reading List",
                    7,
                    fieldPrefix + ".book",
                    "Resource #" + (index + 1)
                            + " is not linked to "
                            + "book/resource information.");
        } else {
            if (isBlank(book.getTitle())) {
                collector.add(
                        "READING_BOOK_TITLE_REQUIRED",
                        "Reading List",
                        7,
                        fieldPrefix + ".title",
                        "Resource #" + (index + 1)
                                + " does not have a "
                                + "book/resource title.");
            }

            if (isBlank(book.getAuthor())) {
                collector.add(
                        "READING_BOOK_AUTHOR_REQUIRED",
                        "Reading List",
                        7,
                        fieldPrefix + ".author",
                        "Resource #" + (index + 1)
                                + " does not have an author.");
            }

            if (book.getYear() == null
                    || book.getYear() <= 0) {

                collector.add(
                        "READING_BOOK_YEAR_REQUIRED",
                        "Reading List",
                        7,
                        fieldPrefix + ".year",
                        "Resource #" + (index + 1)
                                + " does not have a valid publication year "
                                + ".");
            }
        }

        if (linkedBook.getUsageType() == null) {
            collector.add(
                    "READING_USAGE_TYPE_REQUIRED",
                    "Reading List",
                    7,
                    fieldPrefix + ".usageType",
                    "Resource #" + (index + 1)
                            + " does not specify a usage type.");
        }
    }
}
    private void validateWorkload(
            Syllabus syllabus,
            List<Topic> topics,
            ValidationCollector collector) {

        Double total = parseWorkload(
                syllabus.getWorkloadTotal(),
                "WORKLOAD_TOTAL_INVALID",
                "workloadTotal",
                "Total Workload must be greater than 0.",
                true,
                collector);
        Double contact = parseContactWorkload(
                syllabus.getWorkloadContact(),
                "WORKLOAD_CONTACT_INVALID",
                "workloadContact",
                "Contact Hours must be non-negative.",
                false,
                collector);
        Double privateStudy = parseWorkload(
                syllabus.getWorkloadPrivate(),
                "WORKLOAD_PRIVATE_INVALID",
                "workloadPrivate",
                "Private Study must be non-negative.",
                false,
                collector);

        collector.workloadTotal = total;
        collector.workloadContact = contact;
        collector.workloadPrivate = privateStudy;

        double topicContact = topics.stream()
                .mapToDouble(topic
                        -> safeHours(topic.getTeachingHours())
                + safeHours(topic.getLabHours()))
                .sum();
        double topicPrivate = topics.stream()
                .mapToDouble(topic -> safeHours(topic.getSelfStudyHours()))
                .sum();

        collector.topicContactHours = round(topicContact);
        collector.topicPrivateHours = round(topicPrivate);

        if (total != null && contact != null && privateStudy != null
                && !closeEnough(total, contact + privateStudy)) {
            collector.add(
                    "WORKLOAD_SUM_MISMATCH",
                    "General Information",
                    1,
                    "workloadTotal",
                    "Total Workload must equal Contact Hours + Private Study. Current values: "
                    + formatNumber(total) + " ≠ "
                    + formatNumber(contact) + " + "
                    + formatNumber(privateStudy) + ".");
        }

        /*
         * The official template declares aggregate lecture/laboratory/private
         * workload, but does not allocate those hours to each topic. Topic
         * totals remain useful diagnostics in the response and are not a
         * submission blocker.
         */
    }

    private Double parseWorkload(
            String raw,
            String code,
            String field,
            String message,
            boolean mustBePositive,
            ValidationCollector collector) {

        if (isBlank(raw)) {
            collector.add(
                    code,
                    "General Information",
                    1,
                    field,
                    message);
            return null;
        }

        Matcher matcher = NUMBER_PATTERN.matcher(raw.trim());
        if (!matcher.find()) {
            collector.add(
                    code,
                    "General Information",
                    1,
                    field,
                    message);
            return null;
        }

        try {
            double value = Double.parseDouble(
                    matcher.group().replace(',', '.'));
            boolean invalid = mustBePositive ? value <= 0d : value < 0d;
            if (invalid) {
                collector.add(
                        code,
                        "General Information",
                        1,
                        field,
                        message);
                return null;
            }
            return round(value);
        } catch (NumberFormatException ex) {
            collector.add(
                    code,
                    "General Information",
                    1,
                    field,
                    message);
            return null;
        }
    }

    /**
     * Contact workload is currently stored in a legacy display string, for
     * example "45 (lecture) + 30 (laboratory)". All components contribute to
     * contact hours; taking only the first number incorrectly rejects a
     * mathematically valid workload.
     */
    private Double parseContactWorkload(
            String raw,
            String code,
            String field,
            String message,
            boolean mustBePositive,
            ValidationCollector collector) {
        if (isBlank(raw)) {
            collector.add(code, "General Information", 1, field, message);
            return null;
        }

        Matcher matcher = NUMBER_PATTERN.matcher(raw.trim());
        double total = 0d;
        boolean found = false;
        try {
            while (matcher.find()) {
                total += Double.parseDouble(matcher.group().replace(',', '.'));
                found = true;
            }
        } catch (NumberFormatException ex) {
            found = false;
        }

        boolean invalid = !found || (mustBePositive ? total <= 0d : total < 0d);
        if (invalid) {
            collector.add(code, "General Information", 1, field, message);
            return null;
        }
        return round(total);
    }

    private void required(
            String value,
            String code,
            String field,
            String message,
            ValidationCollector collector) {

        if (isBlank(value)) {
            collector.add(
                    code,
                    "General Information",
                    1,
                    field,
                    message);
        }
    }

    private boolean isEmptyCourseTypes(String raw) {
        if (isBlank(raw)) {
            return true;
        }
        String normalized = raw.trim()
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
        return normalized.equals("[]")
                || normalized.equals("null")
                || normalized.equals("{}")
                || normalized.equals("[\"\"]");
    }

    private boolean sameSyllabus(
            Syllabus first,
            Syllabus second) {

        if (first == null || second == null) {
            return false;
        }

        if (first == second) {
            return true;
        }

        return first.getId() != null
                && first.getId().equals(second.getId());
    }

    private String safeCloLabel(Clo clo) {
        if (clo == null) {
            return "Unspecified CLO";
        }

        if (!isBlank(clo.getCode())) {
            return clo.getCode().trim();
        }

        return clo.getId() == null
                ? "Unspecified CLO"
                : "CLO #" + clo.getId();
    }
    
    private String safePloLabel(Plo plo) {
    if (plo == null) {
        return "Unspecified PLO";
    }

    if (!isBlank(plo.getCode())) {
        return plo.getCode().trim();
    }

    return plo.getId() == null
            ? "Unspecified PLO"
            : "PLO #" + plo.getId();
}
    private int safeHours(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    private boolean closeEnough(double left, double right) {
        return Math.abs(left - right) <= EPSILON;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private double round(double value) {
        return Math.round(value * 100d) / 100d;
    }

    private String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) <= EPSILON) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static final class ValidationCollector {

        private final Integer syllabusId;
        private final java.util.ArrayList<SubmissionValidationIssue> issues
                = new java.util.ArrayList<>();

        private Double assessmentTotalWeight;
        private Double workloadTotal;
        private Double workloadContact;
        private Double workloadPrivate;
        private Double topicContactHours;
        private Double topicPrivateHours;

        private ValidationCollector(Integer syllabusId) {
            this.syllabusId = syllabusId;
        }

        private void add(
                String code,
                String section,
                Integer tabId,
                String field,
                String message) {

            issues.add(SubmissionValidationIssue.builder()
                    .code(code)
                    .section(section)
                    .tabId(tabId)
                    .field(field)
                    .message(message)
                    .build());
        }

        private SubmissionValidationResponse build() {
            boolean valid = issues.isEmpty();
            return SubmissionValidationResponse.builder()
                    .syllabusId(syllabusId)
                    .valid(valid)
                    .message(valid
                            ? "The syllabus meets all submission requirements."
                            : "The syllabus has " + issues.size()
                            + " errors that must be fixed before submission.")
                    .errorCount(issues.size())
                    .assessmentTotalWeight(assessmentTotalWeight)
                    .workloadTotal(workloadTotal)
                    .workloadContact(workloadContact)
                    .workloadPrivate(workloadPrivate)
                    .topicContactHours(topicContactHours)
                    .topicPrivateHours(topicPrivateHours)
                    .issues(List.copyOf(issues))
                    .build();
        }
    }
}
