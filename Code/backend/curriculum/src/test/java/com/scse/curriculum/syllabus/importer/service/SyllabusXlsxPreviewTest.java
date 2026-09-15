package com.scse.curriculum.syllabus.importer.service;

import com.scse.curriculum.syllabus.importer.parser.SyllabusFileParser;
import com.scse.curriculum.syllabus.importer.parser.SyllabusXlsxParser;
import com.scse.curriculum.syllabus.importer.parser.SyllabusDocxParser;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyllabusXlsxPreviewTest {
    @Mock private List<SyllabusFileParser> syllabusFileParsers;
    @Mock private SyllabusRepository syllabusRepository;
    @Mock private com.scse.curriculum.course.repository.CourseRepository courseRepository;
    @Mock private com.scse.curriculum.courseprogram.repository.CourseProgramRepository courseProgramRepository;
    @Mock private com.scse.curriculum.syllabus.service.SyllabusAccessService syllabusAccessService;
    @Mock private com.scse.curriculum.program.repository.ProgramRepository programRepository;
    @Mock private com.scse.curriculum.cohort.repository.CohortRepository cohortRepository;
    @Mock private com.scse.curriculum.topicclo.repository.TopicCloRepository topicCloRepository;
    @Mock private com.scse.curriculum.assessment.repository.AssessmentCloRepository assessmentCloRepository;
    @Mock private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @Mock private com.scse.curriculum.syllabus.service.SyllabusIdentityService syllabusIdentityService;
    @Mock private com.scse.curriculum.syllabus.history.SyllabusHistoryService syllabusHistoryService;
    @InjectMocks private SyllabusImportServiceImpl service;

    private byte[] completedWorkbook() throws Exception {
        try (var input = Files.newInputStream(Path.of("../../templates/syllabus-import-template.xlsx"));
             var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(input);
             var bytes = new java.io.ByteArrayOutputStream()) {
            for (var row : workbook.getSheet("General Info")) {
                if ("course code".equals(row.getCell(0).toString())) row.getCell(1).setCellValue("IT116IU");
                if ("course name".equals(row.getCell(0).toString())) row.getCell(1).setCellValue("Introduction to Computing");
            }
            workbook.write(bytes);
            return bytes.toByteArray();
        }
    }

    @Test
    void instructorCannotConfirmWorkbookForAnotherCourse() throws Exception {
        var course = com.scse.curriculum.course.entity.Course.builder().id(20).courseCode("IT999IU").build();
        var user = com.scse.curriculum.user.entity.UserAccount.builder()
                .role(com.scse.curriculum.user.entity.UserRole.INSTRUCTOR).build();
        var request = new com.scse.curriculum.syllabus.importer.dto.ConfirmSyllabusImportRequest();
        request.setCourseId(20);
        request.setProgramId(1);
        request.setCohortId(12);
        request.setAssignmentId(30);
        request.setData(new SyllabusXlsxParser().parse(new java.io.ByteArrayInputStream(completedWorkbook()), new java.util.ArrayList<>()));
        when(courseRepository.findById(20)).thenReturn(java.util.Optional.of(course));
        when(syllabusAccessService.authorizeImport(30, course, 1, 12)).thenReturn(
                new com.scse.curriculum.syllabus.service.SyllabusAccessService.CreationAuthorization(
                        user, course, "2026-2027", "HK1", List.of()));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.confirm(request))
                .isInstanceOf(com.scse.curriculum.common.exception.ForbiddenOperationException.class);
        verify(syllabusAccessService).authorizeImport(30, course, 1, 12);
        verifyNoInteractions(syllabusRepository);
    }

    @Test
    void selectsXlsxParserAndReturnsReviewDataWithoutSaving() throws Exception {
        when(syllabusFileParsers.stream()).thenReturn(Stream.of(new SyllabusXlsxParser()));
        var file = new MockMultipartFile("file", "syllabus.xlsx", SyllabusXlsxParser.MIME,
                completedWorkbook());
        var preview = service.preview(file);
        assertThat(preview.isValid()).isTrue();
        assertThat(preview.getErrorCount()).isZero();
        assertThat(preview.getFileName()).isEqualTo("syllabus.xlsx");
        assertThat(preview.getData().getClos()).hasSize(1);
        assertThat(preview.getData().getTopics()).hasSize(1);
        verifyNoInteractions(syllabusRepository);
    }

    @Test
    void existingDocxPreviewStillUsesDocxParser() throws Exception {
        when(syllabusFileParsers.stream()).thenReturn(Stream.of(new SyllabusXlsxParser(), new SyllabusDocxParser()));
        try (var document = new org.apache.poi.xwpf.usermodel.XWPFDocument();
             var bytes = new java.io.ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("Course Name: Introduction to Computing");
            document.createParagraph().createRun().setText("Course Code: IT001IU");
            document.write(bytes);
            var preview = service.preview(new MockMultipartFile("file", "syllabus.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document", bytes.toByteArray()));
            assertThat(preview.getData().getSourceCourseCode()).isEqualTo("IT001IU");
            assertThat(preview.getData().getSourceCourseName()).isEqualTo("Introduction to Computing");
            verifyNoInteractions(syllabusRepository);
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"docx", "xlsx", "pdf"})
    void confirmedImportCreatesCanonicalFirstDraft(String extension) {
        var course = com.scse.curriculum.course.entity.Course.builder().id(20).courseCode("IT116IU").build();
        var user = com.scse.curriculum.user.entity.UserAccount.builder()
                .id(1).role(com.scse.curriculum.user.entity.UserRole.INSTRUCTOR).build();
        var program = com.scse.curriculum.program.entity.Program.builder().id(1).code("CS-2021").build();
        var cohort = com.scse.curriculum.cohort.entity.Cohort.builder().id(12).name("CS2026").program(program).build();
        when(cohortRepository.findById(12)).thenReturn(java.util.Optional.of(cohort));
        var mapping = com.scse.curriculum.courseprogram.entity.CourseProgram.builder().id(246).course(course).program(program).cohort(cohort).semesterSuggest(2).build();
        var request = new com.scse.curriculum.syllabus.importer.dto.ConfirmSyllabusImportRequest();
        request.setCourseId(20);
        request.setProgramId(1);
        request.setCohortId(12);
        request.setAssignmentId(30);
        request.setOriginalFileName("syllabus." + extension);
        request.setOriginalFileType(extension);
        var data = new com.scse.curriculum.syllabus.importer.dto.SyllabusImportData();
        data.setSourceCourseCode("IT116IU");
        data.setObjectives("Source document says Version 1; preserve this content.");
        request.setData(data);
        when(courseRepository.findById(20)).thenReturn(java.util.Optional.of(course));
        when(programRepository.findById(1)).thenReturn(java.util.Optional.of(program));
        when(syllabusAccessService.authorizeImport(30, course, 1, 12)).thenReturn(
                new com.scse.curriculum.syllabus.service.SyllabusAccessService.CreationAuthorization(
                        user, course, "2026-2027", "HK1", List.of()));
        when(courseProgramRepository.findByCourse_IdAndProgram_IdAndCohort_Id(20, 1, 12))
                .thenReturn(java.util.Optional.of(mapping));
        var response = service.confirm(request);
        assertThat(response.getVersionNumber()).isEqualTo(1);
        assertThat(response.getVersionLabel()).isEqualTo("v1.0");
        assertThat(response.getStatus()).isEqualTo("DRAFT");
        var saved = mapping.getSyllabus();
        assertThat(saved.getAcademicYear()).isEqualTo("CS2026");
        assertThat(saved.getProgram()).isEqualTo("CS-2021");
        assertThat(saved.getSemester()).isEqualTo("Semester 2");
        assertThat(org.springframework.test.util.ReflectionTestUtils.getField(saved, "versionLabel"))
                .isEqualTo("v1.0");
        assertThat(saved.getObjectives()).isEqualTo(data.getObjectives());
    }
}
