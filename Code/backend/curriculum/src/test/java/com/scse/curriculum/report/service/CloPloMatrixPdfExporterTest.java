package com.scse.curriculum.report.service;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.scse.curriculum.dashboard.dto.DashboardHeatmapResponse;
import com.scse.curriculum.syllabus.pdf.SyllabusPdfFontProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CloPloMatrixPdfExporterTest {

    @Test
    void exportsVietnameseWideAndMultiPageMatrixWithoutDuplicateCourses() throws Exception {
        SyllabusPdfFontProvider fonts = testFonts();
        DashboardHeatmapResponse matrix = matrix(18, 55);

        // Defensive duplicate row: must not appear twice in the PDF.
        List<DashboardHeatmapResponse.CourseCoverage> duplicated = new ArrayList<>(matrix.getCourseCoverages());
        duplicated.add(matrix.getCourseCoverages().get(0));
        matrix.setCourseCoverages(duplicated);

        byte[] content = CloPloMatrixPdfExporter.export(matrix, fonts);

        assertThat(content).startsWith("%PDF".getBytes());
        assertThat(content.length).isGreaterThan(4_000);

        PdfReader reader = new PdfReader(content);
        try {
            // 18 PLO columns are split into 3 horizontal sections, and 55 courses
            // also force vertical pagination.
            assertThat(reader.getNumberOfPages()).isGreaterThanOrEqualTo(3);

            StringBuilder text = new StringBuilder();
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page)).append('\n');
            }

            String allText = text.toString();
            assertThat(allText)
                    .contains("MA TRẬN CLO–PLO TOÀN CHƯƠNG TRÌNH ĐÀO TẠO")
                    .contains("Khoa học Máy tính")
                    .contains("Khóa tuyển sinh")
                    .contains("Nhập môn Tin học")
                    .contains("Học kỳ 1")
                    .contains("PLO1")
                    .contains("PLO18");

            // Course IT001 is repeated once per horizontal section, not twice
            // because of the duplicate source row.
            assertThat(countOccurrences(allText, "IT001")).isEqualTo(3);
        } finally {
            reader.close();
        }
    }

    @Test
    void rejectsNonUnicodeFallbackFont() {
        SyllabusPdfFontProvider nonUnicode = new SyllabusPdfFontProvider(
                "Z:/missing/font.ttf",
                "Z:/missing/font-bold.ttf");

        // On machines that have a standard system Unicode font, the provider will
        // still resolve it. This assertion only applies when no Unicode candidate exists.
        if (!nonUnicode.isUnicodeReady()) {
            assertThatThrownBy(() -> CloPloMatrixPdfExporter.export(matrix(2, 1), nonUnicode))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("font Unicode");
        }
    }

    private static SyllabusPdfFontProvider testFonts() {
        List<Path[]> candidates = List.of(
                new Path[]{Path.of("C:/Windows/Fonts/arial.ttf"), Path.of("C:/Windows/Fonts/arialbd.ttf")},
                new Path[]{Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"), Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf")},
                new Path[]{Path.of("/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf"), Path.of("/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf")}
        );
        for (Path[] candidate : candidates) {
            if (Files.isRegularFile(candidate[0]) && Files.isRegularFile(candidate[1])) {
                SyllabusPdfFontProvider provider = new SyllabusPdfFontProvider(
                        candidate[0].toString(),
                        candidate[1].toString());
                assertThat(provider.isUnicodeReady()).isTrue();
                return provider;
            }
        }
        throw new IllegalStateException("Máy chạy test không có font Unicode hỗ trợ.");
    }

    private static DashboardHeatmapResponse matrix(int ploCount, int courseCount) {
        DashboardHeatmapResponse matrix = new DashboardHeatmapResponse();
        matrix.setProgramId(1);
        matrix.setProgramCode("CS-2021");
        matrix.setProgramName("Computer Science");
        matrix.setProgramNameVn("Khoa học Máy tính");
        matrix.setCohortId(2);
        matrix.setCohortName("CS2021");
        matrix.setAcademicYear("2026-2027");
        matrix.setSemester("1");
        matrix.setCourseTypeNameVn("Tất cả");
        matrix.setDataSource("Canonical heatmap query");
        matrix.setScopeKey("program=1|cohort=2|academicYear=2026-2027|semester=1|courseType=ALL");

        DashboardHeatmapResponse.HeatmapSummary summary = new DashboardHeatmapResponse.HeatmapSummary();
        summary.setTotalCourses(courseCount);
        summary.setCoursesWithApprovedSyllabus(courseCount);
        summary.setPloCoveragePercentage(100.0);
        summary.setCloMappingPercentage(92.5);
        matrix.setSummary(summary);

        List<DashboardHeatmapResponse.PloColumn> plos = new ArrayList<>();
        for (int index = 1; index <= ploCount; index++) {
            DashboardHeatmapResponse.PloColumn plo = new DashboardHeatmapResponse.PloColumn();
            plo.setId(100 + index);
            plo.setCode("PLO" + index);
            plos.add(plo);
        }
        matrix.setPloDetails(plos);

        List<DashboardHeatmapResponse.CourseCoverage> courses = new ArrayList<>();
        for (int index = 1; index <= courseCount; index++) {
            DashboardHeatmapResponse.CourseCoverage course = new DashboardHeatmapResponse.CourseCoverage();
            course.setCourseId(index);
            course.setCourseCode(String.format("IT%03d", index));
            course.setCourseNameVn(index == 1 ? "Nhập môn Tin học" : "Học phần tiếng Việt số " + index);
            course.setCourseTypeNameVn(index % 2 == 0 ? "Môn tự chọn" : "Môn bắt buộc");
            course.setSyllabusVersionLabel("v2.0");
            course.setHasApprovedSyllabus(true);

            List<DashboardHeatmapResponse.CellCoverage> cells = new ArrayList<>();
            for (DashboardHeatmapResponse.PloColumn plo : plos) {
                DashboardHeatmapResponse.CellCoverage cell = new DashboardHeatmapResponse.CellCoverage();
                cell.setPloId(plo.getId());
                cell.setPloCode(plo.getCode());
                cell.setLevel((plo.getId() + index) % 3 == 0 ? "A" : ((plo.getId() + index) % 2 == 0 ? "D" : "I"));
                cells.add(cell);
            }
            course.setCells(cells);
            courses.add(course);
        }
        matrix.setCourseCoverages(courses);
        return matrix;
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int position = 0;
        while ((position = text.indexOf(needle, position)) >= 0) {
            count++;
            position += needle.length();
        }
        return count;
    }
}
