package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyllabusPdfCs2026WorkloadRegressionTest {

    private final SyllabusPdfParser parser =
            new SyllabusPdfParser();

    @Test
    void parsesInterleavedCs2026WorkloadLayoutsWithoutWrapperContamination()
            throws Exception {

        assertWorkload(
                """
                Workload (incl. contact hours, Total workload: 195
                self-study hours) Contact hours (please specify whether lecture, exercise,
                laboratory session, etc.): 45 (lecture) + 30 (laboratory)
                Private study including examination preparation, specified in
                hours: 120
                """,
                "195",
                "45 (lecture) + 30 (laboratory)",
                "120",
                null
        );

        assertWorkload(
                """
                Workload (incl. Contact hours: 300 hours
                contact hours, self- Private study including examination preparation, specified in hours:
                study hours) 300
                """,
                null,
                "300 hours",
                "300",
                null
        );

        assertWorkload(
                """
                Workload (incl. contact hours, (Estimated) Total workload: 182.5 hours
                self-study hours) Contact hours (please specify whether lecture, exercise,
                laboratory session, etc.): Lecture: 37.5 hours + Laboratory: 25
                hours
                Private study including examination preparation, specified in
                hours: 120 hours
                """,
                "182.5 hours",
                "Lecture: 37.5 hours + Laboratory: 25 hours",
                "120 hours",
                null
        );

        assertWorkload(
                """
                Workload (incl. (Estimated) Total workload: 195 hours. Contact hours: Lecture
                contact hours, self- 45 hours, Lab 30 hours: Private hours: 120 hours. Student
                study hours) responsibility: Students are expected to spend at least 8 hours
                per week for self – studying. This time should be made up of
                reading, working on exercises and problems and group
                assignment.
                """,
                "195 hours.",
                "Lecture 45 hours, Lab 30 hours",
                "120 hours.",
                "Students are expected to spend at least 8 hours per week for self – studying. "
                        + "This time should be made up of reading, working on exercises and problems "
                        + "and group assignment."
        );

        assertWorkload(
                """
                Workload (incl. contact hours, (Estimated) Total workload: 195
                self-study hours) Contact hours (please specify whether lecture, exercise, laboratory
                session, etc.): 45 (lecture) + 30 (laboratory)
                Private study including examination preparation, specified in hours:
                120
                """,
                "195",
                "45 (lecture) + 30 (laboratory)",
                "120",
                null
        );

        assertWorkload(
                """
                workload: 135
                Contact hours (please specify whether lecture, exercise,
                Workload (incl.
                laboratory session, etc.): 45 (lecture)
                contact hours, self-
                Private study including examination preparation, specified in
                study hours)
                hours: 90

                Number of credits : 3
                """,
                "135",
                "45 (lecture)",
                "90",
                null
        );

        assertWorkload(
                """
                Workload (incl. contact hours, self- Total workload: 135
                study hours) Contact hours (please specify whether lecture,
                exercise, laboratory session, etc.): 45 (lecture)
                Private study including examination preparation,
                specified in hours: 90
                """,
                "135",
                "45 (lecture)",
                "90",
                null
        );

        assertWorkload(
                """
                Workload (incl. Total workload: 90 hours
                contact hours, self- Private study including examination preparation, specified in hours: 90
                study hours)
                """,
                "90 hours",
                null,
                "90",
                null
        );

        assertWorkload(
                """
                Workload (incl. (Estimated) Total workload: 60
                contact hours, self- Contact hours (please specify whether lecture, exercise, laboratory session, etc.):
                study hours) lecture: 30
                Private study including examination preparation, specified in hours: 30
                """,
                "60",
                "lecture: 30",
                "30",
                null
        );
    }

    private void assertWorkload(
            String extracted,
            String expectedTotal,
            String expectedContact,
            String expectedPrivate,
            String expectedResponsibility)
            throws Exception {

        SyllabusImportData data =
                SyllabusImportData.builder()
                        .build();

        Method method =
                SyllabusPdfParser.class.getDeclaredMethod(
                        "parseWorkload",
                        String.class,
                        SyllabusImportData.class);

        method.setAccessible(true);

        method.invoke(
                parser,
                extracted,
                data);

        assertEquals(
                expectedTotal,
                data.getWorkloadTotal(),
                "workloadTotal");

        assertEquals(
                expectedContact,
                data.getWorkloadContact(),
                "workloadContact");

        assertEquals(
                expectedPrivate,
                data.getWorkloadPrivate(),
                "workloadPrivate");

        assertEquals(
                expectedResponsibility,
                data.getWorkloadStudentResponsibility(),
                "workloadStudentResponsibility");
    }
}