package com.scse.curriculum.syllabus.service;

import com.scse.curriculum.syllabus.entity.SyllabusVersion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scse.curriculum.approval.entity.ApprovalRequest;
import com.scse.curriculum.approval.entity.ApprovalStatus;
import com.scse.curriculum.approval.entity.ApprovalStep;
import com.scse.curriculum.approval.repository.ApprovalRequestRepository;
import com.scse.curriculum.assessment.entity.AssessmentClo;
import com.scse.curriculum.assessment.entity.AssessmentCloId;
import com.scse.curriculum.assessment.entity.AssessmentComponent;
import com.scse.curriculum.assessment.repository.AssessmentCloRepository;
import com.scse.curriculum.assessment.repository.AssessmentComponentRepository;
import com.scse.curriculum.classsection.entity.ClassSection;
import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.clo.entity.BloomLevel;
import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.clo.entity.CompetencyLevel;
import com.scse.curriculum.clo.repository.CloRepository;
import com.scse.curriculum.cloplomapping.entity.CloPloMapping;
import com.scse.curriculum.cloplomapping.repository.CloPloMappingRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.common.exception.ForbiddenOperationException;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.course.repository.CourseRepository;
import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.enrollment.repository.EnrollmentRepository;
import com.scse.curriculum.email.WorkflowNotificationService;
import com.scse.curriculum.studentscore.repository.StudentScoreRepository;
import com.scse.curriculum.syllabus.dto.CloneSyllabusRequest;
import com.scse.curriculum.syllabus.dto.CreateSyllabusRequest;
import com.scse.curriculum.syllabus.dto.SyllabusDiffResponse;
import com.scse.curriculum.syllabus.dto.SyllabusCreateContextResponse;
import com.scse.curriculum.syllabus.dto.SyllabusResponse;
import com.scse.curriculum.syllabus.dto.SubmissionValidationResponse;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusImportStatus;
import com.scse.curriculum.syllabus.entity.SyllabusSourceType;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.syllabusbook.entity.SyllabusBook;
import com.scse.curriculum.syllabusbook.entity.SyllabusBookId;
import com.scse.curriculum.syllabusbook.repository.SyllabusBookRepository;
import com.scse.curriculum.syllabus.importer.repository.SyllabusImportHistoryRepository;
import com.scse.curriculum.topic.entity.Topic;
import com.scse.curriculum.topic.entity.TopicType;
import com.scse.curriculum.topic.repository.TopicRepository;
import com.scse.curriculum.topicclo.entity.TopicClo;
import com.scse.curriculum.topicclo.entity.TopicCloId;
import com.scse.curriculum.topicclo.repository.TopicCloRepository;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.entity.CurriculumTerm;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.plo.entity.Plo;
import com.scse.curriculum.plo.repository.PloRepository;
import lombok.RequiredArgsConstructor;
import com.scse.curriculum.syllabus.dto.SyllabusCatalogResponse;
import com.scse.curriculum.syllabus.comparison.dto.SemanticSyllabusDiffResponse;
import com.scse.curriculum.syllabus.comparison.service.SyllabusSemanticComparisonService;
@Service
@RequiredArgsConstructor
public class SyllabusServiceImpl implements SyllabusService {
        private final CourseProgramRepository courseProgramRepository;
        private final CohortRepository cohortRepository;
        private final PloRepository ploRepository;
        private final SyllabusRepository repository;
        private final CourseRepository courseRepository;
        private final EnrollmentRepository enrollmentRepository;
        private final CloRepository cloRepository;
        private final CloPloMappingRepository cloPloMappingRepository;
        private final TopicRepository topicRepository;
        private final TopicCloRepository topicCloRepository;
        private final SyllabusBookRepository syllabusBookRepository;
        private final AssessmentComponentRepository assessmentComponentRepository;
        private final AssessmentCloRepository assessmentCloRepository;
        private final StudentScoreRepository studentScoreRepository;
        private final SyllabusSemanticComparisonService syllabusSemanticComparisonService;
        private final ApprovalRequestRepository approvalRequestRepository;
        private final ClassSectionRepository classSectionRepository;
        private final SyllabusImportHistoryRepository syllabusImportHistoryRepository;

        private final WorkflowNotificationService workflowNotificationService;
        private final SyllabusAccessService syllabusAccessService;
        private final SyllabusSubmissionValidationService submissionValidationService;
        private final SyllabusDiffService syllabusDiffService;
        private final EntityManager entityManager;
        private final SyllabusIdentityService syllabusIdentityService;
        private final com.scse.curriculum.syllabus.history.SyllabusHistoryService syllabusHistoryService;

        @Override
        @Transactional
        public SyllabusResponse create(CreateSyllabusRequest request) {

                if (request.getAssignmentId() == null && request.getCohortId() == null) {
                        throw new IllegalArgumentException(
                                        "Cohort is required when an administrator creates or imports a syllabus");
                }

                // Version number luôn do server cấp, không nhận từ client.
                Course requestedCourse = courseRepository
                                .findById(request.getCourseId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Course not found"));

                /*
                 * FR-03.1:
                 * Backend xác thực chính xác ClassSection.
                 * Với Faculty, course/year/semester thật được lấy từ assignment.
                 */
                SyllabusAccessService.CreationAuthorization creationAuthorization = syllabusAccessService
                                .authorizeCreate(
                                                request.getAssignmentId(),
                                                requestedCourse,
                                                request.getAcademicYear(),
                                                request.getSemester(),
                                                request.getProgramId(),
                                                request.getCohortId());

                Course course = creationAuthorization.course();
                UserAccount creator = creationAuthorization.creator();

                /*
                 * Version phải được tính theo course đã được backend xác thực,
                 * không tính trực tiếp từ dữ liệu client.
                 */
                Integer version = 1;
                SyllabusSourceType sourceType = resolveSourceType(request.getSourceType());
                boolean imported = sourceType == SyllabusSourceType.IMPORT_PDF
                                || sourceType == SyllabusSourceType.IMPORT_DOCX
                                || sourceType == SyllabusSourceType.IMPORT_XLSX;

                CourseProgram courseProgram = null;

                if (request.getCourseProgramId() != null) {
                        courseProgram = courseProgramRepository
                                        .findById(request.getCourseProgramId())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "CourseProgram not found"));

                        if (courseProgram.getCourse() == null
                                        || !Objects.equals(
                                                        courseProgram.getCourse().getId(),
                                                        course.getId())) {

                                throw new IllegalArgumentException(
                                                "The syllabus does not belong to the correct course "
                                                                + "in the curriculum program");
                        }

                        if (courseProgram.getSemesterSuggest() != null
                                        && !normalizeSemesterLabel(creationAuthorization.semester())
                                                        .equals("Semester " + courseProgram.getSemesterSuggest())) {
                                throw new IllegalArgumentException(
                                                "The teaching assignment semester does not match the selected curriculum entry");
                        }

                        if (request.getCohortId() != null) {
                                Cohort selectedCohort = cohortRepository.findById(request.getCohortId())
                                                .orElseThrow(() -> new ResourceNotFoundException("Cohort not found"));

                                if (courseProgram.getProgram() == null
                                                || selectedCohort.getProgram() == null
                                                || !Objects.equals(courseProgram.getProgram().getId(), selectedCohort.getProgram().getId())) {
                                        throw new IllegalArgumentException(
                                                        "The selected Cohort does not belong to the curriculum program");
                                }

                                if (courseProgram.getCohort() == null) {
                                        CourseProgram baseCourseProgram = courseProgram;
                                        courseProgram = courseProgramRepository
                                                        .findByCourse_IdAndProgram_IdAndCohort_Id(
                                                                        course.getId(),
                                                                        baseCourseProgram.getProgram().getId(),
                                                                        selectedCohort.getId())
                                                        .orElseGet(() -> courseProgramRepository.save(
                                                                        CourseProgram.builder()
                                                                                        .termCode(baseCourseProgram.getTermCode())
                                                                                        .course(baseCourseProgram.getCourse())
                                                                                        .program(baseCourseProgram.getProgram())
                                                                                        .cohort(selectedCohort)
                                                                                        .courseType(baseCourseProgram.getCourseType())
                                                                                        .semesterSuggest(baseCourseProgram.getSemesterSuggest())
                                                                                        .yearSuggest(baseCourseProgram.getYearSuggest())
                                                                                        .required(baseCourseProgram.getRequired())
                                                                                        .build()));
                                } else if (!Objects.equals(courseProgram.getCohort().getId(), selectedCohort.getId())) {
                                        throw new IllegalArgumentException(
                                                        "The selected curriculum entry does not belong to the requested Cohort");
                                }
                        }
                }
if (courseProgram == null && request.getCohortId() != null) {
    courseProgram = courseProgramRepository.findByCourse_IdAndCohort_Id(course.getId(), request.getCohortId())
            .stream().filter(cp -> request.getProgramId() == null || cp.getProgram() != null
                    && Objects.equals(cp.getProgram().getId(), request.getProgramId())).findFirst().orElse(null);
}
if (courseProgram == null && !creationAuthorization.assignments().isEmpty()) {
    ClassSection assignment = creationAuthorization.assignments().get(0);
    if (assignment.getCohort() != null) {
        courseProgram = courseProgramRepository.findByCourse_IdAndCohort_Id(course.getId(), assignment.getCohort().getId())
                .stream().filter(cp -> assignment.getProgram() == null || cp.getProgram() != null
                        && Objects.equals(cp.getProgram().getId(), assignment.getProgram().getId())).findFirst().orElse(null);
    }
}
String canonicalAcademicYear =
        creationAuthorization.academicYear();

String canonicalSemester =
        normalizeSemesterLabel(
                creationAuthorization.semester());

String canonicalProgram =
        request.getProgram();

/*
 * Canonical curriculum context always wins.
 *
 * Syllabus.academicYear in this project represents
 * the applicable curriculum cohort, e.g. CS2026.
 */
if (courseProgram != null) {

    if (courseProgram.getCohort() != null
            && courseProgram.getCohort().getName() != null) {

        canonicalAcademicYear =
                courseProgram
                        .getCohort()
                        .getName();
    }

    if (courseProgram.getProgram() != null
            && courseProgram.getProgram().getCode() != null) {

        canonicalProgram =
                courseProgram
                        .getProgram()
                        .getCode();
    }

    if (courseProgram.getSemesterSuggest() != null) {

        canonicalSemester =
                "Semester "
                        + courseProgram
                                .getSemesterSuggest();
    }
}
                syllabusIdentityService.assertAvailable(course, canonicalProgram, canonicalAcademicYear, canonicalSemester, null);
                Syllabus syllabus = Syllabus.builder()
                                .course(course)
                                .versionNumber(version)
                                .versionLabel(SyllabusVersion.format(version))
                                .academicYear(
        canonicalAcademicYear)
                                .courseDesignation(request.getCourseDesignation())
                                .courseTypes(request.getCourseTypes())
                                .semester(
        canonicalSemester)
        .program(
        canonicalProgram)
                                .language(request.getLanguage())
                                .relation(request.getRelation())
                                .teachingMethods(request.getTeachingMethods())
                                .workloadTotal(request.getWorkloadTotal())
                                .workloadContact(request.getWorkloadContact())
                                .workloadPrivate(request.getWorkloadPrivate())
                                .prerequisites(request.getPrerequisites())
                                .objectives(request.getObjectives())
                                .examForms(request.getExamForms())
                                .examRequirements(request.getExamRequirements())
                                .rubrics(request.getRubrics())
                                .major(request.getMajor())
                                .status(SyllabusStatus.DRAFT)
                                .isCurrent(false)
                                .createdBy(creator)
                                .approvedBy(null)
                                .submittedAt(null)
                                .approvedAt(null)
                                .changeSummary(request.getChangeSummary())
                                .notes(request.getNotes())
                                .sourceType(sourceType)
                                .originalFileName(imported ? request.getOriginalFileName() : null)
                                .originalFileType(imported ? request.getOriginalFileType() : null)
                                .importStatus(imported
                                                ? SyllabusImportStatus.CONFIRMED
                                                : SyllabusImportStatus.NONE)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                if (request.getClos() != null) {
                        List<Clo> clos = request.getClos()
                                        .stream()
                                        .map(dto -> Clo.builder()
                                                        .syllabus(syllabus)
                                                        .code(dto.getCode())
                                                        .description(dto.getDescription())
                                                        .descriptionVn(dto.getDescriptionVn())
                                                        .competencyLevel(dto.getCompetencyLevel() != null
                                                                        ? CompetencyLevel.valueOf(
                                                                                        dto.getCompetencyLevel())
                                                                        : null)
                                                        .bloomLevel(dto.getBloomLevel() != null
                                                                        ? BloomLevel.valueOf(dto.getBloomLevel())
                                                                        : null)
                                                        .orderIndex(dto.getOrderIndex())
                                                        .build())
                                        .collect(Collectors.toList());

                        syllabus.setClos(clos);
                }

