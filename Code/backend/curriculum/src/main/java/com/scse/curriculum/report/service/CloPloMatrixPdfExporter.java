package com.scse.curriculum.report.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.scse.curriculum.dashboard.dto.DashboardHeatmapResponse;
import com.scse.curriculum.syllabus.pdf.SyllabusPdfFontProvider;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * PDF exporter for the scoped CLO-PLO matrix.
 *
 * <p>The exporter consumes the canonical heatmap response only. It does not query
 * persistence, so the PDF scope is identical to the heatmap and Excel exports.</p>
 *
 * <p>Wide matrices are rendered in horizontal PLO chunks. Every chunk repeats the
 * fixed course columns and table header, preventing columns from being clipped or
 * reduced to unreadable widths.</p>
 */
final class CloPloMatrixPdfExporter {

    static final int MAX_PLO_COLUMNS_PER_SECTION = 8;
    private static final int FIXED_COLUMNS = 4;

    private static final Color TITLE_COLOR = new Color(15, 46, 78);
    private static final Color HEADER_BACKGROUND = new Color(219, 234, 254);
    private static final Color SUBHEADER_BACKGROUND = new Color(239, 246, 255);
    private static final Color BORDER_COLOR = new Color(148, 163, 184);

    private CloPloMatrixPdfExporter() {
    }

