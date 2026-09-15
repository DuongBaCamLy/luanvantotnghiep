package com.scse.curriculum.report.service;

import com.scse.curriculum.syllabus.entity.SyllabusVersion;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.scse.curriculum.report.dto.SyllabusSemesterReportData;
import com.scse.curriculum.syllabus.pdf.SyllabusPdfFontProvider;

final class SyllabusSemesterReportExporter {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private SyllabusSemesterReportExporter() {
    }

    static byte[] excel(SyllabusSemesterReportData data) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Syllabus theo học kỳ");
            writeMeta(sheet, 0, "Năm học", data.getAcademicYear());
            writeMeta(sheet, 1, "Học kỳ", data.getSemester());
            writeMeta(sheet, 2, "Chương trình", text(data.getProgramCode(), "Tất cả"));
            writeMeta(sheet, 3, "Cohort", text(data.getCohortName(), "Tất cả"));
            writeMeta(sheet, 4, "Trạng thái", data.getStatus());
            writeMeta(sheet, 5, "Số syllabus đại diện", String.valueOf(data.getRows().size()));

            String[] headers = {"STT", "Mã môn", "Tên học phần", "Version", "Trạng thái", "Giảng viên", "Username GV",
                "Năm học", "Học kỳ", "Cohort", "Ngày tạo", "Ngày nộp", "Ngày duyệt", "Người duyệt cuối",
                "Username người duyệt", "Bước duyệt cuối", "Kết quả duyệt cuối", "Thời điểm duyệt cuối", "Ghi chú duyệt cuối"};
            int headerRowIndex = 7;

            Row headerRow
                    = sheet.createRow(headerRowIndex);

            CellStyle headerStyle
                    = workbook.createCellStyle();

            org.apache.poi.ss.usermodel.Font excelHeaderFont
                    = workbook.createFont();

            excelHeaderFont.setBold(true);
            excelHeaderFont.setColor(
                    IndexedColors.WHITE.getIndex());

            headerStyle.setFont(excelHeaderFont);
            headerStyle.setFillForegroundColor(
                    IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(
                    FillPatternType.SOLID_FOREGROUND);
            headerStyle.setWrapText(true);
            headerStyle.setAlignment(
                    HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(
                    VerticalAlignment.CENTER);

            for (int index = 0;
                    index < headers.length;
                    index++) {

                Cell cell = headerRow.createCell(index);
                cell.setCellValue(headers[index]);
                cell.setCellStyle(headerStyle);
            }
            CellStyle body = workbook.createCellStyle();
            body.setWrapText(true);
            body.setVerticalAlignment(VerticalAlignment.TOP);
            int r = headerRowIndex + 1, no = 1;
            for (var row : data.getRows()) {
                Row x = sheet.createRow(r++);
                int c = 0;
                set(x, c++, no++, body);
                set(x, c++, row.getCourseCode(), body);
                set(x, c++, text(row.getCourseNameVn(), row.getCourseName()), body);
                set(x, c++, SyllabusVersion.display(row.getVersionNumber(), row.getVersionLabel()), body);
                set(x, c++, row.getStatus(), body);
                set(x, c++, row.getInstructorFullName(), body);
                set(x, c++, row.getInstructorUsername(), body);
                set(x, c++, row.getAcademicYear(), body);
                set(x, c++, row.getSemester(), body);
                set(x, c++, row.getCohortNames(), body);
                set(x, c++, date(row.getCreatedAt()), body);
                set(x, c++, date(row.getSubmittedAt()), body);
                set(x, c++, date(row.getApprovedAt()), body);
                set(x, c++, row.getFinalReviewerFullName(), body);
                set(x, c++, row.getFinalReviewerUsername(), body);
                set(x, c++, row.getFinalApprovalStep(), body);
                set(x, c++, row.getFinalApprovalStatus(), body);
                set(x, c++, date(row.getFinalReviewedAt()), body);
                set(x, c, row.getFinalComment(), body);
            }
            sheet.createFreezePane(3, headerRowIndex + 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(headerRowIndex, Math.max(headerRowIndex, r - 1), 0, headers.length - 1));
            int[] widths = {7, 15, 35, 12, 18, 24, 20, 14, 10, 24, 18, 18, 18, 24, 20, 22, 22, 20, 38};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể xuất danh sách syllabus ra Excel.", e);
        }
    }

