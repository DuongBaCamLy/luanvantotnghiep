package com.scse.curriculum.syllabus.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.Normalizer;
import com.scse.curriculum.assessment.entity.AssessmentClo;
import com.scse.curriculum.assessment.entity.AssessmentComponent;
import com.scse.curriculum.assessment.repository.AssessmentCloRepository;
import com.scse.curriculum.assessment.repository.AssessmentComponentRepository;
import com.scse.curriculum.book.entity.Book;
import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.clo.repository.CloRepository;
import com.scse.curriculum.cloplomapping.entity.CloPloMapping;
import com.scse.curriculum.cloplomapping.repository.CloPloMappingRepository;
import com.scse.curriculum.syllabus.dto.SyllabusDiffResponse;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabusbook.entity.SyllabusBook;
import com.scse.curriculum.syllabusbook.repository.SyllabusBookRepository;
import com.scse.curriculum.topic.entity.Topic;
import com.scse.curriculum.topic.repository.TopicRepository;
import com.scse.curriculum.topicclo.entity.TopicClo;
import com.scse.curriculum.topicclo.repository.TopicCloRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;

/**
 * Canonical field-by-field structural comparison for syllabus content.
 *
 * The comparison follows the standard syllabus form and is intended for
 * logical syllabus rows of the same course/program across cohorts.
 * Access/comparability rules remain the responsibility of SyllabusServiceImpl.
 *
 * Raw Syllabus.notes JSON is never returned as one diff field. Structured
 * supplemental values are parsed and placed in their canonical form sections.
 */
@Service
@RequiredArgsConstructor
public class SyllabusDiffService {

    private static final ObjectMapper NOTES_MAPPER = new ObjectMapper();
private static final Pattern WORKLOAD_NUMBER_PATTERN =
        Pattern.compile("-?\\d+(?:[.,]\\d+)?");
    private static final String DEFAULT_ASSESSMENT_PASS_NOTE =
            "Target percentage of students having scores greater than 50 out of 100.";

    private static final String DEFAULT_CONTENT_NOTE =
            "Weight: lecture session (3 hours). Teaching levels: "
                    + "I (Introduce); T (Teach); U (Utilize).";

    private final CloRepository cloRepository;
    private final CloPloMappingRepository cloPloMappingRepository;
    private final TopicRepository topicRepository;
    private final TopicCloRepository topicCloRepository;
    private final AssessmentComponentRepository assessmentComponentRepository;
    private final AssessmentCloRepository assessmentCloRepository;
    private final SyllabusBookRepository syllabusBookRepository;

    @Transactional(readOnly = true)
    public SyllabusDiffResponse compare(
            Syllabus oldSyllabus,
            Syllabus newSyllabus) {

        Integer oldId = oldSyllabus.getId();
        Integer newId = newSyllabus.getId();

        /*
         * Syllabus.notes contains the supplemental portion of the standard
         * syllabus form. Parse it once and compare its fields individually.
         *
         * Relation-backed matrices/readings remain authoritative in their
         * dedicated repositories and are not compared again from raw JSON.
         */
        SupplementalNotes oldSupplemental =
                parseSupplementalNotes(oldSyllabus.getNotes());
        SupplementalNotes newSupplemental =
                parseSupplementalNotes(newSyllabus.getNotes());

        Map<String, SyllabusDiffResponse.FieldDiff> generalInfoDiff =
                compareGeneralInfo(
                        oldSyllabus,
                        newSyllabus,
                        oldSupplemental,
                        newSupplemental);

        Map<String, SyllabusDiffResponse.FieldDiff> workloadCreditDiff =
                compareWorkloadCredit(
                        oldSyllabus,
                        newSyllabus,
                        oldSupplemental,
                        newSupplemental);

        Map<String, SyllabusDiffResponse.FieldDiff> requirementsDiff =
                compareRequirements(oldSyllabus, newSyllabus);

        Map<String, SyllabusDiffResponse.FieldDiff> contentDiff =
                compareContentInfo(oldSupplemental, newSupplemental);

        Map<String, SyllabusDiffResponse.FieldDiff> assessmentInfoDiff =
                compareAssessmentInfo(oldSupplemental, newSupplemental);

        Map<String, SyllabusDiffResponse.FieldDiff> examinationDiff =
                compareExamination(oldSyllabus, newSyllabus);

        Map<String, SyllabusDiffResponse.FieldDiff> revisionInfoDiff =
                compareRevisionInfo(
                        oldSyllabus,
                        newSyllabus,
                        oldSupplemental,
                        newSupplemental);

        List<Clo> oldClos = cloRepository.findBySyllabusId(oldId);
        List<Clo> newClos = cloRepository.findBySyllabusId(newId);

        MatchResult<Clo> cloMatches = matchItems(
                oldClos,
                newClos,
                clo -> normalizedKey(clo.getCode()),
                clo -> numberKey(clo.getOrderIndex()));

        IdentityKeys cloIdentities = buildIdentityKeys(
                cloMatches,
                Clo::getId,
                clo -> normalizedKey(clo.getCode()));

        List<CloPloMapping> oldCloPloMappings =
                cloPloMappingRepository.findByClo_Syllabus_Id(oldId);
        List<CloPloMapping> newCloPloMappings =
                cloPloMappingRepository.findByClo_Syllabus_Id(newId);

        Map<Integer, List<String>> oldPlosByClo =
                plosByClo(oldCloPloMappings);
        Map<Integer, List<String>> newPlosByClo =
                plosByClo(newCloPloMappings);

        SyllabusDiffResponse.ListDiff<SyllabusDiffResponse.CloDiff> cloDiff =
                compareClos(cloMatches, oldPlosByClo, newPlosByClo);

        List<Topic> oldTopics = topicRepository.findBySyllabusId(oldId);
        List<Topic> newTopics = topicRepository.findBySyllabusId(newId);

        Map<Topic, TopicSupplement> oldTopicSupplements =
                topicSupplements(oldTopics, oldSupplemental);
        Map<Topic, TopicSupplement> newTopicSupplements =
                topicSupplements(newTopics, newSupplemental);

        /*
         * Cross-cohort topic identity:
         * 1. normalized topic name first;
         * 2. position only for unnamed legacy rows.
         */
        MatchResult<Topic> topicMatches = matchItems(
                oldTopics,
                newTopics,
                this::topicNameKey,
                this::topicPositionFallbackKey);

        IdentityKeys topicIdentities = buildIdentityKeys(
                topicMatches,
                Topic::getId,
                this::topicIdentityKey);

        SyllabusDiffResponse.ListDiff<SyllabusDiffResponse.TopicDiff> topicDiff =
                compareTopics(
                        topicMatches,
                        oldTopicSupplements,
                        newTopicSupplements);

        List<PlannedActivity> oldPlannedActivities =
                effectivePlannedActivities(
                        oldTopics,
                        oldSupplemental,
                        oldTopicSupplements);

        List<PlannedActivity> newPlannedActivities =
                effectivePlannedActivities(
                        newTopics,
                        newSupplemental,
                        newTopicSupplements);

        SyllabusDiffResponse.ListDiff<SyllabusDiffResponse.PlannedActivityDiff>
                plannedActivityDiff =
                comparePlannedActivities(
                        oldPlannedActivities,
                        newPlannedActivities);

        List<AssessmentComponent> oldAssessments =
                assessmentComponentRepository.findBySyllabusId(oldId);
        List<AssessmentComponent> newAssessments =
                assessmentComponentRepository.findBySyllabusId(newId);

        /*
         * Cross-cohort assessment identity:
         * 1. normalized name;
         * 2. unique type;
         * 3. order only for completely unnamed/untyped legacy rows.
         */
        MatchResult<AssessmentComponent> assessmentMatches = matchItems(
                oldAssessments,
                newAssessments,
                this::assessmentNameKey,
                this::assessmentSecondaryKey);

        IdentityKeys assessmentIdentities = buildIdentityKeys(
                assessmentMatches,
                AssessmentComponent::getId,
                this::assessmentIdentityKey);

        SyllabusDiffResponse.ListDiff<SyllabusDiffResponse.AssessmentDiff>
                assessmentDiff =
                compareAssessments(assessmentMatches);

        SyllabusDiffResponse.ListDiff<SyllabusDiffResponse.CloPloMappingDiff>
                cloPloDiff =
                compareCloPloMappings(
                        oldCloPloMappings,
                        newCloPloMappings,
                        cloIdentities);

        SyllabusDiffResponse.ListDiff<SyllabusDiffResponse.TopicCloMappingDiff>
                topicCloDiff =
                compareTopicCloMappings(
                        topicCloRepository.findByTopic_Syllabus_Id(oldId),
                        topicCloRepository.findByTopic_Syllabus_Id(newId),
                        topicIdentities,
                        cloIdentities);

        SyllabusDiffResponse.ListDiff<SyllabusDiffResponse.AssessmentCloMappingDiff>
                assessmentCloDiff =
                compareAssessmentCloMappings(
                        assessmentCloRepository
                                .findByAssessmentComponent_Syllabus_Id(oldId),
                        assessmentCloRepository
                                .findByAssessmentComponent_Syllabus_Id(newId),
                        assessmentIdentities,
                        cloIdentities);

        /*
         * Reading relations are canonical. notes.readings is intentionally
         * not compared a second time, preventing duplicate changes.
         */
        SyllabusDiffResponse.ListDiff<SyllabusDiffResponse.readingsDiff>
                readingsDiff =
                comparereadings(
                        syllabusBookRepository.findBySyllabus_Id(oldId),
                        syllabusBookRepository.findBySyllabus_Id(newId));

        boolean hasChanges =
                !generalInfoDiff.isEmpty()
                        || !workloadCreditDiff.isEmpty()
                        || !requirementsDiff.isEmpty()
                        || !contentDiff.isEmpty()
                        || !assessmentInfoDiff.isEmpty()
                        || !examinationDiff.isEmpty()
                        || !revisionInfoDiff.isEmpty()
                        || hasEntries(cloDiff)
                        || hasEntries(cloPloDiff)
                        || hasEntries(topicDiff)
                        || hasEntries(topicCloDiff)
                        || hasEntries(plannedActivityDiff)
                        || hasEntries(assessmentDiff)
                        || hasEntries(assessmentCloDiff)
                        || hasEntries(readingsDiff);

        return SyllabusDiffResponse.builder()
                .oldSyllabusId(oldId)
                .newSyllabusId(newId)
                .oldVersionLabel(oldSyllabus.getVersionLabel())
                .newVersionLabel(newSyllabus.getVersionLabel())
                .hasChanges(hasChanges)
                .generalInfoDiff(generalInfoDiff)
                .workloadCreditDiff(workloadCreditDiff)
                .requirementsDiff(requirementsDiff)
                .contentDiff(contentDiff)
                .assessmentInfoDiff(assessmentInfoDiff)
                .examinationDiff(examinationDiff)
                .revisionInfoDiff(revisionInfoDiff)
                .cloDiff(cloDiff)
                .cloPloDiff(cloPloDiff)
                .topicDiff(topicDiff)
                .topicCloDiff(topicCloDiff)
                .plannedActivityDiff(plannedActivityDiff)
                .assessmentDiff(assessmentDiff)
                .assessmentCloDiff(assessmentCloDiff)
                .readingsDiff(readingsDiff)
                .build();
    }

