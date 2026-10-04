package com.scse.curriculum.syllabus.pdf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyllabusPdfRendererTest {

    @Test
    void rendersAReadableMultiSectionPdf() throws Exception {
        SyllabusPdfFontProvider fonts = new SyllabusPdfFontProvider("", "");
        SyllabusPdfRenderer renderer = new SyllabusPdfRenderer(
                fonts,
                new ObjectMapper(),
                "VIETNAM NATIONAL UNIVERSITY HCMC - INTERNATIONAL UNIVERSITY",
                "SCHOOL OF COMPUTER SCIENCE AND ENGINEERING");

        SyllabusPdfDocument data = sampleDocument();
        byte[] bytes = renderer.render(data, SyllabusPdfMode.PREVIEW);

        assertTrue(bytes.length > 2_000);
        assertEquals("%PDF", new String(bytes, 0, 4));

        PdfReader reader = new PdfReader(bytes);
        assertTrue(reader.getNumberOfPages() >= 2);
        String allText = java.util.stream.IntStream.rangeClosed(1, reader.getNumberOfPages())
                .mapToObj(page -> {
                    try {
                        return new PdfTextExtractor(reader).getTextFromPage(page);
                    } catch (Exception exception) {
                        throw new IllegalStateException(exception);
                    }
                })
                .reduce("", String::concat);
        assertTrue(allText.contains("1. General information"));
        assertTrue(allText.contains("Course learning outcomes"));
        assertTrue(allText.contains("3. Planned learning activities and teaching methods"));
        assertTrue(allText.contains("Assessment Type"));
        assertTrue(allText.contains("Rubrics (optional)"));
        reader.close();

        Path qaDirectory = Path.of("target", "pdf-qa");
        Files.createDirectories(qaDirectory);
        Files.write(qaDirectory.resolve("syllabus-reference-layout.pdf"), bytes);
        try (var document = Loader.loadPDF(bytes)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            int previewPages = Math.min(3, document.getNumberOfPages());
            for (int page = 0; page < previewPages; page++) {
                ImageIO.write(
                        pdfRenderer.renderImageWithDPI(page, 120),
                        "png",
                        qaDirectory.resolve("page-" + (page + 1) + ".png").toFile());
            }
        }
    }


    @Test
    void rendersNew2027WithExactTargetSectionContract() throws Exception {
        SyllabusPdfFontProvider fonts = new SyllabusPdfFontProvider("", "");
        SyllabusPdfRenderer renderer = new SyllabusPdfRenderer(
                fonts,
                new ObjectMapper(),
                "VIETNAM NATIONAL UNIVERSITY HCMC - INTERNATIONAL UNIVERSITY",
                "SCHOOL OF COMPUTER SCIENCE AND ENGINEERING");

        SyllabusPdfDocument base = sampleDocument();
        SyllabusPdfDocument data = new SyllabusPdfDocument(
                base.syllabusId(), base.status(), base.courseCode(), base.courseName(),
                base.courseNameVn(), base.departmentCode(), base.departmentName(),
                "CS2027", "NEW_2027", base.semester(), base.versionNumber(), base.versionLabel(),
                base.currentVersion(), base.courseDesignation(), base.courseTypes(), base.language(),
                base.relation(), base.teachingMethods(), base.workloadTotal(), base.workloadContact(),
                base.workloadPrivate(), base.prerequisites(), base.objectives(), base.examForms(),
                base.examRequirements(), base.rubrics(), base.major(), base.creditTheory(),
                base.creditLab(), base.responsiblePersons(), base.createdBy(), base.approvedBy(),
                base.submittedAt(), base.approvedAt(), base.updatedAt(), base.changeSummary(),
                "{\"schemaVersion\":1,\"assessmentPassNote\":\"Pass requirement from source\","
                        + "\"plannedActivities\":[{\"week\":1,\"topic\":\"Course overview\","
                        + "\"clo\":\"CLO1\",\"assessments\":\"\","
                        + "\"learningActivities\":\"Lecture, Discussion\","
                        + "\"resources\":\"[1]. Chapter 1\"}]}",
                base.clos(), base.plos(), base.cloPloCells(), base.topics(),
                base.assessments(), base.books());

        byte[] bytes = renderer.render(data, SyllabusPdfMode.EXPORT);
        try (var document = Loader.loadPDF(bytes)) {
            String text = new org.apache.pdfbox.text.PDFTextStripper().getText(document)
                    .replace('\u2013', '-')
                    .replace('\u2014', '-')
                    .replaceAll("\\s+", " ");

            for (String heading : List.of(
                    "1. General Information",
                    "2. Course Objectives",
                    "3. Course Learning Outcomes (CLO)",
                    "4. Course Content",
                    "5. Course CLO-PLO Alignment",
                    "6. Detailed CLO-LLO Table",
                    "7. Examination Forms",
                    "8. Study and Examination Requirements",
                    "9. Planned Learning Activities and Teaching Methods",
                    "10. Assessment Plan",
                    "11. Assignment Description and Rubric Summary",
                    "12. Reading List")) {
                assertTrue(text.contains(heading), "Missing NEW_2027 section: " + heading);
            }

            assertTrue(text.contains("CS2027"));
            assertTrue(text.contains("No source LLO data is available. LLO values are not generated."));
            assertTrue(text.contains("Lecture, Discussion"));
            assertTrue(text.contains("[1]. Chapter 1"));
            assertTrue(!text.contains("2. Learning Outcomes Matrix"));
        }
    }
    @Test
    void rendersWideCloPloMatrixOnLandscapePage() throws Exception {
        SyllabusPdfFontProvider fonts = new SyllabusPdfFontProvider("", "");
        SyllabusPdfRenderer renderer = new SyllabusPdfRenderer(
                fonts,
                new ObjectMapper(),
                "VIETNAM NATIONAL UNIVERSITY HCMC - INTERNATIONAL UNIVERSITY",
                "SCHOOL OF COMPUTER SCIENCE AND ENGINEERING");

        SyllabusPdfDocument base = sampleDocument();
        var plos = java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(index -> new SyllabusPdfDocument.PloColumn(
                        index,
                        "PLO" + index,
                        "Program learning outcome " + index))
                .toList();
        var mappings = java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(index -> new SyllabusPdfDocument.CloPloCell(
                        index % 2 == 0 ? 1 : 2,
                        index,
                        index % 3 == 0 ? "A" : index % 2 == 0 ? "D" : "I",
                        1f,
                        null))
                .toList();

        SyllabusPdfDocument wide = new SyllabusPdfDocument(
                base.syllabusId(), base.status(), base.courseCode(), base.courseName(),
                base.courseNameVn(), base.departmentCode(), base.departmentName(),
                base.academicYear(), base.targetTemplateProfile(), base.semester(), base.versionNumber(), base.versionLabel(),
                base.currentVersion(), base.courseDesignation(), base.courseTypes(), base.language(),
                base.relation(), base.teachingMethods(), base.workloadTotal(), base.workloadContact(),
                base.workloadPrivate(), base.prerequisites(), base.objectives(), base.examForms(),
                base.examRequirements(), base.rubrics(), base.major(), base.creditTheory(),
                base.creditLab(), base.responsiblePersons(), base.createdBy(), base.approvedBy(),
                base.submittedAt(), base.approvedAt(), base.updatedAt(), base.changeSummary(),
                base.notes(), base.clos(), plos, mappings, base.topics(), base.assessments(), base.books());

        byte[] bytes = renderer.render(wide, SyllabusPdfMode.EXPORT);
        PdfReader reader = new PdfReader(bytes);
        boolean hasLandscapePage = java.util.stream.IntStream
                .rangeClosed(1, reader.getNumberOfPages())
                .anyMatch(page -> reader.getPageRotation(page) == 90
                        || reader.getPageSizeWithRotation(page).getWidth()
                        > reader.getPageSizeWithRotation(page).getHeight());
        reader.close();

        assertTrue(hasLandscapePage);
    }

    private SyllabusPdfDocument sampleDocument() {
        var clos = List.of(
                new SyllabusPdfDocument.CloRow(
                        1, "CLO1", "KNOWLEDGE", "UNDERSTAND",
                        "Explain the core principles of software engineering.",
                        "Giải thích các nguyên lý cốt lõi của kỹ nghệ phần mềm.", 1),
                new SyllabusPdfDocument.CloRow(
                        2, "CLO2", "SKILL", "APPLY",
                        "Apply appropriate methods to solve a practical problem.",
                        "Áp dụng phương pháp phù hợp để giải quyết bài toán thực tế.", 2));

        var plos = List.of(
                new SyllabusPdfDocument.PloColumn(1, "PLO1", "Technical knowledge"),
                new SyllabusPdfDocument.PloColumn(2, "PLO2", "Problem solving"));

        return new SyllabusPdfDocument(
                1,
                "DRAFT",
                "IT001IU",
                "Introduction to Information Technology",
                "Nhập môn Công nghệ Thông tin",
                "IT",
                "Department of Information Technology",
                "2026-2027",
                "SOURCE_TEMPLATE",
                "1",
                1,
                "v1.0",
                false,
                "This course introduces foundational knowledge and professional practices.",
                "Compulsory",
                "English",
                "Compulsory",
                "Lectures, laboratory sessions, assignments and project work",
                "120",
                "60",
                "60",
                "None",
                "Provide students with foundational knowledge and practical skills.",
                "Written examination and project presentation",
                "Students must attend at least 80% of the scheduled sessions.",
                "Assessment rubrics are published before each assignment.",
                "Information Technology",
                3,
                1,
                "Nguyễn Văn An",
                "instructor1",
                null,
                null,
                null,
                LocalDateTime.of(2026, 7, 25, 9, 0),
                "Initial version",
                "Internal note",
                clos,
                plos,
                List.of(
                        new SyllabusPdfDocument.CloPloCell(1, 1, "I", 1f, null),
                        new SyllabusPdfDocument.CloPloCell(2, 2, "D", 1f, null)),
                List.of(
                        new SyllabusPdfDocument.TopicRow(
                                1, 1, 1, "Course overview", "Tổng quan môn học",
                                3, 1, 4, "LECTURE", "Lecture and discussion",
                                "Case study", null, List.of("CLO1")),
                        new SyllabusPdfDocument.TopicRow(
                                2, 2, 1, "Problem solving", "Giải quyết vấn đề",
                                3, 2, 5, "LAB", "Guided laboratory",
                                "Team exercise", null, List.of("CLO2"))),
                List.of(
                        new SyllabusPdfDocument.AssessmentRow(
                                1, "Assignments", "Bài tập", "ASSIGNMENT",
                                30f, 0f, 10f, 1,
                                List.of(new SyllabusPdfDocument.AssessmentCloRow("CLO2", 100f))),
                        new SyllabusPdfDocument.AssessmentRow(
                                2, "Final examination", "Thi cuối kỳ", "FINAL_EXAM",
                                70f, 0f, 10f, 2,
                                List.of(
                                        new SyllabusPdfDocument.AssessmentCloRow("CLO1", 50f),
                                        new SyllabusPdfDocument.AssessmentCloRow("CLO2", 50f)))),
                List.of(
                        new SyllabusPdfDocument.BookRow(
                                1, "REQUIRED", 1,
                                "Software Engineering", "Ian Sommerville", "Pearson",
                                2016, "10th edition", "978-0133943030", null, "TEXTBOOK")));
    }
}
