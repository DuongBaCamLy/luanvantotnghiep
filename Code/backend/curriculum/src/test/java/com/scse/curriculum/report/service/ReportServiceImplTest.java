package com.scse.curriculum.report.service;

import com.scse.curriculum.dashboard.dto.DashboardHeatmapResponse;
import com.scse.curriculum.dashboard.service.DashboardService;
import com.scse.curriculum.syllabus.pdf.SyllabusPdfFontProvider;
import jakarta.persistence.EntityManager;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.poi.ss.usermodel.Row;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private DashboardService dashboardService;

    @Mock
    private SyllabusSemesterReportQueryService
            syllabusSemesterReportQueryService;

    private ReportServiceImpl service;

    @BeforeEach
    void setUp() {
    SyllabusPdfFontProvider fonts =
            new SyllabusPdfFontProvider("", "");

    service = new ReportServiceImpl(
            entityManager,
            dashboardService,
            fonts,
            syllabusSemesterReportQueryService);
}

    @Test
    void excelUsesExactScopeAndRemovesDuplicateCourses() throws Exception {
        DashboardHeatmapResponse matrix = sampleMatrix();
        matrix.setCourseCoverages(List.of(course(10, "IT001IU", "Nhập môn Tin học"), course(10, "IT001IU", "Nhập môn Tin học")));
        when(dashboardService.getHeatmapCoverage(1L, 2, null, null, null)).thenReturn(matrix);

        byte[] bytes = service.generateCloPloMatrixExcel(1L, 2, "2026-2027", "1", 3);

        verify(dashboardService).getHeatmapCoverage(1L, 2, null, null, null);
        try (Workbook workbook =
             new XSSFWorkbook(new ByteArrayInputStream(bytes))) {

    var sheet = workbook.getSheet(
            CloPloMatrixExcelExporter.MATRIX_SHEET);

    assertThat(sheet).isNotNull();

    // Row 1: Chương trình
    assertThat(sheet.getRow(1).getCell(0).getStringCellValue())
            .isEqualTo("Chương trình");

    assertThat(sheet.getRow(1).getCell(1).getStringCellValue())
            .contains("CS", "Khoa học Máy tính");

    // Row 2: Khóa tuyển sinh
    assertThat(sheet.getRow(2).getCell(0).getStringCellValue())
            .isEqualTo("Khóa tuyển sinh");

    assertThat(sheet.getRow(2).getCell(1).getStringCellValue())
            .isEqualTo("CS-2021");

    // Row 3: toàn bộ curriculum của cohort
    assertThat(sheet.getRow(3).getCell(0).getStringCellValue())
            .isEqualTo("Phạm vi");

    assertThat(sheet.getRow(3).getCell(1).getStringCellValue())
            .isEqualTo("Toàn bộ chương trình của cohort");

    // Row 6: Scope key
    assertThat(sheet.getRow(6).getCell(1).getStringCellValue())
            .isEqualTo(
                    "program=1|cohort=2");

    // Row 8: unique course count
    assertThat(sheet.getRow(8).getCell(1).getNumericCellValue())
            .isEqualTo(1);

    // Row 9: duplicate rows removed
    assertThat(sheet.getRow(9).getCell(1).getNumericCellValue())
            .isEqualTo(1);

    // Row 11: PLO coverage = 75% được lưu dạng 0.75
    assertThat(sheet.getRow(11).getCell(1).getNumericCellValue())
            .isEqualTo(0.75);

    // Row 12: CLO mapping coverage = 80% được lưu dạng 0.80
    assertThat(sheet.getRow(12).getCell(1).getNumericCellValue())
            .isEqualTo(0.80);

    Row header =
            sheet.getRow(
                    CloPloMatrixExcelExporter.HEADER_ROW_INDEX);

    assertThat(header.getCell(1).getStringCellValue())
            .isEqualTo("Mã môn");

    assertThat(header.getCell(11).getStringCellValue())
            .isEqualTo("PLO1");

    Row courseRow =
            sheet.getRow(
                    CloPloMatrixExcelExporter.DATA_START_ROW_INDEX);

    assertThat(courseRow.getCell(1).getStringCellValue())
            .isEqualTo("IT001IU");

    assertThat(courseRow.getCell(2).getStringCellValue())
            .isEqualTo("Nhập môn Tin học");

    assertThat(courseRow.getCell(11).getStringCellValue())
            .isEqualTo("XX");

    // Chỉ có đúng 1 dòng course sau khi loại trùng
    assertThat(
            sheet.getLastRowNum())
            .isEqualTo(
                    CloPloMatrixExcelExporter.DATA_START_ROW_INDEX);
}
    }

    @Test
    void pdfIsGeneratedWithVietnameseScopedContent() {
        DashboardHeatmapResponse matrix = sampleMatrix();
        matrix.setCourseCoverages(List.of(course(10, "IT001IU", "Nhập môn Tin học")));
        when(dashboardService.getHeatmapCoverage(1L, 2, null, null, null)).thenReturn(matrix);

        byte[] bytes = service.generateCloPloMatrixPdf(1L, 2, "2026-2027", "1", 3);

        assertThat(bytes).startsWith("%PDF".getBytes());
        assertThat(bytes.length).isGreaterThan(1000);
    }

    private static DashboardHeatmapResponse sampleMatrix() {
        DashboardHeatmapResponse response = new DashboardHeatmapResponse();
        response.setProgramId(1);
        response.setProgramCode("CS");
        response.setProgramName("Computer Science");
        response.setProgramNameVn("Khoa học Máy tính");
        response.setCohortId(2);
        response.setCohortName("CS-2021");
        response.setAcademicYear("2026-2027");
        response.setSemester("1");
        response.setCourseTypeId(3);
        response.setCourseTypeCode("COMPULSORY");
        response.setCourseTypeName("Required");
        response.setCourseTypeNameVn("Bắt buộc");
        response.setDataSource("APPROVED syllabus theo chương trình, khóa, năm học và học kỳ");
        response.setScopeKey("program=1|cohort=2");
        DashboardHeatmapResponse.HeatmapSummary summary = new DashboardHeatmapResponse.HeatmapSummary();
        summary.setPloCoveragePercentage(75.0);
        summary.setCloMappingPercentage(80.0);
        summary.setCoursesWithApprovedSyllabus(1);
        summary.setTotalCourses(1);
        response.setSummary(summary);
        DashboardHeatmapResponse.PloColumn plo = new DashboardHeatmapResponse.PloColumn();
        plo.setId(100);
        plo.setCode("PLO1");
        response.setPloDetails(List.of(plo));
        return response;
    }

    private static DashboardHeatmapResponse.CourseCoverage course(
        int id,
        String code,
        String nameVn) {

    DashboardHeatmapResponse.CourseCoverage course =
            new DashboardHeatmapResponse.CourseCoverage();

    course.setCourseId(id);
    course.setCourseCode(code);
    course.setCourseName("Introduction to Computing");
    course.setCourseNameVn(nameVn);

    course.setCourseTypeCode("COMPULSORY");
    course.setCourseTypeName("Required");
    course.setCourseTypeNameVn("Bắt buộc");

    course.setSyllabusVersionLabel("v2.0");
    course.setSyllabusAcademicYear("2026-2027");
    course.setSyllabusSemester("1");
    course.setHasApprovedSyllabus(true);

    course.setTotalClos(4);
    course.setMappedClos(4);

    DashboardHeatmapResponse.CellCoverage cell =
            new DashboardHeatmapResponse.CellCoverage();

    cell.setPloId(100);
    cell.setPloCode("PLO1");
    cell.setLevel("XX");

    course.setCells(List.of(cell));

    return course;
}
}
