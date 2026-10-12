package com.scse.curriculum.syllabus.pdf;

import com.scse.curriculum.syllabus.entity.SyllabusVersion;

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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final ObjectMapper objectMapper;
    private final String institutionName;
    private final String schoolName;

    public SyllabusPdfRenderer(
            SyllabusPdfFontProvider fonts,
            ObjectMapper objectMapper,
            @Value("${app.pdf.institution-name:VIETNAM NATIONAL UNIVERSITY HCMC - INTERNATIONAL UNIVERSITY}")
            String institutionName,
            @Value("${app.pdf.school-name:SCHOOL OF COMPUTER SCIENCE AND ENGINEERING}")
            String schoolName) {
        this.fonts = fonts;
        this.objectMapper = objectMapper;
        this.institutionName = institutionName;
        this.schoolName = schoolName;
    }

    public byte[] render(SyllabusPdfDocument data, SyllabusPdfMode mode) {
        return render(data, mode, null);
    }

    /** Native paragraphs and tables shared by PDF and editable Word export. */
    public record LayoutBlock(Element element, Rectangle pageSize) { }

    public List<LayoutBlock> layout(SyllabusPdfDocument data, SyllabusPdfMode mode) {
        List<LayoutBlock> blocks = new ArrayList<>();
        render(data, mode, blocks);
        return List.copyOf(blocks);
    }

    private byte[] render(SyllabusPdfDocument data, SyllabusPdfMode mode, List<LayoutBlock> blocks) {
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
                    44) {
                @Override
                public boolean add(Element element) throws DocumentException {
                    if (blocks != null) {
                        blocks.add(new LayoutBlock(element, new Rectangle(getPageSize())));
                    }
                    return super.add(element);
                }
            };

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

            if (isNew2027Target(data)) {
                renderNew2027Target(document, writer, data, mode);
            } else {
                addGeneralInformation(document, data);
                addLearningOutcomesMatrix(document, writer, data);
                addPlannedLearningActivities(document, data);
                addAssessmentPlan(document, data);
                addRubrics(document, data);
                addRevisionAndApproval(document, data);
            }

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
                "Course Name: " + safe(data.courseName()),
                fonts.bold(10.5f, TEXT));
        courseName.setAlignment(Element.ALIGN_CENTER);
        courseName.setSpacingAfter(2);
        document.add(courseName);

        Paragraph courseCode = new Paragraph(
                "Course Code: " + safe(data.courseCode()),
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
                SyllabusVersion.display(data.versionNumber(), data.versionLabel()));

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

    /* PATCH_3_4A_NEW_2027_EXPORT */
    private static boolean isNew2027Target(SyllabusPdfDocument data) {
        return data != null
                && "NEW_2027".equalsIgnoreCase(
                        safeForCell(data.targetTemplateProfile()));
    }

    private void renderNew2027Target(
            Document document,
            PdfWriter writer,
            SyllabusPdfDocument data,
            SyllabusPdfMode mode)
            throws DocumentException {

        addIdentityCard(document, data, mode);
        addNew2027GeneralInformation(document, data);
        addSourceTables(document, data, "1");
        addNew2027Objectives(document, data);
        addSourceTables(document, data, "2");
        addNew2027Clos(document, data);
        addSourceTables(document, data, "3");
        addNew2027Content(document, data);
        addSourceTables(document, data, "4");
        addNew2027CloPlo(document, writer, data);
        addSourceTables(document, data, "5");
        addNew2027CloLlo(document);
        addSourceTables(document, data, "6");
        addNew2027Examination(document, data);
        addSourceTables(document, data, "7");
        addNew2027StudyRequirements(document, data);
        addSourceTables(document, data, "8");
        addNew2027PlannedActivities(document, data);
        addSourceTables(document, data, "9");
        addNew2027Assessment(document, data);
        addSourceTables(document, data, "10");
        addNew2027Rubrics(document, data);
        addSourceTables(document, data, "11");
        addNew2027Readings(document, data);
        addSourceTables(document, data, "12");
    }

    private boolean hasSourceTables(SyllabusPdfDocument data, String section) {
        JsonNode tables = importedNotes(data).path("sourceSupplementalSections").path(section);
        return tables.isArray() && !tables.isEmpty();
    }

    /** The same source table goes to PDF and DOCX; source blanks remain blank. */
    private void addSourceTables(Document document, SyllabusPdfDocument data, String section) throws DocumentException {
        for (JsonNode source : importedNotes(data).path("sourceSupplementalSections").path(section)) {
            if ("11".equals(section) && "DS2026_RUBRIC_GRID_V1".equals(source.path("format").asText())) {
                addSourceRubricGrid(document, source);
                continue;
            }
            JsonNode headers = source.path("headers");
            if (!headers.isArray() || headers.isEmpty()) continue;
            addTextOrEmpty(document, source.path("title").asText(""), "");
            List<String> pages = new ArrayList<>();
            source.path("pages").forEach(page -> pages.add(page.asText()));
            if (!pages.isEmpty()) addTextOrEmpty(document, "Source PDF pages: " + String.join(", ", pages), "");
            PdfPTable table = new PdfPTable(headers.size());
            table.setWidthPercentage(100);
            // Give long source narratives room without turning TT or LO into inferred numbers.
            float[] widths = new float[headers.size()];
            java.util.Arrays.fill(widths, 1f);
            for (int index = 0; index < headers.size(); index++) {
                String label = headers.get(index).asText().toLowerCase(java.util.Locale.ROOT);
                if (label.contains("hoạt động") || label.contains("mô tả") || label.contains("tiêu chí") || label.contains("chuẩn đầu ra") || label.contains("topics")) widths[index] = 3f;
                else if (label.contains("nội dung") || label.equals("tên")) widths[index] = 2f;
            }
            if ("12".equals(section) && "DS2026_READING_ROWS_V1".equals(source.path("format").asText()) && widths.length == 2)
                widths = new float[]{1, 12};
            table.setWidths(widths);
            table.setHeaderRows(1);
            table.setSplitLate(false);
            table.setSplitRows(true);
            table.setSpacingAfter(8);
            headers.forEach(header -> addHeaderCell(table, header.asText("")));
            for (JsonNode row : source.path("rows"))
                for (int index = 0; index < headers.size(); index++) addBodyCell(table, row.path(index).asText(""), Element.ALIGN_LEFT);
            document.add(table);
        }
    }

    private void addSourceRubricGrid(Document document, JsonNode source) throws DocumentException {
        addTextOrEmpty(document, source.path("title").asText(), "");
        int previousPage = -1;
        for (JsonNode block : source.path("blocks")) {
            int page = block.path("page").asInt();
            if (page != previousPage) addTextOrEmpty(document, "Source PDF page: " + page, "");
            previousPage = page;
            if ("text".equals(block.path("kind").asText())) {
                addTextOrEmpty(document, block.path("text").asText(), "");
                continue;
            }
            JsonNode widths = block.path("widths");
            if (!widths.isArray() || widths.isEmpty()) continue;
            PdfPTable table = new PdfPTable(widths.size());
            float[] values = new float[widths.size()];
            for (int i = 0; i < values.length; i++) values[i] = (float) widths.get(i).asDouble();
            table.setWidths(values);
            table.setWidthPercentage(100);
            table.setSplitLate(false);
            table.setSplitRows(true);
            table.setSpacingAfter(8);
            for (JsonNode row : block.path("rows")) for (JsonNode cell : row) {
                PdfPCell rendered = new PdfPCell(fonts.phrase(cell.path("text").asText(""), fonts.regular(7.3f, TEXT)));
                rendered.setBorderColor(BORDER);
                rendered.setColspan(cell.path("colSpan").asInt(1));
                rendered.setRowspan(cell.path("rowSpan").asInt(1));
                rendered.setVerticalAlignment(Element.ALIGN_TOP);
                rendered.setPadding(4);
                table.addCell(rendered);
            }
            document.add(table);
        }
    }

    private void addNew2027GeneralInformation(
            Document document,
            SyllabusPdfDocument data)
            throws DocumentException {

        addSectionTitle(document, "1. General Information");

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{29, 71});
        table.setSpacingAfter(8);
        table.setSplitLate(false);
        table.setSplitRows(true);

        addGeneralRow(table, "Course Code", data.courseCode());
        addGeneralRow(table, "Course Name", data.courseName());
        addGeneralRow(table, "Course Designation", data.courseDesignation());
        addGeneralRow(table, "Course Type", data.courseTypes());
        String sourceSemester = importedNoteText(data, "sourceSemester");
        addGeneralRow(table, "Semester", sourceSemester.isBlank() ? data.semester() : sourceSemester);
        addGeneralRow(table, "Person Responsible", responsiblePersons(data));
        addGeneralRow(table, "Language", data.language());
        addGeneralRow(table, "Relation to Curriculum", sourceCheckboxText(data.relation()));
        addGeneralRow(table, "Teaching Methods", data.teachingMethods());
        addGeneralRow(table, importedNotes(data).path("sourceWorkloadEstimated").asBoolean(false)
                ? "Total Workload (estimated)" : "Total Workload", data.workloadTotal());
        boolean sourceTiet = "tiết".equals(importedNoteText(data, "sourceWorkloadUnit"));
        addGeneralRow(table, sourceTiet ? "Contact workload (source unit: tiết)" : "Contact Hours", data.workloadContact());
        addGeneralRow(table, sourceTiet ? "Self-study workload (source unit: tiết)" : "Self-study Hours", data.workloadPrivate());
        addGeneralRow(table, "Credit Points", importedNoteText(data, "creditPoints"));
        addGeneralRow(table, "Theory / Lecture Credits", importedNoteText(data, "lectureCredits"));
        addGeneralRow(table, "Practice / Laboratory Credits", importedNoteText(data, "laboratoryCredits"));
        addGeneralRow(table, "Prerequisites", data.prerequisites());

        document.add(table);
    }

    private void addNew2027Objectives(
            Document document,
            SyllabusPdfDocument data)
            throws DocumentException {
        addSectionTitle(document, "2. Course Objectives");
        addTextOrEmpty(document, data.objectives(), "No course objectives are available for this syllabus.");
    }

    private void addNew2027Clos(
            Document document,
            SyllabusPdfDocument data)
            throws DocumentException {
        addSectionTitle(document, "3. Course Learning Outcomes (CLO)");

        if (data.clos().isEmpty()) {
            addEmptyState(document, "No course learning outcomes are available for this syllabus.");
            return;
        }

        Map<Integer, String> ploCodeById = data.plos().stream()
                .filter(plo -> plo.id() != null)
                .collect(Collectors.toMap(
                        SyllabusPdfDocument.PloColumn::id,
                        plo -> safeForCell(plo.code()),
                        (left, ignored) -> left,
                        LinkedHashMap::new));

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{13, 12, 27, 33, 15});
        table.setHeaderRows(1);
        table.setSplitLate(false);
        table.setSplitRows(true);
        table.setSpacingAfter(8);

        addHeaderCell(table, "Competency Level");
        addHeaderCell(table, "CLO Code");
        addHeaderCell(table, "CLO — Vietnamese");
        addHeaderCell(table, "CLO — English");
        addHeaderCell(table, "Mapped PLOs");

        for (SyllabusPdfDocument.CloRow clo : data.clos()) {
            String mappedPlos = data.cloPloCells().stream()
                    .filter(cell -> Objects.equals(cell.cloId(), clo.id()))
                    .map(cell -> ploCodeById.get(cell.ploId()))
                    .filter(value -> value != null && !value.isBlank())
                    .distinct()
                    .collect(Collectors.joining(", "));

            addBodyCell(table, titleCase(clo.competencyLevel()), Element.ALIGN_LEFT);
            addBodyCell(table, clo.code(), Element.ALIGN_CENTER);
            addBodyCell(table, clo.descriptionVn(), Element.ALIGN_LEFT);
            addBodyCell(table, "VI".equals(importedNoteText(data, "sourceCloLanguage")) && Objects.equals(clo.description(), clo.descriptionVn())
                    ? "" : clo.description(), Element.ALIGN_LEFT);
            addBodyCell(table, mappedPlos, Element.ALIGN_CENTER);
        }

        document.add(table);
    }

    private void addNew2027Content(
            Document document,
            SyllabusPdfDocument data)
            throws DocumentException {
        addSectionTitle(document, "4. Course Content");
        JsonNode sourceContentRows = importedNotes(data).path("sourceContentRows");
        String sourceDescription=importedNotes(data).path("contentNote").asText("");
        if(!sourceDescription.isBlank())addTextOrEmpty(document,sourceDescription,"");
        boolean hasSourceRows = sourceContentRows.isArray() && sourceContentRows.size() > 0;
        if (data.topics().isEmpty() && !hasSourceRows) {
            if(!sourceDescription.isBlank())return;
            addEmptyState(document, "No course-content rows are available for this syllabus.");
            return;
        }

        PdfPTable table = new PdfPTable(hasSourceRows ? 7 : 5);
        table.setWidthPercentage(100);
        table.setWidths(hasSourceRows ? new float[]{15, 30, 8, 10, 12, 7, 18} : new float[]{25, 30, 14, 14, 17});
        table.setHeaderRows(1);
        table.setSplitLate(false);
        table.setSplitRows(true);
        table.setSpacingAfter(8);

        addHeaderCell(table, "Topic — Vietnamese");
        addHeaderCell(table, "Topic — English");
        addHeaderCell(table, "Weight");
        addHeaderCell(table, "Level (I/T/U)");
        addHeaderCell(table, "Related CLOs");
        if (hasSourceRows) { addHeaderCell(table, "Week"); addHeaderCell(table, "Hours"); }

        if (hasSourceRows) {
            addEmptyState(document, "The source content table is shown separately from the weekly activity plan. Week, hours and related CLOs are not inferred for unverified rows.");
            for (JsonNode row : sourceContentRows) {
                addBodyCell(table, row.path("nameVn").asText(""), Element.ALIGN_LEFT);
                addBodyCell(table, "VI".equals(importedNoteText(data, "sourceContentLanguage")) && row.path("name").equals(row.path("nameVn"))
                        ? "" : row.path("name").asText(""), Element.ALIGN_LEFT);
                addBodyCell(table, row.path("weight").asText(""), Element.ALIGN_CENTER);
                addBodyCell(table, row.path("level").asText(""), Element.ALIGN_CENTER);
                String hours = java.util.stream.Stream.of("teachingHours", "labHours", "selfStudyHours")
                        .filter(key -> !row.path(key).isMissingNode() && !row.path(key).isNull())
                        .map(key -> (key.equals("teachingHours") ? "Teaching" : key.equals("labHours") ? "Lab" : "Self-study") + ": " + row.path(key).asText())
                        .collect(Collectors.joining("; "));
                List<String> related = new ArrayList<>();
                row.path("relatedCloCodes").forEach(code -> related.add(code.asText()));
                addBodyCell(table, String.join(", ", related), Element.ALIGN_CENTER);
                addBodyCell(table, row.path("weekNumber").asText(""), Element.ALIGN_CENTER);
                addBodyCell(table, hours, Element.ALIGN_LEFT);
            }
        } else for (SyllabusPdfDocument.TopicRow topic : data.topics()) {
            addBodyCell(table, topic.nameVn(), Element.ALIGN_LEFT);
            addBodyCell(table, topic.name(), Element.ALIGN_LEFT);
            addBodyCell(table, topicMetadataText(topic, "contentWeight"), Element.ALIGN_CENTER);
            addBodyCell(table, topicMetadataText(topic, "contentLevel"), Element.ALIGN_CENTER);
            addBodyCell(
                    table,
                    topic.cloCodes() == null ? "" : String.join(", ", topic.cloCodes()),
                    Element.ALIGN_CENTER);
        }

        document.add(table);
    }

    private void addNew2027CloPlo(
            Document document,
            PdfWriter writer,
            SyllabusPdfDocument data)
            throws DocumentException {
        addSectionTitle(document, "5. Course CLO–PLO Alignment");

        JsonNode sourceNotes = importedNotes(data);
        JsonNode sourceMatrix = sourceNotes.path("cloPloMatrix");
        if (sourceNotes.path("sourceAlignmentPreserved").asBoolean(false) && sourceMatrix.isObject() && sourceMatrix.size() > 0) {
            java.util.Set<String> columns = new java.util.LinkedHashSet<>();
            sourceNotes.path("ploCodes").forEach(code -> columns.add(code.asText()));
            sourceMatrix.fields().forEachRemaining(row -> row.getValue().fieldNames().forEachRemaining(columns::add));
            List<String> orderedColumns = columns.stream().sorted(Comparator.comparingInt(code -> {
                try { return Integer.parseInt(code.replaceAll("\\D", "")); } catch (NumberFormatException error) { return Integer.MAX_VALUE; }
            })).toList();
            PdfPTable raw = new PdfPTable(orderedColumns.size() + 1);
            raw.setWidthPercentage(100); raw.setHeaderRows(1); raw.setSplitRows(true);
            raw.setSpacingBefore(6); raw.setSpacingAfter(8);
            addHeaderCell(raw, "CLO"); for (String column : orderedColumns) addHeaderCell(raw, column);
            sourceMatrix.fields().forEachRemaining(row -> {
                addBodyCell(raw, row.getKey(), Element.ALIGN_CENTER);
                for (String column : orderedColumns) addBodyCell(raw, row.getValue().path(column).asText(""), Element.ALIGN_CENTER);
            });
            document.add(raw);
            addTextOrEmpty(document, "Source alignment marks are preserved verbatim. Numeric marks are not converted to percentages or contribution enums; source-only PLO columns do not activate programme outcomes.", "");
            return;
        }

        if (data.plos().isEmpty()) {
            addEmptyState(document, "No CLO-PLO alignment data is available for this syllabus.");
            return;
        }

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{14, 18, 43, 25});
        table.setHeaderRows(1);
        table.setSplitLate(false);
        table.setSplitRows(true);
        table.setSpacingAfter(8);

        addHeaderCell(table, "PLO");
        addHeaderCell(table, "PLO Group");
        addHeaderCell(table, "PLO Description");
        addHeaderCell(table, "Contributing CLOs");

        Map<Integer, String> cloCodeById = data.clos().stream()
                .filter(clo -> clo.id() != null)
                .collect(Collectors.toMap(
                        SyllabusPdfDocument.CloRow::id,
                        clo -> safeForCell(clo.code()),
                        (left, ignored) -> left,
                        LinkedHashMap::new));

        for (SyllabusPdfDocument.PloColumn plo : data.plos()) {
            String contributingClos = data.cloPloCells().stream()
                    .filter(cell -> Objects.equals(cell.ploId(), plo.id()))
                    .map(cell -> cloCodeById.get(cell.cloId()))
                    .filter(value -> value != null && !value.isBlank())
                    .distinct()
                    .collect(Collectors.joining(", "));

            addBodyCell(table, plo.code(), Element.ALIGN_CENTER);
            addBodyCell(table, "", Element.ALIGN_CENTER);
            addBodyCell(table, plo.description(), Element.ALIGN_LEFT);
            addBodyCell(table, contributingClos, Element.ALIGN_CENTER);
        }

        document.add(table);
    }

    private void addNew2027CloLlo(Document document)
            throws DocumentException {
        addSectionTitle(document, "6. Detailed CLO–LLO Table");
        addEmptyState(
                document,
                "Structured LLO data has not been verified against the source. No LLO values are generated.");
    }

    private void addNew2027Examination(
            Document document,
            SyllabusPdfDocument data)
            throws DocumentException {
        addSectionTitle(document, "7. Examination Forms");
        addTextOrEmpty(document, data.examForms(), "No examination-form data is available for this syllabus.");
    }

    private void addNew2027StudyRequirements(
            Document document,
            SyllabusPdfDocument data)
            throws DocumentException {
        addSectionTitle(document, "8. Study and Examination Requirements");

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{31, 69});
        table.setSpacingAfter(8);
        table.setSplitLate(false);

        addGeneralRow(table, "Study / Examination Requirements", data.examRequirements());
        addGeneralRow(table, "Passing Requirement", importedNoteText(data, "assessmentPassNote"));
        document.add(table);
    }

    private void addNew2027PlannedActivities(
            Document document,
            SyllabusPdfDocument data)
            throws DocumentException {
        addSectionTitle(document, "9. Planned Learning Activities and Teaching Methods");
        if (hasSourceTables(data, "9")) return;
        if (importedNotes(data).path("sourceWeeklyReviewRequired").asBoolean(false))
            addEmptyState(document, "Only verified weekly rows are shown. Additional source rows require review; Course Content is preserved independently in Section 4.");

        JsonNode planned = importedNotes(data).path("plannedActivities");
        boolean hasImportedPlan = planned.isArray() && planned.size() > 0;

        if (!hasImportedPlan && data.topics().isEmpty()) {
            addEmptyState(document, "No planned-learning-activity data is available for this syllabus.");
            return;
        }

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{8, 31, 11, 18, 18, 14});
        table.setHeaderRows(1);
        table.setSplitLate(true);
        table.setSplitRows(true);
        table.setSpacingAfter(8);

        addHeaderCell(table, "Week");
        addHeaderCell(table, "Topic");
        addHeaderCell(table, "CLO");
        addHeaderCell(table, "Activities");
        addHeaderCell(table, "Assessment");
        addHeaderCell(table, "Resources");

        if (hasImportedPlan) {
            for (JsonNode row : planned) {
                addBodyCell(table, row.path("week").asText(""), Element.ALIGN_CENTER);
                addBodyCell(table, row.path("topic").asText(""), Element.ALIGN_LEFT);
                addBodyCell(table, row.path("clo").asText(""), Element.ALIGN_CENTER);
                addBodyCell(table, row.path("learningActivities").asText(""), Element.ALIGN_LEFT);
                addBodyCell(table, row.path("assessments").asText(""), Element.ALIGN_LEFT);
                addBodyCell(table, row.path("resources").asText(""), Element.ALIGN_LEFT);
            }
        } else {
            for (SyllabusPdfDocument.TopicRow topic : data.topics()) {
                addBodyCell(table, safeNumber(topic.weekNumber()), Element.ALIGN_CENTER);
                addBodyCell(table, bilingual(topic.name(), topic.nameVn()), Element.ALIGN_LEFT);
                addBodyCell(
                        table,
                        topic.cloCodes() == null ? "" : String.join(", ", topic.cloCodes()),
                        Element.ALIGN_CENTER);
                addBodyCell(
                        table,
                        joinNonBlank(", ", topic.teachingMethod(), topic.learningActivity()),
                        Element.ALIGN_LEFT);
                addBodyCell(table, topic.assessments(), Element.ALIGN_LEFT);
                addBodyCell(table, topic.resources(), Element.ALIGN_LEFT);
            }
        }

        document.add(table);
    }

    private void addNew2027Assessment(
            Document document,
            SyllabusPdfDocument data)
            throws DocumentException {
        addSectionTitle(document, "10. Assessment Plan");
        if (hasSourceTables(data, "10")) return;

        if (data.assessments().isEmpty()) {
            addEmptyState(document, "No assessment-plan data is available for this syllabus.");
            return;
        }

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{42, 16, 20, 22});
        table.setHeaderRows(1);
        table.setSplitLate(false);
        table.setSplitRows(true);
        table.setSpacingAfter(8);

        addHeaderCell(table, "Assessment");
        addHeaderCell(table, "Weight (%)");
        addHeaderCell(table, "CLO");
        addHeaderCell(table, "Contribution (%)");

        for (SyllabusPdfDocument.AssessmentRow assessment : data.assessments()) {
            if (assessment.cloContributions() == null || assessment.cloContributions().isEmpty()) {
                addBodyCell(table, firstNonBlankOrEmpty(assessment.name(), assessment.nameVn()), Element.ALIGN_LEFT);
                addBodyCell(table, numberOrEmpty(assessment.weightPercent()), Element.ALIGN_CENTER);
                addBodyCell(table, "", Element.ALIGN_CENTER);
                addBodyCell(table, "", Element.ALIGN_CENTER);
                continue;
            }

            boolean first = true;
            for (SyllabusPdfDocument.AssessmentCloRow contribution : assessment.cloContributions()) {
                addBodyCell(
                        table,
                        first ? firstNonBlankOrEmpty(assessment.name(), assessment.nameVn()) : "",
                        Element.ALIGN_LEFT);
                addBodyCell(
                        table,
                        first ? numberOrEmpty(assessment.weightPercent()) : "",
                        Element.ALIGN_CENTER);
                addBodyCell(table, contribution.cloCode(), Element.ALIGN_CENTER);
                addBodyCell(table, numberOrEmpty(contribution.contributionPercent()), Element.ALIGN_CENTER);
                first = false;
            }
        }

        document.add(table);
    }

    private void addNew2027Rubrics(
            Document document,
            SyllabusPdfDocument data)
            throws DocumentException {
        addSectionTitle(document, "11. Assignment Description and Rubric Summary");

        String raw = data.rubrics();
        boolean sourceGrid = false;
        for (JsonNode source : importedNotes(data).path("sourceSupplementalSections").path("11"))
            if ("DS2026_RUBRIC_GRID_V1".equals(source.path("format").asText())) sourceGrid = true;
        if (raw == null || raw.isBlank()) {
            if (sourceGrid) return;
            addEmptyState(document, "No assignment/rubric data is available for this syllabus.");
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode rubrics = root.path("rubrics");
            if (!rubrics.isArray() || rubrics.isEmpty()) {
                if (sourceGrid && rubrics.isArray() && rubrics.isEmpty()) return;
                addTextOrEmpty(document, raw, "No assignment/rubric data is available for this syllabus.");
                return;
            }

            int rubricNumber = 1;
            for (JsonNode rubric : rubrics) {
                String title = rubric.path("title").asText("Rubric " + rubricNumber);
                Paragraph heading = new Paragraph(
                        "11." + rubricNumber + ". " + title,
                        fonts.bold(9, NAVY));
                heading.setSpacingBefore(4);
                heading.setSpacingAfter(4);
                document.add(heading);

                JsonNode labels = rubric.path("scaleLabels");
                JsonNode criteria = rubric.path("criteria");
                int scaleCount = labels.isArray() ? labels.size() : 0;
                if (scaleCount == 0 || !criteria.isArray() || criteria.isEmpty()) {
                    addEmptyState(document, "No rubric criteria are available for this syllabus.");
                    rubricNumber++;
                    continue;
                }

                PdfPTable table = new PdfPTable(scaleCount + 1);
                table.setWidthPercentage(100);
                float[] widths = new float[scaleCount + 1];
                widths[0] = 30;
                for (int index = 1; index < widths.length; index++) {
                    widths[index] = 70f / scaleCount;
                }
                table.setWidths(widths);
                table.setHeaderRows(1);
                table.setSplitLate(false);
                table.setSplitRows(true);
                table.setSpacingAfter(8);

                addHeaderCell(table, "Criterion");
                for (JsonNode label : labels) {
                    addHeaderCell(table, label.asText("Level"));
                }

                for (JsonNode criterion : criteria) {
                    addBodyCell(table, criterion.path("criterion").asText(""), Element.ALIGN_LEFT);
                    JsonNode levels = criterion.path("levels");
                    for (int index = 0; index < scaleCount; index++) {
                        addBodyCell(
                                table,
                                levels.isArray() && index < levels.size()
                                        ? levels.get(index).asText("")
                                        : "",
                                Element.ALIGN_LEFT);
                    }
                }
                document.add(table);
                rubricNumber++;
            }
        } catch (Exception ignored) {
            addTextOrEmpty(document, raw, "No assignment/rubric data is available for this syllabus.");
        }
    }

    private void addNew2027Readings(
            Document document,
            SyllabusPdfDocument data)
            throws DocumentException {
        addSectionTitle(document, "12. Reading List");
        for (JsonNode source : importedNotes(data).path("sourceSupplementalSections").path("12"))
            if ("DS2026_READING_ROWS_V1".equals(source.path("format").asText())) return;

        String sourceReadingList=importedNotes(data).path("sourceReadingList").asText("");
        if(!sourceReadingList.isBlank()) {
            addTextOrEmpty(document,sourceReadingList,"");
            return;
        }

        if (data.books().isEmpty()) {
            addEmptyState(document, "No reading-list data is available for this syllabus.");
            return;
        }

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{31, 22, 12, 23, 12});
        table.setHeaderRows(1);
        table.setSplitLate(false);
        table.setSplitRows(true);
        table.setSpacingAfter(8);

        addHeaderCell(table, "Title");
        addHeaderCell(table, "Author");
        addHeaderCell(table, "Publication Year");
        addHeaderCell(table, "Publisher");
        addHeaderCell(table, "Usage Type");

        for (SyllabusPdfDocument.BookRow book : data.books()) {
            addBodyCell(table, book.title(), Element.ALIGN_LEFT);
            addBodyCell(table, book.author(), Element.ALIGN_LEFT);
            addBodyCell(table, book.year() == null ? "" : String.valueOf(book.year()), Element.ALIGN_CENTER);
            addBodyCell(table, book.publisher(), Element.ALIGN_LEFT);
            addBodyCell(table, "SOURCE_UNSPECIFIED".equals(importedNotes(data).path("readingUsagePolicy").asText())
                    ? "Not specified in source" : titleCase(book.usageType()), Element.ALIGN_CENTER);
        }

        document.add(table);
    }

    private void addTextOrEmpty(
            Document document,
            String value,
            String emptyMessage)
            throws DocumentException {
        if (isBlank(value)) {
            addEmptyState(document, emptyMessage);
            return;
        }

        Paragraph paragraph = new Paragraph(value.trim(), fonts.regular(8.2f, TEXT));
        paragraph.setLeading(11.5f);
        paragraph.setSpacingAfter(8);
        document.add(paragraph);
    }

    private JsonNode importedNotes(SyllabusPdfDocument data) {
        if (data == null || isBlank(data.notes())) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode root = objectMapper.readTree(data.notes());
            return root != null && root.isObject()
                    ? root
                    : objectMapper.createObjectNode();
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private String sourceCheckboxText(String value) {
        return value == null ? null : value.replace("☒", "[checked]").replace("🗹", "[checked]")
                .replace("□", "[unchecked]").replace("☐", "[unchecked]");
    }

    private String importedNoteText(
            SyllabusPdfDocument data,
            String field) {
        return importedNotes(data).path(field).asText("").trim();
    }

    private String topicMetadataText(
            SyllabusPdfDocument.TopicRow topic,
            String field) {
        if (topic == null || isBlank(topic.notes())) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(topic.notes());
            return root.path(field).asText("").trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String numberOrEmpty(Number value) {
        return value == null ? "" : number(value);
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
                responsiblePersons(data));

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
                "Course learning outcomes",
                data.clos().stream()
                        .map(clo -> safe(clo.code()) + ". "
                                + firstNonBlank(clo.description(), clo.descriptionVn()))
                        .collect(Collectors.joining("\n")));

        addGeneralRowIfPresent(
                table,
                "Content",
                data.topics().stream()
                        .map(topic -> safeNumber(topic.weekNumber()) + ". "
                                + firstNonBlank(topic.name(), topic.nameVn()))
                        .collect(Collectors.joining("\n")));

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
                "Reading list",
                java.util.stream.IntStream.range(0, data.books().size())
                        .mapToObj(index -> (index + 1) + ". " + citation(data.books().get(index)))
                        .collect(Collectors.joining("\n")));

        document.add(table);

        addCompetencyLevelSummary(document, data);
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

        addCompetencyLevelSummary(document, data);
    }

    /**
     * Matches the reference syllabus's "Competency level -> CLO" sub-table
     * (Knowledge/Skill/Attitude rows listing the CLO codes at that level).
     */
    private void addCompetencyLevelSummary(
            Document document,
            SyllabusPdfDocument data)
            throws DocumentException {

        Map<String, List<String>> cloCodesByLevel = new LinkedHashMap<>();
        cloCodesByLevel.put("KNOWLEDGE", new ArrayList<>());
        cloCodesByLevel.put("SKILL", new ArrayList<>());
        cloCodesByLevel.put("ATTITUDE", new ArrayList<>());

        for (SyllabusPdfDocument.CloRow clo : data.clos()) {
            String level = clo.competencyLevel();
            if (level != null && cloCodesByLevel.containsKey(level)) {
                cloCodesByLevel.get(level).add(clo.code());
            }
        }

        boolean anyAssigned = cloCodesByLevel.values().stream().anyMatch(codes -> !codes.isEmpty());
        if (!anyAssigned) {
            return;
        }

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{35, 65});
        table.setSpacingAfter(8);

        addHeaderCell(table, "Competency level");
        addHeaderCell(table, "Course learning outcome (CLO)");

        for (Map.Entry<String, List<String>> entry : cloCodesByLevel.entrySet()) {
            addBodyCell(table, titleCase(entry.getKey()), Element.ALIGN_LEFT);
            addBodyCell(
                    table,
                    entry.getValue().isEmpty() ? "-" : String.join(", ", entry.getValue()),
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
                        28,
                        8,
                        16,
                        24,
                        16
                });

        table.setHeaderRows(1);
        table.setSplitLate(true);
        table.setSplitRows(true);
        table.setSpacingAfter(8);

        addHeaderCell(
                table,
                "Week");

        addHeaderCell(
                table,
                "Topic");

        addHeaderCell(
                table,
                "CLO");

        addHeaderCell(
                table,
                "Assessments");

        addHeaderCell(
                table,
                "Learning activities");

        addHeaderCell(
                table,
                "Resources");

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
                    topic.assessments(),
                    Element.ALIGN_LEFT);

            addBodyCell(
                    table,
                    joinNonBlank(
                            ", ",
                            topic.teachingMethod(),
                            topic.learningActivity()),
                    Element.ALIGN_LEFT);

            addBodyCell(
                    table,
                    topic.resources(),
                    Element.ALIGN_LEFT);
        }

        document.add(table);
    }

    private String assessmentsForTopic(
            SyllabusPdfDocument.TopicRow topic,
            List<SyllabusPdfDocument.AssessmentRow> assessments) {
        if (topic.cloCodes() == null || topic.cloCodes().isEmpty()) {
            return "";
        }
        return assessments.stream()
                .filter(assessment -> assessment.cloContributions().stream()
                        .anyMatch(mapping -> topic.cloCodes().contains(mapping.cloCode())))
                .map(assessment -> firstNonBlank(assessment.name(), assessment.nameVn()))
                .distinct()
                .collect(Collectors.joining(", "));
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

        int columnCount = 1 + data.clos().size();
        PdfPTable table = new PdfPTable(columnCount);

        table.setWidthPercentage(100);
        float[] widths = new float[columnCount];
        widths[0] = 38;
        for (int index = 1; index < columnCount; index++) {
            widths[index] = 62f / Math.max(1, data.clos().size());
        }
        table.setWidths(widths);

        table.setHeaderRows(1);
        table.setSplitLate(false);
        table.setSplitRows(true);
        table.setSpacingAfter(5);

        addHeaderCell(
                table,
                "Assessment Type");
        for (SyllabusPdfDocument.CloRow clo : data.clos()) {
            addHeaderCell(table, clo.code());
        }

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
                    firstNonBlank(assessment.name(), assessment.nameVn())
                            + (assessment.weightPercent() == null
                            ? ""
                            : " (" + number(assessment.weightPercent()) + "%)"),
                    Element.ALIGN_LEFT);
            for (SyllabusPdfDocument.CloRow clo : data.clos()) {
                String contribution = assessment.cloContributions().stream()
                        .filter(item -> clo.code().equalsIgnoreCase(safe(item.cloCode())))
                        .map(item -> percent(item.contributionPercent()))
                        .findFirst()
                        .orElse("");
                addBodyCell(table, contribution, Element.ALIGN_CENTER);
            }
        }

        document.add(table);

        Paragraph summary =
                new Paragraph(
                        "Note: %Pass is the target percentage of students having scores greater than 50 out of 100. "
                                + "Total assessment weight: " + number(totalWeight) + "%.",
                        fonts.regular(8, TEXT));

        summary.setAlignment(Element.ALIGN_LEFT);
        summary.setSpacingAfter(8);

        document.add(summary);
    }

    private void addreadings(
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

    private void addRubrics(
            Document document,
            SyllabusPdfDocument data)
            throws DocumentException {

        addSectionTitle(document, "Rubrics (optional)");

        String raw = data.rubrics();
        if (raw == null || raw.isBlank()) {
            addEmptyState(document, "No grading rubric has been defined.");
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode rubrics = root.path("rubrics");
            if (!rubrics.isArray()) {
                addRubricLegacyText(document, raw);
                return;
            }

            int rubricNumber = 1;
            for (JsonNode rubric : rubrics) {
                String title = rubric.path("title").asText("Rubric " + rubricNumber);
                Paragraph heading = new Paragraph(
                        "5." + rubricNumber + ". " + title,
                        fonts.bold(9, NAVY));
                heading.setSpacingBefore(4);
                heading.setSpacingAfter(4);
                document.add(heading);

                JsonNode labels = rubric.path("scaleLabels");
                JsonNode criteria = rubric.path("criteria");
                int scaleCount = labels.isArray() ? labels.size() : 0;
                if (scaleCount == 0 || !criteria.isArray() || criteria.isEmpty()) {
                    addEmptyState(document, "No criteria have been defined for this rubric.");
                    rubricNumber++;
                    continue;
                }

                PdfPTable table = new PdfPTable(scaleCount + 1);
                table.setWidthPercentage(100);
                float[] widths = new float[scaleCount + 1];
                widths[0] = 30;
                for (int index = 1; index < widths.length; index++) {
                    widths[index] = 70f / scaleCount;
                }
                table.setWidths(widths);
                table.setHeaderRows(1);
                table.setSplitLate(false);
                table.setSplitRows(true);
                table.setSpacingAfter(8);

                addHeaderCell(table, "Criterion");
                for (JsonNode label : labels) {
                    addHeaderCell(table, label.asText("Level"));
                }

                for (JsonNode criterion : criteria) {
                    addBodyCell(table, criterion.path("criterion").asText(""), Element.ALIGN_LEFT);
                    JsonNode levels = criterion.path("levels");
                    for (int index = 0; index < scaleCount; index++) {
                        addBodyCell(
                                table,
                                levels.isArray() && index < levels.size()
                                        ? levels.get(index).asText("")
                                        : "",
                                Element.ALIGN_LEFT);
                    }
                }
                document.add(table);
                rubricNumber++;
            }
        } catch (Exception ignored) {
            addRubricLegacyText(document, raw);
        }
    }

    private void addRubricLegacyText(Document document, String raw)
            throws DocumentException {
        Paragraph paragraph = new Paragraph(raw, fonts.regular(8, TEXT));
        paragraph.setLeading(11);
        paragraph.setSpacingAfter(8);
        document.add(paragraph);
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
                "Revision and approval");

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
                        fonts.phrase(
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
                        fonts.phrase(
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
                        fonts.phrase(
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

        if (theory == null && lab == null) return "";

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

    private String responsiblePersons(SyllabusPdfDocument data) {
        if (!isBlank(data.responsiblePersons())
                && !"Not assigned".equalsIgnoreCase(data.responsiblePersons().trim())) {
            return data.responsiblePersons();
        }

        if (!isBlank(data.notes())) {
            try {
                String personResponsible = objectMapper
                        .readTree(data.notes())
                        .path("personResponsible")
                        .asText("")
                        .trim();
                if (!personResponsible.isBlank()) {
                    return personResponsible;
                }
            } catch (Exception ignored) {
                // Legacy notes can be plain text; keep the assignment fallback.
            }
        }

        return "";
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
