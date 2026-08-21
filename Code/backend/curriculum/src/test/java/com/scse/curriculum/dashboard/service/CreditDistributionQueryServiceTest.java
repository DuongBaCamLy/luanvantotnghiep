package com.scse.curriculum.dashboard.service;

import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.coursetype.entity.CourseType;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.program.repository.ProgramRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditDistributionQueryServiceTest {

    @Mock ProgramRepository programRepository;
    @Mock CohortRepository cohortRepository;
    @Mock CourseProgramRepository courseProgramRepository;
    @InjectMocks CreditDistributionQueryService service;

    @Test
    void aggregatesCreditsAndRemovesDuplicateCourseRows() {
        Program program = Program.builder().id(1).code("CS-2021").name("Computer Science")
                .nameVn("Khoa học máy tính").totalCredits(7).build();
        Cohort cohort = Cohort.builder().id(10).program(program).entryYear(2021).name("CS2021").build();
        CourseType compulsory = CourseType.builder().id(1).code("COMPULSORY")
                .name("Compulsory").nameVn("Môn bắt buộc").build();
        CourseType elective = CourseType.builder().id(2).code("ELECTIVE")
                .name("Elective").nameVn("Môn tự chọn").build();
        Course c1 = Course.builder().id(100).courseCode("IT001").name("A").nameVn("A")
                .creditTheory(3).creditLab(1).build();
        Course c2 = Course.builder().id(101).courseCode("IT002").name("B").nameVn("B")
                .creditTheory(3).creditLab(0).build();

        CourseProgram c1General = CourseProgram.builder().id(1).program(program).course(c1)
                .courseType(elective).cohort(null).build();
        CourseProgram c1Specific = CourseProgram.builder().id(2).program(program).course(c1)
                .courseType(compulsory).cohort(cohort).build();
        CourseProgram c2Specific = CourseProgram.builder().id(3).program(program).course(c2)
                .courseType(elective).cohort(cohort).build();

        when(programRepository.findById(1)).thenReturn(Optional.of(program));
        when(cohortRepository.findById(10)).thenReturn(Optional.of(cohort));
        when(courseProgramRepository.findEffectiveByProgramIdAndCohortIdWithRelations(1, 10))
                .thenReturn(List.of(c1General, c1Specific, c2Specific));

        var result = service.getDistribution(1, 10);

        assertThat(result.getCalculatedTotalCredits()).isEqualTo(7);
        assertThat(result.getUniqueCourseCount()).isEqualTo(2);
        assertThat(result.getDuplicateRowsRemoved()).isEqualTo(1);
        assertThat(result.getMatchesDeclaredTotal()).isTrue();
        assertThat(result.getGroups()).extracting("code", "credits", "courseCount")
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("COMPULSORY", 4, 1),
                        org.assertj.core.groups.Tuple.tuple("ELECTIVE", 3, 1));
        assertThat(result.getGroups().stream().mapToInt(g -> g.getCredits()).sum()).isEqualTo(7);
    }
}
