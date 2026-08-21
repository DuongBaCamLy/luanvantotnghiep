package com.scse.curriculum.report.service;

import com.scse.curriculum.report.dto.CurriculumChangeReportData;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CurriculumChangeReportExporterTest {
    @Test
    void exportsExcelWithAddedRemovedModifiedCreditsAndMetadata() throws Exception {
        var oldCohort = CurriculumChangeReportData.CohortSnapshot.builder().id(1).name("CS2021").entryYear(2021).courseCount(10).totalCredits(120).build();
        var newCohort = CurriculumChangeReportData.CohortSnapshot.builder().id(2).name("CS2022").entryYear(2022).courseCount(11).totalCredits(123).build();
        var change = CurriculumChangeReportData.CourseChange.builder().changeType("MODIFIED").courseId(10).courseCode("IT001")
                .courseNameVn("Nhập môn Tin học")
                .oldValue(CurriculumChangeReportData.CourseSnapshot.builder().totalCredits(3).semesterSuggest(1).build())
                .newValue(CurriculumChangeReportData.CourseSnapshot.builder().totalCredits(4).semesterSuggest(2).build())
                .changes(List.of(CurriculumChangeReportData.FieldChange.builder().field("totalCredits").label("Tổng tín chỉ").oldValue("3").newValue("4").build()))
                .build();
        var data = CurriculumChangeReportData.builder().formatVersion("FR-07.6-v1").generatedAt(OffsetDateTime.now())
                .scopeKey("program=1|oldCohort=1|newCohort=2").programId(1).programCode("CS").programNameVn("Khoa học Máy tính")
                .oldCohort(oldCohort).newCohort(newCohort)
                .summary(CurriculumChangeReportData.Summary.builder().oldCourseCount(10).newCourseCount(11).addedCourses(1).removedCourses(0).modifiedCourses(1).unchangedCourses(9).oldTotalCredits(120).newTotalCredits(123).creditDifference(3).metadataChangeCount(1).build())
                .metadataChanges(List.of(CurriculumChangeReportData.MetadataChange.builder().field("entryYear").label("Năm tuyển sinh").oldValue("2021").newValue("2022").build()))
                .courseChanges(List.of(change)).warnings(List.of()).build();

        byte[] bytes = CurriculumChangeReportExporter.excel(data);
        assertThat(bytes).startsWith((byte) 'P', (byte) 'K');
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getSheet("Summary")).isNotNull();
            assertThat(workbook.getSheet("Course Changes")).isNotNull();
            assertThat(workbook.getSheet("Metadata Changes")).isNotNull();
            assertThat(workbook.getSheet("Course Changes").getRow(1).getCell(2).getStringCellValue()).isEqualTo("IT001");
            assertThat(workbook.getSheet("Course Changes").getRow(1).getCell(18).getStringCellValue()).contains("3", "4");
        }
    }
}
