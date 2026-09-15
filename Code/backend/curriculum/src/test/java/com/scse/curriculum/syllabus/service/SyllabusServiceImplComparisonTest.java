package com.scse.curriculum.syllabus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.syllabus.comparison.dto.SemanticSyllabusDiffResponse;
import com.scse.curriculum.syllabus.comparison.service.SyllabusSemanticComparisonService;
import com.scse.curriculum.syllabus.dto.SyllabusDiffResponse;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;

@ExtendWith(MockitoExtension.class)
class SyllabusServiceImplComparisonTest {

    @Mock
    private SyllabusRepository repository;

    @Mock
    private CourseProgramRepository courseProgramRepository;

    @Mock
    private SyllabusAccessService syllabusAccessService;

    @Mock
    private SyllabusDiffService syllabusDiffService;

    @Mock
    private SyllabusSemanticComparisonService syllabusSemanticComparisonService;

    @Mock
    private ClassSectionRepository classSectionRepository;

    @InjectMocks
    private SyllabusServiceImpl service;

    private Course course;
    private Program program;
    private Cohort oldCohort;
    private Cohort newCohort;
    private UserAccount creator;

    private Syllabus oldApproved;
    private Syllabus newApproved;

    private CourseProgram oldMapping;
    private CourseProgram newMapping;

    @BeforeEach
    void setUp() {
        course = Course.builder()
                .id(10)
                .courseCode("IT116IU")
                .name("C/C++ Programming")
                .creditTheory(3)
                .creditLab(1)
                .build();

        program = Program.builder()
                .id(20)
                .code("CS")
                .name("Computer Science")
                .build();

        oldCohort = Cohort.builder()
                .id(30)
                .program(program)
                .entryYear(2021)
                .name("CS2021")
                .build();

        newCohort = Cohort.builder()
                .id(31)
                .program(program)
                .entryYear(2026)
                .name("CS2026")
                .build();

        creator = UserAccount.builder()
                .id(40)
                .username("admin")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();

        oldApproved = syllabus(
                100,
                oldCohort,
                SyllabusStatus.APPROVED);

        newApproved = syllabus(
                200,
                newCohort,
                SyllabusStatus.APPROVED);

        oldMapping = mapping(
                1000,
                oldApproved,
                program,
                oldCohort);

        newMapping = mapping(
                2000,
                newApproved,
                program,
                newCohort);
    }

    @Test
    void structuralDiffAllowsApprovedSameCourseSameProgramDifferentCohorts() {
        SyllabusDiffResponse expected =
                SyllabusDiffResponse.builder()
                        .oldSyllabusId(100)
                        .newSyllabusId(200)
                        .hasChanges(true)
                        .build();

        when(repository.findByIdWithRelations(100))
                .thenReturn(Optional.of(oldApproved));

        when(repository.findByIdWithRelations(200))
                .thenReturn(Optional.of(newApproved));

        when(courseProgramRepository.findBySyllabus_Id(100))
                .thenReturn(List.of(oldMapping));

        when(courseProgramRepository.findBySyllabus_Id(200))
                .thenReturn(List.of(newMapping));

        when(syllabusDiffService.compare(
                oldApproved,
                newApproved))
                .thenReturn(expected);

        SyllabusDiffResponse result =
                service.getDiff(100, 200);

        assertThat(result).isSameAs(expected);

        verify(syllabusAccessService)
                .assertCanView(oldApproved);

        verify(syllabusAccessService)
                .assertCanView(newApproved);

        verify(syllabusDiffService)
                .compare(
                        oldApproved,
                        newApproved);
    }