    private Map<String, SyllabusDiffResponse.FieldDiff> compareGeneralInfo(
            Syllabus oldSyllabus,
            Syllabus newSyllabus,
            SupplementalNotes oldSupplemental,
            SupplementalNotes newSupplemental) {

        Map<String, SyllabusDiffResponse.FieldDiff> changes =
                new LinkedHashMap<>();

        addFieldDiff(
                changes,
                "courseCode",
                courseCodeForDiff(oldSyllabus),
                courseCodeForDiff(newSyllabus));

        addFieldDiff(
                changes,
                "courseName",
                courseNameForDiff(oldSyllabus),
                courseNameForDiff(newSyllabus));

        addFieldDiff(
                changes,
                "courseNameVn",
                courseNameVnForDiff(oldSyllabus),
                courseNameVnForDiff(newSyllabus));

        addFieldDiff(
                changes,
                "academicYear",
                oldSyllabus.getAcademicYear(),
                newSyllabus.getAcademicYear());

        addFieldDiff(
                changes,
                "program",
                oldSyllabus.getProgram(),
                newSyllabus.getProgram());

        addFieldDiff(
                changes,
                "courseDesignation",
                oldSyllabus.getCourseDesignation(),
                newSyllabus.getCourseDesignation());

        addFieldDiff(
                changes,
                "courseTypes",
                oldSyllabus.getCourseTypes(),
                newSyllabus.getCourseTypes());

        addFieldDiff(
                changes,
                "semester",
                oldSyllabus.getSemester(),
                newSyllabus.getSemester());

        addFieldDiff(
                changes,
                "personResponsible",
                oldSupplemental.personResponsible(),
                newSupplemental.personResponsible());

        addFieldDiff(
                changes,
                "language",
                oldSyllabus.getLanguage(),
                newSyllabus.getLanguage());

        addFieldDiff(
                changes,
                "relation",
                oldSyllabus.getRelation(),
                newSyllabus.getRelation());

        addFieldDiff(
                changes,
                "teachingMethods",
                oldSyllabus.getTeachingMethods(),
                newSyllabus.getTeachingMethods());

        addFieldDiff(
                changes,
                "major",
                oldSyllabus.getMajor(),
                newSyllabus.getMajor());

        return changes;
    }

    private Map<String, SyllabusDiffResponse.FieldDiff> compareWorkloadCredit(
            Syllabus oldSyllabus,
            Syllabus newSyllabus,
            SupplementalNotes oldSupplemental,
            SupplementalNotes newSupplemental) {

        Map<String, SyllabusDiffResponse.FieldDiff> changes =
                new LinkedHashMap<>();

        addFieldDiff(
                changes,
                "workloadTotal",
                oldSyllabus.getWorkloadTotal(),
                newSyllabus.getWorkloadTotal());

        addFieldDiff(
                changes,
                "workloadContact",
                oldSyllabus.getWorkloadContact(),
                newSyllabus.getWorkloadContact());

        addFieldDiff(
                changes,
                "workloadPrivate",
                oldSyllabus.getWorkloadPrivate(),
                newSyllabus.getWorkloadPrivate());

        addFieldDiff(
                changes,
                "workloadStudentResponsibility",
                oldSupplemental.workloadStudentResponsibility(),
                newSupplemental.workloadStudentResponsibility());

        addFieldDiff(
                changes,
                "creditPoints",
                effectiveCreditPoints(oldSyllabus, oldSupplemental),
                effectiveCreditPoints(newSyllabus, newSupplemental));

        addFieldDiff(
                changes,
                "lectureCredits",
                effectiveLectureCredits(oldSyllabus, oldSupplemental),
                effectiveLectureCredits(newSyllabus, newSupplemental));

        addFieldDiff(
                changes,
                "laboratoryCredits",
                effectiveLaboratoryCredits(oldSyllabus, oldSupplemental),
                effectiveLaboratoryCredits(newSyllabus, newSupplemental));

        return changes;
    }

    private Map<String, SyllabusDiffResponse.FieldDiff> compareRequirements(
            Syllabus oldSyllabus,
            Syllabus newSyllabus) {

        Map<String, SyllabusDiffResponse.FieldDiff> changes =
                new LinkedHashMap<>();

        addFieldDiff(
                changes,
                "prerequisites",
                oldSyllabus.getPrerequisites(),
                newSyllabus.getPrerequisites());

        addFieldDiff(
                changes,
                "objectives",
                oldSyllabus.getObjectives(),
                newSyllabus.getObjectives());

        return changes;
    }

    private Map<String, SyllabusDiffResponse.FieldDiff> compareContentInfo(
            SupplementalNotes oldSupplemental,
            SupplementalNotes newSupplemental) {

        Map<String, SyllabusDiffResponse.FieldDiff> changes =
                new LinkedHashMap<>();

        addFieldDiff(
                changes,
                "contentNote",
                oldSupplemental.contentNote(),
                newSupplemental.contentNote());

        return changes;
    }

    private Map<String, SyllabusDiffResponse.FieldDiff> compareAssessmentInfo(
            SupplementalNotes oldSupplemental,
            SupplementalNotes newSupplemental) {

        Map<String, SyllabusDiffResponse.FieldDiff> changes =
                new LinkedHashMap<>();

        addFieldDiff(
                changes,
                "assessmentPassNote",
                oldSupplemental.assessmentPassNote(),
                newSupplemental.assessmentPassNote());

        return changes;
    }

    private Map<String, SyllabusDiffResponse.FieldDiff> compareExamination(
            Syllabus oldSyllabus,
            Syllabus newSyllabus) {

        Map<String, SyllabusDiffResponse.FieldDiff> changes =
                new LinkedHashMap<>();

        addFieldDiff(
                changes,
                "examForms",
                oldSyllabus.getExamForms(),
                newSyllabus.getExamForms());

        addFieldDiff(
                changes,
                "examRequirements",
                oldSyllabus.getExamRequirements(),
                newSyllabus.getExamRequirements());

        addFieldDiff(
                changes,
                "rubrics",
                oldSyllabus.getRubrics(),
                newSyllabus.getRubrics());

        return changes;
    }

