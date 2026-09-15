package com.scse.curriculum.syllabus.maintenance;

import com.scse.curriculum.classsection.entity.ClassSection;
import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.common.exception.ForbiddenOperationException;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.syllabus.history.SyllabusRevisionSnapshot;
import com.scse.curriculum.syllabus.history.SyllabusRevisionSnapshotRepository;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.syllabus.service.SyllabusService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyllabusCohortResetServiceTest {

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private CourseProgramRepository courseProgramRepository;

    @Mock
    private ClassSectionRepository classSectionRepository;

    @Mock
    private SyllabusRepository syllabusRepository;

    @Mock
    private SyllabusRevisionSnapshotRepository revisionSnapshotRepository;

    @Mock
    private SyllabusService syllabusService;

    @InjectMocks
    private SyllabusCohortResetService service;

    private Program program;
    private Cohort cohort;

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(
                service,
                "cohortResetEnabled",
                true
        );

        program =
                Program.builder()
                        .id(1)
                        .code("CS")
                        .build();

        cohort =
                Cohort.builder()
                        .id(12)
                        .program(program)
                        .entryYear(2026)
                        .name("CS2026")
                        .build();
    }

    @Test
    void disabledFeatureFlagRejectsReset() {

        ReflectionTestUtils.setField(
                service,
                "cohortResetEnabled",
                false
        );

        assertThatThrownBy(() ->
                service.resetForReimport(
                        1,
                        12,
                        "CS2026"
                ))
                .isInstanceOf(
                        ForbiddenOperationException.class
                )
                .hasMessageContaining(
                        "disabled"
                );

        verifyNoInteractions(
                cohortRepository,
                courseProgramRepository,
                classSectionRepository,
                syllabusRepository,
                revisionSnapshotRepository,
                syllabusService
        );
    }

    @Test
    void rejectsCohortOutsideSelectedProgram() {

        Program otherProgram =
                Program.builder()
                        .id(2)
                        .code("IT")
                        .build();

        Cohort wrong =
                Cohort.builder()
                        .id(12)
                        .program(otherProgram)
                        .entryYear(2026)
                        .name("CS2026")
                        .build();

        when(cohortRepository.findById(12))
                .thenReturn(
                        Optional.of(wrong)
                );

        assertThatThrownBy(() ->
                service.resetForReimport(
                        1,
                        12,
                        "CS2026"
                ))
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "does not belong"
                );

        verifyNoInteractions(
                courseProgramRepository,
                classSectionRepository,
                syllabusRepository,
                revisionSnapshotRepository,
                syllabusService
        );
    }

    @Test
    void rejectsWrongConfirmationText() {

        when(cohortRepository.findById(12))
                .thenReturn(
                        Optional.of(cohort)
                );

        assertThatThrownBy(() ->
                service.resetForReimport(
                        1,
                        12,
                        "CS2025"
                ))
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "CS2026"
                );

        verifyNoInteractions(
                courseProgramRepository,
                classSectionRepository,
                syllabusRepository,
                revisionSnapshotRepository,
                syllabusService
        );
    }

    @Test
    void historyFreeSyllabusUsesNormalPhysicalDelete() {

        Syllabus syllabus =
                Syllabus.builder()
                        .id(100)
                        .status(
                                SyllabusStatus.SUBMITTED
                        )
                        .build();

        CourseProgram mapping =
                CourseProgram.builder()
                        .id(1000)
                        .program(program)
                        .cohort(cohort)
                        .syllabus(syllabus)
                        .build();

        when(cohortRepository.findById(12))
                .thenReturn(
                        Optional.of(cohort)
                );

        when(
                courseProgramRepository
                        .findByProgram_IdAndCohort_Id(
                                1,
                                12
                        )
        ).thenReturn(
                List.of(mapping)
        );

        when(
                courseProgramRepository
                        .findBySyllabus_Id(100)
        ).thenReturn(
                List.of(mapping)
        );

        when(
                revisionSnapshotRepository
                        .findBySyllabusIdOrderByCapturedAtAscIdAsc(
                                100
                        )
        ).thenReturn(
                List.of()
        );

        when(
                syllabusRepository
                        .findByIdWithRelations(100)
        ).thenReturn(
                Optional.of(syllabus)
        );

        when(
                syllabusRepository
                        .saveAndFlush(
                                any(Syllabus.class)
                        )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        int reset =
                service.resetForReimport(
                        1,
                        12,
                        "CS2026"
                );

        assertThat(reset)
                .isEqualTo(1);

        assertThat(
                syllabus.getStatus()
        ).isEqualTo(
                SyllabusStatus.DRAFT
        );

        verify(
                syllabusService
        ).delete(100);

        verifyNoInteractions(
                classSectionRepository
        );
    }

    @Test
    void syllabusWithImmutableHistoryIsRetiredNotDeleted() {

        Syllabus syllabus =
                Syllabus.builder()
                        .id(3013)
                        .status(
                                SyllabusStatus.APPROVED
                        )
                        .program("CS")
                        .academicYear("CS2026")
                        .semester("Semester 2")
                        .isCurrent(true)
                        .build();

        CourseProgram mapping =
                CourseProgram.builder()
                        .id(246)
                        .program(program)
                        .cohort(cohort)
                        .syllabus(syllabus)
                        .build();

        ClassSection classSection =
                mock(ClassSection.class);

        SyllabusRevisionSnapshot snapshot =
                mock(
                        SyllabusRevisionSnapshot.class
                );

        when(cohortRepository.findById(12))
                .thenReturn(
                        Optional.of(cohort)
                );

        when(
                courseProgramRepository
                        .findByProgram_IdAndCohort_Id(
                                1,
                                12
                        )
        ).thenReturn(
                List.of(mapping)
        );

        when(
                courseProgramRepository
                        .findBySyllabus_Id(3013)
        ).thenReturn(
                List.of(mapping)
        );

        when(
                revisionSnapshotRepository
                        .findBySyllabusIdOrderByCapturedAtAscIdAsc(
                                3013
                        )
        ).thenReturn(
                List.of(snapshot)
        );

        when(
                syllabusRepository
                        .findByIdWithRelations(3013)
        ).thenReturn(
                Optional.of(syllabus)
        );

        when(
                classSectionRepository
                        .findBySyllabusId(3013)
        ).thenReturn(
                List.of(classSection)
        );

        /*
         * Null academic year is treated as no conflicting
         * external cohort information.
         */
        when(
                classSection.getAcademicYear()
        ).thenReturn(null);

        when(
                syllabusRepository
                        .saveAndFlush(
                                any(Syllabus.class)
                        )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        int reset =
                service.resetForReimport(
                        1,
                        12,
                        "CS2026"
                );

        assertThat(reset)
                .isEqualTo(1);

        /*
         * Keep truthful historical workflow status.
         */
        assertThat(
                syllabus.getStatus()
        ).isEqualTo(
                SyllabusStatus.APPROVED
        );

        assertThat(
                syllabus.getIsCurrent()
        ).isFalse();

        /*
         * Original CS2026 logical identity must be released.
         */
        assertThat(
                syllabus.getProgram()
        ).isEqualTo(
                "__AUDIT_RESET__3013"
        );

        assertThat(
                syllabus.getAcademicYear()
        ).isEqualTo(
                "AUDIT2026"
        );

        /*
         * Curriculum mapping remains,
         * only old syllabus link is removed.
         */
        assertThat(
                mapping.getSyllabus()
        ).isNull();

        verify(
                classSection
        ).setSyllabus(null);

        /*
         * Immutable-history syllabus must never go through
         * physical normal delete.
         */
        verify(
                syllabusService,
                never()
        ).delete(3013);

        /*
         * Most important regression rule:
         * revision snapshots must never be deleted.
         */
        verify(
                revisionSnapshotRepository,
                never()
        ).deleteAll(any());

        verify(
                revisionSnapshotRepository,
                never()
        ).delete(any());
    }

    @Test
    void abortsEntireResetForCrossScopeLink() {

        Syllabus syllabus =
                Syllabus.builder()
                        .id(100)
                        .status(
                                SyllabusStatus.DRAFT
                        )
                        .build();

        CourseProgram validMapping =
                CourseProgram.builder()
                        .id(1000)
                        .program(program)
                        .cohort(cohort)
                        .syllabus(syllabus)
                        .build();

        Program otherProgram =
                Program.builder()
                        .id(2)
                        .code("IT")
                        .build();

        Cohort otherCohort =
                Cohort.builder()
                        .id(13)
                        .program(otherProgram)
                        .entryYear(2025)
                        .name("IT2025")
                        .build();

        CourseProgram foreignMapping =
                CourseProgram.builder()
                        .id(1001)
                        .program(otherProgram)
                        .cohort(otherCohort)
                        .syllabus(syllabus)
                        .build();

        when(cohortRepository.findById(12))
                .thenReturn(
                        Optional.of(cohort)
                );

        when(
                courseProgramRepository
                        .findByProgram_IdAndCohort_Id(
                                1,
                                12
                        )
        ).thenReturn(
                List.of(validMapping)
        );

        when(
                courseProgramRepository
                        .findBySyllabus_Id(100)
        ).thenReturn(
                List.of(
                        validMapping,
                        foreignMapping
                )
        );

        assertThatThrownBy(() ->
                service.resetForReimport(
                        1,
                        12,
                        "CS2026"
                ))
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessageContaining(
                        "outside the selected"
                );

        verifyNoInteractions(
                revisionSnapshotRepository,
                classSectionRepository,
                syllabusRepository,
                syllabusService
        );
    }
}