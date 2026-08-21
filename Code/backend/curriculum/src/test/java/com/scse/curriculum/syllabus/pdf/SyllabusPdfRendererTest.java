package com.scse.curriculum.syllabus.pdf;

import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.Test;

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
                "VIETNAM NATIONAL UNIVERSITY HCMC - INTERNATIONAL UNIVERSITY",
                "SCHOOL OF COMPUTER SCIENCE AND ENGINEERING");

        SyllabusPdfDocument data = sampleDocument();
        byte[] bytes = renderer.render(data, SyllabusPdfMode.PREVIEW);

        assertTrue(bytes.length > 2_000);
        assertEquals("%PDF", new String(bytes, 0, 4));

        PdfReader reader = new PdfReader(bytes);
        assertTrue(reader.getNumberOfPages() >= 2);
        reader.close();
    }


    @Test
    void rendersWideCloPloMatrixOnLandscapePage() throws Exception {
        SyllabusPdfFontProvider fonts = new SyllabusPdfFontProvider("", "");
        SyllabusPdfRenderer renderer = new SyllabusPdfRenderer(
                fonts,
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
                base.academicYear(), base.semester(), base.versionNumber(), base.versionLabel(),
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