                if (request.getTopics() != null) {
                        List<Topic> topics = request.getTopics()
                                        .stream()
                                        .map(dto -> Topic.builder()
                                                        .syllabus(syllabus)
                                                        .weekNumber(dto.getWeekNumber())
                                                        .orderInWeek(dto.getOrderInWeek())
                                                        .name(dto.getName())
                                                        .nameVn(dto.getNameVn())
                                                        .teachingHours(topicHoursOrDefault(dto.getTeachingHours(), 3))
                                                        .labHours(topicHoursOrDefault(dto.getLabHours(), 0))
                                                        .selfStudyHours(topicHoursOrDefault(dto.getSelfStudyHours(), 6))
                                                        .topicType(dto.getTopicType() != null
                                                                        ? TopicType.valueOf(dto.getTopicType())
                                                                        : null)
                                                        .teachingMethod(dto.getTeachingMethod())
                                                        .learningActivity(dto.getLearningActivity())
                                                        .assessments(dto.getAssessments())
                                                        .resources(dto.getResources())
                                                        .notes(dto.getNotes())
                                                        .build())
                                        .collect(Collectors.toList());

                        syllabus.setTopics(topics);
                }

                if (request.getAssessments() != null) {
                        List<AssessmentComponent> assessments = request.getAssessments()
                                        .stream()
                                        .map(dto -> AssessmentComponent.builder()
                                                        .syllabus(syllabus)
                                                        .name(dto.getName())
                                                        .nameVn(dto.getNameVn())
                                                        .assessmentType(dto.getAssessmentType())
                                                        .weightPercent(dto.getWeightPercent() != null
                                                                        ? dto.getWeightPercent()
                                                                        : 0f)
                                                        .minScore(dto.getMinScore() != null
                                                                        ? dto.getMinScore()
                                                                        : 0f)
                                                        .maxScore(dto.getMaxScore() != null
                                                                        ? dto.getMaxScore()
                                                                        : 100f)
                                                        .orderIndex(dto.getOrderIndex() != null
                                                                        ? dto.getOrderIndex()
                                                                        : 1)
                                                        .build())
                                        .collect(Collectors.toList());

                        syllabus.setAssessments(assessments);
                }

                Syllabus savedSyllabus = repository.save(syllabus);

                // Gắn Draft vừa tạo vào mọi nhóm lớp khớp cùng assignment.
                // Admin có thể tạo dữ liệu migration nên danh sách assignment có thể rỗng.
                if (!creationAuthorization.assignments().isEmpty()) {
                        creationAuthorization.assignments()
                                        .forEach(assignment -> assignment.setSyllabus(savedSyllabus));
                        classSectionRepository.saveAll(creationAuthorization.assignments());
                }

                if (courseProgram != null) {
                        courseProgram.setSyllabus(savedSyllabus);
                        courseProgramRepository.save(courseProgram);
                }

                if (request.getSourceSyllabusId() != null) {
                        cloneDetailsFromSource(
                                        request.getSourceSyllabusId(),
                                        savedSyllabus);
                }

