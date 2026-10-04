package com.scse.curriculum.courseprogram.service;

import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.cohort.service.CohortOperationalGuard;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.course.repository.CourseRepository;
import com.scse.curriculum.courseprogram.dto.CreateCourseProgramRequest;
import com.scse.curriculum.courseprogram.dto.UpdateCourseProgramRequest;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.coursetype.repository.CourseTypeRepository;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.program.repository.ProgramRepository;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseProgramServiceImplArchiveGuardTest {

    @Mock
    private SyllabusRepository syllabusRepository;

    @Mock
    private CourseProgramRepository repository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private CohortOperationalGuard cohortOperationalGuard;

    @Mock
    private CourseTypeRepository courseTypeRepository;

    @InjectMocks
    private CourseProgramServiceImpl service;

    private Course course;
    private Program program;
    private Cohort archivedCohort;
    private CourseProgram courseProgram;

    @BeforeEach
    void setUp() {

        course = Course.builder()
                .id(10)
                .courseCode("CS101")
                .build();

        program = Program.builder()
                .id(70)
                .code("CS")
                .name("Computer Science")
                .build();

        archivedCohort = Cohort.builder()
                .id(700)
                .name("CS2026")
                .entryYear(2026)
                .program(program)
                .isActive(false)
                .build();

        courseProgram = CourseProgram.builder()
                .id(900)
                .course(course)
                .program(program)
                .cohort(archivedCohort)
                .build();
    }

    @Test
    void cannotCreateCourseProgramForArchivedCohort() {

        CreateCourseProgramRequest request =
                new CreateCourseProgramRequest();

        request.setCourseId(10);
        request.setProgramId(70);
        request.setCohortId(700);

        when(courseRepository.findById(10))
                .thenReturn(Optional.of(course));

        when(programRepository.findById(70))
                .thenReturn(Optional.of(program));

        when(cohortRepository.findById(700))
                .thenReturn(Optional.of(archivedCohort));

        doThrow(new IllegalStateException(
                "Archived Cohort is read-only."))
                .when(cohortOperationalGuard)
                .assertActive(archivedCohort);

        assertThatThrownBy(() ->
                service.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Archived Cohort");

        verify(cohortOperationalGuard)
                .assertActive(archivedCohort);

        verifyNoInteractions(courseTypeRepository);

        verify(repository, never())
                .save(any(CourseProgram.class));
    }

    @Test
    void cannotUpdateCourseProgramForArchivedCohort() {

        UpdateCourseProgramRequest request =
                new UpdateCourseProgramRequest();

        when(repository.findById(900))
                .thenReturn(Optional.of(courseProgram));

        doThrow(new IllegalStateException(
                "Archived Cohort is read-only."))
                .when(cohortOperationalGuard)
                .assertActive(archivedCohort);

        assertThatThrownBy(() ->
                service.update(900, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Archived Cohort");

        verify(cohortOperationalGuard)
                .assertActive(archivedCohort);

        verifyNoInteractions(courseTypeRepository);

        verify(repository, never())
                .save(any(CourseProgram.class));
    }

    @Test
    void cannotDeleteCourseProgramForArchivedCohort() {

        when(repository.findById(900))
                .thenReturn(Optional.of(courseProgram));

        doThrow(new IllegalStateException(
                "Archived Cohort is read-only."))
                .when(cohortOperationalGuard)
                .assertActive(archivedCohort);

        assertThatThrownBy(() ->
                service.delete(900))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Archived Cohort");

        verify(cohortOperationalGuard)
                .assertActive(archivedCohort);

        verify(repository, never())
                .delete(any(CourseProgram.class));
    }

    @Test
    void cannotAssignSyllabusToCourseProgramForArchivedCohort() {

        when(repository.findById(900))
                .thenReturn(Optional.of(courseProgram));

        doThrow(new IllegalStateException(
                "Archived Cohort is read-only."))
                .when(cohortOperationalGuard)
                .assertActive(archivedCohort);

        assertThatThrownBy(() ->
                service.assignSyllabus(900, 500))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Archived Cohort");

        verify(cohortOperationalGuard)
                .assertActive(archivedCohort);

        verifyNoInteractions(syllabusRepository);

        verify(repository, never())
                .save(any(CourseProgram.class));
    }
}