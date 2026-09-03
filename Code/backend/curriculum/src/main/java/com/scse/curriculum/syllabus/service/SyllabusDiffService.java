package com.scse.curriculum.syllabus.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.RequiredArgsConstructor;

/**
 * FR-03.7: tạo diff đầy đủ giữa hai version của cùng một syllabus.
 *
 * Dịch vụ này chỉ chịu trách nhiệm so sánh nội dung. Kiểm tra quyền truy cập
 * và việc hai version có cùng course được thực hiện tại SyllabusServiceImpl.
 */
@Service
@RequiredArgsConstructor
public class SyllabusDiffService {

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

        Map<String, SyllabusDiffResponse.FieldDiff> generalInfoDiff
                = compareGeneralInfo(oldSyllabus, newSyllabus);

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

        List<CloPloMapping> oldCloPloMappings
                = cloPloMappingRepository.findByClo_Syllabus_Id(oldId);
        List<CloPloMapping> newCloPloMappings
                = cloPloMappingRepository.findByClo_Syllabus_Id(newId);

        Map<Integer, List<String>> oldPlosByClo
                = plosByClo(oldCloPloMappings);
        Map<Integer, List<String>> newPlosByClo
                = plosByClo(newCloPloMappings);

        SyllabusDiffResponse.ListDiff<SyllabusDiffResponse.CloDiff> cloDiff
                = compareClos(cloMatches, oldPlosByClo, newPlosByClo);

        List<Topic> oldTopics = topicRepository.findBySyllabusId(oldId);
        List<Topic> newTopics = topicRepository.findBySyllabusId(newId);
        MatchResult<Topic> topicMatches = matchItems(
                oldTopics,
                newTopics,
                this::topicPositionKey,
                this::topicNameKey);
        IdentityKeys topicIdentities = buildIdentityKeys(
                topicMatches,
                Topic::getId,
                this::topicPositionKey);
        SyllabusDiffResponse.ListDiff<SyllabusDiffResponse.TopicDiff> topicDiff
                = compareTopics(topicMatches);

        List<AssessmentComponent> oldAssessments
                = assessmentComponentRepository.findBySyllabusId(oldId);
        List<AssessmentComponent> newAssessments
                = assessmentComponentRepository.findBySyllabusId(newId);
        MatchResult<AssessmentComponent> assessmentMatches = matchItems(
                oldAssessments,
                newAssessments,
                assessment -> numberKey(assessment.getOrderIndex()),
                assessment -> normalizedKey(assessment.getName()));
        IdentityKeys assessmentIdentities = buildIdentityKeys(
                assessmentMatches,
                AssessmentComponent::getId,
                assessment -> numberKey(assessment.getOrderIndex()));
        SyllabusDiffResponse.ListDiff<SyllabusDiffResponse.AssessmentDiff>
                assessmentDiff = compareAssessments(assessmentMatches);

        SyllabusDiffResponse.ListDiff<SyllabusDiffResponse.CloPloMappingDiff>
                cloPloDiff = compareCloPloMappings(
                        oldCloPloMappings,
                        newCloPloMappings,
                        cloIdentities);

        SyllabusDiffResponse.ListDiff<SyllabusDiffResponse.TopicCloMappingDiff>
                topicCloDiff = compareTopicCloMappings(
                        topicCloRepository.findByTopic_Syllabus_Id(oldId),
                        topicCloRepository.findByTopic_Syllabus_Id(newId),
                        topicIdentities,
                        cloIdentities);

        SyllabusDiffResponse.ListDiff<SyllabusDiffResponse.AssessmentCloMappingDiff>
                assessmentCloDiff = compareAssessmentCloMappings(
                        assessmentCloRepository
                                .findByAssessmentComponent_Syllabus_Id(oldId),
                        assessmentCloRepository
                                .findByAssessmentComponent_Syllabus_Id(newId),
                        assessmentIdentities,
                        cloIdentities);

        SyllabusDiffResponse.ListDiff<SyllabusDiffResponse.readingsDiff>
                readingsDiff = comparereadings(
                        syllabusBookRepository.findBySyllabus_Id(oldId),
                        syllabusBookRepository.findBySyllabus_Id(newId));

