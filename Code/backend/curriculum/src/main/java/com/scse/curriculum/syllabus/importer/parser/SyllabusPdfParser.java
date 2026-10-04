package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.syllabus.importer.dto.AssessmentImportData;
import com.scse.curriculum.syllabus.importer.dto.CloImportData;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportIssue;
import com.scse.curriculum.syllabus.importer.dto.TopicImportData;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the editable SCSE syllabus form from the standard CS programme PDF.
 */
@Component
@Slf4j
public class SyllabusPdfParser implements SyllabusFileParser {

        private final SyllabusSectionBoundaryDetector boundaryDetector = new SyllabusSectionBoundaryDetector();
private final PdfOcrFallback ocrFallback =
        new PdfOcrFallback();
        private static final Pattern CONTENT_ROW = Pattern.compile(
                        "(?m)^(.+?)[ \\t]+(\\d+(?:\\.\\d+)?)[ \\t]+((?:I|T|U)(?:[ \\t]*,[ \\t]*(?:I|T|U))*)[ \\t]*$");

        private static final Pattern ASSESSMENT_ROW = Pattern.compile(
                        "(?im)^(.+?)"
                                        + "\\s*\\((\\d+(?:\\.\\d+)?)%\\)"
                                        + "([^\\r\\n]*)$");

        public SyllabusImportData parse(MultipartFile file) {
                List<SyllabusImportIssue> issues = new ArrayList<>();
                try (InputStream input = file.getInputStream()) {
                        return parsePdf(input, issues);
                } catch (IOException exception) {
                        throw new IllegalArgumentException("Cannot read syllabus PDF", exception);
                }
        }

