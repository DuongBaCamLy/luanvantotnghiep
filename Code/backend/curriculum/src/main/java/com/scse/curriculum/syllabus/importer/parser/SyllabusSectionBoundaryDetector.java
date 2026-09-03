package com.scse.curriculum.syllabus.importer.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Detects syllabus boundaries in programme PDF documents by layout markers,
 * not by cohort/year.
 *
 * IMPORTANT:
 * - This class only detects page boundaries.
 * - It does not write to the database.
 * - It does not replace the existing syllabus field parser.
 */
public final class SyllabusSectionBoundaryDetector {

    private static final int FLAGS =
            Pattern.CASE_INSENSITIVE
                    | Pattern.MULTILINE
                    | Pattern.UNICODE_CASE;

    /**
     * Existing/legacy English format:
     *   Course Name: ...
     *
     * Newer format:
     *   1. Course Name: ...
     *   2. Course Name: ...
     *   12) Course Name: ...
     */
    private static final Pattern ENGLISH_COURSE_NAME = Pattern.compile(
            "^\\s*(?:\\d{1,3}\\s*[.)-]\\s*)?Course\\s+Name\\s*:",
            FLAGS);

    private static final Pattern ENGLISH_COURSE_CODE = Pattern.compile(
            "^\\s*Course\\s+Code\\s*:",
            FLAGS);

    /**
     * Optional Vietnamese fallback for programme dossiers.
     * This is only boundary detection. Existing field parsing remains unchanged.
     */
    private static final Pattern VIETNAMESE_COURSE_NAME = Pattern.compile(
            "^\\s*(?:\\d{1,3}\\s*[.)-]\\s*)?"
                    + "(?:Tên\\s+môn\\s+học|Tên\\s+học\\s+phần)"
                    + "(?:\\s*\\([^\\r\\n)]*\\))?\\s*[:;]",
            FLAGS);

    private static final Pattern VIETNAMESE_COURSE_CODE = Pattern.compile(
            "^\\s*(?:Mã\\s+số\\s+môn\\s+học|Mã\\s+học\\s+phần)\\s*[:;]",
            FLAGS);

    public DetectionResult detect(List<String> pageTexts) {

        if (pageTexts == null || pageTexts.isEmpty()) {
            return new DetectionResult(
                    "UNKNOWN",
                    List.of(),
                    0);
        }

        List<Integer> englishStarts = collectStarts(
                pageTexts,
                ENGLISH_COURSE_NAME,
                ENGLISH_COURSE_CODE);

        /*
         * Prefer the English appendix when available.
         * This matches the existing DOCX import strategy and prevents
         * bilingual programme dossiers from importing a course twice.
         */
        if (!englishStarts.isEmpty()) {
            return new DetectionResult(
                    "COURSE_NAME_CODE_ENGLISH",
                    List.copyOf(englishStarts),
                    100);
        }

        List<Integer> vietnameseStarts = collectStarts(
                pageTexts,
                VIETNAMESE_COURSE_NAME,
                VIETNAMESE_COURSE_CODE);

        if (!vietnameseStarts.isEmpty()) {
            return new DetectionResult(
                    "COURSE_NAME_CODE_VIETNAMESE",
                    List.copyOf(vietnameseStarts),
                    70);
        }

        return new DetectionResult(
                "UNKNOWN",
                List.of(),
                0);
    }

    private List<Integer> collectStarts(
            List<String> pageTexts,
            Pattern courseNamePattern,
            Pattern courseCodePattern) {

        List<Integer> starts = new ArrayList<>();

        for (int pageIndex = 0; pageIndex < pageTexts.size(); pageIndex++) {

            String pageText =
                    pageTexts.get(pageIndex) == null
                            ? ""
                            : pageTexts.get(pageIndex);

            boolean hasCourseName =
                    courseNamePattern.matcher(pageText).find();

            boolean hasCourseCode =
                    courseCodePattern.matcher(pageText).find();

            if (hasCourseName && hasCourseCode) {
                /*
                 * All existing importer boundaries are 1-based PDF pages.
                 */
                starts.add(pageIndex + 1);
            }
        }

        return starts;
    }

    public record DetectionResult(
            String format,
            List<Integer> startPages,
            int confidence) {
    }
}
