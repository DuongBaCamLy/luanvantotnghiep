package com.scse.curriculum.syllabus.importer.service;

import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.coursetype.entity.CourseType;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SyllabusImportSemesterResolutionTest {

    @Mock
    private CourseProgramRepository courseProgramRepository;

    @InjectMocks
    private SyllabusImportServiceImpl service;

    @Test
    void pdfSemesterIsAuthoritativeWhenFileProvidesAValidSemester()
            throws Exception {

        CourseProgram selected =
                mapping(
                        "CS2026",
                        "CS-2021",
                        "IT999IU",
                        7,
                        "COMPULSORY");

        Integer result =
                resolveCurriculumSemester(
                        selected,
                        1,
                        12,
                        "Semester 5");

        /*
         * The imported file says Semester 5.
         * Existing curriculum metadata must not overwrite it.
         */
        assertThat(result)
                .isEqualTo(5);
    }

    @Test
    void existingCurriculumSemesterIsKeptWhenFileHasNoSemester()
            throws Exception {

        CourseProgram selected =
                mapping(
                        "SE2027",
                        "SE-2022",
                        "SE321IU",
                        6,
                        "ELECTIVE");

        Integer result =
                resolveCurriculumSemester(
                        selected,
                        1,
                        12,
                        null);

        assertThat(result)
                .isEqualTo(6);
    }

    @Test
    void missingSemesterGetsStableTemporaryPlacementBetweenFourAndEight()
            throws Exception {

        CourseProgram selected =
                mapping(
                        "DS2028",
                        "DS-2023",
                        "DS450IU",
                        null,
                        "ELECTIVE");

        Integer first =
                resolveCurriculumSemester(
                        selected,
                        1,
                        12,
                        null);

        Integer second =
                resolveCurriculumSemester(
                        selected,
                        1,
                        12,
                        null);

        assertThat(first)
                .isBetween(4, 8);

        /*
         * Temporary placement must be deterministic.
         * Re-importing the same course must not randomly move it.
         */
        assertThat(second)
                .isEqualTo(first);
    }

    @Test
    void temporaryPlacementWorksForArbitraryFutureCourseCodes()
            throws Exception {

        List<String> futureCourseCodes =
                List.of(
                        "CS501IU",
                        "SE620IU",
                        "DS777IU",
                        "AI410IU",
                        "MA888IU",
                        "BA350IU",
                        "IT990IU");

        for (String courseCode : futureCourseCodes) {

            CourseProgram selected =
                    mapping(
                            "XX2030",
                            "XX-2030",
                            courseCode,
                            null,
                            "ELECTIVE");

            Integer result =
                    resolveCurriculumSemester(
                            selected,
                            1,
                            12,
                            null);

            assertThat(result)
                    .as(courseCode)
                    .isBetween(4, 8);
        }
    }

    @Test
    void autoReconciledElectiveKeepsSemesterFromImportedFile()
            throws Exception {

        SyllabusImportData data =
                SyllabusImportData.builder()
                        .sourceCourseCode("NEW501IU")
                        .semester("Semester 7")
                        .build();

        Integer result =
                resolveAutoReconciledSemester(
                        data,
                        "ELECTIVE");

        /*
         * ELECTIVE must not automatically mean N/A.
         * If the source says Semester 7, keep Semester 7.
         */
        assertThat(result)
                .isEqualTo(7);
    }

    @Test
    void autoReconciledCourseWithoutSemesterGetsTemporaryPlacement()
            throws Exception {

        SyllabusImportData data =
                SyllabusImportData.builder()
                        .sourceCourseCode("NEW777IU")
                        .semester(null)
                        .build();

        Integer first =
                resolveAutoReconciledSemester(
                        data,
                        "ELECTIVE");

        Integer second =
                resolveAutoReconciledSemester(
                        data,
                        "ELECTIVE");

        assertThat(first)
                .isBetween(4, 8);

        assertThat(second)
                .isEqualTo(first);
    }

    @Test
    void differentFilesWithRealSemestersRemainUntouched()
            throws Exception {

        CourseProgram selected =
                mapping(
                        "CS2031",
                        "CS-2031",
                        "CS700IU",
                        null,
                        "COMPULSORY");

        assertThat(
                resolveCurriculumSemester(
                        selected,
                        1,
                        12,
                        "Semester 1"))
                .isEqualTo(1);

        assertThat(
                resolveCurriculumSemester(
                        selected,
                        1,
                        12,
                        "Semester 4"))
                .isEqualTo(4);

        assertThat(
                resolveCurriculumSemester(
                        selected,
                        1,
                        12,
                        "Semester 8"))
                .isEqualTo(8);
    }

    private CourseProgram mapping(
            String cohortName,
            String programCode,
            String courseCode,
            Integer semester,
            String courseTypeCode) {

        Program program =
                Program.builder()
                        .id(1)
                        .code(programCode)
                        .build();

        Cohort cohort =
                Cohort.builder()
                        .id(12)
                        .name(cohortName)
                        .program(program)
                        .build();

        Course course =
                Course.builder()
                        .id(
                                Math.abs(
                                        courseCode.hashCode()))
                        .courseCode(courseCode)
                        .build();

        CourseType courseType =
                CourseType.builder()
                        .id(3)
                        .code(courseTypeCode)
                        .name(courseTypeCode)
                        .nameVn(courseTypeCode)
                        .build();

        return CourseProgram.builder()
                .id(
                        Math.abs(
                                courseCode.hashCode()))
                .course(course)
                .program(program)
                .cohort(cohort)
                .courseType(courseType)
                .semesterSuggest(semester)
                .build();
    }

    private Integer resolveCurriculumSemester(
            CourseProgram selected,
            Integer programId,
            Integer cohortId,
            String importedSemester)
            throws Exception {

        Method method =
                SyllabusImportServiceImpl.class
                        .getDeclaredMethod(
                                "resolveCurriculumSemester",
                                CourseProgram.class,
                                Integer.class,
                                Integer.class,
                                String.class);

        method.setAccessible(true);

        return (Integer) method.invoke(
                service,
                selected,
                programId,
                cohortId,
                importedSemester);
    }

    private Integer resolveAutoReconciledSemester(
            SyllabusImportData data,
            String courseTypeCode)
            throws Exception {

        Method method =
                SyllabusImportServiceImpl.class
                        .getDeclaredMethod(
                                "resolveAutoReconciledSemester",
                                SyllabusImportData.class,
                                String.class);

        method.setAccessible(true);

        return (Integer) method.invoke(
                service,
                data,
                courseTypeCode);
    }
}