    static byte[] pdf(
        SyllabusSemesterReportData data,
        SyllabusPdfFontProvider fonts) {

    validateData(data);

    if (fonts == null) {
        throw new IllegalArgumentException(
                "SyllabusPdfFontProvider không được để trống.");
    }

    Document document = new Document(
            PageSize.A3.rotate(),
            20,
            20,
            25,
            25);

    try (ByteArrayOutputStream out =
                 new ByteArrayOutputStream()) {

        PdfWriter.getInstance(document, out);
        document.open();

        com.lowagie.text.Font titleFont =
                fonts.bold(14);

        com.lowagie.text.Font headerFont =
                fonts.bold(7);

        com.lowagie.text.Font bodyFont =
                fonts.regular(6.5f);

        document.add(new Paragraph(
                "DANH SÁCH SYLLABUS THEO HỌC KỲ",
                titleFont));

        document.add(new Paragraph(
                "Năm học: "
                        + text(
                                data.getAcademicYear(),
                                "-")
                        + " | Học kỳ: "
                        + text(
                                data.getSemester(),
                                "-")
                        + " | Chương trình: "
                        + text(
                                data.getProgramCode(),
                                "Tất cả")
                        + " | Cohort: "
                        + text(
                                data.getCohortName(),
                                "Tất cả"),
                bodyFont));

        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(
                new float[]{
                        1.1f,
                        2.5f,
                        4.5f,
                        1.4f,
                        2f,
                        3f,
                        2f,
                        2.6f,
                        2.4f,
                        2.6f,
                        2.5f
                });

        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSplitRows(true);
        table.setSplitLate(false);

        String[] headers = {
                "Mã môn",
                "Tên học phần",
                "Version",
                "Trạng thái",
                "Giảng viên",
                "Cohort",
                "Ngày nộp",
                "Ngày duyệt",
                "Người duyệt cuối",
                "Bước/KQ cuối",
                "Thời điểm cuối"
        };

        for (String header : headers) {
            cell(
                    table,
                    header,
                    headerFont,
                    true);
        }

        for (var row : data.getRows()) {
            cell(
                    table,
                    row.getCourseCode(),
                    bodyFont,
                    false);

            cell(
                    table,
                    text(
                            row.getCourseNameVn(),
                            row.getCourseName()),
                    bodyFont,
                    false);

            cell(
                    table,
                    SyllabusVersion.display(row.getVersionNumber(), row.getVersionLabel()),
                    bodyFont,
                    false);

            cell(
                    table,
                    row.getStatus(),
                    bodyFont,
                    false);

            cell(
                    table,
                    text(
                            row.getInstructorFullName(),
                            row.getInstructorUsername()),
                    bodyFont,
                    false);

            cell(
                    table,
                    row.getCohortNames(),
                    bodyFont,
                    false);

            cell(
                    table,
                    date(row.getSubmittedAt()),
                    bodyFont,
                    false);

            cell(
                    table,
                    date(row.getApprovedAt()),
                    bodyFont,
                    false);

            cell(
                    table,
                    text(
                            row.getFinalReviewerFullName(),
                            row.getFinalReviewerUsername()),
                    bodyFont,
                    false);

            cell(
                    table,
                    text(
                            row.getFinalApprovalStep(),
                            "")
                            + " / "
                            + text(
                                    row.getFinalApprovalStatus(),
                                    ""),
                    bodyFont,
                    false);

            cell(
                    table,
                    date(
                            row.getFinalReviewedAt()),
                    bodyFont,
                    false);
        }

        document.add(table);
        document.close();

        return out.toByteArray();
    } catch (Exception exception) {
        if (document.isOpen()) {
            document.close();
        }

        throw new IllegalStateException(
                "Không thể xuất danh sách syllabus ra PDF.",
                exception);
    }
}
    private static void writeMeta(Sheet s, int r, String l, String v) {
        Row row = s.createRow(r);
        row.createCell(0).setCellValue(l);
        row.createCell(1).setCellValue(v == null ? "" : v);
    }

    private static void set(Row r, int c, Object v, CellStyle s) {
        Cell x = r.createCell(c);
        if (v instanceof Number n) {
            x.setCellValue(n.doubleValue());
        } else {
            x.setCellValue(v == null ? "" : String.valueOf(v));

        }
        x.setCellStyle(s);
    }

    private static void cell(
            PdfPTable table,
            String value,
            com.lowagie.text.Font font,
            boolean header) {

        PdfPCell cell = new PdfPCell(
                new Phrase(value == null ? "" : value, font));

        cell.setPadding(3f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        if (header) {
            cell.setBackgroundColor(new Color(219, 234, 254));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        }

        table.addCell(cell);
    }

    private static String date(LocalDateTime value) {
        return value == null
                ? ""
                : DATE_TIME.format(value);
    }

    private static String text(String a, String b) {
        return a != null && !a.isBlank() ? a : b == null ? "" : b;
    }
    private static void validateData(
        SyllabusSemesterReportData data) {

    if (data == null) {
        throw new IllegalArgumentException(
                "Dữ liệu báo cáo syllabus không được để trống.");
    }

    if (data.getAcademicYear() == null
            || data.getAcademicYear().isBlank()) {
        throw new IllegalArgumentException(
                "Năm học là bắt buộc.");
    }

    if (data.getSemester() == null
            || data.getSemester().isBlank()) {
        throw new IllegalArgumentException(
                "Học kỳ là bắt buộc.");
    }

    if (data.getRows() == null) {
        throw new IllegalArgumentException(
                "Danh sách syllabus không được để null.");
    }
}
}
