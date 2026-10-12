package com.scse.curriculum.syllabus.importer.service;

import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.course.repository.CourseRepository;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.cohort.service.CohortOperationalGuard;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.syllabus.service.SyllabusAccessService;
import com.scse.curriculum.syllabus.template.TargetTemplateProfileResolver;
import com.scse.curriculum.syllabus.importer.dto.*;
import com.scse.curriculum.user.entity.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Cohort2027IdentityGuardTest {
    private final SyllabusImportServiceImpl service = mock(SyllabusImportServiceImpl.class, CALLS_REAL_METHODS);
    private final SyllabusRepository syllabuses = mock(SyllabusRepository.class);
    private final CourseProgramRepository assignments = mock(CourseProgramRepository.class);
    private final Course course = Course.builder().id(1).courseCode("TEST001IU").build();
    private ConfirmSyllabusImportRequest setup() throws Exception {
        var courses = mock(CourseRepository.class);
        var cohorts = mock(CohortRepository.class);
        var access = mock(SyllabusAccessService.class);
        var templates = mock(TargetTemplateProfileResolver.class);
        set("courseRepository", courses);set("cohortRepository", cohorts);set("syllabusAccessService", access);
        set("targetTemplateProfileResolver", templates);set("courseProgramRepository", assignments);
        set("syllabusRepository", syllabuses);set("cohortOperationalGuard", mock(CohortOperationalGuard.class));
        when(courses.findById(1)).thenReturn(Optional.of(course));
        when(courses.lockIdentity(1)).thenReturn(Optional.of(course));
        var cohort = Cohort.builder().id(15).program(Program.builder().id(1).build()).build();
        when(cohorts.findById(15)).thenReturn(Optional.of(cohort));
        when(templates.resolve(cohort)).thenReturn("NEW_2027");
        var actor = UserAccount.builder().id(2).role(UserRole.INSTRUCTOR).build();
        when(access.authorizeImport(null, course, 1, 15)).thenReturn(
                new SyllabusAccessService.CreationAuthorization(actor, course, null, null, List.of()));
        var request = new ConfirmSyllabusImportRequest();
        request.setCourseId(1);request.setProgramId(1);request.setCohortId(15);
        var data = new SyllabusImportData();data.setSourceCourseCode("TEST001IU");request.setData(data);
        return request;
    }
    private void set(String name, Object value) throws Exception {
        var field = SyllabusImportServiceImpl.class.getDeclaredField(name);field.setAccessible(true);field.set(service,value);
    }
    @Test void wrongSourceCourseCannotWriteAnySyllabus() throws Exception {
        var request = setup();request.getData().setSourceCourseCode("OTHER001IU");
        assertTrue(assertThrows(IllegalArgumentException.class, () -> service.confirm(request)).getMessage().contains("source course code"));
        verifyNoInteractions(syllabuses);
    }
    @Test void missingExactAssignmentCannotCreateCurriculumDuringImport() throws Exception {
        var request = setup();
        when(assignments.findByCourse_IdAndProgram_IdAndCohort_Id(1,1,15)).thenReturn(Optional.empty());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> service.confirm(request)).getMessage().contains("existing exact curriculum"));
        verify(assignments, never()).save(any());verifyNoInteractions(syllabuses);
    }
    @Test void repeatImportCannotCreateDuplicateOrOverwriteExistingSyllabus() throws Exception {
        var request = setup();
        when(assignments.findByCourse_IdAndProgram_IdAndCohort_Id(1,1,15)).thenReturn(Optional.of(
                CourseProgram.builder().course(course).syllabus(Syllabus.builder().id(99).notes("Existing user content").build()).build()));
        assertTrue(assertThrows(IllegalStateException.class, () -> service.confirm(request)).getMessage().contains("duplicate import"));
        verifyNoInteractions(syllabuses);
    }
}
