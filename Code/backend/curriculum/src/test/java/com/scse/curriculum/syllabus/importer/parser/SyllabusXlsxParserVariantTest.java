package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportIssue;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyllabusXlsxParserVariantTest {

    private final SyllabusXlsxParser parser =
            new SyllabusXlsxParser();

    @Test
    void parsesAlternativeGeneralInformationSheetName()
            throws Exception {

        byte[] bytes;

        try (XSSFWorkbook workbook =
                     new XSSFWorkbook();
             ByteArrayOutputStream output =
                     new ByteArrayOutputStream()) {

            var sheet =
                    workbook.createSheet(
                            "General Information"
                    );

            var header =
                    sheet.createRow(0);

            header.createCell(0)
                    .setCellValue("Field");

            header.createCell(1)
                    .setCellValue("Value");

            var code =
                    sheet.createRow(1);

            code.createCell(0)
                    .setCellValue("Course Code");

            code.createCell(1)
                    .setCellValue("IT200IU");

            var name =
                    sheet.createRow(2);

            name.createCell(0)
                    .setCellValue("Course Name");

            name.createCell(1)
                    .setCellValue("Algorithms");

            workbook.write(output);
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

        assertEquals(
                "Algorithms",
                data.getSourceCourseName()
        );
    }
    @Test
void parsesAlternativeGeneralInformationFieldLabels()
        throws Exception {

    byte[] bytes;

    try (XSSFWorkbook workbook =
                 new XSSFWorkbook();
         ByteArrayOutputStream output =
                 new ByteArrayOutputStream()) {

        var sheet =
                workbook.createSheet(
                        "General Info"
                );

        var header =
                sheet.createRow(0);

        header.createCell(0)
                .setCellValue("Field");

        header.createCell(1)
                .setCellValue("Value");

        var code =
                sheet.createRow(1);

        code.createCell(0)
                .setCellValue("Module Code");

        code.createCell(1)
                .setCellValue("IT200IU");

        var name =
                sheet.createRow(2);

        name.createCell(0)
                .setCellValue("Module Name");

        name.createCell(1)
                .setCellValue("Algorithms");

        workbook.write(output);
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

    assertEquals(
            "Algorithms",
            data.getSourceCourseName()
    );
}
@Test
void parsesAlternativeSemanticGeneralInformationFields()
        throws Exception {

    byte[] bytes;

    try (XSSFWorkbook workbook =
                 new XSSFWorkbook();
         ByteArrayOutputStream output =
                 new ByteArrayOutputStream()) {

        var sheet =
                workbook.createSheet(
                        "General Information"
                );

        var header =
                sheet.createRow(0);

        header.createCell(0)
                .setCellValue("Field");

        header.createCell(1)
                .setCellValue("Value");

        String[][] rows = {
                {"Module Code", "IT200IU"},
                {"Module Name", "Algorithms"},
                {"Course Classification", "Core course"},
                {"Course Type", "Required"},
                {"Medium of Instruction", "English"},
                {"Curriculum Relation", "Computer Science core"},
                {"Instructional Methods", "Lecture and laboratory"},
                {"Required Prerequisites", "IT069IU"},
                {"Course Aims", "Develop algorithmic problem-solving skills"}
        };

        for (int index = 0;
             index < rows.length;
             index++) {

            var row =
                    sheet.createRow(
                            index + 1
                    );

            row.createCell(0)
                    .setCellValue(
                            rows[index][0]
                    );

            row.createCell(1)
                    .setCellValue(
                            rows[index][1]
                    );
        }

        workbook.write(output);
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
            "Required",
            data.getCourseTypes()
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
            "IT069IU",
            data.getPrerequisites()
    );

    assertEquals(
            "Develop algorithmic problem-solving skills",
            data.getObjectives()
    );
}
@Test
void parsesAlternativeStructuralSheetNames()
        throws Exception {

    byte[] bytes;

    try (XSSFWorkbook workbook =
                 new XSSFWorkbook();
         ByteArrayOutputStream output =
                 new ByteArrayOutputStream()) {

        // Mandatory General Info
        var general =
                workbook.createSheet(
                        "General Info"
                );

        var generalHeader =
                general.createRow(0);

        generalHeader.createCell(0)
                .setCellValue("Field");

        generalHeader.createCell(1)
                .setCellValue("Value");

        var code =
                general.createRow(1);

        code.createCell(0)
                .setCellValue("Course Code");

        code.createCell(1)
                .setCellValue("IT200IU");

        var name =
                general.createRow(2);

        name.createCell(0)
                .setCellValue("Course Name");

        name.createCell(1)
                .setCellValue("Algorithms");


        // CLO alias
        var clo =
                workbook.createSheet(
                        "Learning Outcomes"
                );

        var cloHeader =
                clo.createRow(0);

        cloHeader.createCell(0)
                .setCellValue("Code");

        cloHeader.createCell(1)
                .setCellValue("Description");

        var cloRow =
                clo.createRow(1);

        cloRow.createCell(0)
                .setCellValue("CLO1");

        cloRow.createCell(1)
                .setCellValue(
                        "Explain algorithmic concepts"
                );


        // Topics alias
        var topics =
                workbook.createSheet(
                        "Course Content"
                );

        var topicHeader =
                topics.createRow(0);

        topicHeader.createCell(0)
                .setCellValue("Week Number");

        topicHeader.createCell(1)
                .setCellValue("Order In Week");

        topicHeader.createCell(2)
                .setCellValue("Name");

        var topicRow =
                topics.createRow(1);

        topicRow.createCell(0)
                .setCellValue(1);

        topicRow.createCell(1)
                .setCellValue(1);

        topicRow.createCell(2)
                .setCellValue(
                        "Sorting algorithms"
                );


        // Assessment alias
        var assessments =
                workbook.createSheet(
                        "Assessment Plan"
                );

        var assessmentHeader =
                assessments.createRow(0);

        assessmentHeader.createCell(0)
                .setCellValue("Name");

        assessmentHeader.createCell(1)
                .setCellValue("Assessment Type");

        assessmentHeader.createCell(2)
                .setCellValue("Weight Percent");

        var assessmentRow =
                assessments.createRow(1);

        assessmentRow.createCell(0)
                .setCellValue(
                        "Final examination"
                );

        assessmentRow.createCell(1)
                .setCellValue(
                        "FINAL_EXAM"
                );

        assessmentRow.createCell(2)
                .setCellValue(60);


        // Reading alias
        var readings =
                workbook.createSheet(
                        "References"
                );

        var readingHeader =
                readings.createRow(0);

        readingHeader.createCell(0)
                .setCellValue("Title");

        readingHeader.createCell(1)
                .setCellValue("Author");

        readingHeader.createCell(2)
                .setCellValue("Publisher");

        readingHeader.createCell(3)
                .setCellValue("Year");

        readingHeader.createCell(4)
                .setCellValue("Book Type");

        var readingRow =
                readings.createRow(1);

        readingRow.createCell(0)
                .setCellValue(
                        "Introduction to Algorithms"
                );

        readingRow.createCell(1)
                .setCellValue(
                        "Thomas H. Cormen"
                );

        readingRow.createCell(2)
                .setCellValue(
                        "MIT Press"
                );

        readingRow.createCell(3)
                .setCellValue(2022);

        readingRow.createCell(4)
                .setCellValue(
                        "Textbook"
                );


        workbook.write(output);
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
            data.getTopics().size()
    );

    assertEquals(
            "Sorting algorithms",
            data.getTopics().getFirst().getName()
    );

    assertEquals(
            1,
            data.getAssessments().size()
    );

    assertEquals(
            "FINAL_EXAM",
            data.getAssessments()
                    .getFirst()
                    .getAssessmentType()
    );

    assertEquals(
            1,
            data.getReadings().size()
    );

    assertEquals(
            "Introduction to Algorithms",
            data.getReadings()
                    .getFirst()
                    .getTitle()
    );
}
@Test
void parsesAlternativeColumnHeaders()
        throws Exception {

    byte[] bytes;

    try (XSSFWorkbook workbook =
                 new XSSFWorkbook();
         ByteArrayOutputStream output =
                 new ByteArrayOutputStream()) {

        var general =
                workbook.createSheet(
                        "General Info"
                );

        var generalHeader =
                general.createRow(0);

        generalHeader.createCell(0)
                .setCellValue("Field");

        generalHeader.createCell(1)
                .setCellValue("Value");

        var code =
                general.createRow(1);

        code.createCell(0)
                .setCellValue("Course Code");

        code.createCell(1)
                .setCellValue("IT200IU");

        var name =
                general.createRow(2);

        name.createCell(0)
                .setCellValue("Course Name");

        name.createCell(1)
                .setCellValue("Algorithms");


        var clo =
                workbook.createSheet("CLO");

        var cloHeader =
                clo.createRow(0);

        cloHeader.createCell(0)
                .setCellValue("CLO Code");

        cloHeader.createCell(1)
                .setCellValue("Learning Outcome");

        var cloRow =
                clo.createRow(1);

        cloRow.createCell(0)
                .setCellValue("CLO1");

        cloRow.createCell(1)
                .setCellValue(
                        "Explain algorithmic concepts"
                );


        var topics =
                workbook.createSheet("Topics");

        var topicHeader =
                topics.createRow(0);

        topicHeader.createCell(0)
                .setCellValue("Week");

        topicHeader.createCell(1)
                .setCellValue("Topic Order");

        topicHeader.createCell(2)
                .setCellValue("Topic Name");

        var topicRow =
                topics.createRow(1);

        topicRow.createCell(0)
                .setCellValue(1);

        topicRow.createCell(1)
                .setCellValue(1);

        topicRow.createCell(2)
                .setCellValue(
                        "Sorting algorithms"
                );


        var assessments =
                workbook.createSheet("Assessments");

        var assessmentHeader =
                assessments.createRow(0);

        assessmentHeader.createCell(0)
                .setCellValue("Assessment Name");

        assessmentHeader.createCell(1)
                .setCellValue("Assessment Category");

        assessmentHeader.createCell(2)
                .setCellValue("Weight");

        var assessmentRow =
                assessments.createRow(1);

        assessmentRow.createCell(0)
                .setCellValue(
                        "Final examination"
                );

        assessmentRow.createCell(1)
                .setCellValue(
                        "FINAL_EXAM"
                );

        assessmentRow.createCell(2)
                .setCellValue(60);


        var readings =
                workbook.createSheet(
                        "Reading List"
                );

        var readingHeader =
                readings.createRow(0);

        readingHeader.createCell(0)
                .setCellValue("Reference Title");

        readingHeader.createCell(1)
                .setCellValue("Author");

        readingHeader.createCell(2)
                .setCellValue("Publisher");

        readingHeader.createCell(3)
                .setCellValue("Publication Year");

        readingHeader.createCell(4)
                .setCellValue("Resource Type");

        var readingRow =
                readings.createRow(1);

        readingRow.createCell(0)
                .setCellValue(
                        "Introduction to Algorithms"
                );

        readingRow.createCell(1)
                .setCellValue(
                        "Thomas H. Cormen"
                );

        readingRow.createCell(2)
                .setCellValue(
                        "MIT Press"
                );

        readingRow.createCell(3)
                .setCellValue(2022);

        readingRow.createCell(4)
                .setCellValue("Textbook");


        workbook.write(output);
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
            "CLO1",
            data.getClos()
                    .getFirst()
                    .getCode()
    );

    assertEquals(
            "Explain algorithmic concepts",
            data.getClos()
                    .getFirst()
                    .getDescription()
    );

    assertEquals(
            "Sorting algorithms",
            data.getTopics()
                    .getFirst()
                    .getName()
    );

    assertEquals(
            "FINAL_EXAM",
            data.getAssessments()
                    .getFirst()
                    .getAssessmentType()
    );

    assertEquals(
            60.0f,
            data.getAssessments()
                    .getFirst()
                    .getWeightPercent()
    );

    assertEquals(
            "Introduction to Algorithms",
            data.getReadings()
                    .getFirst()
                    .getTitle()
    );

    assertEquals(
            2022,
            data.getReadings()
                    .getFirst()
                    .getYear()
    );
}
@Test
void preservesTemplateSectionsForAlternativeSheetsAndHeaders()
        throws Exception {

    byte[] bytes;

    try (XSSFWorkbook workbook =
                 new XSSFWorkbook();
         ByteArrayOutputStream output =
                 new ByteArrayOutputStream()) {

        var general =
                workbook.createSheet(
                        "General Information"
                );

        var generalHeader =
                general.createRow(0);

        generalHeader.createCell(0)
                .setCellValue("Field");

        generalHeader.createCell(1)
                .setCellValue("Value");

        var code =
                general.createRow(1);

        code.createCell(0)
                .setCellValue("Course Code");

        code.createCell(1)
                .setCellValue("IT200IU");

        var name =
                general.createRow(2);

        name.createCell(0)
                .setCellValue("Course Name");

        name.createCell(1)
                .setCellValue("Algorithms");


        var clo =
                workbook.createSheet(
                        "Learning Outcomes"
                );

        var cloHeader =
                clo.createRow(0);

        cloHeader.createCell(0)
                .setCellValue("CLO Code");

        cloHeader.createCell(1)
                .setCellValue("Learning Outcome");

        var cloRow =
                clo.createRow(1);

        cloRow.createCell(0)
                .setCellValue("CLO1");

        cloRow.createCell(1)
                .setCellValue(
                        "Explain algorithmic concepts"
                );


        var topics =
                workbook.createSheet(
                        "Course Content"
                );

        var topicHeader =
                topics.createRow(0);

        topicHeader.createCell(0)
                .setCellValue("Week");

        topicHeader.createCell(1)
                .setCellValue("Topic Order");

        topicHeader.createCell(2)
                .setCellValue("Topic Name");

        var topicRow =
                topics.createRow(1);

        topicRow.createCell(0)
                .setCellValue(1);

        topicRow.createCell(1)
                .setCellValue(1);

        topicRow.createCell(2)
                .setCellValue(
                        "Sorting algorithms"
                );


        var assessments =
                workbook.createSheet(
                        "Assessment Plan"
                );

        var assessmentHeader =
                assessments.createRow(0);

        assessmentHeader.createCell(0)
                .setCellValue("Assessment Name");

        assessmentHeader.createCell(1)
                .setCellValue("Assessment Category");

        assessmentHeader.createCell(2)
                .setCellValue("Weight");

        var assessmentRow =
                assessments.createRow(1);

        assessmentRow.createCell(0)
                .setCellValue(
                        "Final examination"
                );

        assessmentRow.createCell(1)
                .setCellValue(
                        "FINAL_EXAM"
                );

        assessmentRow.createCell(2)
                .setCellValue(60);


        var readings =
                workbook.createSheet(
                        "References"
                );

        var readingHeader =
                readings.createRow(0);

        readingHeader.createCell(0)
                .setCellValue("Reference Title");

        readingHeader.createCell(1)
                .setCellValue("Author");

        readingHeader.createCell(2)
                .setCellValue("Publisher");

        readingHeader.createCell(3)
                .setCellValue("Publication Year");

        readingHeader.createCell(4)
                .setCellValue("Resource Type");

        var readingRow =
                readings.createRow(1);

        readingRow.createCell(0)
                .setCellValue(
                        "Introduction to Algorithms"
                );

        readingRow.createCell(1)
                .setCellValue(
                        "Thomas H. Cormen"
                );

        readingRow.createCell(2)
                .setCellValue(
                        "MIT Press"
                );

        readingRow.createCell(3)
                .setCellValue(2022);

        readingRow.createCell(4)
                .setCellValue(
                        "Textbook"
                );


        workbook.write(output);
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
            List.of(
                    "general",
                    "clo",
                    "content",
                    "assessment",
                    "readings"
            ),
            data.getTemplateSections()
                    .stream()
                    .map(
                            SyllabusImportData.TemplateSection::getKey
                    )
                    .toList()
    );
}
}