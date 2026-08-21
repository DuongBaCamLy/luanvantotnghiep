package com.scse.curriculum.report.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.scse.curriculum.report.dto.PloCoverageReportData;
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
import java.util.Locale;

final class PloCoverageReportExporter {

    static final String SUMMARY_SHEET = "PLO Coverage";
    static final String CONTRIBUTION_SHEET = "CLO-Course Contributions";
    static final int SUMMARY_HEADER_ROW = 12;
    static final int SUMMARY_DATA_ROW = SUMMARY_HEADER_ROW + 1;

    private PloCoverageReportExporter() {
    }

    static byte[] excel(PloCoverageReportData data) {
        validate(data);
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ExcelStyles styles = createStyles(workbook);
            Sheet summarySheet = workbook.createSheet(SUMMARY_SHEET);
            writeSummaryMetadata(summarySheet, data, styles);
            writePloSummary(summarySheet, data, styles);
            configureSummarySheet(summarySheet, data.getPlos().size());

            Sheet contributionSheet = workbook.createSheet(CONTRIBUTION_SHEET);
            writeContributionSheet(contributionSheet, data, styles);
            configureContributionSheet(contributionSheet);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể xuất PLO Coverage Report ra Excel.", exception);
        }
    }

    static byte[] pdf(PloCoverageReportData data, SyllabusPdfFontProvider fonts) {
        validate(data);
        if (fonts == null) {
            throw new IllegalArgumentException("SyllabusPdfFontProvider không được để trống.");
        }
        if (!fonts.isUnicodeReady()) {
            throw new IllegalStateException("Không tìm thấy font Unicode nhúng được để xuất PDF tiếng Việt.");
        }

        Document document = new Document(PageSize.A3.rotate(), 22, 22, 26, 26);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titleFont = fonts.bold(15);
            com.lowagie.text.Font sectionFont = fonts.bold(10);
            com.lowagie.text.Font headerFont = fonts.bold(7);
            com.lowagie.text.Font bodyFont = fonts.regular(6.5f);

            document.add(new Paragraph("PLO COVERAGE REPORT", titleFont));
            document.add(new Paragraph(scopeText(data), bodyFont));
            document.add(new Paragraph(summaryText(data), bodyFont));
            document.add(new Paragraph(" "));

            PdfPTable summaryTable = new PdfPTable(new float[]{1.3f, 3.8f, 1.2f, 1.5f, 1.4f, 1.2f, 1.2f, 1.2f});
            summaryTable.setWidthPercentage(100);
            summaryTable.setHeaderRows(1);
            summaryTable.setSplitRows(true);
            summaryTable.setSplitLate(false);
            for (String header : new String[]{"PLO", "Mô tả", "Trạng thái", "Số môn", "Số CLO", "I", "D", "A"}) {
                pdfCell(summaryTable, header, headerFont, true, Element.ALIGN_CENTER);
            }
            for (PloCoverageReportData.PloCoverageRow row : data.getPlos()) {
                pdfCell(summaryTable, row.getPloCode(), bodyFont, false, Element.ALIGN_CENTER);
                pdfCell(summaryTable, preferred(row.getDescriptionVn(), row.getDescription()), bodyFont, false, Element.ALIGN_LEFT);
                pdfCell(summaryTable, Boolean.TRUE.equals(row.getCovered()) ? "Covered" : "Chưa cover", bodyFont, false, Element.ALIGN_CENTER);
                pdfCell(summaryTable, String.valueOf(row.getContributingCourseCount()), bodyFont, false, Element.ALIGN_CENTER);
                pdfCell(summaryTable, String.valueOf(row.getContributingCloCount()), bodyFont, false, Element.ALIGN_CENTER);
                pdfCell(summaryTable, String.valueOf(row.getIntroductionCount()), bodyFont, false, Element.ALIGN_CENTER);
                pdfCell(summaryTable, String.valueOf(row.getDevelopmentCount()), bodyFont, false, Element.ALIGN_CENTER);
                pdfCell(summaryTable, String.valueOf(row.getAchievementCount()), bodyFont, false, Element.ALIGN_CENTER);
            }
            document.add(summaryTable);

            document.newPage();
            document.add(new Paragraph("CLO/MÔN HỌC ĐÓNG GÓP THEO TỪNG PLO", sectionFont));
            document.add(new Paragraph(" "));
            for (PloCoverageReportData.PloCoverageRow plo : data.getPlos()) {
                document.add(new Paragraph(
                        safe(plo.getPloCode()) + " – " + preferred(plo.getDescriptionVn(), plo.getDescription()),
                        sectionFont));
                if (plo.getCourses().isEmpty()) {
                    document.add(new Paragraph("Chưa có CLO hoặc môn học đóng góp.", bodyFont));
                    document.add(new Paragraph(" "));
                    continue;
                }
                PdfPTable detail = new PdfPTable(new float[]{1.7f, 4f, 2.4f, 1.2f, 1.5f, 4.5f});
                detail.setWidthPercentage(100);
                detail.setHeaderRows(1);
                detail.setSplitRows(true);
                detail.setSplitLate(false);
                for (String header : new String[]{"Mã môn", "Tên học phần", "Nhóm môn", "Mức", "Số mapping", "CLO đóng góp"}) {
                    pdfCell(detail, header, headerFont, true, Element.ALIGN_CENTER);
                }
                for (PloCoverageReportData.CourseContribution course : plo.getCourses()) {
                    pdfCell(detail, course.getCourseCode(), bodyFont, false, Element.ALIGN_CENTER);
                    pdfCell(detail, preferred(course.getCourseNameVn(), course.getCourseName()), bodyFont, false, Element.ALIGN_LEFT);
                    pdfCell(detail, preferred(course.getCourseTypeNameVn(), course.getCourseTypeName()), bodyFont, false, Element.ALIGN_LEFT);
                    pdfCell(detail, course.getLevel(), bodyFont, false, Element.ALIGN_CENTER);
                    pdfCell(detail, String.valueOf(course.getMappingCount()), bodyFont, false, Element.ALIGN_CENTER);
                    pdfCell(detail, String.join(", ", course.getCloCodes()), bodyFont, false, Element.ALIGN_LEFT);
                }
                document.add(detail);
                document.add(new Paragraph(" "));
            }

            document.close();
            return out.toByteArray();
        } catch (Exception exception) {
            if (document.isOpen()) {
                document.close();
            }
            throw new IllegalStateException("Không thể xuất PLO Coverage Report ra PDF.", exception);
        }
    }

    private static void writeSummaryMetadata(
            Sheet sheet,
            PloCoverageReportData data,
            ExcelStyles styles) {
        Row title = sheet.createRow(0);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue("PLO COVERAGE REPORT");
        titleCell.setCellStyle(styles.title());
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

        writeMeta(sheet, 1, "Chương trình", joinCodeName(data.getProgramCode(), preferred(data.getProgramNameVn(), data.getProgramName())), styles);
        writeMeta(sheet, 2, "Cohort", data.getCohortName(), styles);
        writeMeta(sheet, 3, "Năm học", data.getAcademicYear(), styles);
        writeMeta(sheet, 4, "Học kỳ", data.getSemester(), styles);
        writeMeta(sheet, 5, "Nhóm môn", preferred(data.getCourseTypeNameVn(), preferred(data.getCourseTypeName(), "Tất cả")), styles);
        writeMeta(sheet, 6, "Scope key", data.getScopeKey(), styles);
        writeMeta(sheet, 7, "Tổng PLO", data.getSummary().getTotalPlos(), styles);
        writeMeta(sheet, 8, "PLO đã cover", data.getSummary().getCoveredPlos(), styles);
        writeMeta(sheet, 9, "PLO chưa cover", data.getSummary().getUncoveredPlos(), styles);
        writeMeta(sheet, 10, "Coverage", asFraction(data.getSummary().getCoveragePercentage()), styles, true);
        writeMeta(sheet, 11, "CLO mapped / tổng CLO",
                data.getSummary().getMappedClos() + " / " + data.getSummary().getTotalClos(), styles);
    }

    private static void writePloSummary(Sheet sheet, PloCoverageReportData data, ExcelStyles styles) {
        Row header = sheet.createRow(SUMMARY_HEADER_ROW);
        String[] headers = {"STT", "Mã PLO", "Mô tả", "Danh mục", "Trạng thái", "Số môn đóng góp", "Số CLO đóng góp", "I", "D", "A", "Các môn đóng góp"};
        for (int index = 0; index < headers.length; index++) {
            writeCell(header, index, headers[index], styles.header());
        }

        int rowIndex = SUMMARY_DATA_ROW;
        int ordinal = 1;
        for (PloCoverageReportData.PloCoverageRow plo : data.getPlos()) {
            Row row = sheet.createRow(rowIndex++);
            writeNumber(row, 0, ordinal++, styles.integer());
            writeCell(row, 1, plo.getPloCode(), styles.center());
            writeCell(row, 2, preferred(plo.getDescriptionVn(), plo.getDescription()), styles.body());
            writeCell(row, 3, plo.getCategory(), styles.body());
            writeCell(row, 4, Boolean.TRUE.equals(plo.getCovered()) ? "Covered" : "Chưa cover", styles.center());
            writeNumber(row, 5, plo.getContributingCourseCount(), styles.integer());
            writeNumber(row, 6, plo.getContributingCloCount(), styles.integer());
            writeNumber(row, 7, plo.getIntroductionCount(), styles.integer());
            writeNumber(row, 8, plo.getDevelopmentCount(), styles.integer());
            writeNumber(row, 9, plo.getAchievementCount(), styles.integer());
            writeCell(row, 10, plo.getCourses().stream()
                    .map(PloCoverageReportData.CourseContribution::getCourseCode)
                    .filter(code -> code != null && !code.isBlank())
                    .distinct()
                    .reduce((left, right) -> left + ", " + right)
                    .orElse(""), styles.body());
        }
    }

    private static void writeContributionSheet(
            Sheet sheet,
            PloCoverageReportData data,
            ExcelStyles styles) {
        Row header = sheet.createRow(0);
        String[] headers = {"Mã PLO", "Mô tả PLO", "Mã môn", "Tên học phần", "Nhóm môn", "Mức I/D/A", "Số mapping", "Các CLO đóng góp"};
        for (int index = 0; index < headers.length; index++) {
            writeCell(header, index, headers[index], styles.header());
        }
        int rowIndex = 1;
        for (PloCoverageReportData.PloCoverageRow plo : data.getPlos()) {
            if (plo.getCourses().isEmpty()) {
                Row row = sheet.createRow(rowIndex++);
                writeCell(row, 0, plo.getPloCode(), styles.center());
                writeCell(row, 1, preferred(plo.getDescriptionVn(), plo.getDescription()), styles.body());
                writeCell(row, 2, "Chưa có đóng góp", styles.body());
                continue;
            }
            for (PloCoverageReportData.CourseContribution course : plo.getCourses()) {
                Row row = sheet.createRow(rowIndex++);
                writeCell(row, 0, plo.getPloCode(), styles.center());
                writeCell(row, 1, preferred(plo.getDescriptionVn(), plo.getDescription()), styles.body());
                writeCell(row, 2, course.getCourseCode(), styles.center());
                writeCell(row, 3, preferred(course.getCourseNameVn(), course.getCourseName()), styles.body());
                writeCell(row, 4, preferred(course.getCourseTypeNameVn(), course.getCourseTypeName()), styles.body());
                writeCell(row, 5, course.getLevel(), styles.center());
                writeNumber(row, 6, course.getMappingCount(), styles.integer());
                writeCell(row, 7, String.join(", ", course.getCloCodes()), styles.body());
            }
        }
    }

    private static ExcelStyles createStyles(XSSFWorkbook workbook) {
        CellStyle title = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 15);
        title.setFont(titleFont);

        CellStyle label = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font labelFont = workbook.createFont();
        labelFont.setBold(true);
        label.setFont(labelFont);

        CellStyle header = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        header.setFont(headerFont);
        header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);
        header.setWrapText(true);

        CellStyle body = workbook.createCellStyle();
        body.setWrapText(true);
        body.setVerticalAlignment(VerticalAlignment.TOP);

        CellStyle center = workbook.createCellStyle();
        center.cloneStyleFrom(body);
        center.setAlignment(HorizontalAlignment.CENTER);

        CellStyle integer = workbook.createCellStyle();
        integer.cloneStyleFrom(center);
        integer.setDataFormat(workbook.createDataFormat().getFormat("0"));

        CellStyle percentage = workbook.createCellStyle();
        percentage.cloneStyleFrom(center);
        percentage.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));

        return new ExcelStyles(title, label, header, body, center, integer, percentage);
    }

    private static void configureSummarySheet(Sheet sheet, int rows) {
        sheet.createFreezePane(2, SUMMARY_DATA_ROW);
        sheet.setAutoFilter(new CellRangeAddress(SUMMARY_HEADER_ROW,
                Math.max(SUMMARY_HEADER_ROW, SUMMARY_DATA_ROW + rows - 1), 0, 10));
        int[] widths = {7, 13, 44, 18, 15, 17, 17, 8, 8, 8, 40};
        for (int index = 0; index < widths.length; index++) {
            sheet.setColumnWidth(index, widths[index] * 256);
        }
    }

    private static void configureContributionSheet(Sheet sheet) {
        sheet.createFreezePane(2, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, sheet.getLastRowNum()), 0, 7));
        int[] widths = {13, 42, 15, 38, 24, 12, 14, 42};
        for (int index = 0; index < widths.length; index++) {
            sheet.setColumnWidth(index, widths[index] * 256);
        }
    }

    private static void writeMeta(Sheet sheet, int rowIndex, String label, Object value, ExcelStyles styles) {
        writeMeta(sheet, rowIndex, label, value, styles, false);
    }

    private static void writeMeta(Sheet sheet, int rowIndex, String label, Object value, ExcelStyles styles, boolean percentage) {
        Row row = sheet.createRow(rowIndex);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(styles.label());
        Cell valueCell = row.createCell(1);
        if (value instanceof Number number) {
            valueCell.setCellValue(number.doubleValue());
            valueCell.setCellStyle(percentage ? styles.percentage() : styles.body());
        } else {
            valueCell.setCellValue(value == null ? "" : String.valueOf(value));
            valueCell.setCellStyle(styles.body());
        }
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 1, 7));
    }

    private static void writeCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : String.valueOf(value));
        cell.setCellStyle(style);
    }

    private static void writeNumber(Row row, int column, Integer value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? 0 : Math.max(0, value));
        cell.setCellStyle(style);
    }

    private static void pdfCell(
            PdfPTable table,
            String value,
            com.lowagie.text.Font font,
            boolean header,
            int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(safe(value), font));
        cell.setPadding(3f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setHorizontalAlignment(alignment);
        if (header) {
            cell.setBackgroundColor(new Color(219, 234, 254));
        }
        table.addCell(cell);
    }

    private static void validate(PloCoverageReportData data) {
        if (data == null) {
            throw new IllegalArgumentException("Dữ liệu PLO Coverage Report không được để trống.");
        }
        if (data.getProgramId() == null || data.getCohortId() == null) {
            throw new IllegalArgumentException("Program và cohort là bắt buộc.");
        }
        if (data.getAcademicYear() == null || data.getAcademicYear().isBlank()) {
            throw new IllegalArgumentException("Năm học là bắt buộc.");
        }
        if (data.getSemester() == null || data.getSemester().isBlank()) {
            throw new IllegalArgumentException("Học kỳ là bắt buộc.");
        }
        if (data.getPlos() == null || data.getSummary() == null) {
            throw new IllegalArgumentException("Dữ liệu PLO và summary không được để null.");
        }
    }

    private static String scopeText(PloCoverageReportData data) {
        return "Chương trình: " + joinCodeName(data.getProgramCode(), preferred(data.getProgramNameVn(), data.getProgramName()))
                + " | Cohort: " + safe(data.getCohortName())
                + " | Năm học: " + safe(data.getAcademicYear())
                + " | Học kỳ: " + safe(data.getSemester())
                + " | Nhóm môn: " + preferred(data.getCourseTypeNameVn(), preferred(data.getCourseTypeName(), "Tất cả"));
    }

    private static String summaryText(PloCoverageReportData data) {
        return "PLO đã cover: " + data.getSummary().getCoveredPlos() + "/" + data.getSummary().getTotalPlos()
                + " (" + formatPercent(data.getSummary().getCoveragePercentage()) + ")"
                + " | Môn đóng góp: " + data.getSummary().getContributingCourses()
                + " | CLO mapped: " + data.getSummary().getMappedClos() + "/" + data.getSummary().getTotalClos();
    }

    private static String formatPercent(Double value) {
        double safeValue = value == null ? 0d : value;
        return String.format(Locale.ROOT, safeValue % 1d == 0d ? "%.0f%%" : "%.2f%%", safeValue);
    }

    private static Double asFraction(Double percentage) {
        return percentage == null ? 0d : percentage / 100d;
    }

    private static String preferred(String first, String fallback) {
        return first != null && !first.isBlank() ? first.trim() : safe(fallback);
    }

    private static String joinCodeName(String code, String name) {
        if (code == null || code.isBlank()) {
            return safe(name);
        }
        if (name == null || name.isBlank()) {
            return code.trim();
        }
        return code.trim() + " – " + name.trim();
    }

    private static String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record ExcelStyles(
            CellStyle title,
            CellStyle label,
            CellStyle header,
            CellStyle body,
            CellStyle center,
            CellStyle integer,
            CellStyle percentage) {
    }
}