                return map(savedSyllabus);
        }

        private SyllabusSourceType resolveSourceType(String rawSourceType) {
                if (rawSourceType == null || rawSourceType.isBlank()) {
                        return SyllabusSourceType.MANUAL;
                }
                try {
                        return SyllabusSourceType.valueOf(rawSourceType.trim().toUpperCase());
                } catch (IllegalArgumentException ignored) {
                        return SyllabusSourceType.MANUAL;
                }
        }

        private String normalizeSemesterLabel(String value) {
                if (value == null || value.isBlank()) {
                        return value;
                }

                String normalized = value.trim();
                java.util.regex.Matcher matcher = java.util.regex.Pattern
                                .compile("(?i)^(?:(?:semester|hk)\\s*)?([1-8])$")
                                .matcher(normalized);

                if (matcher.matches()) {
                        return "Semester " + matcher.group(1);
                }
                if (normalized.equalsIgnoreCase("summer")
                                || normalized.equalsIgnoreCase("summer semester")) {
                        return "Summer Semester";
                }
                return normalized;
        }

        private void synchronizeLinkedCourseProgramSemester(
                        Syllabus syllabus,
                        String semesterLabel) {
                CurriculumTerm term;
                try {
                        term = CurriculumTerm.fromValue(semesterLabel);
                } catch (IllegalArgumentException ignored) {
                        return;
                }

                Integer semesterNumber = term.getSemesterNumber();
                if (semesterNumber == null) {
                        return;
                }

                List<CourseProgram> linkedMappings = courseProgramRepository
                                .findBySyllabus_Id(syllabus.getId());
                linkedMappings.forEach(mapping -> {
                        mapping.setSemesterSuggest(semesterNumber);
                        mapping.setTermCode(term);
                });

                if (!linkedMappings.isEmpty()) {
                        courseProgramRepository.saveAll(linkedMappings);
                }
        }

        private int topicHoursOrDefault(Integer value, int defaultValue) {
                return value != null ? value : defaultValue;
        }

        private void cloneDetailsFromSource(
                        Integer sourceId,
                        Syllabus savedSyllabus) {

                List<Clo> sourceClos = cloRepository.findBySyllabusId(sourceId);
                Map<Integer, Clo> oldCloToNewCloMap = new HashMap<>();

                for (Clo srcClo : sourceClos) {
                        Clo targetClo = Clo.builder()
                                        .syllabus(savedSyllabus)
                                        .code(srcClo.getCode())
                                        .description(srcClo.getDescription())
                                        .descriptionVn(srcClo.getDescriptionVn())
                                        .competencyLevel(srcClo.getCompetencyLevel())
                                        .bloomLevel(srcClo.getBloomLevel())
                                        .orderIndex(srcClo.getOrderIndex())
                                        .build();

                        targetClo = cloRepository.save(targetClo);
                        oldCloToNewCloMap.put(srcClo.getId(), targetClo);

                        List<CloPloMapping> srcMappings = cloPloMappingRepository.findByCloId(srcClo.getId());

                        for (CloPloMapping srcMap : srcMappings) {
                                CloPloMapping targetMap = CloPloMapping.builder()
                                                .clo(targetClo)
                                                .plo(srcMap.getPlo())
                                                .level(srcMap.getLevel())
                                                .contributionWeight(srcMap.getContributionWeight() == null
                                                                ? 1.0f
                                                                : srcMap.getContributionWeight())
                                                .notes(srcMap.getNotes())
                                                .build();

                                cloPloMappingRepository.save(targetMap);
                        }
                }

                List<Topic> sourceTopics = topicRepository.findBySyllabusId(sourceId);

                for (Topic srcTopic : sourceTopics) {
                        Topic targetTopic = Topic.builder()
                                        .syllabus(savedSyllabus)
                                        .weekNumber(srcTopic.getWeekNumber())
                                        .orderInWeek(srcTopic.getOrderInWeek())
                                        .name(srcTopic.getName())
                                        .nameVn(srcTopic.getNameVn())
                                        .teachingHours(srcTopic.getTeachingHours())
                                        .labHours(srcTopic.getLabHours())
                                        .selfStudyHours(srcTopic.getSelfStudyHours())
                                        .topicType(srcTopic.getTopicType())
                                        .teachingMethod(srcTopic.getTeachingMethod())
                                        .learningActivity(srcTopic.getLearningActivity())
                                        .assessments(srcTopic.getAssessments())
                                        .resources(srcTopic.getResources())
                                        .notes(srcTopic.getNotes())
                                        .build();

                        targetTopic = topicRepository.save(targetTopic);

                        List<TopicClo> srcTopicClos = topicCloRepository.findByIdTopicId(srcTopic.getId());

                        for (TopicClo srcTc : srcTopicClos) {
                                Clo targetCloForTc = oldCloToNewCloMap.get(srcTc.getClo().getId());

                                if (targetCloForTc == null) {
                                        throw new IllegalStateException(
                                                        "Unable to clone Topic–CLO mapping because the source CLO does not exist in the copy.");
                                }

                                TopicCloId tcId = new TopicCloId(targetTopic.getId(), targetCloForTc.getId());

                                TopicClo targetTc = TopicClo.builder()
                                                .id(tcId)
                                                .topic(targetTopic)
                                                .clo(targetCloForTc)
                                                .teachingLevel(srcTc.getTeachingLevel())
                                                .build();

                                topicCloRepository.save(targetTc);
                        }
                }

                List<SyllabusBook> sourceBooks = syllabusBookRepository.findBySyllabus_Id(sourceId);

                for (SyllabusBook srcBook : sourceBooks) {
                        SyllabusBookId sbId = new SyllabusBookId(savedSyllabus.getId(), srcBook.getBook().getId());

                        SyllabusBook targetBook = SyllabusBook.builder()
                                        .id(sbId)
                                        .syllabus(savedSyllabus)
                                        .book(srcBook.getBook())
                                        .usageType(srcBook.getUsageType())
                                        .orderIndex(srcBook.getOrderIndex())
                                        .build();

                        syllabusBookRepository.save(targetBook);
                }

                List<AssessmentComponent> sourceComps = assessmentComponentRepository.findBySyllabusId(sourceId);

                for (AssessmentComponent srcComp : sourceComps) {
                        AssessmentComponent targetComp = AssessmentComponent.builder()
                                        .syllabus(savedSyllabus)
                                        .name(srcComp.getName())
                                        .nameVn(srcComp.getNameVn())
                                        .assessmentType(srcComp.getAssessmentType())
                                        .weightPercent(srcComp.getWeightPercent())
                                        .minScore(srcComp.getMinScore())
                                        .maxScore(srcComp.getMaxScore())
                                        .orderIndex(srcComp.getOrderIndex())
                                        .build();

                        targetComp = assessmentComponentRepository.save(targetComp);

                        List<AssessmentClo> srcAssessmentClos = assessmentCloRepository
                                        .findByAssessmentComponent_Id(srcComp.getId());

                        for (AssessmentClo srcAc : srcAssessmentClos) {
                                Clo targetCloForAc = oldCloToNewCloMap.get(srcAc.getClo().getId());

                                if (targetCloForAc == null) {
                                        throw new IllegalStateException(
                                                        "Unable to clone Assessment–CLO mapping because the source CLO does not exist in the copy.");
                                }

                                AssessmentCloId acId = new AssessmentCloId(targetComp.getId(), targetCloForAc.getId());

                                AssessmentClo targetAc = AssessmentClo.builder()
                                                .id(acId)
                                                .assessmentComponent(targetComp)
                                                .clo(targetCloForAc)
                                                .contributionPercent(srcAc.getContributionPercent())
                                                .build();

                                assessmentCloRepository.save(targetAc);
                        }
                }
        }

        @Override
        @Transactional(readOnly = true)
        public SyllabusCreateContextResponse getCreateContext(Integer courseId) {
                Course course = courseRepository.findById(courseId)
                                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

                syllabusAccessService.assertCanCreateContext(course);

                Syllabus latestApproved = repository
                                .findTopByCourseIdAndStatusOrderByVersionNumberDescIdDesc(
                                                courseId, SyllabusStatus.APPROVED)
                                .orElse(null);
                Syllabus latest = repository
                                .findTopByCourseIdOrderByVersionNumberDescIdDesc(courseId)
                                .orElse(null);

                // Never expose another instructor's working Draft as a template.
                if (syllabusAccessService.currentUser().getRole() != com.scse.curriculum.user.entity.UserRole.ADMIN
                                && latest != null
                                && latest.getStatus() == SyllabusStatus.DRAFT
                                && !syllabusAccessService.canView(latest)) {
                        latest = latestApproved;
                }

                // Priority 2 fallback: curriculum/course context (CourseProgram -> Program ->
                // PLO).
                List<CourseProgram> coursePrograms = courseProgramRepository.findByCourse_Id(courseId);

                List<SyllabusCreateContextResponse.CourseProgramSummary> courseProgramSummaries = coursePrograms
                                .stream()
                                .map(cp -> SyllabusCreateContextResponse.CourseProgramSummary.builder()
                                                .id(cp.getId())
                                                .programId(cp.getProgram() == null ? null : cp.getProgram().getId())
                                                .programCode(cp.getProgram() == null ? null : cp.getProgram().getCode())
                                                .programName(cp.getProgram() == null ? null : cp.getProgram().getName())
                                                .cohortId(cp.getCohort() == null ? null : cp.getCohort().getId())
                                                .cohortName(cp.getCohort() == null ? null : cp.getCohort().getName())
                                                .courseTypeId(cp.getCourseType() == null ? null
                                                                : cp.getCourseType().getId())
                                                .courseTypeName(cp.getCourseType() == null ? null
                                                                : cp.getCourseType().getName())
                                                .semesterSuggest(cp.getSemesterSuggest())
                                                .required(cp.getRequired())
                                                .build())
                                .toList();

                List<Integer> programIds = coursePrograms.stream()
                                .map(cp -> cp.getProgram() == null ? null : cp.getProgram().getId())
                                .filter(Objects::nonNull)
                                .distinct()
                                .toList();

                List<SyllabusCreateContextResponse.PloSummary> ploSummaries = new ArrayList<>();
                for (Integer programId : programIds) {
                        for (Plo plo : ploRepository.findByProgramId(programId)) {
                                ploSummaries.add(SyllabusCreateContextResponse.PloSummary.builder()
                                                .id(plo.getId())
                                                .programId(programId)
                                                .programCode(plo.getProgram() == null ? null
                                                                : plo.getProgram().getCode())
                                                .code(plo.getCode())
                                                .description(plo.getDescription())
                                                .build());
                        }
                }

                // Priority 3 fallback: static defaults, only used when nothing else supplies a
                // value.
                String defaultCourseType = coursePrograms.stream()
                                .map(cp -> cp.getCourseType() == null ? null : cp.getCourseType().getName())
                                .filter(Objects::nonNull)
                                .findFirst()
                                .orElse(null);

                String defaultSemester = coursePrograms.stream()
                                .map(CourseProgram::getSemesterSuggest)
                                .filter(Objects::nonNull)
                                .findFirst()
                                .map(semesterSuggest -> "Semester " + semesterSuggest)
                                .orElse(null);

                SyllabusCreateContextResponse.DefaultsContext defaults = SyllabusCreateContextResponse.DefaultsContext
                                .builder()
                                .language("English")
                                .teachingMethods("Lecture")
                                .semester(defaultSemester)
                                .courseTypes(defaultCourseType)
                                .build();

                return SyllabusCreateContextResponse.builder()
                                .course(SyllabusCreateContextResponse.CourseContext.builder()
                                                .id(course.getId())
                                                .courseCode(course.getCourseCode())
                                                .name(course.getName())
                                                .nameVn(course.getNameVn())
                                                .creditTheory(course.getCreditTheory())
                                                .creditLab(course.getCreditLab())
                                                .courseLevel(course.getCourseLevel() == null ? null
                                                                : course.getCourseLevel().name())
                                                .description(course.getDescription())
                                                .departmentName(course.getDepartment() == null ? null
                                                                : course.getDepartment().getName())
                                                .build())
                                .latestSyllabus(latest == null ? null : map(latest))
                                .latestApprovedSyllabus(latestApproved == null ? null : map(latestApproved))
                                .coursePrograms(courseProgramSummaries)
                                .plos(ploSummaries)
                                .defaults(defaults)
                                .build();
        }

        @Override
        @Transactional(readOnly = true)
        public List<SyllabusResponse> getAll() {
                UserAccount currentUser = syllabusAccessService.currentUser();
                List<Syllabus> source = repository.findAllWithRelations();

                return source
                                .stream()
                                .filter(syllabusAccessService::canView)
                                .map(this::map)
                                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public SyllabusResponse getById(Integer id) {

                Syllabus syllabus = repository.findByIdWithRelations(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found"));

                syllabusAccessService.assertCanView(syllabus);
                return map(syllabus);
        }

        @Override
        @Transactional(readOnly = true)
        public SyllabusResponse getByIdForEdit(Integer id) {
                Syllabus syllabus = repository.findByIdWithRelations(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found"));

                syllabusAccessService.assertCanModify(syllabus);
                return map(syllabus);
        }

        @Override
        @Transactional(readOnly = true)
        public List<SyllabusResponse> getByCourse(Integer courseId) {

                return repository.findByCourseIdWithRelations(courseId)
                                .stream()
                                .filter(syllabusAccessService::canView)
                                .map(this::map)
                                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public SyllabusResponse getPreviousComparable(
                        Integer id) {

                Syllabus current =
                                repository.findByIdWithRelations(id)
                                                .orElseThrow(() ->
                                                                new ResourceNotFoundException(
                                                                                "Syllabus not found"));

                syllabusAccessService.assertCanView(current);

                /*
                 * Visible cohort comparison is APPROVED-only.
                 *
                 * A Draft/Submitted/Under Review/Rejected syllabus must never
                 * acquire a Compare action merely because an older syllabus
                 * exists for the same course.
                 */
                if (current.getStatus() != SyllabusStatus.APPROVED) {
                        return null;
                }

                ComparisonContext currentContext =
                                findComparisonContext(current);

                if (currentContext == null
                                || currentContext.entryYear() == null) {
                        return null;
                }

                /*
                 * SAME COURSE
                 * SAME PROGRAM
                 * DIFFERENT / OLDER COHORT
                 * APPROVED candidate only
                 */
                CourseProgram previousMapping =
                                courseProgramRepository
                                                .findPreviousComparableCandidates(
                                                                currentContext.courseId(),
                                                                currentContext.programId(),
                                                                currentContext.entryYear())
                                                .stream()
                                                .filter(cp ->
                                                                cp != null
                                                                                && cp.getSyllabus() != null
                                                                                && cp.getSyllabus().getStatus()
                                                                                                == SyllabusStatus.APPROVED)
                                                .filter(cp ->
                                                                cp.getCohort() != null
                                                                                && cp.getCohort().getId() != null
                                                                                && !Objects.equals(
                                                                                                cp.getCohort().getId(),
                                                                                                currentContext.cohortId()))
                                                .filter(cp ->
                                                                cp.getCourse() != null
                                                                                && Objects.equals(
                                                                                                cp.getCourse().getId(),
                                                                                                currentContext.courseId()))
                                                .filter(cp ->
                                                                cp.getProgram() != null
                                                                                && Objects.equals(
                                                                                                cp.getProgram().getId(),
                                                                                                currentContext.programId()))
                                                .filter(cp ->
                                                                syllabusAccessService
                                                                                .canView(
                                                                                                cp.getSyllabus()))
                                                .findFirst()
                                                .orElse(null);

                if (previousMapping == null) {
                        return null;
                }

                return map(
                                previousMapping.getSyllabus());
        }
        /**
         * "Person responsible for the course": derived from the linked teaching
         * assignment's instructor, matching the PDF export. Not the account that
         * created/imported the draft (createdBy).
         */
        private String resolveResponsibleInstructors(Integer syllabusId) {
                return classSectionRepository.findForPdfBySyllabusId(syllabusId)
                                .stream()
                                .map(ClassSection::getInstructor)
                                .filter(Objects::nonNull)
                                .map(instructor -> instructor.getFullName())
                                .filter(value -> value != null && !value.isBlank())
                                .distinct()
                                .sorted(String.CASE_INSENSITIVE_ORDER)
                                .reduce((left, right) -> left + ", " + right)
                                .orElse(null);
        }

        private SyllabusResponse map(Syllabus syllabus) {

                CourseProgram syllabusCourseProgram = courseProgramRepository
                                .findBySyllabus_Id(syllabus.getId())
                                .stream()
                                .filter(courseProgram -> courseProgram.getCohort() != null)
                                .findFirst()
                                .orElse(null);

                return SyllabusResponse.builder()
                                .id(syllabus.getId())
                                .courseId(syllabus.getCourse().getId())
                                .courseCode(syllabus.getCourse().getCourseCode())
                                .courseName(syllabus.getCourse().getName())
                                .courseNameVn(syllabus.getCourse().getNameVn())
                                .versionNumber(syllabus.getVersionNumber())
                                .versionLabel(syllabus.getVersionLabel())
                                .cohortId(syllabusCourseProgram == null
                                                ? null
                                                : syllabusCourseProgram.getCohort().getId())
                                .cohortName(syllabusCourseProgram == null
                                                ? null
                                                : syllabusCourseProgram.getCohort().getName())
                                .courseProgramId(syllabusCourseProgram == null ? null : syllabusCourseProgram.getId())
                                .programId(syllabusCourseProgram == null ? null : syllabusCourseProgram.getProgram().getId())
                                .programCode(syllabusCourseProgram == null ? null : syllabusCourseProgram.getProgram().getCode())
                                .programName(syllabusCourseProgram == null ? null : syllabusCourseProgram.getProgram().getName())
                                .academicYear(syllabusCourseProgram == null || syllabusCourseProgram.getCohort() == null
                                                ? syllabus.getAcademicYear()
                                                : syllabusCourseProgram.getCohort().getName())
                                .creditTheory(syllabus.getCourse().getCreditTheory())
                                .creditLab(syllabus.getCourse().getCreditLab())
                                .responsibleInstructors(resolveResponsibleInstructors(syllabus.getId()))
                                .courseDesignation(syllabus.getCourseDesignation())
                                .courseTypes(syllabus.getCourseTypes())
                                .semester(syllabus.getSemester())
                                .language(syllabus.getLanguage())
                                .relation(syllabus.getRelation())
                                .teachingMethods(syllabus.getTeachingMethods())
                                .workloadTotal(syllabus.getWorkloadTotal())
                                .workloadContact(syllabus.getWorkloadContact())
                                .workloadPrivate(syllabus.getWorkloadPrivate())
                                .prerequisites(syllabus.getPrerequisites())
                                .objectives(syllabus.getObjectives())
                                .examForms(syllabus.getExamForms())
                                .examRequirements(syllabus.getExamRequirements())
                                .rubrics(syllabus.getRubrics())
                                .major(syllabus.getMajor())
                                .sourceType(syllabus.getSourceType() == null
                                                ? null
                                                : syllabus.getSourceType().name())
                                .originalFileName(syllabus.getOriginalFileName())
                                .originalFileType(syllabus.getOriginalFileType())
                                .importStatus(syllabus.getImportStatus() == null
                                                ? null
                                                : syllabus.getImportStatus().name())
                                .finalApprovalDate(syllabus.getFinalApprovalDate())
                                .status(syllabus.getStatus().name())
                                .isCurrent(syllabus.getIsCurrent())
                                .createdById(syllabus.getCreatedBy().getId())
                                .createdByUsername(syllabus.getCreatedBy().getUsername())
                                .approvedById(syllabus.getApprovedBy() == null
                                                ? null
                                                : syllabus.getApprovedBy().getId())
                                .approvedByUsername(syllabus.getApprovedBy() == null
                                                ? null
                                                : syllabus.getApprovedBy().getUsername())
                                .submittedAt(syllabus.getSubmittedAt())
                                .approvedAt(syllabus.getApprovedAt())
                                .changeSummary(syllabus.getChangeSummary())
                                .notes(syllabus.getNotes())
                                .clos(syllabus.getClos() != null
                                                ? syllabus.getClos()
                                                                .stream()
                                                                .map(clo -> new CreateSyllabusRequest.CloDTO(
                                                                                clo.getId(),
                                                                                clo.getCode(),
                                                                                clo.getDescription(),
                                                                                clo.getDescriptionVn(),
                                                                                clo.getCompetencyLevel() != null
                                                                                                ? clo.getCompetencyLevel()
                                                                                                                .name()
                                                                                                : null,
                                                                                clo.getBloomLevel() != null
                                                                                                ? clo.getBloomLevel()
                                                                                                                .name()
                                                                                                : null,
                                                                                clo.getOrderIndex()))
                                                                .collect(Collectors.toList())
                                                : new ArrayList<>())
                                .topics(syllabus.getTopics() != null
                                                ? syllabus.getTopics()
                                                                .stream()
                                                                .map(topic -> new CreateSyllabusRequest.TopicDTO(
                                                                                topic.getId(),
                                                                                topic.getWeekNumber(),
                                                                                topic.getOrderInWeek(),
                                                                                topic.getName(),
                                                                                topic.getNameVn(),
                                                                                topic.getTeachingHours(),
                                                                                topic.getLabHours(),
                                                                                topic.getSelfStudyHours(),
                                                                                topic.getTopicType() != null
                                                                                                ? topic.getTopicType()
                                                                                                                .name()
                                                                                                : null,
                                                                                topic.getTeachingMethod(),
                                                                                topic.getLearningActivity(),
                                                                                topic.getAssessments(),
                                                                                topic.getResources(),
                                                                                topic.getNotes()))
                                                                .collect(Collectors.toList())
                                                : new ArrayList<>())
                                .assessments(syllabus.getAssessments() != null
                                                ? syllabus.getAssessments()
                                                                .stream()
                                                                .map(assessment -> new CreateSyllabusRequest.AssessmentDTO(
                                                                                assessment.getId(),
                                                                                assessment.getName(),
                                                                                assessment.getNameVn(),
                                                                                assessment.getAssessmentType(),
                                                                                assessment.getWeightPercent(),
                                                                                assessment.getMinScore(),
                                                                                assessment.getMaxScore(),
                                                                                assessment.getOrderIndex()))
                                                                .collect(Collectors.toList())
                                                : new ArrayList<>())
                                .createdAt(syllabus.getCreatedAt())
                                .updatedAt(syllabus.getUpdatedAt())
                                .build();
        }

        @Override
        @Transactional
        public SyllabusResponse update(
                        Integer id,
                        CreateSyllabusRequest request) {

                Syllabus syllabus = repository.findByIdWithRelations(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found"));

                syllabusAccessService.assertCanModify(syllabus);
                assertContentIsMutable(syllabus);

                if (request.getCourseId() != null
                                && (syllabus.getCourse() == null
                                                || !request.getCourseId().equals(syllabus.getCourse().getId()))) {

                        Course course = courseRepository.findById(request.getCourseId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

                        syllabus.setCourse(course);
                }

                if (request.getVersionLabel() != null) {
                        SyllabusVersion.requireCanonical(syllabus.getVersionNumber(), request.getVersionLabel());
                }
                if (request.getAcademicYear() != null) {
                        syllabus.setAcademicYear(request.getAcademicYear());
                }
                if (request.getChangeSummary() != null) {
                        syllabus.setChangeSummary(request.getChangeSummary());
                }
                if (request.getNotes() != null) {
                        syllabus.setNotes(request.getNotes());
                }

                if (request.getCourseDesignation() != null) {
                        syllabus.setCourseDesignation(request.getCourseDesignation());
                }
                if (request.getCourseTypes() != null) {
                        syllabus.setCourseTypes(request.getCourseTypes());
                }
                if (request.getSemester() != null) {
                        String normalizedSemester = normalizeSemesterLabel(request.getSemester());
                        syllabus.setSemester(normalizedSemester);
                        synchronizeLinkedCourseProgramSemester(syllabus, normalizedSemester);
                }
                if (request.getLanguage() != null) {
                        syllabus.setLanguage(request.getLanguage());
                }
                if (request.getRelation() != null) {
                        syllabus.setRelation(request.getRelation());
                }
                if (request.getTeachingMethods() != null) {
                        syllabus.setTeachingMethods(request.getTeachingMethods());
                }
                if (request.getWorkloadTotal() != null) {
                        syllabus.setWorkloadTotal(request.getWorkloadTotal());
                }
                if (request.getWorkloadContact() != null) {
                        syllabus.setWorkloadContact(request.getWorkloadContact());
                }
                if (request.getWorkloadPrivate() != null) {
                        syllabus.setWorkloadPrivate(request.getWorkloadPrivate());
                }
                if (request.getPrerequisites() != null) {
                        syllabus.setPrerequisites(request.getPrerequisites());
                }
                if (request.getObjectives() != null) {
                        syllabus.setObjectives(request.getObjectives());
                }
                if (request.getExamForms() != null) {
                        syllabus.setExamForms(request.getExamForms());
                }
                if (request.getExamRequirements() != null) {
                        syllabus.setExamRequirements(request.getExamRequirements());
                }
                if (request.getRubrics() != null) {
                        syllabus.setRubrics(request.getRubrics());
                }
                if (request.getMajor() != null) {
                        syllabus.setMajor(request.getMajor());
                }

                // Nếu đổi course/năm học/học kỳ, quyền phải được kiểm tra lại
                // trên giá trị cuối cùng, tránh chuyển Draft sang môn không được phân công.
                syllabusAccessService.assertCanUseAssignmentFor(
                                syllabus.getCourse(),
                                syllabus.getAcademicYear(),
                                syllabus.getSemester());

                if (request.getClos() != null) {
                        reconcileClos(syllabus, request.getClos());
                }

                if (request.getTopics() != null) {
                        reconcileTopics(syllabus, request.getTopics());
                }

                if (request.getAssessments() != null) {
                        reconcileAssessments(syllabus, request.getAssessments());
                }

                syllabus.setUpdatedAt(LocalDateTime.now());

                syllabusIdentityService.assertAvailable(syllabus.getCourse(), syllabus.getProgram(), syllabus.getAcademicYear(), syllabus.getSemester(), syllabus.getId());
                Syllabus saved = repository.save(syllabus);
                repository.flush();

                return map(repository.findByIdWithRelations(saved.getId())
                                .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found")));
        }

        /**
         * Reconcile child rows in place so their database IDs remain stable. The
         * mapping tables reference those IDs, therefore delete-and-recreate would
         * silently erase CLO-PLO, Topic-CLO, Assessment-CLO and student scores.
         *
         * Explicit IDs are preferred. Natural-key fallbacks keep older clients
         * working, but newly returned Draft payloads always contain child IDs.
         */
        private void reconcileClos(
                        Syllabus syllabus,
                        List<CreateSyllabusRequest.CloDTO> requested) {

                List<Clo> existing = new ArrayList<>(syllabus.getClos());
                List<Clo> retained = new ArrayList<>();

                for (CreateSyllabusRequest.CloDTO dto : requested) {
                        Clo clo = findCloForUpdate(existing, retained, dto);
                        if (clo == null) {
                                clo = Clo.builder().syllabus(syllabus).build();
                                syllabus.getClos().add(clo);
                        }

                        clo.setCode(dto.getCode());
                        clo.setDescription(dto.getDescription());
                        clo.setDescriptionVn(dto.getDescriptionVn());
                        clo.setCompetencyLevel(dto.getCompetencyLevel() == null
                                        ? null
                                        : CompetencyLevel.valueOf(dto.getCompetencyLevel()));
                        clo.setBloomLevel(dto.getBloomLevel() == null
                                        ? null
                                        : BloomLevel.valueOf(dto.getBloomLevel()));
                        clo.setOrderIndex(dto.getOrderIndex());
                        retained.add(clo);
                }

                List<Clo> removed = existing.stream()
                                .filter(clo -> !retained.contains(clo))
                                .toList();

                for (Clo clo : removed) {
                        deleteAllIfPresent(assessmentCloRepository.findByClo_Id(clo.getId()),
                                        assessmentCloRepository);
                        deleteAllIfPresent(topicCloRepository.findByIdCloId(clo.getId()),
                                        topicCloRepository);
                        deleteAllIfPresent(cloPloMappingRepository.findByCloId(clo.getId()),
                                        cloPloMappingRepository);
                }
                syllabus.getClos().removeAll(removed);
        }

        private Clo findCloForUpdate(
                        List<Clo> existing,
                        List<Clo> retained,
                        CreateSyllabusRequest.CloDTO dto) {

                if (dto.getId() != null) {
                        return findOwnedChildById(existing, retained, dto.getId(), Clo::getId, "CLO");
                }

                Clo match = existing.stream()
                                .filter(candidate -> !retained.contains(candidate))
                                .filter(candidate -> sameText(candidate.getCode(), dto.getCode()))
                                .findFirst()
                                .orElse(null);

                if (match == null && dto.getOrderIndex() != null) {
                        match = existing.stream()
                                        .filter(candidate -> !retained.contains(candidate))
                                        .filter(candidate -> Objects.equals(candidate.getOrderIndex(), dto.getOrderIndex()))
                                        .findFirst()
                                        .orElse(null);
                }
                return match;
        }

        private void reconcileTopics(
                        Syllabus syllabus,
                        List<CreateSyllabusRequest.TopicDTO> requested) {

                List<Topic> existing = new ArrayList<>(syllabus.getTopics());
                List<Topic> retained = new ArrayList<>();

                for (CreateSyllabusRequest.TopicDTO dto : requested) {
                        Topic topic = findTopicForUpdate(existing, retained, dto);
                        if (topic == null) {
                                topic = Topic.builder().syllabus(syllabus).build();
                                syllabus.getTopics().add(topic);
                        }

                        topic.setWeekNumber(dto.getWeekNumber());
                        topic.setOrderInWeek(dto.getOrderInWeek());
                        topic.setName(dto.getName());
                        topic.setNameVn(dto.getNameVn());
                        topic.setTeachingHours(topicHoursOrDefault(dto.getTeachingHours(), 3));
                        topic.setLabHours(topicHoursOrDefault(dto.getLabHours(), 0));
                        topic.setSelfStudyHours(topicHoursOrDefault(dto.getSelfStudyHours(), 6));
                        topic.setTopicType(dto.getTopicType() == null
                                        ? null
                                        : TopicType.valueOf(dto.getTopicType()));
                        topic.setTeachingMethod(dto.getTeachingMethod());
                        topic.setLearningActivity(dto.getLearningActivity());
                        topic.setAssessments(dto.getAssessments());
                        topic.setResources(dto.getResources());
                        topic.setNotes(dto.getNotes());
                        retained.add(topic);
                }

                List<Topic> removed = existing.stream()
                                .filter(topic -> !retained.contains(topic))
                                .toList();
                for (Topic topic : removed) {
                        deleteAllIfPresent(topicCloRepository.findByIdTopicId(topic.getId()),
                                        topicCloRepository);
                }
                syllabus.getTopics().removeAll(removed);
        }

        private Topic findTopicForUpdate(
                        List<Topic> existing,
                        List<Topic> retained,
                        CreateSyllabusRequest.TopicDTO dto) {

                if (dto.getId() != null) {
                        return findOwnedChildById(existing, retained, dto.getId(), Topic::getId, "Topic");
                }

                Topic match = existing.stream()
                                .filter(candidate -> !retained.contains(candidate))
                                .filter(candidate -> Objects.equals(candidate.getWeekNumber(), dto.getWeekNumber())
                                                && Objects.equals(candidate.getOrderInWeek(), dto.getOrderInWeek()))
                                .findFirst()
                                .orElse(null);

                if (match == null) {
                        match = existing.stream()
                                        .filter(candidate -> !retained.contains(candidate))
                                        .filter(candidate -> sameText(candidate.getName(), dto.getName()))
                                        .findFirst()
                                        .orElse(null);
                }
                return match;
        }

        private void reconcileAssessments(
                        Syllabus syllabus,
                        List<CreateSyllabusRequest.AssessmentDTO> requested) {

                List<AssessmentComponent> existing = new ArrayList<>(syllabus.getAssessments());
                List<AssessmentComponent> retained = new ArrayList<>();

                for (CreateSyllabusRequest.AssessmentDTO dto : requested) {
                        AssessmentComponent assessment = findAssessmentForUpdate(existing, retained, dto);
                        if (assessment == null) {
                                assessment = AssessmentComponent.builder().syllabus(syllabus).build();
                                syllabus.getAssessments().add(assessment);
                        }

                        assessment.setName(dto.getName());
                        assessment.setNameVn(dto.getNameVn());
                        assessment.setAssessmentType(dto.getAssessmentType());
                        assessment.setWeightPercent(dto.getWeightPercent() == null ? 0f : dto.getWeightPercent());
                        assessment.setMinScore(dto.getMinScore() == null ? 0f : dto.getMinScore());
                        assessment.setMaxScore(dto.getMaxScore() == null ? 100f : dto.getMaxScore());
                        assessment.setOrderIndex(dto.getOrderIndex() == null ? 1 : dto.getOrderIndex());
                        retained.add(assessment);
                }

                List<AssessmentComponent> removed = existing.stream()
                                .filter(assessment -> !retained.contains(assessment))
                                .toList();

                for (AssessmentComponent assessment : removed) {
                        if (!studentScoreRepository.findByAssessmentComponent_Id(assessment.getId()).isEmpty()) {
                                throw new IllegalStateException(
                                                "Assessment component " + assessment.getId()
                                                                + " cannot be removed because student scores exist.");
                        }
                        deleteAllIfPresent(
                                        assessmentCloRepository.findByAssessmentComponent_Id(assessment.getId()),
                                        assessmentCloRepository);
                }
                syllabus.getAssessments().removeAll(removed);
        }

        private AssessmentComponent findAssessmentForUpdate(
                        List<AssessmentComponent> existing,
                        List<AssessmentComponent> retained,
                        CreateSyllabusRequest.AssessmentDTO dto) {

                if (dto.getId() != null) {
                        return findOwnedChildById(existing, retained, dto.getId(),
                                        AssessmentComponent::getId, "Assessment component");
                }

                AssessmentComponent match = existing.stream()
                                .filter(candidate -> !retained.contains(candidate))
                                .filter(candidate -> sameText(candidate.getName(), dto.getName()))
                                .findFirst()
                                .orElse(null);

                if (match == null && dto.getOrderIndex() != null) {
                        match = existing.stream()
                                        .filter(candidate -> !retained.contains(candidate))
                                        .filter(candidate -> Objects.equals(candidate.getOrderIndex(), dto.getOrderIndex()))
                                        .findFirst()
                                        .orElse(null);
                }
                return match;
        }

        private <T> T findOwnedChildById(
                        List<T> existing,
                        List<T> retained,
                        Integer requestedId,
                        java.util.function.Function<T, Integer> idExtractor,
                        String childType) {

                T match = existing.stream()
                                .filter(candidate -> !retained.contains(candidate))
                                .filter(candidate -> Objects.equals(idExtractor.apply(candidate), requestedId))
                                .findFirst()
                                .orElse(null);

                if (match == null) {
                        throw new IllegalArgumentException(
                                        childType + " id " + requestedId
                                                        + " does not belong to this syllabus or is duplicated.");
                }
                return match;
        }

        private boolean sameText(String left, String right) {
                if (left == null || right == null) {
                        return left == null && right == null;
                }
                return left.trim().equalsIgnoreCase(right.trim());
        }

        private <T, ID> void deleteAllIfPresent(
                        List<T> entities,
                        org.springframework.data.jpa.repository.JpaRepository<T, ID> repository) {
                if (entities != null && !entities.isEmpty()) {
                        repository.deleteAll(entities);
                }
        }
        @Override
        @Transactional
        public void delete(Integer id) {
                syllabusHistoryService.assertDeletable(id);

                Syllabus syllabus = repository.findByIdWithRelations(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found"));

                if (syllabus.getStatus() == SyllabusStatus.APPROVED) {
                        throw new ForbiddenOperationException(
                                        "Approved syllabus cannot be deleted. Archive it instead.");
                }

                syllabusAccessService.assertCanModify(syllabus);
                assertContentIsMutable(syllabus);

                /*
                 * CourseProgram la curriculum data: GIU record, chi bo link syllabus.
                 */
                List<CourseProgram> linkedCoursePrograms =
                                courseProgramRepository.findBySyllabus_Id(id);

                for (CourseProgram courseProgram : linkedCoursePrograms) {
                        courseProgram.setSyllabus(null);
                }

                if (!linkedCoursePrograms.isEmpty()) {
                        courseProgramRepository.saveAll(linkedCoursePrograms);
                        courseProgramRepository.flush();
                }

                /*
                 * ClassSection / Teaching Assignment: GIU record, chi bo link syllabus.
                 */
                List<ClassSection> linkedClassSections =
                                classSectionRepository.findBySyllabusId(id);

                for (ClassSection classSection : linkedClassSections) {
                        classSection.setSyllabus(null);
                }

                if (!linkedClassSections.isEmpty()) {
                        classSectionRepository.saveAll(linkedClassSections);
                        classSectionRepository.flush();
                }

                /*
                 * Xoa CHI snapshot cua syllabus nay.
                 * KHONG xoa SourceDocument / Original Word.
                 */
                deleteOwnedSourceSnapshot(id);

                /*
                 * Xoa structured child data thuoc rieng syllabus.
                 */
                deleteSyllabusDetailData(id);

                /*
                 * Xoa relationship syllabus_book, KHONG xoa Book.
                 */
                syllabusBookRepository.deleteAll(
                                syllabusBookRepository.findBySyllabus_Id(id));

                approvalRequestRepository.deleteAll(
                                approvalRequestRepository.findBySyllabusId(id));

                syllabusImportHistoryRepository.deleteAll(
                                syllabusImportHistoryRepository
                                                .findBySyllabusIdOrderByCreatedAtDesc(id));

                /*
                 * Day tat ca UPDATE/DELETE child xuong DB truoc parent.
                 */
                entityManager.flush();

                repository.deleteById(id);
                repository.flush();
        }

        private void deleteOwnedSourceSnapshot(Integer syllabusId) {

                /*
                 * Xoa truc tiep dung row FK dang chan syllabus.
                 * FOREIGN KEY van bat.
                 *
                 * CHI bang syllabus_source_snapshot bi tac dong.
                 * KHONG xoa source_document.
                 */
                entityManager.flush();

                entityManager.createNativeQuery("""
                                DELETE FROM syllabus_source_snapshot
                                WHERE syllabus_id = :syllabusId
                                """)
                                .setParameter("syllabusId", syllabusId)
                                .executeUpdate();

                Number remaining = (Number) entityManager.createNativeQuery("""
                                SELECT COUNT(*)
                                FROM syllabus_source_snapshot
                                WHERE syllabus_id = :syllabusId
                                """)
                                .setParameter("syllabusId", syllabusId)
                                .getSingleResult();

                if (remaining.longValue() != 0L) {
                        throw new IllegalStateException(
                                        "Source snapshot cleanup failed for syllabus "
                                                        + syllabusId
                                                        + ". The syllabus was NOT deleted.");
                }
        }


        private void deleteSyllabusDetailData(Integer syllabusId) {

                List<AssessmentComponent> assessments = assessmentComponentRepository.findBySyllabusId(syllabusId);

                for (AssessmentComponent assessment : assessments) {
                        studentScoreRepository.deleteAll(
                                        studentScoreRepository.findByAssessmentComponent_Id(
                                                        assessment.getId()));

                        assessmentCloRepository.deleteAll(
                                        assessmentCloRepository.findByAssessmentComponent_Id(
                                                        assessment.getId()));
                }

                assessmentComponentRepository.deleteAll(assessments);
                assessmentComponentRepository.flush();

                List<Topic> topics = topicRepository.findBySyllabusId(syllabusId);

                for (Topic topic : topics) {
                        topicCloRepository.deleteAll(
                                        topicCloRepository.findByIdTopicId(topic.getId()));
                }

                topicRepository.deleteAll(topics);
                topicRepository.flush();

                List<Clo> clos = cloRepository.findBySyllabusId(syllabusId);

                for (Clo clo : clos) {
                        assessmentCloRepository.deleteAll(
                                        assessmentCloRepository.findByClo_Id(clo.getId()));

                        topicCloRepository.deleteAll(
                                        topicCloRepository.findByIdCloId(clo.getId()));

                        cloPloMappingRepository.deleteAll(
                                        cloPloMappingRepository.findByCloId(clo.getId()));
                }

                cloRepository.deleteAll(clos);
                cloRepository.flush();
        }

        @Override
        @Transactional(readOnly = true)
        public SubmissionValidationResponse validateForSubmit(Integer id) {

                Syllabus syllabus = repository.findByIdWithRelations(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Syllabus not found"));

                syllabusAccessService.assertCanModify(syllabus);
                assertContentIsMutable(syllabus);

                return submissionValidationService.validate(syllabus);
        }
@Override
@Transactional
public SyllabusResponse submit(Integer id) {
    repository.lockWorkflow(id);

    Syllabus draft =
            repository.findByIdWithRelations(id)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Syllabus not found"));

    /*
     * Only an authorized Instructor/Admin may submit
     * an editable Draft.
     */
    syllabusAccessService.assertCanModify(draft);
    assertContentIsMutable(draft);

    if (draft.getCourse() == null
            || draft.getCourse().getId() == null) {
        throw new IllegalStateException(
                "The Draft is not linked to a valid course.");
    }

    List<CourseProgram> linkedCoursePrograms =
            courseProgramRepository
                    .findBySyllabus_Id(
                            draft.getId());

    /*
     * =====================================================
     * CANONICAL CURRICULUM CONTEXT
     * =====================================================
     *
     * Do not trust legacy academicYear/semester text.
     *
     * Submitted syllabus identity comes from:
     * CourseProgram -> Program -> Cohort.
     */
    CourseProgram curriculumContext =
            linkedCoursePrograms
                    .stream()
                    .filter(cp ->
                            cp.getProgram() != null
                            && cp.getCohort() != null)
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "The Draft is not linked to a valid Program/Cohort curriculum entry."));

    String canonicalAcademicYear =
            curriculumContext
                    .getCohort()
                    .getName();

    if (canonicalAcademicYear == null
            || canonicalAcademicYear.isBlank()) {
        throw new IllegalStateException(
                "The linked Cohort does not have a valid name.");
    }

    String canonicalProgram =
            curriculumContext
                    .getProgram()
                    .getCode();

    if (canonicalProgram == null
            || canonicalProgram.isBlank()) {
        throw new IllegalStateException(
                "The linked Program does not have a valid code.");
    }

    Integer semesterSuggest = curriculumContext.getSemesterSuggest();
    if (semesterSuggest == null || semesterSuggest < 1 || semesterSuggest > 8) {
        throw new IllegalStateException(
                "The linked curriculum entry must specify a semester from 1 to 8.");
    }
    String canonicalSemester = "Semester " + semesterSuggest;

    syllabusIdentityService.assertAvailable(draft.getCourse(), canonicalProgram, canonicalAcademicYear, canonicalSemester, draft.getId());
    /*
     * Normalize the Draft too, so History will never
     * preserve 2026-2027 / HK1 style legacy metadata.
     */
    draft.setAcademicYear(
            canonicalAcademicYear);

    draft.setProgram(
            canonicalProgram);

    draft.setSemester(
            canonicalSemester);

    /*
     * Resolve the correct Department Head.
     */
    List<UserAccount> deptHeads =
            syllabusAccessService
                    .findActiveDeptHeadsFor(draft);

    /*
     * IMPORTANT:
     * Pending workflow detection must use canonical
     * Program/Cohort semester metadata.
     */
    boolean hasPendingApproval =
            approvalRequestRepository
                    .existsBySyllabus_Course_IdAndSyllabus_ProgramAndSyllabus_AcademicYearAndSyllabus_SemesterAndStatus(
                            draft.getCourse().getId(),
                            canonicalProgram,
                            canonicalAcademicYear,
                            canonicalSemester,
                            ApprovalStatus.PENDING);

    if (hasPendingApproval) {
        throw new IllegalStateException(
                "This course already has a version pending approval.");
    }

    LocalDateTime now = LocalDateTime.now();
    draft.setStatus(SyllabusStatus.SUBMITTED);
    draft.setSubmittedAt(now);
    draft.setUpdatedAt(now);
    draft.setVersionLabel(SyllabusVersion.format(draft.getVersionNumber()));
    repository.saveAndFlush(draft);
    approvalRequestRepository.save(ApprovalRequest.builder().syllabus(draft)
            .syllabusVersionNumber(draft.getVersionNumber())
            .step(ApprovalStep.STEP1_DEPT_HEAD).status(ApprovalStatus.PENDING)
            .requestedBy(draft.getCreatedBy()).createdAt(now).build());
    syllabusHistoryService.capture(draft, "SUBMITTED", syllabusAccessService.currentUser().getUsername());
    workflowNotificationService.notifySubmitted(draft, deptHeads);
    return map(draft);
}
        @Override
@Transactional
public SyllabusResponse createRevisionDraftFromRejected(Integer sourceId) {
    repository.lockWorkflow(sourceId);
    Syllabus syllabus = repository.findByIdWithRelations(sourceId)
            .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found"));
    syllabusAccessService.assertCanModify(syllabus);
    if (syllabus.getStatus() != SyllabusStatus.REJECTED) {
        throw new IllegalStateException("A revision can only start from REJECTED.");
    }
    if (approvalRequestRepository.existsBySyllabusIdAndStatus(sourceId, ApprovalStatus.PENDING)) {
        throw new IllegalStateException("Resolve pending approval requests before starting a revision.");
    }
    syllabus.setVersionNumber(Math.addExact(syllabus.getVersionNumber(), 1));
    syllabus.setVersionLabel(SyllabusVersion.format(syllabus.getVersionNumber()));
    syllabus.setStatus(SyllabusStatus.DRAFT);
    syllabus.setSubmittedAt(null);
    syllabus.setApprovedAt(null);
    syllabus.setApprovedBy(null);
    syllabus.setFinalApprovalDate(null);
    syllabus.setIsCurrent(false);
    syllabus.setUpdatedAt(LocalDateTime.now());
    repository.saveAndFlush(syllabus);
    syllabusHistoryService.capture(syllabus, "REVISION_CREATED", syllabusAccessService.currentUser().getUsername());
    return map(syllabus);
}
        private void assertContentIsMutable(Syllabus syllabus) {
                if (syllabus.getStatus() != SyllabusStatus.DRAFT
                                && syllabus.getStatus() != SyllabusStatus.REVISION_REQUESTED) {
                        throw new IllegalStateException(
                                        "Only a DRAFT or REVISION_REQUESTED syllabus may be changed.");
                }
        }

        @Override
        @Transactional(readOnly = true)
        public List<SyllabusResponse> getByStatus(String status) {

                SyllabusStatus syllabusStatus = SyllabusStatus.valueOf(status.trim().toUpperCase());

                return repository.findByStatusWithRelations(syllabusStatus)
                                .stream()
                                .filter(syllabusAccessService::canView)
                                .map(this::map)
                                .toList();
        }

        /**
         * Canonical safety boundary for visible syllabus comparison.
         *
         * Business rule:
         * - both syllabuses must be APPROVED;
         * - same Course;
         * - same Program;
         * - different Cohorts.
         *
         * The Program/Cohort identity comes from CourseProgram, not from
         * free-text Syllabus.program / academicYear fields.
         */
        private void assertApprovedCohortComparison(
                        Syllabus oldSyllabus,
                        Syllabus newSyllabus) {

                if (oldSyllabus == null || newSyllabus == null) {
                        throw new IllegalArgumentException(
                                        "Two syllabuses are required for comparison.");
                }

                if (Objects.equals(
                                oldSyllabus.getId(),
                                newSyllabus.getId())) {
                        throw new IllegalArgumentException(
                                        "Comparison requires two different cohort syllabuses.");
                }

                if (oldSyllabus.getStatus() != SyllabusStatus.APPROVED
                                || newSyllabus.getStatus() != SyllabusStatus.APPROVED) {
                        throw new IllegalArgumentException(
                                        "Only APPROVED syllabuses can be compared between cohorts.");
                }

                if (oldSyllabus.getCourse() == null
                                || newSyllabus.getCourse() == null
                                || oldSyllabus.getCourse().getId() == null
                                || newSyllabus.getCourse().getId() == null
                                || !Objects.equals(
                                                oldSyllabus.getCourse().getId(),
                                                newSyllabus.getCourse().getId())) {

                        throw new IllegalArgumentException(
                                        "Only syllabuses of the same course can be compared.");
                }

                ComparisonContext oldContext =
                                requireComparisonContext(
                                                oldSyllabus,
                                                "Old");

                ComparisonContext newContext =
                                requireComparisonContext(
                                                newSyllabus,
                                                "New");

                if (!Objects.equals(
                                oldContext.courseId(),
                                newContext.courseId())) {
                        throw new IllegalArgumentException(
                                        "Only syllabuses of the same course can be compared.");
                }

                if (!Objects.equals(
                                oldContext.programId(),
                                newContext.programId())) {
                        throw new IllegalArgumentException(
                                        "Only syllabuses in the same program can be compared.");
                }

                if (Objects.equals(
                                oldContext.cohortId(),
                                newContext.cohortId())) {
                        throw new IllegalArgumentException(
                                        "Comparison requires two different cohorts.");
                }
        }

        private ComparisonContext requireComparisonContext(
                        Syllabus syllabus,
                        String sideLabel) {

                ComparisonContext context =
                                findComparisonContext(
                                                syllabus);

                if (context == null) {
                        throw new IllegalArgumentException(
                                        sideLabel
                                                        + " syllabus is not linked to a valid Program/Cohort curriculum context.");
                }

                return context;
        }

        /**
         * Resolve one logical syllabus context from CourseProgram.
         *
         * A syllabus is allowed to have repeated technical links only when
         * they all represent the same Course + Program + Cohort identity.
         * Conflicting curriculum contexts are rejected instead of selecting
         * an arbitrary first row.
         */
        private ComparisonContext findComparisonContext(
                        Syllabus syllabus) {

                if (syllabus == null
                                || syllabus.getId() == null
                                || syllabus.getCourse() == null
                                || syllabus.getCourse().getId() == null) {
                        return null;
                }

                List<CourseProgram> mappings =
                                courseProgramRepository
                                                .findBySyllabus_Id(
                                                                syllabus.getId())
                                                .stream()
                                                .filter(Objects::nonNull)
                                                .filter(cp ->
                                                                cp.getCourse() != null
                                                                                && cp.getCourse().getId() != null
                                                                                && cp.getProgram() != null
                                                                                && cp.getProgram().getId() != null
                                                                                && cp.getCohort() != null
                                                                                && cp.getCohort().getId() != null)
                                                .filter(cp ->
                                                                Objects.equals(
                                                                                cp.getCourse().getId(),
                                                                                syllabus.getCourse().getId()))
                                                .toList();

                if (mappings.isEmpty()) {
                        return null;
                }

                CourseProgram first =
                                mappings.getFirst();

                ComparisonContext context =
                                new ComparisonContext(
                                                first.getCourse().getId(),
                                                first.getProgram().getId(),
                                                first.getCohort().getId(),
                                                first.getCohort().getEntryYear());

                boolean conflictingContext =
                                mappings.stream()
                                                .anyMatch(cp ->
                                                                !Objects.equals(
                                                                                cp.getCourse().getId(),
                                                                                context.courseId())
                                                                                || !Objects.equals(
                                                                                                cp.getProgram().getId(),
                                                                                                context.programId())
                                                                                || !Objects.equals(
                                                                                                cp.getCohort().getId(),
                                                                                                context.cohortId()));

                if (conflictingContext) {
                        throw new IllegalArgumentException(
                                        "Syllabus is linked to more than one Program/Cohort curriculum context.");
                }

                return context;
        }

        private record ComparisonContext(
                        Integer courseId,
                        Integer programId,
                        Integer cohortId,
                        Integer entryYear) {
        }


        @Override
        @Transactional(readOnly = true)
        public SyllabusDiffResponse getDiff(
                        Integer oldId,
                        Integer newId) {

                Syllabus oldSyllabus = repository.findByIdWithRelations(oldId)
                                .orElseThrow(() -> new ResourceNotFoundException("Old Syllabus not found"));

                Syllabus newSyllabus = repository.findByIdWithRelations(newId)
                                .orElseThrow(() -> new ResourceNotFoundException("New Syllabus not found"));

                syllabusAccessService.assertCanView(oldSyllabus);
                syllabusAccessService.assertCanView(newSyllabus);

                /*
                 * Structural comparison is a comparison BETWEEN TWO APPROVED
                 * COHORT SYLLABUSES, not a revision/version comparison inside
                 * one cohort.
                 */
                assertApprovedCohortComparison(
                                oldSyllabus,
                                newSyllabus);

                return syllabusDiffService.compare(
                                oldSyllabus,
                                newSyllabus);
        }

        @Override
        @Transactional
        public SyllabusResponse clone(
                        Integer id,
                        CloneSyllabusRequest request) {

                if (request == null) {
                        throw new IllegalArgumentException(
                                        "Select a target semester or teaching assignment before cloning the syllabus.");
                }

                Syllabus source = repository.findByIdWithRelations(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found"));

                /*
                 * FR-03.2 + FR-01.5:
                 * - Không nhận userId/createdBy từ client.
                 * - Faculty phải clone vào đúng assignment đang hoạt động của mình.
                 * - Course, năm học và học kỳ đích đều do backend xác thực.
                 */
                SyllabusAccessService.CloneAuthorization authorization = syllabusAccessService.authorizeClone(
                                source,
                                request.getClassSectionId(),
                                request.getAcademicYear(),
                                request.getSemester());

                Integer targetCohortId = request.getCohortId();
                if (!authorization.assignments().isEmpty()) {
                    ClassSection assignment = authorization.assignments().getFirst();
                    if (assignment.getCohort() == null) {
                        throw new IllegalArgumentException("The target teaching assignment must have a Cohort.");
                    }
                    if (targetCohortId != null && !Objects.equals(targetCohortId, assignment.getCohort().getId())) {
                        throw new IllegalArgumentException("The target Cohort must match the teaching assignment.");
                    }
                    targetCohortId = assignment.getCohort().getId();
                }
                CourseProgram targetContext = courseProgramRepository.findByCourse_IdAndCohort_Id(
                        source.getCourse().getId(), targetCohortId).stream()
                        .filter(cp -> cp.getProgram() != null && cp.getCohort() != null
                                && Objects.equals(cp.getProgram().getCode(), source.getProgram()))
                        .findFirst().orElseThrow(() -> new IllegalArgumentException("Select the target Program/Cohort curriculum entry."));
                String targetCohort = targetContext.getCohort().getName();
                String targetProgram = targetContext.getProgram().getCode();
                String targetSemester = targetContext.getSemesterSuggest() == null ? null : "Semester " + targetContext.getSemesterSuggest();
                syllabusIdentityService.assertAvailable(source.getCourse(), targetProgram, targetCohort, targetSemester, null);
                int nextVersion = 1;
                LocalDateTime now = LocalDateTime.now();

                String changeSummary = request.getChangeSummary();
                if (changeSummary == null || changeSummary.isBlank()) {
                        changeSummary = "Cloned from "
                                        + source.getVersionLabel()
                                        + " ("
                                        + source.getAcademicYear()
                                        + " - "
                                        + source.getSemester()
                                        + ")";
                }

                Syllabus draft = Syllabus.builder()
                                .course(authorization.course())
                                .versionNumber(nextVersion)
                                .versionLabel(SyllabusVersion.format(nextVersion))
                                .academicYear(targetCohort)
                                .program(targetProgram)
                                .courseDesignation(source.getCourseDesignation())
                                .courseTypes(source.getCourseTypes())
                                .semester(targetSemester)
                                .language(source.getLanguage())
                                .relation(source.getRelation())
                                .teachingMethods(source.getTeachingMethods())
                                .workloadTotal(source.getWorkloadTotal())
                                .workloadContact(source.getWorkloadContact())
                                .workloadPrivate(source.getWorkloadPrivate())
                                .prerequisites(source.getPrerequisites())
                                .objectives(source.getObjectives())
                                .examForms(source.getExamForms())
                                .examRequirements(source.getExamRequirements())
                                .rubrics(source.getRubrics())
                                .major(source.getMajor())
                                .status(SyllabusStatus.DRAFT)
                                .isCurrent(false)
                                .createdBy(authorization.creator())
                                .approvedBy(null)
                                .submittedAt(null)
                                .approvedAt(null)
                                .notes(source.getNotes())
                                .changeSummary(changeSummary.trim())
                                .createdAt(now)
                                .updatedAt(now)
                                .build();

                draft = repository.saveAndFlush(draft);

                // Deep-copy: Reading List, CLO-PLO, Topic-CLO, Assessment-CLO.
                cloneDetailsFromSource(source.getId(), draft);

                if (!authorization.assignments().isEmpty()) {
                        final Syllabus clonedDraft = draft;
                        authorization.assignments().forEach(assignment -> assignment.setSyllabus(clonedDraft));
                        classSectionRepository.saveAll(authorization.assignments());
                }

                targetContext.setSyllabus(draft);
                courseProgramRepository.save(targetContext);

                repository.flush();
                entityManager.clear();

                return map(repository.findByIdWithRelations(draft.getId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Cloned syllabus not found")));
        }
        @Override
        @Transactional(readOnly = true)
        public SemanticSyllabusDiffResponse getSemanticDiff(
                        Integer oldId,
                        Integer newId) {

                Syllabus oldSyllabus =
                                repository.findByIdWithRelations(oldId)
                                                .orElseThrow(
                                                                () -> new ResourceNotFoundException(
                                                                                "Old Syllabus not found"));

                Syllabus newSyllabus =
                                repository.findByIdWithRelations(newId)
                                                .orElseThrow(
                                                                () -> new ResourceNotFoundException(
                                                                                "New Syllabus not found"));

                syllabusAccessService.assertCanView(oldSyllabus);
                syllabusAccessService.assertCanView(newSyllabus);

                /*
                 * Semantic AI must obey the same comparison boundary as the
                 * deterministic structural diff. Never let the AI endpoint
                 * become a bypass for Draft/same-cohort/different-program
                 * comparisons.
                 */
                assertApprovedCohortComparison(
                                oldSyllabus,
                                newSyllabus);

                return syllabusSemanticComparisonService.compare(
                                oldSyllabus,
                                newSyllabus);
        }
}