    private Map<String, SyllabusDiffResponse.FieldDiff> compareRevisionInfo(
            Syllabus oldSyllabus,
            Syllabus newSyllabus,
            SupplementalNotes oldSupplemental,
            SupplementalNotes newSupplemental) {

        Map<String, SyllabusDiffResponse.FieldDiff> changes =
                new LinkedHashMap<>();

        addFieldDiff(
                changes,
                "dateRevised",
                oldSupplemental.dateRevised(),
                newSupplemental.dateRevised());

        addFieldDiff(
                changes,
                "internalNotes",
                oldSupplemental.internalNotes(),
                newSupplemental.internalNotes());

        addFieldDiff(
                changes,
                "changeSummary",
                oldSyllabus.getChangeSummary(),
                newSyllabus.getChangeSummary());

        return changes;
    }

    private SyllabusDiffResponse.ListDiff<SyllabusDiffResponse.CloDiff>
            compareClos(
                    MatchResult<Clo> matches,
                    Map<Integer, List<String>> oldPlosByClo,
                    Map<Integer, List<String>> newPlosByClo) {

        return buildListDiff(
                matches,
                clo -> cloSnapshot(
                        clo,
                        newPlosByClo.getOrDefault(clo.getId(), List.of()),
                        null),
                clo -> cloSnapshot(
                        clo,
                        oldPlosByClo.getOrDefault(clo.getId(), List.of()),
                        null),
                (oldClo, newClo) -> {
                    Map<String, SyllabusDiffResponse.FieldDiff> changes
                            = new LinkedHashMap<>();
                    addFieldDiff(changes, "code",
                            oldClo.getCode(), newClo.getCode());
                    addFieldDiff(changes, "description",
                            oldClo.getDescription(), newClo.getDescription());
                    addFieldDiff(changes, "descriptionVn",
                            oldClo.getDescriptionVn(), newClo.getDescriptionVn());
                    addFieldDiff(changes, "competencyLevel",
                            oldClo.getCompetencyLevel(), newClo.getCompetencyLevel());
                    addFieldDiff(changes, "bloomLevel",
                            oldClo.getBloomLevel(), newClo.getBloomLevel());
                    addFieldDiff(changes, "orderIndex",
                            oldClo.getOrderIndex(), newClo.getOrderIndex());

                    List<String> oldPlos = oldPlosByClo
                            .getOrDefault(oldClo.getId(), List.of());
                    List<String> newPlos = newPlosByClo
                            .getOrDefault(newClo.getId(), List.of());
                    addFieldDiff(changes, "plos",
                            String.join(", ", oldPlos),
                            String.join(", ", newPlos));

                    return changes.isEmpty()
                            ? null
                            : cloSnapshot(newClo, newPlos, changes);
                });
    }

    private SyllabusDiffResponse.CloDiff cloSnapshot(
            Clo clo,
            List<String> plos,
            Map<String, SyllabusDiffResponse.FieldDiff> changes) {

        return SyllabusDiffResponse.CloDiff.builder()
                .code(clo.getCode())
                .description(clo.getDescription())
                .descriptionVn(clo.getDescriptionVn())
                .competencyLevel(diffText(clo.getCompetencyLevel()))
                .bloomLevel(diffText(clo.getBloomLevel()))
                .orderIndex(clo.getOrderIndex())
                .plos(plos)
                .changes(changes)
                .build();
    }

    private SyllabusDiffResponse.ListDiff<SyllabusDiffResponse.TopicDiff>
            compareTopics(
                    MatchResult<Topic> matches,
                    Map<Topic, TopicSupplement> oldSupplements,
                    Map<Topic, TopicSupplement> newSupplements) {

        return buildListDiff(
                matches,
                topic -> topicSnapshot(
                        topic,
                        newSupplements.get(topic),
                        null),
                topic -> topicSnapshot(
                        topic,
                        oldSupplements.get(topic),
                        null),
                (oldTopic, newTopic) -> {
                    Map<String, SyllabusDiffResponse.FieldDiff> changes =
                            new LinkedHashMap<>();

                    TopicSupplement oldSupplement =
                            oldSupplements.getOrDefault(
                                    oldTopic,
                                    TopicSupplement.empty());
                    TopicSupplement newSupplement =
                            newSupplements.getOrDefault(
                                    newTopic,
                                    TopicSupplement.empty());

                    addFieldDiff(
                            changes,
                            "weekNumber",
                            oldTopic.getWeekNumber(),
                            newTopic.getWeekNumber());

                    addFieldDiff(
                            changes,
                            "orderInWeek",
                            oldTopic.getOrderInWeek(),
                            newTopic.getOrderInWeek());

                    addFieldDiff(
                            changes,
                            "name",
                            oldTopic.getName(),
                            newTopic.getName());

                    addFieldDiff(
                            changes,
                            "nameVn",
                            oldTopic.getNameVn(),
                            newTopic.getNameVn());

                    addFieldDiff(
                            changes,
                            "teachingHours",
                            oldTopic.getTeachingHours(),
                            newTopic.getTeachingHours());

                    addFieldDiff(
                            changes,
                            "labHours",
                            oldTopic.getLabHours(),
                            newTopic.getLabHours());

                    addFieldDiff(
                            changes,
                            "selfStudyHours",
                            oldTopic.getSelfStudyHours(),
                            newTopic.getSelfStudyHours());

                    addFieldDiff(
                            changes,
                            "topicType",
                            oldTopic.getTopicType(),
                            newTopic.getTopicType());

                    addFieldDiff(
                            changes,
                            "teachingMethod",
                            oldTopic.getTeachingMethod(),
                            newTopic.getTeachingMethod());

                    addFieldDiff(
                            changes,
                            "learningActivity",
                            oldTopic.getLearningActivity(),
                            newTopic.getLearningActivity());

                    addFieldDiff(
                            changes,
                            "assessments",
                            oldSupplement.assessments(),
                            newSupplement.assessments());

                    addFieldDiff(
                            changes,
                            "resources",
                            oldSupplement.resources(),
                            newSupplement.resources());

                    addFieldDiff(
                            changes,
                            "contentWeight",
                            oldSupplement.weight(),
                            newSupplement.weight());

                    addFieldDiff(
                            changes,
                            "contentLevel",
                            oldSupplement.level(),
                            newSupplement.level());

                    /*
                     * Do not compare Topic.notes as a raw string.
                     * Legacy structured contentWeight/contentLevel values are
                     * already interpreted above.
                     */
                    return changes.isEmpty()
                            ? null
                            : topicSnapshot(
                                    newTopic,
                                    newSupplement,
                                    changes);
                });
    }

    private SyllabusDiffResponse.TopicDiff topicSnapshot(
            Topic topic,
            TopicSupplement supplement,
            Map<String, SyllabusDiffResponse.FieldDiff> changes) {

        TopicSupplement effective =
                supplement == null
                        ? TopicSupplement.empty()
                        : supplement;

        return SyllabusDiffResponse.TopicDiff.builder()
                .name(topic.getName())
                .nameVn(topic.getNameVn())
                .weekNumber(topic.getWeekNumber())
                .orderInWeek(topic.getOrderInWeek())
                .teachingHours(topic.getTeachingHours())
                .labHours(topic.getLabHours())
                .selfStudyHours(topic.getSelfStudyHours())
                .topicType(diffText(topic.getTopicType()))
                .teachingMethod(topic.getTeachingMethod())
                .learningActivity(topic.getLearningActivity())
                .assessments(effective.assessments())
                .resources(effective.resources())
                .contentWeight(effective.weight())
                .contentLevel(effective.level())
                .changes(changes)
                .build();
    }

    private SyllabusDiffResponse.ListDiff<SyllabusDiffResponse.AssessmentDiff>
            compareAssessments(MatchResult<AssessmentComponent> matches) {

        return buildListDiff(
                matches,
                assessment -> assessmentSnapshot(assessment, null),
                assessment -> assessmentSnapshot(assessment, null),
                (oldAssessment, newAssessment) -> {
                    Map<String, SyllabusDiffResponse.FieldDiff> changes
                            = new LinkedHashMap<>();
                    addFieldDiff(changes, "name",
                            oldAssessment.getName(), newAssessment.getName());
                    addFieldDiff(changes, "nameVn",
                            oldAssessment.getNameVn(), newAssessment.getNameVn());
                    addFieldDiff(changes, "assessmentType",
                            oldAssessment.getAssessmentType(),
                            newAssessment.getAssessmentType());
                    addFieldDiff(changes, "weightPercent",
                            oldAssessment.getWeightPercent(),
                            newAssessment.getWeightPercent());
                    addFieldDiff(changes, "minScore",
                            oldAssessment.getMinScore(), newAssessment.getMinScore());
                    addFieldDiff(changes, "maxScore",
                            oldAssessment.getMaxScore(), newAssessment.getMaxScore());
                    addFieldDiff(changes, "orderIndex",
                            oldAssessment.getOrderIndex(),
                            newAssessment.getOrderIndex());

                    return changes.isEmpty()
                            ? null
                            : assessmentSnapshot(newAssessment, changes);
                });
    }

