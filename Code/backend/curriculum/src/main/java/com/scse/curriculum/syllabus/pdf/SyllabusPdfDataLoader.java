package com.scse.curriculum.syllabus.pdf;

import com.scse.curriculum.assessment.entity.AssessmentClo;
import com.scse.curriculum.assessment.entity.AssessmentComponent;
import com.scse.curriculum.assessment.repository.AssessmentCloRepository;
import com.scse.curriculum.assessment.repository.AssessmentComponentRepository;
import com.scse.curriculum.classsection.entity.ClassSection;
import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.clo.repository.CloRepository;
import com.scse.curriculum.cloplomapping.entity.CloPloMapping;
import com.scse.curriculum.cloplomapping.repository.CloPloMappingRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.syllabus.service.SyllabusAccessService;
import com.scse.curriculum.syllabusbook.entity.SyllabusBook;
import com.scse.curriculum.syllabusbook.repository.SyllabusBookRepository;
import com.scse.curriculum.topic.entity.Topic;
import com.scse.curriculum.topic.repository.TopicRepository;
import com.scse.curriculum.topicclo.entity.TopicClo;
import com.scse.curriculum.topicclo.repository.TopicCloRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class SyllabusPdfDataLoader {

    private final SyllabusRepository syllabusRepository;
    private final SyllabusAccessService syllabusAccessService;
    private final ClassSectionRepository classSectionRepository;
    private final CloRepository cloRepository;
    private final CloPloMappingRepository cloPloMappingRepository;
    private final TopicRepository topicRepository;
    private final TopicCloRepository topicCloRepository;
    private final AssessmentComponentRepository assessmentComponentRepository;
    private final AssessmentCloRepository assessmentCloRepository;
    private final SyllabusBookRepository syllabusBookRepository;

    public SyllabusPdfDataLoader(
            SyllabusRepository syllabusRepository,
            SyllabusAccessService syllabusAccessService,
            ClassSectionRepository classSectionRepository,
            CloRepository cloRepository,
            CloPloMappingRepository cloPloMappingRepository,
            TopicRepository topicRepository,
            TopicCloRepository topicCloRepository,
            AssessmentComponentRepository assessmentComponentRepository,
            AssessmentCloRepository assessmentCloRepository,
            SyllabusBookRepository syllabusBookRepository) {
        this.syllabusRepository = syllabusRepository;
        this.syllabusAccessService = syllabusAccessService;
        this.classSectionRepository = classSectionRepository;
        this.cloRepository = cloRepository;
        this.cloPloMappingRepository = cloPloMappingRepository;
        this.topicRepository = topicRepository;
        this.topicCloRepository = topicCloRepository;
        this.assessmentComponentRepository = assessmentComponentRepository;
        this.assessmentCloRepository = assessmentCloRepository;
        this.syllabusBookRepository = syllabusBookRepository;
    }

    @Transactional(readOnly = true)
    public SyllabusPdfDocument load(Integer syllabusId) {
        Syllabus syllabus = syllabusRepository.findByIdForPdf(syllabusId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề cương."));

        syllabusAccessService.assertCanView(syllabus);

        List<Clo> clos = new ArrayList<>(cloRepository.findForPdfBySyllabusId(syllabusId));
        clos.sort(Comparator
                .comparing(Clo::getOrderIndex, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(Clo::getCode, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        List<CloPloMapping> mappings = new ArrayList<>(
                cloPloMappingRepository.findForPdfBySyllabusId(syllabusId));
        mappings.sort(Comparator
                .comparing((CloPloMapping item) -> item.getClo().getCode(),
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(item -> item.getPlo().getCode(),
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        Map<Integer, SyllabusPdfDocument.PloColumn> ploById = new LinkedHashMap<>();
        for (CloPloMapping mapping : mappings) {
            if (mapping.getPlo() == null || mapping.getPlo().getId() == null) {
                continue;
            }
            ploById.putIfAbsent(
                    mapping.getPlo().getId(),
                    new SyllabusPdfDocument.PloColumn(
                            mapping.getPlo().getId(),
                            mapping.getPlo().getCode(),
                            mapping.getPlo().getDescription()));
        }
        List<SyllabusPdfDocument.PloColumn> plos = new ArrayList<>(ploById.values());
        plos.sort(Comparator.comparing(
                SyllabusPdfDocument.PloColumn::code,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        List<SyllabusPdfDocument.CloPloCell> cloPloCells = mappings.stream()
                .filter(item -> item.getClo() != null && item.getPlo() != null)
                .map(item -> new SyllabusPdfDocument.CloPloCell(
                        item.getClo().getId(),
                        item.getPlo().getId(),
                        item.getLevel() == null ? null : item.getLevel().name(),
                        item.getContributionWeight(),
                        item.getNotes()))
                .toList();

        List<Topic> topics = new ArrayList<>(
                topicRepository.findBySyllabusIdOrderByWeekNumberAscOrderInWeekAsc(syllabusId));
        Map<Integer, Set<String>> topicCloCodes = new LinkedHashMap<>();
        for (TopicClo item : topicCloRepository.findForPdfBySyllabusId(syllabusId)) {
            if (item.getTopic() == null || item.getClo() == null) {
                continue;
            }
            topicCloCodes
                    .computeIfAbsent(item.getTopic().getId(), ignored -> new LinkedHashSet<>())
                    .add(item.getClo().getCode());
        }

        List<AssessmentComponent> assessments = new ArrayList<>(
                assessmentComponentRepository.findBySyllabusId(syllabusId));
        assessments.sort(Comparator
                .comparing(AssessmentComponent::getOrderIndex,
                        Comparator.nullsLast(Integer::compareTo))
                .thenComparing(AssessmentComponent::getName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        Map<Integer, List<SyllabusPdfDocument.AssessmentCloRow>> assessmentCloRows = new LinkedHashMap<>();
        for (AssessmentClo item : assessmentCloRepository
                .findForPdfBySyllabusId(syllabusId)) {
            if (item.getAssessmentComponent() == null || item.getClo() == null) {
                continue;
            }
            assessmentCloRows
                    .computeIfAbsent(item.getAssessmentComponent().getId(), ignored -> new ArrayList<>())
                    .add(new SyllabusPdfDocument.AssessmentCloRow(
                            item.getClo().getCode(),
                            item.getContributionPercent()));
        }
        assessmentCloRows.values().forEach(rows -> rows.sort(Comparator.comparing(
                SyllabusPdfDocument.AssessmentCloRow::cloCode,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))));

        List<SyllabusBook> syllabusBooks = new ArrayList<>(
                syllabusBookRepository.findForPdfBySyllabusId(syllabusId));
        syllabusBooks.sort(Comparator
                .comparing(SyllabusBook::getOrderIndex,
                        Comparator.nullsLast(Integer::compareTo))
                .thenComparing(item -> item.getBook() == null ? null : item.getBook().getTitle(),
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        List<ClassSection> sections = classSectionRepository.findForPdfBySyllabusId(syllabusId);
        String responsiblePersons = sections.stream()
                .map(ClassSection::getInstructor)
                .filter(Objects::nonNull)
                .map(instructor -> instructor.getFullName())
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .reduce((left, right) -> left + ", " + right)
                /*
                 * The technical account that created/imported the syllabus
                 * is not automatically the academic person responsible for
                 * the course. If no linked teaching assignment exists, make
                 * the missing assignment explicit instead of printing
                 * "admin" as the responsible instructor.
                 */
                .orElse("Not assigned");

        var course = syllabus.getCourse();
        var department = course == null ? null : course.getDepartment();

        /*
         * A syllabus may contain legacy academicYear values such as "CS2021".
         * For the official document, prefer the linked teaching assignment scope,
         * because FR-03.1 authorizes the syllabus by Course + Academic Year + Semester.
         */
        String resolvedAcademicYear = sections.stream()
                .map(ClassSection::getAcademicYear)
                .filter(value -> value != null && value.matches("\\d{4}-\\d{4}"))
                .findFirst()
                .orElse(syllabus.getAcademicYear());

        String resolvedSemester = sections.stream()
                .map(section -> section.getSemester() == null
                        ? null
                        : String.valueOf(section.getSemester()))
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(syllabus.getSemester());

        String courseCode = course == null ? null : course.getCourseCode();

        return new SyllabusPdfDocument(
                syllabus.getId(),
                syllabus.getStatus() == null ? "UNKNOWN" : syllabus.getStatus().name(),
                course == null ? null : course.getCourseCode(),
                course == null ? null : course.getName(),
                course == null ? null : course.getNameVn(),
                department == null ? null : department.getCode(),
                department == null ? null : department.getName(),
                resolvedAcademicYear,
                resolvedSemester,
                syllabus.getVersionNumber(),
                syllabus.getVersionLabel(),
                Boolean.TRUE.equals(syllabus.getIsCurrent()),
                cleanLegacyField(syllabus.getCourseDesignation(), courseCode),
                cleanLegacyField(syllabus.getCourseTypes(), courseCode),
                cleanLegacyField(syllabus.getLanguage(), courseCode),
                cleanLegacyField(syllabus.getRelation(), courseCode),
                cleanLegacyField(syllabus.getTeachingMethods(), courseCode),
                syllabus.getWorkloadTotal(),
                syllabus.getWorkloadContact(),
                syllabus.getWorkloadPrivate(),
                syllabus.getPrerequisites(),
                syllabus.getObjectives(),
                syllabus.getExamForms(),
                syllabus.getExamRequirements(),
                syllabus.getRubrics(),
                syllabus.getMajor(),
                course == null ? null : course.getCreditTheory(),
                course == null ? null : course.getCreditLab(),
                responsiblePersons,
                syllabus.getCreatedBy() == null ? null : syllabus.getCreatedBy().getUsername(),
                syllabus.getApprovedBy() == null ? null : syllabus.getApprovedBy().getUsername(),
                syllabus.getSubmittedAt(),
                syllabus.getApprovedAt(),
                syllabus.getUpdatedAt(),
                syllabus.getChangeSummary(),
                syllabus.getNotes(),
                clos.stream().map(item -> new SyllabusPdfDocument.CloRow(
                        item.getId(),
                        item.getCode(),
                        item.getCompetencyLevel() == null ? null : item.getCompetencyLevel().name(),
                        item.getBloomLevel() == null ? null : item.getBloomLevel().name(),
                        item.getDescription(),
                        item.getDescriptionVn(),
                        item.getOrderIndex())).toList(),
                plos,
                cloPloCells,
                topics.stream().map(item -> new SyllabusPdfDocument.TopicRow(
                        item.getId(),
                        item.getWeekNumber(),
                        item.getOrderInWeek(),
                        item.getName(),
                        item.getNameVn(),
                        item.getTeachingHours(),
                        item.getLabHours(),
                        item.getSelfStudyHours(),
                        item.getTopicType() == null ? null : item.getTopicType().name(),
                        item.getTeachingMethod(),
                        item.getLearningActivity(),
                        item.getNotes(),
                        new ArrayList<>(topicCloCodes.getOrDefault(item.getId(), Set.of())))).toList(),
                assessments.stream().map(item -> new SyllabusPdfDocument.AssessmentRow(
                        item.getId(),
                        item.getName(),
                        item.getNameVn(),
                        item.getAssessmentType(),
                        item.getWeightPercent(),
                        item.getMinScore(),
                        item.getMaxScore(),
                        item.getOrderIndex(),
                        assessmentCloRows.getOrDefault(item.getId(), List.of()))).toList(),
                syllabusBooks.stream()
                        .filter(item -> item.getBook() != null)
                        .map(item -> new SyllabusPdfDocument.BookRow(
                                item.getBook().getId(),
                                item.getUsageType() == null ? null : item.getUsageType().name(),
                                item.getOrderIndex(),
                                item.getBook().getTitle(),
                                item.getBook().getAuthor(),
                                item.getBook().getPublisher(),
                                item.getBook().getYear(),
                                item.getBook().getEdition(),
                                item.getBook().getIsbn(),
                                item.getBook().getUrl(),
                                item.getBook().getBookType() == null
                                        ? null
                                        : item.getBook().getBookType().name()))
                        .toList());
    }
    /**
     * Old seed data used "Course Code" (and occasionally the course code itself)
     * as a placeholder in unrelated Module Handbook fields. An official PDF must
     * show missing data as blank rather than publish a misleading value.
     */
    private String cleanLegacyField(String value, String courseCode) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();

        if ("Course Code".equalsIgnoreCase(normalized)) {
            return null;
        }

        if (courseCode != null
                && !courseCode.isBlank()
                && courseCode.trim().equalsIgnoreCase(normalized)) {
            return null;
        }

        return normalized;
    }

}