    static byte[] export(
            DashboardHeatmapResponse matrix,
            SyllabusPdfFontProvider fonts) {
        validate(matrix, fonts);

        List<DashboardHeatmapResponse.PloColumn> plos = uniquePlos(matrix.getPloDetails());
        List<DashboardHeatmapResponse.CourseCoverage> courses = uniqueCourses(matrix.getCourseCoverages());
        List<List<DashboardHeatmapResponse.PloColumn>> sections = partition(plos, MAX_PLO_COLUMNS_PER_SECTION);
        if (sections.isEmpty()) {
            sections = List.of(List.of());
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Rectangle pageSize = PageSize.A4.rotate();
            Document document = new Document(pageSize, 22, 22, 24, 24);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setViewerPreferences(PdfWriter.PageLayoutOneColumn);
            document.addTitle("Ma trận CLO-PLO toàn chương trình đào tạo");
            document.addAuthor("SCSE Curriculum Management System");
            document.addSubject(safe(matrix.getScopeKey()));
            document.addKeywords("CLO,PLO,curriculum,coverage,heatmap");
            document.open();

            Font titleFont = fonts.bold(14, TITLE_COLOR);
            Font headingFont = fonts.bold(9);
            Font bodyFont = fonts.regular(8);
            Font smallFont = fonts.regular(7);
            Font headerFont = fonts.bold(7);

            writeDocumentHeader(document, matrix, titleFont, headingFont, bodyFont);

            for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
                if (sectionIndex > 0) {
                    document.newPage();
                    writeContinuationHeader(
                            document,
                            matrix,
                            sectionIndex + 1,
                            sections.size(),
                            headingFont,
                            smallFont);
                }

                List<DashboardHeatmapResponse.PloColumn> sectionPlos = sections.get(sectionIndex);
                PdfPTable table = createTable(sectionPlos.size());
                writeTableHeader(table, sectionPlos, headerFont);
                writeCourseRows(table, courses, sectionPlos, bodyFont);

                if (courses.isEmpty()) {
                    PdfPCell empty = createCell(
                            "Không có học phần trong phạm vi đã chọn.",
                            bodyFont,
                            Element.ALIGN_CENTER,
                            Color.WHITE);
                    empty.setColspan(FIXED_COLUMNS + sectionPlos.size());
                    empty.setPadding(10);
                    table.addCell(empty);
                }

                document.add(table);

                Paragraph sectionNote = new Paragraph(
                        sectionLabel(sectionIndex, sections.size(), sectionPlos),
                        smallFont);
                sectionNote.setSpacingBefore(4);
                sectionNote.setSpacingAfter(3);
                document.add(sectionNote);
            }

            Paragraph legend = new Paragraph(
                    "Chú thích: I – Introduce; D – Develop; A – Apply.",
                    bodyFont);
            legend.setSpacingBefore(6);
            document.add(legend);

            document.close();
            return out.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Không thể xuất ma trận CLO–PLO ra PDF.",
                    exception);
        }
    }

    private static void validate(
            DashboardHeatmapResponse matrix,
            SyllabusPdfFontProvider fonts) {
        if (matrix == null) {
            throw new IllegalArgumentException("Dữ liệu ma trận CLO–PLO không được để trống.");
        }
        if (matrix.getProgramId() == null) {
            throw new IllegalArgumentException("programId của ma trận là bắt buộc.");
        }
        if (matrix.getCohortId() == null) {
            throw new IllegalArgumentException("cohortId của ma trận là bắt buộc.");
        }
        if (isBlank(matrix.getAcademicYear())) {
            throw new IllegalArgumentException("academicYear của ma trận là bắt buộc.");
        }
        if (isBlank(matrix.getSemester())) {
            throw new IllegalArgumentException("semester của ma trận là bắt buộc.");
        }
        if (fonts == null) {
            throw new IllegalArgumentException("Font provider của PDF là bắt buộc.");
        }
        if (!fonts.isUnicodeReady()) {
            throw new IllegalStateException(
                    "Không tìm thấy font Unicode nhúng được. "
                            + "Hãy cấu hình app.pdf.font.regular và app.pdf.font.bold.");
        }
    }

    private static void writeDocumentHeader(
            Document document,
            DashboardHeatmapResponse matrix,
            Font titleFont,
            Font headingFont,
            Font bodyFont) throws Exception {
        Paragraph title = new Paragraph(
                "MA TRẬN CLO–PLO TOÀN CHƯƠNG TRÌNH ĐÀO TẠO",
                titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(8);
        document.add(title);

        PdfPTable metadata = new PdfPTable(2);
        metadata.setWidthPercentage(100);
        metadata.setWidths(new float[]{1.25f, 5.75f});
        metadata.setSpacingAfter(8);

        addMetadata(metadata, "Chương trình", joinCodeName(
                matrix.getProgramCode(),
                preferred(matrix.getProgramNameVn(), matrix.getProgramName())), headingFont, bodyFont);
        addMetadata(metadata, "Khóa tuyển sinh", preferred(
                matrix.getCohortName(), safe(matrix.getCohortEntryYear())), headingFont, bodyFont);
        addMetadata(metadata, "Năm học / Học kỳ", safe(matrix.getAcademicYear())
                + " / Học kỳ " + safe(matrix.getSemester()), headingFont, bodyFont);
        addMetadata(metadata, "Nhóm môn", preferred(
                matrix.getCourseTypeNameVn(),
                preferred(matrix.getCourseTypeName(), "Tất cả")), headingFont, bodyFont);
        addMetadata(metadata, "Khóa phạm vi", safe(matrix.getScopeKey()), headingFont, bodyFont);
        addMetadata(metadata, "Nguồn dữ liệu", safe(matrix.getDataSource()), headingFont, bodyFont);

        DashboardHeatmapResponse.HeatmapSummary summary = matrix.getSummary();
        if (summary != null) {
            addMetadata(metadata, "Tóm tắt",
                    "PLO coverage " + formatPercent(summary.getPloCoveragePercentage())
                            + "; CLO mapping " + formatPercent(summary.getCloMappingPercentage())
                            + "; đề cương APPROVED " + safe(summary.getCoursesWithApprovedSyllabus())
                            + "/" + safe(summary.getTotalCourses()),
                    headingFont,
                    bodyFont);
        }
        document.add(metadata);
    }

    private static void writeContinuationHeader(
            Document document,
            DashboardHeatmapResponse matrix,
            int section,
            int totalSections,
            Font headingFont,
            Font bodyFont) throws Exception {
        Paragraph continuation = new Paragraph(
                "MA TRẬN CLO–PLO – phần " + section + "/" + totalSections,
                headingFont);
        continuation.setSpacingAfter(3);
        document.add(continuation);

        Paragraph scope = new Paragraph(
                preferred(matrix.getCohortName(), "N/A")
                        + " | " + safe(matrix.getAcademicYear())
                        + " | Học kỳ " + safe(matrix.getSemester())
                        + " | " + preferred(
                        matrix.getCourseTypeNameVn(),
                        preferred(matrix.getCourseTypeName(), "Tất cả")),
                bodyFont);
        scope.setSpacingAfter(6);
        document.add(scope);
    }

    private static PdfPTable createTable(int ploCount) throws Exception {
        int columns = FIXED_COLUMNS + ploCount;
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSplitRows(true);
        table.setSplitLate(false);
        table.setKeepTogether(false);

        float[] widths = new float[columns];
        widths[0] = 1.20f; // code
        widths[1] = 3.65f; // name
        widths[2] = 1.35f; // group
        widths[3] = 1.00f; // version
        for (int i = FIXED_COLUMNS; i < columns; i++) {
            widths[i] = 0.72f;
        }
        table.setWidths(widths);
        return table;
    }

    private static void writeTableHeader(
            PdfPTable table,
            List<DashboardHeatmapResponse.PloColumn> plos,
            Font font) {
        addHeader(table, "Mã môn", font);
        addHeader(table, "Tên học phần", font);
        addHeader(table, "Nhóm môn", font);
        addHeader(table, "Phiên bản", font);
        for (DashboardHeatmapResponse.PloColumn plo : plos) {
            addHeader(table, preferred(plo.getCode(), "PLO"), font);
        }
    }

    private static void writeCourseRows(
            PdfPTable table,
            List<DashboardHeatmapResponse.CourseCoverage> courses,
            List<DashboardHeatmapResponse.PloColumn> plos,
            Font font) {
        boolean alternate = false;
        for (DashboardHeatmapResponse.CourseCoverage course : courses) {
            Color background = alternate ? new Color(248, 250, 252) : Color.WHITE;
            alternate = !alternate;

            addBody(table, course.getCourseCode(), font, Element.ALIGN_LEFT, background);
            addBody(table, preferred(course.getCourseNameVn(), course.getCourseName()), font,
                    Element.ALIGN_LEFT, background);
            addBody(table, preferred(course.getCourseTypeNameVn(),
                    preferred(course.getCourseTypeName(), "Chưa phân nhóm")), font,
                    Element.ALIGN_LEFT, background);
            addBody(table, course.getSyllabusVersionLabel(), font,
                    Element.ALIGN_CENTER, background);

            Map<String, DashboardHeatmapResponse.CellCoverage> cells = indexCells(course.getCells());
            for (DashboardHeatmapResponse.PloColumn plo : plos) {
                DashboardHeatmapResponse.CellCoverage coverage = cells.get(ploKey(plo));
                addBody(table,
                        normalizeLevel(coverage == null ? null : coverage.getLevel()),
                        font,
                        Element.ALIGN_CENTER,
                        background);
            }
        }
    }

    private static void addMetadata(
            PdfPTable table,
            String label,
            String value,
            Font labelFont,
            Font valueFont) {
        PdfPCell labelCell = createCell(label, labelFont, Element.ALIGN_LEFT, SUBHEADER_BACKGROUND);
        labelCell.setPadding(4);
        table.addCell(labelCell);

        PdfPCell valueCell = createCell(value, valueFont, Element.ALIGN_LEFT, Color.WHITE);
        valueCell.setPadding(4);
        table.addCell(valueCell);
    }

    private static void addHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = createCell(text, font, Element.ALIGN_CENTER, HEADER_BACKGROUND);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private static void addBody(
            PdfPTable table,
            String text,
            Font font,
            int alignment,
            Color background) {
        PdfPCell cell = createCell(text, font, alignment, background);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3.5f);
        table.addCell(cell);
    }

    private static PdfPCell createCell(
            String text,
            Font font,
            int alignment,
            Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(safe(text), font));
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(background);
        cell.setBorderColor(BORDER_COLOR);
        cell.setBorderWidth(0.5f);
        return cell;
    }

    private static List<DashboardHeatmapResponse.CourseCoverage> uniqueCourses(
            List<DashboardHeatmapResponse.CourseCoverage> courses) {
        Map<String, DashboardHeatmapResponse.CourseCoverage> unique = new LinkedHashMap<>();
        if (courses != null) {
            for (DashboardHeatmapResponse.CourseCoverage course : courses) {
                if (course == null) {
                    continue;
                }
                String key = courseKey(course);
                if (key != null) {
                    unique.putIfAbsent(key, course);
                }
            }
        }
        return new ArrayList<>(unique.values());
    }

    private static List<DashboardHeatmapResponse.PloColumn> uniquePlos(
            List<DashboardHeatmapResponse.PloColumn> plos) {
        Map<String, DashboardHeatmapResponse.PloColumn> unique = new LinkedHashMap<>();
        if (plos != null) {
            for (DashboardHeatmapResponse.PloColumn plo : plos) {
                if (plo != null) {
                    unique.putIfAbsent(ploKey(plo), plo);
                }
            }
        }
        return new ArrayList<>(unique.values());
    }

    private static List<List<DashboardHeatmapResponse.PloColumn>> partition(
            List<DashboardHeatmapResponse.PloColumn> values,
            int size) {
        List<List<DashboardHeatmapResponse.PloColumn>> result = new ArrayList<>();
        for (int start = 0; start < values.size(); start += size) {
            result.add(new ArrayList<>(values.subList(start, Math.min(start + size, values.size()))));
        }
        return result;
    }

    private static Map<String, DashboardHeatmapResponse.CellCoverage> indexCells(
            List<DashboardHeatmapResponse.CellCoverage> cells) {
        Map<String, DashboardHeatmapResponse.CellCoverage> result = new LinkedHashMap<>();
        if (cells != null) {
            for (DashboardHeatmapResponse.CellCoverage cell : cells) {
                if (cell == null) {
                    continue;
                }
                String key = cell.getPloId() != null
                        ? "ID:" + cell.getPloId()
                        : "CODE:" + normalize(cell.getPloCode());
                result.putIfAbsent(key, cell);
            }
        }
        return result;
    }

    private static String courseKey(DashboardHeatmapResponse.CourseCoverage course) {
        if (course.getCourseId() != null) {
            return "ID:" + course.getCourseId();
        }
        String code = normalize(course.getCourseCode());
        return code.isBlank() ? null : "CODE:" + code;
    }

    private static String ploKey(DashboardHeatmapResponse.PloColumn plo) {
        if (plo.getId() != null) {
            return "ID:" + plo.getId();
        }
        return "CODE:" + normalize(plo.getCode());
    }

    private static String normalizeLevel(String level) {
        String normalized = normalize(level);
        return switch (normalized) {
            case "I", "D", "A" -> normalized;
            default -> "";
        };
    }

    private static String sectionLabel(
            int sectionIndex,
            int totalSections,
            List<DashboardHeatmapResponse.PloColumn> plos) {
        if (plos.isEmpty()) {
            return "Không có cột PLO trong phạm vi đã chọn.";
        }
        return "Phần " + (sectionIndex + 1) + "/" + totalSections
                + ": " + preferred(plos.get(0).getCode(), "PLO")
                + " đến " + preferred(plos.get(plos.size() - 1).getCode(), "PLO");
    }

    private static String formatPercent(Double value) {
        if (value == null) {
            return "0%";
        }
        return value % 1 == 0
                ? String.format(Locale.ROOT, "%.0f%%", value)
                : String.format(Locale.ROOT, "%.2f%%", value);
    }

    private static String joinCodeName(String code, String name) {
        if (isBlank(code)) {
            return safe(name);
        }
        if (isBlank(name)) {
            return safe(code);
        }
        return code.trim() + " – " + name.trim();
    }

    private static String preferred(String primary, String fallback) {
        return isBlank(primary) ? safe(fallback) : primary.trim();
    }

    private static String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