    @Test
    void structuralDiffRejectsDraftEvenWhenCourseAndCohortsOtherwiseMatch() {
        newApproved.setStatus(
                SyllabusStatus.DRAFT);

        when(repository.findByIdWithRelations(100))
                .thenReturn(Optional.of(oldApproved));

        when(repository.findByIdWithRelations(200))
                .thenReturn(Optional.of(newApproved));

        assertThatThrownBy(
                () -> service.getDiff(100, 200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Only APPROVED syllabuses can be compared between cohorts.");

        verifyNoInteractions(
                syllabusDiffService);

        verify(
                courseProgramRepository,
                never())
                .findBySyllabus_Id(100);
    }

    @Test
    void structuralDiffRejectsSameCohort() {
        CourseProgram sameCohortMapping =
                mapping(
                        2000,
                        newApproved,
                        program,
                        oldCohort);

        when(repository.findByIdWithRelations(100))
                .thenReturn(Optional.of(oldApproved));

        when(repository.findByIdWithRelations(200))
                .thenReturn(Optional.of(newApproved));

        when(courseProgramRepository.findBySyllabus_Id(100))
                .thenReturn(List.of(oldMapping));

        when(courseProgramRepository.findBySyllabus_Id(200))
                .thenReturn(
                        List.of(
                                sameCohortMapping));

        assertThatThrownBy(
                () -> service.getDiff(100, 200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Comparison requires two different cohorts.");

        verifyNoInteractions(
                syllabusDiffService);
    }

    @Test
    void structuralDiffRejectsDifferentPrograms() {
        Program otherProgram =
                Program.builder()
                        .id(21)
                        .code("IT")
                        .name("Information Technology")
                        .build();

        Cohort otherProgramCohort =
                Cohort.builder()
                        .id(32)
                        .program(otherProgram)
                        .entryYear(2026)
                        .name("IT2026")
                        .build();

        CourseProgram otherMapping =
                mapping(
                        2001,
                        newApproved,
                        otherProgram,
                        otherProgramCohort);

        when(repository.findByIdWithRelations(100))
                .thenReturn(Optional.of(oldApproved));

        when(repository.findByIdWithRelations(200))
                .thenReturn(Optional.of(newApproved));

        when(courseProgramRepository.findBySyllabus_Id(100))
                .thenReturn(List.of(oldMapping));

        when(courseProgramRepository.findBySyllabus_Id(200))
                .thenReturn(List.of(otherMapping));

        assertThatThrownBy(
                () -> service.getDiff(100, 200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Only syllabuses in the same program can be compared.");

        verifyNoInteractions(
                syllabusDiffService);
    }

    @Test
    void semanticDiffUsesExactlyTheSameApprovedCohortBoundary() {
        newApproved.setStatus(
                SyllabusStatus.UNDER_REVIEW);

        when(repository.findByIdWithRelations(100))
                .thenReturn(Optional.of(oldApproved));

        when(repository.findByIdWithRelations(200))
                .thenReturn(Optional.of(newApproved));

        assertThatThrownBy(
                () -> service.getSemanticDiff(
                        100,
                        200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Only APPROVED syllabuses can be compared between cohorts.");

        verifyNoInteractions(
                syllabusSemanticComparisonService);
    }

    @Test
    void semanticDiffAllowsApprovedSameCourseSameProgramDifferentCohorts() {
        SemanticSyllabusDiffResponse expected =
                SemanticSyllabusDiffResponse.builder()
                        .oldSyllabusId(100)
                        .newSyllabusId(200)
                        .courseId(10)
                        .courseCode("IT116IU")
                        .courseName("C/C++ Programming")
                        .hasMeaningfulChanges(false)
                        .hasUnresolvedItems(false)
                        .build();

        when(repository.findByIdWithRelations(100))
                .thenReturn(Optional.of(oldApproved));

        when(repository.findByIdWithRelations(200))
                .thenReturn(Optional.of(newApproved));

        when(courseProgramRepository.findBySyllabus_Id(100))
                .thenReturn(List.of(oldMapping));

        when(courseProgramRepository.findBySyllabus_Id(200))
                .thenReturn(List.of(newMapping));

        when(syllabusSemanticComparisonService.compare(
                oldApproved,
                newApproved))
                .thenReturn(expected);

        SemanticSyllabusDiffResponse result =
                service.getSemanticDiff(
                        100,
                        200);

        assertThat(result).isSameAs(expected);

        verify(syllabusSemanticComparisonService)
                .compare(
                        oldApproved,
                        newApproved);
    }

    @Test
    void previousComparableReturnsNothingForNonApprovedCurrentSyllabus() {
        newApproved.setStatus(
                SyllabusStatus.DRAFT);

        when(repository.findByIdWithRelations(200))
                .thenReturn(Optional.of(newApproved));

        assertThat(
                service.getPreviousComparable(200))
                .isNull();

        verify(
                courseProgramRepository,
                never())
                .findPreviousComparableCandidates(
                        10,
                        20,
                        2026);
    }

    @Test
    void previousComparableSkipsDraftCandidateAndReturnsNearestViewableApprovedCohort() {
        Cohort draftCohort =
                Cohort.builder()
                        .id(35)
                        .program(program)
                        .entryYear(2025)
                        .name("CS2025")
                        .build();

        Syllabus draftCandidate =
                syllabus(
                        150,
                        draftCohort,
                        SyllabusStatus.DRAFT);

        CourseProgram draftMapping =
                mapping(
                        1500,
                        draftCandidate,
                        program,
                        draftCohort);

        when(repository.findByIdWithRelations(200))
                .thenReturn(Optional.of(newApproved));

        when(courseProgramRepository.findBySyllabus_Id(200))
                .thenReturn(List.of(newMapping));

        when(courseProgramRepository.findPreviousComparableCandidates(
                10,
                20,
                2026))
                .thenReturn(
                        List.of(
                                draftMapping,
                                oldMapping));

        when(syllabusAccessService.canView(
                oldApproved))
                .thenReturn(true);

        /*
         * map(...) resolves the selected syllabus' CourseProgram again.
         */
        when(courseProgramRepository.findBySyllabus_Id(100))
                .thenReturn(List.of(oldMapping));

        when(classSectionRepository.findForPdfBySyllabusId(100))
                .thenReturn(List.of());

        var result =
                service.getPreviousComparable(200);

        assertThat(result).isNotNull();

        assertThat(result.getId())
                .isEqualTo(100);

        assertThat(result.getCohortId())
                .isEqualTo(30);

        assertThat(result.getCohortName())
                .isEqualTo("CS2021");

        assertThat(result.getStatus())
                .isEqualTo("APPROVED");

        verify(
                syllabusAccessService,
                never())
                .canView(
                        draftCandidate);
    }

    @Test
    void comparisonRejectsAmbiguousProgramCohortContextInsteadOfPickingFirstMapping() {
        Program otherProgram =
                Program.builder()
                        .id(21)
                        .code("SE")
                        .name("Software Engineering")
                        .build();

        Cohort otherCohort =
                Cohort.builder()
                        .id(33)
                        .program(otherProgram)
                        .entryYear(2021)
                        .name("SE2021")
                        .build();

        CourseProgram conflicting =
                mapping(
                        1001,
                        oldApproved,
                        otherProgram,
                        otherCohort);

        when(repository.findByIdWithRelations(100))
                .thenReturn(Optional.of(oldApproved));

        when(repository.findByIdWithRelations(200))
                .thenReturn(Optional.of(newApproved));

        when(courseProgramRepository.findBySyllabus_Id(100))
                .thenReturn(
                        List.of(
                                oldMapping,
                                conflicting));

        assertThatThrownBy(
                () -> service.getDiff(100, 200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Syllabus is linked to more than one Program/Cohort curriculum context.");

        verifyNoInteractions(
                syllabusDiffService);
    }

    private Syllabus syllabus(
            int id,
            Cohort cohort,
            SyllabusStatus status) {

        return Syllabus.builder()
                .id(id)
                .course(course)
                .versionNumber(1)
                .versionLabel("v1.0")
                .academicYear(cohort.getName())
                .program(program.getCode())
                .semester("Semester 2")
                .status(status)
                .isCurrent(
                        status
                        == SyllabusStatus.APPROVED)
                .createdBy(creator)
                .clos(List.of())
                .topics(List.of())
                .assessments(List.of())
                .build();
    }

    private CourseProgram mapping(
            int id,
            Syllabus syllabus,
            Program mappingProgram,
            Cohort cohort) {

        return CourseProgram.builder()
                .id(id)
                .course(course)
                .program(mappingProgram)
                .cohort(cohort)
                .syllabus(syllabus)
                .semesterSuggest(2)
                .yearSuggest(
                        cohort.getEntryYear())
                .build();
    }
}