        boolean hasChanges = !generalInfoDiff.isEmpty()
                || hasEntries(cloDiff)
                || hasEntries(cloPloDiff)
                || hasEntries(topicDiff)
                || hasEntries(topicCloDiff)
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
                .cloDiff(cloDiff)
                .cloPloDiff(cloPloDiff)
                .topicDiff(topicDiff)
                .topicCloDiff(topicCloDiff)
                .assessmentDiff(assessmentDiff)
                .assessmentCloDiff(assessmentCloDiff)
                .readingsDiff(readingsDiff)
                .build();
    }

    private Map<String, SyllabusDiffResponse.FieldDiff> compareGeneralInfo(
            Syllabus oldSyllabus,
            Syllabus newSyllabus) {

        Map<String, SyllabusDiffResponse.FieldDiff> changes
                = new LinkedHashMap<>();

        addFieldDiff(changes, "courseName",
                oldSyllabus.getCourse().getName(),
                newSyllabus.getCourse().getName());
        addFieldDiff(changes, "academicYear",
                oldSyllabus.getAcademicYear(),
                newSyllabus.getAcademicYear());
        addFieldDiff(changes, "courseDesignation",
                oldSyllabus.getCourseDesignation(),
                newSyllabus.getCourseDesignation());
        addFieldDiff(changes, "courseTypes",
                oldSyllabus.getCourseTypes(),
                newSyllabus.getCourseTypes());
        addFieldDiff(changes, "semester",
                oldSyllabus.getSemester(),
                newSyllabus.getSemester());
        addFieldDiff(changes, "language",
                oldSyllabus.getLanguage(),
                newSyllabus.getLanguage());
        addFieldDiff(changes, "relation",
                oldSyllabus.getRelation(),
                newSyllabus.getRelation());
        addFieldDiff(changes, "teachingMethods",
                oldSyllabus.getTeachingMethods(),
                newSyllabus.getTeachingMethods());
        addFieldDiff(changes, "workloadTotal",
                oldSyllabus.getWorkloadTotal(),
                newSyllabus.getWorkloadTotal());
        addFieldDiff(changes, "workloadContact",
                oldSyllabus.getWorkloadContact(),
                newSyllabus.getWorkloadContact());
        addFieldDiff(changes, "workloadPrivate",
                oldSyllabus.getWorkloadPrivate(),
                newSyllabus.getWorkloadPrivate());
        addFieldDiff(changes, "prerequisites",
                oldSyllabus.getPrerequisites(),
                newSyllabus.getPrerequisites());
        addFieldDiff(changes, "objectives",
                oldSyllabus.getObjectives(),
                newSyllabus.getObjectives());
        addFieldDiff(changes, "examForms",
                oldSyllabus.getExamForms(),
                newSyllabus.getExamForms());
        addFieldDiff(changes, "examRequirements",
                oldSyllabus.getExamRequirements(),
                newSyllabus.getExamRequirements());
        addFieldDiff(changes, "rubrics",
                oldSyllabus.getRubrics(),
                newSyllabus.getRubrics());
        addFieldDiff(changes, "major",
                oldSyllabus.getMajor(),
                newSyllabus.getMajor());
        addFieldDiff(changes, "notes",
                oldSyllabus.getNotes(),
                newSyllabus.getNotes());

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
            compareTopics(MatchResult<Topic> matches) {

        return buildListDiff(
                matches,
                topic -> topicSnapshot(topic, null),
                topic -> topicSnapshot(topic, null),
                (oldTopic, newTopic) -> {
                    Map<String, SyllabusDiffResponse.FieldDiff> changes
                            = new LinkedHashMap<>();
                    addFieldDiff(changes, "weekNumber",
                            oldTopic.getWeekNumber(), newTopic.getWeekNumber());
                    addFieldDiff(changes, "orderInWeek",
                            oldTopic.getOrderInWeek(), newTopic.getOrderInWeek());
                    addFieldDiff(changes, "name",
                            oldTopic.getName(), newTopic.getName());
                    addFieldDiff(changes, "nameVn",
                            oldTopic.getNameVn(), newTopic.getNameVn());
                    addFieldDiff(changes, "teachingHours",
                            oldTopic.getTeachingHours(), newTopic.getTeachingHours());
                    addFieldDiff(changes, "labHours",
                            oldTopic.getLabHours(), newTopic.getLabHours());
                    addFieldDiff(changes, "selfStudyHours",
                            oldTopic.getSelfStudyHours(), newTopic.getSelfStudyHours());
                    addFieldDiff(changes, "topicType",
                            oldTopic.getTopicType(), newTopic.getTopicType());
                    addFieldDiff(changes, "teachingMethod",
                            oldTopic.getTeachingMethod(), newTopic.getTeachingMethod());
                    addFieldDiff(changes, "learningActivity",
                            oldTopic.getLearningActivity(), newTopic.getLearningActivity());
                    addFieldDiff(changes, "notes",
                            oldTopic.getNotes(), newTopic.getNotes());

                    return changes.isEmpty()
                            ? null
                            : topicSnapshot(newTopic, changes);
                });
    }

    private SyllabusDiffResponse.TopicDiff topicSnapshot(
            Topic topic,
            Map<String, SyllabusDiffResponse.FieldDiff> changes) {

        return SyllabusDiffResponse.TopicDiff.builder()
                .name(topic.getName())
                .nameVn(topic.getNameVn())
                .weekNumber(topic.getWeekNumber())
                .orderInWeek(topic.getOrderInWeek())
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
                .weightPercent(assessment.getWeightPercent() == null
                        ? null
                        : assessment.getWeightPercent().doubleValue())
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
        String name = normalizedKey(topic.getName());
        String nameVn = normalizedKey(topic.getNameVn());
        if (name == null && nameVn == null) {
            return null;
        }
        return compositeKey(name, nameVn);
    }

    private String readingPrimaryKey(SyllabusBook item) {
        Book book = item.getBook();
        if (book.getId() != null) {
            return "book-id:" + book.getId();
        }
        String isbn = normalizedKey(book.getIsbn());
        return isbn == null ? null : "isbn:" + isbn;
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

        Object comparableOld = comparableValue(oldValue);
        Object comparableNew = comparableValue(newValue);

        if (!Objects.equals(comparableOld, comparableNew)) {
            changes.put(
                    fieldName,
                    SyllabusDiffResponse.FieldDiff.builder()
                            .oldValue(diffText(oldValue))
                            .newValue(diffText(newValue))
                            .build());
        }
    }

    private Object comparableValue(Object value) {
        if (value instanceof String text) {
            String trimmed = text.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
        return value;
    }

    private String diffText(Object value) {
        return value == null ? "" : String.valueOf(value);
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
