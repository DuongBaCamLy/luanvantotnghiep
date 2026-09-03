package com.scse.curriculum.report.service;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Service;

import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.scse.curriculum.dashboard.dto.DashboardHeatmapResponse;
import com.scse.curriculum.dashboard.service.DashboardService;
import com.scse.curriculum.report.dto.PloCoverageReportData;
import com.scse.curriculum.syllabus.pdf.SyllabusPdfFontProvider;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final EntityManager entityManager;
    private final DashboardService dashboardService;
    private final SyllabusPdfFontProvider pdfFonts;
    private final SyllabusSemesterReportQueryService syllabusSemesterReportQueryService;

    @Override
    public byte[] generateCloPloMatrixExcel(
            long programId,
            Integer cohortId,
            String academicYear,
            String semester,
            Integer courseTypeId) {
        DashboardHeatmapResponse matrix = loadScopedMatrix(
                programId,
                cohortId,
                academicYear,
                semester,
                courseTypeId);
        return CloPloMatrixExcelExporter.export(matrix);
    }

    @Override
    public byte[] generateCloPloMatrixPdf(
            long programId,
            Integer cohortId,
            String academicYear,
            String semester,
            Integer courseTypeId) {
        DashboardHeatmapResponse matrix = loadScopedMatrix(
                programId,
                cohortId,
                academicYear,
                semester,
                courseTypeId);
        return CloPloMatrixPdfExporter.export(matrix, pdfFonts);
    }

    @Override
    public byte[] generateCloPloMatrixExcel(long programId, Integer cohortId,
                                            String semester, String search, String status) {
        return CloPloMatrixExcelExporter.export(
                loadScopedMatrix(programId, cohortId, semester, search, status));
    }

    @Override
    public byte[] generateCloPloMatrixPdf(long programId, Integer cohortId,
                                          String semester, String search, String status) {
        return CloPloMatrixPdfExporter.export(
                loadScopedMatrix(programId, cohortId, semester, search, status), pdfFonts);
    }

    private DashboardHeatmapResponse loadScopedMatrix(long programId, Integer cohortId,
                                                       String semester, String search, String status) {
        DashboardHeatmapResponse matrix = dashboardService.getHeatmapCoverage(
                programId, cohortId, null, semester, null, search, status);
        if (matrix == null) throw new IllegalStateException("Không có dữ liệu ma trận CLO–PLO cho phạm vi đã chọn.");
        if (matrix.getPloDetails() == null) matrix.setPloDetails(new ArrayList<>());
        if (matrix.getCourseCoverages() == null) matrix.setCourseCoverages(new ArrayList<>());
        return matrix;
    }

    private DashboardHeatmapResponse loadScopedMatrix(long programId, Integer cohortId, String academicYear, String semester, Integer courseTypeId) {
        if (cohortId == null) throw new IllegalArgumentException("cohortId là bắt buộc.");
        DashboardHeatmapResponse matrix = dashboardService.getHeatmapCoverage(
                programId, cohortId, null, null, null);
        if (matrix == null) throw new IllegalStateException("Không có dữ liệu ma trận CLO–PLO cho phạm vi đã chọn.");
        if (matrix.getPloDetails() == null) matrix.setPloDetails(new ArrayList<>());
        if (matrix.getCourseCoverages() == null) matrix.setCourseCoverages(new ArrayList<>());
        return matrix;
    }

    static List<DashboardHeatmapResponse.CourseCoverage> uniqueCourses(List<DashboardHeatmapResponse.CourseCoverage> courses) {
        Map<Integer, DashboardHeatmapResponse.CourseCoverage> unique = new LinkedHashMap<>();
        if (courses != null) {
            for (DashboardHeatmapResponse.CourseCoverage course : courses) {
                if (course != null && course.getCourseId() != null) unique.putIfAbsent(course.getCourseId(), course);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private static void writeMetadata(Sheet sheet, int rowIndex, String label, String value) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value == null ? "" : value);
    }

    private static void writeCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private static void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private static void addPdfHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(safe(text), font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(new Color(219, 234, 254));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private static void addPdfBody(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(safe(text), font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private static String formatPercent(Double value) {
        return value == null ? "0%" : (value % 1 == 0
                ? String.format(java.util.Locale.ROOT, "%.0f%%", value)
                : String.format(java.util.Locale.ROOT, "%.1f%%", value));
    }

    private static String preferred(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : safe(fallback);
    }

    private static String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @Override
    public byte[] generateSyllabusListExcel(String academicYear, String semester, Integer programId, Integer cohortId, String status) {
        return SyllabusSemesterReportExporter.excel(
                syllabusSemesterReportQueryService.load(academicYear, semester, programId, cohortId, status));
    }

    @Override
    public byte[] generateSyllabusListPdf(String academicYear, String semester, Integer programId, Integer cohortId, String status) {
        return SyllabusSemesterReportExporter.pdf(
                syllabusSemesterReportQueryService.load(academicYear, semester, programId, cohortId, status), pdfFonts);
    }

    @Override
    public PloCoverageReportData getPloCoverageReport(
            long programId,
            Integer cohortId,
            String academicYear,
            String semester,
            Integer courseTypeId) {
        return PloCoverageReportAssembler.from(loadScopedMatrix(
                programId, cohortId, academicYear, semester, courseTypeId));
    }

    @Override
    public byte[] generatePloCoverageExcel(
            long programId,
            Integer cohortId,
            String academicYear,
            String semester,
            Integer courseTypeId) {
        return PloCoverageReportExporter.excel(getPloCoverageReport(
                programId, cohortId, academicYear, semester, courseTypeId));
    }

    @Override
    public byte[] generatePloCoveragePdf(
            long programId,
            Integer cohortId,
            String academicYear,
            String semester,
            Integer courseTypeId) {
        return PloCoverageReportExporter.pdf(getPloCoverageReport(
                programId, cohortId, academicYear, semester, courseTypeId), pdfFonts);
    }

}
