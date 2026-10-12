package com.scse.curriculum.syllabus.importer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scse.curriculum.book.entity.Book;
import com.scse.curriculum.book.repository.BookRepository;
import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.course.repository.CourseRepository;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.entity.CurriculumTerm;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.coursetype.entity.CourseType;
import com.scse.curriculum.coursetype.repository.CourseTypeRepository;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.program.repository.ProgramRepository;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.importer.dto.*;
import com.scse.curriculum.syllabus.importer.parser.SyllabusPdfParser;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.syllabus.service.SyllabusAccessService;
import com.scse.curriculum.syllabus.source.entity.SourceDocument;
import com.scse.curriculum.syllabus.source.repository.SourceDocumentRepository;
import com.scse.curriculum.syllabus.template.TargetTemplateProfileResolver;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real service and real source parser; repositories are isolated in-memory mocks. */
class Ds2021All45ImportTest {
    private final SyllabusImportServiceImpl service = mock(SyllabusImportServiceImpl.class, CALLS_REAL_METHODS);
    private final CourseProgramRepository mappings = mock(CourseProgramRepository.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final SourceDocumentRepository documents = mock(SourceDocumentRepository.class);
    private final SyllabusAccessService access = mock(SyllabusAccessService.class);
    private final Program program = Program.builder().id(3).code("DS").build();
    private final Cohort cohort = Cohort.builder().id(7).name("DS2021").entryYear(2021).program(program).build();
    private final Course cloud = Course.builder().id(115).courseCode("IT164IU").name("Cloud Computing").build();
    private final UserAccount admin = UserAccount.builder().id(1).role(UserRole.ADMIN).build();
    private final List<CourseProgram> rows = new ArrayList<>();
    private SourceDocument source;

    private void set(String name, Object value) throws Exception {
        var field = SyllabusImportServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true); field.set(service, value);
    }

    @BeforeEach void setup() throws Exception {
        // Mock all repositories/guards: no Spring context, JDBC, HTTP or database writes.
        for (var field : SyllabusImportServiceImpl.class.getDeclaredFields()) {
            if (field.getType().getName().startsWith("com.scse.")) set(field.getName(), mock(field.getType()));
        }
        set("objectMapper", new ObjectMapper()); set("targetTemplateProfileResolver", new TargetTemplateProfileResolver());
        set("courseProgramRepository", mappings); set("courseRepository", courses);
        set("sourceDocumentRepository", documents); set("syllabusAccessService", access);
        var programs = mock(ProgramRepository.class); var cohorts = mock(CohortRepository.class);
        set("programRepository", programs); set("cohortRepository", cohorts);
        when(programs.findById(3)).thenReturn(Optional.of(program)); when(programs.getReferenceById(3)).thenReturn(program);
        when(cohorts.findById(7)).thenReturn(Optional.of(cohort)); when(cohorts.getReferenceById(7)).thenReturn(cohort);
        var types = mock(CourseTypeRepository.class); set("courseTypeRepository", types);
        when(types.findByCode(anyString())).thenAnswer(call -> Optional.of(CourseType.builder().id(1).code(call.getArgument(0)).build()));
        try (var fixture = getClass().getResourceAsStream("/ds2021-effective-mappings.json")) {
            for (var row : new ObjectMapper().readTree(fixture)) {
                Course course = Course.builder().id(row.path("courseId").asInt()).courseCode(row.path("courseCode").asText())
                        .name(row.path("courseName").asText()).build();
                rows.add(CourseProgram.builder().id(row.path("id").asInt()).course(course).program(program)
                        .cohort(row.path("cohortId").isNull() ? null : cohort).build());
            }
        }
        List<Course> catalog = new ArrayList<>(rows.stream().map(CourseProgram::getCourse).toList()); catalog.add(cloud);
        when(courses.findAll()).thenReturn(catalog);
        when(courses.findById(anyInt())).thenAnswer(call -> catalog.stream().filter(c -> c.getId().equals(call.getArgument(0))).findFirst());
        when(courses.lockIdentity(115)).thenReturn(Optional.of(cloud));
        when(mappings.findEffectiveByProgramIdAndCohortIdWithRelations(3,7)).thenAnswer(call -> List.copyOf(rows));
        when(mappings.findByCourse_IdAndProgram_IdAndCohort_Id(anyInt(),eq(3),eq(7))).thenAnswer(call -> rows.stream()
                .filter(row -> row.getCourse().getId().equals(call.getArgument(0)) && row.getCohort() != null).findFirst());
        when(mappings.findById(anyInt())).thenAnswer(call -> rows.stream().filter(row -> row.getId().equals(call.getArgument(0))).findFirst());
        when(mappings.save(any())).thenAnswer(call -> {
            CourseProgram row = call.getArgument(0);
            if (row.getId() == null) { row.setId(900 + rows.size()); rows.add(row); }
            return row;
        });
        when(access.authorizeImport(any(),any(),eq(3),eq(7))).thenAnswer(call ->
                new SyllabusAccessService.CreationAuthorization(admin,call.getArgument(1),null,null,List.of()));
        var syllabuses = mock(SyllabusRepository.class); set("syllabusRepository", syllabuses);
        var sequence = new AtomicInteger(10000);
        var saved = new HashMap<Integer,Syllabus>();
        when(syllabuses.findById(anyInt())).thenAnswer(call -> Optional.ofNullable(saved.get(call.getArgument(0))));
        when(syllabuses.saveAndFlush(any())).thenAnswer(call -> {
            Syllabus syllabus = call.getArgument(0);
            if (syllabus.getId() == null) syllabus.setId(sequence.incrementAndGet());
            syllabus.getClos().forEach(clo -> { if (clo.getId() == null) clo.setId(sequence.incrementAndGet()); });
            syllabus.getTopics().forEach(topic -> { if (topic.getId() == null) topic.setId(sequence.incrementAndGet()); });
            syllabus.getAssessments().forEach(assessment -> { if (assessment.getId() == null) assessment.setId(sequence.incrementAndGet()); });
            saved.put(syllabus.getId(),syllabus);
            return syllabus;
        });
        var books = mock(BookRepository.class); set("bookRepository", books);
        when(books.save(any())).thenAnswer(call -> { Book book = call.getArgument(0); book.setId(sequence.incrementAndGet()); return book; });
        source = SourceDocument.builder().id(100L).program(program).cohort(cohort).contentType("application/pdf")
                .sha256("8c71516bd33227bc95a10844dda52efdf0c48e20cde127a74596d696a405a15e").build();
        when(documents.findById(100L)).thenReturn(Optional.of(source));
    }

