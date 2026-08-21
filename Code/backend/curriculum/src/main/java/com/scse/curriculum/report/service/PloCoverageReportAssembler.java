package com.scse.curriculum.report.service;

import com.scse.curriculum.dashboard.dto.DashboardHeatmapResponse;
import com.scse.curriculum.report.dto.PloCoverageReportData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class PloCoverageReportAssembler {

    private PloCoverageReportAssembler() {
    }

    static PloCoverageReportData from(DashboardHeatmapResponse matrix) {
        validate(matrix);

        PloCoverageReportData report = new PloCoverageReportData();
        report.setProgramId(matrix.getProgramId());
        report.setProgramCode(matrix.getProgramCode());
        report.setProgramName(matrix.getProgramName());
        report.setProgramNameVn(matrix.getProgramNameVn());
        report.setCohortId(matrix.getCohortId());
        report.setCohortName(matrix.getCohortName());
        report.setAcademicYear(matrix.getAcademicYear());
        report.setSemester(matrix.getSemester());
        report.setCourseTypeId(matrix.getCourseTypeId());
        report.setCourseTypeCode(matrix.getCourseTypeCode());
        report.setCourseTypeName(matrix.getCourseTypeName());
        report.setCourseTypeNameVn(matrix.getCourseTypeNameVn());
        report.setScopeKey(matrix.getScopeKey());
        report.setDataSource(matrix.getDataSource());

        List<DashboardHeatmapResponse.PloColumn> plos = uniquePlos(matrix.getPloDetails());
        List<DashboardHeatmapResponse.CourseCoverage> courses = uniqueCourses(matrix.getCourseCoverages());

        Map<String, PloCoverageReportData.PloCoverageRow> rowsByPlo = new LinkedHashMap<>();
        for (DashboardHeatmapResponse.PloColumn plo : plos) {
            PloCoverageReportData.PloCoverageRow row = new PloCoverageReportData.PloCoverageRow();
            row.setPloId(plo.getId());
            row.setPloCode(safe(plo.getCode()));
            row.setDescription(plo.getDescription());
            row.setDescriptionVn(plo.getDescriptionVn());
            row.setCategory(plo.getCategory());
            rowsByPlo.put(ploKey(plo.getId(), plo.getCode()), row);
        }

        Set<Integer> contributingCourseIds = new LinkedHashSet<>();
        Set<String> contributingCourseCodes = new LinkedHashSet<>();
        int totalClos = 0;
        int mappedClos = 0;

        for (DashboardHeatmapResponse.CourseCoverage course : courses) {
            totalClos += nonNegative(course.getTotalClos());
            mappedClos += nonNegative(course.getMappedClos());
            if (course.getCells() == null) {
                continue;
            }

            for (DashboardHeatmapResponse.CellCoverage cell : course.getCells()) {
                if (cell == null) {
                    continue;
                }
                PloCoverageReportData.PloCoverageRow ploRow = rowsByPlo.get(
                        ploKey(cell.getPloId(), cell.getPloCode()));
                if (ploRow == null || !isContribution(cell)) {
                    continue;
                }

                PloCoverageReportData.CourseContribution contribution =
                        new PloCoverageReportData.CourseContribution();
                contribution.setCourseId(course.getCourseId());
                contribution.setCourseCode(course.getCourseCode());
                contribution.setCourseName(course.getCourseName());
                contribution.setCourseNameVn(course.getCourseNameVn());
                contribution.setCourseTypeName(course.getCourseTypeName());
                contribution.setCourseTypeNameVn(course.getCourseTypeNameVn());
                contribution.setLevel(normalizeLevel(cell.getLevel()));
                contribution.setMappingCount(resolveMappingCount(cell));
                contribution.setCloCodes(uniqueStrings(cell.getCloCodes()));
                ploRow.getCourses().add(contribution);

                if (course.getCourseId() != null) {
                    contributingCourseIds.add(course.getCourseId());
                } else if (course.getCourseCode() != null && !course.getCourseCode().isBlank()) {
                    contributingCourseCodes.add(normalize(course.getCourseCode()));
                }
            }
        }

        for (PloCoverageReportData.PloCoverageRow row : rowsByPlo.values()) {
            row.getCourses().sort(Comparator.comparing(
                    PloCoverageReportData.CourseContribution::getCourseCode,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
            row.setContributingCourseCount(row.getCourses().size());
            row.setContributingCloCount(row.getCourses().stream()
                    .mapToInt(PloCoverageReportData.CourseContribution::getMappingCount)
                    .sum());
            row.setIntroductionCount(countLevel(row.getCourses(), "I"));
            row.setDevelopmentCount(countLevel(row.getCourses(), "D"));
            row.setAchievementCount(countLevel(row.getCourses(), "A"));
            row.setCovered(!row.getCourses().isEmpty());
        }

        List<PloCoverageReportData.PloCoverageRow> rows = new ArrayList<>(rowsByPlo.values());
        rows.sort(Comparator.comparing(
                PloCoverageReportData.PloCoverageRow::getPloCode,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        report.setPlos(rows);

        PloCoverageReportData.Summary summary = report.getSummary();
        summary.setTotalPlos(rows.size());
        summary.setCoveredPlos((int) rows.stream().filter(r -> Boolean.TRUE.equals(r.getCovered())).count());
        summary.setUncoveredPlos(summary.getTotalPlos() - summary.getCoveredPlos());
        summary.setCoveragePercentage(summary.getTotalPlos() == 0
                ? 0d
                : round2(summary.getCoveredPlos() * 100d / summary.getTotalPlos()));
        summary.setTotalCourses(courses.size());
        summary.setContributingCourses(contributingCourseIds.size() + contributingCourseCodes.size());
        summary.setTotalClos(totalClos);
        summary.setMappedClos(mappedClos);
        summary.setUnmappedClos(Math.max(0, totalClos - mappedClos));

        if (rows.isEmpty()) {
            report.getWarnings().add("Không có PLO trong phạm vi đã chọn.");
        }
        List<String> uncovered = rows.stream()
                .filter(r -> !Boolean.TRUE.equals(r.getCovered()))
                .map(PloCoverageReportData.PloCoverageRow::getPloCode)
                .filter(code -> code != null && !code.isBlank())
                .toList();
        if (!uncovered.isEmpty()) {
            report.getWarnings().add("PLO chưa được cover: " + String.join(", ", uncovered));
        }
        if (courses.isEmpty()) {
            report.getWarnings().add("Không có môn học trong phạm vi đã chọn.");
        }
        return report;
    }

    private static void validate(DashboardHeatmapResponse matrix) {
        if (matrix == null) {
            throw new IllegalArgumentException("Dữ liệu heatmap không được để trống.");
        }
        if (matrix.getProgramId() == null || matrix.getCohortId() == null) {
            throw new IllegalArgumentException("Program và cohort là bắt buộc.");
        }
        if (matrix.getAcademicYear() == null || matrix.getAcademicYear().isBlank()) {
            throw new IllegalArgumentException("Năm học là bắt buộc.");
        }
        if (matrix.getSemester() == null || matrix.getSemester().isBlank()) {
            throw new IllegalArgumentException("Học kỳ là bắt buộc.");
        }
    }

    private static List<DashboardHeatmapResponse.PloColumn> uniquePlos(
            List<DashboardHeatmapResponse.PloColumn> source) {
        Map<String, DashboardHeatmapResponse.PloColumn> unique = new LinkedHashMap<>();
        if (source != null) {
            for (DashboardHeatmapResponse.PloColumn plo : source) {
                if (plo != null) {
                    unique.putIfAbsent(ploKey(plo.getId(), plo.getCode()), plo);
                }
            }
        }
        return new ArrayList<>(unique.values());
    }

    private static List<DashboardHeatmapResponse.CourseCoverage> uniqueCourses(
            List<DashboardHeatmapResponse.CourseCoverage> source) {
        Map<String, DashboardHeatmapResponse.CourseCoverage> unique = new LinkedHashMap<>();
        if (source != null) {
            for (DashboardHeatmapResponse.CourseCoverage course : source) {
                if (course == null) {
                    continue;
                }
                String key = course.getCourseId() != null
                        ? "ID:" + course.getCourseId()
                        : "CODE:" + normalize(course.getCourseCode());
                if (!key.endsWith(":")) {
                    unique.putIfAbsent(key, course);
                }
            }
        }
        return new ArrayList<>(unique.values());
    }

    private static boolean isContribution(DashboardHeatmapResponse.CellCoverage cell) {
        return resolveMappingCount(cell) > 0 || !normalizeLevel(cell.getLevel()).isBlank();
    }

    private static int resolveMappingCount(DashboardHeatmapResponse.CellCoverage cell) {
        int explicit = nonNegative(cell.getMappingCount());
        if (explicit > 0) {
            return explicit;
        }
        return uniqueStrings(cell.getCloCodes()).size();
    }

    private static List<String> uniqueStrings(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .distinct()
                .toList());
    }

    private static int countLevel(
            List<PloCoverageReportData.CourseContribution> courses,
            String level) {
        return (int) courses.stream().filter(c -> level.equals(c.getLevel())).count();
    }

    private static String normalizeLevel(String value) {
        String normalized = normalize(value);
        return switch (normalized) {
            case "I", "D", "A" -> normalized;
            default -> "";
        };
    }

    private static String ploKey(Integer id, String code) {
        return id != null ? "ID:" + id : "CODE:" + normalize(code);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static int nonNegative(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static double round2(double value) {
        return Math.round(value * 100d) / 100d;
    }
}
