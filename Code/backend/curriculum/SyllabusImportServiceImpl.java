package com.scse.curriculum.syllabus.importer.service;

import com.scse.curriculum.syllabus.entity.SyllabusVersion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scse.curriculum.book.entity.Book;
import com.scse.curriculum.book.entity.BookType;
import com.scse.curriculum.book.repository.BookRepository;
import com.scse.curriculum.course.repository.CourseRepository;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.entity.CurriculumTerm;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.coursetype.repository.CourseTypeRepository;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.program.repository.ProgramRepository;
import com.scse.curriculum.clo.entity.BloomLevel;
import com.scse.curriculum.clo.entity.CompetencyLevel;
import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.cloplomapping.entity.CloPloMapping;
import com.scse.curriculum.cloplomapping.entity.ContributionLevel;
import com.scse.curriculum.cloplomapping.repository.CloPloMappingRepository;
import com.scse.curriculum.plo.entity.Plo;
import com.scse.curriculum.plo.repository.PloRepository;

import com.scse.curriculum.assessment.entity.AssessmentComponent;
import com.scse.curriculum.assessment.entity.AssessmentClo;
import com.scse.curriculum.assessment.entity.AssessmentCloId;
import com.scse.curriculum.assessment.repository.AssessmentCloRepository;

import com.scse.curriculum.syllabus.dto.SyllabusResponse;

import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusImportStatus;
import com.scse.curriculum.syllabus.entity.SyllabusSourceType;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.syllabus.service.SyllabusAccessService;

import com.scse.curriculum.syllabus.importer.dto.*;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData.*;

import com.scse.curriculum.syllabus.importer.entity.ImportStatus;
import com.scse.curriculum.syllabus.importer.entity.SyllabusImportHistory;

import com.scse.curriculum.syllabus.importer.parser.SyllabusPdfParser;
import com.scse.curriculum.syllabus.importer.parser.SyllabusFileParser;