    private SyllabusDiffResponse.AssessmentDiff assessmentSnapshot(
            AssessmentComponent assessment,
            Map<String, SyllabusDiffResponse.FieldDiff> changes) {

        return SyllabusDiffResponse.AssessmentDiff.builder()
                .name(assessment.getName())
                .nameVn(assessment.getNameVn())
                .assessmentType(assessment.getAssessmentType())
                .weightPercent(toDouble(assessment.getWeightPercent()))
                .minScore(toDouble(assessment.getMinScore()))
                .maxScore(toDouble(assessment.getMaxScore()))
                .orderIndex(assessment.getOrderIndex())
                .changes(changes)
                .build();
    }

    private SyllabusDiffResponse.ListDiff<SyllabusDiffResponse.CloPloMappingDiff>
            compareCloPloMappings(
                    List<CloPloMapping> oldMappings,
                    List<CloPloMapping> newMappings,
                    IdentityKeys cloIdentities) {

        MatchResult<CloPloMapping> matches = matchItems(
                oldMappings,
                newMappings,
                mapping -> cloPloKey(
                        mapping,
                        cloIdentities.oldKeys()),
                mapping -> cloPloSecondaryKey(
                        mapping,
                        cloIdentities.oldKeys()),
                mapping -> cloPloKey(
                        mapping,
                        cloIdentities.newKeys()),
                mapping -> cloPloSecondaryKey(
                        mapping,
                        cloIdentities.newKeys()));

        return buildListDiff(
                matches,
                mapping -> cloPloSnapshot(mapping, null),
                mapping -> cloPloSnapshot(mapping, null),
                (oldMapping, newMapping) -> {
                    Map<String, SyllabusDiffResponse.FieldDiff> changes
                            = new LinkedHashMap<>();
                    addFieldDiff(changes, "cloCode",
                            oldMapping.getClo().getCode(),
                            newMapping.getClo().getCode());
                    addFieldDiff(changes, "ploCode",
                            oldMapping.getPlo().getCode(),
                            newMapping.getPlo().getCode());
                    addFieldDiff(changes, "level",
                            oldMapping.getLevel(), newMapping.getLevel());
                    addFieldDiff(changes, "contributionWeight",
                            oldMapping.getContributionWeight(),
                            newMapping.getContributionWeight());
                    addFieldDiff(changes, "notes",
                            oldMapping.getNotes(), newMapping.getNotes());

                    return changes.isEmpty()
                            ? null
                            : cloPloSnapshot(newMapping, changes);
                });
    }

    private SyllabusDiffResponse.CloPloMappingDiff cloPloSnapshot(
            CloPloMapping mapping,
            Map<String, SyllabusDiffResponse.FieldDiff> changes) {

        return SyllabusDiffResponse.CloPloMappingDiff.builder()
                .cloCode(mapping.getClo().getCode())
                .ploCode(mapping.getPlo().getCode())
                .level(diffText(mapping.getLevel()))
                .contributionWeight(mapping.getContributionWeight())
                .notes(mapping.getNotes())
                .changes(changes)
                .build();
    }

    private SyllabusDiffResponse.ListDiff<SyllabusDiffResponse.TopicCloMappingDiff>
            compareTopicCloMappings(
                    List<TopicClo> oldMappings,
                    List<TopicClo> newMappings,
                    IdentityKeys topicIdentities,
                    IdentityKeys cloIdentities) {

        MatchResult<TopicClo> matches = matchItems(
                oldMappings,
                newMappings,
                mapping -> topicCloKey(
                        mapping,
                        topicIdentities.oldKeys(),
                        cloIdentities.oldKeys()),
                this::topicCloSecondaryKey,
                mapping -> topicCloKey(
                        mapping,
                        topicIdentities.newKeys(),
                        cloIdentities.newKeys()),
                this::topicCloSecondaryKey);

        return buildListDiff(
                matches,
                mapping -> topicCloSnapshot(mapping, null),
                mapping -> topicCloSnapshot(mapping, null),
                (oldMapping, newMapping) -> {
                    Map<String, SyllabusDiffResponse.FieldDiff> changes
                            = new LinkedHashMap<>();
                    addFieldDiff(changes, "topicName",
                            oldMapping.getTopic().getName(),
                            newMapping.getTopic().getName());
                    addFieldDiff(changes, "weekNumber",
                            oldMapping.getTopic().getWeekNumber(),
                            newMapping.getTopic().getWeekNumber());
                    addFieldDiff(changes, "orderInWeek",
                            oldMapping.getTopic().getOrderInWeek(),
                            newMapping.getTopic().getOrderInWeek());
                    addFieldDiff(changes, "cloCode",
                            oldMapping.getClo().getCode(),
                            newMapping.getClo().getCode());
                    addFieldDiff(changes, "teachingLevel",
                            oldMapping.getTeachingLevel(),
                            newMapping.getTeachingLevel());

                    return changes.isEmpty()
                            ? null
                            : topicCloSnapshot(newMapping, changes);
                });
    }

    private SyllabusDiffResponse.TopicCloMappingDiff topicCloSnapshot(
            TopicClo mapping,
            Map<String, SyllabusDiffResponse.FieldDiff> changes) {

        Topic topic = mapping.getTopic();
        return SyllabusDiffResponse.TopicCloMappingDiff.builder()
                .topicName(topic.getName())
                .weekNumber(topic.getWeekNumber())
                .orderInWeek(topic.getOrderInWeek())
                .cloCode(mapping.getClo().getCode())
                .teachingLevel(diffText(mapping.getTeachingLevel()))
                .changes(changes)
                .build();
    }

    private SyllabusDiffResponse.ListDiff<SyllabusDiffResponse.AssessmentCloMappingDiff>
            compareAssessmentCloMappings(
                    List<AssessmentClo> oldMappings,
                    List<AssessmentClo> newMappings,
                    IdentityKeys assessmentIdentities,
                    IdentityKeys cloIdentities) {

        MatchResult<AssessmentClo> matches = matchItems(
                oldMappings,
                newMappings,
                mapping -> assessmentCloKey(
                        mapping,
                        assessmentIdentities.oldKeys(),
                        cloIdentities.oldKeys()),
                this::assessmentCloSecondaryKey,
                mapping -> assessmentCloKey(
                        mapping,
                        assessmentIdentities.newKeys(),
                        cloIdentities.newKeys()),
                this::assessmentCloSecondaryKey);

        return buildListDiff(
                matches,
                mapping -> assessmentCloSnapshot(mapping, null),
                mapping -> assessmentCloSnapshot(mapping, null),
                (oldMapping, newMapping) -> {
                    Map<String, SyllabusDiffResponse.FieldDiff> changes
                            = new LinkedHashMap<>();
                    addFieldDiff(changes, "assessmentName",
                            oldMapping.getAssessmentComponent().getName(),
                            newMapping.getAssessmentComponent().getName());
                    addFieldDiff(changes, "orderIndex",
                            oldMapping.getAssessmentComponent().getOrderIndex(),
                            newMapping.getAssessmentComponent().getOrderIndex());
                    addFieldDiff(changes, "cloCode",
                            oldMapping.getClo().getCode(),
                            newMapping.getClo().getCode());
                    addFieldDiff(changes, "contributionPercent",
                            oldMapping.getContributionPercent(),
                            newMapping.getContributionPercent());

                    return changes.isEmpty()
                            ? null
                            : assessmentCloSnapshot(newMapping, changes);
                });
    }

    private SyllabusDiffResponse.AssessmentCloMappingDiff assessmentCloSnapshot(
            AssessmentClo mapping,
            Map<String, SyllabusDiffResponse.FieldDiff> changes) {

        AssessmentComponent assessment = mapping.getAssessmentComponent();
        return SyllabusDiffResponse.AssessmentCloMappingDiff.builder()
                .assessmentName(assessment.getName())
                .orderIndex(assessment.getOrderIndex())
                .cloCode(mapping.getClo().getCode())
                .contributionPercent(mapping.getContributionPercent())
                .changes(changes)
                .build();
    }

