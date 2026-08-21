package com.scse.curriculum.syllabus.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class SyllabusPdfRenderer {

    /*
     * Softer SCSE palette.
     * The previous renderer used almost-black navy/black text everywhere,
     * which made the PDF visually heavy. The new palette keeps hierarchy
     * while reducing contrast for labels/body text.
     */
    private static final Color NAVY = new Color(39, 72, 88);
    private static final Color BLUE = new Color(0, 125, 132);
    private static final Color TEXT = new Color(51, 65, 85);
    private static final Color LIGHT_BLUE = new Color(238, 248, 248);
    private static final Color LIGHT_SLATE = new Color(248, 250, 252);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color MUTED = new Color(100, 116, 139);

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ENGLISH);

    private final SyllabusPdfFontProvider fonts;
    private final String institutionName;
    private final String schoolName;

    public SyllabusPdfRenderer(
            SyllabusPdfFontProvider fonts,
            @Value("${app.pdf.institution-name:VIETNAM NATIONAL UNIVERSITY HCMC - INTERNATIONAL UNIVERSITY}")
            String institutionName,
            @Value("${app.pdf.school-name:SCHOOL OF COMPUTER SCIENCE AND ENGINEERING}")
            String schoolName) {
        this.fonts = fonts;
        this.institutionName = institutionName;
        this.schoolName = schoolName;
    }

    public byte[] render(SyllabusPdfDocument data, SyllabusPdfMode mode) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            /*
             * More compact margins while still leaving enough room for the
             * page-event header/footer.
             */
            Document document = new Document(
                    PageSize.A4,
                    38,
                    38,
                    50,
                    44);

            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPdfVersion(PdfWriter.VERSION_1_7);

            writer.setPageEvent(new SyllabusPdfPageEvent(
                    fonts,
                    data.courseCode(),
                    data.versionLabel(),
                    watermark(data, mode)));

            document.addTitle("Course Syllabus - " + safe(data.courseCode()));
            document.addSubject("SCSE course syllabus PDF");
            document.addAuthor(safe(data.createdBy()));
            document.addCreator("SCSE Curriculum Management System");
            document.addKeywords("syllabus, course, SCSE, curriculum");
            document.open();

            addInstitutionHeader(document, data);
            addIdentityCard(document, data, mode);
            addGeneralInformation(document, data);
            addCourseLearningOutcomes(document, data);

            /*
             * Content overview is included only when real content exists.
             * It no longer renders a generic fallback paragraph that wastes space.
             */
            addContentOverview(document, data);

            addLearningOutcomesMatrix(document, writer, data);
            addPlannedLearningActivities(document, data);
            addAssessmentPlan(document, data);
            addReadingList(document, data);
            addRevisionAndApproval(document, data);

            document.close();
            return output.toByteArray();

        } catch (DocumentException exception) {
            throw new IllegalStateException(
                    "Unable to generate the syllabus PDF.",
                    exception);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to generate the syllabus PDF: "
                            + exception.getMessage(),
                    exception);
        }
    }

    private void addInstitutionHeader(
            Document document,
            SyllabusPdfDocument data)
            throws DocumentException {

        Paragraph institution = new Paragraph(
                institutionName,
                fonts.bold(8.8f, NAVY));
        institution.setAlignment(Element.ALIGN_CENTER);
        institution.setSpacingAfter(1.5f);
        document.add(institution);

        String department =
                firstNonBlank(
                        data.departmentName(),
                        schoolName);

        Paragraph school = new Paragraph(
                department.toUpperCase(Locale.ROOT),
                fonts.regular(8.6f, NAVY));
        school.setAlignment(Element.ALIGN_CENTER);
        school.setSpacingAfter(8);
        document.add(school);

        Paragraph title = new Paragraph(
                "COURSE SYLLABUS",
                fonts.bold(18, NAVY));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4);
        document.add(title);

        Paragraph courseName = new Paragraph(
                safe(data.courseName()),
                fonts.bold(11.5f, TEXT));
        courseName.setAlignment(Element.ALIGN_CENTER);
        courseName.setSpacingAfter(2);
        document.add(courseName);

        Paragraph courseCode = new Paragraph(
                "Course Code: "
                        + safe(data.courseCode()),
                fonts.regular(9, MUTED));
        courseCode.setAlignment(Element.ALIGN_CENTER);
        courseCode.setSpacingAfter(10);
        document.add(courseCode);
    }

    private void addIdentityCard(
            Document document,
            SyllabusPdfDocument data,
            SyllabusPdfMode mode)
            throws DocumentException {

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{
                18, 32, 18, 32
        });
        table.setSpacingAfter(10);
        table.setKeepTogether(true);

        addLabelCell(
                table,
                "Academic year");
        addValueCell(
                table,
                data.academicYear());

        addLabelCell(
                table,
                "Semester");
        addValueCell(
                table,
                data.semester());

        addLabelCell(
                table,
                "Version");
        addValueCell(
                table,
                firstNonBlank(
                        data.versionLabel(),
                        "v"
                                + safeNumber(
                                data.versionNumber())));

        addLabelCell(
                table,
                "Status");
        addValueCell(
                table,
                statusLabel(
                        data.status())
                        + (
                        mode
                                == SyllabusPdfMode.PREVIEW
                                ? " · Preview"
                                : ""
                ));

        addLabelCell(
                table,
                "Major");
        addValueCell(
                table,
                data.major());

        addLabelCell(
                table,
                "Department");
        addValueCell(
                table,
                firstNonBlank(
                        data.departmentCode(),
                        data.departmentName()));

        document.add(table);
    }

    private void addGeneralInformation(
            Document document,
            SyllabusPdfDocument data)
            throws DocumentException {

        addSectionTitle(
                document,
                "1. General information");

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{
                29, 71
        });
        table.setSpacingAfter(8);
        table.setSplitLate(false);
        table.setSplitRows(true);

        /*
         * Keep the core SCSE identity rows.
         */
        addGeneralRow(
                table,
                "Course designation",
                data.courseDesignation());

        addGeneralRow(
                table,
                "Semester(s) in which the course is taught",
                data.semester());

        addGeneralRow(
                table,
                "Person responsible for the course",
                data.responsiblePersons());

        addGeneralRowIfPresent(
                table,
                "Language",
                data.language());

        addGeneralRowIfPresent(
                table,
                "Relation to curriculum",
                firstNonBlankOrEmpty(
                        data.relation(),
                        data.courseTypes()));

        addGeneralRowIfPresent(
                table,
                "Teaching methods",
                data.teachingMethods());

        String workload =
                joinNonBlank(
                        "\n",
                        labeled(
                                "Estimated total workload",
                                data.workloadTotal()),
                        labeled(
                                "Contact hours",
                                data.workloadContact()),
                        labeled(
                                "Private/self-study hours",
                                data.workloadPrivate()));

        addGeneralRowIfPresent(
                table,
                "Workload",
                workload);

        addGeneralRow(
                table,
                "Credit points",
                creditText(
                        data.creditTheory(),
                        data.creditLab()));

        /*
         * Optional rows are omitted when no real value exists.
         * This removes large empty blocks from incomplete/legacy syllabi.
         */
        addGeneralRowIfPresent(
                table,
                "Required and recommended prerequisites",
                data.prerequisites());

        addGeneralRowIfPresent(
                table,
                "Course objectives",
                data.objectives());

        addGeneralRowIfPresent(
                table,
                "Examination forms",
                data.examForms());

        addGeneralRowIfPresent(
                table,
                "Study and examination requirements",
                data.examRequirements());

        addGeneralRowIfPresent(
                table,
                "Rubrics / grading guidance",
                data.rubrics());

        document.add(table);
    }

    private void addCourseLearningOutcomes(
            Document document,
            SyllabusPdfDocument data)
            throws DocumentException {

        addSubsectionTitle(
                document,
                "Course learning outcomes");

        if (data.clos().isEmpty()) {
            addEmptyState(
                    document,
                    "No course learning outcomes have been defined.");
            return;
        }

        PdfPTable table =
                new PdfPTable(5);

        table.setWidthPercentage(100);
        table.setWidths(
                new float[]{
                        10,
                        16,
                        16,
                        29,
                        29
                });
        table.setHeaderRows(1);
        table.setSplitLate(false);
        table.setSpacingAfter(8);

        addHeaderCell(
                table,
                "CLO");
        addHeaderCell(
                table,
                "Competency");
        addHeaderCell(
                table,
                "Bloom level");
        addHeaderCell(
                table,
                "Description (EN)");
        addHeaderCell(
                table,
                "Description (VI)");

        for (
                SyllabusPdfDocument.CloRow clo
                : data.clos()
        ) {
            addBodyCell(
                    table,
                    clo.code(),
                    Element.ALIGN_CENTER);

            addBodyCell(
                    table,
                    titleCase(
                            clo.competencyLevel()),
                    Element.ALIGN_LEFT);

            addBodyCell(
                    table,
                    titleCase(
                            clo.bloomLevel()),
                    Element.ALIGN_LEFT);

            addBodyCell(
                    table,
                    clo.description(),
                    Element.ALIGN_LEFT);

            addBodyCell(
                    table,
                    clo.descriptionVn(),
                    Element.ALIGN_LEFT);
        }

        document.add(table);
    }

    private void addContentOverview(
            Document document,
            SyllabusPdfDocument data)
            throws DocumentException {

        /*
         * Avoid repeating a blank or legacy placeholder value.
         */
        if (
                isBlank(
                        data.courseDesignation())
        ) {
            return;
        }

        addSubsectionTitle(
                document,
                "Content overview");

        Paragraph text = new Paragraph(
                data.courseDesignation().trim(),
                fonts.regular(
                        8.4f,
                        TEXT));

        text.setLeading(11.5f);
        text.setSpacingAfter(7);

        document.add(text);
    }

    private void addLearningOutcomesMatrix(
            Document document,
            PdfWriter writer,
            SyllabusPdfDocument data)
            throws DocumentException {

        if (
                data.clos().isEmpty()
                        || data.plos().isEmpty()
        ) {
            addSectionTitle(
                    document,
                    "2. Learning Outcomes Matrix");

            addEmptyState(
                    document,
                    "No CLO-PLO mapping data is available for this syllabus.");
            return;
        }

        boolean landscape =
                data.plos().size() > 7;

        if (landscape) {
            forcePageSize(
                    document,
                    writer,
                    PageSize.A4.rotate());
        }

        addSectionTitle(
                document,
                "2. Learning Outcomes Matrix");

        int columnCount =
                1 + data.plos().size();

        PdfPTable table =
                new PdfPTable(
                        columnCount);

        table.setWidthPercentage(100);

        float[] widths =
                new float[columnCount];

        widths[0] = 18;

        for (
                int index = 1;
                index < columnCount;
                index++
        ) {
            widths[index] =
                    82f
                            / data.plos().size();
        }

        table.setWidths(widths);
        table.setHeaderRows(1);
        table.setSplitLate(false);
        table.setSpacingAfter(5);

        addHeaderCell(
                table,
                "CLO / PLO");

        for (
                SyllabusPdfDocument.PloColumn plo
                : data.plos()
        ) {
            addHeaderCell(
                    table,
                    plo.code());
        }

        Map<String, String> levelByCell =
                data.cloPloCells()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        item ->
                                                item.cloId()
                                                        + ":"
                                                        + item.ploId(),
                                        item ->
                                                safe(
                                                        item.level()),
                                        (
                                                left,
                                                right
                                        ) -> left,
                                        LinkedHashMap::new));

        for (
                SyllabusPdfDocument.CloRow clo
                : data.clos()
        ) {
            addBodyCell(
                    table,
                    clo.code(),
                    Element.ALIGN_CENTER);

            for (
                    SyllabusPdfDocument.PloColumn plo
                    : data.plos()
            ) {
                addBodyCell(
                        table,
                        levelByCell
                                .getOrDefault(
                                        clo.id()
                                                + ":"
                                                + plo.id(),
                                        ""),
                        Element.ALIGN_CENTER);
            }
        }

        document.add(table);

        Paragraph legend =
                new Paragraph(
                        "Legend: I = Introduce, D = Develop, A = Achieve.",
                        fonts.regular(
                                7.5f,
                                MUTED));

        legend.setSpacingAfter(8);
        document.add(legend);

        if (landscape) {
            forcePageSize(
                    document,
                    writer,
                    PageSize.A4);
        }
    }

    private void forcePageSize(
            Document document,
            PdfWriter writer,
            Rectangle pageSize) {

        document.setPageSize(
                pageSize);

        /*
         * OpenPDF does not create a new page when the current page is
         * considered empty. Mark it non-empty before switching page size.
         */
        writer.setPageEmpty(false);
        document.newPage();
    }

    private void addPlannedLearningActivities(
            Document document,
            SyllabusPdfDocument data)
            throws DocumentException {

        addSectionTitle(
                document,
                "3. Planned learning activities and teaching methods");

        if (data.topics().isEmpty()) {
            addEmptyState(
                    document,
                    "No weekly topics have been defined.");
            return;
        }

        PdfPTable table =
                new PdfPTable(6);

        table.setWidthPercentage(100);

        table.setWidths(
                new float[]{
                        8,
                        29,
                        12,
                        14,
                        18,
                        19
                });

        table.setHeaderRows(1);
        table.setSplitLate(false);
        table.setSplitRows(true);
        table.setSpacingAfter(8);

        addHeaderCell(
                table,
                "Week");

        addHeaderCell(
                table,
                "Topics");

        addHeaderCell(
                table,
                "CLO");

        addHeaderCell(
                table,
                "Hours\nL/Lab/Self");

        addHeaderCell(
                table,
                "Teaching method");

        addHeaderCell(
                table,
                "Learning activities / Notes");

        for (
                SyllabusPdfDocument.TopicRow topic
                : data.topics()
        ) {
            addBodyCell(
                    table,
                    safeNumber(
                            topic.weekNumber()),
                    Element.ALIGN_CENTER);

            addBodyCell(
                    table,
                    bilingual(
                            topic.name(),
                            topic.nameVn()),
                    Element.ALIGN_LEFT);

            addBodyCell(
                    table,
                    String.join(
                            ", ",
                            topic.cloCodes()),
                    Element.ALIGN_CENTER);

            addBodyCell(
                    table,
                    safeNumber(
                            topic.teachingHours())
                            + "/"
                            + safeNumber(
                            topic.labHours())
                            + "/"
                            + safeNumber(
                            topic.selfStudyHours()),
                    Element.ALIGN_CENTER);

            addBodyCell(
                    table,
                    joinNonBlank(
                            "\n",
                            titleCase(
                                    topic.topicType()),
                            topic.teachingMethod()),
                    Element.ALIGN_LEFT);

            addBodyCell(
                    table,
                    joinNonBlank(
                            "\n",
                            topic.learningActivity(),
                            topic.notes()),
                    Element.ALIGN_LEFT);
        }

        document.add(table);
    }

    private void addAssessmentPlan(
            Document document,
            SyllabusPdfDocument data)
            throws DocumentException {

        addSectionTitle(
                document,
                "4. Assessment plan");

        if (
                data.assessments().isEmpty()
        ) {
            addEmptyState(
                    document,
                    "No assessment components have been defined.");
            return;
        }

        PdfPTable table =
                new PdfPTable(6);

        table.setWidthPercentage(100);

        table.setWidths(
                new float[]{
                        23,
                        15,
                        12,
                        14,
                        20,
                        16
                });

        table.setHeaderRows(1);
        table.setSplitLate(false);
        table.setSplitRows(true);
        table.setSpacingAfter(5);

        addHeaderCell(
                table,
                "Assessment");
        addHeaderCell(
                table,
                "Type");
        addHeaderCell(
                table,
                "Weight");
        addHeaderCell(
                table,
                "Score range");
        addHeaderCell(
                table,
                "CLO coverage");
        addHeaderCell(
                table,
                "Target / notes");

        float totalWeight = 0;

        for (
                SyllabusPdfDocument.AssessmentRow assessment
                : data.assessments()
        ) {
            totalWeight +=
                    assessment.weightPercent()
                            == null
                            ? 0
                            : assessment.weightPercent();

            addBodyCell(
                    table,
                    bilingual(
                            assessment.name(),
                            assessment.nameVn()),
                    Element.ALIGN_LEFT);

            addBodyCell(
                    table,
                    titleCase(
                            assessment.assessmentType()),
                    Element.ALIGN_LEFT);

            addBodyCell(
                    table,
                    percent(
                            assessment.weightPercent()),
                    Element.ALIGN_CENTER);

            addBodyCell(
                    table,
                    scoreRange(
                            assessment.minScore(),
                            assessment.maxScore()),
                    Element.ALIGN_CENTER);

            addBodyCell(
                    table,
                    assessment
                            .cloContributions()
                            .stream()
                            .map(
                                    item ->
                                            safe(
                                                    item.cloCode())
                                                    + (
                                                    item.contributionPercent()
                                                            == null
                                                            ? ""
                                                            : " ("
                                                            + number(
                                                            item.contributionPercent())
                                                            + "%)"
                                            ))
                            .collect(
                                    Collectors.joining(
                                            "; ")),
                    Element.ALIGN_LEFT);

            addBodyCell(
                    table,
                    "",
                    Element.ALIGN_CENTER);
        }

        document.add(table);

        Color totalColor =
                Math.abs(
                        totalWeight - 100f)
                        < 0.01f
                        ? BLUE
                        : new Color(
                        185,
                        28,
                        28);

        Paragraph summary =
                new Paragraph(
                        "Total assessment weight: "
                                + number(
                                totalWeight)
                                + "%",
                        fonts.bold(
                                8,
                                totalColor));

        summary.setAlignment(
                Element.ALIGN_RIGHT);
        summary.setSpacingAfter(8);

        document.add(summary);
    }

    private void addReadingList(
            Document document,
            SyllabusPdfDocument data)
            throws DocumentException {

        addSectionTitle(
                document,
                "5. Reading list");

        if (data.books().isEmpty()) {
            addEmptyState(
                    document,
                    "No reading materials have been defined.");
            return;
        }

        PdfPTable table =
                new PdfPTable(4);

        table.setWidthPercentage(100);

        table.setWidths(
                new float[]{
                        8,
                        18,
                        49,
                        25
                });

        table.setHeaderRows(1);
        table.setSplitLate(false);
        table.setSplitRows(true);
        table.setSpacingAfter(8);

        addHeaderCell(
                table,
                "No.");
        addHeaderCell(
                table,
                "Use");
        addHeaderCell(
                table,
                "Reference");
        addHeaderCell(
                table,
                "ISBN / URL");

        int index = 1;

        for (
                SyllabusPdfDocument.BookRow book
                : data.books()
        ) {
            addBodyCell(
                    table,
                    String.valueOf(
                            index++),
                    Element.ALIGN_CENTER);

            addBodyCell(
                    table,
                    titleCase(
                            book.usageType()),
                    Element.ALIGN_LEFT);

            addBodyCell(
                    table,
                    citation(book),
                    Element.ALIGN_LEFT);

            addBodyCell(
                    table,
                    joinNonBlank(
                            "\n",
                            book.isbn(),
                            book.url()),
                    Element.ALIGN_LEFT);
        }

        document.add(table);
    }

    private void addRevisionAndApproval(
            Document document,
            SyllabusPdfDocument data)
            throws DocumentException {

        /*
         * Do not force a new page here.
         * The previous renderer always created a new page, often leaving a
         * large blank region on the previous page.
         */
        addSectionTitle(
                document,
                "6. Revision and approval");

        PdfPTable table =
                new PdfPTable(2);

        table.setWidthPercentage(100);
        table.setWidths(
                new float[]{
                        29,
                        71
                });

        table.setSpacingAfter(8);
        table.setSplitLate(false);

        addGeneralRow(
                table,
                "Date revised",
                formatDate(
                        data.updatedAt()));

        addGeneralRow(
                table,
                "Prepared by",
                firstNonBlank(
                        data.responsiblePersons(),
                        data.createdBy()));

        addGeneralRowIfPresent(
                table,
                "Submitted at",
                formatDateOrEmpty(
                        data.submittedAt()));

        addGeneralRowIfPresent(
                table,
                "Approved by",
                data.approvedBy());

        addGeneralRowIfPresent(
                table,
                "Approved at",
                formatDateOrEmpty(
                        data.approvedAt()));

        addGeneralRowIfPresent(
                table,
                "Change summary",
                data.changeSummary());

        /*
         * Internal notes are workflow/reviewer metadata, not part of the
         * official SCSE syllabus document. They may contain structured JSON
         * from imports or internal validation snapshots, so never expose
         * them in Preview/Official PDF output.
         */

        document.add(table);

        PdfPTable signatures =
                new PdfPTable(2);

        signatures.setWidthPercentage(100);
        signatures.setWidths(
                new float[]{
                        50,
                        50
                });

        signatures.setSpacingBefore(6);
        signatures.setKeepTogether(true);

        signatures.addCell(
                signatureCell(
                        "Person responsible for the course",
                        data.responsiblePersons()));

        signatures.addCell(
                signatureCell(
                        "Head / Dean of Department / School",
                        data.approvedBy()));

        document.add(signatures);
    }

    private PdfPCell signatureCell(
            String title,
            String person) {

        PdfPCell cell =
                new PdfPCell();

        cell.setBorder(
                Rectangle.BOX);
        cell.setBorderColor(
                BORDER);
        cell.setPadding(8);
        cell.setMinimumHeight(78);
        cell.setHorizontalAlignment(
                Element.ALIGN_CENTER);

        Paragraph heading =
                new Paragraph(
                        title,
                        fonts.bold(
                                8,
                                NAVY));

        heading.setAlignment(
                Element.ALIGN_CENTER);
        heading.setSpacingAfter(24);
        cell.addElement(heading);

        Paragraph signature =
                new Paragraph(
                        "Signature",
                        fonts.regular(
                                7.4f,
                                MUTED));

        signature.setAlignment(
                Element.ALIGN_CENTER);
        signature.setSpacingAfter(7);
        cell.addElement(signature);

        Paragraph name =
                new Paragraph(
                        safe(person),
                        fonts.regular(
                                8,
                                TEXT));

        name.setAlignment(
                Element.ALIGN_CENTER);
        cell.addElement(name);

        return cell;
    }

    private void addSectionTitle(
            Document document,
            String text)
            throws DocumentException {

        Paragraph paragraph =
                new Paragraph(
                        text,
                        fonts.bold(
                                10.8f,
                                NAVY));

        paragraph.setSpacingBefore(3);
        paragraph.setSpacingAfter(5);
        paragraph.setKeepTogether(true);

        document.add(paragraph);
    }

    private void addSubsectionTitle(
            Document document,
            String text)
            throws DocumentException {

        Paragraph paragraph =
                new Paragraph(
                        text,
                        fonts.bold(
                                9.2f,
                                BLUE));

        paragraph.setSpacingBefore(2);
        paragraph.setSpacingAfter(4);
        paragraph.setKeepTogether(true);

        document.add(paragraph);
    }

    private void addEmptyState(
            Document document,
            String text)
            throws DocumentException {

        PdfPTable table =
                new PdfPTable(1);

        table.setWidthPercentage(100);
        table.setSpacingAfter(7);

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                text,
                                fonts.regular(
                                        8,
                                        MUTED)));

        cell.setBackgroundColor(
                LIGHT_SLATE);
        cell.setBorderColor(
                BORDER);
        cell.setPadding(6);

        table.addCell(cell);
        document.add(table);
    }

    private void addGeneralRowIfPresent(
            PdfPTable table,
            String label,
            String value) {

        if (isBlank(value)) {
            return;
        }

        addGeneralRow(
                table,
                label,
                value);
    }

    private void addGeneralRow(
            PdfPTable table,
            String label,
            String value) {

        PdfPCell labelCell =
                new PdfPCell(
                        new Phrase(
                                label,
                                fonts.bold(
                                        7.7f,
                                        NAVY)));

        labelCell.setBackgroundColor(
                LIGHT_SLATE);
        labelCell.setBorderColor(
                BORDER);
        labelCell.setPadding(4.5f);
        labelCell.setVerticalAlignment(
                Element.ALIGN_TOP);

        table.addCell(labelCell);

        PdfPCell valueCell =
                new PdfPCell(
                        new Phrase(
                                safe(value),
                                fonts.regular(
                                        7.9f,
                                        TEXT)));

        valueCell.setBorderColor(
                BORDER);
        valueCell.setPadding(4.5f);
        valueCell.setVerticalAlignment(
                Element.ALIGN_TOP);

        table.addCell(valueCell);
    }

    private void addLabelCell(
            PdfPTable table,
            String value) {

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                value,
                                fonts.bold(
                                        7.5f,
                                        NAVY)));

        cell.setBackgroundColor(
                LIGHT_BLUE);
        cell.setBorderColor(
                BORDER);
        cell.setPadding(4.5f);

        table.addCell(cell);
    }

    private void addValueCell(
            PdfPTable table,
            String value) {

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                safe(value),
                                fonts.regular(
                                        7.8f,
                                        TEXT)));

        cell.setBorderColor(
                BORDER);
        cell.setPadding(4.5f);

        table.addCell(cell);
    }

    private void addHeaderCell(
            PdfPTable table,
            String value) {

        /*
         * Light header background instead of a very dark navy bar.
         * This keeps exported PDFs readable when printed in grayscale.
         */
        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                value,
                                fonts.bold(
                                        7.2f,
                                        NAVY)));

        cell.setBackgroundColor(
                LIGHT_BLUE);
        cell.setBorderColor(
                BORDER);
        cell.setPadding(4.5f);
        cell.setHorizontalAlignment(
                Element.ALIGN_CENTER);
        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE);

        table.addCell(cell);
    }

    private void addBodyCell(
            PdfPTable table,
            String value,
            int alignment) {

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                safeForCell(
                                        value),
                                fonts.regular(
                                        7.3f,
                                        TEXT)));

        cell.setBorderColor(
                BORDER);
        cell.setPadding(3.8f);
        cell.setHorizontalAlignment(
                alignment);
        cell.setVerticalAlignment(
                Element.ALIGN_TOP);

        table.addCell(cell);
    }

    private String watermark(
            SyllabusPdfDocument data,
            SyllabusPdfMode mode) {

        String status =
                safe(
                        data.status())
                        .toUpperCase(
                                Locale.ROOT);

        /*
         * Preview watermark is deliberately short to reduce visual noise.
         * APPROVED official exports have no watermark.
         */
        if (
                mode
                        == SyllabusPdfMode.PREVIEW
        ) {
            return "PREVIEW";
        }

        return switch (status) {
            case "APPROVED" ->
                    null;

            case "SUBMITTED",
                 "UNDER_REVIEW" ->
                    "UNDER REVIEW";

            case "REJECTED",
                 "REVISION_REQUESTED" ->
                    "REVISION REQUIRED";

            default ->
                    "DRAFT";
        };
    }

    private static String citation(
            SyllabusPdfDocument.BookRow book) {

        List<String> parts =
                new ArrayList<>();

        if (
                !isBlank(
                        book.author())
        ) {
            parts.add(
                    book.author());
        }

        if (
                !isBlank(
                        book.title())
        ) {
            parts.add(
                    book.title());
        }

        if (
                !isBlank(
                        book.edition())
        ) {
            parts.add(
                    book.edition());
        }

        if (
                !isBlank(
                        book.publisher())
        ) {
            parts.add(
                    book.publisher());
        }

        if (
                book.year() != null
        ) {
            parts.add(
                    String.valueOf(
                            book.year()));
        }

        return parts.isEmpty()
                ? "-"
                : String.join(
                ", ",
                parts)
                + ".";
    }

    private static String creditText(
            Integer theory,
            Integer lab) {

        int theoryValue =
                theory == null
                        ? 0
                        : theory;

        int labValue =
                lab == null
                        ? 0
                        : lab;

        int total =
                theoryValue
                        + labValue;

        return total
                + " credits (Theory: "
                + theoryValue
                + ", Laboratory/Practice: "
                + labValue
                + ")";
    }

    private static String scoreRange(
            Float min,
            Float max) {

        if (
                min == null
                        && max == null
        ) {
            return "-";
        }

        return number(min)
                + " - "
                + number(max);
    }

    private static String percent(
            Float value) {

        return value == null
                ? "-"
                : number(value)
                + "%";
    }

    private static String number(
            Number value) {

        if (value == null) {
            return "-";
        }

        double number =
                value.doubleValue();

        if (
                Math.rint(number)
                        == number
        ) {
            return String.valueOf(
                    (long) number);
        }

        return String
                .format(
                        Locale.ENGLISH,
                        "%.2f",
                        number)
                .replaceAll(
                        "0+$",
                        "")
                .replaceAll(
                        "\\.$",
                        "");
    }

    private static String safeNumber(
            Number value) {

        return value == null
                ? "-"
                : String.valueOf(
                value);
    }

    private static String formatDate(
            LocalDateTime value) {

        return value == null
                ? "-"
                : DATE_TIME.format(
                value);
    }

    private static String formatDateOrEmpty(
            LocalDateTime value) {

        return value == null
                ? ""
                : DATE_TIME.format(
                value);
    }

    private static String bilingual(
            String english,
            String vietnamese) {

        if (isBlank(english)) {
            return safe(vietnamese);
        }

        if (
                isBlank(vietnamese)
                        || Objects.equals(
                        english.trim(),
                        vietnamese.trim())
        ) {
            return english.trim();
        }

        return english.trim()
                + "\n"
                + vietnamese.trim();
    }

    private static String labeled(
            String label,
            String value) {

        return isBlank(value)
                ? null
                : label
                + ": "
                + value.trim();
    }

    private static String joinNonBlank(
            String separator,
            String... values) {

        return java.util.Arrays
                .stream(values)
                .filter(
                        value ->
                                !isBlank(value))
                .map(String::trim)
                .collect(
                        Collectors.joining(
                                separator));
    }

    private static String firstNonBlank(
            String... values) {

        for (
                String value
                : values
        ) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }

        return "-";
    }

    private static String firstNonBlankOrEmpty(
            String... values) {

        for (
                String value
                : values
        ) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }

        return "";
    }

    private static String safe(
            String value) {

        return isBlank(value)
                ? "-"
                : value.trim();
    }

    private static String safeForCell(
            String value) {

        return value == null
                ? ""
                : value.trim();
    }

    private static boolean isBlank(
            String value) {

        return value == null
                || value.isBlank();
    }

    private static String titleCase(
            String value) {

        if (isBlank(value)) {
            return "-";
        }

        String normalized =
                value.trim()
                        .replace(
                                '_',
                                ' ')
                        .toLowerCase(
                                Locale.ROOT);

        StringBuilder output =
                new StringBuilder();

        for (
                String token
                : normalized.split(
                "\\s+")
        ) {
            if (
                    output.length() > 0
            ) {
                output.append(' ');
            }

            output
                    .append(
                            Character.toUpperCase(
                                    token.charAt(0)))
                    .append(
                            token.substring(1));
        }

        return output.toString();
    }

    private static String statusLabel(
            String status) {

        return switch (
                safe(status)
                        .toUpperCase(
                                Locale.ROOT)
        ) {
            case "DRAFT" ->
                    "Draft";

            case "SUBMITTED" ->
                    "Submitted";

            case "UNDER_REVIEW" ->
                    "Under review";

            case "APPROVED" ->
                    "Approved";

            case "REJECTED" ->
                    "Rejected";

            case "REVISION_REQUESTED" ->
                    "Revision requested";

            case "ARCHIVED" ->
                    "Archived";

            default ->
                    safe(status);
        };
    }
}
