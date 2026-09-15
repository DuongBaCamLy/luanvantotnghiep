package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyllabusPdfParserVariantTest {

    private final SyllabusPdfParser parser =
            new SyllabusPdfParser();

    @Test
    void parsesColonClosAndMatrixWithoutFixedSectionNumbers() {

        String text = """
                Course learning outcomes

                CLO1: Understand basic computing concepts.
                CLO2: Apply computing concepts to solve problems.

                Competency level

                0. Learning Outcomes Matrix
                1 x
                2  xx

                0. Planned learning activities and teaching methods
                """;

        SyllabusImportData data =
                new SyllabusImportData();

        parser.parseClos(
                text,
                data
        );

        parser.parseCloPloMatrix(
                text,
                data
        );

        assertEquals(
                List.of(
                        "CLO1",
                        "CLO2"
                ),
                data.getClos()
                        .stream()
                        .map(clo -> clo.getCode())
                        .toList()
        );

        assertEquals(
                2,
                data.getCloPloMappings()
                        .size()
        );

        assertEquals(
                "CLO1",
                data.getCloPloMappings()
                        .get(0)
                        .getCloCode()
        );

        assertEquals(
                "PLO1",
                data.getCloPloMappings()
                        .get(0)
                        .getPloCode()
        );

        assertEquals(
                "x",
                data.getCloPloMappings()
                        .get(0)
                        .getValue()
        );

        assertEquals(
                "CLO2",
                data.getCloPloMappings()
                        .get(1)
                        .getCloCode()
        );

        assertEquals(
                "PLO2",
                data.getCloPloMappings()
                        .get(1)
                        .getPloCode()
        );

        assertEquals(
                "xx",
                data.getCloPloMappings()
                        .get(1)
                        .getValue()
        );
    }

    @Test
void doesNotCreateCloFromCompetencyReferences() {

    String text = """
            Course learning outcomes

            CLO 1. Outcome one.
            CLO 2. Outcome two.
            CLO 3. Outcome three.
            CLO 4. Outcome four.
            CLO 5. Outcome five.

            Competency level Course learning outcome (CLO)
            Knowledge CLO1, CLO2, CLO4, CLO5
            Skill CLO2, CLO3, CLO4, CLO6
            Attitude

            Content The description of the contents
            """;

    SyllabusImportData data =
            new SyllabusImportData();

    parser.parseClos(
            text,
            data
    );

    assertEquals(
            List.of(
                    "CLO1",
                    "CLO2",
                    "CLO3",
                    "CLO4",
                    "CLO5"
            ),
            data.getClos()
                    .stream()
                    .map(clo -> clo.getCode())
                    .toList()
    );
}

    @Test
void parsesClosWithoutPunctuationAfterNumber() {

    String text = """
            Course learning outcomes

            CLO 1. Define DevOps principles.
            CLO 2 Explain the benefit of DevOps.
            CLO 3 Understand infrastructure automation.
            CLO 4. Work with common DevOps tools.

            Competency level
            """;

    SyllabusImportData data =
            new SyllabusImportData();

    parser.parseClos(
            text,
            data
    );

    assertEquals(
            List.of(
                    "CLO1",
                    "CLO2",
                    "CLO3",
                    "CLO4"
            ),
            data.getClos()
                    .stream()
                    .map(clo -> clo.getCode())
                    .toList()
    );
}
@Test
void parsesObjectivesWithAlternativeSemanticHeadings() {

    String text = """
            Course aims

            Develop foundational programming knowledge and
            problem-solving skills for software development.

            Learning outcomes

            CLO1: Apply programming concepts to solve problems.
            """;

    assertEquals(
            "Develop foundational programming knowledge and problem-solving skills for software development.",
            parser.parseObjectives(text)
    );
}
@Test
void stopsCloParsingAtAlternativeCompetencyHeading() {

    String text = """
            Learning outcomes

            CLO1: Explain fundamental programming concepts.
            CLO2: Apply programming concepts to solve problems.

            Competency classification
            Knowledge CLO1, CLO2

            Content
            Introduction to programming
            """;

    SyllabusImportData data =
            new SyllabusImportData();

    parser.parseClos(
            text,
            data
    );

    assertEquals(
            2,
            data.getClos().size()
    );

    assertEquals(
            "Explain fundamental programming concepts.",
            data.getClos().get(0).getDescription()
    );

    assertEquals(
            "Apply programming concepts to solve problems.",
            data.getClos().get(1).getDescription()
    );
}
@Test
void parsesCloPloMatrixWithAlternativeSemanticHeading() {

    String text = """
            CLO-PLO Mapping Matrix

            1 x
            2  xx

            Teaching and learning activities
            """;

    SyllabusImportData data =
            new SyllabusImportData();

    parser.parseCloPloMatrix(
            text,
            data
    );

    assertEquals(
            2,
            data.getCloPloMappings().size()
    );

    assertEquals(
            "CLO1",
            data.getCloPloMappings()
                    .get(0)
                    .getCloCode()
    );

    assertEquals(
            "PLO1",
            data.getCloPloMappings()
                    .get(0)
                    .getPloCode()
    );

    assertEquals(
            "x",
            data.getCloPloMappings()
                    .get(0)
                    .getValue()
    );

    assertEquals(
            "CLO2",
            data.getCloPloMappings()
                    .get(1)
                    .getCloCode()
    );

    assertEquals(
            "PLO2",
            data.getCloPloMappings()
                    .get(1)
                    .getPloCode()
    );

    assertEquals(
            "xx",
            data.getCloPloMappings()
                    .get(1)
                    .getValue()
    );
}
@Test
void parsesAssessmentPlanWithoutFixedSectionNumber() throws Exception {

    String text = """
            Assessment plan

            Assessment Type        CLO1
            Final examination (100%) 100%

            Rubrics (optional)
            """;

    SyllabusImportData data =
            new SyllabusImportData();

    Method parseAssessments =
            SyllabusPdfParser.class.getDeclaredMethod(
                    "parseAssessments",
                    String.class,
                    SyllabusImportData.class
            );

    parseAssessments.setAccessible(true);

    parseAssessments.invoke(
            parser,
            text,
            data
    );

    assertEquals(
            1,
            data.getAssessments().size()
    );

    assertEquals(
            "Final examination",
            data.getAssessments()
                    .get(0)
                    .getName()
    );

    assertEquals(
            "FINAL_EXAM",
            data.getAssessments()
                    .get(0)
                    .getAssessmentType()
    );

    assertEquals(
            100f,
            data.getAssessments()
                    .get(0)
                    .getWeightPercent()
    );
}
@Test
void parsesTopicsWithAlternativeCourseContentHeading() throws Exception {

    String text = """
            Course content

            Topic Weight Level
            Data structures and algorithms 25 I, T
            Object-oriented programming 30 T, U

            Assessment plan
            """;

    SyllabusImportData data =
            new SyllabusImportData();

    Method parseContent =
            SyllabusPdfParser.class.getDeclaredMethod(
                    "parseContent",
                    String.class,
                    SyllabusImportData.class
            );

    parseContent.setAccessible(true);

    parseContent.invoke(
            parser,
            text,
            data
    );

    assertEquals(
            2,
            data.getTopics().size()
    );

    assertEquals(
            "Data structures and algorithms",
            data.getTopics()
                    .get(0)
                    .getName()
    );

    assertEquals(
            "25",
            data.getTopics()
                    .get(0)
                    .getContentWeight()
    );

    assertEquals(
            "I, T",
            data.getTopics()
                    .get(0)
                    .getContentLevel()
    );

    assertEquals(
            "Object-oriented programming",
            data.getTopics()
                    .get(1)
                    .getName()
    );

    assertEquals(
            "30",
            data.getTopics()
                    .get(1)
                    .getContentWeight()
    );

    assertEquals(
            "T, U",
            data.getTopics()
                    .get(1)
                    .getContentLevel()
    );
}
@Test
void parsesWeeklyActivitiesWithAlternativeSemanticHeading() throws Exception {

    String text = """
            Teaching and learning activities

            1 Introduction to programming 1 Quiz 1

            Assessment plan
            """;

    SyllabusImportData data =
            new SyllabusImportData();

    Method parseWeeklyActivities =
            SyllabusPdfParser.class.getDeclaredMethod(
                    "parseWeeklyActivities",
                    String.class,
                    SyllabusImportData.class
            );

    parseWeeklyActivities.setAccessible(true);

    parseWeeklyActivities.invoke(
            parser,
            text,
            data
    );

    assertEquals(
            1,
            data.getWeeklyActivities().size()
    );

    assertEquals(
            1,
            data.getWeeklyActivities()
                    .get(0)
                    .getWeek()
    );

    assertEquals(
            "Introduction to programming",
            data.getWeeklyActivities()
                    .get(0)
                    .getTopic()
    );

    assertEquals(
            "1",
            data.getWeeklyActivities()
                    .get(0)
                    .getClo()
    );

    assertEquals(
            "Quiz",
            data.getWeeklyActivities()
                    .get(0)
                    .getAssessments()
    );
}
@Test
void parsesProjectAssessmentFromAlternativeTemplate() throws Exception {

    String text = """
            Assessment plan

            Assessment Type        CLO1
            Project (25%)          100%

            Rubrics (optional)
            """;

    SyllabusImportData data =
            new SyllabusImportData();

    Method parseAssessments =
            SyllabusPdfParser.class.getDeclaredMethod(
                    "parseAssessments",
                    String.class,
                    SyllabusImportData.class
            );

    parseAssessments.setAccessible(true);

    parseAssessments.invoke(
            parser,
            text,
            data
    );

    assertEquals(
            1,
            data.getAssessments().size()
    );

    assertEquals(
            "Project",
            data.getAssessments()
                    .get(0)
                    .getName()
    );

    assertEquals(
            "PROJECT",
            data.getAssessments()
                    .get(0)
                    .getAssessmentType()
    );

    assertEquals(
            25f,
            data.getAssessments()
                    .get(0)
                    .getWeightPercent()
    );
}
@Test
void parsesReadingListWithReferencesHeading() throws Exception {

    String text = """
            References

            1. Robert Sedgewick, Algorithms, Addison-Wesley, 2011

            CLO-PLO Mapping Matrix
            """;

    SyllabusImportData data =
            new SyllabusImportData();

    Method parseReadingList =
            SyllabusPdfParser.class.getDeclaredMethod(
                    "parseReadingList",
                    String.class,
                    SyllabusImportData.class
            );

    parseReadingList.setAccessible(true);

    parseReadingList.invoke(
            parser,
            text,
            data
    );

    assertEquals(
            1,
            data.getReadings().size()
    );

    assertEquals(
            "Robert Sedgewick",
            data.getReadings()
                    .get(0)
                    .getAuthor()
    );

    assertEquals(
            "Algorithms",
            data.getReadings()
                    .get(0)
                    .getTitle()
    );

    assertEquals(
            "Addison-Wesley",
            data.getReadings()
                    .get(0)
                    .getPublisher()
    );

    assertEquals(
            2011,
            data.getReadings()
                    .get(0)
                    .getYear()
    );
}
@Test
void parsesRevisionDateWithAlternativeHeadingAndIsoFormat() throws Exception {

    String text = """
            Last revised: 2026-09-01
            """;

    SyllabusImportData data =
            new SyllabusImportData();

    Method parseRevisionDate =
            SyllabusPdfParser.class.getDeclaredMethod(
                    "parseRevisionDate",
                    String.class,
                    SyllabusImportData.class
            );

    parseRevisionDate.setAccessible(true);

    parseRevisionDate.invoke(
            parser,
            text,
            data
    );

    assertEquals(
            "2026-09-01",
            data.getDateRevised()
    );
}
@Test
void parsesExaminationFieldsWithAlternativeSemanticHeadings() {

    String text = """
            Examination methods

            Written examination

            Examination requirements

            Students must complete all required assessments.

            References
            """;

    SyllabusImportData data =
            new SyllabusImportData();

    parser.parseExaminationFields(
            text,
            data
    );

    assertEquals(
            "Written examination",
            data.getExamForms()
    );

    assertEquals(
            "Students must complete all required assessments.",
            data.getExamRequirements()
    );
}
@Test
void parsesPrerequisitesWithAlternativeSemanticHeading() throws Exception {

    String text = """
            Course Name: Algorithms
            Course Code: IT200IU

            Prerequisites

            Data Structures

            Course aims

            Develop algorithmic problem-solving skills.
            """;

    SyllabusImportData data =
            new SyllabusImportData();

    Method parseGeneralInformation =
            SyllabusPdfParser.class.getDeclaredMethod(
                    "parseGeneralInformation",
                    String.class,
                    SyllabusImportData.class
            );

    parseGeneralInformation.setAccessible(true);

    parseGeneralInformation.invoke(
            parser,
            text,
            data
    );

    assertEquals(
            "Data Structures",
            data.getPrerequisites()
    );
}
@Test
void parsesSemesterWithAlternativeSemanticHeading() throws Exception {

    String text = """
            Course Name: Algorithms
            Course Code: IT200IU

            Semester: 4

            Person responsible for the course
            Dr. Nguyen
            """;

    SyllabusImportData data =
            new SyllabusImportData();

    Method parseGeneralInformation =
            SyllabusPdfParser.class.getDeclaredMethod(
                    "parseGeneralInformation",
                    String.class,
                    SyllabusImportData.class
            );

    parseGeneralInformation.setAccessible(true);

    parseGeneralInformation.invoke(
            parser,
            text,
            data
    );

    assertEquals(
            "Semester 4",
            data.getSemester()
    );
}
@Test
void parsesCreditsWithAlternativeSemanticHeadings() throws Exception {

    String text = """
            Course Name: Algorithms
            Course Code: IT200IU

            Credits: 4
            Lecture credits: 3
            Laboratory credits: 1

            Prerequisites
            Data Structures
            """;

    SyllabusImportData data =
            new SyllabusImportData();

    Method parseGeneralInformation =
            SyllabusPdfParser.class.getDeclaredMethod(
                    "parseGeneralInformation",
                    String.class,
                    SyllabusImportData.class
            );

    parseGeneralInformation.setAccessible(true);

    parseGeneralInformation.invoke(
            parser,
            text,
            data
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
}
@Test
void parsesPersonResponsibleWithAlternativeSemanticHeading() throws Exception {

    String text = """
            Course Name: Algorithms
            Course Code: IT200IU

            Course coordinator: Dr. Nguyen Van An

            Language: English
            """;

    SyllabusImportData data =
            new SyllabusImportData();

    Method parseGeneralInformation =
            SyllabusPdfParser.class.getDeclaredMethod(
                    "parseGeneralInformation",
                    String.class,
                    SyllabusImportData.class
            );

    parseGeneralInformation.setAccessible(true);

    parseGeneralInformation.invoke(
            parser,
            text,
            data
    );

    assertEquals(
            "Dr. Nguyen Van An",
            data.getPersonResponsible()
    );
}
@Test
void parsesLanguageWithAlternativeSemanticHeading() throws Exception {

    String text = """
            Course Name: Algorithms
            Course Code: IT200IU

            Medium of instruction: English

            Relation to curriculum
            Compulsory
            """;

    SyllabusImportData data =
            new SyllabusImportData();

    Method parseGeneralInformation =
            SyllabusPdfParser.class.getDeclaredMethod(
                    "parseGeneralInformation",
                    String.class,
                    SyllabusImportData.class
            );

    parseGeneralInformation.setAccessible(true);

    parseGeneralInformation.invoke(
            parser,
            text,
            data
    );

    assertEquals(
            "English",
            data.getLanguage()
    );
}
@Test
void parsesTeachingMethodsWithAlternativeSemanticHeading() throws Exception {

    String text = """
            Course Name: Algorithms
            Course Code: IT200IU

            Instructional methods: Lecture and laboratory practice

            Workload
            Total workload: 120
            """;

    SyllabusImportData data =
            new SyllabusImportData();

    Method parseGeneralInformation =
            SyllabusPdfParser.class.getDeclaredMethod(
                    "parseGeneralInformation",
                    String.class,
                    SyllabusImportData.class
            );

    parseGeneralInformation.setAccessible(true);

    parseGeneralInformation.invoke(
            parser,
            text,
            data
    );

    assertEquals(
            "Lecture and laboratory practice",
            data.getTeachingMethods()
    );
}
@Test
void parsesRelationWithAlternativeSemanticHeading() throws Exception {

    String text = """
            Course Name: Algorithms
            Course Code: IT200IU

            Curriculum relation: Compulsory

            Teaching methods
            Lecture and laboratory practice
            """;

    SyllabusImportData data =
            new SyllabusImportData();

    Method parseGeneralInformation =
            SyllabusPdfParser.class.getDeclaredMethod(
                    "parseGeneralInformation",
                    String.class,
                    SyllabusImportData.class
            );

    parseGeneralInformation.setAccessible(true);

    parseGeneralInformation.invoke(
            parser,
            text,
            data
    );

    assertEquals(
            "Compulsory",
            data.getRelation()
    );
}
@Test
void parsesCourseDesignationWithAlternativeSemanticHeading() throws Exception {

    String text = """
            Course Name: Algorithms
            Course Code: IT200IU

            Course classification: Core course

            Semester: 4
            """;

    SyllabusImportData data =
            new SyllabusImportData();

    Method parseGeneralInformation =
            SyllabusPdfParser.class.getDeclaredMethod(
                    "parseGeneralInformation",
                    String.class,
                    SyllabusImportData.class
            );

    parseGeneralInformation.setAccessible(true);

    parseGeneralInformation.invoke(
            parser,
            text,
            data
    );

    assertEquals(
            "Core course",
            data.getCourseDesignation()
    );
}
@Test
void parsesWorkloadWithAlternativeSemanticHeading() throws Exception {

    String text = """
            Course Name: Algorithms
            Course Code: IT200IU

            Study load: 120 hours
            Contact hours: 45 hours
            Private hours: 75 hours

            Credit points: 4
            """;

    SyllabusImportData data =
            new SyllabusImportData();

    Method parseWorkload =
            SyllabusPdfParser.class.getDeclaredMethod(
                    "parseWorkload",
                    String.class,
                    SyllabusImportData.class
            );

    parseWorkload.setAccessible(true);

    parseWorkload.invoke(
            parser,
            text,
            data
    );

    assertEquals(
            "120 hours",
            data.getWorkloadTotal()
    );

    assertEquals(
            "45 hours",
            data.getWorkloadContact()
    );

    assertEquals(
            "75 hours",
            data.getWorkloadPrivate()
    );
}
@Test
void parsesContentNoteWithAlternativeSemanticHeading() throws Exception {

    String text = """
            Course Name: Algorithms
            Course Code: IT200IU

            Content note: Covers fundamental algorithms and data structures.

            Topic Weight Level
            """;

    SyllabusImportData data =
            new SyllabusImportData();

    Method parseGeneralInformation =
            SyllabusPdfParser.class.getDeclaredMethod(
                    "parseGeneralInformation",
                    String.class,
                    SyllabusImportData.class
            );

    parseGeneralInformation.setAccessible(true);

    parseGeneralInformation.invoke(
            parser,
            text,
            data
    );

    assertEquals(
            "Covers fundamental algorithms and data structures.",
            data.getContentNote()
    );
}
@Test
void parsesAssessmentPassNoteWithAlternativeSemanticHeading() throws Exception {

    String text = """
            Course Name: Algorithms
            Course Code: IT200IU

            Passing requirement: Students must achieve at least 50% overall.

            Rubrics
            """;

    SyllabusImportData data =
            new SyllabusImportData();

    Method parseGeneralInformation =
            SyllabusPdfParser.class.getDeclaredMethod(
                    "parseGeneralInformation",
                    String.class,
                    SyllabusImportData.class
            );

    parseGeneralInformation.setAccessible(true);

    parseGeneralInformation.invoke(
            parser,
            text,
            data
    );

    assertEquals(
            "Students must achieve at least 50% overall.",
            data.getAssessmentPassNote()
    );
}
@Test
void preservesUnknownRubricFromAlternativeTemplate() throws Exception {

    String text = """
            Rubrics

            Project evaluation rubric

            Date revised
            2026-09-01
            """;

    SyllabusImportData data =
            new SyllabusImportData();

    Method parseRubrics =
            SyllabusPdfParser.class.getDeclaredMethod(
                    "parseRubrics",
                    String.class,
                    SyllabusImportData.class
            );

    parseRubrics.setAccessible(true);

    parseRubrics.invoke(
            parser,
            text,
            data
    );

    assertEquals(
            1,
            data.getRubricItems().size()
    );

    assertEquals(
            "Project evaluation rubric",
            data.getRubricItems().getFirst().getTitle()
    );
}
}