    private SyllabusDiffResponse.ListDiff<SyllabusDiffResponse.readingsDiff>
            comparereadings(
                    List<SyllabusBook> oldItems,
                    List<SyllabusBook> newItems) {

        MatchResult<SyllabusBook> matches = matchItems(
                oldItems,
                newItems,
                this::readingPrimaryKey,
                this::readingSecondaryKey);

        return buildListDiff(
                matches,
                item -> readingSnapshot(item, null),
                item -> readingSnapshot(item, null),
                (oldItem, newItem) -> {
                    Map<String, SyllabusDiffResponse.FieldDiff> changes
                            = new LinkedHashMap<>();
                    Book oldBook = oldItem.getBook();
                    Book newBook = newItem.getBook();

                    addFieldDiff(changes, "title",
                            oldBook.getTitle(), newBook.getTitle());
                    addFieldDiff(changes, "author",
                            oldBook.getAuthor(), newBook.getAuthor());
                    addFieldDiff(changes, "publisher",
                            oldBook.getPublisher(), newBook.getPublisher());
                    addFieldDiff(changes, "year",
                            oldBook.getYear(), newBook.getYear());
                    addFieldDiff(changes, "edition",
                            oldBook.getEdition(), newBook.getEdition());
                    addFieldDiff(changes, "isbn",
                            oldBook.getIsbn(), newBook.getIsbn());
                    addFieldDiff(changes, "url",
                            oldBook.getUrl(), newBook.getUrl());
                    addFieldDiff(changes, "bookType",
                            oldBook.getBookType(), newBook.getBookType());
                    addFieldDiff(changes, "usageType",
                            oldItem.getUsageType(), newItem.getUsageType());
                    addFieldDiff(changes, "orderIndex",
                            oldItem.getOrderIndex(), newItem.getOrderIndex());

                    return changes.isEmpty()
                            ? null
                            : readingSnapshot(newItem, changes);
                });
    }

    private SyllabusDiffResponse.readingsDiff readingSnapshot(
            SyllabusBook item,
            Map<String, SyllabusDiffResponse.FieldDiff> changes) {

        Book book = item.getBook();
        return SyllabusDiffResponse.readingsDiff.builder()
                .bookId(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .publisher(book.getPublisher())
                .year(book.getYear())
                .edition(book.getEdition())
                .isbn(book.getIsbn())
                .url(book.getUrl())
                .bookType(diffText(book.getBookType()))
                .usageType(diffText(item.getUsageType()))
                .orderIndex(item.getOrderIndex())
                .changes(changes)
                .build();
    }


    private Map<Topic, TopicSupplement> topicSupplements(
            List<Topic> topics,
            SupplementalNotes supplemental) {

        Map<Topic, TopicSupplement> result = new LinkedHashMap<>();

        for (int index = 0; index < topics.size(); index++) {
            Topic topic = topics.get(index);

            TopicSupplement stored =
                    supplemental.topicDetails().get(String.valueOf(index));

            TopicSupplement legacy =
                    parseTopicContentNotes(topic.getNotes());

            String clo =
                    stored == null
                            ? ""
                            : nullToEmpty(stored.clo());

            /*
             * Frontend semantics use nullish fallback for assessments/resources:
             * when a stored topicDetails row exists, its value is authoritative
             * even when the value is an empty string.
             */
            String assessments =
                    stored == null
                            ? nullToEmpty(topic.getAssessments())
                            : nullToEmpty(stored.assessments());

            String resources =
                    stored == null
                            ? nullToEmpty(topic.getResources())
                            : nullToEmpty(stored.resources());

            /*
             * Weight and level use truthy fallback in the form. Stored non-empty
             * values win; otherwise legacy Topic.notes values may supply them.
             */
            String level = firstNonEmpty(
                    stored == null ? null : stored.level(),
                    legacy.level());

            String weight = firstNonEmpty(
                    stored == null ? null : stored.weight(),
                    legacy.weight());

            result.put(
                    topic,
                    new TopicSupplement(
                            clo,
                            assessments,
                            resources,
                            nullToEmpty(level),
                            nullToEmpty(weight)));
        }

        return result;
    }

    private SyllabusDiffResponse.ListDiff<SyllabusDiffResponse.PlannedActivityDiff>
            comparePlannedActivities(
                    List<PlannedActivity> oldItems,
                    List<PlannedActivity> newItems) {

        MatchResult<PlannedActivity> matches = matchItems(
                oldItems,
                newItems,
                this::plannedActivityTopicKey,
                this::plannedActivityWeekFallbackKey);

        return buildListDiff(
                matches,
                item -> plannedActivitySnapshot(item, null),
                item -> plannedActivitySnapshot(item, null),
                (oldItem, newItem) -> {
                    Map<String, SyllabusDiffResponse.FieldDiff> changes =
                            new LinkedHashMap<>();

                    addFieldDiff(
                            changes,
                            "week",
                            oldItem.week(),
                            newItem.week());

                    addFieldDiff(
                            changes,
                            "topic",
                            oldItem.topic(),
                            newItem.topic());

                    addFieldDiff(
                            changes,
                            "clo",
                            oldItem.clo(),
                            newItem.clo());

                    addFieldDiff(
                            changes,
                            "assessments",
                            oldItem.assessments(),
                            newItem.assessments());

                    addFieldDiff(
                            changes,
                            "learningActivities",
                            oldItem.learningActivities(),
                            newItem.learningActivities());

                    addFieldDiff(
                            changes,
                            "resources",
                            oldItem.resources(),
                            newItem.resources());

                    return changes.isEmpty()
                            ? null
                            : plannedActivitySnapshot(newItem, changes);
                });
    }

    private SyllabusDiffResponse.PlannedActivityDiff plannedActivitySnapshot(
            PlannedActivity activity,
            Map<String, SyllabusDiffResponse.FieldDiff> changes) {

        return SyllabusDiffResponse.PlannedActivityDiff.builder()
                .week(activity.week())
                .topic(activity.topic())
                .clo(activity.clo())
                .assessments(activity.assessments())
                .learningActivities(activity.learningActivities())
                .resources(activity.resources())
                .changes(changes)
                .build();
    }

    private List<PlannedActivity> effectivePlannedActivities(
            List<Topic> topics,
            SupplementalNotes supplemental,
            Map<Topic, TopicSupplement> topicSupplements) {

        if (!supplemental.plannedActivities().isEmpty()) {
            return supplemental.plannedActivities();
        }

        List<PlannedActivity> result = new ArrayList<>();

        for (int index = 0; index < topics.size(); index++) {
            Topic topic = topics.get(index);
            TopicSupplement detail =
                    topicSupplements.getOrDefault(
                            topic,
                            TopicSupplement.empty());

            Integer week =
                    topic.getWeekNumber() == null
                            ? index + 1
                            : topic.getWeekNumber();

            result.add(
                    new PlannedActivity(
                            week,
                            nullToEmpty(topic.getName()),
                            nullToEmpty(detail.clo()),
                            nullToEmpty(detail.assessments()),
                            mergeSelectedValues(
                                    topic.getTeachingMethod(),
                                    topic.getLearningActivity()),
                            nullToEmpty(detail.resources())));
        }

        return result;
    }

    private String plannedActivityTopicKey(PlannedActivity activity) {
        String topic = normalizedIdentityText(activity.topic());
        return topic == null ? null : "topic:" + topic;
    }

    private String plannedActivityWeekFallbackKey(PlannedActivity activity) {
        /*
         * A named planned-activity row is never matched by week alone.
         * This avoids pairing unrelated activities that happen to occupy
         * the same week after a curriculum redesign.
         */
        if (plannedActivityTopicKey(activity) != null) {
            return null;
        }

        String week = numberKey(activity.week());
        return week == null ? null : "week:" + week;
    }

    private SupplementalNotes parseSupplementalNotes(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return SupplementalNotes.empty();
        }

        try {
            JsonNode parsed = NOTES_MAPPER.readTree(raw);

            /*
             * Frontend asRecord(JSON.parse(raw)) turns non-object JSON into an
             * empty record. Mirror that behavior here.
             */
            if (parsed == null || !parsed.isObject()) {
                return SupplementalNotes.empty();
            }

            Map<String, TopicSupplement> topicDetails =
                    parseTopicDetails(parsed.get("topicDetails"));

            List<PlannedActivity> plannedActivities =
                    parsePlannedActivities(parsed.get("plannedActivities"));

            return new SupplementalNotes(
                    jsonText(parsed, "internalNotes"),
                    firstNonEmpty(
                            jsonText(parsed, "personResponsible"),
                            jsonText(parsed, "instructor")),
                    jsonText(parsed, "dateRevised"),
                    firstNonEmpty(
                            jsonText(parsed, "creditPoints"),
                            jsonText(parsed, "ects")),
                    firstNonEmpty(
                            jsonText(parsed, "lectureCredits"),
                            jsonText(parsed, "creditsTheory")),
                    firstNonEmpty(
                            jsonText(parsed, "laboratoryCredits"),
                            jsonText(parsed, "creditsPractice")),
                    jsonText(parsed, "workloadStudentResponsibility"),
                    topicDetails,
                    plannedActivities,
                    defaultIfEmpty(
                            jsonText(parsed, "assessmentPassNote"),
                            DEFAULT_ASSESSMENT_PASS_NOTE),
                    defaultIfEmpty(
                            jsonText(parsed, "contentNote"),
                            DEFAULT_CONTENT_NOTE));
        } catch (Exception ignored) {
            /*
             * Frontend fallback for malformed/legacy plain-text notes:
             * keep the raw text as Internal Notes while all structured fields
             * receive the standard defaults.
             */
            return SupplementalNotes.malformed(raw);
        }
    }