        @Override
        public boolean supports(String fileName, String contentType) {
                return (fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".pdf"))
                                || "application/pdf".equalsIgnoreCase(contentType);
        }

        @Override
        public SyllabusImportData parse(InputStream input, List<SyllabusImportIssue> issues) throws IOException {
                return parsePdf(input, issues);
        }

        public SyllabusImportData parsePdf(
                        InputStream inputStream,
                        List<SyllabusImportIssue> issues) throws IOException {

                try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
                        PDFTextStripper stripper = new PDFTextStripper();
                        stripper.setSortByPosition(true);
                        String text = cleanSourceText(stripper.getText(document));
                        return parseText(text, issues);
                }
        }

        /**
         * Existing public API retained for backward compatibility.
         */
        public List<ParsedSyllabusSection> parsePdfBatch(
                        InputStream inputStream) throws IOException {

                return parsePdfBatchWithMetadata(inputStream).sections();
        }

        /**
         * Reads the real PDF page count and detects syllabus boundaries using
         * layout markers. The existing parseText(...) logic is intentionally
         * preserved for all field extraction.
         */
        public ParsedPdfBatch parsePdfBatchWithMetadata(
                        InputStream inputStream) throws IOException {

                try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {

                        PDFTextStripper stripper = new PDFTextStripper();

                        stripper.setSortByPosition(true);

                        int pageCount = document.getNumberOfPages();

                        List<String> pageTexts = new ArrayList<>(pageCount);

                        /*
                         * Phase 1 only changes boundary detection.
                         * Each page is extracted once for detection.
                         */
                        for (int page = 1; page <= pageCount; page++) {

                                stripper.setStartPage(page);
                                stripper.setEndPage(page);

                                pageTexts.add(
                                                cleanSourceText(
                                                                stripper.getText(document)));
                        }

                        SyllabusSectionBoundaryDetector.DetectionResult detection = boundaryDetector.detect(pageTexts);

                        List<Integer> starts = detection.startPages();

                        log.info(
                                        "Program PDF detected format={} confidence={} pages={} syllabusStarts={}",
                                        detection.format(),
                                        detection.confidence(),
                                        pageCount,
                                        starts.size());

                        List<ParsedSyllabusSection> result = new ArrayList<>();

                        for (int index = 0; index < starts.size(); index++) {

                                int startPage = starts.get(index);

                                int endPage = index + 1 < starts.size()
                                                ? starts.get(index + 1) - 1
                                                : pageCount;

                                stripper.setStartPage(startPage);
                                stripper.setEndPage(endPage);

                                List<SyllabusImportIssue> issues = new ArrayList<>();

                                /*
                                 * KEEP the current field extraction behavior.
                                 * No CLO/topic/assessment/general-info parsing code is replaced.
                                 */
                                String sectionText =
        cleanSourceText(
                stripper.getText(document));

SyllabusImportData data =
        parseText(
                sectionText,
                issues);

/*
 * Automatic OCR fallback for scanned/image-only syllabus.
 *
 * IMPORTANT:
 * - normal PDF text remains authoritative;
 * - OCR runs only when structured extraction is effectively empty;
 * - use OCR only when it recovers more structured evidence.
 */
if (needsOcrFallback(
        sectionText,
        data)) {

    try {

        String ocrText =
                cleanSourceText(
                        ocrFallback.extract(
                                document,
                                startPage,
                                endPage));

        List<SyllabusImportIssue> ocrIssues =
                new ArrayList<>();

        SyllabusImportData ocrData =
                parseText(
                        ocrText,
                        ocrIssues);

        if (structuredScore(ocrData)
                > structuredScore(data)) {

            /*
             * Preserve already-detected identity if OCR
             * could not read it reliably.
             */
            /*
 * Identity from the normal PDF/boundary extraction is authoritative.
 *
 * OCR is only allowed to recover syllabus CONTENT.
 * It must never replace an already-detected course code/name because
 * OCR can confuse characters such as IU -> TU.
 */
if (data.getSourceCourseCode() != null
        && !data.getSourceCourseCode().isBlank()) {

    ocrData.setSourceCourseCode(
            data.getSourceCourseCode());
}

if (data.getSourceCourseName() != null
        && !data.getSourceCourseName().isBlank()) {

    ocrData.setSourceCourseName(
            data.getSourceCourseName());
}

            data = ocrData;

            issues.clear();
            issues.addAll(ocrIssues);

            log.info(
                    "OCR fallback recovered syllabus pages {}-{} code={}",
                    startPage,
                    endPage,
                    data.getSourceCourseCode());
        }

    } catch (IOException exception) {

        log.warn(
                "OCR fallback failed for pages {}-{}: {}",
                startPage,
                endPage,
                exception.getMessage());
    }
}

                                /*
                                 * PATCH_3_2F2_POSITIONAL_WEEKLY
                                 *
                                 * The flattened PDF text does not preserve table columns.
                                 * For real programme dossiers, recover the weekly table from
                                 * TextPosition geometry. No value is invented: if positional
                                 * evidence is unavailable, the existing text parser remains
                                 * the compatibility fallback.
                                 */
                                if (applyPositionalWeeklyPlan(
                                                document,
                                                startPage,
                                                endPage,
                                                data)) {

                                        sanitizeImportedData(data);

                                        data.setTemplateSections(
                                                        SyllabusTemplateSectionDetector.fromPdfText(
                                                                        sectionText,
                                                                        data));
                                }

                                result.add(
                                                new ParsedSyllabusSection(
                                                                startPage,
                                                                endPage,
                                                                data,
                                                                issues));
                        }

                        return new ParsedPdfBatch(
                                        pageCount,
                                        List.copyOf(result));
                }
        }

        public record ParsedPdfBatch(
                        int pageCount,
                        List<ParsedSyllabusSection> sections) {
        }

        private SyllabusImportData parseText(String text, List<SyllabusImportIssue> issues) {
                if (text.isBlank()) {
                        issues.add(issue("ERROR", "File", null, "file", "PDF does not contain extractable text."));
                        return SyllabusImportData.builder().build();
                }
                SyllabusImportData data = SyllabusImportData.builder().build();
                parseGeneralInformation(text, data);
                parseClos(text, data);
                parseContent(text, data);
                parseWeeklyActivities(text, data);
                parseCloPloMatrix(text, data);
                parseAssessments(text, data);
                parseReadingList(text, data);
                parseRubrics(text, data);
                parseRevisionDate(text, data);
                sanitizeImportedData(data);

                /*
                 * Preserve the recognized section/field shape of this exact PDF
                 * syllabus. The comparison page later uses this profile instead of
                 * assuming one fixed template for every cohort.
                 */
                data.setTemplateSections(
                                SyllabusTemplateSectionDetector.fromPdfText(
                                                text,
                                                data));

                validateCoreSections(data, issues);
                return data;
        }

        public record ParsedSyllabusSection(
                        int startPage,
                        int endPage,
                        SyllabusImportData data,
                        List<SyllabusImportIssue> issues) {
        }

        private String cleanSourceText(String value) {

                String cleaned = value == null
                                ? ""
                                : value.replace('\u00a0', ' ')
                                                .replace("\r\n", "\n")
                                                .replace('\r', '\n');

                return stripPdfPageArtifacts(cleaned);
        }

        String stripPdfPageArtifacts(String value) {

                if (value == null || value.isBlank()) {
                        return value == null ? "" : value;
                }

                String cleaned = value.replace("\r\n", "\n")
                                .replace('\r', '\n');

                /*
                 * Diagnostic page marker sometimes present in extracted fixtures.
                 */
                cleaned = cleaned.replaceAll(
                                "(?im)^\\s*<PARSED\\s+TEXT\\s+FOR\\s+PAGE:\\s*"
                                                + "\\d{1,4}\\s*/\\s*\\d{1,5}>\\s*$",
                                "");

                /*
                 * Example:
                 *
                 * dynamic data types.44
                 * 44 / 517
                 *
                 * becomes:
                 *
                 * dynamic data types.
                 */
                cleaned = cleaned.replaceAll(
                                "(?m)(?<=\\D)(\\d{1,4})[ \\t]*\\n"
                                                + "[ \\t]*\\1[ \\t]+/[ \\t]+\\d{1,5}"
                                                + "[ \\t]*(?=\\n|$)",
                                "");

                /*
                 * Remaining standalone page footer.
                 *
                 * Requiring spaces around '/' keeps values such as 50/100 intact.
                 */
                cleaned = cleaned.replaceAll(
                                "(?m)^[ \\t]*\\d{1,4}[ \\t]+/[ \\t]+"
                                                + "\\d{1,5}[ \\t]*(?=\\n|$)",
                                "");

                cleaned = cleaned.replaceAll(
                                "(?m)^[ \\t]+$",
                                "");

                cleaned = cleaned.replaceAll(
                                "\\n{3,}",
                                "\n\n");

                return cleaned;
        }

        private void parseGeneralInformation(String text, SyllabusImportData data) {
                String flat = collapse(text);

                String sourceCourseName = capture(
                                flat,
                                "Course Name:\\s*(.*?)\\s+Course Code:");

                String sourceCourseCode = capture(
                                flat,
                                "Course Code:\\s*([A-Za-z0-9._/-]+)");

                /*
                 * Some older programme documents contain complete Vietnamese syllabus
                 * sections whose identity is expressed as:
                 *
                 * Tên môn học (tiếng Anh): ...
                 * Mã số môn học: PE015IU
                 *
                 * They are real syllabus boundaries, not false positives. Parse that
                 * identity before falling back to the compact SCSE/IU header layout.
                 */
                if (sourceCourseName == null
                                || sourceCourseCode == null) {

                        VietnameseCourseHeader vietnameseHeader = parseVietnameseCourseHeader(text);

                        if (sourceCourseName == null) {
                                sourceCourseName = vietnameseHeader.courseName();
                        }

                        if (sourceCourseCode == null) {
                                sourceCourseCode = vietnameseHeader.courseCode();
                        }
                }

                /*
                 * Mixed-format programme dossiers may also contain a compact SCSE/IU
                 * syllabus header without explicit "Course Name:" / "Course Code:"
                 * labels, for example:
                 *
                 * COURSE SYLLABUS
                 * 6. GENERAL LAW
                 * PE021IU
                 * General information
                 *
                 * Existing English-labelled extraction remains first priority,
                 * Vietnamese-labelled extraction is second, and this compact layout
                 * is the final identity fallback.
                 */
                if (sourceCourseName == null
                                || sourceCourseCode == null) {

                        CompactCourseHeader compactHeader = parseCompactCourseHeader(text);

                        if (sourceCourseName == null) {
                                sourceCourseName = compactHeader.courseName();
                        }

                        if (sourceCourseCode == null) {
                                sourceCourseCode = compactHeader.courseCode();
                        }
                }

                data.setSourceCourseName(
                                sourceCourseName);

                data.setSourceCourseCode(
                                sourceCourseCode);
                // General-information labels and values wrap differently from course to course.
                // Bound each value by the next semantic label instead of stopping at a physical
                // line.
                data.setCourseDesignation(parseCourseDesignation(text));
                data.setSemester(
                                parseSemester(text));
                data.setPersonResponsible(parsePersonResponsible(text));
                data.setLanguage(parseLanguage(text));
                data.setRelation(parseRelation(text));
                data.setTeachingMethods(parseTeachingMethods(text));

                /*
                 * Workload tables vary heavily between schools/templates. Never search for
                 * the generic phrase "Contact hours" from the whole syllabus because it also
                 * appears inside the wrapper label "Workload (incl. contact hours, self-study
                 * hours)". Parse the semantic workload block in-order instead.
                 */
                parseWorkload(text, data);

                data.setCreditPoints(
                                parseCreditPoints(text));

                data.setLectureCredits(
                                parseLectureCredits(text));

                data.setLaboratoryCredits(
                                parseLaboratoryCredits(text));
                data.setPrerequisites(
                                parsePrerequisites(text));
                data.setObjectives(parseObjectives(text));
                parseExaminationFields(text, data);
                data.setContentNote(
                                parseContentNote(text, flat));
                data.setAssessmentPassNote(
                                parseAssessmentPassNote(text, flat));

                String relation = data.getRelation();
                if (relation != null) {
                        if (containsIgnoreCase(relation, "compulsory") || containsIgnoreCase(relation, "required")) {
                                data.setCourseTypes("Compulsory");
                        } else if (containsIgnoreCase(relation, "elective")
                                        || containsIgnoreCase(relation, "optional")) {
                                data.setCourseTypes("Elective");
                        } else if (containsIgnoreCase(relation, "general")) {
                                data.setCourseTypes("General");
                        }
                }
        }

        void parseExaminationFields(
                        String text,
                        SyllabusImportData data) {

                LabelMatch examinationHeading = firstFlexibleLabel(
                                text,
                                0,
                                "Examination forms",
                                "Examination methods");

                if (examinationHeading == null) {
                        data.setExamForms(null);
                        data.setExamRequirements(null);
                        return;
                }

                int examinationStart = examinationHeading.end();

                LabelMatch examinationEndHeading = firstFlexibleLabel(
                                text,
                                examinationStart,
                                "Reading list",
                                "References",
                                "Bibliography",
                                "Rubrics (optional)",
                                "Rubrics");

                int examinationEnd = examinationEndHeading == null
                                ? text.length()
                                : examinationEndHeading.start();

                /*
                 * Keep physical PDFBox lines until the requirements boundary has been
                 * recognized. Some real CS2026 pages interleave the wrapped table label:
                 *
                 * Attendance: ...
                 * Study and
                 * ...
                 * examination
                 * ...
                 * requirements
                 */
                String rawExaminationBlock = text.substring(
                                examinationStart,
                                examinationEnd);

                String examinationBlock = collapseToNull(
                                rawExaminationBlock);

                if (examinationBlock == null) {
                        data.setExamForms(null);
                        data.setExamRequirements(null);
                        return;
                }

                /*
                 * Normal semantic layouts.
                 */
                LabelMatch requirementsHeading = firstFlexibleLabel(
                                rawExaminationBlock,
                                0,
                                "Study and examination requirements",
                                "Examination requirements");

                if (requirementsHeading != null) {

                        String examForms = collapseToNull(
                                        rawExaminationBlock.substring(
                                                        0,
                                                        requirementsHeading.start()));

                        String examRequirements = collapseToNull(
                                        rawExaminationBlock.substring(
                                                        requirementsHeading.end()));

                        data.setExamForms(examForms);
                        data.setExamRequirements(examRequirements);
                        return;
                }

                /*
                 * Real CS2026 two-column extraction, observed in IT120IU:
                 *
                 * Examination forms Multiple-choice...
                 * Attendance: ...
                 * Study and
                 * ...
                 * examination
                 * ...
                 * requirements
                 * Assignments/Examination: ...
                 *
                 * The three isolated words are table-label fragments. Attendance is the
                 * first semantic value of the Study / Examination Requirements column.
                 */
                Matcher attendanceFirstInterleaved = Pattern.compile(
                                "(?im)^\\s*Attendance\\s*:")
                                .matcher(
                                                rawExaminationBlock);

                if (attendanceFirstInterleaved.find()) {

                        String candidateRequirements = rawExaminationBlock.substring(
                                        attendanceFirstInterleaved.start());

                        boolean hasStudyFragment = Pattern.compile(
                                        "(?im)^\\s*Study\\s+and\\s*$")
                                        .matcher(candidateRequirements)
                                        .find();

                        boolean hasExaminationFragment = Pattern.compile(
                                        "(?im)^\\s*examination\\s*$")
                                        .matcher(candidateRequirements)
                                        .find();

                        boolean hasRequirementsFragment = Pattern.compile(
                                        "(?im)^\\s*requirements\\s*$")
                                        .matcher(candidateRequirements)
                                        .find();

                        if (hasStudyFragment
                                        && hasExaminationFragment
                                        && hasRequirementsFragment) {

                                String examForms = collapseToNull(
                                                rawExaminationBlock.substring(
                                                                0,
                                                                attendanceFirstInterleaved.start()));

                                String requirementsRaw = candidateRequirements
                                                .replaceAll(
                                                                "(?im)^\\s*Study\\s+and\\s*\\R?",
                                                                "")
                                                .replaceAll(
                                                                "(?im)^\\s*examination\\s*\\R?",
                                                                "")
                                                .replaceAll(
                                                                "(?im)^\\s*requirements\\s*\\R?",
                                                                "");

                                /*
                                 * IT120IU also has bibliography entries physically emitted before
                                 * the later "Reading list" label. Remove that spill only when the
                                 * numbered bibliography starts after the explicit passing sentence.
                                 */
                                int assignmentsIndex = indexOfIgnoreCase(
                                                requirementsRaw,
                                                "Assignments/Examination:");

                                if (assignmentsIndex >= 0) {

                                        Matcher possibleReadingSpill = Pattern.compile(
                                                        "(?m)^\\s*1\\.\\s+")
                                                        .matcher(
                                                                        requirementsRaw);

                                        if (possibleReadingSpill.find(assignmentsIndex)) {

                                                String passingBlock = requirementsRaw.substring(
                                                                assignmentsIndex,
                                                                possibleReadingSpill.start());

                                                boolean looksLikePassingRequirement = containsIgnoreCase(
                                                                passingBlock,
                                                                "pass this course")
                                                                || containsIgnoreCase(
                                                                                passingBlock,
                                                                                "points overall");

                                                if (looksLikePassingRequirement) {
                                                        requirementsRaw = requirementsRaw.substring(
                                                                        0,
                                                                        possibleReadingSpill.start());
                                                }
                                        }
                                }

                                String examRequirements = collapseToNull(
                                                requirementsRaw
                                                                .replaceAll(
                                                                                "(?i)\\bforthe\\b",
                                                                                "for the")
                                                                .replaceAll(
                                                                                "(?i)\\bsessions\\.(?=Students\\b)",
                                                                                "sessions. "));

                                data.setExamForms(examForms);
                                data.setExamRequirements(examRequirements);
                                return;
                        }
                }

                /*
                 * Existing interleaved PDFBox layout:
                 *
                 * Study and Attendance: ...
                 * examination compulsory ...
                 * requirements the basis ...
                 */
                Matcher interleavedHeading = Pattern.compile(
                                "\\bStudy\\s+and\\s+(?=Attendance\\s*:)",
                                Pattern.CASE_INSENSITIVE)
                                .matcher(
                                                examinationBlock);

                if (interleavedHeading.find()) {

                        String examForms = collapseToNull(
                                        examinationBlock.substring(
                                                        0,
                                                        interleavedHeading.start()));

                        String examRequirements = examinationBlock.substring(
                                        interleavedHeading.end());

                        examRequirements = examRequirements
                                        .replaceAll(
                                                        "(?i)\\bis\\s+examination\\s+compulsory\\b",
                                                        "is compulsory")
                                        .replaceAll(
                                                        "(?i)\\bon\\s+requirements\\s+the\\s+basis\\b",
                                                        "on the basis")
                                        .replaceAll(
                                                        "(?i)\\bfor\\s+the\\s+class\\s+requirements\\s+sessions\\b",
                                                        "for the class sessions")
                                        .replaceAll(
                                                        "(?i)\\bsessions\\.(?=Students\\b)",
                                                        "sessions. ")
                                        .replaceAll(
                                                        "(?i)\\bforthe\\b",
                                                        "for the");

                        data.setExamForms(examForms);
                        data.setExamRequirements(
                                        collapseToNull(examRequirements));
                        return;
                }

                /*
                 * Existing legacy CS2021 extraction. Preserve its behavior.
                 */
                Matcher legacyHeading = Pattern.compile(
                                "\\bStudy\\s+and\\s+examination\\b",
                                Pattern.CASE_INSENSITIVE)
                                .matcher(
                                                examinationBlock);

                if (legacyHeading.find()) {

                        String examForms = collapseToNull(
                                        examinationBlock.substring(
                                                        0,
                                                        legacyHeading.start()));

                        String examRequirements = examinationBlock.substring(
                                        legacyHeading.end());

                        examRequirements = examRequirements
                                        .replaceFirst(
                                                        "(?i)^\\s*requirements\\b\\s*",
                                                        "")
                                        .replaceAll(
                                                        "(?i)\\bis\\s+examination\\s+compulsory\\b",
                                                        "is compulsory")
                                        .replaceAll(
                                                        "(?i)\\bfor\\s+requirements\\s+the\\s+class\\s+sessions\\b",
                                                        "for the class sessions")
                                        .replaceAll(
                                                        "(?i)\\bfor\\s+the\\s+class\\s+requirements\\s+sessions\\b",
                                                        "for the class sessions")
                                        .replaceAll(
                                                        "(?i)\\bon\\s+requirements\\s+the\\s+basis\\b",
                                                        "on the basis")
                                        .replaceAll(
                                                        "(?i)\\bsessions\\.(?=Students\\b)",
                                                        "sessions. ")
                                        .replaceAll(
                                                        "(?i)\\bforthe\\b",
                                                        "for the");

                        data.setExamForms(examForms);
                        data.setExamRequirements(
                                        collapseToNull(examRequirements));
                        return;
                }

                data.setExamForms(examinationBlock);
                data.setExamRequirements(null);
        }

        private VietnameseCourseHeader parseVietnameseCourseHeader(
                        String text) {

                if (text == null
                                || text.isBlank()) {

                        return new VietnameseCourseHeader(
                                        null,
                                        null);
                }

                String englishName = capture(
                                text,
                                "(?m)^\\s*(?:Tên\\s+môn\\s+học|Tên\\s+học\\s+phần)"
                                                + "\\s*\\(\\s*tiếng\\s+Anh\\s*\\)"
                                                + "\\s*[:;]\\s*([^\\n]+)");

                String vietnameseName = capture(
                                text,
                                "(?m)^\\s*(?:Tên\\s+môn\\s+học|Tên\\s+học\\s+phần)"
                                                + "\\s*\\(\\s*tiếng\\s+Việt\\s*\\)"
                                                + "\\s*[:;]\\s*([^\\n]+)");

                String courseCode = capture(
                                text,
                                "(?m)^\\s*(?:Mã\\s+số\\s+môn\\s+học|Mã\\s+học\\s+phần)"
                                                + "\\s*[:;]\\s*([A-Za-z0-9._/-]+)");

                String courseName = englishName != null
                                ? englishName
                                : vietnameseName;

                return new VietnameseCourseHeader(
                                courseName,
                                courseCode);
        }

        private record VietnameseCourseHeader(
                        String courseName,
                        String courseCode) {
        }

        private void parseWorkload(
                        String text,
                        SyllabusImportData data) {

                LabelMatch teachingMethods = firstFlexibleLabel(
                                text,
                                0,
                                "Teaching methods",
                                "Instructional methods",
                                "Teaching methodology",
                                "Teaching and learning methods",
                                "Methods of instruction");

                int searchFrom = teachingMethods == null
                                ? 0
                                : teachingMethods.end();

                LabelMatch workloadLabel = firstFlexibleLabel(
                                text,
                                searchFrom,
                                "Workload",
                                "Study load",
                                "Study workload",
                                "Student workload");

                if (workloadLabel == null) {
                        return;
                }

                LabelMatch creditPoints = firstFlexibleLabel(
                                text,
                                workloadLabel.end(),
                                "Credit points",
                                "Number of credits",
                                "Credits");

                int end = creditPoints == null
                                ? Math.min(
                                                text.length(),
                                                workloadLabel.end() + 4000)
                                : creditPoints.start();

                String block = text.substring(
                                workloadLabel.end(),
                                end);

                /*
                 * PDFBox may interleave the two-column wrapper
                 *
                 * Workload (incl. contact hours, self-study hours)
                 *
                 * with the actual workload values. Remove only those known wrapper
                 * fragments before semantic label extraction.
                 */
                String flat = normalizeWorkloadBlock(
                                block);

                String workloadTotal = captureWorkloadValue(
                                flat,
                                "(?:\\(Estimated\\)\\s*)?Total\\s+workload\\s*:",
                                "Contact\\s+hours\\b",
                                "Private\\s+(?:study|hours)\\b",
                                "Student\\s+responsibility\\b",
                                "Number\\s+of\\s+credits\\b");

                /*
                 * Some layouts print simply:
                 *
                 * Workload: 135
                 * Contact hours: ...
                 *
                 * Because workloadLabel already consumed "Workload",
                 * the remaining block begins with ": 135".
                 */
                if (workloadTotal == null) {

                        Matcher leadingTotal = Pattern.compile(
                                        "^\\s*:\\s*"
                                                        + "(\\d+(?:\\.\\d+)?"
                                                        + "(?:\\s+hours?\\.?)?)"
                                                        + "\\s*(?=Contact\\s+hours\\b)",
                                        Pattern.CASE_INSENSITIVE)
                                        .matcher(flat);

                        if (leadingTotal.find()) {
                                workloadTotal = collapse(
                                                leadingTotal.group(1));
                        }
                }

                workloadTotal = cleanWorkloadValue(
                                workloadTotal);

                String workloadContact = captureWorkloadValue(
                                flat,
                                "Contact\\s+hours"
                                                + "(?:\\s*\\([^)]*\\))?"
                                                + "\\s*:",
                                "Private\\s+study\\b",
                                "Private\\s+hours\\b",
                                "Student\\s+responsibility\\b",
                                "Number\\s+of\\s+credits\\b");

                workloadContact = cleanWorkloadContact(
                                workloadContact);

                if (workloadContact != null
                                && !workloadContact.isBlank()) {

                        workloadContact = workloadContact
                                        .replaceFirst(
                                                        "\\s*:\\s*$",
                                                        "")
                                        .trim();
                }

                String workloadPrivate = captureWorkloadValue(
                                flat,
                                "(?:Private\\s+study"
                                                + "\\s+including\\s+examination\\s+preparation"
                                                + "\\s*,?\\s*specified\\s+in\\s+hours\\s*\\d*"
                                                + "|Private\\s+hours)"
                                                + "\\s*:",
                                "Student\\s+responsibility\\b",
                                "Number\\s+of\\s+credits\\b");

                workloadPrivate = cleanWorkloadValue(
                                workloadPrivate);

                String workloadStudentResponsibility = captureWorkloadValue(
                                flat,
                                "Student\\s+responsibility\\s*:",
                                "$");

                workloadStudentResponsibility = cleanWorkloadValue(
                                workloadStudentResponsibility);

                data.setWorkloadTotal(
                                workloadTotal);

                data.setWorkloadContact(
                                workloadContact);

                data.setWorkloadPrivate(
                                workloadPrivate);

                data.setWorkloadStudentResponsibility(
                                workloadStudentResponsibility);
        }

        private String normalizeWorkloadBlock(
                        String value) {

                if (value == null
                                || value.isBlank()) {
                        return "";
                }

                String cleaned = value.replace(
                                '\u00A0',
                                ' ');

                /*
                 * Remove an embedded/repeated wrapper header.
                 * Example:
                 *
                 * Workload (incl.
                 * laboratory session ... 45
                 * contact hours, self-
                 */
                cleaned = cleaned.replaceAll(
                                "(?i)\\bWorkload\\s*"
                                                + "\\(\\s*incl\\.?\\s*",
                                " ");

                /*
                 * The first "Workload" label has already been consumed by
                 * findFlexibleLabel(), so the block can begin directly with "(incl.".
                 */
                cleaned = cleaned.replaceFirst(
                                "(?i)^\\s*"
                                                + "\\(\\s*incl\\.?\\s*",
                                " ");

                /*
                 * Standard two-column wrapper fragment:
                 *
                 * contact hours, self-
                 */
                cleaned = cleaned.replaceAll(
                                "(?i)\\bcontact\\s+hours"
                                                + "\\s*,\\s*self\\s*[-–—]?\\s*",
                                " ");

                /*
                 * Another layout places "Total workload" between
                 * "contact hours," and "self-study hours)".
                 */
                cleaned = cleaned.replaceAll(
                                "(?i)\\bcontact\\s+hours"
                                                + "\\s*,\\s*"
                                                + "(?=(?:\\(Estimated\\)\\s*)?"
                                                + "Total\\s+workload\\s*:)",
                                " ");
                /*
                 * IT116/legacy two-column wrapper:
                 *
                 * Total workload: 195
                 * hours, self-study hours)
                 *
                 * Here "hours," belongs to the leaked wrapper text, so remove the
                 * complete suffix before the generic self-study wrapper cleanup.
                 */
                cleaned = cleaned.replaceAll(
                                "(?i)(?<=\\d)"
                                                + "\\s+hours\\s*,\\s*"
                                                + "self\\s*[-–—]?\\s*study"
                                                + "\\s+hours\\s*\\)",
                                "");
                cleaned = cleaned.replaceAll(
                                "(?i)\\bself\\s*[-–—]?\\s*"
                                                + "study\\s+hours\\s*\\)",
                                " ");

                /*
                 * Handles the second half of a split wrapper:
                 *
                 * Student
                 * study hours) responsibility:
                 *
                 * -> Student responsibility:
                 */
                cleaned = cleaned.replaceAll(
                                "(?i)\\bstudy\\s+hours\\s*\\)",
                                " ");

                return collapse(
                                cleaned);
        }

        private String cleanWorkloadValue(
                        String value) {

                if (value == null
                                || value.isBlank()) {
                        return null;
                }

                String cleaned = collapse(
                                value)

                                /*
                                 * Legacy/IT116 two-column wrapper:
                                 *
                                 * Total workload: 195
                                 * hours, self-study hours)
                                 *
                                 * The first "hours" belongs to the wrapper text, not to
                                 * the actual workload value. Remove the complete leaked
                                 * suffix so the source-faithful result remains "195".
                                 */
                                .replaceFirst(
                                                "(?i)\\s+hours\\s*,\\s*"
                                                                + "self\\s*[-–—]?\\s*study"
                                                                + "\\s+hours\\s*\\)\\s*$",
                                                "")

                                /*
                                 * Other CS2026 wrapper variants:
                                 *
                                 * 195 self-study hours)
                                 * 135 study hours)
                                 * 90 study hours)
                                 */
                                .replaceFirst(
                                                "(?i)\\s+"
                                                                + "(?:(?:self[-\\s]?study|study)"
                                                                + "\\s+hours)"
                                                                + "\\)\\s*$",
                                                "")
                                .trim();

                return cleaned.isBlank()
                                ? null
                                : cleaned;
        }

        private String captureWorkloadValue(
                        String source,
                        String startExpression,
                        String... endExpressions) {

                if (source == null
                                || source.isBlank()) {
                        return null;
                }

                Matcher start = Pattern.compile(
                                startExpression,
                                Pattern.CASE_INSENSITIVE)
                                .matcher(source);

                if (!start.find()) {
                        return null;
                }

                int end = source.length();

                for (String endExpression : endExpressions) {
                        if ("$".equals(endExpression)) {
                                continue;
                        }

                        Matcher candidate = Pattern.compile(
                                        endExpression,
                                        Pattern.CASE_INSENSITIVE)
                                        .matcher(source);

                        if (candidate.find(start.end())
                                        && candidate.start() < end) {
                                end = candidate.start();
                        }
                }

                String value = collapse(
                                source.substring(
                                                start.end(),
                                                end));

                /*
                 * Empty cells are valid in programme dossiers. If another semantic label
                 * follows immediately, return null instead of persisting the label itself.
                 */
                if (value.isBlank()
                                || Pattern.compile(
                                                "^(?:Contact\\s+hours|Private\\s+study|Student\\s+responsibility|Credit\\s+points)\\b",
                                                Pattern.CASE_INSENSITIVE)
                                                .matcher(value)
                                                .find()) {
                        return null;
                }

                return value;
        }

        private CompactCourseHeader parseCompactCourseHeader(
                        String text) {

                if (text == null
                                || text.isBlank()) {

                        return new CompactCourseHeader(
                                        null,
                                        null);
                }

                String[] lines = text.split("\\R");

                Pattern marker = Pattern.compile(
                                "^\\s*COURSE\\s+SYLLABUS\\s*$",
                                Pattern.CASE_INSENSITIVE);

                Pattern numberedTitle = Pattern.compile(
                                "^\\s*\\d{1,3}\\s*[.)-]\\s*(.+?)\\s*$",
                                Pattern.CASE_INSENSITIVE);

                Pattern standaloneCode = Pattern.compile(
                                "^\\s*([A-Z]{2,4}\\s*[- ]?\\s*\\d{2,3}(?:\\s*(?:IU|WE))?)\\s*$",
                                Pattern.CASE_INSENSITIVE);

                for (int markerIndex = 0; markerIndex < lines.length; markerIndex++) {

                        if (!marker.matcher(
                                        nullToEmpty(
                                                        lines[markerIndex]))
                                        .matches()) {

                                continue;
                        }

                        String courseName = null;

                        String courseCode = null;

                        int end = Math.min(
                                        lines.length,
                                        markerIndex + 12);

                        for (int index = markerIndex + 1; index < end; index++) {

                                String line = collapse(
                                                lines[index]);

                                if (line.isBlank()) {
                                        continue;
                                }

                                if (courseName == null) {
                                        Matcher titleMatcher = numberedTitle.matcher(
                                                        line);

                                        if (titleMatcher.matches()) {
                                                courseName = collapseToNull(
                                                                titleMatcher.group(1));

                                                continue;
                                        }
                                }

                                if (courseName != null
                                                && courseCode == null) {

                                        Matcher codeMatcher = standaloneCode.matcher(
                                                        line);

                                        if (codeMatcher.matches()) {
                                                courseCode = collapseToNull(
                                                                codeMatcher.group(1));

                                                break;
                                        }
                                }
                        }

                        if (courseName != null
                                        && courseCode != null) {

                                return new CompactCourseHeader(
                                                courseName,
                                                courseCode);
                        }
                }

                return new CompactCourseHeader(
                                null,
                                null);
        }

        private record CompactCourseHeader(
                        String courseName,
                        String courseCode) {
        }

        private String parseCreditPoints(
                        String text) {

                return capture(
                                text,
                                "(?m)^\\s*"
                                                + "(?:"
                                                + "Credit\\s+points\\s+Number\\s+of\\s+credits"
                                                + "|Credit\\s+points"
                                                + "|Credits"
                                                + ")"
                                                + "\\s*:\\s*"
                                                + "([^\\r\\n]+)");
        }

        private String parseLectureCredits(
                        String text) {

                return capture(
                                text,
                                "(?m)^\\s*"
                                                + "(?:"
                                                + "Lecture\\s+credits"
                                                + "|Lecture"
                                                + ")"
                                                + "\\s*:\\s*"
                                                + "([^\\r\\n]+)");
        }

        private String parseLaboratoryCredits(
                        String text) {

                return capture(
                                text,
                                "(?m)^\\s*"
                                                + "(?:"
                                                + "Laboratory\\s+credits"
                                                + "|Laboratory"
                                                + "|Lab\\s+credits"
                                                + "|Lab"
                                                + ")"
                                                + "\\s*:\\s*"
                                                + "([^\\r\\n]+)");
        }

        private String parsePrerequisites(
                        String text) {
                Matcher interleavedPrerequisites = Pattern.compile(
                                "(?im)^\\s*Required\\s+and\\s+"
                                                + "([^\\r\\n]+?)\\s*\\R\\s*"
                                                + "recommended\\s+prerequisites\\s+for\\s+joining\\s+"
                                                + "the\\s+course\\s*$")
                                .matcher(text);

                if (interleavedPrerequisites.find()) {
                        return sanitizeImportedPlainText(
                                        interleavedPrerequisites.group(1));
                }
                LabelMatch startHeading = firstFlexibleLabel(
                                text,
                                0,
                                "Required and recommended prerequisites for joining the course",
                                "Prerequisites",
                                "Required prerequisites",
                                "Recommended prerequisites");

                if (startHeading == null) {
                        return null;
                }

                int start = startHeading.end();

                LabelMatch endHeading = firstFlexibleLabel(
                                text,
                                start,
                                "Course objectives",
                                "Course aims",
                                "Course learning outcomes",
                                "Learning outcomes");

                int end = endHeading == null
                                ? text.length()
                                : endHeading.start();

                String value = collapseToNull(
                                text.substring(
                                                start,
                                                end));

                return sanitizeImportedPlainText(
                                value);
        }

        private String parseAssessmentPassNote(
                        String text,
                        String flat) {

                /*
                 * A semantic passing heading must begin a physical line and must end as a
                 * heading. This avoids interpreting assessment-matrix text such as
                 * "Pass Criteria 2 Criteria ..." as a section heading.
                 */
                Matcher passHeading = Pattern.compile(
                                "(?im)^\\s*"
                                                + "(?:"
                                                + "Passing\\s+requirements?"
                                                + "|Pass\\s+criteria"
                                                + "|Passing\\s+criteria"
                                                + "|Minimum\\s+passing\\s+requirement"
                                                + ")"
                                                + "\\s*"
                                                + "(?::\\s*|[-\\u2013\\u2014]\\s*|\\R\\s*)")
                                .matcher(
                                                text);

                if (passHeading.find()) {

                        int start = passHeading.end();

                        LabelMatch endHeading = firstFlexibleLabel(
                                        text,
                                        start,
                                        "Rubrics (optional)",
                                        "Rubrics",
                                        "Reading list",
                                        "References",
                                        "Date revised");

                        int end = endHeading == null
                                        ? text.length()
                                        : endHeading.start();

                        String value = collapseToNull(
                                        text.substring(
                                                        start,
                                                        end));

                        return sanitizeImportedPlainText(value);
                }

                /*
                 * Standard IU assessment note, observed directly in the CS2026 source.
                 * Capture only the physical Note: %Pass line.
                 */
                Matcher percentPassLine = Pattern.compile(
                                "(?im)^\\s*Note\\s*:\\s*(%Pass\\s*:.*)$")
                                .matcher(
                                                text);

                if (percentPassLine.find()) {
                        return sanitizeImportedPlainText(
                                        collapseToNull(
                                                        percentPassLine.group(1)));
                }

                /*
                 * Backward-compatible fallback for old templates where PDFBox collapses
                 * the note and Rubrics heading into one flat sequence.
                 */
                String legacyValue = capture(
                                flat,
                                "Note:\\s*(%Pass:.*?)\\s+"
                                                + "(?:(?:\\d+(?:\\.\\d+)*)\\.\\s*)?"
                                                + "Rubrics\\b"
                                                + "(?:\\s*\\(optional\\))?");

                return sanitizeImportedPlainText(legacyValue);
        }

        private String parseContentNote(
                        String text,
                        String flat) {

                LabelMatch contentNoteHeading = firstFlexibleLabel(
                                text,
                                0,
                                "Content note",
                                "Content description",
                                "Description of course content");

                if (contentNoteHeading != null) {

                        int start = contentNoteHeading.end();

                        LabelMatch endHeading = firstFlexibleLabel(
                                        text,
                                        start,
                                        "Topic Weight Level",
                                        "Course topics",
                                        "Topics",
                                        "Examination",
                                        "Assessment plan",
                                        "Reading list",
                                        "References",
                                        "Rubrics",
                                        "Date revised");

                        int end = endHeading == null
                                        ? text.length()
                                        : endHeading.start();

                        String value = collapseToNull(
                                        text.substring(
                                                        start,
                                                        end));

                        if (value != null) {
                                value = value.replaceFirst(
                                                "^\\s*:\\s*",
                                                "")
                                                .trim();
                        }

                        return sanitizeImportedPlainText(
                                        value);
                }

                /*
                 * Legacy IU template fallback:
                 *
                 * Content
                 * The description of the contents ...
                 * Topic Weight Level
                 *
                 * Keep this unchanged so existing syllabus imports
                 * remain source-compatible.
                 */
                String legacyValue = capture(
                                flat,
                                "Content\\s+"
                                                + "(The description of the contents.*?)"
                                                + "\\s+Topic\\s+Weight\\s+Level");

                return sanitizeImportedPlainText(
                                legacyValue);
        }

        String parseObjectives(String text) {

                String flat = collapse(text);

                LabelMatch startHeading = firstFlexibleLabel(
                                flat,
                                0,
                                "Course objectives",
                                "Course aims",
                                "Mục đích/mục tiêu môn học (Course Purposes/Aims)",
                                "Mục đích/mục tiêu môn học",
                                "Mục tiêu môn học");

                if (startHeading == null) {
                        return null;
                }

                int start = startHeading.end();

                /*
                 * Different syllabus generations may use different semantic
                 * headings for the same canonical section.
                 */
                LabelMatch endHeading = firstFlexibleLabel(
                                flat,
                                start,
                                "Course learning outcomes",
                                "Course learning",
                                "Learning outcomes",
                                "3. Mô tả môn học (Course Outlines)",
                                "3. Mô tả môn học",
                                "Mô tả môn học (Course Outlines)",
                                "Mô tả môn học",
                                "Course Outlines",
                                "Chuẩn đầu ra môn học");

                /*
                 * Some legacy PDF tables are extracted by PDFBox in column order.
                 * For example, PH012 may become:
                 *
                 * Course learning
                 * CLO 1. ...
                 * outcomes
                 *
                 * In that case a complete semantic heading may be found much later
                 * in the syllabus, causing Course Objectives to swallow CLO/content/
                 * examination/readings. CLO 1 is already a valid legacy boundary,
                 * so always compare both candidates and stop at the earliest one.
                 */
                Matcher firstClo = Pattern.compile(
                                "CLO\\s*1\\b\\s*[.:]?",
                                Pattern.CASE_INSENSITIVE)
                                .matcher(flat);

                int firstCloStart = firstClo.find(start)
                                ? firstClo.start()
                                : -1;

                int end;

                if (endHeading != null
                                && firstCloStart >= 0) {

                        end = Math.min(
                                        endHeading.start(),
                                        firstCloStart);

                } else if (endHeading != null) {

                        end = endHeading.start();

                } else if (firstCloStart >= 0) {

                        end = firstCloStart;

                } else {

                        return null;
                }
                /*
                 * PH012 legacy PDFBox extraction may split the heading as:
                 *
                 * Course learning
                 * CLO 1. ...
                 * outcomes
                 *
                 * When "Course learning" is immediately before CLO 1,
                 * treat that fragment as the real section boundary so it
                 * does not leak into Course Objectives.
                 */
                if (firstCloStart >= 0) {

                        String beforeFirstClo = flat.substring(
                                        start,
                                        firstCloStart);

                        Matcher splitLearningHeading = Pattern.compile(
                                        "(?i)\\bCourse\\s+learning\\s*$")
                                        .matcher(beforeFirstClo);

                        if (splitLearningHeading.find()) {
                                end = Math.min(
                                                end,
                                                start + splitLearningHeading.start());
                        }
                }
                String value = collapse(
                                flat.substring(
                                                start,
                                                end));

                value = sanitizeImportedPlainText(
                                value);

                return value == null || value.isBlank()
                                ? null
                                : value;
        }

        private LabelMatch firstFlexibleLabel(
                        String source,
                        int fromIndex,
                        String... labels) {

                LabelMatch earliest = null;

                for (String label : labels) {

                        LabelMatch candidate = findFlexibleLabel(
                                        source,
                                        label,
                                        fromIndex);

                        if (candidate != null
                                        && (earliest == null
                                                        || candidate.start() < earliest.start())) {

                                earliest = candidate;
                        }
                }

                return earliest;
        }

        private String parseLanguage(
                        String text) {

                LabelMatch languageHeading = firstFlexibleLabel(
                                text,
                                0,
                                "Language",
                                "Medium of instruction",
                                "Language of instruction",
                                "Instruction language");

                if (languageHeading == null) {
                        return null;
                }

                int start = languageHeading.end();

                LabelMatch endHeading = firstFlexibleLabel(
                                text,
                                start,
                                "Relation to curriculum",
                                "Relation to the curriculum",
                                "Relation to",
                                "Teaching methods",
                                "Workload");

                int end = endHeading == null
                                ? text.length()
                                : endHeading.start();

                String raw = collapseToNull(
                                text.substring(
                                                start,
                                                end));

                if (raw == null) {
                        return null;
                }

                raw = raw.replaceFirst(
                                "^\\s*:\\s*",
                                "");

                return normalizeLanguage(
                                raw);
        }

        /*
         * PATCH_3_2G_SEMANTIC_LABEL_CLEANUP
         *
         * Prefer the explicit Course designation label when both
         * Course classification and Course designation are present in the same
         * source block (PE021IU). The classification label is only a fallback for
         * older templates that genuinely use it as the designation field.
         */
        private String parseCourseDesignation(
                        String text) {

                LabelMatch designationHeading = firstFlexibleLabel(
                                text,
                                0,
                                "Course designation");

                if (designationHeading == null) {
                        designationHeading = firstFlexibleLabel(
                                        text,
                                        0,
                                        "Course classification",
                                        "Course category");
                }

                if (designationHeading != null) {

                        int start = designationHeading.end();

                        LabelMatch endHeading =
        firstFlexibleLabel(
                text,
                start,

                "Course Name",
                "Course name",
                "Course description",
                "Course Description",
                "Course type",
                "Course Type",
                "Type of course",

                "Semester(s) in which the course is taught",
                "Semester(s) in",
                "Semester",
                "Person responsible for the course",
                "Person responsible",
                "Course coordinator",
                "Instructor in charge",
                "Person",
                "Language",
                "Medium of instruction"
        );

                        int end = endHeading == null
                                        ? text.length()
                                        : endHeading.start();

                        String value = collapseToNull(
                                        text.substring(
                                                        start,
                                                        end));

                        if (value == null) {
                                return null;
                        }

                        value = value.replaceFirst(
                                        "^\\s*:\\s*",
                                        "")
                                        .trim();

                        return sanitizeImportedPlainText(
                                        value.isBlank()
                                                        ? null
                                                        : value);
                }

                /*
                 * Legacy fallback retained for older IU layouts where the designation
                 * label itself is interleaved by PDFBox.
                 */
                String value = collapseToNull(
                                sectionUntilAny(
                                                text,
                                                "1. General information",
                                                "Semester(s)"));

                if (value != null) {
                        value = value.replaceFirst(
                                        "(?i)^\\s*Course\\s*",
                                        "")
                                        .replaceFirst(
                                                        "(?i)\\s+designation\\s*$",
                                                        "")
                                        .trim();
                }

                return sanitizeImportedPlainText(value);
        }

        private String parsePersonResponsible(
                        String text) {

                Matcher interleavedPerson = Pattern.compile(
                                "(?im)^\\s*Person\\s+responsible\\s+for\\s+the\\s+"
                                                + "([^\\r\\n]+?)\\s*\\R\\s*course\\s*$")
                                .matcher(text);

                if (interleavedPerson.find()) {
                        return cleanPersonResponsibleValue(
                                        interleavedPerson.group(1));
                }

                LabelMatch personHeading = firstFlexibleLabel(
                                text,
                                0,
                                "Person responsible for the course",
                                "Person responsible",
                                "Course coordinator",
                                "Instructor in charge",
                                "Course instructor",
                                "Person");

                if (personHeading == null) {
                        return null;
                }

                int start = personHeading.end();

                LabelMatch endHeading = firstFlexibleLabel(
                                text,
                                start,
                                "Language",
                                "Relation to curriculum",
                                "Relation to the curriculum",
                                "Relation to",
                                "Teaching methods",
                                "Workload");

                int end = endHeading == null
                                ? text.length()
                                : endHeading.start();

                return cleanPersonResponsibleValue(
                                text.substring(
                                                start,
                                                end));
        }

        private String cleanPersonResponsibleValue(
                        String rawValue) {

                if (rawValue == null) {
                        return null;
                }

                String value = collapse(rawValue)
                                .replaceFirst(
                                                "^\\s*:\\s*",
                                                "")
                                .replaceAll(
                                                "(?i)^\\s*responsible\\s+for\\s+the\\s+course\\s*",
                                                "")
                                .replaceAll(
                                                "(?i)^\\s*responsible\\s+for\\s*",
                                                "")
                                /*
                                 * Two-column PDF extraction can interleave the left-side
                                 * label fragment inside/right after instructor names:
                                 * Dr. A for the course Dr. B
                                 * Dr. A responsible for the course
                                 * A physical PDF page number can also immediately follow
                                 * that label residue (IT094IU: "for the course 175").
                                 * Remove only this exact field-label residue.
                                 */
                                .replaceAll(
                                                "(?i)\\s+(?:responsible\\s+)?for\\s+the\\s+course(?:\\s+\\d{1,3})?(?=\\s|$)",
                                                " ")
                                .replaceAll(
                                                "\\s+",
                                                " ")
                                .trim();

                return value.isBlank()
                                ? null
                                : sanitizeImportedPlainText(value);
        }

        private String parseRelation(
                        String text) {

                LabelMatch relationHeading = firstFlexibleLabel(
                                text,
                                0,
                                "Relation to curriculum",
                                "Relation to the curriculum",
                                "Curriculum relation",
                                "Relation to");

                if (relationHeading == null) {
                        return null;
                }

                int start = relationHeading.end();

                LabelMatch endHeading = firstFlexibleLabel(
                                text,
                                start,
                                "Teaching methods",
                                "Instructional methods",
                                "Teaching methodology",
                                "Teaching and learning methods",
                                "Methods of instruction",
                                "Teaching",
                                "Workload");

                int end = endHeading == null
                                ? text.length()
                                : endHeading.start();

                String value = collapse(
                                text.substring(
                                                start,
                                                end));

                value = value.replaceFirst(
                                "^\\s*:\\s*",
                                "")
                                .replaceFirst(
                                                "(?i)^\\s*curriculum\\s*:?\\s*",
                                                "")
                                .replaceFirst(
                                                "(?i)^\\s*the\\s+curriculum\\s*:?\\s*",
                                                "")
                                .replaceFirst(
                                                "(?i)\\s+curriculum\\s*$",
                                                "")
                                .trim();

                return sanitizeImportedPlainText(
                                value.isBlank()
                                                ? null
                                                : value);
        }

        private String parseTeachingMethods(
                        String text) {

                LabelMatch teachingHeading = firstFlexibleLabel(
                                text,
                                0,
                                "Teaching methods",
                                "Instructional methods",
                                "Teaching methodology",
                                "Teaching and learning methods",
                                "Methods of instruction",
                                "Teaching");

                if (teachingHeading == null) {
                        return null;
                }

                int start = teachingHeading.end();

                LabelMatch endHeading = firstFlexibleLabel(
                                text,
                                start,
                                "Workload (incl. contact",
                                "(Estimated) Total workload",
                                "Total workload",
                                "Contact hours",
                                "Workload");

                int end = endHeading == null
                                ? text.length()
                                : endHeading.start();

                String value = collapse(
                                text.substring(
                                                start,
                                                end));

                value = value.replaceFirst(
                                "^\\s*:\\s*",
                                "")
                                .replaceFirst(
                                                "(?i)^\\s*methods\\s*:?\\s*",
                                                "")
                                .replaceFirst(
                                                "(?i)\\s+methods\\s*$",
                                                "")
                                .trim();

                return sanitizeImportedPlainText(
                                value.isBlank()
                                                ? null
                                                : value);
        }

        private String normalizeLanguage(String raw) {
                String value = collapse(raw);
                if (value.isBlank())
                        return null;
                List<String> languages = new ArrayList<>();
                if (containsIgnoreCase(value, "English"))
                        languages.add("English");
                if (containsIgnoreCase(value, "Vietnamese"))
                        languages.add("Vietnamese");
                if (containsIgnoreCase(value, "German"))
                        languages.add("German");
                if (containsIgnoreCase(value, "French"))
                        languages.add("French");
                if (!languages.isEmpty())
                        return String.join(" / ", languages);
                return value.length() <= 100 ? value : value.substring(0, 100).trim();
        }

        private String parseSemester(
                        String text) {

                LabelMatch semesterHeading = firstFlexibleLabel(
                                text,
                                0,
                                "Semester(s) in which the course is taught",
                                "Semester(s) in",
                                "Semester");

                if (semesterHeading == null) {
                        return null;
                }

                int start = semesterHeading.end();

                LabelMatch endHeading = firstFlexibleLabel(
                                text,
                                start,
                                "Person responsible for the course",
                                "Person",
                                "Language",
                                "Teaching methods");

                int end = endHeading == null
                                ? text.length()
                                : endHeading.start();

                String value = collapse(
                                text.substring(
                                                start,
                                                end));

                value = value.replaceFirst(
                                "(?i)^\\s*:\\s*",
                                "");

                value = value.replaceFirst(
                                "(?i)^\\s*which\\s+the\\s+course\\s+is\\s+taught\\s*",
                                "");

                return normalizeSemester(
                                value);
        }

        String normalizeSemester(String rawValue) {
                if (rawValue == null || rawValue.isBlank()) {
                        return null;
                }
                Matcher matcher = Pattern.compile("(?<!\\d)([1-8])(?!\\d)").matcher(rawValue);
                List<String> semesters = new ArrayList<>();
                while (matcher.find()) {
                        String semester = matcher.group(1);
                        if (!semesters.contains(semester))
                                semesters.add(semester);
                }
                return semesters.isEmpty() ? null : "Semester " + String.join(", ", semesters);
        }

        void parseClos(
                        String text,
                        SyllabusImportData data) {

                String flat = collapse(text);

                Matcher firstClo = Pattern.compile(
                                "CLO\\s*1\\b\\s*[.:]?",
                                Pattern.CASE_INSENSITIVE).matcher(flat);

                int start = firstClo.find()
                                ? firstClo.start()
                                : -1;

                LabelMatch endHeading =
        start < 0
                ? null
                : firstFlexibleLabel(
                        flat,
                        start,

                        "Competency level",
                        "Competency classification",

                        /*
                         * Some old syllabus tables do not expose a clean
                         * Competency heading after PDF extraction.
                         * These rows are CLO references, not CLO definitions.
                         */
                        "Knowledge CLO",
                        "Skill CLO",
                        "Attitude CLO",

                        "Content The description of the contents",
                        "The description of the contents should clearly indicate",
                        "Topic Weight Level",

                        "Planned learning activities and teaching methods",
                        "Planned learning activities",
                        "Assessment plan",
                        "Examination forms",
                        "Examination methods",
                        "Study and examination requirements",
                        "Reading list",
                        "References",
                        "Bibliography",
                        "Rubrics",
                        "Date revised"
                );

                int end = endHeading == null
                                ? -1
                                : endHeading.start();

                String section = start < 0
                                ? ""
                                : flat.substring(
                                                start,
                                                end < 0
                                                                ? flat.length()
                                                                : end);

                Map<String, CloImportData> closByCode = new LinkedHashMap<>();

                Matcher matcher = Pattern.compile(
                                "CLO\\s*(\\d+)\\b\\s*[.:]?\\s+(.*?)"
                                                + "(?=\\s+CLO\\s*\\d+\\b\\s*[.:]?\\s+|$)",
                                Pattern.CASE_INSENSITIVE).matcher(section);

                while (matcher.find()) {

                        int number = Integer.parseInt(
                                        matcher.group(1));

                        String description = collapse(
                                        matcher.group(2))
                                        .replaceFirst(
                                                        "(?i)\\bthe\\s+outcomes\\s+interfaces\\b",
                                                        "the interfaces");

                        String code = "CLO" + number;

                        CloImportData parsed = CloImportData.builder()
                                        .code(code)
                                        .description(description)
                                        .orderIndex(number)
                                        .build();

                        CloImportData existing = closByCode.get(code);

                        if (existing == null
                                        || nullToEmpty(
                                                        parsed.getDescription())
                                                        .length() > nullToEmpty(
                                                                        existing.getDescription()).length()) {

                                closByCode.put(
                                                code,
                                                parsed);
                        }
                }

                List<CloImportData> clos = new ArrayList<>(
                                closByCode.values());

                Map<Integer, String> competency = new HashMap<>();

                assignCompetency(
                                text,
                                "Knowledge",
                                "KNOWLEDGE",
                                competency);

                assignCompetency(
                                text,
                                "Skill",
                                "SKILL",
                                competency);

                assignCompetency(
                                text,
                                "Attitude",
                                "ATTITUDE",
                                competency);

                for (CloImportData clo : clos) {

                        int number = Integer.parseInt(
                                        clo.getCode()
                                                        .replaceAll(
                                                                        "\\D",
                                                                        ""));

                        clo.setCompetencyLevel(
                                        competency.get(number));
                }

                /*
                 * Legacy Vietnamese political-science syllabi use LO.1, LO.2, ...
                 * instead of CLO1, CLO2, ...
                 *
                 * Only activate this fallback when the normal CLO parser found nothing.
                 */
                if (clos.isEmpty()) {

                        clos.addAll(
                                        parseLegacyVietnameseClos(
                                                        text));
                }

                data.setClos(
                                clos);
        }

        private List<CloImportData> parseLegacyVietnameseClos(
                        String text) {

                String flat = collapse(
                                text);

                LabelMatch heading = firstFlexibleLabel(
                                flat,
                                0,
                                "Chuẩn đầu ra môn học (Course Learning Outcomes)",
                                "Chuẩn đầu ra môn học");

                if (heading == null) {

                        return List.of();
                }

                int start = heading.end();

                LabelMatch endHeading = firstFlexibleLabel(
                                flat,
                                start,
                                "6. Kế hoạch giảng dạy theo buổi học (Course Plan)",
                                "6. Kế hoạch giảng dạy môn học (Course Plan)",
                                "6. Kế hoạch giảng dạy",
                                "Kế hoạch giảng dạy");

                int end = endHeading == null
                                ? flat.length()
                                : endHeading.start();

                String section = flat.substring(
                                start,
                                end);

                /*
                 * PDF extraction variants seen in the real CS2021 source:
                 *
                 * LO.l -> LO.1
                 * L0.7 -> LO.7
                 * LO. 1.1 -> LO.1.1
                 */
                section = section
                                .replaceAll(
                                                "(?i)\\bL0\\.",
                                                "LO.")
                                .replaceAll(
                                                "(?i)(LO\\.\\d+)(?=LO\\.)",
                                                "$1 ")
                                .replaceAll(
                                                "(?i)\\bLO\\.\\s*(?:L|I)\\b",
                                                "LO.1")
                                .replaceAll(
                                                "(?i)\\bLO\\.\\s+",
                                                "LO.");

                LabelMatch skillHeading = firstFlexibleLabel(
                                section,
                                0,
                                "5.2. Kỹ năng",
                                "2. Kỹ năng");

                LabelMatch attitudeHeading = firstFlexibleLabel(
                                section,
                                0,
                                "5.3. Thái độ",
                                "3. Thái độ");

                int skillStart = skillHeading == null
                                ? -1
                                : skillHeading.start();

                int attitudeStart = attitudeHeading == null
                                ? -1
                                : attitudeHeading.start();

                Map<Integer, CloImportData> byNumber = new java.util.TreeMap<>();

                Map<Integer, Integer> sourcePosition = new java.util.HashMap<>();

                /*
                 * Preferred form:
                 *
                 * LO.1
                 * TRIẾT HỌC VÀ VAI TRÒ ...
                 * LO.1.1 - ...
                 */
                Pattern parentPattern = Pattern.compile(
                                "(?i)\\bLO\\.([1-9])"
                                                + "(?!\\s*\\.\\s*\\d)");

                Matcher parents = parentPattern.matcher(
                                section);

                List<Integer> parentStarts = new ArrayList<>();

                List<Integer> parentEnds = new ArrayList<>();

                List<Integer> parentNumbers = new ArrayList<>();

                while (parents.find()) {

                        parentStarts.add(
                                        parents.start());

                        parentEnds.add(
                                        parents.end());

                        parentNumbers.add(
                                        Integer.parseInt(
                                                        parents.group(1)));
                }

                Pattern childPattern = Pattern.compile(
                                "(?i)\\bLO\\.([1-9])\\.\\d+");

                for (int index = 0; index < parentStarts.size(); index++) {

                        int number = parentNumbers.get(
                                        index);

                        int blockEnd = index + 1 < parentStarts.size()
                                        ? parentStarts.get(
                                                        index + 1)
                                        : section.length();

                        String block = section.substring(
                                        parentEnds.get(index),
                                        blockEnd);

                        Matcher firstChild = childPattern.matcher(
                                        block);

                        String description = collapse(
                                        firstChild.find()
                                                        ? block.substring(
                                                                        0,
                                                                        firstChild.start())
                                                        : block);

                        description = description.replaceFirst(
                                        "^[\\s:;,.\\-–]+",
                                        "")
                                        .trim();

                        if (!description.isBlank()) {

                                byNumber.put(
                                                number,
                                                CloImportData.builder()
                                                                .code(
                                                                                "CLO" + number)
                                                                .description(
                                                                                sanitizeImportedPlainText(
                                                                                                description))
                                                                .orderIndex(
                                                                                number)
                                                                .build());

                                sourcePosition.put(
                                                number,
                                                parentStarts.get(
                                                                index));
                        }
                }

                /*
                 * Some Vietnamese tables omit the parent LO row from PDF text
                 * but still expose child rows such as LO.1.1, LO.1.2, ...
                 *
                 * For such a parent, preserve the source criteria by grouping
                 * its child outcomes. Nothing is invented.
                 */
                Pattern anyLoPattern = Pattern.compile(
                                "(?i)\\bLO\\.([1-9])"
                                                + "(?:\\.(\\d+))?");

                Matcher allLo = anyLoPattern.matcher(
                                section);

                List<Integer> tokenStarts = new ArrayList<>();

                List<Integer> tokenEnds = new ArrayList<>();

                List<Integer> tokenParents = new ArrayList<>();

                List<String> tokenChildren = new ArrayList<>();

                while (allLo.find()) {

                        tokenStarts.add(
                                        allLo.start());

                        tokenEnds.add(
                                        allLo.end());

                        tokenParents.add(
                                        Integer.parseInt(
                                                        allLo.group(1)));

                        tokenChildren.add(
                                        allLo.group(2));
                }

                Map<Integer, List<String>> children = new java.util.TreeMap<>();

                for (int index = 0; index < tokenStarts.size(); index++) {

                        if (tokenChildren.get(index) == null) {

                                continue;
                        }

                        int parent = tokenParents.get(
                                        index);

                        int tokenEnd = tokenEnds.get(
                                        index);

                        int nextStart = index + 1 < tokenStarts.size()
                                        ? tokenStarts.get(
                                                        index + 1)
                                        : section.length();

                        String value = collapse(
                                        section.substring(
                                                        tokenEnd,
                                                        nextStart));

                        value = value.replaceFirst(
                                        "^[\\s:;,.\\-–]+",
                                        "")
                                        .trim();

                        for (String marker : List.of(
                                        "5.2. Kỹ năng",
                                        "2. Kỹ năng",
                                        "5.3. Thái độ",
                                        "3. Thái độ")) {

                                int markerIndex = indexOfIgnoreCase(
                                                value,
                                                marker);

                                if (markerIndex >= 0) {

                                        value = value.substring(
                                                        0,
                                                        markerIndex)
                                                        .trim();
                                }
                        }

                        if (!value.isBlank()) {

                                children
                                                .computeIfAbsent(
                                                                parent,
                                                                ignored -> new ArrayList<>())
                                                .add(
                                                                value);

                                sourcePosition.putIfAbsent(
                                                parent,
                                                tokenStarts.get(
                                                                index));
                        }
                }

                for (Map.Entry<Integer, List<String>> entry : children.entrySet()) {

                        int number = entry.getKey();

                        if (byNumber.containsKey(
                                        number)) {

                                continue;
                        }

                        String description = sanitizeImportedPlainText(
                                        String.join(
                                                        "; ",
                                                        entry.getValue()));

                        if (description == null
                                        || description.isBlank()) {

                                continue;
                        }

                        byNumber.put(
                                        number,
                                        CloImportData.builder()
                                                        .code(
                                                                        "CLO" + number)
                                                        .description(
                                                                        description)
                                                        .orderIndex(
                                                                        number)
                                                        .build());
                }

                /*
                 * Preserve the source competency grouping:
                 * 5.1 Knowledge / 5.2 Skill / 5.3 Attitude.
                 */
                for (Map.Entry<Integer, CloImportData> entry : byNumber.entrySet()) {

                        int position = sourcePosition.getOrDefault(
                                        entry.getKey(),
                                        -1);

                        String competency;

                        if (position >= 0
                                        && attitudeStart >= 0
                                        && position >= attitudeStart) {

                                competency = "ATTITUDE";

                        } else if (position >= 0
                                        && skillStart >= 0
                                        && position >= skillStart) {

                                competency = "SKILL";

                        } else {

                                competency = "KNOWLEDGE";
                        }

                        entry.getValue()
                                        .setCompetencyLevel(
                                                        competency);
                }

                return new ArrayList<>(
                                byNumber.values());
        }

        /**
         * PDF extraction is plain text. Remove only constructs that can accidentally be
         * interpreted as active markup when the preview is posted back for
         * confirmation;
         * ordinary comparison symbols and chemistry notation remain untouched.
         */
        String sanitizeImportedPlainText(String value) {
                if (value == null)
                        return null;
                String sanitized = HtmlUtils.htmlUnescape(value)
                                // A PDF row can be cut before the closing '>'. Neutralize the opening
                                // marker itself instead of requiring a complete HTML tag.
                                .replaceAll("(?i)<(?=\\s*/?\\s*(?:script|iframe|object|embed|svg|math|meta|link)\\b)",
                                                "‹")
                                .replaceAll("(?i)\\b(?:javascript|vbscript)\\s*:", "")
                                .replaceAll("(?i)\\bon[a-z]{3,}\\s*=", "")
                                .replaceAll("(?i)data\\s*:\\s*text/html", "")
                                .replaceAll("(?i)expression\\s*\\(", "expression ")
                                .replaceAll("(?i)%3c\\s*/?\\s*script", "");
                return collapseToNull(sanitized);
        }

        /**
         * Every value in this DTO originates from PDF text extraction and is posted
         * back
         * through the normal XSS validator during confirm. Normalize the complete
         * import
         * payload once so a signature in exam forms, requirements, a table cell, etc.
         * does
         * not merely move the same failure to the next field. This does not run for
         * manual
         * create/update requests.
         */
        void sanitizeImportedData(SyllabusImportData data) {

                sanitizeImportedValue(
                                data,
                                new IdentityHashMap<>());

                data.setWorkloadContact(
                                cleanWorkloadContact(
                                                data.getWorkloadContact()));
        }

        String cleanWorkloadContact(String value) {

                if (value == null || value.isBlank()) {
                        return value;
                }

                return collapse(value)
                                .replaceFirst(
                                                "(?i)\\s+(?:(?:self[-\\s]?study|study)"
                                                                + "\\s+hours)\\)\\s*$",
                                                "")
                                .trim();
        }

        private void sanitizeImportedValue(Object value, IdentityHashMap<Object, Boolean> visited) {
                if (value == null || visited.put(value, Boolean.TRUE) != null)
                        return;
                if (value instanceof Iterable<?> values) {
                        values.forEach(item -> sanitizeImportedValue(item, visited));
                        return;
                }
                Class<?> type = value.getClass();
                if (type.isPrimitive() || type.isEnum() || type.getName().startsWith("java."))
                        return;
                for (Class<?> current = type; current != null
                                && current != Object.class; current = current.getSuperclass()) {
                        for (Field field : current.getDeclaredFields()) {
                                if (Modifier.isStatic(field.getModifiers()) || !field.trySetAccessible())
                                        continue;
                                try {
                                        Object fieldValue = field.get(value);
                                        if (fieldValue instanceof String text) {
                                                field.set(value, sanitizeImportedPlainText(text));
                                        } else {
                                                sanitizeImportedValue(fieldValue, visited);
                                        }
                                } catch (IllegalAccessException ignored) {
                                        // An inaccessible optional field must not make the import fail.
                                }
                        }
                }
        }

        private void assignCompetency(
                        String text,
                        String label,
                        String competency,
                        Map<Integer, String> target) {
                Matcher matcher = Pattern.compile(
                                "(?im)^" + Pattern.quote(label) + "[ \\t]+((?:(?:CLO)?\\s*\\d+\\s*,?\\s*)*)$")
                                .matcher(text);
                if (!matcher.find()) {
                        return;
                }
                Matcher numberMatcher = Pattern.compile("\\d+").matcher(matcher.group(1));
                while (numberMatcher.find()) {
                        target.put(Integer.parseInt(numberMatcher.group()), competency);
                }
        }

        private void parseContent(
                        String text,
                        SyllabusImportData data) {

                LabelMatch contentHeading = firstFlexibleLabel(
                                text,
                                0,
                                "Content The description of the contents",
                                "Contet The description of the contents",
                                "Course content");

                if (contentHeading == null) {
                        data.setTopics(
                                        new ArrayList<>());
                        return;
                }

                int contentStart = contentHeading.end();

                LabelMatch contentEndHeading = firstFlexibleLabel(
        text,
        contentStart,

        "Planned learning activities and teaching methods",
        "Planned learning activities",
        "Teaching and learning activities",

        "Assessment plan",
        "Examination forms",
        "Examination methods",
        "Study and examination requirements",
        "Reading list",
        "References",
        "Bibliography",
        "Rubrics (optional)",
        "Rubrics",
        "Date revised");

                int contentEnd = contentEndHeading == null
                                ? text.length()
                                : contentEndHeading.start();

                String content = text.substring(
                                contentStart,
                                contentEnd);

                List<TopicImportData> topics = new ArrayList<>();

                Matcher matcher = CONTENT_ROW.matcher(content);

                List<MatcherSnapshot> rows = new ArrayList<>();

                while (matcher.find()) {
                        rows.add(
                                        new MatcherSnapshot(
                                                        matcher.start(),
                                                        matcher.end(),
                                                        matcher.group(1),
                                                        matcher.group(2),
                                                        matcher.group(3)));
                }

                int week = 1;

                for (int index = 0; index < rows.size(); index++) {

                        MatcherSnapshot row = rows.get(index);

                        int continuationEnd = index + 1 < rows.size()
                                        ? rows.get(index + 1).start()
                                        : content.length();

                        String continuation = content.substring(
                                        row.end(),
                                        continuationEnd)
                                        .lines()
                                        .map(this::collapse)
                                        .filter(line -> !line.isBlank())
                                        .filter(line -> !containsIgnoreCase(
                                                        line,
                                                        "Topic Weight Level"))
                                        .filter(line -> !containsIgnoreCase(
                                                        line,
                                                        "Weight: lecture session"))
                                        .filter(line -> !containsIgnoreCase(
                                                        line,
                                                        "Teaching levels:"))
                                        .reduce(
                                                        "",
                                                        (left, right) -> collapse(
                                                                        left
                                                                                        + " "
                                                                                        + right));

                        String name = collapse(
                                        row.name()
                                                        + " "
                                                        + continuation)
                                        .replaceFirst(
                                                        "[;.]$",
                                                        "");

                        String weight = collapse(
                                        row.weight());

                        String level = row.level()
                                        .replaceAll(
                                                        "\\s*,\\s*",
                                                        ", ")
                                        .trim();

                        topics.add(
                                        TopicImportData.builder()
                                                        .weekNumber(week)
                                                        .orderInWeek(1)
                                                        .name(name)
                                                        .contentWeight(weight)
                                                        .contentLevel(level)
                                                        .teachingLevel(level)
                                                        .topicType("LECTURE")
                                                        .build());

                        week++;
                }

                data.setTopics(
                                topics);
        }

        private record MatcherSnapshot(int start, int end, String name, String weight, String level) {
        }

        private void parseWeeklyActivities(
                        String text,
                        SyllabusImportData data) {

                LabelMatch weeklyHeading = firstFlexibleLabel(
                                text,
                                0,
                                "Planned learning activities and teaching methods",
                                "Planned learning activities",
                                "Teaching and learning activities");

                if (weeklyHeading == null) {
                        data.setWeeklyActivities(
                                        new ArrayList<>());
                        data.setTopicCloMappings(
                                        new ArrayList<>());
                        return;
                }

                int weeklyStart = weeklyHeading.end();

                LabelMatch weeklyEndHeading = firstFlexibleLabel(
                                text,
                                weeklyStart,
                                "Assessment plan");

                int weeklyEnd = weeklyEndHeading == null
                                ? text.length()
                                : weeklyEndHeading.start();

                String weeklySection = text.substring(
                                weeklyStart,
                                weeklyEnd);
                Matcher matcher = Pattern.compile("(?m)^(\\d{1,2})[ \\t]+.*$").matcher(weeklySection);
                List<Integer> starts = new ArrayList<>();
                List<Integer> weeks = new ArrayList<>();
                while (matcher.find()) {
                        int week = Integer.parseInt(matcher.group(1));
                        if (week >= 1 && week <= 15 && !weeks.contains(week)) {
                                weeks.add(week);
                                starts.add(matcher.start());
                        }
                }

                List<SyllabusImportData.WeeklyActivityItem> activities = new ArrayList<>();
                List<SyllabusImportData.TopicCloMappingItem> topicCloMappings = new ArrayList<>();
                int contentTopicIndex = 0;
                for (int index = 0; index < starts.size(); index++) {
                        int week = weeks.get(index);
                        int end = index + 1 < starts.size() ? starts.get(index + 1) : weeklySection.length();
                        String block = weeklySection.substring(starts.get(index), end).trim();
                        String firstLine = block.lines().findFirst().orElse("");
                        String sourceTopic = collapse(firstLine.replaceFirst("^\\d{1,2}\\s+", ""));
                        boolean examinationWeek = sourceTopic.matches(
                                        "(?i)^(?:midterm(?:\\s+exam(?:ination)?)?|final(?:\\s+exam(?:ination)?)?)$");
                        TopicImportData topic = examinationWeek || contentTopicIndex >= data.getTopics().size()
                                        ? null
                                        : data.getTopics().get(contentTopicIndex++);
                        String topicName = examinationWeek ? sourceTopic
                                        : topic != null ? topic.getName() : extractWeeklyTopic(firstLine);
                        String clo = examinationWeek ? ""
                                        : capture(firstLine, "\\s(\\d+(?:\\s*,\\s*\\d+)*)\\s+Quiz");
                        String assessments = examinationWeek ? ""
                                        : containsIgnoreCase(block, "Midterm")
                                                        ? "Quiz, Lab, Midterm"
                                                        : containsIgnoreCase(block, "Final") ? "Quiz, Lab, Final"
                                                                        : "Quiz";
                        String learningActivities = examinationWeek ? ""
                                        : containsIgnoreCase(block, "Discussion")
                                                        || containsIgnoreCase(block, "In-class")
                                                                        ? "Lecture, Discussion, In-class Exercise"
                                                                        : "Lecture";
                        String resources = examinationWeek ? "" : capture(firstLine, "(\\d+)[ \\t]*$");

                        activities.add(SyllabusImportData.WeeklyActivityItem.builder()
                                        .week(week)
                                        .topic(topicName)
                                        .clo(clo)
                                        .assessments(assessments)
                                        .learningActivities(learningActivities)
                                        .resources(resources)
                                        .build());
                        if (topic != null) {
                                // Preserve the official week number. Content rows are compact
                                // and omit exam weeks, while the planned-activities table is
                                // the authoritative source for scheduling and Topic-CLO links.
                                topic.setWeekNumber(week);
                                topic.setOrderInWeek(1);
                                topic.setTeachingMethod("Lecture");
                                topic.setLearningActivity(learningActivities);
                                topic.setResources(resources);
                        }
                        Matcher cloNumber = Pattern.compile("\\d+").matcher(nullToEmpty(clo));
                        while (cloNumber.find()) {
                                topicCloMappings.add(SyllabusImportData.TopicCloMappingItem.builder()
                                                .topicIndex(week)
                                                .cloCode("CLO" + cloNumber.group())
                                                .build());
                        }
                }
                data.setWeeklyActivities(activities);
                data.setTopicCloMappings(topicCloMappings);
        }

        /*
         * ============================================================
         * PATCH 3.2F2 - POSITIONAL WEEKLY TABLE EXTRACTION
         * ============================================================
         *
         * Evidence from the real 517-page CS2026 programme dossier
         * shows that PDFTextStripper's flattened text interleaves the
         * weekly-plan columns and caused the old parser to fabricate
         * Quiz/Lab/Midterm/Lecture/resource values.
         *
         * This extractor:
         * - derives semantic header anchors from TextPosition geometry;
         * - supports split headers (We+ek, CL+O, Resourc+es, etc.);
         * - maps Assessment/Learning by semantic label, not fixed X order;
         * - treats Resources as optional;
         * - expands explicit source week groups/ranges (5,6 / 1-3);
         * - never generates CLO/LLO or default weekly values.
         */
        private boolean applyPositionalWeeklyPlan(
                        PDDocument document,
                        int startPage,
                        int endPage,
                        SyllabusImportData data) throws IOException {

                List<WeeklyPageLine> lines = collectWeeklyPositionLines(
                                document,
                                startPage,
                                endPage);

                int headingIndex = findWeeklyPositionHeading(
                                lines);

                if (headingIndex < 0) {
                        return false;
                }

                WeeklyHeaderGeometry geometry = findWeeklyHeaderGeometry(
                                lines,
                                headingIndex);

                if (geometry == null) {
                        return false;
                }

                List<PositionalWeeklyRow> sourceRows = parsePositionalWeeklyRows(
                                lines,
                                geometry);
                if (sourceRows.isEmpty()) {
                        return false;
                }

                List<SyllabusImportData.WeeklyActivityItem> activities = new ArrayList<>();

                List<SyllabusImportData.TopicCloMappingItem> mappings = new ArrayList<>();

                List<TopicImportData> topics = data.getTopics() == null
                                ? List.of()
                                : data.getTopics();

                /*
                 * Remove values that the legacy weekly parser had injected into
                 * TopicImportData before replacing them with positional evidence.
                 */
                for (TopicImportData topic : topics) {

                        topic.setTeachingMethod(
                                        null);

                        topic.setLearningActivity(
                                        null);

                        topic.setResources(
                                        null);
                }

                int contentTopicIndex = 0;

                Set<String> mappingKeys = new LinkedHashSet<>();
                int greatestSourceWeek = 0;
                for (PositionalWeeklyRow row : sourceRows) {

                        boolean examinationRow = isWeeklyExaminationTopic(
                                        row.topic());

                        TopicImportData topic = !examinationRow
                                        && contentTopicIndex < topics.size()
                                                        ? topics.get(
                                                                        contentTopicIndex++)
                                                        : null;

                        Integer primaryWeek = row.weeks().isEmpty()
                                        ? null
                                        : row.weeks().get(0);
                        if (examinationRow
                                        && primaryWeek != null
                                        && primaryWeek < greatestSourceWeek) {

                                continue;
                        }

                        for (Integer sourceWeek : row.weeks()) {

                                if (sourceWeek != null) {

                                        greatestSourceWeek = Math.max(
                                                        greatestSourceWeek,
                                                        sourceWeek);
                                }
                        }
                        if (topic != null
                                        && primaryWeek != null) {

                                topic.setWeekNumber(
                                                primaryWeek);

                                topic.setOrderInWeek(
                                                1);

                                topic.setLearningActivity(
                                                nullIfBlank(
                                                                row.learningActivities()));

                                topic.setResources(
                                                nullIfBlank(
                                                                row.resources()));
                        }

                        List<Integer> cloOrdinals = parseOrdinalReferences(
                                        row.clo());

                        for (Integer week : row.weeks()) {

                                activities.add(
                                                SyllabusImportData.WeeklyActivityItem.builder()
                                                                .week(week)
                                                                .topic(nullIfBlank(row.topic()))
                                                                .clo(nullIfBlank(row.clo()))
                                                                .assessments(nullIfBlank(row.assessments()))
                                                                .learningActivities(
                                                                                nullIfBlank(
                                                                                                row.learningActivities()))
                                                                .resources(
                                                                                nullIfBlank(
                                                                                                row.resources()))
                                                                .build());

                                for (Integer ordinal : cloOrdinals) {

                                        String key = week
                                                        + "|"
                                                        + ordinal;

                                        if (!mappingKeys.add(
                                                        key)) {
                                                continue;
                                        }

                                        mappings.add(
                                                        SyllabusImportData.TopicCloMappingItem.builder()
                                                                        .topicIndex(week)
                                                                        .cloCode(
                                                                                        "CLO"
                                                                                                        + ordinal)
                                                                        .build());
                                }
                        }
                }

                activities.sort(
                                Comparator.comparing(
                                                item -> item.getWeek() == null
                                                                ? Integer.MAX_VALUE
                                                                : item.getWeek()));

                data.setWeeklyActivities(
                                activities);

                data.setTopicCloMappings(
                                mappings);

                return true;
        }

        private List<WeeklyPageLine> collectWeeklyPositionLines(
                        PDDocument document,
                        int startPage,
                        int endPage) throws IOException {

                List<WeeklyPageLine> result = new ArrayList<>();

                for (int page = startPage; page <= endPage; page++) {

                        WeeklyPositionCollector collector = new WeeklyPositionCollector();

                        collector.setStartPage(
                                        page);

                        collector.setEndPage(
                                        page);

                        collector.getText(
                                        document);

                        for (WeeklyVisualLine line : buildWeeklyVisualLines(
                                        collector.glyphs())) {

                                result.add(
                                                new WeeklyPageLine(
                                                                page,
                                                                line.y(),
                                                                line.text(),
                                                                line.words()));
                        }
                }

                return result;
        }

        private int findWeeklyPositionHeading(
                        List<WeeklyPageLine> lines) {

                for (int index = 0; index < lines.size(); index++) {

                        String current = normalizeWeeklyMatch(
                                        lines.get(index)
                                                        .text());

                        /*
                         * PATCH 3.2F2 v4
                         *
                         * A one-line heading must return its own line. The v3 look-ahead
                         * concatenated current + next but returned current's index, so the
                         * line BEFORE "Planned learning activities..." could be reported
                         * as the heading. Header detection then started on the heading text
                         * itself and contaminated semantic X anchors.
                         */
                        if (isWeeklyPositionHeading(
                                        current)) {

                                return index;
                        }

                        if (index + 1 < lines.size()) {

                                String following = normalizeWeeklyMatch(
                                                lines.get(index + 1)
                                                                .text());

                                String combined = current
                                                + " "
                                                + following;

                                /*
                                 * For a genuine two-line heading, return the LAST heading line
                                 * so findWeeklyHeaderGeometry(...) begins strictly after it.
                                 */
                                if (isWeeklyPositionHeading(
                                                combined)) {

                                        return index + 1;
                                }
                        }
                }

                return -1;
        }

        private String weeklyAscii(
                        String value) {

                if (value == null) {
                        return "";
                }

                return java.text.Normalizer.normalize(
                                value.toLowerCase(
                                                Locale.ROOT)
                                                .replace(
                                                                'đ',
                                                                'd'),
                                java.text.Normalizer.Form.NFD)
                                .replaceAll(
                                                "\\p{M}+",
                                                "")
                                .replaceAll(
                                                "\\s+",
                                                " ")
                                .trim();
        }

        private boolean isWeeklyPositionHeading(
                        String value) {

                String normalized = value == null
                                ? ""
                                : value;

                String ascii = weeklyAscii(
                                normalized);

                return normalized.contains(
                                "planned learning activities")
                                || normalized.contains(
                                                "teaching and learning activities")
                                || (normalized.contains(
                                                "planned")
                                                && normalized.contains(
                                                                "learning")
                                                && normalized.contains(
                                                                "activities")
                                                && normalized.contains(
                                                                "teaching"));

        }

        private WeeklyHeaderGeometry findWeeklyHeaderGeometry(
                        List<WeeklyPageLine> lines,
                        int headingIndex) {

                WeeklyHeaderGeometry best = null;

                int limit = Math.min(
                                lines.size(),
                                headingIndex + 40);

                for (int start = headingIndex + 1; start < limit; start++) {

                        for (int size = 1; size <= 4
                                        && start + size <= lines.size(); size++) {

                                int end = start + size - 1;

                                int page = lines.get(start)
                                                .page();

                                boolean samePage = true;

                                for (int index = start; index <= end; index++) {

                                        if (lines.get(index)
                                                        .page() != page) {

                                                samePage = false;

                                                break;
                                        }
                                }

                                if (!samePage) {
                                        break;
                                }

                                WeeklyHeaderGeometry candidate = buildWeeklyHeaderGeometry(
                                                lines,
                                                start,
                                                end);

                                if (candidate == null) {
                                        continue;
                                }

                                if (best == null
                                                || candidate.score() > best.score()
                                                || (candidate.score() == best.score()
                                                                && candidate.headerStartIndex() < best
                                                                                .headerStartIndex())) {

                                        best = candidate;
                                }
                        }

                        if (best != null
                                        && best.score() >= 6) {
                                break;
                        }
                }

                return best;
        }

        private WeeklyHeaderGeometry buildWeeklyHeaderGeometry(
                        List<WeeklyPageLine> lines,
                        int start,
                        int end) {

                List<HeaderStack> stacks = new ArrayList<>();

                final float xTolerance = 9.0f;

                for (int index = start; index <= end; index++) {

                        WeeklyPageLine line = lines.get(index);

                        for (WeeklyWordBox word : line.words()) {

                                HeaderStack target = null;

                                for (HeaderStack stack : stacks) {

                                        if (Math.abs(
                                                        stack.x
                                                                        - word.x()) <= xTolerance) {

                                                target = stack;

                                                break;
                                        }
                                }

                                if (target == null) {

                                        target = new HeaderStack(
                                                        word.x());

                                        stacks.add(
                                                        target);
                                }

                                target.append(
                                                word.text());
                        }
                }

                Map<WeeklyColumn, Float> anchors = new LinkedHashMap<>();

                for (HeaderStack stack : stacks) {

                        WeeklyColumn column = classifyWeeklyHeader(
                                        stack.compact());

                        if (column != null) {

                                anchors.putIfAbsent(
                                                column,
                                                stack.x);
                        }
                }

                if (!anchors.containsKey(
                                WeeklyColumn.WEEK)
                                || !anchors.containsKey(
                                                WeeklyColumn.TOPIC)
                                || !anchors.containsKey(
                                                WeeklyColumn.CLO)
                                || !anchors.containsKey(
                                                WeeklyColumn.ASSESSMENT)
                                || !anchors.containsKey(
                                                WeeklyColumn.LEARNING)) {

                        return null;
                }

                /*
                 * PATCH 3.2F2 v4 - semantic geometry sanity gate.
                 *
                 * The source families may swap the PHYSICAL order of Assessment and
                 * Learning (EN011/EN012), so never impose Assessment < Learning.
                 * What is stable in the audited layouts is the table skeleton:
                 * Week < Topic < CLO, and every post-CLO semantic column is physically
                 * to the right of CLO. Reject a candidate that violates only those
                 * source-backed invariants; this prevents heading words such as the
                 * "learning" in "Planned learning activities" from becoming a
                 * false LEARNING column anchor. Resources remains optional.
                 */
                float weekAnchor = anchors.get(
                                WeeklyColumn.WEEK);

                float topicAnchor = anchors.get(
                                WeeklyColumn.TOPIC);

                float cloAnchor = anchors.get(
                                WeeklyColumn.CLO);

                if (!(weekAnchor < topicAnchor
                                && topicAnchor < cloAnchor)) {

                        return null;
                }

                for (WeeklyColumn postClo : List.of(
                                WeeklyColumn.ASSESSMENT,
                                WeeklyColumn.LEARNING,
                                WeeklyColumn.RESOURCES)) {

                        Float postAnchor = anchors.get(
                                        postClo);

                        if (postAnchor != null
                                        && postAnchor <= cloAnchor) {

                                return null;
                        }
                }

                int score = anchors.size();

                return new WeeklyHeaderGeometry(
                                lines.get(start).page(),
                                start,
                                end,
                                Map.copyOf(anchors),
                                score);
        }

        private WeeklyColumn classifyWeeklyHeader(
                        String value) {

                String compact = value == null
                                ? ""
                                : value.toLowerCase(
                                                Locale.ROOT)
                                                .replaceAll(
                                                                "[^a-z]",
                                                                "");

                if (compact.contains(
                                "week")) {
                        return WeeklyColumn.WEEK;
                }

                if (compact.contains(
                                "topic")
                                || compact.equals(
                                                "content")
                                || compact.startsWith(
                                                "content")) {

                        return WeeklyColumn.TOPIC;
                }

                if (compact.contains(
                                "clo")) {

                        return WeeklyColumn.CLO;
                }

                if (compact.contains(
                                "assess")) {

                        return WeeklyColumn.ASSESSMENT;
                }

                if (compact.contains(
                                "learning")
                                || compact.startsWith(
                                                "teaching")) {

                        return WeeklyColumn.LEARNING;
                }

                if (compact.contains(
                                "resour")) {

                        return WeeklyColumn.RESOURCES;
                }

                return null;
        }

        private List<PositionalWeeklyRow> parsePositionalWeeklyRows(
                        List<WeeklyPageLine> lines,
                        WeeklyHeaderGeometry geometry) {

                /*
                 * Row-band parser.
                 *
                 * A PDF table row may span many visual lines while its TT/Week
                 * number is vertically centered. A forward accumulator can
                 * therefore attach the beginning of row N+1 to row N.
                 *
                 * Collect all source lines first, detect explicit row markers,
                 * then assign every visual line to the nearest marker.
                 */
                List<WeeklyPageLine> tableLines = new ArrayList<>();

                List<Integer> markerLineIndexes = new ArrayList<>();

                List<List<Integer>> markerWeeks = new ArrayList<>();
                int consumedWeekContinuationIndex = -1;
                for (int index = geometry.headerEndIndex() + 1; index < lines.size(); index++) {

                        WeeklyPageLine line = lines.get(index);

                        if (isWeeklyPageArtifact(
                                        line)) {
                                continue;
                        }

                        String normalized = normalizeWeeklyMatch(
                                        line.text());

                        if (isWeeklyPositionStopHeading(
                                        normalized)) {
                                break;
                        }

                        int repeatedHeaderEnd = repeatedWeeklyHeaderEnd(
                                        lines,
                                        index,
                                        geometry);

                        if (repeatedHeaderEnd >= index) {

                                index = repeatedHeaderEnd;

                                continue;
                        }

                        if (isTrailingWeeklyHeaderContinuation(
                                        lines,
                                        index,
                                        geometry)) {

                                continue;
                        }

                        List<Integer> weeks = extractSourceWeeks(
                                        line,
                                        geometry);
                        if (index == consumedWeekContinuationIndex) {

                                weeks = List.of();

                        } else {

                                String currentWeekCell = compactWeekCell(
                                                line,
                                                geometry);

                                if (weeks.size() == 1
                                                && currentWeekCell.matches(
                                                                "\\d{1,2}[-,+]")
                                                && index + 1 < lines.size()) {

                                        WeeklyPageLine nextLine = lines.get(index + 1);

                                        String nextWeekCell = compactWeekCell(
                                                        nextLine,
                                                        geometry);

                                        List<Integer> nextWeeks = extractSourceWeeks(
                                                        nextLine,
                                                        geometry);

                                        if (nextLine.page() == line.page()
                                                        && nextWeekCell.matches(
                                                                        "\\d{1,2}")
                                                        && nextWeeks.size() == 1
                                                        && nextWeeks.get(0) > weeks.get(0)) {

                                                int first = weeks.get(0);

                                                int last = nextWeeks.get(0);

                                                List<Integer> expanded = new ArrayList<>();

                                                for (int week = first; week <= last; week++) {

                                                        expanded.add(
                                                                        week);
                                                }

                                                weeks = expanded;

                                                consumedWeekContinuationIndex = index + 1;
                                        }
                                }
                        }
                        int tableLineIndex = tableLines.size();

                        tableLines.add(
                                        line);

                        if (!weeks.isEmpty()) {

                                markerLineIndexes.add(
                                                tableLineIndex);

                                markerWeeks.add(
                                                List.copyOf(
                                                                weeks));
                        }
                }

                if (markerLineIndexes.isEmpty()) {
                        return List.of();
                }

                float pageStride = 0f;

                for (WeeklyPageLine line : tableLines) {

                        pageStride = Math.max(
                                        pageStride,
                                        line.y());
                }

                pageStride = Math.max(
                                pageStride + 100f,
                                1000f);

                List<Float> markerPositions = new ArrayList<>();

                for (Integer markerLineIndex : markerLineIndexes) {

                        WeeklyPageLine markerLine = tableLines.get(
                                        markerLineIndex);

                        markerPositions.add(
                                        weeklyRowBandPosition(
                                                        markerLine,
                                                        geometry,
                                                        pageStride));
                }

                List<WeeklyRowAccumulator> accumulators = new ArrayList<>();

                for (List<Integer> weeks : markerWeeks) {

                        accumulators.add(
                                        new WeeklyRowAccumulator(
                                                        weeks));
                }

                for (WeeklyPageLine line : tableLines) {

                        float position = weeklyRowBandPosition(
                                        line,
                                        geometry,
                                        pageStride);

                        int target = 0;

                        float bestDistance = Math.abs(
                                        position
                                                        - markerPositions.get(0));

                        for (int marker = 1; marker < markerPositions.size(); marker++) {

                                float distance = Math.abs(
                                                position
                                                                - markerPositions.get(
                                                                                marker));

                                if (distance < bestDistance) {

                                        target = marker;

                                        bestDistance = distance;
                                }
                        }

                        appendWeeklyPositionLine(
                                        accumulators.get(
                                                        target),
                                        line,
                                        geometry);
                }

                List<PositionalWeeklyRow> rows = new ArrayList<>();

                for (WeeklyRowAccumulator accumulator : accumulators) {

                        PositionalWeeklyRow completed = accumulator.toRow();

                        if (completed != null) {

                                rows.add(
                                                completed);
                        }
                }

                return rows;
        }

        private boolean isTrailingWeeklyHeaderContinuation(
                        List<WeeklyPageLine> lines,
                        int index,
                        WeeklyHeaderGeometry geometry) {

                if (index <= geometry.headerEndIndex()
                                || index >= lines.size()) {

                        return false;
                }

                WeeklyPageLine line = lines.get(index);

                WeeklyPageLine headerEnd = lines.get(
                                geometry.headerEndIndex());

                if (line.page() != headerEnd.page()) {

                        return false;
                }

                float deltaY = line.y()
                                - headerEnd.y();

                if (deltaY < 0f
                                || deltaY > 16f) {

                        return false;
                }

                String normalized = normalizeWeeklyMatch(
                                line.text());

                /*
                 * English split header.
                 */
                if (normalized.equals(
                                "activities")
                                || normalized.equals(
                                                "activity")) {

                        Float learningX = geometry.anchors()
                                        .get(
                                                        WeeklyColumn.LEARNING);

                        if (learningX == null
                                        || line.words().isEmpty()) {

                                return false;
                        }

                        for (WeeklyWordBox word : line.words()) {

                                if (Math.abs(
                                                word.x()
                                                                - learningX) > 12f) {

                                        return false;
                                }
                        }

                        return true;
                }

                /*
                 * Legacy Vietnamese table:
                 *
                 * TT | Nội dung | LO | Hoạt động... | Đánh giá
                 * (Tiết) giảng dạy
                 *
                 * The second physical line is still header text.
                 */

                return false;
        }

        private float weeklyRowBandPosition(
                        WeeklyPageLine line,
                        WeeklyHeaderGeometry geometry,
                        float pageStride) {

                return (line.page()
                                - geometry.page()) * pageStride
                                + line.y();
        }

        private int repeatedWeeklyHeaderEnd(
                        List<WeeklyPageLine> lines,
                        int start,
                        WeeklyHeaderGeometry sourceGeometry) {

                /*
                 * PATCH 3.2F2 v5
                 *
                 * Do not look ahead from an arbitrary data row and then call the whole
                 * window a repeated header. That swallowed EN011 week 9 because the
                 * four-line window beginning at the week-9 row happened to end inside
                 * the repeated table header.
                 *
                 * A repeated header may start only when THIS line contains a semantic
                 * header token at approximately the same X anchor as the accepted
                 * source header. Then choose the highest-scoring header geometry within
                 * the next four same-page visual lines; on equal score keep the
                 * shortest window so real row data is never consumed speculatively.
                 */
                if (!isWeeklyRepeatedHeaderStart(
                                lines.get(start),
                                sourceGeometry)) {

                        return -1;
                }

                WeeklyHeaderGeometry best = null;

                for (int size = 1; size <= 4
                                && start + size <= lines.size(); size++) {

                        int end = start + size - 1;

                        int page = lines.get(start)
                                        .page();

                        boolean samePage = true;

                        for (int index = start; index <= end; index++) {

                                if (lines.get(index)
                                                .page() != page) {

                                        samePage = false;

                                        break;
                                }
                        }

                        if (!samePage) {
                                break;
                        }

                        WeeklyHeaderGeometry candidate = buildWeeklyHeaderGeometry(
                                        lines,
                                        start,
                                        end);

                        if (candidate == null) {
                                continue;
                        }

                        if (best == null
                                        || candidate.score() > best.score()) {

                                best = candidate;
                        }
                }

                if (best == null) {
                        return -1;
                }

                int end = best.headerEndIndex();

                /*
                 * EN011/EN012 place the word "activities" on the line immediately
                 * after the rest of the repeated header. It is header text, not weekly
                 * row data. Consume it only when it is the whole visual line and it
                 * sits on the accepted Learning-column X anchor.
                 */
                if (end + 1 < lines.size()) {

                        WeeklyPageLine following = lines.get(end + 1);

                        if (following.page() == lines.get(start)
                                        .page()
                                        && isWeeklyLearningHeaderContinuation(
                                                        following,
                                                        sourceGeometry)) {

                                end++;
                        }
                }

                return end;
        }

        private boolean isWeeklyRepeatedHeaderStart(
                        WeeklyPageLine line,
                        WeeklyHeaderGeometry geometry) {

                final float anchorTolerance = 18f;

                for (WeeklyWordBox word : line.words()) {

                        WeeklyColumn column = classifyWeeklyHeader(
                                        word.text());

                        if (column == null) {
                                continue;
                        }

                        Float sourceAnchor = geometry.anchors()
                                        .get(
                                                        column);

                        if (sourceAnchor != null
                                        && Math.abs(
                                                        word.x()
                                                                        - sourceAnchor) <= anchorTolerance) {

                                return true;
                        }
                }

                return false;
        }

        private boolean isWeeklyLearningHeaderContinuation(
                        WeeklyPageLine line,
                        WeeklyHeaderGeometry geometry) {

                if (!normalizeWeeklyMatch(
                                line.text())
                                .equals(
                                                "activities")) {

                        return false;
                }

                Float learningX = geometry.anchors()
                                .get(
                                                WeeklyColumn.LEARNING);

                if (learningX == null) {
                        return false;
                }

                for (WeeklyWordBox word : line.words()) {

                        if (normalizeWeeklyMatch(
                                        word.text())
                                        .equals(
                                                        "activities")
                                        && Math.abs(
                                                        word.x()
                                                                        - learningX) <= 12f) {

                                return true;
                        }
                }

                return false;
        }

        private boolean isWeeklyPositionStopHeading(
                        String value) {

                String normalized = value == null
                                ? ""
                                : value;

                String ascii = weeklyAscii(
                                normalized);

                return normalized.matches(
                                "^(?:\\d+\\.?\\s*)?assessment plan\\b.*")
                                || normalized.matches(
                                                "^(?:\\d+\\.?\\s*)?examination forms?\\b.*")
                                || normalized.matches(
                                                "^(?:\\d+\\.?\\s*)?study and examination\\b.*")
                                || normalized.matches(
                                                "^(?:\\d+\\.?\\s*)?reading list\\b.*")
                                || normalized.matches(
                                                "^(?:\\d+\\.?\\s*)?rubrics?\\b.*")
                                || normalized.matches(
                                                "^(?:\\d+\\.?\\s*)?date revised\\b.*")
                                || ascii.matches(
                                                "^(?:\\d+\\.?\\s*)?danh gia mon hoc\\b.*");
        }

        private boolean isWeeklyPageArtifact(
                        WeeklyPageLine line) {

                String value = collapse(
                                line.text());

                if (value.matches(
                                "\\d{1,4}\\s*/\\s*\\d{1,4}")) {
                        return true;
                }

                return line.y() < 60f
                                && value.matches(
                                                "\\d{1,4}");
        }

        private List<Integer> extractSourceWeeks(
                        WeeklyPageLine line,
                        WeeklyHeaderGeometry geometry) {

                return extractSourceWeekCell(
                                line,
                                geometry)
                                .weeks();
        }

        private String compactWeekCell(
                        WeeklyPageLine line,
                        WeeklyHeaderGeometry geometry) {

                float weekX = geometry.anchor(
                                WeeklyColumn.WEEK);

                float topicX = geometry.anchor(
                                WeeklyColumn.TOPIC);

                float left = weekX - 25f;

                StringBuilder result = new StringBuilder();

                for (WeeklyWordBox word : line.words()) {

                        if (word.right() < left
                                        || word.x() >= topicX) {

                                continue;
                        }

                        String value = word.text() == null
                                        ? ""
                                        : word.text()
                                                        .replaceAll(
                                                                        "\\s+",
                                                                        "");

                        if (value.matches(
                                        "\\d{1,2}[-,+]?|[-,+]")) {

                                result.append(
                                                value);
                        }
                }

                return result.toString();
        }

        private WeeklyWeekCell extractSourceWeekCell(
                        WeeklyPageLine line,
                        WeeklyHeaderGeometry geometry) {

                float weekX = geometry.anchor(
                                WeeklyColumn.WEEK);

                float topicX = geometry.anchor(
                                WeeklyColumn.TOPIC);

                float startBoundary = (weekX
                                + topicX)
                                / 2f;

                float left = weekX
                                - 25f;

                List<WeeklyWordBox> candidates = new ArrayList<>();

                for (WeeklyWordBox word : line.words()) {

                        if (word.right() < left
                                        || word.x() >= topicX) {

                                continue;
                        }

                        candidates.add(
                                        word);
                }

                /*
                 * PATCH 3.2F2 v5
                 *
                 * EN011 proves that a Topic word can start a fraction to the LEFT of
                 * the Week/Topic midpoint ("Appropriate": x~118.1 while midpoint is
                 * ~119.2). Therefore never concatenate arbitrary words from the Week
                 * band. A source week cell must START with explicit numeric week
                 * syntax whose word centre is on the Week side.
                 *
                 * Once a real week token has started, allow only adjacent explicit
                 * range/group fragments up to Topic X. This preserves source forms
                 * such as "3 - 4", "8, 9", "5,6" and "8 + 9" without treating Topic
                 * enumerators as week numbers.
                 */
                for (int index = 0; index < candidates.size(); index++) {

                        WeeklyWordBox first = candidates.get(index);

                        float center = (first.x()
                                        + first.right())
                                        / 2f;

                        String firstCompact = first.text() == null
                                        ? ""
                                        : first.text()
                                                        .replaceAll(
                                                                        "\\s+",
                                                                        "");

                        boolean explicitGroupedWeekStart = firstCompact.matches(
                                        "\\d{1,2}[-,+].*");

                        if ((!explicitGroupedWeekStart
                                        && center > startBoundary)
                                        || !isWeeklyWeekStartWord(
                                                        first.text())) {

                                continue;
                        }
                        List<WeeklyWordBox> parts = new ArrayList<>();

                        parts.add(
                                        first);

                        WeeklyWordBox previous = first;

                        for (int nextIndex = index + 1; nextIndex < candidates.size(); nextIndex++) {

                                WeeklyWordBox next = candidates.get(
                                                nextIndex);

                                float gap = next.x()
                                                - previous.right();

                                if (next.x() >= topicX
                                                || gap > 8f
                                                || !isWeeklyWeekContinuationWord(
                                                                next.text())) {

                                        break;
                                }

                                parts.add(
                                                next);

                                previous = next;
                        }

                        /*
                         * Prefer the longest valid explicit source expression. If a PDF
                         * word split leaves a harmless extra numeric fragment, fall back
                         * only to a shorter prefix that is itself complete week syntax.
                         */
                        for (int size = parts.size(); size >= 1; size--) {

                                List<WeeklyWordBox> prefix = new ArrayList<>(
                                                parts.subList(
                                                                0,
                                                                size));

                                List<Integer> weeks = parseWeeklyWeekExpression(
                                                prefix);

                                if (!weeks.isEmpty()) {

                                        return new WeeklyWeekCell(
                                                        List.copyOf(
                                                                        prefix),
                                                        List.copyOf(
                                                                        weeks));
                                }
                        }
                }

                return new WeeklyWeekCell(
                                List.of(),
                                List.of());
        }

        private boolean isWeeklyWeekStartWord(
                        String value) {

                if (value == null
                                || value.isBlank()) {

                        return false;
                }

                String compact = value.replaceAll(
                                "\\s+",
                                "");

                return compact.matches(
                                "\\d{1,2}(?:[-,+]\\d{1,2})*[.;:,+-]?");
        }

        private boolean isWeeklyWeekContinuationWord(
                        String value) {

                if (value == null
                                || value.isBlank()) {

                        return false;
                }

                String compact = value.replaceAll(
                                "\\s+",
                                "");

                return compact.matches(
                                "[-,+]")
                                || compact.matches(
                                                "[-,+]?\\d{1,2}[.;:]?");
        }

        private List<Integer> parseWeeklyWeekExpression(
                        List<WeeklyWordBox> words) {

                if (words == null
                                || words.isEmpty()) {

                        return List.of();
                }

                StringBuilder raw = new StringBuilder();

                for (WeeklyWordBox word : words) {

                        raw.append(
                                        word.text());
                }

                String compact = raw.toString()
                                .replaceAll(
                                                "\\s+",
                                                "")
                                .replaceAll(
                                                "[.;:]$",
                                                "");

                if (!compact.matches(
                                "\\d{1,2}(?:[-,+]\\d{1,2})*")) {

                        return List.of();
                }

                List<Integer> result = new ArrayList<>();

                for (String group : compact.split(
                                "[,+]")) {

                        String[] values = group.split("-");

                        if (values.length == 0) {
                                return List.of();
                        }

                        List<Integer> numbers = new ArrayList<>();

                        for (String value : values) {

                                if (value.isBlank()) {
                                        return List.of();
                                }

                                int week;

                                try {
                                        week = Integer.parseInt(
                                                        value);
                                } catch (NumberFormatException exception) {
                                        return List.of();
                                }

                                /*
                                 * The CS2021 source explicitly contains teaching-plan
                                 * week/session numbers beyond 15, including 16, 17
                                 * and ranges reaching 20. Preserve only explicit
                                 * source values; do not fabricate arbitrary weeks.
                                 */
                                if (week < 1
                                                || week > 30) {

                                        return List.of();
                                }

                                numbers.add(
                                                week);
                        }

                        if (numbers.size() == 1) {

                                int week = numbers.get(0);

                                if (!result.contains(
                                                week)) {

                                        result.add(
                                                        week);
                                }

                                continue;
                        }

                        if (numbers.size() == 2) {

                                int first = numbers.get(0);

                                int last = numbers.get(1);

                                if (first > last) {
                                        return List.of();
                                }

                                for (int week = first; week <= last; week++) {

                                        if (!result.contains(
                                                        week)) {

                                                result.add(
                                                                week);
                                        }
                                }

                                continue;
                        }

                        /*
                         * Source forms such as 5-6-7 explicitly enumerate
                         * several consecutive teaching weeks. Preserve the
                         * stated values rather than failing on the second '-'.
                         */
                        for (Integer week : numbers) {

                                if (!result.contains(
                                                week)) {

                                        result.add(
                                                        week);
                                }
                        }
                }

                return result;
        }

        private void appendWeeklyPositionLine(
                        WeeklyRowAccumulator row,
                        WeeklyPageLine line,
                        WeeklyHeaderGeometry geometry) {

                float weekX = geometry.anchor(
                                WeeklyColumn.WEEK);

                float topicX = geometry.anchor(
                                WeeklyColumn.TOPIC);

                float cloX = geometry.anchor(
                                WeeklyColumn.CLO);

                float weekRight = (weekX
                                + topicX)
                                / 2f;

                WeeklyWeekCell weekCell = extractSourceWeekCell(
                                line,
                                geometry);

                List<WeeklyWordBox> weekWords = weekCell.words();

                List<WeeklyWordBox> cloWords = new ArrayList<>();

                for (WeeklyWordBox word : line.words()) {

                        if (Math.abs(
                                        word.x()
                                                        - cloX) <= 42f
                                        && isCloPositionWord(
                                                        word.text())) {

                                cloWords.add(
                                                word);
                        }
                }

                String cloFragment = cleanPositionalField(
                                joinWeeklyWords(
                                                cloWords));

                if (!cloFragment.isBlank()) {

                        row.clo = appendPositionalField(
                                        row.clo,
                                        cloFragment);
                }

                float topicCutoff = cloX
                                - 5f;

                for (WeeklyWordBox word : line.words()) {

                        if (weekWords.contains(
                                        word)) {

                                continue;
                        }

                        if (cloWords.contains(
                                        word)) {

                                continue;
                        }

                        /*
                         * Use the visual centre for the Week/Topic boundary. EN011's
                         * "Appropriate" begins just left of the midpoint but belongs
                         * clearly to Topic; source week fragments that cross the midpoint
                         * were already captured above as explicit weekWords.
                         */
                        float wordCenter = (word.x()
                                        + word.right())
                                        / 2f;

                        if (wordCenter <= weekRight) {

                                continue;
                        }

                        if (word.x() < topicCutoff) {

                                row.topic = appendPositionalField(
                                                row.topic,
                                                word.text());

                                continue;
                        }

                        WeeklyColumn target = resolvePostCloColumn(
                                        word.x(),
                                        geometry);

                        if (target == WeeklyColumn.ASSESSMENT) {

                                row.assessments = appendPositionalField(
                                                row.assessments,
                                                word.text());

                        } else if (target == WeeklyColumn.LEARNING) {

                                row.learningActivities = appendPositionalField(
                                                row.learningActivities,
                                                word.text());

                        } else if (target == WeeklyColumn.RESOURCES) {

                                row.resources = appendPositionalField(
                                                row.resources,
                                                word.text());
                        }
                }
        }

        private WeeklyColumn resolvePostCloColumn(
                        float x,
                        WeeklyHeaderGeometry geometry) {

                List<ColumnAnchor> columns = new ArrayList<>();

                for (WeeklyColumn column : List.of(
                                WeeklyColumn.ASSESSMENT,
                                WeeklyColumn.LEARNING,
                                WeeklyColumn.RESOURCES)) {

                        Float anchor = geometry.anchors()
                                        .get(
                                                        column);

                        if (anchor != null) {

                                columns.add(
                                                new ColumnAnchor(
                                                                column,
                                                                anchor));
                        }
                }

                columns.sort(
                                Comparator.comparingDouble(
                                                ColumnAnchor::x));

                if (columns.isEmpty()) {
                        return null;
                }

                float cloX = geometry.anchor(
                                WeeklyColumn.CLO);

                float firstBoundary = (cloX
                                + columns.get(0)
                                                .x())
                                / 2f;

                if (x < firstBoundary) {
                        return null;
                }

                for (int index = 0; index < columns.size(); index++) {

                        float left = index == 0
                                        ? firstBoundary
                                        : (columns.get(index - 1)
                                                        .x()
                                                        + columns.get(index)
                                                                        .x())
                                                        / 2f;

                        float right = index + 1 < columns.size()
                                        ? (columns.get(index)
                                                        .x()
                                                        + columns.get(index + 1)
                                                                        .x())
                                                        / 2f
                                        : Float.POSITIVE_INFINITY;

                        if (x >= left
                                        && x < right) {

                                return columns.get(index)
                                                .column();
                        }
                }

                return null;
        }

        private boolean isCloPositionWord(
                        String value) {

                if (value == null
                                || value.isBlank()) {

                        return false;
                }

                String compact = value.replaceAll(
                                "\\s+",
                                "")
                                .replaceAll(
                                                "(?i)^L0\\.",
                                                "LO.")
                                .replaceAll(
                                                "(?i)^LO\\.[lI](?=[,;\\-]?$)",
                                                "LO.1");

                return compact.matches(
                                "(?i)(?:(?:CLO|LO)\\.?\\d{1,2}|\\d{1,2})[,;\\-]?");
        }

        private String joinWeeklyWords(
                        List<WeeklyWordBox> words) {

                StringBuilder result = new StringBuilder();

                for (WeeklyWordBox word : words) {

                        if (result.length() > 0) {
                                result.append(
                                                " ");
                        }

                        result.append(
                                        word.text());
                }

                return result.toString();
        }

        private String appendPositionalField(
                        String current,
                        String fragment) {

                String left = cleanPositionalField(
                                current);

                String right = cleanPositionalField(
                                fragment);

                if (right.isBlank()) {
                        return left;
                }

                if (left.isBlank()) {
                        return right;
                }

                if (left.endsWith(
                                "-")) {

                        return cleanPositionalField(
                                        left
                                                        + right);
                }

                if (right.matches(
                                "^[,.;:)\\]].*")) {

                        return cleanPositionalField(
                                        left
                                                        + right);
                }

                return cleanPositionalField(
                                left
                                                + " "
                                                + right);
        }

        private String cleanPositionalField(
                        String value) {

                if (value == null) {
                        return "";
                }

                return collapse(
                                value)
                                .replaceAll(
                                                "(?i)\\bL[O0]\\.\\s*[lI]\\b",
                                                "LO.1")
                                .replaceAll(
                                                "(?i)\\bL0\\.",
                                                "LO.")
                                .replaceAll(
                                                "\\s+([,.;:])",
                                                "$1")
                                .replaceAll(
                                                "\\[\\s+",
                                                "[")
                                .replaceAll(
                                                "\\s+\\]",
                                                "]")
                                .replaceAll(
                                                "(?i)\\bIn-\\s+class\\b",
                                                "In-class")
                                .trim();
        }

        private String nullIfBlank(
                        String value) {

                String cleaned = cleanPositionalField(
                                value);

                return cleaned.isBlank()
                                ? null
                                : cleaned;
        }

        private List<Integer> parseOrdinalReferences(
                        String value) {

                if (value == null
                                || value.isBlank()) {
                        return List.of();
                }

                String compact = value.toUpperCase(
                                Locale.ROOT)
                                .replace(
                                                "CLO",
                                                "")
                                .replaceAll(
                                                "\\s+",
                                                "");

                List<Integer> result = new ArrayList<>();

                Matcher matcher = Pattern.compile(
                                "\\d{1,2}(?:-\\d{1,2})?")
                                .matcher(
                                                compact);

                while (matcher.find()) {

                        String token = matcher.group();

                        if (token.contains(
                                        "-")) {

                                String[] bounds = token.split(
                                                "-",
                                                2);

                                int first = Integer.parseInt(
                                                bounds[0]);

                                int last = Integer.parseInt(
                                                bounds[1]);

                                if (first >= 1
                                                && last <= 30
                                                && first <= last) {

                                        for (int ordinal = first; ordinal <= last; ordinal++) {

                                                if (!result.contains(
                                                                ordinal)) {

                                                        result.add(
                                                                        ordinal);
                                                }
                                        }
                                }

                        } else {

                                int ordinal = Integer.parseInt(
                                                token);

                                if (ordinal >= 1
                                                && ordinal <= 30
                                                && !result.contains(
                                                                ordinal)) {

                                        result.add(
                                                        ordinal);
                                }
                        }
                }

                return result;
        }

        private boolean isWeeklyExaminationTopic(
                        String value) {

                String normalized = normalizeWeeklyMatch(
                                value);

                return normalized.startsWith(
                                "midterm")
                                || normalized.startsWith(
                                                "final exam")
                                || normalized.startsWith(
                                                "final examination");
        }

        private String normalizeWeeklyMatch(
                        String value) {

                if (value == null) {
                        return "";
                }

                return value.toLowerCase(
                                Locale.ROOT)
                                .replaceAll(
                                                "\\s+",
                                                " ")
                                .trim();
        }

        private List<WeeklyVisualLine> buildWeeklyVisualLines(
                        List<WeeklyGlyph> glyphs) {

                List<WeeklyGlyph> ordered = new ArrayList<>(
                                glyphs);

                ordered.sort(
                                Comparator
                                                .comparingDouble(
                                                                WeeklyGlyph::y)
                                                .thenComparingDouble(
                                                                WeeklyGlyph::x));

                List<List<WeeklyGlyph>> groups = new ArrayList<>();

                final float yTolerance = 2.2f;

                for (WeeklyGlyph glyph : ordered) {

                        List<WeeklyGlyph> target = null;

                        if (!groups.isEmpty()) {

                                List<WeeklyGlyph> last = groups.get(
                                                groups.size() - 1);

                                float baseY = last.get(0)
                                                .y();

                                if (Math.abs(
                                                baseY
                                                                - glyph.y()) <= yTolerance) {

                                        target = last;
                                }
                        }

                        if (target == null) {

                                target = new ArrayList<>();

                                groups.add(
                                                target);
                        }

                        target.add(
                                        glyph);
                }

                List<WeeklyVisualLine> result = new ArrayList<>();

                for (List<WeeklyGlyph> group : groups) {

                        group.sort(
                                        Comparator.comparingDouble(
                                                        WeeklyGlyph::x));

                        List<WeeklyWordBox> words = weeklyWordsFromGlyphs(
                                        group);

                        if (words.isEmpty()) {
                                continue;
                        }

                        String text = words.stream()
                                        .map(
                                                        WeeklyWordBox::text)
                                        .reduce(
                                                        "",
                                                        (left, right) -> left.isBlank()
                                                                        ? right
                                                                        : left
                                                                                        + " "
                                                                                        + right);

                        result.add(
                                        new WeeklyVisualLine(
                                                        group.get(0)
                                                                        .y(),
                                                        text,
                                                        words));
                }

                return result;
        }

        private List<WeeklyWordBox> weeklyWordsFromGlyphs(
                        List<WeeklyGlyph> glyphs) {

                List<WeeklyWordBox> result = new ArrayList<>();

                StringBuilder current = new StringBuilder();

                float startX = 0f;

                float endX = 0f;

                float previousRight = -1f;

                float previousHeight = 0f;

                for (WeeklyGlyph glyph : glyphs) {

                        String value = glyph.text();

                        if (value == null
                                        || value.isBlank()) {
                                continue;
                        }

                        float gap = previousRight < 0
                                        ? 0
                                        : glyph.x()
                                                        - previousRight;

                        float threshold = Math.max(
                                        2.4f,
                                        previousHeight
                                                        * 0.28f);

                        boolean newWord = current.length() > 0
                                        && gap > threshold;

                        if (newWord) {

                                result.add(
                                                new WeeklyWordBox(
                                                                startX,
                                                                endX,
                                                                current.toString()));

                                current.setLength(
                                                0);
                        }

                        if (current.length() == 0) {

                                startX = glyph.x();
                        }

                        current.append(
                                        value);

                        endX = glyph.x()
                                        + glyph.width();

                        previousRight = endX;

                        previousHeight = glyph.height();
                }

                if (current.length() > 0) {

                        result.add(
                                        new WeeklyWordBox(
                                                        startX,
                                                        endX,
                                                        current.toString()));
                }

                return result;
        }

        private enum WeeklyColumn {
                WEEK,
                TOPIC,
                CLO,
                ASSESSMENT,
                LEARNING,
                RESOURCES
        }

        private record WeeklyGlyph(
                        String text,
                        float x,
                        float y,
                        float width,
                        float height) {
        }

        private record WeeklyWordBox(
                        float x,
                        float right,
                        String text) {
        }

        private record WeeklyVisualLine(
                        float y,
                        String text,
                        List<WeeklyWordBox> words) {
        }

        private record WeeklyPageLine(
                        int page,
                        float y,
                        String text,
                        List<WeeklyWordBox> words) {
        }

        private record WeeklyHeaderGeometry(
                        int page,
                        int headerStartIndex,
                        int headerEndIndex,
                        Map<WeeklyColumn, Float> anchors,
                        int score) {

                private float anchor(
                                WeeklyColumn column) {

                        Float value = anchors.get(
                                        column);

                        if (value == null) {

                                throw new IllegalStateException(
                                                "Missing weekly column anchor: "
                                                                + column);
                        }

                        return value;
                }
        }

        private record ColumnAnchor(
                        WeeklyColumn column,
                        float x) {
        }

        private record WeeklyWeekCell(
                        List<WeeklyWordBox> words,
                        List<Integer> weeks) {
        }

        private record PositionalWeeklyRow(
                        List<Integer> weeks,
                        String topic,
                        String clo,
                        String assessments,
                        String learningActivities,
                        String resources) {
        }

        private static final class HeaderStack {

                private final float x;

                private final StringBuilder text = new StringBuilder();

                private HeaderStack(
                                float x) {

                        this.x = x;
                }

                private void append(
                                String value) {

                        if (value == null
                                        || value.isBlank()) {
                                return;
                        }

                        text.append(
                                        value);
                }

                private String compact() {
                        return text.toString();
                }
        }

        private final class WeeklyRowAccumulator {

                private final List<Integer> weeks;

                private String topic = "";

                private String clo = "";

                private String assessments = "";

                private String learningActivities = "";

                private String resources = "";

                private WeeklyRowAccumulator(
                                List<Integer> weeks) {

                        this.weeks = List.copyOf(
                                        weeks);
                }

                private PositionalWeeklyRow toRow() {

                        String cleanedTopic = cleanPositionalField(
                                        topic);

                        String cleanedClo = cleanPositionalField(
                                        clo);

                        String cleanedAssessments = cleanPositionalField(
                                        assessments);

                        String cleanedLearning = cleanPositionalField(
                                        learningActivities);

                        String cleanedResources = cleanPositionalField(
                                        resources);

                        if (weeks.isEmpty()) {
                                return null;
                        }

                        return new PositionalWeeklyRow(
                                        weeks,
                                        cleanedTopic,
                                        cleanedClo,
                                        cleanedAssessments,
                                        cleanedLearning,
                                        cleanedResources);
                }
        }

        private static final class WeeklyPositionCollector
                        extends PDFTextStripper {

                private final List<WeeklyGlyph> glyphs = new ArrayList<>();

                private WeeklyPositionCollector()
                                throws IOException {

                        super();

                        setSortByPosition(
                                        true);
                }

                @Override
                protected void processTextPosition(
                                TextPosition text) {

                        String unicode = text.getUnicode();

                        if (unicode != null
                                        && !unicode.isBlank()) {

                                glyphs.add(
                                                new WeeklyGlyph(
                                                                unicode,
                                                                text.getXDirAdj(),
                                                                text.getYDirAdj(),
                                                                text.getWidthDirAdj(),
                                                                text.getHeightDir()));
                        }

                        super.processTextPosition(
                                        text);
                }

                private List<WeeklyGlyph> glyphs() {
                        return glyphs;
                }
        }

        private String extractWeeklyTopic(String firstLine) {
                return collapse(firstLine.replaceFirst("^\\d{1,2}\\s+", "")
                                .replaceFirst("\\s+\\d+(?:\\s*,\\s*\\d+)*\\s+Quiz.*$", ""));
        }

        void parseCloPloMatrix(
                        String text,
                        SyllabusImportData data) {

                LabelMatch matrixHeading = firstFlexibleLabel(
                                text,
                                0,
                                "Learning Outcomes Matrix",
                                "CLO-PLO Mapping Matrix");

                if (matrixHeading == null) {
                        data.setCloPloMappings(
                                        new ArrayList<>());
                        return;
                }

                int matrixStart = matrixHeading.end();

                LabelMatch matrixEndHeading = firstFlexibleLabel(
                                text,
                                matrixStart,
                                "Planned learning activities",
                                "Teaching and learning activities");

                int matrixEnd = matrixEndHeading == null
                                ? text.length()
                                : matrixEndHeading.start();

                String matrix = text.substring(
                                matrixStart,
                                matrixEnd);

                List<SyllabusImportData.CloPloMappingItem> mappings = new ArrayList<>();

                Matcher rowMatcher = Pattern.compile(
                                "(?m)^(\\d+)(.*[xX].*)$")
                                .matcher(matrix);

                while (rowMatcher.find()) {

                        int ploNumber = 0;

                        Matcher markMatcher = Pattern.compile(
                                        "([ \\t]+)([xX]+)")
                                        .matcher(
                                                        rowMatcher.group(2));

                        while (markMatcher.find()) {

                                ploNumber = ploNumber == 0
                                                ? markMatcher
                                                                .group(1)
                                                                .length()
                                                : ploNumber
                                                                + Math.max(
                                                                                1,
                                                                                markMatcher
                                                                                                .group(1)
                                                                                                .length()
                                                                                                - 1);

                                String mark = markMatcher.group(2)
                                                .toLowerCase(
                                                                Locale.ROOT);

                                float weight = switch (mark.length()) {

                                        case 1 -> 33.33f;

                                        case 2 -> 66.67f;

                                        default -> 100f;
                                };

                                mappings.add(
                                                SyllabusImportData.CloPloMappingItem
                                                                .builder()

                                                                .cloCode(
                                                                                "CLO"
                                                                                                + rowMatcher
                                                                                                                .group(1))

                                                                .ploCode(
                                                                                "PLO"
                                                                                                + Math.max(
                                                                                                                1,
                                                                                                                ploNumber))

                                                                .value(mark)

                                                                .contributionWeight(
                                                                                weight)

                                                                .build());
                        }
                }

                data.setCloPloMappings(
                                mappings);
        }

        private void parseAssessments(String text, SyllabusImportData data) {
                String assessmentSection = sectionUntilAny(
                                text,
                                "Assessment plan",
                                "Rubrics (optional)",
                                "Rubrics");
                String header = assessmentSection.lines()
                                .filter(line -> containsIgnoreCase(line, "Assessment Type")
                                                && containsIgnoreCase(line, "CLO1"))
                                .findFirst().orElse("");
                List<Integer> cloColumns = new ArrayList<>();
                Matcher headerMatcher = Pattern.compile("CLO\\s*(\\d+)", Pattern.CASE_INSENSITIVE).matcher(header);
                while (headerMatcher.find())
                        cloColumns.add(headerMatcher.start());

                Matcher matcher = ASSESSMENT_ROW.matcher(assessmentSection);
                List<AssessmentImportData> assessments = new ArrayList<>();
                List<SyllabusImportData.AssessmentCloMappingItem> mappings = new ArrayList<>();
                int index = 1;
                while (matcher.find()) {
                        String sourceName = collapse(
                                        matcher.group(1));

                        String normalizedName = sourceName.toLowerCase(
                                        Locale.ROOT);

                        String name;
                        String type;

                        if (normalizedName.startsWith("quiz")
                                        || normalizedName.startsWith("exercises")) {

                                name = "Quiz / Assignment";
                                type = "ASSIGNMENT";

                        } else if (normalizedName.equals("labs")
                                        || normalizedName.equals("lab")
                                        || normalizedName.startsWith("laboratory")) {

                                name = sourceName;
                                type = "LAB_REPORT";

                        } else if (normalizedName.startsWith("midterm")) {

                                name = "Midterm examination";
                                type = "MIDTERM_EXAM";

                        } else if (normalizedName.startsWith("final")) {

                                name = "Final examination";
                                type = "FINAL_EXAM";

                        } else if (normalizedName.contains("project")) {

                                name = sourceName;
                                type = "PROJECT";

                        } else if (normalizedName.contains("presentation")) {

                                name = sourceName;
                                type = "PRESENTATION";

                        } else if (normalizedName.contains("participation")
                                        || normalizedName.contains("attendance")) {

                                name = sourceName;
                                type = "PARTICIPATION";

                        } else if (normalizedName.contains("assignment")) {

                                name = sourceName;
                                type = "ASSIGNMENT";

                        } else {

                                name = sourceName;
                                type = "ASSESSMENT";
                        }
                        assessments.add(AssessmentImportData.builder()
                                        .name(name)
                                        .assessmentType(type)
                                        .weightPercent(Float.parseFloat(matcher.group(2)))
                                        .minScore(0f)
                                        .maxScore(100f)
                                        .orderIndex(index)
                                        .build());
                        String percentageCells = matcher.group(3);
                        List<Double> percentages = new ArrayList<>();
                        Matcher percentageMatcher = Pattern.compile("(\\d+(?:\\.\\d+)?)%").matcher(percentageCells);
                        while (percentageMatcher.find()) {
                                percentages.add(Double.parseDouble(percentageMatcher.group(1)));
                        }
                        int leadingSpaces = percentageCells.length() - percentageCells.stripLeading().length();
                        int firstClo = percentages.size() >= cloColumns.size() || leadingSpaces <= 1
                                        ? 1
                                        : cloColumns.size() - percentages.size() + 1;
                        for (int percentageIndex = 0; percentageIndex < percentages.size(); percentageIndex++) {
                                mappings.add(SyllabusImportData.AssessmentCloMappingItem.builder()
                                                .assessmentIndex(index)
                                                .cloCode("CLO" + (firstClo + percentageIndex))
                                                .percentage(percentages.get(percentageIndex))
                                                .build());
                        }
                        index++;
                }
                if (assessments.isEmpty()
                                && parseLegacyVietnameseAssessments(
                                                text,
                                                data)) {

                        return;
                }
                data.setAssessments(assessments);
                data.setAssessmentCloMappings(mappings);
        }

        private boolean parseLegacyVietnameseAssessments(
                        String text,
                        SyllabusImportData data) {

                LabelMatch heading = firstFlexibleLabel(
                                text,
                                0,
                                "7. Đánh giá môn học",
                                "Đánh giá môn học");

                if (heading == null) {
                        return false;
                }

                LabelMatch endHeading = firstFlexibleLabel(
                                text,
                                heading.end(),
                                "8. Tiêu chí đánh giá chuẩn đầu ra môn học",
                                "Tiêu chí đánh giá chuẩn đầu ra môn học");

                int end = endHeading == null
                                ? text.length()
                                : endHeading.start();

                String section = collapse(
                                text.substring(
                                                heading.end(),
                                                end));

                /*
                 * Legacy CS2021 political-course assessment table:
                 *
                 * STT | Mã | Tên | Mô tả | Tỷ trọng | Hình thức | LO
                 *
                 * PDF text can wrap the ordinal and code onto separate physical
                 * lines. collapse(...) restores whitespace without inventing data.
                 */
                Matcher rowMatcher = Pattern.compile(
                                "(?iu)(?:^|\\s)"
                                                + "(\\d+)\\s+"
                                                + "(GHW|Quiz|DIC|Die|FEX)\\b"
                                                + "(.*?)"
                                                + "(?="
                                                + "(?:\\s+\\d+\\s+"
                                                + "(?:GHW|Quiz|DIC|Die|FEX)\\b)"
                                                + "|(?:\\s+Tổng\\s+cộng\\b)"
                                                + "|$"
                                                + ")")
                                .matcher(
                                                section);

                List<AssessmentImportData> assessments = new ArrayList<>();

                int order = 1;

                while (rowMatcher.find()) {

                        String code = collapse(
                                        rowMatcher.group(2));

                        String row = collapse(
                                        rowMatcher.group(3));

                        Matcher weightMatcher = Pattern.compile(
                                        "(\\d+(?:[.,]\\d+)?)\\s*%")
                                        .matcher(
                                                        row);

                        if (!weightMatcher.find()) {
                                continue;
                        }

                        float weight = Float.parseFloat(
                                        weightMatcher.group(1)
                                                        .replace(
                                                                        ',',
                                                                        '.'));

                        String beforeWeight = collapse(
                                        row.substring(
                                                        0,
                                                        weightMatcher.start()));

                        String name = legacyVietnameseAssessmentName(
                                        code,
                                        beforeWeight);

                        assessments.add(
                                        AssessmentImportData.builder()
                                                        .name(
                                                                        name)
                                                        .nameVn(
                                                                        name)
                                                        .assessmentType(
                                                                        legacyVietnameseAssessmentType(
                                                                                        code))
                                                        .weightPercent(
                                                                        weight)
                                                        .minScore(
                                                                        0f)
                                                        .maxScore(
                                                                        100f)
                                                        .orderIndex(
                                                                        order++)
                                                        .build());
                }

                if (assessments.isEmpty()) {
                        return false;
                }

                data.setAssessments(
                                assessments);

                /*
                 * Source LO column expresses membership only.
                 * AssessmentCloMappingItem requires a percentage.
                 * Do not invent 100%, therefore leave mappings empty.
                 */
                data.setAssessmentCloMappings(
                                new ArrayList<>());

                return true;
        }

        private String legacyVietnameseAssessmentName(
                        String code,
                        String sourcePrefix) {

                String normalized = weeklyAscii(
                                sourcePrefix);

                return switch (code.toUpperCase(
                                Locale.ROOT)) {

                        case "GHW" ->
                                normalized.startsWith(
                                                "thuyet trinh nhom")
                                                                ? "Thuyết trình nhóm"
                                                                : code;

                        case "QUIZ" ->
                                normalized.startsWith(
                                                "bai thi giua ky")
                                                                ? "Bài thi giữa kỳ"
                                                                : code;

                        case "DIC", "DIE" -> {

                                if (normalized.startsWith(
                                                "thao luan chuyen can tai lop")) {

                                        yield "Thảo luận, chuyên cần tại lớp";
                                }

                                if (normalized.startsWith(
                                                "thao luan, chuyen can tai lop")) {

                                        yield "Thảo luận, chuyên cần tại lớp";
                                }

                                if (normalized.startsWith(
                                                "thao luan tai lop")) {

                                        yield "Thảo luận tại lớp";
                                }

                                yield code;
                        }

                        case "FEX" ->
                                normalized.startsWith(
                                                "thi cuoi ky")
                                                                ? "Thi cuối kỳ"
                                                                : code;

                        default ->
                                code;
                };
        }

        private String legacyVietnameseAssessmentType(
                        String code) {

                return switch (code.toUpperCase(
                                Locale.ROOT)) {

                        case "GHW" ->
                                "PRESENTATION";

                        case "QUIZ" ->
                                "MIDTERM_EXAM";

                        case "DIC", "DIE" ->
                                "PARTICIPATION";

                        case "FEX" ->
                                "FINAL_EXAM";

                        default ->
                                "ASSESSMENT";
                };
        }

        private void parseReadingList(String text, SyllabusImportData data) {
                List<SyllabusImportData.ReadingItem> readings = new ArrayList<>();

                // Reading entries frequently wrap over several physical PDF lines. Isolate the
                // semantic section first so that the next numbered syllabus heading is never
                // mistaken for another bibliography item.
                LabelMatch readingHeading = firstFlexibleLabel(
                                text,
                                0,
                                "Reading list",
                                "References",
                                "Bibliography",
                                "Tài liệu phục vụ học tập");

                if (readingHeading == null) {
                        data.setReadings(
                                        new ArrayList<>());
                        return;
                }

                int readingStart = readingHeading.end();

                LabelMatch readingEndHeading = firstFlexibleLabel(
                                text,
                                readingStart,
                                "Learning Outcomes Matrix",
                                "CLO-PLO Mapping Matrix",
                                "Planned learning activities",
                                "Teaching and learning activities",
                                "Assessment plan",
                                "Date revised",
                                "Chuẩn đầu ra môn học");
                int readingEnd = readingEndHeading == null
                                ? text.length()
                                : readingEndHeading.start();

                String section = text.substring(
                                readingStart,
                                readingEnd);
                String flatSection = collapse(section);

                /*
                 * Legacy Vietnamese programme specifications can use:
                 *
                 * - Tài liệu bắt buộc: ...
                 * - Tài liệu đọc thêm:
                 * + Reference 1
                 * + Reference 2
                 *
                 * Treat '+' as a bibliography marker only for that explicit structure so
                 * ordinary programme documents are not affected.
                 */
                boolean nestedVietnameseReadings = Pattern.compile(
                                "(?im)^\\s*-\\s*Tài liệu đọc thêm\\s*:\\s*$")
                                .matcher(section)
                                .find();

                String bulletMarker = nestedVietnameseReadings
                                ? "[-•+]"
                                : "[-•]";

                Matcher bulletMatcher = Pattern.compile(
                                "(?ms)^\\s*"
                                                + bulletMarker
                                                + "\\s*(.+?)"
                                                + "(?=^\\s*"
                                                + bulletMarker
                                                + "\\s*|\\z)")
                                .matcher(section);

                while (bulletMatcher.find()) {

                        String rawCitation = collapse(
                                        bulletMatcher.group(1));

                        if (nestedVietnameseReadings) {

                                /*
                                 * This is only a category heading. The actual references
                                 * follow as '+' bullets.
                                 */
                                if (rawCitation.matches(
                                                "(?i)^Tài liệu đọc thêm\\s*:?$")) {
                                        continue;
                                }

                                /*
                                 * Preserve the mandatory reference itself, but do not store
                                 * the category label as part of its citation.
                                 */
                                rawCitation = rawCitation.replaceFirst(
                                                "(?i)^Tài liệu bắt buộc\\s*:\\s*",
                                                "");
                        }

                        addReadingCitation(
                                        readings,
                                        rawCitation);
                }
                /*
                 * Bibliography markers are normally 1., 1), or [1]. Restrict the numeric
                 * marker to 1-3 digits so years such as "(amended in 2025)" can never be
                 * mistaken for the next bibliography item.
                 */
                String readingMarker;

                /*
                 * PATCH_3_2G_BRACKETED_READING_MARKERS
                 *
                 * Prefer one explicit bibliography marker family when the source uses
                 * bracketed markers. This prevents an internal title token such as
                 * "Lecture ready 3." from being reinterpreted as bibliography item 3.
                 */
                boolean bracketedReadingMarkers = Pattern.compile(
                                "(?:^|\\s)\\[\\d{1,3}\\]\\s+")
                                .matcher(flatSection)
                                .find();

                readingMarker = bracketedReadingMarkers
                                ? "\\[\\d{1,3}\\]"
                                : "(?:\\[\\d{1,3}\\]|\\d{1,3}[.)])";

                if (readings.isEmpty()) {

                        Matcher numbered = Pattern.compile(
                                        "(?:^|\\s)" + readingMarker
                                                        + "\\s*(.+?)(?=(?:\\s+"
                                                        + readingMarker
                                                        + "\\s+)|$)",
                                        Pattern.CASE_INSENSITIVE)
                                        .matcher(flatSection);

                        while (numbered.find()) {

                                addReadingCitation(
                                                readings,
                                                trimReadingCitationTail(
                                                                numbered.group(1)));
                        }
                }

                // Some templates use bullets or a single unnumbered reference.
                if (readings.isEmpty() && !flatSection.isBlank()) {
                        for (String citation : section.split("(?m)\\R\\s*(?:[-•]|\\[\\d+]|\\d+[.)])?\\s*")) {
                                addReadingCitation(readings, citation);
                        }
                }
                data.setReadings(readings);
        }

        private String trimReadingCitationTail(
                        String rawCitation) {

                String citation = collapse(rawCitation);

                if (citation.isBlank()) {
                        return citation;
                }

                /*
                 * Some dossiers append access notes and resource guidance immediately after
                 * the final numbered legal/book reference. Those are section metadata, not
                 * publisher names. Stop the final citation before these common sub-headings.
                 */
                String[] trailingHeadings = {
                                "Available at ",
                                "Additional materials",
                                "Optional Course Texts and Materials",
                                "Recommended Internet sites",
                                "Other Resources, Support and Information"
                };

                int cut = citation.length();

                for (String heading : trailingHeadings) {
                        int index = indexOfIgnoreCase(
                                        citation,
                                        heading);

                        if (index > 0
                                        && index < cut) {
                                cut = index;
                        }
                }

                return collapse(
                                citation.substring(
                                                0,
                                                cut));
        }

        private void addReadingCitation(
                        List<SyllabusImportData.ReadingItem> readings,
                        String rawCitation) {

                String citation = collapse(rawCitation)
                                .replaceFirst(
                                                "^[,;:.\\-\\s]+",
                                                "")
                                .replaceFirst(
                                                "[,;:.\\-\\s]+$",
                                                "");

                if (citation.isBlank()) {
                        return;
                }

                /*
                 * PATCH_3_2G_APA_READING_PARSE
                 *
                 * EN008IU uses APA-like citations:
                 * Author(s). (2013). Title. Publisher.
                 *
                 * The generic comma parser removed the year first and then treated the
                 * final author fragment plus title as a publisher. Parse only the
                 * explicit "(YYYY)." shape here; all other citation formats continue
                 * through the existing legacy parser below.
                 */
                Matcher apaYearMatcher = Pattern.compile(
                                "\\(((?:19|20)\\d{2})\\)\\.")
                                .matcher(citation);

                if (apaYearMatcher.find()) {

                        Integer apaYear = Integer.parseInt(
                                        apaYearMatcher.group(1));

                        String apaAuthor = collapseToNull(
                                        citation.substring(
                                                        0,
                                                        apaYearMatcher.start())
                                                        .replaceFirst(
                                                                        "[,;:\\-\\s]+$",
                                                                        ""));

                        String remainder = collapse(
                                        citation.substring(
                                                        apaYearMatcher.end()))
                                        .replaceFirst(
                                                        "^[,;:.\\-\\s]+",
                                                        "")
                                        .replaceFirst(
                                                        "[,;:.\\-\\s]+$",
                                                        "");

                        String apaTitle = remainder;

                        String apaPublisher = null;

                        int publisherBoundary = remainder.lastIndexOf(
                                        ". ");

                        if (publisherBoundary > 0
                                        && publisherBoundary + 2 < remainder.length()) {

                                String candidateTitle = collapseToNull(
                                                remainder.substring(
                                                                0,
                                                                publisherBoundary));

                                String candidatePublisher = collapseToNull(
                                                remainder.substring(
                                                                publisherBoundary + 2));

                                if (candidateTitle != null
                                                && candidatePublisher != null) {
                                        apaTitle = candidateTitle;
                                        apaPublisher = candidatePublisher;
                                }
                        }

                        if (apaTitle == null
                                        || apaTitle.isBlank()) {
                                apaTitle = citation;
                        }

                        readings.add(
                                        SyllabusImportData.ReadingItem.builder()
                                                        .author(apaAuthor)
                                                        .title(apaTitle)
                                                        .publisher(apaPublisher)
                                                        .year(apaYear)
                                                        .type("REFERENCE")
                                                        .build());
                        return;
                }

                Integer year = null;
                Matcher yearMatcher = Pattern.compile(
                                "(?<!\\d)((?:19|20)\\d{2})(?!\\d)")
                                .matcher(citation);
                int yearStart = -1;
                int yearEnd = -1;

                while (yearMatcher.find()) {
                        year = Integer.parseInt(
                                        yearMatcher.group(1));
                        yearStart = yearMatcher.start();
                        yearEnd = yearMatcher.end();
                }

                String withoutYear = yearStart < 0
                                ? citation
                                : collapse(
                                                citation.substring(
                                                                0,
                                                                yearStart)
                                                                + " "
                                                                + citation.substring(
                                                                                yearEnd))
                                                .replaceFirst(
                                                                "[,;:.\\-\\s]+$",
                                                                "");

                String author = null;
                String title = withoutYear;
                String publisher = null;
                String[] parts = withoutYear.split(
                                "\\s*,\\s*");

                if (parts.length >= 2) {
                        author = collapseToNull(
                                        parts[0]);

                        if (parts.length == 2) {
                                title = collapse(
                                                parts[1]);
                        } else {
                                publisher = collapseToNull(
                                                parts[parts.length - 1]);
                                title = collapse(
                                                String.join(
                                                                ", ",
                                                                java.util.Arrays.copyOfRange(
                                                                                parts,
                                                                                1,
                                                                                parts.length - 1)));
                        }
                }

                if (title.isBlank()) {
                        title = citation;
                }

                /*
                 * A physical PDF page number can leak into the final bibliography entry
                 * after the publication year. Keep the verified legacy guard.
                 */
                if (publisher != null
                                && year != null
                                && publisher.matches(
                                                "\\d{1,4}")) {
                        publisher = null;
                }

                /*
                 * Keep the verified defensive fallback for unseen templates.
                 */
                if (publisher != null
                                && publisher.length() > 300) {
                        author = null;
                        title = citation;
                        publisher = null;
                }

                readings.add(
                                SyllabusImportData.ReadingItem.builder()
                                                .author(author)
                                                .title(title)
                                                .publisher(publisher)
                                                .year(year)
                                                .type("REFERENCE")
                                                .build());
        }

        private void parseRevisionDate(
                        String text,
                        SyllabusImportData data) {

                LabelMatch revisionHeading = firstFlexibleLabel(
                                text,
                                0,
                                "Date revised",
                                "Last revised",
                                "Revision date");

                if (revisionHeading == null) {
                        return;
                }

                String remainder = text.substring(
                                revisionHeading.end());

                Matcher valueMatcher = Pattern.compile(
                                "^\\s*:?\\s*([^\\r\\n]+)").matcher(remainder);

                if (!valueMatcher.find()) {
                        return;
                }

                String value = collapse(
                                valueMatcher.group(1));

                if (value.isBlank()) {
                        return;
                }

                List<DateTimeFormatter> formatters = List.of(
                                DateTimeFormatter.ISO_LOCAL_DATE,
                                DateTimeFormatter.ofPattern(
                                                "MMMM d, uuuu",
                                                Locale.ENGLISH),
                                DateTimeFormatter.ofPattern(
                                                "MMM d, uuuu",
                                                Locale.ENGLISH),
                                DateTimeFormatter.ofPattern(
                                                "d MMMM uuuu",
                                                Locale.ENGLISH),
                                DateTimeFormatter.ofPattern(
                                                "d MMM uuuu",
                                                Locale.ENGLISH));

                for (DateTimeFormatter formatter : formatters) {
                        try {
                                LocalDate parsed = LocalDate.parse(
                                                value,
                                                formatter);

                                data.setDateRevised(
                                                parsed.toString());

                                return;

                        } catch (DateTimeParseException ignored) {
                                // Try the next supported source format.
                        }
                }

                /*
                 * Preserve the source value when the date format is
                 * unfamiliar instead of silently discarding it.
                 */
                data.setDateRevised(
                                value);
        }

        private void parseRubrics(
                        String text,
                        SyllabusImportData data) {

                List<SyllabusImportData.RubricItem> rubrics = new ArrayList<>();

                /*
                 * Preserve the existing well-known IU rubric mappings.
                 */
                if (containsIgnoreCase(
                                text,
                                "5.1. Grading checklist")) {

                        rubrics.add(
                                        checklistRubric());
                }

                if (containsIgnoreCase(
                                text,
                                "5.2. Holistic rubric")) {

                        rubrics.add(
                                        holisticRubric());
                }

                if (containsIgnoreCase(
                                text,
                                "Critical thinking value rubric")) {

                        rubrics.add(
                                        criticalThinkingRubric());
                }

                if (containsIgnoreCase(
                                text,
                                "Oral communication value rubric")) {

                        rubrics.add(
                                        oralCommunicationRubric());
                }

                /*
                 * Flexible-template fallback.
                 *
                 * If none of the known rubric formats was detected,
                 * preserve the source rubric title instead of guessing
                 * that it belongs to one of the predefined rubric types.
                 */
                if (rubrics.isEmpty()) {

                        LabelMatch rubricsHeading = firstFlexibleLabel(
                                        text,
                                        0,
                                        "Rubrics (optional)",
                                        "Rubrics");

                        if (rubricsHeading != null) {

                                int start = rubricsHeading.end();

                                LabelMatch endHeading = firstFlexibleLabel(
                                                text,
                                                start,
                                                "Date revised",
                                                "Last revised",
                                                "Revision date",
                                                "Reading list",
                                                "References",
                                                "Bibliography");

                                int end = endHeading == null
                                                ? text.length()
                                                : endHeading.start();

                                String rubricSection = text.substring(
                                                start,
                                                end);

                                String sourceTitle = rubricSection
                                                .lines()
                                                .map(this::collapse)
                                                .map(line -> line.replaceFirst(
                                                                "^\\s*:\\s*",
                                                                ""))
                                                .filter(line -> !line.isBlank())
                                                .findFirst()
                                                .orElse(null);

                                sourceTitle = sanitizeImportedPlainText(
                                                sourceTitle);

                                if (sourceTitle != null
                                                && !sourceTitle.isBlank()) {

                                        rubrics.add(
                                                        SyllabusImportData.RubricItem
                                                                        .builder()
                                                                        .title(sourceTitle)
                                                                        .build());
                                }
                        }
                }

                data.setRubricItems(
                                rubrics);
        }

        private SyllabusImportData.RubricItem checklistRubric() {
                List<SyllabusImportData.RubricCriteriaItem> criteria = new ArrayList<>();
                criteria.add(criterion("Technical content (60%)", "60", "", "", ""));
                criteria.add(criterion("Abstract clearly identifies purpose and summarizes principal content", "10", "",
                                "", ""));
                criteria.add(criterion(
                                "Introduction demonstrates thorough knowledge of relevant background and prior work",
                                "15", "", "", ""));
                criteria.add(criterion("Analysis and discussion demonstrate good subject mastery", "30", "", "", ""));
                criteria.add(criterion("Summary and conclusions appropriate and complete", "5", "", "", ""));
                criteria.add(criterion("Organization (10%)", "10", "", "", ""));
                criteria.add(criterion("Distinct introduction, body, conclusions", "5", "", "", ""));
                criteria.add(criterion("Content clearly and logically organized, good transitions", "5", "", "", ""));
                criteria.add(criterion("Presentation (20%)", "20", "", "", ""));
                criteria.add(criterion("Correct spelling, grammar, and syntax", "10", "", "", ""));
                criteria.add(criterion("Clear and easy to read", "10", "", "", ""));
                criteria.add(criterion("Quality of Layout and Graphics (10%)", "10", "", "", ""));
                criteria.add(criterion("TOTAL SCORE", "100", "", "", ""));
                return rubric("GRADING_CHECKLIST", "Grading checklist for Written Reports", criteria);
        }

        private SyllabusImportData.RubricItem holisticRubric() {
                List<SyllabusImportData.RubricCriteriaItem> criteria = List.of(
                                criterion("5", "Demonstrates complete understanding of the problem. All requirements of task are included in response.",
                                                "", "", ""),
                                criterion("4", "Demonstrates considerable understanding of the problem. All requirements of task are included.",
                                                "", "", ""),
                                criterion("3", "Demonstrates partial understanding of the problem. Most requirements of task are included.",
                                                "", "", ""),
                                criterion("2", "Demonstrates little understanding of the problem. Many requirements of task are missing.",
                                                "", "", ""),
                                criterion("1", "Demonstrates no understanding of the problem.", "", "", ""),
                                criterion("0", "No response/task not attempted.", "", "", ""));
                return rubric("HOLISTIC",
                                "Holistic rubric for evaluating the entire document, e.g., exercises/quizzes/HW",
                                criteria);
        }

        private SyllabusImportData.RubricItem criticalThinkingRubric() {
                List<SyllabusImportData.RubricCriteriaItem> criteria = List.of(
                                criterion("Explanation of issues",
                                                "Issue/problem to be considered critically is stated clearly and described comprehensively, delivering all relevant information necessary for full understanding.",
                                                "Issue/problem to be considered critically is stated, described, and clarified so that understanding is not seriously impeded by omissions.",
                                                "Issue/problem to be considered critically is stated but description leaves some terms undefined, ambiguities unexplored, boundaries undetermined, and/or backgrounds unknown.",
                                                "Issue/problem to be considered critically is stated without clarification or description."),
                                criterion("Evidence — Selecting and using information to investigate a point of view or conclusion",
                                                "Information is taken from source(s) with enough interpretation/evaluation to develop a comprehensive analysis or synthesis. Viewpoints of experts are questioned thoroughly.",
                                                "Information is taken from source(s) with enough interpretation/evaluation to develop a coherent analysis or synthesis. Viewpoints of experts are subject to questioning.",
                                                "Information is taken from source(s) with some interpretation/evaluation, but not enough to develop a coherent analysis or synthesis. Viewpoints of experts are taken as mostly fact, with little questioning.",
                                                "Information is taken from source(s) without any interpretation/evaluation. Viewpoints of experts are taken as fact, without question."),
                                criterion("Influence of context and assumptions",
                                                "Thoroughly (systematically and methodically) analyzes own and others' assumptions and carefully evaluates the relevance of contexts when presenting a position.",
                                                "Identifies own and others' assumptions and several relevant contexts when presenting a position.",
                                                "Questions some assumptions. Identifies several relevant contexts when presenting a position. May be more aware of others' assumptions than one's own (or vice versa).",
                                                "Shows an emerging awareness of present assumptions (sometimes labels assertions as assumptions). Begins to identify some contexts when presenting a position."),
                                criterion("Student's position (perspective, thesis/hypothesis)",
                                                "Specific position is imaginative, taking into account the complexities of an issue. Limits of position are acknowledged. Others' points of view are synthesized within position.",
                                                "Specific position takes into account the complexities of an issue. Others' points of view are acknowledged within position.",
                                                "Specific position acknowledges different sides of an issue.",
                                                "Specific position is stated, but is simplistic and obvious."),
                                criterion("Conclusions and related outcomes (implications and consequences)",
                                                "Conclusions and related outcomes are logical and reflect student's informed evaluation and ability to place evidence and perspectives discussed in priority order.",
                                                "Conclusion is logically tied to a range of information, including opposing viewpoints; related outcomes are identified clearly.",
                                                "Conclusion is logically tied to information; some related outcomes are identified clearly.",
                                                "Conclusion is inconsistently tied to some of the information discussed; related outcomes are oversimplified."));
                return rubric("ANALYTIC", "Critical thinking value rubric for evaluating questions in exams", criteria);
        }

        private SyllabusImportData.RubricItem oralCommunicationRubric() {
                List<SyllabusImportData.RubricCriteriaItem> criteria = List.of(
                                criterion("Organization",
                                                "Organizational pattern (specific introduction and conclusion, sequenced material within the body, and transitions) is clearly and consistently observable and is skillful and makes the content of the presentation cohesive.",
                                                "Organizational pattern is clearly and consistently observable within the presentation.",
                                                "Organizational pattern is intermittently observable within the presentation.",
                                                "Organizational pattern is not observable within the presentation."),
                                criterion("Language",
                                                "Language choices are imaginative, memorable, and compelling, and enhance the effectiveness of the presentation. Language in presentation is appropriate to audience.",
                                                "Language choices are thoughtful and generally support the effectiveness of the presentation. Language in presentation is appropriate to audience.",
                                                "Language choices are mundane and commonplace and partially support the effectiveness of the presentation. Language in presentation is appropriate to audience.",
                                                "Language choices are unclear and minimally support the effectiveness of the presentation. Language in presentation is not appropriate to audience."),
                                criterion("Delivery",
                                                "Delivery techniques (posture, gesture, eye contact, and vocal expressiveness) make the presentation compelling, and speaker appears polished and confident.",
                                                "Delivery techniques make the presentation interesting, and speaker appears comfortable.",
                                                "Delivery techniques make the presentation understandable, and speaker appears tentative.",
                                                "Delivery techniques detract from the understandability of the presentation, and speaker appears uncomfortable."),
                                criterion("Supporting Material",
                                                "A variety of types of supporting materials make appropriate reference to information or analysis that significantly supports the presentation or establishes the presenter's credibility/authority on the topic.",
                                                "Supporting materials make appropriate reference to information or analysis that generally supports the presentation or establishes the presenter's credibility/authority on the topic.",
                                                "Supporting materials make appropriate reference to information or analysis that partially supports the presentation or establishes the presenter's credibility/authority on the topic.",
                                                "Insufficient supporting materials make reference to information or analysis that minimally supports the presentation or establishes the presenter's credibility/authority on the topic."),
                                criterion("Central Message",
                                                "Central message is compelling (precisely stated, appropriately repeated, memorable, and strongly supported).",
                                                "Central message is clear and consistent with the supporting material.",
                                                "Central message is basically understandable but is not often repeated and is not memorable.",
                                                "Central message can be deduced but is not explicitly stated in the presentation."));
                return rubric("ORAL_COMMUNICATION", "Oral communication value rubric for evaluating presentation tasks",
                                criteria);
        }

        private SyllabusImportData.RubricItem rubric(
                        String type, String title, List<SyllabusImportData.RubricCriteriaItem> criteria) {
                return SyllabusImportData.RubricItem.builder()
                                .type(type).title(title).criteria(new ArrayList<>(criteria)).build();
        }

        private SyllabusImportData.RubricCriteriaItem criterion(
                        String name, String level1, String level2, String level3, String level4) {
                return SyllabusImportData.RubricCriteriaItem.builder()
                                .criterion(name).level1(level1).level2(level2).level3(level3).level4(level4).build();
        }

        private void validateCoreSections(SyllabusImportData data, List<SyllabusImportIssue> issues) {
                warnIfBlank(data.getSourceCourseCode(), "General information", "sourceCourseCode",
                                "Course code was not found.", issues);
                warnIfBlank(data.getSourceCourseName(), "General information", "sourceCourseName",
                                "Course name was not found.", issues);
                if (data.getClos().isEmpty())
                        issues.add(issue("WARNING", "CLO", null, "clos", "No course learning outcomes were found."));
                if (data.getTopics().isEmpty())
                        issues.add(issue("WARNING", "Content", null, "topics", "No content rows were found."));
                if (data.getAssessments().isEmpty())
                        issues.add(issue("WARNING", "Assessment plan", null, "assessments",
                                        "No assessment rows were found."));
        }

        private void warnIfBlank(
                        String value, String section, String field, String message, List<SyllabusImportIssue> issues) {
                if (value == null || value.isBlank())
                        issues.add(issue("WARNING", section, null, field, message));
        }

        private SyllabusImportIssue issue(
                        String severity, String section, Integer row, String field, String message) {
                return SyllabusImportIssue.builder()
                                .severity(severity).section(section).row(row).field(field).message(message).build();
        }

        private String capture(String source, String expression) {
                Matcher matcher = Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
                                .matcher(source);
                if (!matcher.find())
                        return null;
                String value = collapse(matcher.group(1));
                return value.isBlank() ? null : value;
        }

        private String fieldSection(String source, String startLabel, String endLabel, String wrappedLabelTail) {
                String value = collapse(section(source, startLabel, endLabel));
                if (wrappedLabelTail != null && !wrappedLabelTail.isBlank()) {
                        String tail = Pattern.quote(wrappedLabelTail);
                        value = value.replaceFirst("(?i)^" + tail + "\\s*", "")
                                        .replaceFirst("(?i)\\s*" + tail + "$", "")
                                        .trim();
                }
                return value.isBlank() ? null : value;
        }

        private String valueAfterLastColon(String value) {
                if (value == null)
                        return null;
                int separator = value.lastIndexOf(':');
                String result = separator >= 0 ? value.substring(separator + 1).trim() : value.trim();
                return result.isBlank() ? null : result;
        }

        private String sectionUntilAny(String source, String start, String... ends) {
                LabelMatch startMatch = findFlexibleLabel(source, start, 0);
                if (startMatch == null)
                        return "";
                int startIndex = startMatch.end();
                int endIndex = source.length();
                for (String end : ends) {
                        LabelMatch candidate = findFlexibleLabel(source, end, startIndex);
                        if (candidate != null && candidate.start() < endIndex)
                                endIndex = candidate.start();
                }
                return source.substring(startIndex, endIndex);
        }

        private String collapseToNull(String value) {
                String result = collapse(value);
                return result.isBlank() ? null : result;
        }

        private String section(String source, String start, String end) {
                LabelMatch startMatch = findFlexibleLabel(source, start, 0);
                if (startMatch == null)
                        return "";
                int startIndex = startMatch.end();
                LabelMatch endMatch = findFlexibleLabel(source, end, startIndex);
                return source.substring(startIndex, endMatch == null ? source.length() : endMatch.start());
        }

        private LabelMatch findFlexibleLabel(String source, String label, int fromIndex) {
                String expression = java.util.Arrays.stream(label.trim().split("\\s+"))
                                .map(Pattern::quote)
                                .collect(java.util.stream.Collectors.joining("\\s+"));
                Matcher matcher = Pattern.compile(expression, Pattern.CASE_INSENSITIVE)
                                .matcher(source);
                return matcher.find(fromIndex) ? new LabelMatch(matcher.start(), matcher.end()) : null;
        }

        private record LabelMatch(int start, int end) {
        }

        private int indexOfIgnoreCase(String source, String target) {
                return indexOfIgnoreCase(source, target, 0);
        }

        private int indexOfIgnoreCase(String source, String target, int fromIndex) {
                return source.toLowerCase(Locale.ROOT).indexOf(target.toLowerCase(Locale.ROOT), fromIndex);
        }

        private boolean containsIgnoreCase(String source, String expected) {
                return source != null && indexOfIgnoreCase(source, expected) >= 0;
        }

        private String collapse(String value) {
                return nullToEmpty(value).replaceAll("\\s+", " ").trim();
        }

        private String nullToEmpty(String value) {
                return value == null ? "" : value;
        }