    private byte[] originalPdf() throws Exception {
        Path path = Path.of(System.getProperty("ds2021.source", "../../../Tài Liệu/DS Program Specification - Full.pdf"));
        assumeTrue(Files.isRegularFile(path), "Local PDF regression requires -Dds2021.source=<original PDF>");
        byte[] bytes = Files.readAllBytes(path); source.setContent(bytes); return bytes;
    }

    private BulkConfirmSyllabusImportRequest request() {
        var request = new BulkConfirmSyllabusImportRequest(); request.setProgramId(3); request.setCohortId(7);
        request.setSourceDocumentId(100L); request.setOriginalFileType("application/pdf"); request.setOriginalFileName("DS Program Specification - Full.pdf");
        var data = new SyllabusImportData(); data.setSourceCourseCode("IT164IU"); data.setSourceCourseName("Cloud Computing");
        data.setSemester("Semester 7"); data.setCourseTypes("Elective"); request.setData(data); return request;
    }

    @Test void all45RealPdfSectionsConfirmThroughRealServiceUsingCurrentDsMappings() throws Exception {
        var batch = new SyllabusPdfParser().parsePdfBatchWithMetadata(new java.io.ByteArrayInputStream(originalPdf()));
        assertEquals(457,batch.pageCount()); assertEquals(45,batch.sections().size());
        var imported = new LinkedHashSet<Integer>();
        for (var section : batch.sections()) {
            assertTrue(section.issues().stream().noneMatch(issue -> "ERROR".equals(issue.getSeverity())));
            var request = request(); request.setData(section.data());
            var result = service.confirmBulkItem(request);
            assertTrue(imported.add(result.getId()), "Each section produces its own syllabus");
        }
        assertEquals(45, imported.size());
        assertEquals(45, rows.stream().filter(row -> row.getSyllabus() != null).count());
    }