    private Map<String, TopicSupplement> parseTopicDetails(JsonNode node) {
        Map<String, TopicSupplement> result = new LinkedHashMap<>();

        if (node == null || !node.isObject()) {
            return result;
        }

        node.fields().forEachRemaining(entry -> {
            JsonNode detail = entry.getValue();

            if (detail == null || !detail.isObject()) {
                result.put(entry.getKey(), TopicSupplement.empty());
                return;
            }

            result.put(
                    entry.getKey(),
                    new TopicSupplement(
                            jsonText(detail, "clo"),
                            jsonText(detail, "assessments"),
                            jsonText(detail, "resources"),
                            jsonText(detail, "level"),
                            jsonText(detail, "weight")));
        });

        return result;
    }

    private List<PlannedActivity> parsePlannedActivities(JsonNode node) {
        List<PlannedActivity> result = new ArrayList<>();

        if (node == null || !node.isArray()) {
            return result;
        }

        for (int index = 0; index < node.size(); index++) {
            JsonNode item = node.get(index);

            if (item == null || !item.isObject()) {
                /*
                 * Frontend maps every array item through asRecord(). Preserve
                 * the row and use index + 1 as its fallback week.
                 */
                result.add(
                        new PlannedActivity(
                                index + 1,
                                "",
                                "",
                                "",
                                "",
                                ""));
                continue;
            }

            result.add(
                    new PlannedActivity(
                            activityWeek(item.get("week"), index + 1),
                            jsonText(item, "topic"),
                            jsonText(item, "clo"),
                            jsonText(item, "assessments"),
                            firstNonEmpty(
                                    jsonText(item, "learningActivities"),
                                    jsonText(item, "activities")),
                            jsonText(item, "resources")));
        }

        return result;
    }

    private TopicSupplement parseTopicContentNotes(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return TopicSupplement.empty();
        }

        try {
            JsonNode parsed = NOTES_MAPPER.readTree(raw);

            if (parsed == null || !parsed.isObject()) {
                return TopicSupplement.empty();
            }

            return new TopicSupplement(
                    "",
                    "",
                    "",
                    firstNonEmpty(
                            jsonText(parsed, "contentLevel"),
                            jsonText(parsed, "teachingLevel"),
                            jsonText(parsed, "level")),
                    firstNonEmpty(
                            jsonText(parsed, "contentWeight"),
                            jsonText(parsed, "weight")));
        } catch (Exception ignored) {
            return TopicSupplement.empty();
        }
    }

    private int activityWeek(JsonNode value, int fallback) {
        if (value == null || value.isNull()) {
            return fallback;
        }

        int parsed;

        if (value.isNumber()) {
            parsed = value.asInt();
        } else if (value.isTextual()) {
            try {
                parsed = Integer.parseInt(value.asText().trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        } else {
            return fallback;
        }

        /*
         * JavaScript Number(value) || fallback treats zero as falsy but keeps
         * any other integer, including a negative legacy value.
         */
        return parsed == 0 ? fallback : parsed;
    }

    private String jsonText(JsonNode object, String fieldName) {
        if (object == null || !object.isObject()) {
            return "";
        }

        JsonNode value = object.get(fieldName);

        /*
         * Frontend asText() accepts only actual JSON strings.
         */
        return value != null && value.isTextual()
                ? value.asText()
                : "";
    }

    private String effectiveCreditPoints(
            Syllabus syllabus,
            SupplementalNotes supplemental) {

        if (isNonEmpty(supplemental.creditPoints())) {
            return supplemental.creditPoints();
        }

        if (syllabus.getCourse() == null) {
            return "";
        }

        int lecture =
                syllabus.getCourse().getCreditTheory() == null
                        ? 0
                        : syllabus.getCourse().getCreditTheory();

        int laboratory =
                syllabus.getCourse().getCreditLab() == null
                        ? 0
                        : syllabus.getCourse().getCreditLab();

        return String.valueOf(lecture + laboratory);
    }

    private String effectiveLectureCredits(
            Syllabus syllabus,
            SupplementalNotes supplemental) {

        if (isNonEmpty(supplemental.lectureCredits())) {
            return supplemental.lectureCredits();
        }

        if (syllabus.getCourse() == null) {
            return "";
        }

        Integer value = syllabus.getCourse().getCreditTheory();
        return String.valueOf(value == null ? 0 : value);
    }

    private String effectiveLaboratoryCredits(
            Syllabus syllabus,
            SupplementalNotes supplemental) {

        if (isNonEmpty(supplemental.laboratoryCredits())) {
            return supplemental.laboratoryCredits();
        }

        if (syllabus.getCourse() == null) {
            return "";
        }

        Integer value = syllabus.getCourse().getCreditLab();
        return String.valueOf(value == null ? 0 : value);
    }

    private String courseCodeForDiff(Syllabus syllabus) {
        return firstNonEmpty(
                syllabus.getCourseCodeSnapshot(),
                syllabus.getCourse() == null
                        ? null
                        : syllabus.getCourse().getCourseCode());
    }

    private String courseNameForDiff(Syllabus syllabus) {
        return firstNonEmpty(
                syllabus.getCourseNameSnapshot(),
                syllabus.getCourse() == null
                        ? null
                        : syllabus.getCourse().getName());
    }

    private String courseNameVnForDiff(Syllabus syllabus) {
        return syllabus.getCourse() == null
                ? ""
                : nullToEmpty(syllabus.getCourse().getNameVn());
    }

    private Double toDouble(Number value) {
        return value == null ? null : value.doubleValue();
    }

    private String mergeSelectedValues(String... values) {
        LinkedHashSet<String> selected = new LinkedHashSet<>();

        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (value == null || value.isEmpty()) {
                continue;
            }

            String[] parts = value.split("[,;\\n]+");

            for (String part : parts) {
                String trimmed = part.trim();

                if (!trimmed.isEmpty()) {
                    selected.add(trimmed);
                }
            }
        }

        return String.join(", ", selected);
    }

    private String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (isNonEmpty(value)) {
                return value;
            }
        }

        return "";
    }

    private String defaultIfEmpty(String value, String defaultValue) {
        return isNonEmpty(value) ? value : defaultValue;
    }

    private boolean isNonEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record SupplementalNotes(
            String internalNotes,
            String personResponsible,
            String dateRevised,
            String creditPoints,
            String lectureCredits,
            String laboratoryCredits,
            String workloadStudentResponsibility,
            Map<String, TopicSupplement> topicDetails,
            List<PlannedActivity> plannedActivities,
            String assessmentPassNote,
            String contentNote) {

        private static SupplementalNotes empty() {
            return new SupplementalNotes(
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    new LinkedHashMap<>(),
                    new ArrayList<>(),
                    DEFAULT_ASSESSMENT_PASS_NOTE,
                    DEFAULT_CONTENT_NOTE);
        }

        private static SupplementalNotes malformed(String raw) {
            SupplementalNotes defaults = empty();

            return new SupplementalNotes(
                    raw == null ? "" : raw,
                    defaults.personResponsible(),
                    defaults.dateRevised(),
                    defaults.creditPoints(),
                    defaults.lectureCredits(),
                    defaults.laboratoryCredits(),
                    defaults.workloadStudentResponsibility(),
                    defaults.topicDetails(),
                    defaults.plannedActivities(),
                    defaults.assessmentPassNote(),
                    defaults.contentNote());
        }
    }

    private record TopicSupplement(
            String clo,
            String assessments,
            String resources,
            String level,
            String weight) {

        private static TopicSupplement empty() {
            return new TopicSupplement("", "", "", "", "");
        }
    }

    private record PlannedActivity(
            Integer week,
            String topic,
            String clo,
            String assessments,
            String learningActivities,
            String resources) {
    }

    private Map<Integer, List<String>> plosByClo(
            List<CloPloMapping> mappings) {

        Map<Integer, List<String>> result = new HashMap<>();
        for (CloPloMapping mapping : mappings) {
            result.computeIfAbsent(
                    mapping.getClo().getId(),
                    ignored -> new ArrayList<>())
                    .add(mapping.getPlo().getCode());
        }
        result.values().forEach(values -> values.sort(String::compareTo));
        return result;
    }

    private String cloPloKey(
            CloPloMapping mapping,
            Map<Integer, String> cloKeys) {

        return compositeKey(
                cloKeys.get(mapping.getClo().getId()),
                "plo-code:" + normalizedKey(mapping.getPlo().getCode()));
    }

    private String cloPloSecondaryKey(
            CloPloMapping mapping,
            Map<Integer, String> cloKeys) {

        String ploIdentity = mapping.getPlo().getId() == null
                ? normalizedKey(mapping.getPlo().getCode())
                : "plo-id:" + mapping.getPlo().getId();
        return compositeKey(
                cloKeys.get(mapping.getClo().getId()),
                ploIdentity);
    }

    private String topicCloKey(
            TopicClo mapping,
            Map<Integer, String> topicKeys,
            Map<Integer, String> cloKeys) {

        return compositeKey(
                topicKeys.get(mapping.getTopic().getId()),
                cloKeys.get(mapping.getClo().getId()));
    }

    private String topicCloSecondaryKey(
            TopicClo mapping) {

        return compositeKey(
                topicNameKey(mapping.getTopic()),
                normalizedKey(mapping.getClo().getCode()));
    }

    private String assessmentCloKey(
            AssessmentClo mapping,
            Map<Integer, String> assessmentKeys,
            Map<Integer, String> cloKeys) {

        return compositeKey(
                assessmentKeys.get(
                        mapping.getAssessmentComponent().getId()),
                cloKeys.get(mapping.getClo().getId()));
    }

    private String assessmentCloSecondaryKey(
            AssessmentClo mapping) {

        return compositeKey(
                normalizedKey(mapping.getAssessmentComponent().getName()),
                normalizedKey(mapping.getClo().getCode()));
    }

    private String topicPositionKey(Topic topic) {
        if (topic.getWeekNumber() == null
                || topic.getOrderInWeek() == null) {
            return null;
        }
        return "week:" + topic.getWeekNumber()
                + "|order:" + topic.getOrderInWeek();
    }

  private String topicNameKey(Topic topic) {
    if (topic == null) {
        return null;
    }

    /*
     * English/canonical name is preferred.
     * Vietnamese name is a fallback when the canonical name is absent.
     *
     * Do not combine both names into one key because changing only the
     * Vietnamese translation must not make the whole topic look like
     * a completely different topic.
     */
    String name = normalizedIdentityText(topic.getName());
    if (name != null) {
        return "name:" + name;
    }

    String nameVn = normalizedIdentityText(topic.getNameVn());
    return nameVn == null
            ? null
            : "name-vn:" + nameVn;
}

