package com.scse.curriculum.report.service;

import com.scse.curriculum.dashboard.dto.DashboardHeatmapResponse;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CloPloMatrixExcelExporterTest {

    @Test
    void exportsExcel2016CompatibleWorkbookWithExactScopeAndNoDuplicateCourses() throws Exception {
        DashboardHeatmapResponse matrix = matrix();
        matrix.setCourseCoverages(List.of(
                course(10, "IT001", "Nhập môn Tin học", "COMPULSORY", "Môn bắt buộc", "I", 4, 4),
                course(10, "IT001", "Nhập môn Tin học", "COMPULSORY", "Môn bắt buộc", "I", 4, 4),
                course(11, "IT002", "Cấu trúc dữ liệu", "ELECTIVE", "Môn tự chọn", "D", 5, 3)
        ));

        byte[] content = CloPloMatrixExcelExporter.export(matrix);

        assertThat(content).isNotEmpty();
        assertThat(content[0]).isEqualTo((byte) 'P');
        assertThat(content[1]).isEqualTo((byte) 'K'); // XLSX is an OOXML ZIP package.

        try (OPCPackage ignored = OPCPackage.open(new ByteArrayInputStream(content));
             XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheet(CloPloMatrixExcelExporter.MATRIX_SHEET);
            assertThat(sheet).isNotNull();
            assertThat(workbook.isSheetHidden(workbook.getSheetIndex(CloPloMatrixExcelExporter.META_SHEET))).isTrue();

            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).contains("CS-2021", "Khoa học Máy tính");
            assertThat(sheet.getRow(2).getCell(1).getStringCellValue()).isEqualTo("CS2021");
            assertThat(sheet.getRow(3).getCell(1).getStringCellValue()).isEqualTo("2026-2027");
            assertThat(sheet.getRow(4).getCell(1).getStringCellValue()).isEqualTo("1");
            assertThat(sheet.getRow(5).getCell(1).getStringCellValue()).isEqualTo("Tất cả");
            assertThat(sheet.getRow(6).getCell(1).getStringCellValue())
                    .isEqualTo("program=1|cohort=2|academicYear=2026-2027|semester=1|courseType=ALL");
            assertThat(sheet.getRow(8).getCell(1).getNumericCellValue()).isEqualTo(2);
            assertThat(sheet.getRow(9).getCell(1).getNumericCellValue()).isEqualTo(1);
            assertThat(sheet.getRow(11).getCell(1).getNumericCellValue()).isEqualTo(1.0);
            assertThat(sheet.getRow(12).getCell(1).getNumericCellValue()).isEqualTo(0.7);

            Row header = sheet.getRow(CloPloMatrixExcelExporter.HEADER_ROW_INDEX);
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("Mã môn");
            assertThat(header.getCell(11).getStringCellValue()).isEqualTo("PLO1");
            assertThat(header.getCell(12).getStringCellValue()).isEqualTo("PLO2");

            Set<String> courseCodes = new HashSet<>();
            int dataRows = 0;
            for (int rowIndex = CloPloMatrixExcelExporter.DATA_START_ROW_INDEX;
                 rowIndex <= sheet.getLastRowNum();
                 rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || row.getCell(1) == null || row.getCell(1).getCellType() == CellType.BLANK) continue;
                dataRows++;
                assertThat(courseCodes.add(row.getCell(1).getStringCellValue())).isTrue();
            }
            assertThat(dataRows).isEqualTo(2);
            assertThat(courseCodes).containsExactlyInAnyOrder("IT001", "IT002");

            Row firstCourse = sheet.getRow(CloPloMatrixExcelExporter.DATA_START_ROW_INDEX);
            assertThat(firstCourse.getCell(3).getStringCellValue()).isEqualTo("Môn bắt buộc");
            assertThat(firstCourse.getCell(7).getStringCellValue()).isEqualTo("Có");
            assertThat(firstCourse.getCell(11).getStringCellValue()).isEqualTo("I");
        }
    }

    @Test
    void rejectsMatrixWithoutRequiredScope() {
        DashboardHeatmapResponse matrix = new DashboardHeatmapResponse();
        matrix.setProgramId(1);

        assertThatThrownBy(() -> CloPloMatrixExcelExporter.export(matrix))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cohortId");
    }

    private static DashboardHeatmapResponse matrix() {
        DashboardHeatmapResponse matrix = new DashboardHeatmapResponse();
        matrix.setProgramId(1);
        matrix.setProgramCode("CS-2021");
        matrix.setProgramName("Computer Science");
        matrix.setProgramNameVn("Khoa học Máy tính");
        matrix.setCohortId(2);
        matrix.setCohortName("CS2021");
        matrix.setAcademicYear("2026-2027");
        matrix.setSemester("1");
        matrix.setDataSource("Canonical heatmap query");
        matrix.setScopeKey("program=1|cohort=2|academicYear=2026-2027|semester=1|courseType=ALL");

        DashboardHeatmapResponse.PloColumn plo1 = new DashboardHeatmapResponse.PloColumn();
        plo1.setId(100);
        plo1.setCode("PLO1");
        DashboardHeatmapResponse.PloColumn plo2 = new DashboardHeatmapResponse.PloColumn();
        plo2.setId(101);
        plo2.setCode("PLO2");
        matrix.setPloDetails(List.of(plo1, plo2));

        DashboardHeatmapResponse.HeatmapSummary summary = new DashboardHeatmapResponse.HeatmapSummary();
        summary.setPloCoveragePercentage(100.0);
        summary.setCloMappingPercentage(70.0);
        matrix.setSummary(summary);
        return matrix;
    }

    private static DashboardHeatmapResponse.CourseCoverage course(
            int id,
            String code,
            String nameVn,
            String typeCode,
            String typeNameVn,
            String level,
            int totalClos,
            int mappedClos) {
        DashboardHeatmapResponse.CourseCoverage course = new DashboardHeatmapResponse.CourseCoverage();
        course.setCourseId(id);
        course.setCourseCode(code);
        course.setCourseNameVn(nameVn);
        course.setCourseTypeCode(typeCode);
        course.setCourseTypeNameVn(typeNameVn);
        course.setSyllabusVersionLabel("v2.0");
        course.setSyllabusAcademicYear("2026-2027");
        course.setSyllabusSemester("1");
        course.setHasApprovedSyllabus(true);
        course.setTotalClos(totalClos);
        course.setMappedClos(mappedClos);

        DashboardHeatmapResponse.CellCoverage cell = new DashboardHeatmapResponse.CellCoverage();
        cell.setPloId(100);
        cell.setPloCode("PLO1");
        cell.setLevel(level);
        course.setCells(List.of(cell));
        return course;
    }
}
