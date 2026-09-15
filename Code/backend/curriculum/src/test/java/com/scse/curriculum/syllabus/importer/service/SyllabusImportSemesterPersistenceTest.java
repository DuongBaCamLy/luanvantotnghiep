package com.scse.curriculum.syllabus.importer.service;

import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.entity.CurriculumTerm;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class SyllabusImportSemesterPersistenceTest {

    @Test
    void synchronizesResolvedSemesterIntoCurriculumMapping()
            throws Exception {

        CourseProgram scoped =
                CourseProgram.builder()
                        .semesterSuggest(7)
                        .yearSuggest(4)
                        .termCode(CurriculumTerm.HK7)
                        .build();

        invokeSync(scoped, 5);

        assertThat(scoped.getSemesterSuggest())
                .isEqualTo(5);

        assertThat(scoped.getYearSuggest())
                .isEqualTo(3);

        assertThat(scoped.getTermCode())
                .isEqualTo(CurriculumTerm.HK5);
    }

    @Test
    void synchronizesTemporarySemesterForPreviouslyUnassignedCourse()
            throws Exception {

        CourseProgram scoped =
                CourseProgram.builder()
                        .semesterSuggest(null)
                        .yearSuggest(null)
                        .termCode(null)
                        .build();

        invokeSync(scoped, 8);

        assertThat(scoped.getSemesterSuggest())
                .isEqualTo(8);

        assertThat(scoped.getYearSuggest())
                .isEqualTo(4);

        assertThat(scoped.getTermCode())
                .isEqualTo(CurriculumTerm.HK8);
    }

    @Test
    void nullResolvedSemesterDoesNotOverwriteExistingPlacement()
            throws Exception {

        CourseProgram scoped =
                CourseProgram.builder()
                        .semesterSuggest(6)
                        .yearSuggest(3)
                        .termCode(CurriculumTerm.HK6)
                        .build();

        invokeSync(scoped, null);

        assertThat(scoped.getSemesterSuggest())
                .isEqualTo(6);

        assertThat(scoped.getYearSuggest())
                .isEqualTo(3);

        assertThat(scoped.getTermCode())
                .isEqualTo(CurriculumTerm.HK6);
    }

    private void invokeSync(
            CourseProgram scoped,
            Integer semester)
            throws Exception {

        Method method =
                SyllabusImportServiceImpl.class
                        .getDeclaredMethod(
                                "synchronizeCurriculumSemester",
                                CourseProgram.class,
                                Integer.class);

        method.setAccessible(true);

        /*
         * Static helper:
         * no service dependencies or database are required.
         */
        method.invoke(
                null,
                scoped,
                semester);
    }
}