package com.scse.curriculum.syllabus.importer.service;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Extracts official curriculum course codes from programme-document course-list
 * tables.  The extractor deliberately ignores later brief descriptions and
 * detailed syllabus appendices.
 *
 * Supported table markers include common Vietnamese IU programme templates:
 *   - "Các môn học thuộc CTĐT"
 *   - "Các học phần thuộc CTĐT"
 *   - "Các học phần thuộc chương trình đào tạo"
 *
 * It stops before the teaching-plan / course-description sections.
 *
 * If no reliable table window is found, an empty set is returned so callers can
 * fail open and preserve the previous import behaviour.
 */
final class ProgramCurriculumScopeExtractor {

    private static final int MAX_SCAN_PAGES = 140;

    private static final Pattern COURSE_CODE = Pattern.compile(
            "(?<![A-Z0-9])([A-Z]{2,4})\\s*[- ]?\\s*(\\d{2,3})\\s*(I\\s*U|W\\s*E)?(?![A-Z0-9])",
            Pattern.CASE_INSENSITIVE);

    private ProgramCurriculumScopeExtractor() {
    }

    static Set<String> extractNormalizedCourseCodes(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            return Set.of();
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            StringBuilder scopedText = new StringBuilder();

            boolean started = false;
            boolean finished = false;
            int maxPage = Math.min(document.getNumberOfPages(), MAX_SCAN_PAGES);

            for (int page = 1; page <= maxPage && !finished; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document);

                for (String line : pageText.split("\\R")) {
                    String normalizedLine = normalizeMarkerText(line);

                    if (!started && isStartMarker(normalizedLine)) {
                        started = true;
                    }

                    if (started && isStopMarker(normalizedLine)) {
                        finished = true;
                        break;
                    }

                    if (started) {
                        scopedText.append(line).append('\n');
                    }
                }
            }

            if (!started || scopedText.length() == 0) {
                return Set.of();
            }

            Set<String> codes = new LinkedHashSet<>();
            Matcher matcher = COURSE_CODE.matcher(scopedText);
            while (matcher.find()) {
                String prefix = matcher.group(1).toUpperCase(Locale.ROOT);
                String digits = matcher.group(2);
                String suffix = matcher.group(3) == null
                        ? ""
                        : matcher.group(3).replaceAll("\\s+", "").toUpperCase(Locale.ROOT);

                String code = prefix + digits + suffix;
                codes.add(normalizeLikeImporter(code));
            }

            return codes;
        } catch (IOException exception) {
            return Set.of();
        }
    }

    private static boolean isStartMarker(String value) {
        return value.contains("các môn học thuộc ctđt")
                || value.contains("các học phần thuộc ctđt")
                || value.contains("các học phần thuộc chương trình đào tạo")
                || value.contains("courses in the curriculum")
                || value.contains("courses of the curriculum")
                || value.contains("curriculum course list");
    }

    private static boolean isStopMarker(String value) {
        return value.contains("dự kiến kế hoạch giảng dạy")
                || value.contains("kế hoạch giảng dạy theo từng học kỳ")
                || value.contains("planned teaching schedule")
                || value.contains("mô tả vắn tắt nội dung và khối lượng các học phần")
                || value.contains("course descriptions");
    }

    private static String normalizeMarkerText(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT)
                        .replace('\u00A0', ' ')
                        .replaceAll("\\s+", " ")
                        .trim();
    }

    private static String normalizeLikeImporter(String value) {
        String normalized = value == null
                ? ""
                : value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");

        return normalized.endsWith("IU")
                ? normalized.substring(0, normalized.length() - 2)
                : normalized;
    }
}