private String topicPositionFallbackKey(Topic topic) {
    /*
     * Named topics are NEVER matched by position.
     *
     * If two named topics differ, matching them only because they
     * occupy the same week/order would create a false modification.
     */
    if (topicNameKey(topic) != null) {
        return null;
    }

    return topicPositionKey(topic);
}

private String topicIdentityKey(Topic topic) {
    String nameKey = topicNameKey(topic);

    if (nameKey != null) {
        return nameKey;
    }

    return topicPositionKey(topic);
}

    private String readingPrimaryKey(SyllabusBook item) {
        Book book = item.getBook();
        if (book.getId() != null) {
            return "book-id:" + book.getId();
        }
        String isbn = normalizedKey(book.getIsbn());
        return isbn == null ? null : "isbn:" + isbn;
    }
    private String assessmentNameKey(
        AssessmentComponent assessment) {

    if (assessment == null) {
        return null;
    }

    String name =
            normalizedIdentityText(
                    assessment.getName());

    if (name != null) {
        return "name:" + name;
    }

    String nameVn =
            normalizedIdentityText(
                    assessment.getNameVn());

    return nameVn == null
            ? null
            : "name-vn:" + nameVn;
}

private String assessmentSecondaryKey(
        AssessmentComponent assessment) {

    if (assessment == null) {
        return null;
    }

    /*
     * A unique type can identify an assessment whose wording/name
     * changed between cohorts.
     *
     * matchPhase() already guarantees that the key occurs exactly
     * once on BOTH sides before pairing it.
     */
    String type =
            normalizedIdentityText(
                    assessment.getAssessmentType());

    if (type != null) {
        return "type:" + type;
    }

    /*
     * Last-resort position matching is allowed only when the legacy
     * assessment has neither a usable name nor a type.
     *
     * A named assessment must never be matched by orderIndex alone.
     */
    if (assessmentNameKey(assessment) != null) {
        return null;
    }

    String order =
            numberKey(
                    assessment.getOrderIndex());

    return order == null
            ? null
            : "order:" + order;
}

