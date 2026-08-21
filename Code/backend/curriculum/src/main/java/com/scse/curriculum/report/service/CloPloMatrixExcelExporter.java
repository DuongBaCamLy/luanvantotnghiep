package com.scse.curriculum.report.service;

import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.scse.curriculum.dashboard.dto.DashboardHeatmapResponse;

/**
 * Writes the scoped CLO-PLO matrix to an OOXML workbook compatible with Excel 2016+.
 * The writer is deliberately independent from persistence: it exports exactly the
 * dataset returned by the canonical heatmap query and performs a final defensive
 * de-duplication by course identity.
 */
final class CloPloMatrixExcelExporter {

    static final String MATRIX_SHEET = "CLO-PLO Matrix";
    static final String META_SHEET = "_metadata";
    static final int HEADER_ROW_INDEX = 13;
    static final int DATA_START_ROW_INDEX = HEADER_ROW_INDEX + 1;

    private CloPloMatrixExcelExporter() {
    }

    static byte[] export(DashboardHeatmapResponse matrix) {
        validateMatrix(matrix);

        List<DashboardHeatmapResponse.PloColumn> plos = uniquePlos(matrix.getPloDetails());
        DeduplicatedCourses deduplicated = uniqueCourses(matrix.getCourseCoverages());

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            configureWorkbook(workbook, matrix);
            Sheet sheet = workbook.createSheet(MATRIX_SHEET);
            Styles styles = createStyles(workbook);

            writeTitleAndMetadata(sheet, matrix, plos.size(), deduplicated, styles);
            writeHeader(sheet, plos, styles);
            writeRows(sheet, plos, deduplicated.courses(), styles);
            configureSheet(sheet, plos.size(), deduplicated.courses().size());
            writeMetadataSheet(workbook, matrix, plos.size(), deduplicated);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể xuất ma trận CLO–PLO ra Excel.", exception);
        }
    }

    private static void validateMatrix(DashboardHeatmapResponse matrix) {
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
    }

  private static void configureWorkbook(
        XSSFWorkbook workbook,
        DashboardHeatmapResponse matrix) {

    var coreProperties =
            workbook.getProperties().getCoreProperties();

    coreProperties.setTitle(
            "Ma trận CLO-PLO toàn chương trình đào tạo");

    coreProperties.setCreator(
            "SCSE Curriculum Management System");

    coreProperties.setDescription(
            "Export ma trận CLO-PLO. Phạm vi: "
                    + safe(matrix.getScopeKey()));

    workbook.setForceFormulaRecalculation(false);
}

    private static Styles createStyles(XSSFWorkbook workbook) {
        CellStyle title = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 15);
        title.setFont(titleFont);
        title.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle label = workbook.createCellStyle();
        Font labelFont = workbook.createFont();
        labelFont.setBold(true);
        label.setFont(labelFont);
        label.setVerticalAlignment(VerticalAlignment.TOP);

        CellStyle metadata = workbook.createCellStyle();
        metadata.setWrapText(true);
        metadata.setVerticalAlignment(VerticalAlignment.TOP);

        CellStyle header = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        header.setFont(headerFont);
        header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);
        header.setWrapText(true);
        borders(header);

        CellStyle body = workbook.createCellStyle();
        body.setVerticalAlignment(VerticalAlignment.TOP);
        body.setWrapText(true);
        borders(body);

        CellStyle center = workbook.createCellStyle();
        center.cloneStyleFrom(body);
        center.setAlignment(HorizontalAlignment.CENTER);

        CellStyle integer = workbook.createCellStyle();
        integer.cloneStyleFrom(center);
        integer.setDataFormat(workbook.createDataFormat().getFormat("0"));

        CellStyle percentage = workbook.createCellStyle();
        percentage.cloneStyleFrom(center);
        percentage.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));

        return new Styles(title, label, metadata, header, body, center, integer, percentage);
    }

    private static void writeTitleAndMetadata(
            Sheet sheet,
            DashboardHeatmapResponse matrix,
            int uniquePloCount,
            DeduplicatedCourses deduplicated,
            Styles styles) {
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(24);
        Cell title = titleRow.createCell(0);
        title.setCellValue("MA TRẬN CLO–PLO TOÀN CHƯƠNG TRÌNH ĐÀO TẠO");
        title.setCellStyle(styles.title());
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        writeMetadata(sheet, 1, "Chương trình", joinCodeName(matrix.getProgramCode(), preferred(matrix.getProgramNameVn(), matrix.getProgramName())), styles);
        writeMetadata(sheet, 2, "Khóa tuyển sinh", preferred(matrix.getCohortName(), safe(matrix.getCohortEntryYear())), styles);
        writeMetadata(sheet, 3, "Năm học", matrix.getAcademicYear(), styles);
        writeMetadata(sheet, 4, "Học kỳ", matrix.getSemester(), styles);
        writeMetadata(sheet, 5, "Nhóm môn", preferred(matrix.getCourseTypeNameVn(), preferred(matrix.getCourseTypeName(), "Tất cả")), styles);
        writeMetadata(sheet, 6, "Khóa phạm vi", matrix.getScopeKey(), styles);
        writeMetadata(sheet, 7, "Nguồn dữ liệu", matrix.getDataSource(), styles);
        writeMetadata(sheet, 8, "Số môn duy nhất", deduplicated.courses().size(), styles);
        writeMetadata(sheet, 9, "Dòng course trùng đã loại", deduplicated.duplicatesRemoved(), styles);
        writeMetadata(sheet, 10, "Số PLO", uniquePloCount, styles);

        DashboardHeatmapResponse.HeatmapSummary summary = matrix.getSummary();
        writeMetadata(sheet, 11, "Coverage PLO", summary == null ? null : asFraction(summary.getPloCoveragePercentage()), styles, true);
        writeMetadata(sheet, 12, "Coverage CLO mapping", summary == null ? null : asFraction(summary.getCloMappingPercentage()), styles, true);
    }

    private static void writeHeader(Sheet sheet, List<DashboardHeatmapResponse.PloColumn> plos, Styles styles) {
        Row header = sheet.createRow(HEADER_ROW_INDEX);
        header.setHeightInPoints(34);
        String[] fixedHeaders = {
                "STT", "Mã môn", "Tên học phần", "Nhóm môn", "Phiên bản đề cương",
                "Năm học", "Học kỳ", "APPROVED", "Tổng CLO", "CLO đã mapping", "CLO chưa mapping"
        };
        for (int i = 0; i < fixedHeaders.length; i++) {
            writeCell(header, i, fixedHeaders[i], styles.header());
        }
        for (int i = 0; i < plos.size(); i++) {
            String code = preferred(plos.get(i).getCode(), "PLO-" + (i + 1));
            writeCell(header, fixedHeaders.length + i, code, styles.header());
        }
    }

    private static void writeRows(
            Sheet sheet,
            List<DashboardHeatmapResponse.PloColumn> plos,
            List<DashboardHeatmapResponse.CourseCoverage> courses,
            Styles styles) {
        int rowIndex = DATA_START_ROW_INDEX;
        int ordinal = 1;
        for (DashboardHeatmapResponse.CourseCoverage course : courses) {
            Row row = sheet.createRow(rowIndex++);
            row.setHeightInPoints(30);

            writeNumericCell(row, 0, ordinal++, styles.integer());
            writeCell(row, 1, course.getCourseCode(), styles.body());
            writeCell(row, 2, preferred(course.getCourseNameVn(), course.getCourseName()), styles.body());
            writeCell(row, 3, preferred(course.getCourseTypeNameVn(), preferred(course.getCourseTypeName(), "Chưa phân nhóm")), styles.body());
            writeCell(row, 4, course.getSyllabusVersionLabel(), styles.center());
            writeCell(row, 5, course.getSyllabusAcademicYear(), styles.center());
            writeCell(row, 6, course.getSyllabusSemester(), styles.center());
            writeCell(row, 7, Boolean.TRUE.equals(course.getHasApprovedSyllabus()) ? "Có" : "Không", styles.center());
            writeNumericCell(row, 8, safeInteger(course.getTotalClos()), styles.integer());
            writeNumericCell(row, 9, safeInteger(course.getMappedClos()), styles.integer());
            writeNumericCell(row, 10, Math.max(0, safeInteger(course.getTotalClos()) - safeInteger(course.getMappedClos())), styles.integer());

            Map<String, DashboardHeatmapResponse.CellCoverage> byPlo = indexCells(course.getCells());
            for (int i = 0; i < plos.size(); i++) {
                DashboardHeatmapResponse.PloColumn plo = plos.get(i);
                DashboardHeatmapResponse.CellCoverage coverage = byPlo.get(ploKey(plo));
                writeCell(row, 11 + i, normalizeLevel(coverage == null ? null : coverage.getLevel()), styles.center());
            }
        }
    }

    private static void configureSheet(Sheet sheet, int ploCount, int courseCount) {
        sheet.createFreezePane(3, DATA_START_ROW_INDEX);
        int lastColumn = 10 + ploCount;
        int lastRow = Math.max(HEADER_ROW_INDEX, DATA_START_ROW_INDEX + courseCount - 1);
        sheet.setAutoFilter(new CellRangeAddress(HEADER_ROW_INDEX, lastRow, 0, lastColumn));
        sheet.setRepeatingRows(new CellRangeAddress(HEADER_ROW_INDEX, HEADER_ROW_INDEX, -1, -1));
        sheet.setAutobreaks(true);
        sheet.setFitToPage(true);
        sheet.setDisplayGridlines(false);

        PrintSetup printSetup = sheet.getPrintSetup();
        printSetup.setLandscape(true);
        printSetup.setPaperSize(PrintSetup.A4_PAPERSIZE);
        printSetup.setFitWidth((short) 1);
        printSetup.setFitHeight((short) 0);
        sheet.setMargin(Sheet.LeftMargin, 0.25);
        sheet.setMargin(Sheet.RightMargin, 0.25);
        sheet.setMargin(Sheet.TopMargin, 0.5);
        sheet.setMargin(Sheet.BottomMargin, 0.5);

        int[] widths = {7, 15, 38, 24, 18, 14, 10, 12, 11, 14, 16};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
        for (int i = 0; i < ploCount; i++) {
            sheet.setColumnWidth(11 + i, 10 * 256);
        }
    }

    private static void writeMetadataSheet(
            XSSFWorkbook workbook,
            DashboardHeatmapResponse matrix,
            int uniquePloCount,
            DeduplicatedCourses deduplicated) {
        Sheet meta = workbook.createSheet(META_SHEET);
        Object[][] values = {
                {"formatVersion", "FR-07.1-v1"},
                {"generatedAt", OffsetDateTime.now().toString()},
                {"programId", matrix.getProgramId()},
                {"programCode", safe(matrix.getProgramCode())},
                {"cohortId", matrix.getCohortId()},
                {"cohortName", safe(matrix.getCohortName())},
                {"academicYear", safe(matrix.getAcademicYear())},
                {"semester", safe(matrix.getSemester())},
                {"courseTypeId", matrix.getCourseTypeId()},
                {"courseTypeCode", safe(matrix.getCourseTypeCode())},
                {"scopeKey", safe(matrix.getScopeKey())},
                {"uniqueCourseCount", deduplicated.courses().size()},
                {"duplicateRowsRemoved", deduplicated.duplicatesRemoved()},
                {"uniquePloCount", uniquePloCount}
        };
        for (int rowIndex = 0; rowIndex < values.length; rowIndex++) {
            Row row = meta.createRow(rowIndex);
            row.createCell(0).setCellValue(String.valueOf(values[rowIndex][0]));
            Object value = values[rowIndex][1];
            if (value instanceof Number number) {
                row.createCell(1).setCellValue(number.doubleValue());
            } else {
                row.createCell(1).setCellValue(value == null ? "" : String.valueOf(value));
            }
        }
        workbook.setSheetHidden(workbook.getSheetIndex(meta), true);
    }

    static DeduplicatedCourses uniqueCourses(List<DashboardHeatmapResponse.CourseCoverage> courses) {
        Map<String, DashboardHeatmapResponse.CourseCoverage> unique = new LinkedHashMap<>();
        int sourceRows = 0;
        if (courses != null) {
            for (DashboardHeatmapResponse.CourseCoverage course : courses) {
                if (course == null) {
                    continue;
                }
                sourceRows++;
                String key = courseKey(course);
                if (key != null) {
                    unique.putIfAbsent(key, course);
                }
            }
        }
        return new DeduplicatedCourses(new ArrayList<>(unique.values()), Math.max(0, sourceRows - unique.size()));
    }

    private static List<DashboardHeatmapResponse.PloColumn> uniquePlos(List<DashboardHeatmapResponse.PloColumn> plos) {
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

    private static Map<String, DashboardHeatmapResponse.CellCoverage> indexCells(List<DashboardHeatmapResponse.CellCoverage> cells) {
        Map<String, DashboardHeatmapResponse.CellCoverage> byPlo = new LinkedHashMap<>();
        if (cells != null) {
            for (DashboardHeatmapResponse.CellCoverage cell : cells) {
                if (cell != null) {
                    String key = cell.getPloId() != null
                            ? "ID:" + cell.getPloId()
                            : "CODE:" + normalize(cell.getPloCode());
                    byPlo.putIfAbsent(key, cell);
                }
            }
        }
        return byPlo;
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

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static void writeMetadata(Sheet sheet, int rowIndex, String label, Object value, Styles styles) {
        writeMetadata(sheet, rowIndex, label, value, styles, false);
    }

    private static void writeMetadata(Sheet sheet, int rowIndex, String label, Object value, Styles styles, boolean percentage) {
        Row row = sheet.createRow(rowIndex);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(styles.label());
        Cell valueCell = row.createCell(1);
        if (percentage && value instanceof Number number) {
            valueCell.setCellValue(number.doubleValue());
            valueCell.setCellStyle(styles.percentage());
        } else if (value instanceof Number number) {
            valueCell.setCellValue(number.doubleValue());
            valueCell.setCellStyle(styles.metadata());
        } else {
            valueCell.setCellValue(value == null ? "" : String.valueOf(value));
            valueCell.setCellStyle(styles.metadata());
        }
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 1, 5));
    }

    private static void writeCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : String.valueOf(value));
        cell.setCellStyle(style);
    }

    private static void writeNumericCell(Row row, int column, int value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void borders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private static int safeInteger(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private static Double asFraction(Double percentage) {
        return percentage == null ? null : percentage / 100.0d;
    }

    private static String joinCodeName(String code, String name) {
        if (isBlank(code)) return safe(name);
        if (isBlank(name)) return safe(code);
        return code.trim() + " – " + name.trim();
    }

    private static String preferred(String primary, String fallback) {
        return isBlank(primary) ? safe(fallback) : primary.trim();
    }

    private static String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    record DeduplicatedCourses(List<DashboardHeatmapResponse.CourseCoverage> courses, int duplicatesRemoved) {
    }

    private record Styles(
            CellStyle title,
            CellStyle label,
            CellStyle metadata,
            CellStyle header,
            CellStyle body,
            CellStyle center,
            CellStyle integer,
            CellStyle percentage) {
    }
}
