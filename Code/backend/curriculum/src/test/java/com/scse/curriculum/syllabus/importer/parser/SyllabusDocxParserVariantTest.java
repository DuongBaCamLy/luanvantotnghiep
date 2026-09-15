package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportIssue;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyllabusDocxParserVariantTest {

    private final SyllabusDocxParser parser =
            new SyllabusDocxParser();

    @Test
    void parsesCourseCodeWithAlternativeSemanticHeading() throws Exception {

        byte[] bytes;

        try (XWPFDocument document =
                     new XWPFDocument();
             ByteArrayOutputStream output =
                     new ByteArrayOutputStream()) {

            document.createParagraph()
                    .createRun()
                    .setText("Course Name: Algorithms");

            document.createParagraph()
                    .createRun()
                    .setText("Module Code: IT200IU");

            document.write(output);
            bytes = output.toByteArray();
        }

        List<SyllabusImportIssue> issues =
                new ArrayList<>();

        SyllabusImportData data =
                parser.parse(
                        new ByteArrayInputStream(bytes),
                        issues
                );

        assertEquals(
                "IT200IU",
                data.getSourceCourseCode()
        );
    }
    @Test
void parsesCourseNameWithAlternativeSemanticHeading() throws Exception {

    byte[] bytes;

    try (XWPFDocument document =
                 new XWPFDocument();
         ByteArrayOutputStream output =
                 new ByteArrayOutputStream()) {

        document.createParagraph()
                .createRun()
                .setText("Module Name: Algorithms");

        document.createParagraph()
                .createRun()
                .setText("Module Code: IT200IU");

        document.write(output);
        bytes = output.toByteArray();
    }

    List<SyllabusImportIssue> issues =
            new ArrayList<>();

    SyllabusImportData data =
            parser.parse(
                    new ByteArrayInputStream(bytes),
                    issues
            );

    assertEquals(
            "Algorithms",
            data.getSourceCourseName()
    );
}
@Test
void parsesAlternativeGeneralInformationLabels() throws Exception {

    byte[] bytes;

    try (XWPFDocument document =
                 new XWPFDocument();
         ByteArrayOutputStream output =
                 new ByteArrayOutputStream()) {

        document.createParagraph()
                .createRun()
                .setText("Course Name: Algorithms");

        document.createParagraph()
                .createRun()
                .setText("Course Code: IT200IU");

        var table =
                document.createTable(8, 2);

        table.getRow(0).getCell(0)
                .setText("Course classification");
        table.getRow(0).getCell(1)
                .setText("Core course");

        table.getRow(1).getCell(0)
                .setText("Course coordinator");
        table.getRow(1).getCell(1)
                .setText("Dr. Nguyen");

        table.getRow(2).getCell(0)
                .setText("Medium of instruction");
        table.getRow(2).getCell(1)
                .setText("English");

        table.getRow(3).getCell(0)
                .setText("Curriculum relation");
        table.getRow(3).getCell(1)
                .setText("Computer Science core");

        table.getRow(4).getCell(0)
                .setText("Instructional methods");
        table.getRow(4).getCell(1)
                .setText("Lecture and laboratory");

        table.getRow(5).getCell(0)
                .setText("Study load");
        table.getRow(5).getCell(1)
                .setText(
                        "Total: 135; "
                                + "45 contact periods; "
                                + "90 self-study hours"
                );

        table.getRow(6).getCell(0)
                .setText("Number of credits");
        table.getRow(6).getCell(1)
                .setText(
                        "4 credits; "
                                + "Theory: 3; "
                                + "Practice: 1"
                );

        table.getRow(7).getCell(0)
                .setText("Required prerequisites");
        table.getRow(7).getCell(1)
                .setText("Introduction to Computing");

        document.write(output);
        bytes = output.toByteArray();
    }

    List<SyllabusImportIssue> issues =
            new ArrayList<>();

    SyllabusImportData data =
            parser.parse(
                    new ByteArrayInputStream(bytes),
                    issues
            );

    assertEquals(
            "Core course",
            data.getCourseDesignation()
    );

    assertEquals(
            "Dr. Nguyen",
            data.getPersonResponsible()
    );

    assertEquals(
            "English",
            data.getLanguage()
    );

    assertEquals(
            "Computer Science core",
            data.getRelation()
    );

    assertEquals(
            "Lecture and laboratory",
            data.getTeachingMethods()
    );

    assertEquals(
            "135",
            data.getWorkloadTotal()
    );

    assertEquals(
            "45",
            data.getWorkloadContact()
    );

    assertEquals(
            "90",
            data.getWorkloadPrivate()
    );

    assertEquals(
            "4",
            data.getCreditPoints()
    );

    assertEquals(
            "3",
            data.getLectureCredits()
    );

    assertEquals(
            "1",
            data.getLaboratoryCredits()
    );

    assertEquals(
            "Introduction to Computing",
            data.getPrerequisites()
    );
}
@Test
void parsesObjectivesWithAlternativeSemanticHeading() throws Exception {

    byte[] bytes;

    try (XWPFDocument document =
                 new XWPFDocument();
         ByteArrayOutputStream output =
                 new ByteArrayOutputStream()) {

        document.createParagraph()
                .createRun()
                .setText("Course Name: Algorithms");

        document.createParagraph()
                .createRun()
                .setText("Course Code: IT200IU");

        document.createParagraph()
                .createRun()
                .setText("Course aims");

        document.createParagraph()
                .createRun()
                .setText(
                        "Provide students with foundational "
                                + "algorithmic problem-solving knowledge."
                );

        document.createParagraph()
                .createRun()
                .setText("Course learning outcomes");

        document.write(output);
        bytes = output.toByteArray();
    }

    List<SyllabusImportIssue> issues =
            new ArrayList<>();

    SyllabusImportData data =
            parser.parse(
                    new ByteArrayInputStream(bytes),
                    issues
            );

    assertEquals(
            "Provide students with foundational algorithmic problem-solving knowledge.",
            data.getObjectives()
    );
}
@Test
void parsesClosWithAlternativeSemanticHeading() throws Exception {

    byte[] bytes;

    try (XWPFDocument document =
                 new XWPFDocument();
         ByteArrayOutputStream output =
                 new ByteArrayOutputStream()) {

        document.createParagraph()
                .createRun()
                .setText("Course Name: Algorithms");

        document.createParagraph()
                .createRun()
                .setText("Course Code: IT200IU");

        document.createParagraph()
                .createRun()
                .setText("Learning outcomes");

        var table =
                document.createTable(2, 3);

        table.getRow(0).getCell(0)
                .setText("Level");
        table.getRow(0).getCell(1)
                .setText("CLO");
        table.getRow(0).getCell(2)
                .setText("Description");

        table.getRow(1).getCell(0)
                .setText("Knowledge");
        table.getRow(1).getCell(1)
                .setText("CLO1");
        table.getRow(1).getCell(2)
                .setText("Explain fundamental algorithm concepts.");

        document.write(output);
        bytes = output.toByteArray();
    }

    List<SyllabusImportIssue> issues =
            new ArrayList<>();

    SyllabusImportData data =
            parser.parse(
                    new ByteArrayInputStream(bytes),
                    issues
            );

    assertEquals(
            1,
            data.getClos().size()
    );

    assertEquals(
            "CLO1",
            data.getClos().getFirst().getCode()
    );

    assertEquals(
            "Explain fundamental algorithm concepts.",
            data.getClos().getFirst().getDescription()
    );
}
@Test
void parsesTopicsWithAlternativeSemanticHeading() throws Exception {

    byte[] bytes;

    try (XWPFDocument document =
                 new XWPFDocument();
         ByteArrayOutputStream output =
                 new ByteArrayOutputStream()) {

        document.createParagraph()
                .createRun()
                .setText("Course Name: Algorithms");

        document.createParagraph()
                .createRun()
                .setText("Course Code: IT200IU");

        document.createParagraph()
                .createRun()
                .setText("Course topics");

        var table =
                document.createTable(2, 4);

        table.getRow(0).getCell(0)
                .setText("Topic");

        table.getRow(0).getCell(1)
                .setText("Weight");

        table.getRow(0).getCell(2)
                .setText("Level");

        table.getRow(0).getCell(3)
                .setText("CLO");

        table.getRow(1).getCell(0)
                .setText("Sorting algorithms");

        table.getRow(1).getCell(1)
                .setText("20");

        table.getRow(1).getCell(2)
                .setText("I");

        table.getRow(1).getCell(3)
                .setText("CLO1");

        document.write(output);
        bytes = output.toByteArray();
    }

    List<SyllabusImportIssue> issues =
            new ArrayList<>();

    SyllabusImportData data =
            parser.parse(
                    new ByteArrayInputStream(bytes),
                    issues
            );

    assertEquals(
            1,
            data.getTopics().size()
    );

    assertEquals(
            "Sorting algorithms",
            data.getTopics().getFirst().getName()
    );

    assertEquals(
            "20",
            data.getTopics().getFirst().getContentWeight()
    );

    assertEquals(
            "I",
            data.getTopics().getFirst().getContentLevel()
    );
}
@Test
void parsesWeeklyPlanWithAlternativeSemanticHeading() throws Exception {

    byte[] bytes;

    try (XWPFDocument document =
                 new XWPFDocument();
         ByteArrayOutputStream output =
                 new ByteArrayOutputStream()) {

        document.createParagraph()
                .createRun()
                .setText("Course Name: Algorithms");

        document.createParagraph()
                .createRun()
                .setText("Course Code: IT200IU");

        document.createParagraph()
                .createRun()
                .setText("Teaching and learning activities");

        var table =
                document.createTable(2, 6);

        table.getRow(0).getCell(0)
                .setText("Week");
        table.getRow(0).getCell(1)
                .setText("Topic");
        table.getRow(0).getCell(2)
                .setText("CLO");
        table.getRow(0).getCell(3)
                .setText("Assessment");
        table.getRow(0).getCell(4)
                .setText("Learning activities");
        table.getRow(0).getCell(5)
                .setText("Resources");

        table.getRow(1).getCell(0)
                .setText("1");
        table.getRow(1).getCell(1)
                .setText("Introduction to algorithms");
        table.getRow(1).getCell(2)
                .setText("CLO1");
        table.getRow(1).getCell(3)
                .setText("Quiz 1");
        table.getRow(1).getCell(4)
                .setText("Lecture and exercises");
        table.getRow(1).getCell(5)
                .setText("Chapter 1");

        document.write(output);
        bytes = output.toByteArray();
    }

    List<SyllabusImportIssue> issues =
            new ArrayList<>();

    SyllabusImportData data =
            parser.parse(
                    new ByteArrayInputStream(bytes),
                    issues
            );

    assertEquals(
            1,
            data.getWeeklyActivities().size()
    );

    assertEquals(
            1,
            data.getWeeklyActivities().getFirst().getWeek()
    );

    assertEquals(
            "Introduction to algorithms",
            data.getWeeklyActivities().getFirst().getTopic()
    );

    assertEquals(
            "CLO1",
            data.getWeeklyActivities().getFirst().getClo()
    );

    assertEquals(
            "Lecture and exercises",
            data.getWeeklyActivities().getFirst().getLearningActivities()
    );
}
@Test
void parsesAssessmentsWithAlternativeSemanticHeading() throws Exception {

    byte[] bytes;

    try (XWPFDocument document =
                 new XWPFDocument();
         ByteArrayOutputStream output =
                 new ByteArrayOutputStream()) {

        document.createParagraph()
                .createRun()
                .setText("Course Name: Algorithms");

        document.createParagraph()
                .createRun()
                .setText("Course Code: IT200IU");

        document.createParagraph()
                .createRun()
                .setText("Assessment scheme");

        var table =
                document.createTable(2, 2);

        table.getRow(0).getCell(0)
                .setText("Assessment");

        table.getRow(0).getCell(1)
                .setText("Details");

        table.getRow(1).getCell(0)
                .setText("Final examination (60%)");

        table.getRow(1).getCell(1)
                .setText("Written examination");

        document.write(output);
        bytes = output.toByteArray();
    }

    List<SyllabusImportIssue> issues =
            new ArrayList<>();

    SyllabusImportData data =
            parser.parse(
                    new ByteArrayInputStream(bytes),
                    issues
            );

    assertEquals(
            1,
            data.getAssessments().size()
    );

    assertEquals(
            "Final examination (60%)",
            data.getAssessments().getFirst().getName()
    );

    assertEquals(
            60.0f,
            data.getAssessments().getFirst().getWeightPercent()
    );

    assertEquals(
            "FINAL_EXAM",
            data.getAssessments().getFirst().getAssessmentType()
    );
}
@Test
void parsesReadingListWithAlternativeSemanticHeading() throws Exception {

    byte[] bytes;

    try (XWPFDocument document =
                 new XWPFDocument();
         ByteArrayOutputStream output =
                 new ByteArrayOutputStream()) {

        document.createParagraph()
                .createRun()
                .setText("Course Name: Algorithms");

        document.createParagraph()
                .createRun()
                .setText("Course Code: IT200IU");

        document.createParagraph()
                .createRun()
                .setText("References");

        document.createParagraph()
                .createRun()
                .setText(
                        "[1] Thomas H. Cormen, "
                                + "Introduction to Algorithms, 2022"
                );

        document.createParagraph()
                .createRun()
                .setText("Date revised: 01/09/2026");

        document.write(output);
        bytes = output.toByteArray();
    }

    List<SyllabusImportIssue> issues =
            new ArrayList<>();

    SyllabusImportData data =
            parser.parse(
                    new ByteArrayInputStream(bytes),
                    issues
            );

    assertEquals(
            1,
            data.getReadings().size()
    );

    assertEquals(
            "Thomas H. Cormen, Introduction to Algorithms, 2022",
            data.getReadings().getFirst().getTitle()
    );

    assertEquals(
            2022,
            data.getReadings().getFirst().getYear()
    );
}
@Test
void parsesRevisionDateWithAlternativeHeadingAndIsoFormat() throws Exception {

    byte[] bytes;

    try (XWPFDocument document =
                 new XWPFDocument();
         ByteArrayOutputStream output =
                 new ByteArrayOutputStream()) {

        document.createParagraph()
                .createRun()
                .setText("Course Name: Algorithms");

        document.createParagraph()
                .createRun()
                .setText("Course Code: IT200IU");

        document.createParagraph()
                .createRun()
                .setText("Last revised: 2026-09-01");

        document.write(output);
        bytes = output.toByteArray();
    }

    List<SyllabusImportIssue> issues =
            new ArrayList<>();

    SyllabusImportData data =
            parser.parse(
                    new ByteArrayInputStream(bytes),
                    issues
            );

    assertEquals(
            "2026-09-01",
            data.getDateRevised()
    );
}
@Test
void parsesCloPloMatrixWithAlternativeSemanticHeading() throws Exception {

    byte[] bytes;

    try (XWPFDocument document =
                 new XWPFDocument();
         ByteArrayOutputStream output =
                 new ByteArrayOutputStream()) {

        document.createParagraph()
                .createRun()
                .setText("Course Name: Algorithms");

        document.createParagraph()
                .createRun()
                .setText("Course Code: IT200IU");

        document.createParagraph()
                .createRun()
                .setText("CLO-PLO Mapping Matrix");

        var table =
                document.createTable(2, 3);

        table.getRow(0).getCell(0)
                .setText("CLO");
        table.getRow(0).getCell(1)
                .setText("PLO1");
        table.getRow(0).getCell(2)
                .setText("PLO2");

        table.getRow(1).getCell(0)
                .setText("1");
        table.getRow(1).getCell(1)
                .setText("x");
        table.getRow(1).getCell(2)
                .setText("");

        document.write(output);
        bytes = output.toByteArray();
    }

    List<SyllabusImportIssue> issues =
            new ArrayList<>();

    SyllabusImportData data =
            parser.parse(
                    new ByteArrayInputStream(bytes),
                    issues
            );

    assertEquals(
            1,
            data.getClos().size()
    );

    assertEquals(
            "CLO1",
            data.getClos().getFirst().getCode()
    );

    assertEquals(
            1,
            data.getCloPloMappings().size()
    );

    assertEquals(
            "CLO1",
            data.getCloPloMappings().getFirst().getCloCode()
    );

    assertEquals(
            "PLO1",
            data.getCloPloMappings().getFirst().getPloCode()
    );
}
}