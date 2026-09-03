package com.scse.curriculum.report.service;

import com.scse.curriculum.dashboard.dto.DashboardHeatmapResponse;
import com.scse.curriculum.report.dto.PloCoverageReportData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PloCoverageReportAssemblerTest {

    @Test
    void buildsIndependentCoverageSummaryAndContributionDetailsWithoutDuplicateCourses() {
        DashboardHeatmapResponse matrix = matrix();
        matrix.setCourseCoverages(List.of(
                course(10, "IT001", "X", 2, List.of("CLO1", "CLO2")),
                course(10, "IT001", "X", 2, List.of("CLO1", "CLO2")),
                course(11, "IT002", "XX", 1, List.of("CLO3"))
        ));

        PloCoverageReportData report = PloCoverageReportAssembler.from(matrix);

        assertThat(report.getSummary().getTotalPlos()).isEqualTo(2);
        assertThat(report.getSummary().getCoveredPlos()).isEqualTo(1);
        assertThat(report.getSummary().getUncoveredPlos()).isEqualTo(1);
        assertThat(report.getSummary().getCoveragePercentage()).isEqualTo(50d);
        assertThat(report.getSummary().getTotalCourses()).isEqualTo(2);
        assertThat(report.getSummary().getContributingCourses()).isEqualTo(2);
        assertThat(report.getWarnings()).anyMatch(message -> message.contains("PLO2"));

        PloCoverageReportData.PloCoverageRow plo1 = report.getPlos().get(0);
        assertThat(plo1.getPloCode()).isEqualTo("PLO1");
        assertThat(plo1.getContributingCourseCount()).isEqualTo(2);
        assertThat(plo1.getContributingCloCount()).isEqualTo(3);
        assertThat(plo1.getIntroductionCount()).isEqualTo(1);
        assertThat(plo1.getDevelopmentCount()).isEqualTo(1);
        assertThat(plo1.getCourses()).extracting("courseCode")
                .containsExactly("IT001", "IT002");
    }

    private static DashboardHeatmapResponse matrix() {
        DashboardHeatmapResponse matrix = new DashboardHeatmapResponse();
        matrix.setProgramId(1);
        matrix.setProgramCode("CS-2021");
        matrix.setCohortId(2);
        matrix.setCohortName("CS2021");
        matrix.setScopeKey("program=1|cohort=2");

        DashboardHeatmapResponse.PloColumn plo1 = new DashboardHeatmapResponse.PloColumn();
        plo1.setId(100);
        plo1.setCode("PLO1");
        plo1.setDescriptionVn("Vận dụng kiến thức");
        DashboardHeatmapResponse.PloColumn plo2 = new DashboardHeatmapResponse.PloColumn();
        plo2.setId(101);
        plo2.setCode("PLO2");
        plo2.setDescriptionVn("Kỹ năng nghề nghiệp");
        matrix.setPloDetails(List.of(plo1, plo2));
        return matrix;
    }

    private static DashboardHeatmapResponse.CourseCoverage course(
            int id,
            String code,
            String level,
            int mappingCount,
            List<String> cloCodes) {
        DashboardHeatmapResponse.CourseCoverage course = new DashboardHeatmapResponse.CourseCoverage();
        course.setCourseId(id);
        course.setCourseCode(code);
        course.setCourseNameVn("Môn " + code);
        course.setTotalClos(4);
        course.setMappedClos(3);

        DashboardHeatmapResponse.CellCoverage cell = new DashboardHeatmapResponse.CellCoverage();
        cell.setPloId(100);
        cell.setPloCode("PLO1");
        cell.setLevel(level);
        cell.setMappingCount(mappingCount);
        cell.setCloCodes(cloCodes);
        course.setCells(List.of(cell));
        return course;
    }
}