import com.scse.curriculum.syllabusbook.entity.SyllabusBook;
import com.scse.curriculum.syllabusbook.entity.SyllabusBookId;
import com.scse.curriculum.syllabusbook.entity.UsageType;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.topic.entity.Topic;
import com.scse.curriculum.topicclo.entity.TopicClo;
import com.scse.curriculum.topicclo.entity.TopicCloId;
import com.scse.curriculum.topicclo.entity.TeachingLevel;
import com.scse.curriculum.topicclo.repository.TopicCloRepository;
import com.scse.curriculum.auth.security.CurrentUserService;
import com.scse.curriculum.syllabus.source.entity.SourceDocument;
import com.scse.curriculum.syllabus.source.entity.SyllabusSourceSnapshot;
import com.scse.curriculum.syllabus.source.repository.SourceDocumentRepository;
import com.scse.curriculum.syllabus.source.repository.SyllabusSourceSnapshotRepository;
import com.scse.curriculum.syllabus.importer.parser.ProgramDocumentParser;
import com.scse.curriculum.syllabus.importer.parser.DocxProgramDocumentParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SyllabusImportServiceImpl
        implements SyllabusImportService {

    private final SyllabusRepository syllabusRepository;
    private final com.scse.curriculum.syllabus.service.SyllabusIdentityService syllabusIdentityService;

    private final BookRepository bookRepository;
    private final CourseRepository courseRepository;

    private final SyllabusAccessService syllabusAccessService;
    private final ClassSectionRepository classSectionRepository;
    private final SyllabusPdfParser syllabusPdfParser;
    private final List<SyllabusFileParser> syllabusFileParsers;
    private final CourseProgramRepository courseProgramRepository;
    private final CourseTypeRepository courseTypeRepository;
    private final CohortRepository cohortRepository;
    private final ProgramRepository programRepository;
    private final PloRepository ploRepository;
    private final CloPloMappingRepository cloPloMappingRepository;
    private final TopicCloRepository topicCloRepository;
    private final AssessmentCloRepository assessmentCloRepository;
    private final ObjectMapper objectMapper;
    private final List<ProgramDocumentParser> programDocumentParsers;
    private final SourceDocumentRepository sourceDocumentRepository;
    private final SyllabusSourceSnapshotRepository sourceSnapshotRepository;
    private final CurrentUserService currentUserService;

    /**
     * =====================================================
     * PREVIEW IMPORT
     * =====================================================
     */
    @Override
    @Transactional(readOnly = true)
    public SyllabusImportPreviewResponse preview(
            MultipartFile file) {

        List<SyllabusImportIssue> issues = new ArrayList<>();

        try {

            SyllabusFileParser parser = syllabusFileParsers.stream()
                    .filter(candidate -> candidate.supports(file.getOriginalFilename(), file.getContentType()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unsupported syllabus file. Upload a DOCX, XLSX, or PDF file."));
            SyllabusImportData data = parser.parse(file.getInputStream(), issues);

            return SyllabusImportPreviewResponse.builder()

                    .fileName(
                            file.getOriginalFilename())

                    .fileType(
                            file.getContentType())

                    .valid(
                            issues.stream()
                                    .noneMatch(
                                            i -> "ERROR"
                                                    .equalsIgnoreCase(
                                                            i.getSeverity())))

                    .errorCount(
                            (int) issues.stream()
                                    .filter(
                                            i -> "ERROR"
                                                    .equalsIgnoreCase(
                                                            i.getSeverity()))
                                    .count())

                    .warningCount(
                            (int) issues.stream()
                                    .filter(
                                            i -> "WARNING"
                                                    .equalsIgnoreCase(
                                                            i.getSeverity()))
                                    .count())

                    .data(data)

                    .issues(issues)

                    .build();

        } catch (Exception e) {

            issues.add(
                    SyllabusImportIssue.builder()
                            .severity("ERROR")
                            .message(
                                    "Cannot preview import: "
                                            + e.getMessage())
                            .build());

            return SyllabusImportPreviewResponse.builder()

                    .fileName(
                            file.getOriginalFilename())

                    .fileType(
                            file.getContentType())

                    .valid(false)

                    .errorCount(1)

                    .warningCount(0)

                    .issues(issues)

                    .build();

        }

    }

    @Override
    public BulkSyllabusImportPreviewResponse previewBulk(
            MultipartFile file, Integer programId, Integer cohortId) {
        try {
            if (file == null || file.isEmpty()) throw new IllegalArgumentException("The program document is empty.");
            if (file.getSize() > 200L * 1024 * 1024) throw new IllegalArgumentException("The program document exceeds 200 MB.");
            var program = programRepository.findById(programId)
                    .orElseThrow(() -> new IllegalArgumentException("Program not found"));
            var cohort = cohortRepository.findById(cohortId)
                    .filter(value -> value.getProgram() != null && Objects.equals(value.getProgram().getId(), programId))
                    .orElseThrow(() -> new IllegalArgumentException("Cohort does not belong to the selected program"));
            byte[] content=file.getBytes();
            String filename=sanitizeFilename(file.getOriginalFilename());
            ProgramDocumentParser parser=programDocumentParsers.stream()
                    .filter(candidate -> candidate.supports(filename,file.getContentType(),content))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException(
                            "Only genuine PDF or DOCX program documents are supported."));
            var parsed=parser.parse(content);
            String sourceType=parser.getClass().getSimpleName().startsWith("Docx") ? "DOCX" : "PDF";

            /*
             * Programme dossiers can contain extra/legacy syllabus appendices that are
             * not actually part of the selected curriculum.  For PDF programme
             * documents, derive the official course-code scope from the curriculum
             * course-list tables near the beginning of the document and keep only
             * syllabus sections that belong to that scope.
             *
             * Fail-open rule: if a supported curriculum table cannot be identified,
             * preserve the previous behaviour and return every detected syllabus.
             */
            List<ProgramDocumentParser.ParsedItem> parsedItems = parsed.items();
            if ("PDF".equals(sourceType)) {
                Set<String> curriculumScope =
                        ProgramCurriculumScopeExtractor.extractNormalizedCourseCodes(content);

                if (curriculumScope.size() >= 10) {
                    Set<String> rawDetectedCodes = parsedItems.stream()
                            .filter(item -> item.data() != null)
                            .flatMap(item -> rawNormalizedCourseCodeCandidates(
                                    item.data().getSourceCourseCode()).stream())
                            .collect(Collectors.toCollection(LinkedHashSet::new));

                    List<ProgramDocumentParser.ParsedItem> scopedItems = parsedItems.stream()
                            .filter(item -> item.data() != null
                                    && isOfficialCurriculumSyllabus(
                                            item.data().getSourceCourseCode(),
                                            curriculumScope,
                                            rawDetectedCodes))
                            .toList();

                    if (scopedItems.size() != parsedItems.size()) {
                        log.info(
                                "Bulk programme import scope filter kept {} of {} detected syllabuses for Program {} / Cohort {}.",
                                scopedItems.size(),
                                parsedItems.size(),
                                programId,
                                cohortId);
                    }

                    /*
                     * Some programme dossiers repeat the same detailed syllabus
                     * verbatim in two appendix locations.  Importing both creates
                     * two Syllabus rows for one CourseProgram and the second link
                     * overwrites the first.  Keep exactly one best section per
                     * canonical course code before the UI ever confirms items.
                     */
                    scopedItems = deduplicateParsedSyllabiByCourseCode(scopedItems);

                    /*
                     * A recognized programme dossier is also authoritative evidence
                     * that its listed syllabus belongs to the selected Program/Cohort.
                     *
                     * If the Course already exists in the global Course Catalog but
                     * the exact CourseProgram row is missing, reconcile that mapping
                     * here before confirm.  This prevents repeated
                     * "not part of the selected Program/Cohort" failures for later
                     * cohorts that reuse an existing course.
                     *
                     * Safety boundaries:
                     * - runs only after a reliable official curriculum scope was found;
                     * - creates CourseProgram only for an already-existing unique Course;
                     * - never creates a Course from syllabus text;
                     * - never updates/deletes existing CourseProgram rows;
                     * - unknown/ambiguous catalog codes are left untouched and still
                     *   fail explicitly during confirm.
                     */
                    reconcileExistingCatalogMappingsForOfficialScope(
                            scopedItems,
                            programId,
                            cohortId);

                    parsedItems = scopedItems;
                } else {
                    log.info(
                            "Bulk programme import scope filter skipped because no reliable curriculum course-list table was found.");
                }
            }

            SourceDocument source=sourceDocumentRepository.save(SourceDocument.builder()
                    .originalFilename(filename).contentType(file.getContentType()).fileSize(file.getSize())
                    .sha256(sha256(content)).uploadedAt(LocalDateTime.now()).uploadedBy(currentUserService.getCurrentUser())
                    .program(program).cohort(cohort).content(content).build());
            List<BulkSyllabusImportItem> items = parsedItems.stream().map(section -> {
                List<SyllabusImportIssue> issues = section.issues();
                int errors = (int) issues.stream()
                        .filter(issue -> "ERROR".equalsIgnoreCase(issue.getSeverity())).count();
                int warnings = (int) issues.stream()
                        .filter(issue -> "WARNING".equalsIgnoreCase(issue.getSeverity())).count();
                SyllabusImportPreviewResponse preview = SyllabusImportPreviewResponse.builder()
                        .fileName(file.getOriginalFilename())
                        .fileType(file.getContentType())
                        .valid(errors == 0)
                        .errorCount(errors)
                        .warningCount(warnings)
                        .data(section.data())
                        .issues(issues)
                        .build();
                SyllabusSourceSnapshot snapshot=null;
                if("DOCX".equals(sourceType)) snapshot=sourceSnapshotRepository.save(
                        SyllabusSourceSnapshot.builder().sourceDocument(source)
                                .courseCode(section.data().getSourceCourseCode())
                                .startBoundary(section.startBoundary()).endBoundary(section.endBoundary())
                                .sha256(section.sourceSnapshot()==null?"":sha256(section.sourceSnapshot())).createdAt(LocalDateTime.now())
                                .fieldMap(section.fieldMap()).content(section.sourceSnapshot()==null?new byte[0]:section.sourceSnapshot()).build());
                return BulkSyllabusImportItem.builder()
                        .startPage(section.startBoundary())
                        .endPage(section.endBoundary())
                        .sourceSnapshotId(snapshot == null ? null : snapshot.getId())
                        .sourceType(sourceType)
                        .preview(preview)
                        .build();
            }).toList();
            return BulkSyllabusImportPreviewResponse.builder()
                    .fileName(filename)
                    .pageCount(parsed.unitCount())
                    .syllabusCount(items.size())
                    .sourceDocumentId(source.getId()).sourceType(sourceType)
                    .items(items)
                    .build();
        } catch (Exception exception) {
            throw new IllegalArgumentException("Cannot extract the program document: " + exception.getMessage(), exception);
        }
    }

    private String sanitizeFilename(String value) {
        String name=value==null?"program-document":Paths.get(value).getFileName().toString();
        return name.replaceAll("[\\r\\n\\u0000]", "_").substring(0,Math.min(name.length(),255));
    }

    private String sha256(byte[] content) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)); }
        catch (Exception exception) { throw new IllegalStateException("SHA-256 is unavailable",exception); }
    }

    /**
     * =====================================================
     * CONFIRM IMPORT
     * =====================================================
     */
    @Override
    public SyllabusResponse confirm(
            ConfirmSyllabusImportRequest request) {
        Course requestedCourse = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        SyllabusAccessService.CreationAuthorization authorization =
                syllabusAccessService.authorizeImport(
                        request.getAssignmentId(), requestedCourse,
                        request.getProgramId(), request.getCohortId());

        UserAccount currentUser = authorization.creator();

        /*
         * For administrator/reviewer bulk imports, the selected Program/Cohort
         * curriculum is the authoritative source of Course identity.  Resolve
         * the Course again from that scope using the imported code instead of
         * relying on a client-selected course id or a second independent match.
         *
         * Instructor security is unchanged: instructors must still use the
         * Course derived from their authorized ClassSection.
         */
        Course effectiveCourse = currentUser.getRole() == UserRole.INSTRUCTOR
                ? authorization.course()
                : resolveImportedCourseInSelectedCurriculum(
                        request.getData(),
                        request.getProgramId(),
                        request.getCohortId());

        assertImportedCourseMatches(
                request.getData(),
                effectiveCourse,
                currentUser,
                request.getProgramId(),
                request.getCohortId());

        Integer nextVersion = 1;
        Syllabus syllabus = Syllabus.builder()

.createdBy(currentUser)

.versionNumber(nextVersion)

        .versionLabel(
                SyllabusVersion.format(nextVersion)
        )

        .sourceType(
                detectSourceType(
                        request.getOriginalFileType(), request.getOriginalFileName()
                )
        )

.importStatus(
        SyllabusImportStatus.CONFIRMED
)

.isCurrent(Boolean.FALSE)

.originalFileName(
        request.getOriginalFileName()
)

.originalFileType(
        request.getOriginalFileType()
)

.build();

        // THÊM ĐOẠN NÀY Ở ĐÂY
        syllabus.setCourse(effectiveCourse);

        SyllabusImportData data = request.getData();
        syllabus.setAcademicYear(cohortRepository.findById(request.getCohortId())
                        .orElseThrow(() -> new RuntimeException("Cohort not found")).getName());
        syllabus.setProgram(programRepository.findById(request.getProgramId())
                .orElseThrow(() -> new RuntimeException("Program not found")).getCode());
        updateGeneralInformation(
                syllabus,
                data);
        syllabus.setNotes(buildStandardFormNotes(data));

        /*
         * Update snapshot information
         */

        if (data.getSourceCourseCode() != null) {

            syllabus.setCourseCodeSnapshot(
                    data.getSourceCourseCode());

        }

        if (data.getSourceCourseName() != null) {

            syllabus.setCourseNameSnapshot(
                    data.getSourceCourseName());

        }

        /*
         * MERGE is default
         *
         * Only replace when requested
         */

CourseProgram selected = request.getCourseProgramId() == null
        ? null
        : courseProgramRepository.findById(request.getCourseProgramId()).orElse(null);
Integer curriculumSemester = resolveCurriculumSemester(
        selected,
        request.getProgramId(),
        request.getCohortId(),
        data.getSemester());
syllabus.setSemester(curriculumSemester == null
        ? null
        : "Semester " + curriculumSemester);
CourseProgram scoped = courseProgramRepository
        .findByCourse_IdAndProgram_IdAndCohort_Id(
                effectiveCourse.getId(), request.getProgramId(), request.getCohortId())
        .orElseGet(() -> CourseProgram.builder()
                .course(syllabus.getCourse())
                .program(programRepository.getReferenceById(request.getProgramId()))
                .cohort(cohortRepository.getReferenceById(request.getCohortId()))
                .courseType(selected == null
                        ? courseTypeRepository.findByCode(resolveCourseTypeCode(data.getCourseTypes()))
                                .orElseThrow(() -> new RuntimeException("Default course type not found"))
                        : selected.getCourseType())
                .termCode(selected != null && selected.getTermCode() != null
                        ? selected.getTermCode()
                        : curriculumSemester == null
                                ? null
                                : CurriculumTerm.valueOf("HK" + curriculumSemester))
                .semesterSuggest(curriculumSemester)
                .yearSuggest(selected == null ? null : selected.getYearSuggest())
                .required(selected == null ? Boolean.TRUE : selected.getRequired())
                .build());
if (scoped.getCohort() != null && scoped.getProgram() != null) {
    syllabus.setAcademicYear(scoped.getCohort().getName());
    syllabus.setProgram(scoped.getProgram().getCode());
    syllabus.setSemester(scoped.getSemesterSuggest() == null ? null : "Semester " + scoped.getSemesterSuggest());
}
syllabusIdentityService.assertAvailable(syllabus.getCourse(), syllabus.getProgram(), syllabus.getAcademicYear(), syllabus.getSemester(), null);
syllabusRepository.saveAndFlush(syllabus);
        importClos(
                syllabus,
                data);

        importTopics(
                syllabus,
                data);

        importAssessments(
                syllabus,
                data);

        importReadings(
                syllabus,
                data);

        saveImportHistory(
        syllabus,
        request.getOriginalFileName(),
        request.getOriginalFileType()
);



scoped.setSyllabus(syllabus);
courseProgramRepository.save(scoped);
syllabusRepository.saveAndFlush(syllabus);
authorization.assignments().forEach(assignment -> assignment.setSyllabus(syllabus));
if (!authorization.assignments().isEmpty()) {
    classSectionRepository.saveAll(authorization.assignments());
}
persistImportedCloPloMappings(
        syllabus,
        request.getProgramId(),
        safe(data.getCloPloMappings()));
persistImportedTopicCloMappings(syllabus, data);
persistImportedAssessmentCloMappings(syllabus, data);


return mapToResponse(
        syllabus);

    }

    @Override
    public SyllabusResponse confirmBulkItem(BulkConfirmSyllabusImportRequest request) {
        programRepository.findById(request.getProgramId())
                .orElseThrow(() -> new IllegalArgumentException("Program not found"));
        cohortRepository.findById(request.getCohortId())
                .filter(cohort -> cohort.getProgram() != null
                        && Objects.equals(cohort.getProgram().getId(), request.getProgramId()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cohort does not belong to the selected program"));

        String importedCode = request.getData() == null
                ? null
                : request.getData().getSourceCourseCode();
        if (importedCode == null || importedCode.isBlank()) {
            throw new IllegalArgumentException(
                    "The imported syllabus does not contain a course code.");
        }

        List<CourseProgram> matches = courseProgramRepository
                .findEffectiveByProgramIdAndCohortIdWithRelations(
                        request.getProgramId(), request.getCohortId())
                .stream()
                .filter(mapping -> mapping.getCourse() != null
                        && courseCodesEquivalent(
                                mapping.getCourse().getCourseCode(),
                                importedCode))
                .sorted((left, right) -> Boolean.compare(
                        Objects.equals(right.getCohort() == null ? null : right.getCohort().getId(),
                                request.getCohortId()),
                        Objects.equals(left.getCohort() == null ? null : left.getCohort().getId(),
                                request.getCohortId())))
                .toList();

        Set<Integer> matchedCourseIds = matches.stream()
                .map(mapping -> mapping.getCourse().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (matchedCourseIds.size() > 1) {
            throw new IllegalArgumentException(
                    "Course code " + importedCode
                            + " is ambiguous in the selected Program/Cohort.");
        }
        if (matches.isEmpty()) {
            matches = findUniqueCourseNameAliasInSelectedCurriculum(
                    request.getData(),
                    request.getProgramId(),
                    request.getCohortId());
        }

        if (matches.isEmpty()) {
            boolean existsInCatalog = courseRepository.findAll().stream()
                    .anyMatch(course -> courseCodesEquivalent(
                            course.getCourseCode(),
                            importedCode));
            throw new IllegalArgumentException(existsInCatalog
                    ? "Course " + importedCode
                            + " is not part of the selected Program/Cohort."
                    : "Course " + importedCode
                            + " was not found in the course catalog.");
        }

        CourseProgram resolved = matches.get(0);
        ConfirmSyllabusImportRequest resolvedRequest = new ConfirmSyllabusImportRequest();
        resolvedRequest.setData(request.getData());
        resolvedRequest.setCourseId(resolved.getCourse().getId());
        resolvedRequest.setCourseProgramId(resolved.getId());
        resolvedRequest.setProgramId(request.getProgramId());
        resolvedRequest.setCohortId(request.getCohortId());
        resolvedRequest.setAssignmentId(request.getAssignmentId());
        resolvedRequest.setImportMode(request.getImportMode());
        resolvedRequest.setOriginalFileName(request.getOriginalFileName());
        resolvedRequest.setOriginalFileType(request.getOriginalFileType());
        SyllabusResponse response=confirm(resolvedRequest);
        if(request.getSourceSnapshotId()!=null) {
            SyllabusSourceSnapshot snapshot=sourceSnapshotRepository.findById(request.getSourceSnapshotId())
                    .orElseThrow(() -> new IllegalArgumentException("Source snapshot not found"));
            if(snapshot.getSyllabus()!=null) throw new IllegalArgumentException("Source snapshot is already linked to a syllabus");
            if(!Objects.equals(snapshot.getSourceDocument().getProgram().getId(),request.getProgramId())
                    || !Objects.equals(snapshot.getSourceDocument().getCohort().getId(),request.getCohortId())
                    || !courseCodesEquivalent(snapshot.getCourseCode(), importedCode))
                throw new IllegalArgumentException("Source snapshot does not match the imported Program/Cohort/course");
            if(snapshot.getContent()==null||snapshot.getContent().length==0) {
                DocxProgramDocumentParser docxParser=programDocumentParsers.stream()
                        .filter(DocxProgramDocumentParser.class::isInstance)
                        .map(DocxProgramDocumentParser.class::cast).findFirst()
                        .orElseThrow(() -> new IllegalStateException("DOCX parser is unavailable"));
                try {
                    byte[] snapshotBytes=docxParser.snapshot(snapshot.getSourceDocument().getContent(),
                            snapshot.getStartBoundary(),snapshot.getEndBoundary());
                    snapshot.setContent(snapshotBytes);
                    snapshot.setSha256(sha256(snapshotBytes));
                } catch(java.io.IOException exception) {
                    throw new IllegalArgumentException("Cannot materialize the syllabus Word snapshot",exception);
                }
            }
            snapshot.setSyllabus(syllabusRepository.getReferenceById(response.getId()));
            sourceSnapshotRepository.save(snapshot);
        }
        return response;
    }

    private void assertImportedCourseMatches(
            SyllabusImportData data,
            Course requestedCourse,
            UserAccount currentUser,
            Integer programId,
            Integer cohortId) {
        String importedCode = data == null ? null : data.getSourceCourseCode();
        if (importedCode == null || importedCode.isBlank()) {
            return;
        }
        if (courseCodesEquivalent(
                requestedCourse.getCourseCode(),
                importedCode)) {
            return;
        }

        /*
         * Some official programme specifications contain a course-code typo or
         * legacy alias in the detailed syllabus while the curriculum table uses
         * the canonical code.  Accept that mismatch only when the imported course
         * name resolves to exactly one course in the selected Program/Cohort and
         * that unique course is the requested course.  This is intentionally
         * scoped and conservative: no global name matching and no new course is
         * created from a name alone.
         */
        List<CourseProgram> nameAliases = findUniqueCourseNameAliasInSelectedCurriculum(
                data,
                programId,
                cohortId);
        boolean requestedCourseIsUniqueAlias = nameAliases.size() == 1
                && nameAliases.get(0).getCourse() != null
                && Objects.equals(
                        nameAliases.get(0).getCourse().getId(),
                        requestedCourse.getId());
        if (requestedCourseIsUniqueAlias) {
            log.warn(
                    "Program-document course-code alias resolved: importedCode={} canonicalCode={} programId={} cohortId={}",
                    importedCode,
                    requestedCourse.getCourseCode(),
                    programId,
                    cohortId);
            return;
        }

        String message = currentUser.getRole() == UserRole.INSTRUCTOR
                ? "You are not assigned to this course and cannot create or import its syllabus."
                : "The imported course code does not match the selected course.";
        throw new com.scse.curriculum.common.exception.ForbiddenOperationException(message);
    }

    private List<CourseProgram> findUniqueCourseNameAliasInSelectedCurriculum(
            SyllabusImportData data,
            Integer programId,
            Integer cohortId) {
        String importedName = data == null ? null : data.getSourceCourseName();
        String normalizedImportedName = normalizeCourseNameForMatching(importedName);
        if (normalizedImportedName.isBlank()) {
            return List.of();
        }

        List<CourseProgram> candidates = courseProgramRepository
                .findEffectiveByProgramIdAndCohortIdWithRelations(programId, cohortId)
                .stream()
                .filter(mapping -> mapping.getCourse() != null)
                .filter(mapping -> normalizeCourseNameForMatching(
                        mapping.getCourse().getName())
                        .equals(normalizedImportedName))
                .sorted((left, right) -> Boolean.compare(
                        Objects.equals(right.getCohort() == null ? null : right.getCohort().getId(), cohortId),
                        Objects.equals(left.getCohort() == null ? null : left.getCohort().getId(), cohortId)))
                .toList();

        Set<Integer> courseIds = candidates.stream()
                .map(mapping -> mapping.getCourse().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (courseIds.size() > 1) {
            throw new IllegalArgumentException(
                    "Course name " + importedName
                            + " is ambiguous in the selected Program/Cohort.");
        }
        if (courseIds.isEmpty()) {
            return List.of();
        }
        Integer resolvedCourseId = courseIds.iterator().next();
        return candidates.stream()
                .filter(mapping -> Objects.equals(mapping.getCourse().getId(), resolvedCourseId))
                .toList();
    }

    private String normalizeCourseNameForMatching(String value) {
        if (value == null) {
            return "";
        }
        String normalized = java.text.Normalizer.normalize(
                value,
                java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT)
                .replace('&', ' ')
                .replaceAll("[^A-Z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        /*
         * The CS2021 programme table uses "Chemistry for Engineer" while the
         * detailed syllabus says "Chemistry for Engineers". Normalise only a
         * simple plural on the final word; uniqueness is still enforced inside
         * the selected Program/Cohort before an alias is accepted.
         */
        String[] words = normalized.split(" ");
        if (words.length > 0) {
            int last = words.length - 1;
            String finalWord = words[last];
            if (finalWord.length() > 4
                    && finalWord.endsWith("S")
                    && !finalWord.endsWith("SS")
                    && !finalWord.endsWith("ICS")) {
                words[last] = finalWord.substring(0, finalWord.length() - 1);
                normalized = String.join(" ", words);
            }
        }
        return normalized;
    }

    /**
     * Insert-only reconciliation for a recognized official programme scope.
     *
     * This solves the common case where a later cohort reuses an existing
     * Course Catalog course but its exact CourseProgram row has not yet been
     * seeded.  New/unknown courses are deliberately NOT auto-created here.
     */
    private void reconcileExistingCatalogMappingsForOfficialScope(
            List<ProgramDocumentParser.ParsedItem> items,
            Integer programId,
            Integer cohortId) {

        if (items == null || items.isEmpty()) {
            return;
        }

        for (ProgramDocumentParser.ParsedItem item : items) {
            if (item == null || item.data() == null) {
                continue;
            }

            SyllabusImportData data = item.data();
            String importedCode = data.getSourceCourseCode();
            if (importedCode == null || importedCode.isBlank()) {
                continue;
            }

            List<Course> catalogMatches = courseRepository.findAll().stream()
                    .filter(course -> courseCodesEquivalent(
                            course.getCourseCode(),
                            importedCode))
                    .toList();

            Set<Integer> catalogCourseIds = catalogMatches.stream()
                    .map(Course::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (catalogCourseIds.isEmpty()) {
                // A genuinely new Course must still be reconciled from the
                // official curriculum data before syllabus confirmation.
                continue;
            }

            if (catalogCourseIds.size() > 1) {
                log.warn(
                        "Official-scope auto-reconciliation skipped for importedCode={} because the Course Catalog is ambiguous.",
                        importedCode);
                continue;
            }

            Course course = catalogMatches.get(0);

            boolean exactMappingExists = courseProgramRepository
                    .findByCourse_IdAndProgram_IdAndCohort_Id(
                            course.getId(),
                            programId,
                            cohortId)
                    .isPresent();

            if (exactMappingExists) {
                continue;
            }

            String courseTypeCode =
                    resolveAutoReconciledCourseTypeCode(
                            course,
                            data);

            Integer semester =
                    resolveAutoReconciledSemester(
                            data,
                            courseTypeCode);

            Integer year = semester == null
                    ? null
                    : ((semester + 1) / 2);

            CourseProgram reconciled = CourseProgram.builder()
                    .course(course)
                    .program(programRepository.getReferenceById(programId))
                    .cohort(cohortRepository.getReferenceById(cohortId))
                    .courseType(
                            courseTypeRepository.findByCode(courseTypeCode)
                                    .orElseThrow(() -> new IllegalStateException(
                                            "Course type " + courseTypeCode
                                                    + " is unavailable.")))
                    .termCode(
                            semester == null
                                    ? null
                                    : CurriculumTerm.valueOf(
                                            "HK" + semester))
                    .semesterSuggest(semester)
                    .yearSuggest(year)
                    .required(!"ELECTIVE".equals(courseTypeCode))
                    .build();

            courseProgramRepository.save(reconciled);

            log.info(
                    "Official programme scope auto-reconciled missing CourseProgram: courseCode={} programId={} cohortId={} type={} semester={}.",
                    course.getCourseCode(),
                    programId,
                    cohortId,
                    courseTypeCode,
                    semester);
        }
    }

    /**
     * Resolve the active CourseType for an auto-created exact cohort mapping.
     *
     * Relation-to-curriculum is preferred for ELECTIVE/COMPULSORY because it
     * describes the course inside this programme.  GENERAL is preserved when a
     * known general-education Course already has a GENERAL mapping elsewhere.
     */
    private String resolveAutoReconciledCourseTypeCode(
            Course course,
            SyllabusImportData data) {

        String relation = normalizeCourseNameForMatching(
                data == null ? null : data.getRelation());

        if (relation.contains("ELECTIVE")
                || relation.contains("OPTIONAL")
                || relation.contains("TU CHON")) {
            return "ELECTIVE";
        }

        if (relation.contains("COMPULSORY")
                || relation.contains("REQUIRED")
                || relation.contains("BAT BUOC")) {

            boolean knownGeneralCourse = courseProgramRepository
                    .findByCourse_Id(course.getId())
                    .stream()
                    .anyMatch(mapping -> mapping.getCourseType() != null
                            && "GENERAL".equalsIgnoreCase(
                                    mapping.getCourseType().getCode()));

            return knownGeneralCourse
                    ? "GENERAL"
                    : "COMPULSORY";
        }

        return resolveCourseTypeCode(
                data == null ? null : data.getCourseTypes());
    }

    /**
     * Semester is only auto-populated when the syllabus exposes one unambiguous
     * semester.  Elective-pool courses deliberately remain unplaced.
     */

    
 private Integer resolveAutoReconciledSemester(
        SyllabusImportData data,
        String courseTypeCode) {

    Set<Integer> semesters =
            parseImportedSemesters(
                    data == null
                            ? null
                            : data.getSemester());

    /*
     * If the source file provides a valid semester,
     * preserve it even for ELECTIVE courses.
     */
    if (!semesters.isEmpty()) {
        return semesters
                .iterator()
                .next();
    }

    /*
     * Source semester is missing / N/A.
     * Give the course a stable temporary placement
     * between Semester 4 and Semester 8.
     */
    return resolveTemporarySemester(
            data == null
                    ? null
                    : data.getSourceCourseCode());
}




private Integer resolveTemporarySemester(
        String courseCode) {

    String normalized =
            normalizeCourseCode(
                    courseCode);

    /*
     * Deterministic fallback.
     *
     * Same course code -> same temporary semester.
     * No hard-coded CS2026/course list and no randomness.
     *
     * Range:
     *   4, 5, 6, 7, 8
     */
    if (normalized == null
            || normalized.isBlank()) {
        return 4;
    }

    return 4
            + Math.floorMod(
                    normalized.hashCode(),
                    5);
}
    /**
     * Resolve one and only one Course inside the selected Program/Cohort.
     * This is the authoritative resolver for non-instructor imports.
     */
    private Course resolveImportedCourseInSelectedCurriculum(
            SyllabusImportData data,
            Integer programId,
            Integer cohortId) {

        String importedCode = data == null ? null : data.getSourceCourseCode();
        if (importedCode == null || importedCode.isBlank()) {
            throw new IllegalArgumentException(
                    "The imported syllabus does not contain a course code.");
        }

        List<CourseProgram> matches = courseProgramRepository
                .findEffectiveByProgramIdAndCohortIdWithRelations(programId, cohortId)
                .stream()
                .filter(mapping -> mapping.getCourse() != null
                        && courseCodesEquivalent(
                                mapping.getCourse().getCourseCode(),
                                importedCode))
                .toList();

        Set<Integer> courseIds = matches.stream()
                .map(mapping -> mapping.getCourse().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (courseIds.size() > 1) {
            throw new IllegalArgumentException(
                    "Course code " + importedCode
                            + " is ambiguous in the selected Program/Cohort.");
        }

        if (courseIds.size() == 1) {
            return matches.stream()
                    .map(CourseProgram::getCourse)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseThrow();
        }

        List<CourseProgram> aliases =
                findUniqueCourseNameAliasInSelectedCurriculum(
                        data,
                        programId,
                        cohortId);

        if (aliases.size() == 1 && aliases.get(0).getCourse() != null) {
            return aliases.get(0).getCourse();
        }

        boolean existsInCatalog = courseRepository.findAll().stream()
                .anyMatch(course -> courseCodesEquivalent(
                        course.getCourseCode(),
                        importedCode));

        throw new IllegalArgumentException(
                existsInCatalog
                        ? "Course " + importedCode
                                + " is not part of the selected Program/Cohort."
                        : "Course " + importedCode
                                + " was not found in the course catalog.");
    }

    /**
     * Deterministically remove duplicate detailed syllabus sections for the
     * same canonical course code.  Prefer the section with fewer parser errors;
     * on a tie keep the first occurrence in the source document.
     */
    private List<ProgramDocumentParser.ParsedItem>
            deduplicateParsedSyllabiByCourseCode(
                    List<ProgramDocumentParser.ParsedItem> items) {

        if (items == null || items.isEmpty()) {
            return List.of();
        }

        Map<String, ProgramDocumentParser.ParsedItem> unique =
                new LinkedHashMap<>();
        List<ProgramDocumentParser.ParsedItem> withoutCode =
                new ArrayList<>();

        for (ProgramDocumentParser.ParsedItem item : items) {
            if (item == null || item.data() == null) {
                continue;
            }

            Set<String> candidates =
                    normalizedCourseCodeCandidates(
                            item.data().getSourceCourseCode());

            if (candidates.isEmpty()) {
                withoutCode.add(item);
                continue;
            }

            String key = candidates.stream()
                    .sorted()
                    .collect(Collectors.joining("|"));

            ProgramDocumentParser.ParsedItem existing =
                    unique.get(key);

            if (existing == null) {
                unique.put(key, item);
                continue;
            }

            int existingErrors = countParserErrors(existing);
            int incomingErrors = countParserErrors(item);

            if (incomingErrors < existingErrors) {
                unique.put(key, item);
                log.warn(
                        "Duplicate syllabus section for courseCode={} replaced pages {}-{} with cleaner pages {}-{}.",
                        item.data().getSourceCourseCode(),
                        existing.startBoundary(),
                        existing.endBoundary(),
                        item.startBoundary(),
                        item.endBoundary());
            } else {
                log.warn(
                        "Duplicate syllabus section for courseCode={} ignored at pages {}-{}; keeping pages {}-{}.",
                        item.data().getSourceCourseCode(),
                        item.startBoundary(),
                        item.endBoundary(),
                        existing.startBoundary(),
                        existing.endBoundary());
            }
        }

        List<ProgramDocumentParser.ParsedItem> result =
                new ArrayList<>(unique.values());
        result.addAll(withoutCode);
        return List.copyOf(result);
    }

    private int countParserErrors(
            ProgramDocumentParser.ParsedItem item) {

        return item == null || item.issues() == null
                ? 0
                : (int) item.issues().stream()
                        .filter(issue -> issue != null
                                && "ERROR".equalsIgnoreCase(
                                        issue.getSeverity()))
                        .count();
    }

    /**
     * Raw normalized candidates do not apply known aliases.
     * They are used when comparing a detailed-syllabus appendix against the
     * official curriculum table extracted from the same programme document.
     */
    private Set<String> rawNormalizedCourseCodeCandidates(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }

        return java.util.Arrays.stream(value.split("\\s*[/;,|]\\s*"))
                .map(String::trim)
                .filter(candidate -> !candidate.isBlank())
                .map(this::normalizeCourseCode)
                .filter(candidate -> !candidate.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Include a syllabus when its raw code is explicitly listed by the
     * programme curriculum table.
     *
     * A verified legacy alias is accepted only when the canonical syllabus is
     * NOT already present elsewhere in the same document.  This preserves the
     * CS2021 CHE011IU -> CH011IU case while preventing duplicate import of
     * IT2026 IT155IU when the canonical IT163IU syllabus is already included.
     */
    private boolean isOfficialCurriculumSyllabus(
            String importedCode,
            Set<String> curriculumScope,
            Set<String> rawDetectedCodes) {

        Set<String> rawCandidates = rawNormalizedCourseCodeCandidates(importedCode);
        if (rawCandidates.stream().anyMatch(curriculumScope::contains)) {
            return true;
        }

        Set<String> canonicalCandidates = normalizedCourseCodeCandidates(importedCode);
        for (String canonical : canonicalCandidates) {
            if (curriculumScope.contains(canonical)
                    && !rawDetectedCodes.contains(canonical)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Course-code fields can legitimately contain alternatives, for example:
     *
     * PE008IU/PE008WE
     *
     * Treat each alternative as a candidate and compare candidate sets.
     * This preserves the existing IU-normalization behaviour while avoiding
     * the previous incorrect normalization PE008IUPE008WE.
     */
    private boolean courseCodesEquivalent(
            String left,
            String right) {

        Set<String> leftCandidates =
                normalizedCourseCodeCandidates(
                        left);

        Set<String> rightCandidates =
                normalizedCourseCodeCandidates(
                        right);

        if (leftCandidates.isEmpty()
                || rightCandidates.isEmpty()) {

            return false;
        }

        return leftCandidates.stream()
                .anyMatch(
                        rightCandidates::contains);
    }

    private Set<String> normalizedCourseCodeCandidates(
            String value) {

        if (value == null
                || value.isBlank()) {

            return Set.of();
        }

        return java.util.Arrays.stream(
                        value.split(
                                "\\s*[/;,|]\\s*"))
                .map(
                        String::trim)
                .filter(
                        candidate ->
                                !candidate.isBlank())
                .map(
                        this::normalizeCourseCode)
                .map(
                        this::canonicalizeKnownCourseCodeAlias)
                .filter(
                        candidate ->
                                !candidate.isBlank())
                .collect(
                        Collectors.toCollection(
                                LinkedHashSet::new));
    }

    /**
     * Official CS2021 source inconsistency:
     * - curriculum/catalog canonical code: CH011IU
     * - detailed syllabus code:           CHE011IU
     *
     * After the normal IU suffix removal these become CH011 and CHE011.
     * Canonicalize only this verified pair so every code-based validation path
     * (bulk resolution, confirm validation, source snapshot validation) agrees.
     */
    private String canonicalizeKnownCourseCodeAlias(String normalizedCode) {
        if ("CHE011".equals(normalizedCode)) {
            return "CH011";
        }
        /*
         * IT2026 contains an old duplicate detailed syllabus using IT155IU for
         * "Optimization and Applications", while the official curriculum table
         * and canonical detailed syllabus use IT163IU.
         */
        if ("IT155".equals(normalizedCode)) {
            return "IT163";
        }
        return normalizedCode;
    }

    private String normalizeCourseCode(String value) {
        String normalized = value == null ? "" : value.toUpperCase().replaceAll("[^A-Z0-9]", "");
        return normalized.endsWith("IU")
                ? normalized.substring(0, normalized.length() - 2)
                : normalized;
    }

    @Override
    public CloPloReconciliationResponse reconcileCloPloMappings(
            Integer programId,
            Integer cohortId) {
        programRepository.findById(programId)
                .orElseThrow(() -> new IllegalArgumentException("Program not found"));
        cohortRepository.findById(cohortId)
                .filter(cohort -> cohort.getProgram() != null
                        && Objects.equals(cohort.getProgram().getId(), programId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cohort does not belong to the selected program"));

        int syllabusCount = 0;
        int matrixEntries = 0;
        int existing = 0;
        int inserted = 0;
        int unresolved = 0;
        List<String> unresolvedMappings = new ArrayList<>();

        Set<Integer> processedSyllabi = new LinkedHashSet<>();
        for (CourseProgram courseProgram : courseProgramRepository
                .findByProgramIdAndCohortIdWithRelations(programId, cohortId)) {
            Syllabus linked = courseProgram.getSyllabus();
            if (linked == null || !processedSyllabi.add(linked.getId())) continue;
            syllabusCount++;

            List<SyllabusImportData.CloPloMappingItem> snapshotMappings =
                    readSnapshotCloPloMappings(linked);
            matrixEntries += snapshotMappings.size();
            MappingPersistResult result = persistImportedCloPloMappings(
                    linked, programId, snapshotMappings);
            existing += result.existing();
            inserted += result.inserted();
            unresolved += result.unresolved();
            unresolvedMappings.addAll(result.unresolvedMappings());
        }

        return new CloPloReconciliationResponse(
                syllabusCount, matrixEntries, existing, inserted, unresolved,
                List.copyOf(unresolvedMappings));
    }

    private String resolveCourseTypeCode(String courseTypes) {
        String normalized = courseTypes == null ? "" : courseTypes.toUpperCase(Locale.ROOT);

        /*
         * FR-02.3 / FR-06.5:
         * The active SRS taxonomy is exactly:
         *   GENERAL / COMPULSORY / ELECTIVE
         *
         * Legacy FOUNDATION / CORE / THESIS / INTERNSHIP values are migrated
         * to COMPULSORY by Code/database/migrate_course_type_to_srs_3_groups.sql.
         */
        if (normalized.contains("ELECTIVE") || normalized.contains("OPTIONAL")) {
            return "ELECTIVE";
        }
        if (normalized.contains("GENERAL")) {
            return "GENERAL";
        }
        return "COMPULSORY";
    }


private Integer resolveCurriculumSemester(
        CourseProgram selected,
        Integer programId,
        Integer cohortId,
        String importedSemester) {

    String courseCode =
            selected != null
                    && selected.getCourse() != null
                    ? selected.getCourse().getCourseCode()
                    : null;

    return resolveCurriculumSemester(
            selected,
            programId,
            cohortId,
            importedSemester,
            courseCode);
}

private Integer resolveCurriculumSemester(
        CourseProgram selected,
        Integer programId,
        Integer cohortId,
        String importedSemester,
        String importedCourseCode) {

    Set<Integer> importedSemesters =
            parseImportedSemesters(importedSemester);

    Integer selectedSemester =
            selected != null
                    && selected.getProgram() != null
                    && Objects.equals(
                            selected.getProgram().getId(),
                            programId)
                    && (
                            selected.getCohort() == null
                            || Objects.equals(
                                    selected.getCohort().getId(),
                                    cohortId)
                    )
                    && selected.getSemesterSuggest() != null
                    && selected.getSemesterSuggest() >= 1
                    && selected.getSemesterSuggest() <= 8
                    ? selected.getSemesterSuggest()
                    : null;

    /*
     * 1. One valid semester in uploaded syllabus:
     *    source file is authoritative.
     *
     * 2. Multiple source semesters:
     *    retain the existing curriculum placement when it
     *    belongs to that source set.
     *
     * 3. No semester in source:
     *    preserve an existing curriculum semester.
     *
     * 4. Neither source nor curriculum has a semester:
     *    assign a stable temporary Semester 4-8.
     */
    if (importedSemesters.size() == 1) {
        return importedSemesters
                .iterator()
                .next();
    }

    if (importedSemesters.size() > 1) {

        if (selectedSemester != null
                && importedSemesters.contains(selectedSemester)) {
            return selectedSemester;
        }

        return importedSemesters
                .iterator()
                .next();
    }

    if (selectedSemester != null) {
        return selectedSemester;
    }

    String courseCode = importedCourseCode;

    if ((courseCode == null || courseCode.isBlank())
            && selected != null
            && selected.getCourse() != null) {

        courseCode =
                selected.getCourse()
                        .getCourseCode();
    }

    return resolveTemporarySemester(courseCode);
}

private Set<Integer> parseImportedSemesters(String importedSemester) {
        Set<Integer> values = new LinkedHashSet<>();
        if (importedSemester == null || importedSemester.isBlank()) return values;
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?<!\\d)([1-8])(?!\\d)")
                .matcher(importedSemester);
        while (matcher.find()) values.add(Integer.valueOf(matcher.group(1)));
        return values;
    }

    /**
     * =====================================================
     * IMPORT CLO
     * =====================================================
     */
    private void importClos(
            Syllabus syllabus,
            SyllabusImportData data) {

        int index = 0;
        Set<String> importedCodes = new LinkedHashSet<>();

        for (CloImportData item : safe(data.getClos())) {
            String code = canonicalClo(item.getCode());
            if (code.isBlank() || !importedCodes.add(code)) {
                continue;
            }

            Clo clo = Clo.builder()

                    .syllabus(
                            syllabus)

                    .code(
                            code)

                    .description(
                            item.getDescription())

                    .descriptionVn(
                            item.getDescriptionVn())

                    .orderIndex(
                            item.getOrderIndex() != null
                                    ? item.getOrderIndex()
                                    : index++)

                    .bloomLevel(
                            parseBloomLevel(
                                    item.getBloomLevel()))

                    .competencyLevel(
                            parseCompetencyLevel(
                                    item.getCompetencyLevel()))

                    .build();

            syllabus.getClos()
                    .add(clo);

        }

    }

    /**
     * =====================================================
     * IMPORT TOPICS
     * =====================================================
     */
    private void importTopics(
            Syllabus syllabus,
            SyllabusImportData data) {

        int index = 0;

        for (TopicImportData item : data.getTopics()) {

            Topic topic = Topic.builder()

                    .syllabus(
                            syllabus)

                    .weekNumber(
                            item.getWeekNumber() == null ? index + 1 : item.getWeekNumber())

                    .orderInWeek(item.getOrderInWeek() == null
                            ? 1
                            : Math.max(1, item.getOrderInWeek()))

                    .name(
                            item.getName() == null || item.getName().isBlank()
                                    ? "Imported topic " + (index + 1)
                                    : normalizeImportedTopicName(item.getName()))

                    .nameVn(
                            item.getNameVn())

                    .teachingHours(
                            item.getTeachingHours() == null ? 3 : item.getTeachingHours())

                    .labHours(item.getLabHours() == null ? 0 : item.getLabHours())

                    .selfStudyHours(item.getSelfStudyHours() == null ? 0 : item.getSelfStudyHours())

                    .topicType(com.scse.curriculum.topic.entity.TopicType.LECTURE)

                    .teachingMethod(
                            item.getTeachingMethod())

                    .learningActivity(
                            item.getLearningActivity())

                    .notes(
                            buildTopicNotes(item))

                    .resources(
                            item.getResources())

                    .build();

            syllabus.getTopics()
                    .add(topic);
            index++;

        }

    }

    /**
     * =====================================================
     * IMPORT ASSESSMENT
     * =====================================================
     */
    private void importAssessments(
            Syllabus syllabus,
            SyllabusImportData data) {

        int index = 0;

        for (AssessmentImportData item : data.getAssessments()) {

            AssessmentComponent assessment = AssessmentComponent.builder()

                    .syllabus(
                            syllabus)

                    .name(
                            item.getName())

                    .nameVn(
                            item.getNameVn())

                    .assessmentType(item.getAssessmentType())

                    .weightPercent(
                            item.getWeightPercent() == null ? 0.0f : item.getWeightPercent())

                    .minScore(
                            item.getMinScore() == null ? 0.0f : item.getMinScore())

                    .maxScore(
                            item.getMaxScore() == null ? 100.0f : item.getMaxScore())

                    .orderIndex(
                            item.getOrderIndex() != null
                                    ? item.getOrderIndex()
                                    : index++)

                    .build();

            syllabus.getAssessments()
                    .add(assessment);

        }

    }

    /**
     * =====================================================
     * IMPORT READING LIST
     *
     * ReadingItem
     * |
     * v
     * Book
     * |
     * v
     * SyllabusBook
     *
     * =====================================================
     */
    private void importReadings(
            Syllabus syllabus,
            SyllabusImportData data) {

        int index = 0;

        for (SyllabusImportData.ReadingItem item : data.getReadings()) {

            if (item.getTitle() == null
                    ||
                    item.getTitle().isBlank()) {
                continue;
            }

            Book book = Book.builder()

                    .title(
                            item.getTitle())

                    .author(
                            item.getAuthor())

                    .publisher(
                            item.getPublisher())

                    .year(
                            item.getYear())

                    .bookType(parseBookType(item.getType()))

                    .build();

            bookRepository.save(
                    book);

            SyllabusBook syllabusBook = SyllabusBook.builder()

                    .id(new SyllabusBookId(syllabus.getId(), book.getId()))

                    .syllabus(
                            syllabus)

                    .book(
                            book)

                    .usageType(UsageType.RECOMMENDED)

                    .orderIndex(
                            index++)

                    .build();

            syllabus.getReferences()
                    .add(syllabusBook);

        }

    }

    /**
     * =====================================================
     * IMPORT HISTORY
     *
     * Create after successful import
     *
     * =====================================================
     */
    private void saveImportHistory(
            Syllabus syllabus,
            String fileName,
            String fileType) {

        SyllabusImportHistory history = SyllabusImportHistory.builder()

                .syllabus(
                        syllabus)

                .originalFileName(
                        fileName)

                .originalFileType(
                        fileType)

                .importStatus(
                        ImportStatus.CONFIRMED)

                .build();

        syllabus.getImportHistory()
                .add(history);

    }

    /**
     * =====================================================
     * ENUM PARSER
     * =====================================================
     */

    private BookType parseBookType(String value) {
        if (value == null || value.isBlank()) {
            return BookType.REFERENCE;
        }
        try {
            return BookType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return BookType.REFERENCE;
        }
    }

    private void persistImportedTopicCloMappings(Syllabus syllabus, SyllabusImportData data) {
        List<Topic> topics = syllabus.getTopics();
        Map<String, Clo> clos = syllabus.getClos().stream().collect(Collectors.toMap(
                clo -> canonicalClo(clo.getCode()), clo -> clo, (first, ignored) -> first));
        Set<String> seen = new LinkedHashSet<>();
        List<TopicClo> rows = new ArrayList<>();
        for (TopicCloMappingItem mapping : safe(data.getTopicCloMappings())) {
            Integer sourceIndex = mapping.getTopicIndex();
            if (sourceIndex == null || sourceIndex < 1) continue;
            Topic topic = topics.stream()
                    .filter(candidate -> Objects.equals(candidate.getWeekNumber(), sourceIndex))
                    .findFirst()
                    .orElse(sourceIndex <= topics.size() ? topics.get(sourceIndex - 1) : null);
            if (topic == null) continue;
            Clo clo = clos.get(canonicalClo(mapping.getCloCode()));
            if (topic.getId() == null || clo == null || clo.getId() == null) continue;
            String key = topic.getId() + ":" + clo.getId();
            if (!seen.add(key)) continue;
            rows.add(TopicClo.builder()
                    .id(new TopicCloId(topic.getId(), clo.getId()))
                    .topic(topic)
                    .clo(clo)
                    .teachingLevel(resolveImportedTeachingLevel(topic, data))
                    .build());
        }
        topicCloRepository.saveAll(rows);
    }

    private TeachingLevel resolveImportedTeachingLevel(Topic topic, SyllabusImportData data) {
        TopicImportData sourceTopic = safe(data.getTopics()).stream()
                .filter(candidate -> Objects.equals(candidate.getWeekNumber(), topic.getWeekNumber()))
                .findFirst()
                .orElse(null);
        String sourceLevel = sourceTopic == null ? null
                : text(sourceTopic.getTeachingLevel() != null
                        ? sourceTopic.getTeachingLevel()
                        : sourceTopic.getContentLevel());

        // The official template uses I/T/U while the normalized relation uses
        // I/D/A (Introduce/Develop/Achieve). For combined levels retain the
        // highest demonstrated level so the required relation column remains
        // source-derived rather than filled with an arbitrary default.
        String normalized = sourceLevel.toUpperCase(Locale.ROOT);
        if (normalized.contains("U") || normalized.contains("A")) return TeachingLevel.A;
        if (normalized.contains("T") || normalized.contains("D")) return TeachingLevel.D;
        return TeachingLevel.I;
    }

    private void persistImportedAssessmentCloMappings(Syllabus syllabus, SyllabusImportData data) {
        List<AssessmentComponent> assessments = syllabus.getAssessments();
        Map<String, Clo> clos = syllabus.getClos().stream().collect(Collectors.toMap(
                clo -> canonicalClo(clo.getCode()), clo -> clo, (first, ignored) -> first));
        Set<String> seen = new LinkedHashSet<>();
        List<AssessmentClo> rows = new ArrayList<>();
        for (AssessmentCloMappingItem mapping : safe(data.getAssessmentCloMappings())) {
            Integer sourceIndex = mapping.getAssessmentIndex();
            if (sourceIndex == null || sourceIndex < 1 || sourceIndex > assessments.size()) continue;
            AssessmentComponent assessment = assessments.get(sourceIndex - 1);
            Clo clo = clos.get(canonicalClo(mapping.getCloCode()));
            if (assessment.getId() == null || clo == null || clo.getId() == null) continue;
            String key = assessment.getId() + ":" + clo.getId();
            if (!seen.add(key)) continue;
            rows.add(AssessmentClo.builder()
                    .id(new AssessmentCloId(assessment.getId(), clo.getId()))
                    .assessmentComponent(assessment)
                    .clo(clo)
                    .contributionPercent(mapping.getPercentage() == null
                            ? null : mapping.getPercentage().floatValue())
                    .build());
        }
        assessmentCloRepository.saveAll(rows);
    }

    private BloomLevel parseBloomLevel(
            String value) {

        if (value == null
                ||
                value.isBlank()) {
            return null;
        }

        try {

            return BloomLevel.valueOf(
                    value
                            .trim()
                            .toUpperCase());

        } catch (Exception e) {

            return null;

        }

    }

    private CompetencyLevel parseCompetencyLevel(
            String value) {

        if (value == null
                ||
                value.isBlank()) {
            return null;
        }

        try {

            return CompetencyLevel.valueOf(
                    value
                            .trim()
                            .toUpperCase());

        } catch (Exception e) {

            return null;

        }

    }

    /**
     * =====================================================
     * ENTITY -> RESPONSE
     * =====================================================
     */
    private SyllabusResponse mapToResponse(
            Syllabus syllabus) {

        return SyllabusResponse.builder()

                .id(
                        syllabus.getId())

                .courseId(
                        syllabus.getCourse() != null
                                ? syllabus.getCourse().getId()
                                : null)

                .courseCode(
                        syllabus.getCourseCodeSnapshot())

                .courseName(
                        syllabus.getCourseNameSnapshot())

                .versionNumber(
                        syllabus.getVersionNumber())

                .versionLabel(
                        syllabus.getVersionLabel())

                .academicYear(
                        syllabus.getAcademicYear())

                .program(
                        syllabus.getProgram())

                .semester(
                        syllabus.getSemester())

                .sourceType(
                        syllabus.getSourceType() != null
                                ? syllabus.getSourceType().name()
                                : null)

                .importStatus(
                        syllabus.getImportStatus() != null
                                ? syllabus.getImportStatus().name()
                                : null)

                .status(
                        syllabus.getStatus() != null
                                ? syllabus.getStatus().name()
                                : null)

                .createdAt(
                        syllabus.getCreatedAt())

                .updatedAt(
                        syllabus.getUpdatedAt())

                .build();

    }

    private void updateGeneralInformation(
            Syllabus syllabus,
            SyllabusImportData data) {

        if (data.getCourseDesignation() != null) {
            syllabus.setCourseDesignation(
                    data.getCourseDesignation());
        }

        if (data.getCourseTypes() != null) {
            syllabus.setCourseTypes(
                    data.getCourseTypes());
        }

        if (data.getLanguage() != null) {
            syllabus.setLanguage(
                    normalizeImportedLanguage(data.getLanguage()));
        }

        if (data.getRelation() != null) {
            syllabus.setRelation(
                    data.getRelation());
        }

        if (data.getTeachingMethods() != null) {
            syllabus.setTeachingMethods(
                    normalizeImportedTeachingMethods(data.getTeachingMethods()));
        }

        if (data.getWorkloadTotal() != null) {
            syllabus.setWorkloadTotal(
                    data.getWorkloadTotal());
        }

        if (data.getWorkloadContact() != null) {
            syllabus.setWorkloadContact(
                    data.getWorkloadContact());
        }

        if (data.getWorkloadPrivate() != null) {
            syllabus.setWorkloadPrivate(
                    data.getWorkloadPrivate());
        }

        if (data.getPrerequisites() != null) {
            syllabus.setPrerequisites(
                    data.getPrerequisites());
        }

        if (data.getObjectives() != null) {
            syllabus.setObjectives(
                    data.getObjectives());
        }

        if (data.getExamForms() != null) {
            syllabus.setExamForms(
                    normalizeImportedExamForms(data.getExamForms()));
        }

        if (data.getExamRequirements() != null) {
            syllabus.setExamRequirements(
                    data.getExamRequirements());
        }

        if (data.getMajor() != null) {
    syllabus.setMajor(
            data.getMajor());
}

if (data.getRubricItems() != null
        && !data.getRubricItems().isEmpty()) {

    syllabus.setRubrics(
            serializeImportedRubrics(
                    data.getRubricItems()));
}
if (data.getRubricItems() != null
        && !data.getRubricItems().isEmpty()) {

    syllabus.setRubrics(
            serializeImportedRubrics(
                    data.getRubricItems()));
}

}
private String serializeImportedRubrics(
        List<SyllabusImportData.RubricItem> rubricItems) {

    List<Map<String, Object>> serializedRubrics =
            new ArrayList<>();

    for (SyllabusImportData.RubricItem rubric :
            safe(rubricItems)) {

        if (rubric == null) {
            continue;
        }

        Map<String, Object> serializedRubric =
                new LinkedHashMap<>();

        serializedRubric.put(
                "type",
                text(rubric.getType()));

        serializedRubric.put(
                "title",
                text(rubric.getTitle()));

        /*
         * RubricCriteriaItem currently exposes four normalized
         * scale columns. Keep a deterministic schema that matches
         * SyllabusPdfRenderer.
         */
        serializedRubric.put(
                "scaleLabels",
                List.of(
                        "Level 1",
                        "Level 2",
                        "Level 3",
                        "Level 4"));

        List<Map<String, Object>> serializedCriteria =
                new ArrayList<>();

        for (SyllabusImportData.RubricCriteriaItem criterion :
                safe(rubric.getCriteria())) {

            if (criterion == null) {
                continue;
            }

            Map<String, Object> serializedCriterion =
                    new LinkedHashMap<>();

            serializedCriterion.put(
                    "criterion",
                    text(criterion.getCriterion()));

            serializedCriterion.put(
                    "levels",
                    List.of(
                            text(criterion.getLevel1()),
                            text(criterion.getLevel2()),
                            text(criterion.getLevel3()),
                            text(criterion.getLevel4())));

            serializedCriteria.add(
                    serializedCriterion);
        }

        serializedRubric.put(
                "criteria",
                serializedCriteria);

        serializedRubrics.add(
                serializedRubric);
    }

    Map<String, Object> root =
            new LinkedHashMap<>();

    root.put(
            "rubrics",
            serializedRubrics);

    try {
        return objectMapper.writeValueAsString(
                root);
    } catch (JsonProcessingException exception) {
        throw new IllegalStateException(
                "Cannot preserve imported rubric data",
                exception);
    }
}
private String serializeImportedRubrics(
        List<SyllabusImportData.RubricItem> rubricItems) {

    List<Map<String, Object>> serializedRubrics =
            new ArrayList<>();

    for (SyllabusImportData.RubricItem rubric :
            safe(rubricItems)) {

        if (rubric == null) {
            continue;
        }

        Map<String, Object> serializedRubric =
                new LinkedHashMap<>();

        serializedRubric.put(
                "type",
                text(rubric.getType()));

        serializedRubric.put(
                "title",
                text(rubric.getTitle()));

        serializedRubric.put(
                "scaleLabels",
                List.of(
                        "Level 1",
                        "Level 2",
                        "Level 3",
                        "Level 4"));

        List<Map<String, Object>> serializedCriteria =
                new ArrayList<>();

        for (SyllabusImportData.RubricCriteriaItem criterion :
                safe(rubric.getCriteria())) {

            if (criterion == null) {
                continue;
            }

            Map<String, Object> serializedCriterion =
                    new LinkedHashMap<>();

            serializedCriterion.put(
                    "criterion",
                    text(criterion.getCriterion()));

            serializedCriterion.put(
                    "levels",
                    List.of(
                            text(criterion.getLevel1()),
                            text(criterion.getLevel2()),
                            text(criterion.getLevel3()),
                            text(criterion.getLevel4())));

            serializedCriteria.add(
                    serializedCriterion);
        }

        serializedRubric.put(
                "criteria",
                serializedCriteria);

        serializedRubrics.add(
                serializedRubric);
    }

    Map<String, Object> root =
            new LinkedHashMap<>();

    root.put(
            "rubrics",
            serializedRubrics);

    try {
        return objectMapper.writeValueAsString(
                root);
    } catch (JsonProcessingException exception) {
        throw new IllegalStateException(
                "Cannot preserve imported rubric data",
                exception);
    }
}
    private String normalizeImportedLanguage(String raw) {
        String value = text(raw).replaceAll("\\s+", " ");
        if (value.isBlank()) return null;
        List<String> languages = new ArrayList<>();
        if (value.toLowerCase().contains("english")) languages.add("English");
        if (value.toLowerCase().contains("vietnamese")) languages.add("Vietnamese");
        if (value.toLowerCase().contains("german")) languages.add("German");
        if (value.toLowerCase().contains("french")) languages.add("French");
        if (!languages.isEmpty()) return String.join(" / ", languages);
        return value.length() <= 100 ? value : value.substring(0, 100).trim();
    }

    private String normalizeImportedExamForms(String raw) {
        String value = text(raw).replaceAll("\\s+", " ");
        if (value.isBlank()) return null;
        String lower = value.toLowerCase(java.util.Locale.ROOT);
        int end = value.length();
        for (String marker : List.of(
                "study and examination requirements",
                "study and examination",
                "reading list",
                "rubrics (optional)",
                "rubrics")) {
            int candidate = lower.indexOf(marker);
            if (candidate >= 0 && candidate < end) end = candidate;
        }
        String bounded = value.substring(0, end).trim();
        // A valid examination-form description is short. This cap only protects
        // stale/malformed preview payloads that contain the remainder of a PDF.
        return bounded.length() <= 16_000
                ? bounded
                : bounded.substring(0, 16_000).trim();
    }

    private String normalizeImportedTeachingMethods(String raw) {
        String value = text(raw).replaceAll("\\s+", " ");
        if (value.isBlank()) return null;
        String lower = value.toLowerCase(java.util.Locale.ROOT);
        int end = value.length();
        for (String marker : List.of(
                "workload (incl. contact",
                "(estimated) total workload",
                "estimated total workload",
                "total workload:",
                "contact hours")) {
            int candidate = lower.indexOf(marker);
            if (candidate >= 0 && candidate < end) end = candidate;
        }
        String bounded = value.substring(0, end).trim();
        return bounded.length() <= 16_000
                ? bounded
                : bounded.substring(0, 16_000).trim();
    }

    private String normalizeImportedTopicName(String raw) {
        String value = text(raw).replaceAll("\\s+", " ");
        String lower = value.toLowerCase(java.util.Locale.ROOT);
        int end = value.length();
        for (String marker : List.of(
                "examination forms",
                "study and examination",
                "reading list",
                "rubrics",
                "date revised:",
                "date revised ",
                "ho chi minh city")) {
            int candidate = lower.indexOf(marker);
            if (candidate >= 0 && candidate < end) end = candidate;
        }
        String bounded = value.substring(0, end).trim().replaceFirst("[;.]$", "");
        if (bounded.isBlank()) return "Imported topic";
        return bounded.length() <= 255
                ? bounded
                : bounded.substring(0, 255).trim();
    }

    /**
     * Persist import-only tables in the same JSON schema consumed by the React
     * standard syllabus form. These values do not have dedicated relational
     * columns yet; without this snapshot they disappear after bulk confirm.
     */
    private String buildStandardFormNotes(SyllabusImportData data) {
        Map<String, Object> notes = new LinkedHashMap<>();
        notes.put("schemaVersion", 1);
        notes.put("internalNotes", "");
        notes.put("personResponsible", text(data.getPersonResponsible()));
        notes.put("dateRevised", data.getDateRevised() == null ? "" : data.getDateRevised().toString());
        notes.put("creditPoints", text(data.getCreditPoints()));
        notes.put("lectureCredits", text(data.getLectureCredits()));
        notes.put("laboratoryCredits", text(data.getLaboratoryCredits()));
        notes.put("workloadStudentResponsibility", text(data.getWorkloadStudentResponsibility()));

        Set<String> ploCodes = new LinkedHashSet<>();
        safe(data.getCloPloMappings()).forEach(mapping -> {
            if (!text(mapping.getPloCode()).isBlank()) ploCodes.add(text(mapping.getPloCode()));
        });
        String ploPrefix = ploCodes.stream().anyMatch(code -> code.toUpperCase().startsWith("SLO")) ? "SLO" : "PLO";
        for (int index = 1; index <= 6; index++) ploCodes.add(ploPrefix + index);
        notes.put("ploCodes", new ArrayList<>(ploCodes));

        Map<String, Map<String, String>> cloPloMatrix = new LinkedHashMap<>();
        safe(data.getCloPloMappings()).forEach(mapping -> {
            String clo = canonicalClo(mapping.getCloCode());
            String plo = text(mapping.getPloCode());
            if (!clo.isBlank() && !plo.isBlank()) {
                cloPloMatrix.computeIfAbsent(clo, ignored -> new LinkedHashMap<>())
                        .put(plo, text(mapping.getValue()).toLowerCase());
            }
        });
        notes.put("cloPloMatrix", cloPloMatrix);

        Map<String, Map<String, String>> topicDetails = new LinkedHashMap<>();
        List<TopicImportData> topics = safe(data.getTopics());
        List<WeeklyActivityItem> weekly = safe(data.getWeeklyActivities());
        for (int index = 0; index < topics.size(); index++) {
            TopicImportData topic = topics.get(index);
            int topicNumber = index + 1;
            WeeklyActivityItem activity = weekly.stream()
                    .filter(item -> Objects.equals(item.getWeek(), topic.getWeekNumber()))
                    .findFirst().orElse(null);
            Map<String, String> detail = new LinkedHashMap<>();
            detail.put("clo", safe(data.getTopicCloMappings()).stream()
                    .filter(mapping -> Objects.equals(mapping.getTopicIndex(), topicNumber)
                            || Objects.equals(mapping.getTopicIndex(), topic.getWeekNumber()))
                    .map(mapping -> canonicalClo(mapping.getCloCode()))
                    .filter(value -> !value.isBlank()).distinct().collect(Collectors.joining(", ")));
            if (detail.get("clo").isBlank() && activity != null) detail.put("clo", canonicalCloList(activity.getClo()));
            detail.put("assessments", activity == null ? "" : resolveAssessmentNames(data, activity.getAssessments()));
            detail.put("resources", text(topic.getResources() != null ? topic.getResources()
                    : activity == null ? null : activity.getResources()));
            detail.put("level", text(topic.getTeachingLevel() != null
                    ? topic.getTeachingLevel() : topic.getContentLevel()));
            detail.put("weight", text(topic.getContentWeight() != null
                    ? topic.getContentWeight() : topic.getTeachingHours()));
            topicDetails.put(String.valueOf(index), detail);
        }
        notes.put("topicDetails", topicDetails);

        List<Map<String, Object>> plannedActivities = new ArrayList<>();
        for (int index = 0; index < weekly.size(); index++) {
            WeeklyActivityItem activity = weekly.get(index);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("week", activity.getWeek() == null ? index + 1 : activity.getWeek());
            row.put("topic", text(activity.getTopic()));
            row.put("clo", canonicalCloList(activity.getClo()));
            row.put("assessments", resolveAssessmentNames(data, activity.getAssessments()));
            row.put("learningActivities", text(activity.getLearningActivities()));
            row.put("resources", text(activity.getResources()));
            plannedActivities.add(row);
        }
        notes.put("plannedActivities", plannedActivities);

        Map<String, Map<String, String>> assessmentMatrix = new LinkedHashMap<>();
        List<AssessmentImportData> assessments = safe(data.getAssessments());
        for (int index = 0; index < assessments.size(); index++) {
            AssessmentImportData assessment = assessments.get(index);
            int order = assessment.getOrderIndex() == null ? index + 1 : assessment.getOrderIndex();
            Map<String, String> row = new LinkedHashMap<>();
            for (AssessmentCloMappingItem mapping : safe(data.getAssessmentCloMappings())) {
                if (Objects.equals(mapping.getAssessmentIndex(), order)
                        || Objects.equals(mapping.getAssessmentIndex(), index + 1)) {
                    row.put(canonicalClo(mapping.getCloCode()), text(mapping.getPercentage()));
                }
            }
            assessmentMatrix.put(String.valueOf(index), row);
        }
        notes.put("assessmentCloMatrix", assessmentMatrix);

        notes.put("readings", safe(data.getReadings()).stream().map(reading -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("title", text(reading.getTitle()));
            row.put("author", text(reading.getAuthor()));
            row.put("publisher", text(reading.getPublisher()));
            row.put("year", reading.getYear() == null ? "" : String.valueOf(reading.getYear()));
            row.put("usageType", "REQUIRED");
            return row;
        }).toList());
        notes.put("assessmentPassNote", text(data.getAssessmentPassNote()));
        notes.put("contentNote", text(data.getContentNote()));

        try {
            return objectMapper.writeValueAsString(notes);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot preserve imported syllabus form data", exception);
        }
    }

    private MappingPersistResult persistImportedCloPloMappings(
            Syllabus syllabus,
            Integer programId,
            List<SyllabusImportData.CloPloMappingItem> importedMappings) {
        if (syllabus == null || importedMappings == null || importedMappings.isEmpty()) {
            return new MappingPersistResult(0, 0, 0, List.of());
        }

        Syllabus managed = syllabusRepository.findById(syllabus.getId())
                .orElseThrow(() -> new IllegalArgumentException("Syllabus not found"));
        Map<String, Clo> closByCode = managed.getClos().stream()
                .collect(Collectors.toMap(
                        clo -> canonicalClo(clo.getCode()), clo -> clo,
                        (first, ignored) -> first, LinkedHashMap::new));
        List<Plo> programPlos = ploRepository.findByProgramId(programId).stream()
                .filter(plo -> plo.getIsActive() == null || plo.getIsActive())
                .sorted((left, right) -> Integer.compare(
                        right.getVersionNumber() == null ? 0 : right.getVersionNumber(),
                        left.getVersionNumber() == null ? 0 : left.getVersionNumber()))
                .toList();
        Set<String> existingKeys = cloPloMappingRepository
                .findByClo_Syllabus_Id(managed.getId()).stream()
                .map(mapping -> mapping.getClo().getId() + ":" + mapping.getPlo().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<CloPloMapping> toInsert = new ArrayList<>();
        int existing = 0;
        int unresolved = 0;
        List<String> unresolvedMappings = new ArrayList<>();

        for (SyllabusImportData.CloPloMappingItem imported : importedMappings) {
            Clo clo = closByCode.get(canonicalClo(imported.getCloCode()));
            Plo plo = resolveProgramPlo(programPlos, imported.getPloCode());
            ContributionLevel level = parseContributionLevel(imported.getValue());
            if (clo == null || plo == null || level == null) {
                unresolved++;
                String reason = clo == null ? "CLO_NOT_PERSISTED"
                        : plo == null ? "PLO_NOT_FOUND_IN_PROGRAM"
                        : "INVALID_CONTRIBUTION_LEVEL";
                unresolvedMappings.add(String.format(
                        "%d:%s:%s->%s=%s [%s]",
                        managed.getId(), text(managed.getCourseCodeSnapshot()),
                        text(imported.getCloCode()), text(imported.getPloCode()),
                        text(imported.getValue()), reason));
                continue;
            }
            String key = clo.getId() + ":" + plo.getId();
            if (!existingKeys.add(key)) {
                existing++;
                continue;
            }
            Float weight = imported.getContributionWeight();
            if (weight == null) weight = contributionWeight(level);
            if (weight > 1f) weight /= 100f;
            toInsert.add(CloPloMapping.builder()
                    .clo(clo).plo(plo).level(level).contributionWeight(weight)
                    .notes("Imported from syllabus CLO-PLO matrix").build());
        }
        cloPloMappingRepository.saveAll(toInsert);
        return new MappingPersistResult(
                existing, toInsert.size(), unresolved, List.copyOf(unresolvedMappings));
    }

    private List<SyllabusImportData.CloPloMappingItem> readSnapshotCloPloMappings(
            Syllabus syllabus) {
        List<SyllabusImportData.CloPloMappingItem> result = new ArrayList<>();
        if (syllabus.getNotes() == null || syllabus.getNotes().isBlank()) return result;
        try {
            JsonNode matrix = objectMapper.readTree(syllabus.getNotes()).path("cloPloMatrix");
            if (!matrix.isObject()) return result;
            matrix.fields().forEachRemaining(cloEntry -> {
                if (!cloEntry.getValue().isObject()) return;
                cloEntry.getValue().fields().forEachRemaining(ploEntry -> {
                    String value = ploEntry.getValue().asText("");
                    ContributionLevel level = parseContributionLevel(value);
                    result.add(SyllabusImportData.CloPloMappingItem.builder()
                            .cloCode(cloEntry.getKey()).ploCode(ploEntry.getKey()).value(value)
                            .contributionWeight(level == null ? null : contributionWeight(level))
                            .build());
                });
            });
        } catch (JsonProcessingException exception) {
            log.warn("Cannot read CLO-PLO snapshot for syllabus {}", syllabus.getId(), exception);
        }
        return result;
    }

    private Plo resolveProgramPlo(List<Plo> programPlos, String importedCode) {
        String normalized = normalizeOutcomeCode(importedCode);
        Plo exact = programPlos.stream()
                .filter(plo -> normalizeOutcomeCode(plo.getCode()).equals(normalized))
                .findFirst().orElse(null);
        if (exact != null) return exact;
        Integer ordinal = outcomeOrdinal(normalized);
        if (ordinal == null) return null;
        List<Plo> matches = programPlos.stream()
                .filter(plo -> Objects.equals(
                        outcomeOrdinal(normalizeOutcomeCode(plo.getCode())), ordinal))
                .toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private String normalizeOutcomeCode(String value) {
        return text(value).toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private Integer outcomeOrdinal(String value) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)$").matcher(value);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    private ContributionLevel parseContributionLevel(String value) {
        return switch (text(value).trim().toUpperCase(java.util.Locale.ROOT)) {
            case "X", "I", "1" -> ContributionLevel.I;
            case "XX", "D", "2" -> ContributionLevel.D;
            case "XXX", "A", "3" -> ContributionLevel.A;
            default -> null;
        };
    }

    private float contributionWeight(ContributionLevel level) {
        return switch (level) {
            case I -> 0.3333f;
            case D -> 0.6667f;
            case A -> 1.0f;
        };
    }

    private record MappingPersistResult(
            int existing,
            int inserted,
            int unresolved,
            List<String> unresolvedMappings) {
    }

    private String buildTopicNotes(TopicImportData topic) {
        if (topic.getContentWeight() == null && topic.getTeachingLevel() == null
                && topic.getContentLevel() == null) return topic.getNotes();
        Map<String, Object> notes = new LinkedHashMap<>();
        notes.put("schemaVersion", 1);
        notes.put("contentWeight", topic.getContentWeight() != null
                ? topic.getContentWeight() : topic.getTeachingHours());
        notes.put("contentLevel", topic.getTeachingLevel() != null
                ? topic.getTeachingLevel() : topic.getContentLevel());
        try {
            return objectMapper.writeValueAsString(notes);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot preserve imported content metadata", exception);
        }
    }

    private String resolveAssessmentNames(SyllabusImportData data, String raw) {
        if (raw == null || raw.isBlank()) return "";
        List<String> names = safe(data.getAssessments()).stream()
                .map(AssessmentImportData::getName).filter(Objects::nonNull).toList();
        return java.util.Arrays.stream(raw.split("[,;\\n]+"))
                .map(String::trim).filter(value -> !value.isBlank()).map(value -> {
                    String normalized = value.toUpperCase();
                    return names.stream().filter(name -> {
                        String candidate = name.toUpperCase();
                        if (normalized.contains("MIDTERM")) return candidate.contains("MIDTERM");
                        if (normalized.contains("FINAL")) return candidate.contains("FINAL");
                        if (normalized.contains("QUIZ") || normalized.contains("LAB") || normalized.contains("EXERCISE")) {
                            return candidate.contains("QUIZ") || candidate.contains("ASSIGNMENT")
                                    || candidate.contains("LAB") || candidate.contains("EXERCISE");
                        }
                        return candidate.equals(normalized);
                    }).findFirst().orElse(value);
                }).distinct().collect(Collectors.joining(", "));
    }

    private String canonicalCloList(String raw) {
        if (raw == null || raw.isBlank()) return "";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?:CLO\\s*)?(\\d+)",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(raw);
        Set<String> values = new LinkedHashSet<>();
        while (matcher.find()) values.add("CLO" + matcher.group(1));
        return String.join(", ", values);
    }

    private String canonicalClo(String value) {
        String normalized = text(value).replaceAll("\\s+", "").toUpperCase();
        return normalized.matches("\\d+") ? "CLO" + normalized : normalized;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
    private SyllabusSourceType detectSourceType(
        String fileType, String fileName
){

    if (com.scse.curriculum.syllabus.importer.parser.SyllabusXlsxParser.MIME.equalsIgnoreCase(fileType)
            || fileName != null && fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx")) {
        return SyllabusSourceType.IMPORT_XLSX;
    }

    if(fileType != null
            && fileType.contains("word")){

        return SyllabusSourceType.IMPORT_DOCX;

    }


    return SyllabusSourceType.IMPORT_PDF;
}
}
