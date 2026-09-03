package com.scse.curriculum.curriculummap.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.courserelationship.repository.CourseRelationshipRepository;
import com.scse.curriculum.courserelationship.entity.CourseRelationship;
import com.scse.curriculum.courserelationship.entity.RelationType;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.program.repository.ProgramRepository;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.syllabus.service.SyllabusAccessService;

@ExtendWith(MockitoExtension.class)
class CurriculumMapServiceImplTest {
    @Mock SyllabusRepository syllabuses;
    @Mock SyllabusAccessService syllabusAccess;
    @Mock CourseProgramRepository mappings;
    @Mock CourseRelationshipRepository relationships;
    @Mock ProgramRepository programs;
    @Mock CohortRepository cohorts;

    CurriculumMapServiceImpl service;
    Program program;
    Cohort cohort;

    @BeforeEach
    void setUp() {
        service = new CurriculumMapServiceImpl(syllabuses, syllabusAccess, mappings, relationships, programs, cohorts);
        program = Program.builder().id(1).code("CS").name("Computer Science").build();
        cohort = Cohort.builder().id(21).name("CS2021").program(program).build();
        when(programs.findById(1)).thenReturn(Optional.of(program));
        lenient().when(cohorts.findById(21)).thenReturn(Optional.of(cohort));
        when(relationships.findAllWithCourses()).thenReturn(List.of());
        lenient().when(syllabusAccess.canView(org.mockito.ArgumentMatchers.any(Syllabus.class)))
                .thenReturn(true);
    }

    @Test
    void allFiltersUseExactlyUniqueCatalogCourses() {
        Syllabus first = syllabus(1, course(101, "IT101"), "Semester 1", SyllabusStatus.DRAFT, 1);
        Syllabus newer = syllabus(2, first.getCourse(), "Semester 1", SyllabusStatus.DRAFT, 2);
        Syllabus second = syllabus(3, course(102, "IT102"), "Semester 3", SyllabusStatus.APPROVED, 1);
        stubScope(List.of(first, newer, second));

        var result = service.generate(1, 21, null, null);

        assertThat(courseCount(result)).isEqualTo(2);
        assertThat(result.getSemesters()).flatExtracting(group -> group.getCourses())
                .extracting(node -> node.getSyllabusVersion()).contains("v2");
    }

    @Test
    void semesterFilterLimitsBothNodesAndSemesterGroups() {
        stubScope(List.of(
                syllabus(1, course(101, "IT101"), "Semester 1", SyllabusStatus.APPROVED, 1),
                syllabus(2, course(102, "IT102"), "HK3", SyllabusStatus.APPROVED, 1)));

        var result = service.generate(1, 21, "3", null);

        assertThat(courseCount(result)).isEqualTo(1);
        assertThat(result.getSemesters()).extracting(group -> group.getSemester())
                .containsExactly("Semester 3");
    }

    @Test
    void statusFilterUsesOnlyMatchingSyllabuses() {
        stubScope(List.of(
                syllabus(1, course(101, "IT101"), "1", SyllabusStatus.DRAFT, 1),
                syllabus(2, course(102, "IT102"), "2", SyllabusStatus.APPROVED, 1)));

        var result = service.generate(1, 21, null, "APPROVED");

        assertThat(courseCount(result)).isEqualTo(1);
        assertThat(result.getSemesters()).flatExtracting(group -> group.getCourses())
                .extracting(node -> node.getCourseCode()).containsExactly("IT102");
    }

    @Test
    void zeroCatalogRecordsAlwaysProduceZeroNodesAndEdges() {
        stubScope(List.of());

        var result = service.generate(1, 21, null, null);

        assertThat(result.getSemesters()).isEmpty();
        assertThat(result.getRelations()).isEmpty();
    }

    @Test
    void changingCohortUsesThatCohortsCatalogScope() {
        Cohort newerCohort = Cohort.builder().id(22).name("CS2022").program(program).build();
        when(cohorts.findById(22)).thenReturn(Optional.of(newerCohort));
        when(syllabuses.findCatalogScope(1, 22)).thenReturn(List.of());
        when(mappings.findByProgramIdAndCohortIdWithRelations(1, 22)).thenReturn(List.of());

        var result = service.generate(1, 22, null, null);

        verify(syllabuses).findCatalogScope(1, 22);
        assertThat(result.getCohortName()).isEqualTo("CS2022");
        assertThat(courseCount(result)).isZero();
    }

    @Test
    void relationshipsRequireBothCoursesInFilteredSyllabusSet() {
        Course included = course(101, "IT101");
        Course excluded = course(999, "GHOST");
        stubScope(List.of(syllabus(1, included, "1", SyllabusStatus.APPROVED, 1)));
        when(relationships.findAllWithCourses()).thenReturn(List.of(
                CourseRelationship.builder().course(included).relatedCourse(excluded)
                        .relationType(RelationType.PREREQUISITE).build()));

        var result = service.generate(1, 21, null, null);

        assertThat(result.getRelations()).isEmpty();
    }

    private void stubScope(List<Syllabus> source) {
        when(syllabuses.findCatalogScope(1, 21)).thenReturn(source);
        when(mappings.findByProgramIdAndCohortIdWithRelations(1, 21)).thenReturn(
                source.stream().map(syllabus -> CourseProgram.builder()
                        .course(syllabus.getCourse()).program(program).cohort(cohort).syllabus(syllabus).build())
                        .toList());
    }

    private Course course(int id, String code) {
        return Course.builder().id(id).courseCode(code).name(code).creditTheory(3).creditLab(0).build();
    }

    private Syllabus syllabus(int id, Course course, String semester, SyllabusStatus status, int version) {
        return Syllabus.builder().id(id).course(course).semester(semester).status(status)
                .versionNumber(version).versionLabel("v" + version).isCurrent(version > 1).build();
    }

    private long courseCount(com.scse.curriculum.curriculummap.dto.CurriculumMapResponse response) {
        return response.getSemesters().stream().mapToLong(group -> group.getCourses().size()).sum();
    }
}
