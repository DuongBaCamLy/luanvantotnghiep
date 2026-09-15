package com.scse.curriculum.syllabus.importer.parser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects syllabus boundaries in programme PDF documents by layout markers,
 * not by cohort/year.
 *
 * IMPORTANT:
 * - A single programme document may MIX multiple syllabus header formats.
 * - Therefore detection is additive: all supported boundary strategies are
 *   evaluated page-by-page and their results are merged/deduplicated.
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
     * Existing / labelled English format:
     *
     *   Course Name: ...
     *   Course Code: ...
     *
     * Numbered variant:
     *
     *   7. Course Name: ...
     *   Course Code: ...
     */
    private static final Pattern ENGLISH_COURSE_NAME = Pattern.compile(
            "^\\s*(?:\\d{1,3}\\s*[.)-]\\s*)?Course\\s+Name\\s*:",
            FLAGS);

    private static final Pattern ENGLISH_COURSE_CODE = Pattern.compile(
            "^\\s*Course\\s+Code\\s*:",
            FLAGS);

    /**
     * Vietnamese labelled variants.
     */
    private static final Pattern VIETNAMESE_COURSE_NAME = Pattern.compile(
            "^\\s*(?:\\d{1,3}\\s*[.)-]\\s*)?"
                    + "(?:Tên\\s+môn\\s+học|Tên\\s+học\\s+phần)"
                    + "(?:\\s*\\([^\\r\\n)]*\\))?\\s*[:;]",
            FLAGS);

    private static final Pattern VIETNAMESE_COURSE_CODE = Pattern.compile(
            "^\\s*(?:Mã\\s+số\\s+môn\\s+học|Mã\\s+học\\s+phần)\\s*[:;]",
            FLAGS);

    /**
     * Compact SCSE/IU syllabus format found inside the same CS2026 dossier:
     *
     *   COURSE SYLLABUS
     *   6. GENERAL LAW
     *   PE021IU
     *   General information
     *
     * It intentionally requires the COURSE SYLLABUS marker plus a numbered
     * title and a standalone course code. This avoids treating curriculum
     * summary tables as syllabus boundaries.
     */
    private static final Pattern COURSE_SYLLABUS_MARKER = Pattern.compile(
            "^\\s*COURSE\\s+SYLLABUS\\s*$",
            FLAGS);

    private static final Pattern NUMBERED_TITLE = Pattern.compile(
            "^\\s*\\d{1,3}\\s*[.)-]\\s*(.+?)\\s*$",
            FLAGS);

    private static final Pattern STANDALONE_COURSE_CODE = Pattern.compile(
            "^\\s*([A-Z]{2,4}\\s*[- ]?\\s*\\d{2,3}(?:\\s*(?:IU|WE))?)\\s*$",
            FLAGS);

    public DetectionResult detect(List<String> pageTexts) {

        if (pageTexts == null || pageTexts.isEmpty()) {
            return new DetectionResult(
                    "UNKNOWN",
                    List.of(),
                    0);
        }

        Set<Integer> starts =
                new LinkedHashSet<>();

        Set<String> formats =
                new LinkedHashSet<>();

        for (int pageIndex = 0;
             pageIndex < pageTexts.size();
             pageIndex++) {

            String pageText =
                    pageTexts.get(pageIndex) == null
                            ? ""
                            : pageTexts.get(pageIndex);

            int pageNumber =
                    pageIndex + 1;

            boolean englishLabelled =
                    ENGLISH_COURSE_NAME.matcher(pageText).find()
                            && ENGLISH_COURSE_CODE.matcher(pageText).find();

            if (englishLabelled) {
                starts.add(pageNumber);
                formats.add("COURSE_NAME_CODE_ENGLISH");
            }

            boolean vietnameseLabelled =
                    VIETNAMESE_COURSE_NAME.matcher(pageText).find()
                            && VIETNAMESE_COURSE_CODE.matcher(pageText).find();

            if (vietnameseLabelled) {
                starts.add(pageNumber);
                formats.add("COURSE_NAME_CODE_VIETNAMESE");
            }

            if (isCompactCourseSyllabusStart(pageText)) {
                starts.add(pageNumber);
                formats.add("COURSE_SYLLABUS_COMPACT");
            }
        }

        List<Integer> sortedStarts =
                starts.stream()
                        .sorted()
                        .toList();

        String format =
                formats.isEmpty()
                        ? "UNKNOWN"
                        : String.join("+", formats);

        int confidence =
                sortedStarts.isEmpty()
                        ? 0
                        : 100;

        return new DetectionResult(
                format,
                sortedStarts,
                confidence);
    }

    private boolean isCompactCourseSyllabusStart(
            String pageText) {

        if (!COURSE_SYLLABUS_MARKER
                .matcher(pageText)
                .find()) {
            return false;
        }

        String[] lines =
                pageText.split("\\R");

        int markerIndex =
                -1;

        for (int index = 0;
             index < lines.length;
             index++) {

            if (COURSE_SYLLABUS_MARKER
                    .matcher(lines[index])
                    .find()) {

                markerIndex = index;
                break;
            }
        }

        if (markerIndex < 0) {
            return false;
        }

        boolean numberedTitleFound =
                false;

        boolean courseCodeFound =
                false;

        /*
         * Header information should be close to COURSE SYLLABUS.
         * Limiting the search window prevents unrelated codes farther down
         * the page from becoming false boundaries.
         */
        int end =
                Math.min(
                        lines.length,
                        markerIndex + 12);

        for (int index = markerIndex + 1;
             index < end;
             index++) {

            String line =
                    lines[index] == null
                            ? ""
                            : lines[index].trim();

            if (line.isBlank()) {
                continue;
            }

            if (!numberedTitleFound
                    && NUMBERED_TITLE
                            .matcher(line)
                            .matches()) {

                numberedTitleFound = true;
                continue;
            }

            if (numberedTitleFound
                    && STANDALONE_COURSE_CODE
                            .matcher(line)
                            .matches()) {

                courseCodeFound = true;
                break;
            }
        }

        return numberedTitleFound
                && courseCodeFound;
    }

    public record DetectionResult(
            String format,
            List<Integer> startPages,
            int confidence) {
    }
}
