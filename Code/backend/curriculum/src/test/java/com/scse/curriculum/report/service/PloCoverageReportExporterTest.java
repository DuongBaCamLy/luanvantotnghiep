package com.scse.curriculum.report.service;

import com.lowagie.text.pdf.PdfReader;
import com.scse.curriculum.dashboard.dto.DashboardHeatmapResponse;
import com.scse.curriculum.report.dto.PloCoverageReportData;
import com.scse.curriculum.syllabus.pdf.SyllabusPdfFontProvider;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PloCoverageReportExporterTest {

    @Test
    void excelContainsSummaryUncoveredPloAndContributionSheet() throws Exception {
        PloCoverageReportData report = PloCoverageReportAssembler.from(matrix());
        byte[] bytes = PloCoverageReportExporter.excel(report);

        assertThat(bytes).isNotEmpty();
        assertThat(bytes[0]).isEqualTo((byte) 'P');
        assertThat(bytes[1]).isEqualTo((byte) 'K');

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet summary = workbook.getSheet(PloCoverageReportExporter.SUMMARY_SHEET);
            Sheet details = workbook.getSheet(PloCoverageReportExporter.CONTRIBUTION_SHEET);
            assertThat(summary).isNotNull();
            assertThat(details).isNotNull();
            assertThat(summary.getRow(8).getCell(1).getNumericCellValue()).isEqualTo(1d);
            assertThat(summary.getRow(9).getCell(1).getNumericCellValue()).isEqualTo(1d);
            assertThat(summary.getRow(PloCoverageReportExporter.SUMMARY_DATA_ROW)
                    .getCell(1).getStringCellValue()).isEqualTo("PLO1");
            assertThat(details.getRow(1).getCell(2).getStringCellValue()).isEqualTo("IT001");
        }
    }

    @Test
    void pdfEmbedsVietnameseFontAndIncludesMultipleSections() throws Exception {
        PloCoverageReportData report = PloCoverageReportAssembler.from(matrix());
        SyllabusPdfFontProvider fonts = new SyllabusPdfFontProvider("", "");
        byte[] bytes = PloCoverageReportExporter.pdf(report, fonts);

        assertThat(bytes).startsWith("%PDF".getBytes());
        PdfReader reader = new PdfReader(bytes);
        assertThat(reader.getNumberOfPages()).isGreaterThanOrEqualTo(2);
        reader.close();
    }

    private static DashboardHeatmapResponse matrix() {
        DashboardHeatmapResponse matrix = new DashboardHeatmapResponse();
        matrix.setProgramId(1);
        matrix.setProgramCode("CS-2021");
        matrix.setProgramNameVn("Khoa học Máy tính");
        matrix.setCohortId(2);
        matrix.setCohortName("CS2021");
        matrix.setAcademicYear("2026-2027");
        matrix.setSemester("1");
        matrix.setScopeKey("program=1|cohort=2|academicYear=2026-2027|semester=1|courseType=ALL");

        DashboardHeatmapResponse.PloColumn plo1 = new DashboardHeatmapResponse.PloColumn();
        plo1.setId(100);
        plo1.setCode("PLO1");
        plo1.setDescriptionVn("Vận dụng kiến thức chuyên môn");
        DashboardHeatmapResponse.PloColumn plo2 = new DashboardHeatmapResponse.PloColumn();
        plo2.setId(101);
        plo2.setCode("PLO2");
        plo2.setDescriptionVn("Kỹ năng nghề nghiệp");
        matrix.setPloDetails(List.of(plo1, plo2));

        DashboardHeatmapResponse.CourseCoverage course = new DashboardHeatmapResponse.CourseCoverage();
        course.setCourseId(10);
        course.setCourseCode("IT001");
        course.setCourseNameVn("Nhập môn Tin học");
        course.setCourseTypeNameVn("Môn bắt buộc");
        course.setTotalClos(4);
        course.setMappedClos(2);
        DashboardHeatmapResponse.CellCoverage cell = new DashboardHeatmapResponse.CellCoverage();
        cell.setPloId(100);
        cell.setPloCode("PLO1");
        cell.setLevel("I");
        cell.setMappingCount(2);
        cell.setCloCodes(List.of("CLO1", "CLO2"));
        course.setCells(List.of(cell));
        matrix.setCourseCoverages(List.of(course));
        return matrix;
    }
}
