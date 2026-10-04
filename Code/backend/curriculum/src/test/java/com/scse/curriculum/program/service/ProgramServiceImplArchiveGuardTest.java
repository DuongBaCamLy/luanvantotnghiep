package com.scse.curriculum.program.service;

import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.cohort.service.CohortOperationalGuard;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.department.repository.DepartmentRepository;
import com.scse.curriculum.major.repository.MajorRepository;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.program.repository.ProgramRepository;
import com.scse.curriculum.programtype.repository.ProgramTypeRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ProgramServiceImplArchiveGuardTest {

    @Mock
    private ProgramRepository repository;

    @Mock
    private MajorRepository majorRepository;

    @Mock
    private ProgramTypeRepository programTypeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CourseProgramRepository courseProgramRepository;

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private CohortOperationalGuard cohortOperationalGuard;

    @InjectMocks
    private ProgramServiceImpl service;

    @Test
    void cannotCloneFromArchivedSourceCohort() {

        Program sourceProgram = Program.builder()
                .id(10)
                .code("CS")
                .build();

        Program targetProgram = Program.builder()
                .id(20)
                .code("IT")
                .build();

        Cohort sourceCohort = Cohort.builder()
                .id(100)
                .name("CS2022")
                .program(sourceProgram)
                .isActive(false)
                .build();

        Cohort targetCohort = Cohort.builder()
                .id(200)
                .name("IT2026")
                .program(targetProgram)
                .isActive(true)
                .build();

        org.mockito.Mockito.when(repository.findById(10))
                .thenReturn(Optional.of(sourceProgram));

        org.mockito.Mockito.when(repository.findById(20))
                .thenReturn(Optional.of(targetProgram));

        org.mockito.Mockito.when(cohortRepository.findById(100))
                .thenReturn(Optional.of(sourceCohort));

        org.mockito.Mockito.when(cohortRepository.findById(200))
                .thenReturn(Optional.of(targetCohort));

        doThrow(new IllegalStateException(
                "Archived Cohort is read-only. Restore it before making operational changes."))
                .when(cohortOperationalGuard)
                .assertActive(sourceCohort);

        assertThatThrownBy(() ->
                service.cloneFromCohort(
                        10,
                        100,
                        20,
                        200))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Archived Cohort");

        verify(cohortOperationalGuard)
                .assertActive(sourceCohort);

        verify(cohortOperationalGuard, never())
                .assertActive(targetCohort);

        verifyNoInteractions(courseProgramRepository);
    }

    @Test
    void cannotCloneIntoArchivedTargetCohort() {

        Program sourceProgram = Program.builder()
                .id(10)
                .code("CS")
                .build();

        Program targetProgram = Program.builder()
                .id(20)
                .code("IT")
                .build();

        Cohort sourceCohort = Cohort.builder()
                .id(100)
                .name("CS2025")
                .program(sourceProgram)
                .isActive(true)
                .build();

        Cohort targetCohort = Cohort.builder()
                .id(200)
                .name("IT2022")
                .program(targetProgram)
                .isActive(false)
                .build();

        org.mockito.Mockito.when(repository.findById(10))
                .thenReturn(Optional.of(sourceProgram));

        org.mockito.Mockito.when(repository.findById(20))
                .thenReturn(Optional.of(targetProgram));

        org.mockito.Mockito.when(cohortRepository.findById(100))
                .thenReturn(Optional.of(sourceCohort));

        org.mockito.Mockito.when(cohortRepository.findById(200))
                .thenReturn(Optional.of(targetCohort));

        org.mockito.Mockito.doNothing()
                .when(cohortOperationalGuard)
                .assertActive(sourceCohort);

        doThrow(new IllegalStateException(
                "Archived Cohort is read-only. Restore it before making operational changes."))
                .when(cohortOperationalGuard)
                .assertActive(targetCohort);

        assertThatThrownBy(() ->
                service.cloneFromCohort(
                        10,
                        100,
                        20,
                        200))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Archived Cohort");

        verify(cohortOperationalGuard)
                .assertActive(sourceCohort);

        verify(cohortOperationalGuard)
                .assertActive(targetCohort);

        verifyNoInteractions(courseProgramRepository);
    }
}
