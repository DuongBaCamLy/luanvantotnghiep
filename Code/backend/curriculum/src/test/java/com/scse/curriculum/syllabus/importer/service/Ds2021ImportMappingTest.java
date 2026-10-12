package com.scse.curriculum.syllabus.importer.service;

import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.cohort.service.CohortOperationalGuard;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.course.repository.CourseRepository;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.program.repository.ProgramRepository;
import com.scse.curriculum.syllabus.dto.SyllabusResponse;
import com.scse.curriculum.syllabus.importer.dto.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Ds2021ImportMappingTest {
    private final SyllabusImportServiceImpl service = mock(SyllabusImportServiceImpl.class, CALLS_REAL_METHODS);
    private final CourseProgramRepository mappings = mock(CourseProgramRepository.class);
    private BulkConfirmSyllabusImportRequest setup(String code, String name) throws Exception {
        var programs = mock(ProgramRepository.class);var cohorts = mock(CohortRepository.class);
        var program = Program.builder().id(3).build();var cohort = Cohort.builder().id(7).name("DS2021").program(program).build();
        set("programRepository", programs);set("cohortRepository", cohorts);set("courseProgramRepository", mappings);
        set("cohortOperationalGuard", mock(CohortOperationalGuard.class));set("courseRepository", mock(CourseRepository.class));
        when(programs.findById(3)).thenReturn(Optional.of(program));when(cohorts.findById(7)).thenReturn(Optional.of(cohort));
        var request = new BulkConfirmSyllabusImportRequest();request.setProgramId(3);request.setCohortId(7);
        var data = new SyllabusImportData();data.setSourceCourseCode(code);data.setSourceCourseName(name);request.setData(data);
        return request;
    }
    private void set(String field, Object value) throws Exception {
        var member = SyllabusImportServiceImpl.class.getDeclaredField(field);member.setAccessible(true);member.set(service,value);
    }
    @Test void existingBackendResolvesChemistryAliasWithinDs2021WithoutAddingCatalogRows() throws Exception {
        var request = setup("CHE011IU", "Chemistry for Engineers");
        var course = Course.builder().id(1).courseCode("CH011IU").name("Chemistry for Engineer").build();
        var mapping = CourseProgram.builder().id(154).course(course).program(Program.builder().id(3).build()).build();
        when(mappings.findEffectiveByProgramIdAndCohortIdWithRelations(3,7)).thenReturn(List.of(mapping));
        doReturn(SyllabusResponse.builder().id(1).build()).when(service).confirm(any());
        service.confirmBulkItem(request);
        var argument = org.mockito.ArgumentCaptor.forClass(ConfirmSyllabusImportRequest.class);
        verify(service).confirm(argument.capture());assertEquals(1,argument.getValue().getCourseId());assertEquals(154,argument.getValue().getCourseProgramId());
        assertEquals("CHE011IU",argument.getValue().getData().getSourceCourseCode());verify(mappings,never()).save(any());
    }
    @Test void cloudAppendixCannotBeSavedAsTheDifferentMobileCloudCourse() throws Exception {
        var request = setup("IT164IU", "Cloud Computing");
        var course = Course.builder().id(75).courseCode("IT147IU").name("Mobile Cloud Computing").build();
        when(mappings.findEffectiveByProgramIdAndCohortIdWithRelations(3,7)).thenReturn(List.of(CourseProgram.builder().id(180).course(course).build()));
        assertThrows(IllegalArgumentException.class, () -> service.confirmBulkItem(request));
        verify(service,never()).confirm(any());verify(mappings,never()).save(any());
    }
}
