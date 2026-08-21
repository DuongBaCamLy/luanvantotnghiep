package com.scse.curriculum.program.service;

import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.department.entity.Department;
import com.scse.curriculum.department.repository.DepartmentRepository;
import com.scse.curriculum.major.entity.Major;
import com.scse.curriculum.major.repository.MajorRepository;
import com.scse.curriculum.program.dto.UpdateProgramRequest;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.program.repository.ProgramRepository;
import com.scse.curriculum.programtype.entity.ProgramType;
import com.scse.curriculum.programtype.repository.ProgramTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgramServiceImplMetadataTest {

    @Mock private ProgramRepository repository;
    @Mock private MajorRepository majorRepository;
    @Mock private ProgramTypeRepository programTypeRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private CourseProgramRepository courseProgramRepository;
    @Mock private CohortRepository cohortRepository;
    @InjectMocks private ProgramServiceImpl service;

    private Program program;

    @BeforeEach
    void setUp() {
        Major major = Major.builder().id(1).code("CS").name("Computer Science").nameVn("Khoa học máy tính").build();
        ProgramType type = ProgramType.builder().id(1).code("BSC").name("Bachelor").build();
        Department department = Department.builder().id(1).code("SCSE").name("SCSE").nameVn("Khoa KT&KHM").build();
        program = Program.builder().id(10).code("CS-2021").name("Computer Science").nameVn("Khoa học máy tính")
                .major(major).programType(type).department(department).totalCredits(6).durationYears(4)
                .validFrom(LocalDate.of(2021, 9, 1)).isActive(true).build();
        when(repository.findById(10)).thenReturn(Optional.of(program));
        lenient().when(repository.save(any(Program.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void updateMetadataReplacesEditableFields() {
        Major major = Major.builder().id(2).code("IT").name("Information Technology").nameVn("CNTT").build();
        ProgramType type = ProgramType.builder().id(2).code("ENG").name("Engineer").build();
        Department department = Department.builder().id(2).code("IT").name("IT").nameVn("CNTT").build();
        when(majorRepository.findById(2)).thenReturn(Optional.of(major));
        when(programTypeRepository.findById(2)).thenReturn(Optional.of(type));
        when(departmentRepository.findById(2)).thenReturn(Optional.of(department));

        UpdateProgramRequest request = new UpdateProgramRequest();
        request.setName(" Information Technology "); request.setNameVn("CNTT"); request.setMajorId(2);
        request.setProgramTypeId(2); request.setDepartmentId(2); request.setTotalCredits(140); request.setDurationYears(4);
        request.setValidFrom(LocalDate.of(2024, 9, 1)); request.setAccreditationBody("ABET");

        assertThat(service.update(10, request).getTotalCredits()).isEqualTo(140);
        assertThat(program.getName()).isEqualTo("Information Technology");
        assertThat(program.getMajor().getId()).isEqualTo(2);
        verify(repository).save(program);
    }

    @Test
    void archiveValidCurriculumIsSoftAndPreservesRelatedData() {
        Cohort cohort = Cohort.builder().id(100).name("K21").program(program).entryYear(2021).build();
        Course course = Course.builder().id(1).creditTheory(3).creditLab(3).build();
        CourseProgram courseProgram = CourseProgram.builder().id(50).course(course).program(program).cohort(cohort).build();
        when(cohortRepository.findByProgram_IdOrderByEntryYearDesc(10)).thenReturn(List.of(cohort));
        when(courseProgramRepository.findEffectiveByProgramIdAndCohortIdWithRelations(10, 100)).thenReturn(List.of(courseProgram));

        assertThat(service.archive(10).getIsActive()).isFalse();

        verify(repository).save(program);
        verify(courseProgramRepository, never()).deleteAll(any());
        verify(cohortRepository, never()).delete(any());
    }

    @Test
    void archiveRejectsCreditMismatchAndDoesNotChangeStatus() {
        Cohort cohort = Cohort.builder().id(100).name("K21").program(program).entryYear(2021).build();
        when(cohortRepository.findByProgram_IdOrderByEntryYearDesc(10)).thenReturn(List.of(cohort));
        when(courseProgramRepository.findEffectiveByProgramIdAndCohortIdWithRelations(10, 100)).thenReturn(List.of());

        assertThatThrownBy(() -> service.archive(10)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("K21 has 0 credits; expected 6");
        assertThat(program.getIsActive()).isTrue();
        verify(repository, never()).save(any());
    }

    @Test
    void reactivateRestoresActiveStatusWithoutChangingRelatedData() {
        program.setIsActive(false);

        assertThat(service.reactivate(10).getIsActive()).isTrue();

        verify(repository).save(program);
        verifyNoInteractions(courseProgramRepository, cohortRepository);
    }
}
