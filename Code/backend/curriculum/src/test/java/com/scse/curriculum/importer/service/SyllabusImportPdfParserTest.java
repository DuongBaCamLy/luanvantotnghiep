package com.scse.curriculum.importer.service;


import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportIssue;
import com.scse.curriculum.syllabus.importer.parser.SyllabusPdfParser;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;



@SpringBootTest
class SyllabusImportPdfParserTest {



    @Autowired
    private SyllabusPdfParser syllabusPdfParser;



    @Test
    void parsesRealIt116PdfWithoutMissingTopicWeeks() throws Exception {



        List<SyllabusImportIssue> issues = new ArrayList<>();


        try (InputStream in =
             getClass()
             .getResourceAsStream("/syllabus-import/CS-IT116.pdf")) {



            assertNotNull(
                    in,
                    "Regression PDF must be available in test resources"
            );



            SyllabusImportData data =
                    syllabusPdfParser.parsePdf(
                            in,
                            issues
                    );





            /*
             * Source information
             */


            assertEquals(
                    "IT116",
                    data.getSourceCourseCode()
            );


            assertEquals(
                    "C/C++ Programming",
                    data.getSourceCourseName()
            );





            /*
             * CLO / Topic / Assessment
             */


            assertEquals(
                    3,
                    data.getClos().size()
            );


            assertEquals(
                    15,
                    data.getTopics().size()
            );


            assertEquals(
                    4,
                    data.getAssessments().size()
            );


            assertEquals(
                    1,
                    data.getReadings().size()
            );







            /*
             * Validate topic weeks
             */


            assertTrue(
                    data.getTopics()
                    .stream()
                    .allMatch(
                            topic ->
                            topic.getWeekNumber() != null
                    ),
                    () ->
                    "Topics without week: "
                    +
                    data.getTopics()
                    .stream()
                    .filter(
                            topic ->
                            topic.getWeekNumber() == null
                    )
                    .map(
                            topic ->
                            topic.getName()
                    )
                    .collect(Collectors.toList())
            );





            assertEquals(
                    Set.of(
                            1,
                            2,
                            3,
                            4,
                            5,
                            6,
                            7,
                            8,
                            9,
                            10,
                            11,
                            12,
                            13,
                            14,
                            15
                    ),
                    data.getTopics()
                    .stream()
                    .map(
                            topic ->
                            topic.getWeekNumber()
                    )
                    .collect(
                            Collectors.toSet()
                    )
            );







            /*
             * Topic - CLO Mapping
             */


            assertEquals(
                    27,
                    data.getTopicCloMappings().size(),
                    "Weeks 1-3 map one CLO each and weeks 4-15 map two CLOs each"
            );








            /*
             * CLO - PLO Mapping
             */


            assertTrue(
                    data.getCloPloMappings()
                    .stream()
                    .allMatch(
                            mapping ->
                            mapping.getContributionWeight() != null
                    ),
                    "Every imported CLO-PLO mapping must have a database-safe contribution weight"
            );








            /*
             * Parser issues
             */


            assertTrue(
                    issues.stream()
                    .noneMatch(
                            issue ->
                            "ERROR"
                            .equalsIgnoreCase(
                                    issue.getSeverity()
                            )
                    ),
                    () ->
                    "Unexpected parser errors: "
                    + issues
            );



        }

    }

}