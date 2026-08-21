package com.scse.curriculum.instructor.service;

import com.scse.curriculum.instructor.dto.InstructorExportRequest;
import com.scse.curriculum.instructor.dto.InstructorResponse;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFPrintSetup;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InstructorExcelExportService {

    private static final ZoneId VIETNAM_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private static final DateTimeFormatter GENERATED_AT_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final int COLUMN_COUNT = 11;

    public byte[] export(
            List<InstructorResponse> instructors,
            InstructorExportRequest request) {

        try (
                XSSFWorkbook workbook =
                        new XSSFWorkbook();
                ByteArrayOutputStream output =
                        new ByteArrayOutputStream()
        ) {
            XSSFSheet sheet =
                    workbook.createSheet(
                            "Instructor Directory");

            configurePrint(
                    workbook,
                    sheet);

            CellStyle titleStyle =
                    createTitleStyle(workbook);

            CellStyle subtitleStyle =
                    createSubtitleStyle(workbook);

            CellStyle metaStyle =
                    createMetaStyle(workbook);

            CellStyle headerStyle =
                    createHeaderStyle(workbook);

            CellStyle textStyle =
                    createTextStyle(workbook);

            CellStyle centeredStyle =
                    createCenteredStyle(workbook);

            CellStyle activeStyle =
                    createStatusStyle(
                            workbook,
                            IndexedColors.LIGHT_GREEN,
                            IndexedColors.DARK_GREEN);

            CellStyle inactiveStyle =
                    createStatusStyle(
                            workbook,
                            IndexedColors.ROSE,
                            IndexedColors.DARK_RED);

            CellStyle neutralStyle =
                    createStatusStyle(
                            workbook,
                            IndexedColors.GREY_25_PERCENT,
                            IndexedColors.GREY_80_PERCENT);

            createMergedTextRow(
                    sheet,
                    0,
                    "SCSE – IU · INSTRUCTOR DIRECTORY",
                    titleStyle);

            createMergedTextRow(
                    sheet,
                    1,
                    "School of Computer Science & Engineering · Curriculum Management System",
                    subtitleStyle);

            createMergedTextRow(
                    sheet,
                    2,
                    buildFilterSummary(request),
                    metaStyle);

            String generated =
                    "Generated: "
                            + ZonedDateTime
                            .now(VIETNAM_ZONE)
                            .format(GENERATED_AT_FORMAT)
                            + " · Records: "
                            + instructors.size();

            createMergedTextRow(
                    sheet,
                    3,
                    generated,
                    metaStyle);

            // Blank spacer row.
            sheet.createRow(4)
                    .setHeightInPoints(8);

            String[] headers = {
                    "No.",
                    "Staff Code",
                    "Full Name",
                    "Email",
                    "Department",
                    "Account Role",
                    "Degree",
                    "Academic Rank",
                    "Assigned Courses",
                    "Profile Status",
                    "Login Account"
            };

            var headerRow =
                    sheet.createRow(5);

            headerRow.setHeightInPoints(30);

            for (
                    int column = 0;
                    column < headers.length;
                    column++
            ) {
                Cell cell =
                        headerRow.createCell(column);

                cell.setCellValue(
                        headers[column]);

                cell.setCellStyle(
                        headerStyle);
            }

            int rowIndex = 6;
            int sequence = 1;

            for (
                    InstructorResponse instructor
                    : instructors
            ) {
                var row =
                        sheet.createRow(
                                rowIndex++);

                row.setHeightInPoints(27);

                writeCell(
                        row,
                        0,
                        sequence++,
                        centeredStyle);

                writeCell(
                        row,
                        1,
                        value(instructor.getStaffCode()),
                        centeredStyle);

                writeCell(
                        row,
                        2,
                        value(instructor.getFullName()),
                        textStyle);

                writeCell(
                        row,
                        3,
                        value(instructor.getEmail()),
                        textStyle);

                writeCell(
                        row,
                        4,
                        department(instructor),
                        textStyle);

                writeCell(
                        row,
                        5,
                        role(instructor),
                        centeredStyle);

                writeCell(
                        row,
                        6,
                        value(instructor.getDegree()),
                        centeredStyle);

                writeCell(
                        row,
                        7,
                        value(instructor.getAcademicRank()),
                        textStyle);

                writeCell(
                        row,
                        8,
                        instructor.getCourseCount() != null
                                ? instructor.getCourseCount()
                                : 0,
                        centeredStyle);

                CellStyle profileStatusStyle =
                        Boolean.TRUE.equals(
                                instructor.getIsActive())
                                ? activeStyle
                                : inactiveStyle;

                writeCell(
                        row,
                        9,
                        Boolean.TRUE.equals(
                                instructor.getIsActive())
                                ? "Active"
                                : "Inactive",
                        profileStatusStyle);

                String loginAccount =
                        instructor.getUserAccountId() == null
                                ? "Not linked"
                                : (
                                    Boolean.TRUE.equals(
                                            instructor.getAccountActive())
                                            ? "Active · "
                                            : "Deactivated · "
                                )
                                + value(
                                    instructor.getUsername());

                CellStyle loginStyle =
                        instructor.getUserAccountId() == null
                                ? neutralStyle
                                : (
                                    Boolean.TRUE.equals(
                                            instructor.getAccountActive())
                                            ? activeStyle
                                            : inactiveStyle
                                );

                writeCell(
                        row,
                        10,
                        loginAccount,
                        loginStyle);
            }

            int lastRow =
                    Math.max(
                            5,
                            sheet.getLastRowNum());

            sheet.setAutoFilter(
                    new CellRangeAddress(
                            5,
                            lastRow,
                            0,
                            COLUMN_COUNT - 1));

            sheet.createFreezePane(
                    0,
                    6);

            /*
             * Repeat the table header on every printed page.
             * This is the main requirement for "print without manual fixing".
             */
            sheet.setRepeatingRows(
                    CellRangeAddress.valueOf(
                            "6:6"));

            workbook.setPrintArea(
                    0,
                    0,
                    COLUMN_COUNT - 1,
                    0,
                    lastRow);

            applyColumnWidths(sheet);

            workbook.write(output);

            return output.toByteArray();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to generate instructor Excel file.",
                    exception);
        }
    }

    private void configurePrint(
            XSSFWorkbook workbook,
            XSSFSheet sheet) {

        XSSFPrintSetup printSetup =
                sheet.getPrintSetup();

        printSetup.setLandscape(true);
        printSetup.setPaperSize(
                PrintSetup.A4_PAPERSIZE);
        printSetup.setFitWidth((short) 1);
        printSetup.setFitHeight((short) 0);

        sheet.setFitToPage(true);
        sheet.setAutobreaks(true);
        sheet.setHorizontallyCenter(true);
        sheet.setVerticallyCenter(false);

        sheet.setMargin(
                Sheet.LeftMargin,
                0.25);
        sheet.setMargin(
                Sheet.RightMargin,
                0.25);
        sheet.setMargin(
                Sheet.TopMargin,
                0.45);
        sheet.setMargin(
                Sheet.BottomMargin,
                0.45);
        sheet.setMargin(
                Sheet.HeaderMargin,
                0.20);
        sheet.setMargin(
                Sheet.FooterMargin,
                0.20);

        /*
         * Excel/XSSF footer field codes:
         *   &P = current page
         *   &N = total number of pages
         *
         * Use the field codes directly because org.apache.poi.ss.usermodel.HeaderFooter
         * does not expose page()/numPages() helper methods in the POI version used by
         * this project.
         */
        sheet.getFooter()
                .setCenter(
                        "Page &P of &N");

        sheet.getFooter()
                .setRight(
                        "SCSE – IU");

        workbook.setPrintArea(
                0,
                0,
                COLUMN_COUNT - 1,
                0,
                5);
    }

    private void applyColumnWidths(
            XSSFSheet sheet) {

        int[] widths = {
                7,
                14,
                25,
                31,
                25,
                20,
                12,
                18,
                17,
                16,
                24
        };

        for (
                int index = 0;
                index < widths.length;
                index++
        ) {
            sheet.setColumnWidth(
                    index,
                    widths[index] * 256);
        }
    }

    private CellStyle createTitleStyle(
            XSSFWorkbook workbook) {

        XSSFCellStyle style =
                workbook.createCellStyle();

        Font font =
                workbook.createFont();

        font.setFontName("Aptos Display");
        font.setFontHeightInPoints(
                (short) 18);
        font.setBold(true);
        font.setColor(
                IndexedColors.DARK_TEAL
                        .getIndex());

        style.setFont(font);
        style.setAlignment(
                HorizontalAlignment.CENTER);
        style.setVerticalAlignment(
                VerticalAlignment.CENTER);

        return style;
    }

    private CellStyle createSubtitleStyle(
            XSSFWorkbook workbook) {

        XSSFCellStyle style =
                workbook.createCellStyle();

        Font font =
                workbook.createFont();

        font.setFontName("Aptos");
        font.setFontHeightInPoints(
                (short) 11);
        font.setColor(
                IndexedColors.GREY_80_PERCENT
                        .getIndex());

        style.setFont(font);
        style.setAlignment(
                HorizontalAlignment.CENTER);

        return style;
    }

    private CellStyle createMetaStyle(
            XSSFWorkbook workbook) {

        XSSFCellStyle style =
                workbook.createCellStyle();

        Font font =
                workbook.createFont();

        font.setFontName("Aptos");
        font.setFontHeightInPoints(
                (short) 10);
        font.setColor(
                IndexedColors.GREY_80_PERCENT
                        .getIndex());

        style.setFont(font);
        style.setAlignment(
                HorizontalAlignment.LEFT);
        style.setVerticalAlignment(
                VerticalAlignment.CENTER);

        return style;
    }

    private CellStyle createHeaderStyle(
            XSSFWorkbook workbook) {

        XSSFCellStyle style =
                workbook.createCellStyle();

        Font font =
                workbook.createFont();

        font.setFontName("Aptos");
        font.setFontHeightInPoints(
                (short) 10);
        font.setBold(true);
        font.setColor(
                IndexedColors.WHITE
                        .getIndex());

        style.setFont(font);
        style.setFillForegroundColor(
                IndexedColors.DARK_TEAL
                        .getIndex());
        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(
                HorizontalAlignment.CENTER);
        style.setVerticalAlignment(
                VerticalAlignment.CENTER);
        style.setWrapText(true);

        applyBorders(style);

        return style;
    }

    private CellStyle createTextStyle(
            XSSFWorkbook workbook) {

        XSSFCellStyle style =
                workbook.createCellStyle();

        Font font =
                workbook.createFont();

        font.setFontName("Aptos");
        font.setFontHeightInPoints(
                (short) 10);

        style.setFont(font);
        style.setVerticalAlignment(
                VerticalAlignment.CENTER);
        style.setWrapText(true);

        applyBorders(style);

        return style;
    }

    private CellStyle createCenteredStyle(
            XSSFWorkbook workbook) {

        XSSFCellStyle style =
                (XSSFCellStyle)
                        createTextStyle(workbook);

        style.setAlignment(
                HorizontalAlignment.CENTER);

        return style;
    }

    private CellStyle createStatusStyle(
            XSSFWorkbook workbook,
            IndexedColors background,
            IndexedColors foreground) {

        XSSFCellStyle style =
                workbook.createCellStyle();

        Font font =
                workbook.createFont();

        font.setFontName("Aptos");
        font.setFontHeightInPoints(
                (short) 10);
        font.setBold(true);
        font.setColor(
                foreground.getIndex());

        style.setFont(font);
        style.setAlignment(
                HorizontalAlignment.CENTER);
        style.setVerticalAlignment(
                VerticalAlignment.CENTER);
        style.setFillForegroundColor(
                background.getIndex());
        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND);
        style.setWrapText(true);

        applyBorders(style);

        return style;
    }

    private void applyBorders(
            CellStyle style) {

        style.setBorderTop(
                BorderStyle.THIN);
        style.setBorderBottom(
                BorderStyle.THIN);
        style.setBorderLeft(
                BorderStyle.THIN);
        style.setBorderRight(
                BorderStyle.THIN);

        style.setTopBorderColor(
                IndexedColors.GREY_25_PERCENT
                        .getIndex());
        style.setBottomBorderColor(
                IndexedColors.GREY_25_PERCENT
                        .getIndex());
        style.setLeftBorderColor(
                IndexedColors.GREY_25_PERCENT
                        .getIndex());
        style.setRightBorderColor(
                IndexedColors.GREY_25_PERCENT
                        .getIndex());
    }

    private void createMergedTextRow(
            XSSFSheet sheet,
            int rowIndex,
            String text,
            CellStyle style) {

        var row =
                sheet.createRow(rowIndex);

        row.setHeightInPoints(
                rowIndex == 0
                        ? 29
                        : 20);

        Cell cell =
                row.createCell(0);

        cell.setCellValue(text);
        cell.setCellStyle(style);

        sheet.addMergedRegion(
                new CellRangeAddress(
                        rowIndex,
                        rowIndex,
                        0,
                        COLUMN_COUNT - 1));
    }

    private void writeCell(
            org.apache.poi.ss.usermodel.Row row,
            int column,
            String value,
            CellStyle style) {

        Cell cell =
                row.createCell(column);

        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void writeCell(
            org.apache.poi.ss.usermodel.Row row,
            int column,
            int value,
            CellStyle style) {

        Cell cell =
                row.createCell(column);

        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private String buildFilterSummary(
            InstructorExportRequest request) {

        if (request == null) {
            return "Filters: All instructors";
        }

        return "Filters: Department="
                + safeFilter(request.getDepartment())
                + " · Role="
                + safeFilter(request.getRole())
                + " · Degree="
                + safeFilter(request.getDegree())
                + " · Status="
                + safeFilter(request.getStatus())
                + (
                    request.getSearch() == null
                    || request.getSearch().isBlank()
                        ? ""
                        : " · Search=\""
                            + request.getSearch().trim()
                            + "\""
                );
    }

    private String safeFilter(
            String value) {

        return value == null
                || value.isBlank()
                || "all".equalsIgnoreCase(value)
                ? "All"
                : value;
    }

    private String department(
            InstructorResponse instructor) {

        if (
                instructor.getDepartmentNameVn() != null
                && !instructor
                    .getDepartmentNameVn()
                    .isBlank()
        ) {
            return instructor.getDepartmentNameVn();
        }

        if (
                instructor.getDepartmentName() != null
                && !instructor
                    .getDepartmentName()
                    .isBlank()
        ) {
            return instructor.getDepartmentName();
        }

        return value(
                instructor.getDepartmentCode());
    }

    private String role(
            InstructorResponse instructor) {

        if (instructor.getRole() == null) {
            return "Not linked";
        }

        return switch (
                instructor.getRole()
        ) {
            case ADMIN -> "Administrator";
            case DEAN -> "Dean";
            case DEPT_HEAD -> "Head of Department";
            case INSTRUCTOR -> "Instructor";
            default -> instructor
                    .getRole()
                    .name();
        };
    }

    private String value(
            String value) {

        return value == null
                || value.isBlank()
                ? "—"
                : value;
    }
}