private String assessmentIdentityKey(
        AssessmentComponent assessment) {

    String nameKey =
            assessmentNameKey(assessment);

    if (nameKey != null) {
        return nameKey;
    }

    String secondaryKey =
            assessmentSecondaryKey(
                    assessment);

    if (secondaryKey != null) {
        return secondaryKey;
    }

    return null;
}

    private String readingSecondaryKey(SyllabusBook item) {
        Book book = item.getBook();
        return compositeKey(
                normalizedKey(book.getTitle()),
                normalizedKey(book.getAuthor()),
                numberKey(book.getYear()));
    }


    private <E> IdentityKeys buildIdentityKeys(
            MatchResult<E> matches,
            Function<E, Integer> idExtractor,
            Function<E, String> fallbackKey) {

        Map<Integer, String> oldKeys = new HashMap<>();
        Map<Integer, String> newKeys = new HashMap<>();

        int matchedIndex = 0;
        for (ItemPair<E> pair : matches.pairs()) {
            String key = "matched:" + matchedIndex++;
            putIdentity(oldKeys, idExtractor.apply(pair.oldItem()), key);
            putIdentity(newKeys, idExtractor.apply(pair.newItem()), key);
        }

        int removedIndex = 0;
        for (E item : matches.removed()) {
            String key = "old-only:"
                    + fallbackOrIndex(fallbackKey.apply(item), removedIndex++);
            putIdentity(oldKeys, idExtractor.apply(item), key);
        }

        int addedIndex = 0;
        for (E item : matches.added()) {
            String key = "new-only:"
                    + fallbackOrIndex(fallbackKey.apply(item), addedIndex++);
            putIdentity(newKeys, idExtractor.apply(item), key);
        }

        return new IdentityKeys(oldKeys, newKeys);
    }

    private void putIdentity(
            Map<Integer, String> target,
            Integer id,
            String key) {
        if (id != null) {
            target.put(id, key);
        }
    }

    private String fallbackOrIndex(String fallback, int index) {
        return fallback == null ? String.valueOf(index) : fallback + ":" + index;
    }

    private <E, D> SyllabusDiffResponse.ListDiff<D> buildListDiff(
            MatchResult<E> matches,
            Function<E, D> addedMapper,
            Function<E, D> removedMapper,
            BiFunction<E, E, D> modifiedMapper) {

        List<D> added = matches.added().stream()
                .map(addedMapper)
                .toList();
        List<D> removed = matches.removed().stream()
                .map(removedMapper)
                .toList();
        List<D> modified = matches.pairs().stream()
                .map(pair -> modifiedMapper.apply(
                        pair.oldItem(), pair.newItem()))
                .filter(Objects::nonNull)
                .toList();

        return SyllabusDiffResponse.ListDiff.<D>builder()
                .added(added)
                .removed(removed)
                .modified(modified)
                .build();
    }

    private <E> MatchResult<E> matchItems(
            List<E> oldItems,
            List<E> newItems,
            Function<E, String> primaryKey,
            Function<E, String> secondaryKey) {

        return matchItems(
                oldItems,
                newItems,
                primaryKey,
                secondaryKey,
                primaryKey,
                secondaryKey);
    }

    private <E> MatchResult<E> matchItems(
            List<E> oldItems,
            List<E> newItems,
            Function<E, String> oldPrimaryKey,
            Function<E, String> oldSecondaryKey,
            Function<E, String> newPrimaryKey,
            Function<E, String> newSecondaryKey) {

        int[] oldIndexForNew = new int[newItems.size()];
        Arrays.fill(oldIndexForNew, -1);
        boolean[] oldMatched = new boolean[oldItems.size()];

        matchPhase(
                oldItems,
                newItems,
                oldMatched,
                oldIndexForNew,
                oldPrimaryKey,
                newPrimaryKey);
        matchPhase(
                oldItems,
                newItems,
                oldMatched,
                oldIndexForNew,
                oldSecondaryKey,
                newSecondaryKey);

        List<ItemPair<E>> pairs = new ArrayList<>();
        List<E> added = new ArrayList<>();
        for (int newIndex = 0; newIndex < newItems.size(); newIndex++) {
            int oldIndex = oldIndexForNew[newIndex];
            if (oldIndex >= 0) {
                pairs.add(new ItemPair<>(
                        oldItems.get(oldIndex),
                        newItems.get(newIndex)));
            } else {
                added.add(newItems.get(newIndex));
            }
        }

        List<E> removed = new ArrayList<>();
        for (int oldIndex = 0; oldIndex < oldItems.size(); oldIndex++) {
            if (!oldMatched[oldIndex]) {
                removed.add(oldItems.get(oldIndex));
            }
        }

        return new MatchResult<>(pairs, added, removed);
    }

    private <E> void matchPhase(
            List<E> oldItems,
            List<E> newItems,
            boolean[] oldMatched,
            int[] oldIndexForNew,
            Function<E, String> oldKeyFunction,
            Function<E, String> newKeyFunction) {

        if (oldKeyFunction == null || newKeyFunction == null) {
            return;
        }

        Map<String, List<Integer>> oldIndexesByKey = new HashMap<>();
        Map<String, List<Integer>> newIndexesByKey = new HashMap<>();

        for (int i = 0; i < oldItems.size(); i++) {
            if (oldMatched[i]) {
                continue;
            }
            String key = cleanKey(oldKeyFunction.apply(oldItems.get(i)));
            if (key != null) {
                oldIndexesByKey.computeIfAbsent(
                        key, ignored -> new ArrayList<>()).add(i);
            }
        }

        for (int i = 0; i < newItems.size(); i++) {
            if (oldIndexForNew[i] >= 0) {
                continue;
            }
            String key = cleanKey(newKeyFunction.apply(newItems.get(i)));
            if (key != null) {
                newIndexesByKey.computeIfAbsent(
                        key, ignored -> new ArrayList<>()).add(i);
            }
        }

        for (Map.Entry<String, List<Integer>> entry
                : oldIndexesByKey.entrySet()) {
            List<Integer> oldIndexes = entry.getValue();
            List<Integer> newIndexes = newIndexesByKey.get(entry.getKey());

            // Chỉ ghép khi key là duy nhất ở cả hai phía để tránh ghép sai.
            if (oldIndexes.size() == 1
                    && newIndexes != null
                    && newIndexes.size() == 1) {
                int oldIndex = oldIndexes.get(0);
                int newIndex = newIndexes.get(0);
                oldMatched[oldIndex] = true;
                oldIndexForNew[newIndex] = oldIndex;
            }
        }
    }

    private void addFieldDiff(
        Map<String, SyllabusDiffResponse.FieldDiff> changes,
        String fieldName,
        Object oldValue,
        Object newValue) {

    Object comparableOld =
            comparableValue(fieldName, oldValue);

    Object comparableNew =
            comparableValue(fieldName, newValue);

    if (!Objects.equals(comparableOld, comparableNew)) {
        changes.put(
                fieldName,
                SyllabusDiffResponse.FieldDiff.builder()
                        .oldValue(diffText(oldValue))
                        .newValue(diffText(newValue))
                        .build());
    }
}

private Object comparableValue(
        String fieldName,
        Object value) {

    if (!(value instanceof String text)) {
        return value;
    }

    String normalized =
            Normalizer.normalize(
                            text,
                            Normalizer.Form.NFC)
                    .replace('\u00A0', ' ')
                    .replace('\u2007', ' ')
                    .replace('\u202F', ' ')
                    .replaceAll("[\\s\\p{Z}]+", " ")
                    .trim();

    if (normalized.isEmpty()) {
        return null;
    }

    return switch (fieldName) {

    case "semester" ->
            normalizeSemesterComparable(normalized);

    case "courseTypes",
         "contentLevel" ->
            normalizeUnorderedListComparable(normalized);

    case "creditPoints",
         "lectureCredits",
         "laboratoryCredits" ->
            normalizeDecimalComparable(normalized);

    case "contentWeight" ->
            normalizePercentageComparable(normalized);

    case "workloadTotal",
         "workloadContact",
         "workloadPrivate" ->
            normalizeWorkloadComparable(normalized);

    case "assessmentType" ->
            normalized.toLowerCase(Locale.ROOT);

    default ->
            normalized;
};
}

private String normalizeSemesterComparable(
        String value) {

    return value
            .replaceFirst(
                    "(?i)^semester\\s*[:\\-]?\\s*",
                    "")
            .trim()
            .toLowerCase(Locale.ROOT);
}

private List<String> normalizeUnorderedListComparable(
        String value) {

    return Arrays.stream(
                    value.split("[,;|]"))
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .map(item ->
                    item.toLowerCase(Locale.ROOT))
            .distinct()
            .sorted()
            .toList();
}
private Object normalizePercentageComparable(
        String value) {

    String candidate =
            value.trim()
                    .replace("%", "")
                    .replace(',', '.')
                    .trim();

    try {
        return new BigDecimal(candidate)
                .stripTrailingZeros();
    } catch (NumberFormatException exception) {
        /*
         * Preserve non-numeric academic content instead of
         * guessing or silently discarding information.
         */
        return value;
    }
}

private Object normalizeDecimalComparable(
        String value) {

    String candidate =
            value.trim()
                    .replace(',', '.');

    try {
        return new BigDecimal(candidate)
                .stripTrailingZeros();
    } catch (NumberFormatException exception) {
        /*
         * Do not guess when a template contains non-numeric
         * academic content. Fall back to normalized source text.
         */
        return value;
    }
}

private String normalizeWorkloadComparable(
        String value) {

    String normalized =
            value.toLowerCase(Locale.ROOT)
                    .replace('(', ' ')
                    .replace(')', ' ')
                    .replace('[', ' ')
                    .replace(']', ' ')
                    .replace('{', ' ')
                    .replace('}', ' ')
                    .replaceAll(
                            "(?i)\\b(?:hours?|hrs?|hr|h)\\b",
                            " ")
                    .replaceAll("\\s+", " ")
                    .trim();

    Matcher matcher =
            WORKLOAD_NUMBER_PATTERN.matcher(normalized);

    StringBuffer result =
            new StringBuffer();

    while (matcher.find()) {

        String rawNumber =
                matcher.group()
                        .replace(',', '.');

        String canonicalNumber;

        try {
            canonicalNumber =
                    new BigDecimal(rawNumber)
                            .stripTrailingZeros()
                            .toPlainString();
        } catch (NumberFormatException exception) {
            canonicalNumber = rawNumber;
        }

        matcher.appendReplacement(
                result,
                Matcher.quoteReplacement(canonicalNumber));
    }

    matcher.appendTail(result);

    return result.toString()
            .replaceAll("\\s*\\+\\s*", " + ")
            .replaceAll("\\s+", " ")
            .trim();
}

private String diffText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
private String normalizedIdentityText(
        Object value) {

    if (value == null) {
        return null;
    }

    String text =
            String.valueOf(value)
                    .trim()
                    .replaceAll("\\s+", " ")
                    .toLowerCase(Locale.ROOT);

    return text.isEmpty()
            ? null
            : text;
}
    private String normalizedKey(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text.toLowerCase(Locale.ROOT);
    }

    private String numberKey(Number value) {
        return value == null ? null : String.valueOf(value);
    }

    private String cleanKey(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String compositeKey(String... parts) {
        if (parts == null || Arrays.stream(parts).allMatch(Objects::isNull)) {
            return null;
        }
        return Arrays.stream(parts)
                .map(part -> part == null ? "<null>" : part)
                .reduce((left, right) -> left + "|" + right)
                .orElse(null);
    }

    private boolean hasEntries(SyllabusDiffResponse.ListDiff<?> diff) {
        return diff != null
                && ((!diff.getAdded().isEmpty())
                || (!diff.getRemoved().isEmpty())
                || (!diff.getModified().isEmpty()));
    }

    private record ItemPair<E>(E oldItem, E newItem) {
    }

    private record MatchResult<E>(
            List<ItemPair<E>> pairs,
            List<E> added,
            List<E> removed) {
    }

    private record IdentityKeys(
            Map<Integer, String> oldKeys,
            Map<Integer, String> newKeys) {
    }
}