    @Test void cloudMappingUsesItsOwnIdentityAndVerifiedElectiveSemester() throws Exception {
        originalPdf(); var mobile = rows.stream().filter(row -> row.getCourse().getId().equals(75)).findFirst().orElseThrow();
        var result = service.confirmBulkItem(request());
        var row = rows.stream().filter(mapping -> mapping.getCourse().getId().equals(115)).findFirst().orElseThrow();
        assertEquals(result.getId(),row.getSyllabus().getId()); assertSame(cloud,row.getCourse());
        assertEquals("ELECTIVE",row.getCourseType().getCode()); assertEquals(7,row.getSemesterSuggest());
        assertEquals(CurriculumTerm.HK7,row.getTermCode()); assertFalse(row.getRequired());
        assertNull(mobile.getSyllabus()); assertEquals("IT147IU",mobile.getCourse().getCourseCode());
        var order = inOrder(access,courses,mappings);
        order.verify(access).authorizeImport(any(),eq(cloud),eq(3),eq(7));
        order.verify(courses).lockIdentity(115); order.verify(mappings).save(row);
        verify(courses,never()).save(any()); verify(documents,never()).save(any());
    }

    @ParameterizedTest @ValueSource(strings={"CS2021","IT2021","DS2026","DS2027"})
    void doesNotAddMappingForAnyOtherCohort(String name) {
        cohort.setName(name);
        assertThrows(IllegalArgumentException.class, () -> service.confirmBulkItem(request()));
        verify(mappings,never()).save(any()); verifyNoInteractions(documents);
    }

    @Test void requiresStoredSourceInSameScopeAndChecksActualBytesAsWellAsMetadata() throws Exception {
        originalPdf(); source.setContent("different PDF".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> service.confirmBulkItem(request()));
        verify(mappings,never()).save(any()); verifyNoInteractions(access);
        originalPdf(); source.setCohort(Cohort.builder().id(17).build());
        assertThrows(IllegalArgumentException.class, () -> service.confirmBulkItem(request()));
        verify(mappings,never()).save(any());
    }

    @Test void missingPreviewAndUpdateModeCannotCreateMapping() {
        var request = request(); request.setSourceDocumentId(null);
        assertThrows(IllegalArgumentException.class, () -> service.confirmBulkItem(request));
        request.setSourceDocumentId(100L); request.setImportMode("UPDATE");
        assertThrows(IllegalArgumentException.class, () -> service.confirmBulkItem(request));
        verify(mappings,never()).save(any()); verifyNoInteractions(documents);
    }

    @Test void new2027ProfileNeverUsesThisLegacyExceptionEvenWithDs2021Name() {
        cohort.setEntryYear(2027);
        assertThrows(IllegalArgumentException.class, () -> service.confirmBulkItem(request()));
        verify(mappings,never()).save(any()); verifyNoInteractions(documents);
    }

    @Test void administratorPermissionIsRequiredBeforeLockOrInsert() throws Exception {
        originalPdf(); admin.setRole(UserRole.INSTRUCTOR);
        assertThrows(com.scse.curriculum.common.exception.ForbiddenOperationException.class, () -> service.confirmBulkItem(request()));
        verify(courses,never()).lockIdentity(anyInt()); verify(mappings,never()).save(any());
    }

    @Test void ambiguousCatalogIdentityDoesNotChooseOrInsertAnything() throws Exception {
        originalPdf(); when(courses.findAll()).thenReturn(List.of(cloud,Course.builder().id(999).courseCode("IT164").name("Cloud Computing").build()));
        assertThrows(IllegalArgumentException.class, () -> service.confirmBulkItem(request()));
        verifyNoInteractions(access); verify(mappings,never()).save(any());
    }

    @Test void concurrentExistingMappingIsReusedAndExistingSyllabusIsProtected() throws Exception {
        originalPdf(); var existing = CourseProgram.builder().id(999).course(cloud).program(program).cohort(cohort).build();
        when(mappings.findByCourse_IdAndProgram_IdAndCohort_Id(115,3,7)).thenReturn(Optional.of(existing));
        // Route alone: confirm remains the established implementation in other tests.
        doReturn(com.scse.curriculum.syllabus.dto.SyllabusResponse.builder().id(1).build()).when(service).confirm(any());
        service.confirmBulkItem(request()); verify(mappings,never()).save(any());
        existing.setSyllabus(Syllabus.builder().id(777).build());
        assertThrows(IllegalStateException.class, () -> service.confirmBulkItem(request()));
        verify(service,times(1)).confirm(any()); verify(mappings,never()).save(any());
        assertEquals(777,existing.getSyllabus().getId());
    }
}