private boolean needsOcrFallback(
        String sourceText,
        SyllabusImportData data) {

    if (data == null) {
        return true;
    }

    boolean hasObjectives =
            data.getObjectives() != null
                    && !data.getObjectives().isBlank();

    boolean hasClos =
            data.getClos() != null
                    && !data.getClos().isEmpty();

    boolean hasTopics =
            data.getTopics() != null
                    && !data.getTopics().isEmpty();

    boolean hasAssessments =
            data.getAssessments() != null
                    && !data.getAssessments().isEmpty();

    boolean hasReadings =
            data.getReadings() != null
                    && !data.getReadings().isEmpty();

    int structuredSections = 0;

    if (hasObjectives) structuredSections++;
    if (hasClos) structuredSections++;
    if (hasTopics) structuredSections++;
    if (hasAssessments) structuredSections++;
    if (hasReadings) structuredSections++;

    /*
     * OCR is a fallback only.
     *
     * Normal text PDFs continue through the existing parser.
     * Image/scanned syllabi with almost no structured extraction
     * are OCR'd automatically.
     */
    return structuredSections == 0
            || (
                sourceText != null
                && sourceText.trim().length() < 300
                && structuredSections <= 1
            );
}

private int structuredScore(
        SyllabusImportData data) {

    if (data == null) {
        return 0;
    }

    int score = 0;

    if (data.getSourceCourseCode() != null
            && !data.getSourceCourseCode().isBlank()) {
        score += 2;
    }

    if (data.getObjectives() != null
            && !data.getObjectives().isBlank()) {
        score += 2;
    }

    if (data.getWorkloadTotal() != null) {
        score++;
    }

    if (data.getWorkloadContact() != null) {
        score++;
    }

    if (data.getWorkloadPrivate() != null) {
        score++;
    }

    if (data.getClos() != null) {
        score += data.getClos().size() * 2;
    }

    if (data.getTopics() != null) {
        score += data.getTopics().size();
    }

    if (data.getAssessments() != null) {
        score += data.getAssessments().size();
    }

    if (data.getReadings() != null) {
        score += data.getReadings().size();
    }

    return score;
}
}