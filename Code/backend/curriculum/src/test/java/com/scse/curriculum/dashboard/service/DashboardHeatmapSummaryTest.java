package com.scse.curriculum.dashboard.service;

import com.scse.curriculum.dashboard.dto.DashboardHeatmapResponse;
import com.scse.curriculum.plo.entity.Plo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardHeatmapSummaryTest {

    @Test
    void summaryMatchesFinalHeatmapMatrix() {

        List<Plo> plos =
                List.of(
                        plo(1, "PLO1"),
                        plo(2, "PLO2"),
                        plo(3, "PLO3"),
                        plo(4, "PLO4")
                );

        DashboardHeatmapResponse.CourseCoverage course1 =
                course(
                        true,
                        4,
                        3,
                        List.of(
                                cell(1, "PLO1", 2),
                                cell(2, "PLO2", 1),
                                cell(3, "PLO3", 0),
                                cell(4, "PLO4", 0)
                        )
                );

        DashboardHeatmapResponse.CourseCoverage course2 =
                course(
                        true,
                        2,
                        2,
                        List.of(
                                cell(1, "PLO1", 1),
                                cell(2, "PLO2", 0),
                                cell(3, "PLO3", 1),
                                cell(4, "PLO4", 0)
                        )
                );

        DashboardHeatmapResponse.CourseCoverage course3 =
                course(
                        false,
                        0,
                        0,
                        List.of(
                                cell(1, "PLO1", 0),
                                cell(2, "PLO2", 0),
                                cell(3, "PLO3", 0),
                                cell(4, "PLO4", 0)
                        )
                );

        DashboardHeatmapResponse.HeatmapSummary summary =
                DashboardServiceImpl.buildHeatmapSummary(
                        plos,
                        List.of(
                                course1,
                                course2,
                                course3
                        )
                );

        assertThat(summary.getTotalPlos())
                .isEqualTo(4);

        assertThat(summary.getCoveredPlos())
                .isEqualTo(3);

        assertThat(summary.getUncoveredPlos())
                .isEqualTo(1);

        assertThat(summary.getPloCoveragePercentage())
                .isEqualTo(75.0);

        assertThat(summary.getTotalCourses())
                .isEqualTo(3);

        assertThat(summary.getCoursesWithApprovedSyllabus())
                .isEqualTo(2);

        assertThat(summary.getCoursesWithoutApprovedSyllabus())
                .isEqualTo(1);

        assertThat(summary.getApprovedSyllabusPercentage())
                .isEqualTo(66.7);

        assertThat(summary.getTotalClos())
                .isEqualTo(6);

        assertThat(summary.getMappedClos())
                .isEqualTo(5);

        assertThat(summary.getUnmappedClos())
                .isEqualTo(1);

        assertThat(summary.getCloMappingPercentage())
                .isEqualTo(83.3);
    }

    @Test
    void handlesEmptyMatrixSafely() {

        DashboardHeatmapResponse.HeatmapSummary summary =
                DashboardServiceImpl.buildHeatmapSummary(
                        List.of(),
                        List.of()
                );

        assertThat(summary.getTotalPlos())
                .isZero();

        assertThat(summary.getCoveredPlos())
                .isZero();

        assertThat(summary.getPloCoveragePercentage())
                .isZero();

        assertThat(summary.getTotalCourses())
                .isZero();

        assertThat(summary.getApprovedSyllabusPercentage())
                .isZero();

        assertThat(summary.getTotalClos())
                .isZero();

        assertThat(summary.getMappedClos())
                .isZero();

        assertThat(summary.getCloMappingPercentage())
                .isZero();
    }

    private static Plo plo(
            int id,
            String code) {

        return Plo.builder()
                .id(id)
                .code(code)
                .build();
    }

    private static DashboardHeatmapResponse.CourseCoverage course(
            boolean approved,
            int totalClos,
            int mappedClos,
            List<DashboardHeatmapResponse.CellCoverage> cells) {

        DashboardHeatmapResponse.CourseCoverage course =
                new DashboardHeatmapResponse.CourseCoverage();

        course.setHasApprovedSyllabus(approved);
        course.setTotalClos(totalClos);
        course.setMappedClos(mappedClos);
        course.setCells(cells);

        return course;
    }

    private static DashboardHeatmapResponse.CellCoverage cell(
            int ploId,
            String ploCode,
            int mappingCount) {

        DashboardHeatmapResponse.CellCoverage cell =
                new DashboardHeatmapResponse.CellCoverage();

        cell.setPloId(ploId);
        cell.setPloCode(ploCode);
        cell.setMappingCount(mappingCount);

        return cell;
    }
}