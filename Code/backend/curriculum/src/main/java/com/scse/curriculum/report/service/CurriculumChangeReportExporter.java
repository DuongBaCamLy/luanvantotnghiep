package com.scse.curriculum.report.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.scse.curriculum.report.dto.CurriculumChangeReportData;
import com.scse.curriculum.syllabus.pdf.SyllabusPdfFontProvider;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

public final class CurriculumChangeReportExporter {
    private CurriculumChangeReportExporter() {}

    public static byte[] excel(CurriculumChangeReportData data) {
        validate(data);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet summary = workbook.createSheet("Summary");
            Sheet changes = workbook.createSheet("Course Changes");
            Sheet metadata = workbook.createSheet("Metadata Changes");
            Styles styles = styles(workbook);
            writeSummary(summary, data, styles);
            writeCourseChanges(changes, data.getCourseChanges(), styles);
            writeMetadata(metadata, data.getMetadataChanges(), styles);
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể xuất Curriculum Change Report ra Excel.", exception);
        }
    }

    public static byte[] pdf(CurriculumChangeReportData data, SyllabusPdfFontProvider fonts) {
        validate(data);
        if (fonts == null || !fonts.isUnicodeReady()) {
            throw new IllegalStateException("Không tìm thấy font Unicode nhúng được để xuất PDF tiếng Việt.");
        }
        Document document = new Document(PageSize.A3.rotate(), 20, 20, 24, 24);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, out);
            document.open();
            var title = fonts.bold(15);
            var header = fonts.bold(7);
            var body = fonts.regular(6.5f);
            document.add(new Paragraph("CURRICULUM CHANGE REPORT GIỮA HAI COHORT", title));
            document.add(new Paragraph("Chương trình: " + preferred(data.getProgramNameVn(), data.getProgramName())
                    + " (" + safe(data.getProgramCode()) + ") | "
                    + data.getOldCohort().getName() + " → " + data.getNewCohort().getName(), body));
            document.add(new Paragraph("Scope: " + data.getScopeKey(), body));
            document.add(new Paragraph(" "));

            PdfPTable summary = new PdfPTable(new float[]{2.8f, 1.4f, 1.4f, 1.4f, 1.4f, 1.4f, 1.6f});
            summary.setWidthPercentage(100);
            String[] sh = {"Phạm vi", "Môn cũ", "Môn mới", "Thêm", "Bớt", "Sửa", "Chênh lệch TC"};
            for (String h : sh) cell(summary, h, header, true);
            cell(summary, data.getOldCohort().getName() + " → " + data.getNewCohort().getName(), body, false);
            cell(summary, data.getSummary().getOldCourseCount(), body, false);
            cell(summary, data.getSummary().getNewCourseCount(), body, false);
            cell(summary, data.getSummary().getAddedCourses(), body, false);
            cell(summary, data.getSummary().getRemovedCourses(), body, false);
            cell(summary, data.getSummary().getModifiedCourses(), body, false);
            cell(summary, data.getSummary().getCreditDifference(), body, false);
            document.add(summary);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{1.3f, 1.8f, 3.8f, 2.1f, 1.4f, 1.4f, 1.4f, 1.8f, 4.6f});
            table.setWidthPercentage(100); table.setHeaderRows(1); table.setSplitRows(true); table.setSplitLate(false);
            String[] headers = {"Loại", "Mã môn", "Tên học phần", "Nhóm môn", "TC cũ", "TC mới", "HK cũ", "HK mới", "Chi tiết thay đổi"};
            for (String h : headers) cell(table, h, header, true);
            for (var change : data.getCourseChanges()) {
                cell(table, change.getChangeType(), body, false);
                cell(table, change.getCourseCode(), body, false);
                cell(table, preferred(change.getCourseNameVn(), change.getCourseName()), body, false);
                cell(table, preferred(snapshotGroup(change.getNewValue()), snapshotGroup(change.getOldValue())), body, false);
                cell(table, snapshotCredits(change.getOldValue()), body, false);
                cell(table, snapshotCredits(change.getNewValue()), body, false);
                cell(table, snapshotSemester(change.getOldValue()), body, false);
                cell(table, snapshotSemester(change.getNewValue()), body, false);
                cell(table, details(change.getChanges()), body, false);
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception exception) {
            if (document.isOpen()) document.close();
            throw new IllegalStateException("Không thể xuất Curriculum Change Report ra PDF.", exception);
        }
    }

    private static void writeSummary(Sheet sheet, CurriculumChangeReportData data, Styles styles) {
        Row title = sheet.createRow(0); set(title, 0, "CURRICULUM CHANGE REPORT GIỮA HAI COHORT", styles.title());
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        meta(sheet, 2, "Chương trình", safe(data.getProgramCode()) + " – " + preferred(data.getProgramNameVn(), data.getProgramName()), styles);
        meta(sheet, 3, "Cohort cũ", data.getOldCohort().getName(), styles);
        meta(sheet, 4, "Cohort mới", data.getNewCohort().getName(), styles);
        meta(sheet, 5, "Scope key", data.getScopeKey(), styles);
        meta(sheet, 7, "Số môn cohort cũ", data.getSummary().getOldCourseCount(), styles);
        meta(sheet, 8, "Số môn cohort mới", data.getSummary().getNewCourseCount(), styles);
        meta(sheet, 9, "Môn thêm", data.getSummary().getAddedCourses(), styles);
        meta(sheet, 10, "Môn bớt", data.getSummary().getRemovedCourses(), styles);
        meta(sheet, 11, "Môn thay đổi", data.getSummary().getModifiedCourses(), styles);
        meta(sheet, 12, "Môn không đổi", data.getSummary().getUnchangedCourses(), styles);
        meta(sheet, 13, "Tổng tín chỉ cũ", data.getSummary().getOldTotalCredits(), styles);
        meta(sheet, 14, "Tổng tín chỉ mới", data.getSummary().getNewTotalCredits(), styles);
        meta(sheet, 15, "Chênh lệch tín chỉ", data.getSummary().getCreditDifference(), styles);
        meta(sheet, 16, "Metadata thay đổi", data.getSummary().getMetadataChangeCount(), styles);
        sheet.setColumnWidth(0, 28 * 256); sheet.setColumnWidth(1, 70 * 256);
    }

    private static void writeCourseChanges(Sheet sheet, List<CurriculumChangeReportData.CourseChange> rows, Styles styles) {
        String[] headers = {"STT", "Loại", "Mã môn", "Tên học phần", "Nhóm môn cũ", "Nhóm môn mới",
                "TC LT cũ", "TC TH cũ", "Tổng TC cũ", "TC LT mới", "TC TH mới", "Tổng TC mới",
                "HK cũ", "HK mới", "Năm cũ", "Năm mới", "Bắt buộc cũ", "Bắt buộc mới", "Chi tiết thay đổi"};
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) set(header, i, headers[i], styles.header());
        int index = 1; int ordinal = 1;
        for (var item : rows) {
            Row row = sheet.createRow(index++); int c = 0;
            set(row, c++, ordinal++, styles.center()); set(row, c++, item.getChangeType(), styles.body());
            set(row, c++, item.getCourseCode(), styles.body()); set(row, c++, preferred(item.getCourseNameVn(), item.getCourseName()), styles.body());
            set(row, c++, snapshotGroup(item.getOldValue()), styles.body()); set(row, c++, snapshotGroup(item.getNewValue()), styles.body());
            set(row, c++, snapshot(item.getOldValue(), "theory"), styles.center()); set(row, c++, snapshot(item.getOldValue(), "lab"), styles.center());
            set(row, c++, snapshotCredits(item.getOldValue()), styles.center()); set(row, c++, snapshot(item.getNewValue(), "theory"), styles.center());
            set(row, c++, snapshot(item.getNewValue(), "lab"), styles.center()); set(row, c++, snapshotCredits(item.getNewValue()), styles.center());
            set(row, c++, snapshotSemester(item.getOldValue()), styles.center()); set(row, c++, snapshotSemester(item.getNewValue()), styles.center());
            set(row, c++, snapshot(item.getOldValue(), "year"), styles.center()); set(row, c++, snapshot(item.getNewValue(), "year"), styles.center());
            set(row, c++, snapshot(item.getOldValue(), "required"), styles.center()); set(row, c++, snapshot(item.getNewValue(), "required"), styles.center());
            set(row, c, details(item.getChanges()), styles.body());
        }
        sheet.createFreezePane(4, 1); sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, index - 1), 0, headers.length - 1));
        int[] widths = {7, 12, 15, 34, 22, 22, 10, 10, 12, 10, 10, 12, 10, 10, 10, 10, 12, 12, 55};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);
    }

    private static void writeMetadata(Sheet sheet, List<CurriculumChangeReportData.MetadataChange> rows, Styles styles) {
        String[] headers = {"STT", "Metadata", "Giá trị cũ", "Giá trị mới"};
        Row header = sheet.createRow(0); for (int i = 0; i < headers.length; i++) set(header, i, headers[i], styles.header());
        int index = 1; int ordinal = 1;
        for (var item : rows) { Row row = sheet.createRow(index++); set(row, 0, ordinal++, styles.center()); set(row, 1, item.getLabel(), styles.body()); set(row, 2, item.getOldValue(), styles.body()); set(row, 3, item.getNewValue(), styles.body()); }
        sheet.setColumnWidth(0, 7 * 256); sheet.setColumnWidth(1, 28 * 256); sheet.setColumnWidth(2, 45 * 256); sheet.setColumnWidth(3, 45 * 256);
    }

    private static Styles styles(XSSFWorkbook workbook) {
        CellStyle title = workbook.createCellStyle(); var titleFont = workbook.createFont(); titleFont.setBold(true); titleFont.setFontHeightInPoints((short) 15); title.setFont(titleFont);
        CellStyle header = workbook.createCellStyle(); var headerFont = workbook.createFont(); headerFont.setBold(true); headerFont.setColor(IndexedColors.WHITE.getIndex()); header.setFont(headerFont); header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex()); header.setFillPattern(FillPatternType.SOLID_FOREGROUND); header.setAlignment(HorizontalAlignment.CENTER); header.setVerticalAlignment(VerticalAlignment.CENTER); header.setWrapText(true);
        CellStyle body = workbook.createCellStyle(); body.setWrapText(true); body.setVerticalAlignment(VerticalAlignment.TOP);
        CellStyle center = workbook.createCellStyle(); center.cloneStyleFrom(body); center.setAlignment(HorizontalAlignment.CENTER);
        return new Styles(title, header, body, center);
    }

    private static void meta(Sheet sheet, int rowIndex, String label, Object value, Styles styles) { Row row = sheet.createRow(rowIndex); set(row, 0, label, styles.header()); set(row, 1, value, styles.body()); }
    private static void set(Row row, int column, Object value, CellStyle style) { Cell cell = row.createCell(column); if (value instanceof Number number) cell.setCellValue(number.doubleValue()); else cell.setCellValue(value == null ? "" : String.valueOf(value)); cell.setCellStyle(style); }
    private static void cell(PdfPTable table, Object value, com.lowagie.text.Font font, boolean header) { PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : String.valueOf(value), font)); cell.setPadding(3); cell.setVerticalAlignment(Element.ALIGN_MIDDLE); if (header) { cell.setBackgroundColor(new Color(219, 234, 254)); cell.setHorizontalAlignment(Element.ALIGN_CENTER); } table.addCell(cell); }
    private static String details(List<CurriculumChangeReportData.FieldChange> changes) { if (changes == null || changes.isEmpty()) return ""; return changes.stream().map(c -> c.getLabel() + ": " + safe(c.getOldValue()) + " → " + safe(c.getNewValue())).collect(java.util.stream.Collectors.joining("; ")); }
    private static String snapshotGroup(CurriculumChangeReportData.CourseSnapshot value) { return value == null ? "" : preferred(value.getCourseTypeNameVn(), value.getCourseTypeName()); }
    private static Object snapshotCredits(CurriculumChangeReportData.CourseSnapshot value) { return value == null ? "" : value.getTotalCredits(); }
    private static Object snapshotSemester(CurriculumChangeReportData.CourseSnapshot value) { return value == null ? "" : value.getSemesterSuggest(); }
    private static Object snapshot(CurriculumChangeReportData.CourseSnapshot value, String field) { if (value == null) return ""; return switch (field) { case "theory" -> value.getCreditTheory(); case "lab" -> value.getCreditLab(); case "year" -> value.getYearSuggest(); case "required" -> value.getRequired(); default -> ""; }; }
    private static String preferred(String first, String second) { return first != null && !first.isBlank() ? first : safe(second); }
    private static String safe(Object value) { return value == null ? "" : String.valueOf(value); }
    private static void validate(CurriculumChangeReportData data) { if (data == null || data.getProgramId() == null || data.getOldCohort() == null || data.getNewCohort() == null || data.getSummary() == null) throw new IllegalArgumentException("Dữ liệu Curriculum Change Report không đầy đủ."); }
    private record Styles(CellStyle title, CellStyle header, CellStyle body, CellStyle center) {}
}
