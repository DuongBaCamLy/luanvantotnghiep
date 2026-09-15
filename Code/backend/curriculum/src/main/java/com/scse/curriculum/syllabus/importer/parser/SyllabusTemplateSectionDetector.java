package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects the comparison shape of the actual uploaded syllabus template.
 *
 * <p>The parser DTO is canonical, but the comparison UI must not assume that
 * every uploaded syllabus uses one fixed template. This detector records
 * which source sections/fields were actually recognized and, where the
 * source format exposes it, preserves their source order.</p>
 *
 * <p>It intentionally does not store the original file bytes or presentation
 * styling. It stores only a compact structural profile:
 * section key, display label, field key, and field label.</p>
 */
final class SyllabusTemplateSectionDetector {

    private static final Pattern NUMBERED_HEADING_PREFIX =
            Pattern.compile("^\\s*\\d+(?:\\.\\d+)*[.)]?\\s*");

    private SyllabusTemplateSectionDetector() {
    }

    static List<SyllabusImportData.TemplateSection> fromDocx(
            List<IBodyElement> body,
            SyllabusImportData data) {

        List<SectionCandidate> candidates = new ArrayList<>();

        addDocxGeneralCandidate(body, data, candidates);
        addDocxHeadingCandidates(body, data, candidates);

        /*
         * Relations can be embedded as columns inside a source table instead
         * of having their own heading. Keep them adjacent to their parent
         * section so the comparison never hides mapping changes.
         */
        addDerivedRelationCandidate(
                candidates,
                "topicClo",
                "Topic–CLO Mapping",
                data.getTopicCloMappings(),
                "content",
                List.of(
                        field("topicName", "Topic"),
                        field("weekNumber", "Week"),
                        field("orderInWeek", "Order in Week"),
                        field("cloCode", "CLO"),
                        field("teachingLevel", "Teaching Level")));

        addDerivedRelationCandidate(
                candidates,
                "cloPlo",
                "Learning Outcomes Matrix (CLO × PLO)",
                data.getCloPloMappings(),
                "clo",
                List.of(
                        field("cloCode", "CLO"),
                        field("ploCode", "PLO"),
                        field("level", "Contribution Level"),
                        field("contributionWeight", "Contribution Weight")));

        addDerivedRelationCandidate(
                candidates,
                "assessmentClo",
                "Assessment–CLO Matrix",
                data.getAssessmentCloMappings(),
                "assessment",
                List.of(
                        field("assessmentName", "Assessment"),
                        field("orderIndex", "Assessment Order"),
                        field("cloCode", "CLO"),
                        field("contributionPercent", "Contribution (%)")));

        return orderedSections(candidates);
    }

    static List<SyllabusImportData.TemplateSection> fromPdfText(
            String text,
            SyllabusImportData data) {

        String source = text == null ? "" : text;
        List<SectionCandidate> candidates = new ArrayList<>();

        /*
         * PDF text extraction does not preserve a DOM-like section tree, so
         * section order is derived from semantic heading positions.
         */
        addPdfCandidate(
                candidates,
                source,
                "general",
                "General Information",
                List.of(
                        "general information",
                        "thông tin chung",
                        "thong tin chung"),
                generalFields(data));

        addPdfCandidate(
                candidates,
                source,
                "requirements",
                "Requirements & Course Objectives",
                List.of(
                        "course objectives",
                        "mục tiêu",
                        "muc tieu"),
                requirementFields(data));

        addPdfCandidate(
        candidates,
        source,
        "clo",
        "Course Learning Outcomes (CLO)",
        List.of(
                "course learning outcomes",
                "chuẩn đầu ra",
                "chuan dau ra"),
        pdfCloFields(
                source,
                data));

                addPdfCandidate(
                candidates,
                source,
                "content",
                "Content / Topics",
                List.of(
                        "content",
                        "course content",
                        "nội dung",
                        "noi dung"),
                pdfContentFields(
                        source,
                        data));

        addPdfCandidate(
                candidates,
                source,
                "cloPlo",
                "Learning Outcomes Matrix (CLO × PLO)",
                List.of(
                        "learning outcomes matrix",
                        "clo-plo",
                        "clo – plo",
                        "clo x plo",
                        "ma trận chuẩn đầu ra",
                        "ma tran chuan dau ra"),
                data.getCloPloMappings().isEmpty()
                        ? List.of()
                        : List.of(
                                field("cloCode", "CLO"),
                                field("ploCode", "PLO"),
                                field("level", "Contribution Level"),
                                field("contributionWeight", "Contribution Weight")));

        addPdfCandidate(
                candidates,
                source,
                "plannedActivities",
                "Planned Learning Activities",
                List.of(
                        "planned learning activities",
                        "kế hoạch giảng dạy",
                        "ke hoach giang day"),
                plannedActivityFields(data));

        addPdfCandidate(
        candidates,
        source,
        "assessment",
        "Assessment Plan",
        List.of(
                "assessment plan",
                "kế hoạch đánh giá",
                "ke hoach danh gia"),
        pdfAssessmentFields(
                source,
                data));

        addPdfCandidate(
                candidates,
                source,
                "examination",
                "Examination & Study Requirements",
                List.of(
                        "examination forms",
                        "study and examination requirements",
                        "hình thức thi",
                        "hinh thuc thi"),
                examinationFields(data));

        addPdfCandidate(
        candidates,
        source,
        "readings",
        "Reading List",
        List.of(
                "reading list",
                "tài liệu tham khảo",
                "tai lieu tham khao"),
        pdfReadingFields(
                source,
                data));

        addPdfCandidate(
                candidates,
                source,
                "revision",
                "Revision Information",
                List.of(
                        "date revised",
                        "ngày cập nhật",
                        "ngay cap nhat"),
                revisionFields(data));

        /*
         * Workload/credit rows are normally nested inside General
         * Information in the official PDF. Preserve them as an adjacent
         * comparison block only when they were actually extracted.
         */
        List<SyllabusImportData.TemplateField> workloadFields =
                workloadCreditFields(data);

        if (!workloadFields.isEmpty()) {
            int generalPosition =
                    positionOf(candidates, "general");

            candidates.add(
                    new SectionCandidate(
                            generalPosition < 0
                                    ? Integer.MAX_VALUE - 50
                                    : generalPosition + 1,
                            1,
                            section(
                                    "workloadCredit",
                                    "Workload & Credit Points",
                                    workloadFields)));
        }

        addDerivedRelationCandidate(
                candidates,
                "topicClo",
                "Topic–CLO Mapping",
                data.getTopicCloMappings(),
                "content",
                List.of(
                        field("topicName", "Topic"),
                        field("weekNumber", "Week"),
                        field("orderInWeek", "Order in Week"),
                        field("cloCode", "CLO"),
                        field("teachingLevel", "Teaching Level")));

        addDerivedRelationCandidate(
                candidates,
                "assessmentClo",
                "Assessment–CLO Matrix",
                data.getAssessmentCloMappings(),
                "assessment",
                List.of(
                        field("assessmentName", "Assessment"),
                        field("orderIndex", "Assessment Order"),
                        field("cloCode", "CLO"),
                        field("contributionPercent", "Contribution (%)")));

        /*
         * Some PDFs expose CLO-PLO only as a compact matrix without a stable
         * heading. Ensure it is still represented when parsed.
         */
        if (!data.getCloPloMappings().isEmpty()
                && candidates.stream()
                        .noneMatch(candidate ->
                                "cloPlo".equals(candidate.section().getKey()))) {

            addDerivedRelationCandidate(
                    candidates,
                    "cloPlo",
                    "Learning Outcomes Matrix (CLO × PLO)",
                    data.getCloPloMappings(),
                    "clo",
                    List.of(
                            field("cloCode", "CLO"),
                            field("ploCode", "PLO"),
                            field("level", "Contribution Level"),
                            field("contributionWeight", "Contribution Weight")));
        }

        return orderedSections(candidates);
    }

    static List<SyllabusImportData.TemplateSection> fromXlsx(
            Workbook workbook,
            SyllabusImportData data) {

        List<SectionCandidate> candidates = new ArrayList<>();
        DataFormatter formatter = new DataFormatter(Locale.ROOT);

        int sheetOrder = 0;

        for (Sheet sheet : workbook) {
            String sheetName =
        canonicalXlsxSheetName(
                sheet.getSheetName()
        );

            switch (sheetName) {
                case "general info" -> {
                    List<SyllabusImportData.TemplateField> general =
                            new ArrayList<>();
                    List<SyllabusImportData.TemplateField> workload =
                            new ArrayList<>();
                    List<SyllabusImportData.TemplateField> requirements =
                            new ArrayList<>();
                    List<SyllabusImportData.TemplateField> examination =
                            new ArrayList<>();

                    collectXlsxGeneralFields(
                            sheet,
                            formatter,
                            general,
                            workload,
                            requirements,
                            examination);

                    addCandidate(
                            candidates,
                            sheetOrder,
                            0,
                            "general",
                            "General Information",
                            general);

                    addCandidate(
                            candidates,
                            sheetOrder,
                            1,
                            "workloadCredit",
                            "Workload & Credit Points",
                            workload);

                    addCandidate(
                            candidates,
                            sheetOrder,
                            2,
                            "requirements",
                            "Requirements & Course Objectives",
                            requirements);

                    addCandidate(
                            candidates,
                            sheetOrder,
                            3,
                            "examination",
                            "Examination & Study Requirements",
                            examination);
                }

                case "clo" ->
                        addCandidate(
                                candidates,
                                sheetOrder,
                                0,
                                "clo",
                                "Course Learning Outcomes (CLO)",
                                xlsxHeaderFields(
                                        sheet,
                                        formatter,
                                        Map.ofEntries(
                                                Map.entry("code", field("code", "Code")),
                                                Map.entry("description", field("description", "Description")),
                                                Map.entry("description vn", field("descriptionVn", "Vietnamese Description")),
                                                Map.entry("competency level", field("competencyLevel", "Competency Level")),
                                                Map.entry("bloom level", field("bloomLevel", "Bloom Level")),
                                                Map.entry("order index", field("orderIndex", "Display Order")))));

                case "topics" -> {
                    addCandidate(
                            candidates,
                            sheetOrder,
                            0,
                            "content",
                            "Content / Topics",
                            xlsxHeaderFields(
                                    sheet,
                                    formatter,
                                    Map.ofEntries(
                                            Map.entry("name", field("name", "Topic")),
                                            Map.entry("name vn", field("nameVn", "Vietnamese Topic")),
                                            Map.entry("week number", field("weekNumber", "Week")),
                                            Map.entry("order in week", field("orderInWeek", "Order in Week")),
                                            Map.entry("teaching hours", field("teachingHours", "Teaching Hours")),
                                            Map.entry("lab hours", field("labHours", "Laboratory Hours")),
                                            Map.entry("self study hours", field("selfStudyHours", "Self-study Hours")),
                                            Map.entry("topic type", field("topicType", "Topic Type")),
                                            Map.entry("teaching method", field("teachingMethod", "Teaching Method")),
                                            Map.entry("learning activity", field("learningActivity", "Learning Activity")),
                                            Map.entry("notes", field("contentNote", "Content Note")))));

                    if (!data.getTopicCloMappings().isEmpty()) {
                        addCandidate(
                                candidates,
                                sheetOrder,
                                1,
                                "topicClo",
                                "Topic–CLO Mapping",
                                List.of(
                                        field("topicName", "Topic"),
                                        field("weekNumber", "Week"),
                                        field("orderInWeek", "Order in Week"),
                                        field("cloCode", "CLO"),
                                        field("teachingLevel", "Teaching Level")));
                    }
                }

                case "assessments" -> {
                    addCandidate(
                            candidates,
                            sheetOrder,
                            0,
                            "assessment",
                            "Assessment Plan",
                            xlsxHeaderFields(
                                    sheet,
                                    formatter,
                                    Map.ofEntries(
                                            Map.entry("name", field("name", "Assessment")),
                                            Map.entry("name vn", field("nameVn", "Vietnamese Name")),
                                            Map.entry("assessment type", field("assessmentType", "Assessment Type")),
                                            Map.entry("weight percent", field("weightPercent", "Weight (%)")),
                                            Map.entry("min score", field("minScore", "Minimum Score")),
                                            Map.entry("max score", field("maxScore", "Maximum Score")),
                                            Map.entry("order index", field("orderIndex", "Display Order")))));

                    if (!data.getAssessmentCloMappings().isEmpty()) {
                        addCandidate(
                                candidates,
                                sheetOrder,
                                1,
                                "assessmentClo",
                                "Assessment–CLO Matrix",
                                List.of(
                                        field("assessmentName", "Assessment"),
                                        field("orderIndex", "Assessment Order"),
                                        field("cloCode", "CLO"),
                                        field("contributionPercent", "Contribution (%)")));
                    }
                }

                case "reading list" ->
                        addCandidate(
                                candidates,
                                sheetOrder,
                                0,
                                "readings",
                                "Reading List",
                                xlsxHeaderFields(
                                        sheet,
                                        formatter,
                                        Map.ofEntries(
                                                Map.entry("title", field("title", "Title")),
                                                Map.entry("author", field("author", "Author")),
                                                Map.entry("publisher", field("publisher", "Publisher")),
                                                Map.entry("year", field("year", "Publication Year")),
                                                Map.entry("book type", field("usageType", "Usage Type")))));

                default -> {
                    /*
                     * Unsupported/unknown sheets are not silently converted
                     * into fake syllabus sections. The XLSX parser already
                     * reports missing/unknown contract fields separately.
                     */
                }
            }

            sheetOrder++;
        }

        /*
         * XLSX currently stores relation matrices through canonical relation
         * DTOs rather than dedicated sheets. Keep them adjacent to the
         * relevant source sheet only when the parser produced them.
         */
        addDerivedRelationCandidate(
                candidates,
                "cloPlo",
                "Learning Outcomes Matrix (CLO × PLO)",
                data.getCloPloMappings(),
                "clo",
                List.of(
                        field("cloCode", "CLO"),
                        field("ploCode", "PLO"),
                        field("level", "Contribution Level"),
                        field("contributionWeight", "Contribution Weight")));

        return orderedSections(candidates);
    }

    private static void addDocxGeneralCandidate(
            List<IBodyElement> body,
            SyllabusImportData data,
            List<SectionCandidate> candidates) {

        for (int index = 0; index < body.size(); index++) {
            IBodyElement element = body.get(index);

            if (!(element instanceof XWPFTable table)) {
                continue;
            }

            List<SyllabusImportData.TemplateField> general =
                    new ArrayList<>();

            List<SyllabusImportData.TemplateField> workload =
                    new ArrayList<>();

            List<SyllabusImportData.TemplateField> requirements =
                    new ArrayList<>();

            for (XWPFTableRow row : table.getRows()) {
                if (row.getTableCells().isEmpty()) {
                    continue;
                }

                String label =
                        normalize(
                                cellText(
                                        row.getCell(0)));

                if (matchesAny(label, "course name", "ten hoc phan")) {
                    general.add(field("courseName", "Course Name"));
                } else if (matchesAny(label, "course code", "ma hoc phan", "ma so mon hoc")) {
                    general.add(field("courseCode", "Course Code"));
                } else if (matchesAny(label, "course designation", "mo ta hoc phan")) {
                    general.add(field("courseDesignation", "Course Designation"));
                } else if (matchesAny(label, "course type", "loai hoc phan")) {
                    general.add(field("courseTypes", "Course Type"));
                } else if (matchesAny(label, "semester", "hoc ky giang day")) {
                    general.add(field("semester", "Semester"));
                } else if (matchesAny(label, "person responsible", "nguoi phu trach")) {
                    general.add(field("personResponsible", "Person Responsible for the Course"));
                } else if (matchesAny(label, "language", "ngon ngu giang day")) {
                    general.add(field("language", "Language of Instruction"));
                } else if (matchesAny(label, "relation to curriculum", "quan he voi chuong trinh")) {
                    general.add(field("relation", "Relation to Curriculum"));
                } else if (matchesAny(label, "teaching methods", "phuong phap giang day")) {
                    general.add(field("teachingMethods", "Teaching Methods"));
                } else if (matchesAny(label, "workload", "khoi luong hoc tap")) {
                    if (data.getWorkloadTotal() != null) {
                        workload.add(field("workloadTotal", "Total Workload"));
                    }
                    if (data.getWorkloadContact() != null) {
                        workload.add(field("workloadContact", "Contact Hours"));
                    }
                    if (data.getWorkloadPrivate() != null) {
                        workload.add(field("workloadPrivate", "Self-study / Private Study Hours"));
                    }
                } else if (matchesAny(label, "credit points", "so tin chi")) {
                    if (data.getCreditPoints() != null) {
                        workload.add(field("creditPoints", "Credit Points — Total"));
                    }
                    if (data.getLectureCredits() != null) {
                        workload.add(field("lectureCredits", "Credits — Lecture"));
                    }
                    if (data.getLaboratoryCredits() != null) {
                        workload.add(field("laboratoryCredits", "Credits — Laboratory"));
                    }
                } else if (matchesAny(label, "prerequisites", "dieu kien tien quyet")) {
                    requirements.add(field("prerequisites", "Required / Recommended Prerequisites"));
                }
            }

            if (!general.isEmpty()
                    || !workload.isEmpty()
                    || !requirements.isEmpty()) {

                addCandidate(
                        candidates,
                        index,
                        0,
                        "general",
                        "General Information",
                        general);

                addCandidate(
                        candidates,
                        index,
                        1,
                        "workloadCredit",
                        "Workload & Credit Points",
                        workload);

                addCandidate(
                        candidates,
                        index,
                        2,
                        "requirements",
                        "Requirements & Course Objectives",
                        requirements);

                return;
            }
        }

        /*
         * Compact DOCX templates can express course identity in paragraphs
         * instead of a General Information table.
         */
        List<SyllabusImportData.TemplateField> fallback =
                generalFields(data);

        if (!fallback.isEmpty()) {
            addCandidate(
                    candidates,
                    0,
                    0,
                    "general",
                    "General Information",
                    fallback);
        }
    }

        private static void addDocxHeadingCandidates(
            List<IBodyElement> body,
            SyllabusImportData data,
            List<SectionCandidate> candidates) {

        for (int index = 0; index < body.size(); index++) {
            IBodyElement element = body.get(index);

            if (!(element instanceof XWPFParagraph paragraph)) {
                continue;
            }

            String value =
                    normalizeHeading(
                            paragraph.getText());

            if (value.isBlank()) {
                continue;
            }

            if (matchesAny(
                    value,
                    "course objectives",
                    "muc tieu hoc phan")) {

                mergeCandidate(
                        candidates,
                        index,
                        0,
                        "requirements",
                        "Requirements & Course Objectives",
                        List.of(
                                field(
                                        "objectives",
                                        "Course Objectives")));

                continue;
            }

            if (matchesAny(
                    value,
                    "course learning outcomes",
                    "chuan dau ra hoc phan")) {

                addRecognizedCandidate(
                        candidates,
                        index,
                        0,
                        "clo",
                        "Course Learning Outcomes (CLO)",
                        cloFields(data));

                continue;
            }

            if (matchesAny(
                    value,
                    "learning outcomes matrix",
                    "ma tran chuan dau ra")) {

                if (!data.getCloPloMappings().isEmpty()) {
                    addCandidate(
                            candidates,
                            index,
                            0,
                            "cloPlo",
                            "Learning Outcomes Matrix (CLO × PLO)",
                            List.of(
                                    field(
                                            "cloCode",
                                            "CLO"),
                                    field(
                                            "ploCode",
                                            "PLO"),
                                    field(
                                            "level",
                                            "Contribution Level"),
                                    field(
                                            "contributionWeight",
                                            "Contribution Weight")));
                }

                continue;
            }

            if (matchesAny(
                    value,
                    "course content",
                    "noi dung hoc phan")) {

                addRecognizedCandidate(
                        candidates,
                        index,
                        0,
                        "content",
                        "Content / Topics",
                        contentFields(data));

                continue;
            }

            if (matchesAny(
                    value,
                    "planned learning activities",
                    "ke hoach giang day")) {

                addRecognizedCandidate(
                        candidates,
                        index,
                        0,
                        "plannedActivities",
                        "Planned Learning Activities",
                        plannedActivityFields(data));

                continue;
            }

            if (matchesAny(
                    value,
                    "assessment plan",
                    "ke hoach danh gia")) {

                addRecognizedCandidate(
                        candidates,
                        index,
                        0,
                        "assessment",
                        "Assessment Plan",
                        assessmentFields(data));

                continue;
            }

            if (matchesAny(
                    value,
                    "examination forms",
                    "study and examination requirements")) {

                addRecognizedCandidate(
                        candidates,
                        index,
                        0,
                        "examination",
                        "Examination & Study Requirements",
                        examinationFields(data));

                continue;
            }

            if (matchesAny(
                    value,
                    "reading list",
                    "tai lieu tham khao")) {

                addRecognizedCandidate(
                        candidates,
                        index,
                        0,
                        "readings",
                        "Reading List",
                        readingFields(data));

                continue;
            }

            if (matchesAny(
                    value,
                    "date revised",
                    "ngay cap nhat")) {

                addRecognizedCandidate(
                        candidates,
                        index,
                        0,
                        "revision",
                        "Revision Information",
                        revisionFields(data));
            }
        }
    }
    
    private static List<SyllabusImportData.TemplateField> generalFields(
            SyllabusImportData data) {

        List<SyllabusImportData.TemplateField> fields =
                new ArrayList<>();

        addIfNotNull(fields, data::getSourceCourseCode, "courseCode", "Course Code");
        addIfNotNull(fields, data::getSourceCourseName, "courseName", "Course Name");
        addIfNotNull(fields, data::getCourseDesignation, "courseDesignation", "Course Designation");
        addIfNotNull(fields, data::getCourseTypes, "courseTypes", "Course Type");
        addIfNotNull(fields, data::getSemester, "semester", "Semester");
        addIfNotNull(fields, data::getPersonResponsible, "personResponsible", "Person Responsible for the Course");
        addIfNotNull(fields, data::getLanguage, "language", "Language of Instruction");
        addIfNotNull(fields, data::getRelation, "relation", "Relation to Curriculum");
        addIfNotNull(fields, data::getTeachingMethods, "teachingMethods", "Teaching Methods");
        addIfNotNull(fields, data::getMajor, "major", "Major / Academic Area");

        return fields;
    }

    private static List<SyllabusImportData.TemplateField> workloadCreditFields(
            SyllabusImportData data) {

        List<SyllabusImportData.TemplateField> fields =
                new ArrayList<>();

        addIfNotNull(fields, data::getWorkloadTotal, "workloadTotal", "Total Workload");
        addIfNotNull(fields, data::getWorkloadContact, "workloadContact", "Contact Hours");
        addIfNotNull(fields, data::getWorkloadPrivate, "workloadPrivate", "Self-study / Private Study Hours");
        addIfNotNull(fields, data::getWorkloadStudentResponsibility,
                "workloadStudentResponsibility", "Student Responsibility");
        addIfNotNull(fields, data::getCreditPoints, "creditPoints", "Credit Points — Total");
        addIfNotNull(fields, data::getLectureCredits, "lectureCredits", "Credits — Lecture");
        addIfNotNull(fields, data::getLaboratoryCredits, "laboratoryCredits", "Credits — Laboratory");

        return fields;
    }

    private static List<SyllabusImportData.TemplateField> requirementFields(
            SyllabusImportData data) {

        List<SyllabusImportData.TemplateField> fields =
                new ArrayList<>();

        addIfNotNull(fields, data::getPrerequisites,
                "prerequisites", "Required / Recommended Prerequisites");
        addIfNotNull(fields, data::getObjectives,
                "objectives", "Course Objectives");

        return fields;
    }
        private static List<SyllabusImportData.TemplateField> pdfCloFields(
            String source,
            SyllabusImportData data) {

        List<SyllabusImportData.TemplateField> fields =
                new ArrayList<>(
                        cloFields(data));

        String normalized =
                pdfCloSectionText(source);

        if (normalized.contains("clo code")) {
            fields.add(
                    field(
                            "code",
                            "Code"));
        }

        if (normalized.contains("description")) {
            fields.add(
                    field(
                            "description",
                            "Description"));
        }

        if (normalized.contains("competency level")) {
            fields.add(
                    field(
                            "competencyLevel",
                            "Competency Level"));
        }

        if (normalized.contains("bloom level")) {
            fields.add(
                    field(
                            "bloomLevel",
                            "Bloom Level"));
        }

        if (normalized.contains("order index")
                || normalized.contains("display order")) {

            fields.add(
                    field(
                            "orderIndex",
                            "Display Order"));
        }

        return uniqueFields(fields);
    }
    
        private static String pdfCloSectionText(
            String source) {

        String normalizedSource =
                normalizeForSearch(source);

        List<String> cloHeadings =
                List.of(
                        "course learning outcomes",
                        "chuẩn đầu ra",
                        "chuan dau ra");

        int start = -1;
        int headingLength = 0;

        for (String heading : cloHeadings) {
            String normalizedHeading =
                    normalizeForSearch(heading);

            int candidate =
                    normalizedSource.indexOf(
                            normalizedHeading);

            if (candidate >= 0
                    && (start < 0 || candidate < start)) {

                start = candidate;
                headingLength =
                        normalizedHeading.length();
            }
        }

        if (start < 0) {
            return "";
        }

        int contentStart =
                Math.min(
                        normalizedSource.length(),
                        start + headingLength);

        int end =
                normalizedSource.length();

        List<String> followingSectionHeadings =
                List.of(
                        "general information",
                        "thông tin chung",
                        "thong tin chung",
                        "course objectives",
                        "mục tiêu",
                        "muc tieu",
                        "course content",
                        "nội dung",
                        "noi dung",
                        "learning outcomes matrix",
                        "clo-plo",
                        "clo – plo",
                        "clo x plo",
                        "planned learning activities",
                        "kế hoạch giảng dạy",
                        "ke hoach giang day",
                        "assessment plan",
                        "kế hoạch đánh giá",
                        "ke hoach danh gia",
                        "examination forms",
                        "study and examination requirements",
                        "hình thức thi",
                        "hinh thuc thi",
                        "reading list",
                        "tài liệu tham khảo",
                        "tai lieu tham khao",
                        "date revised",
                        "ngày cập nhật",
                        "ngay cap nhat");

        for (String heading : followingSectionHeadings) {
            String normalizedHeading =
                    normalizeForSearch(heading);

            int candidate =
                    normalizedSource.indexOf(
                            normalizedHeading,
                            contentStart);

            if (candidate >= 0
                    && candidate < end) {

                end = candidate;
            }
        }

        return normalizedSource.substring(
                contentStart,
                end);
    }

    private static List<SyllabusImportData.TemplateField> cloFields(
            SyllabusImportData data) {

        if (data.getClos().isEmpty()) {
            return List.of();
        }

        List<SyllabusImportData.TemplateField> fields =
                new ArrayList<>();

        fields.add(field("code", "Code"));

        if (data.getClos().stream().anyMatch(item -> item.getDescription() != null)) {
            fields.add(field("description", "Description"));
        }
        if (data.getClos().stream().anyMatch(item -> item.getDescriptionVn() != null)) {
            fields.add(field("descriptionVn", "Vietnamese Description"));
        }
        if (data.getClos().stream().anyMatch(item -> item.getCompetencyLevel() != null)) {
            fields.add(field("competencyLevel", "Competency Level"));
        }
        if (data.getClos().stream().anyMatch(item -> item.getBloomLevel() != null)) {
            fields.add(field("bloomLevel", "Bloom Level"));
        }
        if (data.getClos().stream().anyMatch(item -> item.getOrderIndex() != null)) {
            fields.add(field("orderIndex", "Display Order"));
        }

        return fields;
    }
    private static List<SyllabusImportData.TemplateField> pdfContentFields(
            String source,
            SyllabusImportData data) {

        List<SyllabusImportData.TemplateField> fields =
                new ArrayList<>(
                        contentFields(data));

        String normalized =
                pdfContentSectionText(source);

        String headerProbe =
        normalized.length() <= 300
                ? normalized
                : normalized.substring(0, 300);

String paddedHeader =
        " " + headerProbe + " ";

if (headerProbe.contains("topic name")
        || paddedHeader.contains(" topic ")) {

    fields.add(
            field(
                    "name",
                    "Topic"));
}

        if (normalized.contains("vietnamese topic")
                || normalized.contains("name vn")) {

            fields.add(
                    field(
                            "nameVn",
                            "Vietnamese Topic"));
        }

        if (headerProbe.contains("week number")
        || paddedHeader.contains(" week ")) {

    fields.add(
            field(
                    "weekNumber",
                    "Week"));
}

        if (normalized.contains("order in week")
                || normalized.contains("topic order")) {

            fields.add(
                    field(
                            "orderInWeek",
                            "Order in Week"));
        }

        if (normalized.contains("teaching hours")) {
            fields.add(
                    field(
                            "teachingHours",
                            "Teaching Hours"));
        }

        if (normalized.contains("lab hours")
                || normalized.contains("laboratory hours")) {

            fields.add(
                    field(
                            "labHours",
                            "Laboratory Hours"));
        }

        if (normalized.contains("self study hours")
                || normalized.contains("self-study hours")) {

            fields.add(
                    field(
                            "selfStudyHours",
                            "Self-study Hours"));
        }

        if (normalized.contains("topic type")) {
            fields.add(
                    field(
                            "topicType",
                            "Topic Type"));
        }

        if (normalized.contains("teaching method")) {
            fields.add(
                    field(
                            "teachingMethod",
                            "Teaching Method"));
        }

        if (normalized.contains("learning activity")) {
            fields.add(
                    field(
                            "learningActivity",
                            "Learning Activity"));
        }

        if (normalized.contains("resources")) {
            fields.add(
                    field(
                            "resources",
                            "Resources"));
        }

        if (normalized.contains("content weight")) {
            fields.add(
                    field(
                            "contentWeight",
                            "Content Weight"));
        }

        if (normalized.contains("content level")
                || normalized.contains("teaching level")) {

            fields.add(
                    field(
                            "contentLevel",
                            "Content Level (I/T/U)"));
        }

        if (normalized.contains("content note")
                || normalized.contains("notes")) {

            fields.add(
                    field(
                            "contentNote",
                            "Content Note"));
        }

        return uniqueFields(fields);
    }


    private static String pdfContentSectionText(
            String source) {

        String normalizedSource =
                normalizeForSearch(source);

        List<String> contentHeadings =
                List.of(
                        "course content",
                        "content",
                        "nội dung",
                        "noi dung");

        int start = -1;
        int headingLength = 0;

        for (String heading : contentHeadings) {
            String normalizedHeading =
                    normalizeForSearch(heading);

            int candidate =
                    normalizedSource.indexOf(
                            normalizedHeading);

            if (candidate >= 0
                    && (start < 0 || candidate < start)) {

                start = candidate;
                headingLength =
                        normalizedHeading.length();
            }
        }

        if (start < 0) {
            return "";
        }

        int contentStart =
                Math.min(
                        normalizedSource.length(),
                        start + headingLength);

        int end =
                normalizedSource.length();

        List<String> otherSectionHeadings =
                List.of(
                        "general information",
                        "thông tin chung",
                        "thong tin chung",
                        "course objectives",
                        "mục tiêu",
                        "muc tieu",
                        "course learning outcomes",
                        "chuẩn đầu ra",
                        "chuan dau ra",
                        "learning outcomes matrix",
                        "clo-plo",
                        "clo – plo",
                        "clo x plo",
                        "planned learning activities",
                        "kế hoạch giảng dạy",
                        "ke hoach giang day",
                        "assessment plan",
                        "kế hoạch đánh giá",
                        "ke hoach danh gia",
                        "examination forms",
                        "study and examination requirements",
                        "hình thức thi",
                        "hinh thuc thi",
                        "reading list",
                        "tài liệu tham khảo",
                        "tai lieu tham khao",
                        "date revised",
                        "ngày cập nhật",
                        "ngay cap nhat");

        for (String heading : otherSectionHeadings) {
            String normalizedHeading =
                    normalizeForSearch(heading);

            int candidate =
                    normalizedSource.indexOf(
                            normalizedHeading,
                            contentStart);

            if (candidate >= 0
                    && candidate < end) {

                end = candidate;
            }
        }

        return normalizedSource.substring(
                contentStart,
                end);
    }
    private static List<SyllabusImportData.TemplateField> contentFields(
            SyllabusImportData data) {

        List<SyllabusImportData.TemplateField> fields =
                new ArrayList<>();

        if (data.getContentNote() != null) {
            fields.add(field("contentNote", "Content Note"));
        }

        if (data.getTopics().isEmpty()) {
            return fields;
        }

        fields.add(field("name", "Topic"));

        if (data.getTopics().stream().anyMatch(item -> item.getNameVn() != null)) {
            fields.add(field("nameVn", "Vietnamese Topic"));
        }
        if (data.getTopics().stream().anyMatch(item -> item.getWeekNumber() != null)) {
            fields.add(field("weekNumber", "Week"));
        }
        if (data.getTopics().stream().anyMatch(item -> item.getOrderInWeek() != null)) {
            fields.add(field("orderInWeek", "Order in Week"));
        }
        if (data.getTopics().stream().anyMatch(item -> item.getTeachingHours() != null)) {
            fields.add(field("teachingHours", "Teaching Hours"));
        }
        if (data.getTopics().stream().anyMatch(item -> item.getLabHours() != null)) {
            fields.add(field("labHours", "Laboratory Hours"));
        }
        if (data.getTopics().stream().anyMatch(item -> item.getSelfStudyHours() != null)) {
            fields.add(field("selfStudyHours", "Self-study Hours"));
        }
        if (data.getTopics().stream().anyMatch(item -> item.getTopicType() != null)) {
            fields.add(field("topicType", "Topic Type"));
        }
        if (data.getTopics().stream().anyMatch(item -> item.getTeachingMethod() != null)) {
            fields.add(field("teachingMethod", "Teaching Method"));
        }
        if (data.getTopics().stream().anyMatch(item -> item.getLearningActivity() != null)) {
            fields.add(field("learningActivity", "Learning Activity"));
        }
        if (data.getTopics().stream().anyMatch(item -> item.getResources() != null)) {
            fields.add(field("resources", "Resources"));
        }
        if (data.getTopics().stream().anyMatch(item -> item.getContentWeight() != null)) {
            fields.add(field("contentWeight", "Content Weight"));
        }
        if (data.getTopics().stream().anyMatch(item ->
                item.getContentLevel() != null || item.getTeachingLevel() != null)) {
            fields.add(field("contentLevel", "Content Level (I/T/U)"));
        }

        return fields;
    }

    private static List<SyllabusImportData.TemplateField> plannedActivityFields(
            SyllabusImportData data) {

        if (data.getWeeklyActivities().isEmpty()) {
            return List.of();
        }

        List<SyllabusImportData.TemplateField> fields =
                new ArrayList<>();

        fields.add(field("week", "Week"));

        if (data.getWeeklyActivities().stream().anyMatch(item -> item.getTopic() != null)) {
            fields.add(field("topic", "Topic"));
        }
        if (data.getWeeklyActivities().stream().anyMatch(item -> item.getClo() != null)) {
            fields.add(field("clo", "CLOs"));
        }
        if (data.getWeeklyActivities().stream().anyMatch(item -> item.getAssessments() != null)) {
            fields.add(field("assessments", "Assessments"));
        }
        if (data.getWeeklyActivities().stream().anyMatch(item -> item.getLearningActivities() != null)) {
            fields.add(field("learningActivities", "Learning Activities"));
        }
        if (data.getWeeklyActivities().stream().anyMatch(item -> item.getResources() != null)) {
            fields.add(field("resources", "Resources"));
        }

        return fields;
    }
    private static List<SyllabusImportData.TemplateField> pdfAssessmentFields(
            String source,
            SyllabusImportData data) {

        List<SyllabusImportData.TemplateField> fields =
                new ArrayList<>(
                        assessmentFields(data));

        String normalized =
                pdfAssessmentSectionText(source);

        if (normalized.contains("assessment name")) {
            fields.add(
                    field(
                            "name",
                            "Assessment"));
        }

        if (normalized.contains("assessment type")) {
            fields.add(
                    field(
                            "assessmentType",
                            "Assessment Type"));
        }

        if (normalized.contains("weight (%)")
                || normalized.contains("weight %")
                || normalized.contains("weight percent")) {

            fields.add(
                    field(
                            "weightPercent",
                            "Weight (%)"));
        }

        if (normalized.contains("minimum score")
                || normalized.contains("min score")) {

            fields.add(
                    field(
                            "minScore",
                            "Minimum Score"));
        }

        if (normalized.contains("maximum score")
                || normalized.contains("max score")) {

            fields.add(
                    field(
                            "maxScore",
                            "Maximum Score"));
        }

        if (normalized.contains("order index")
                || normalized.contains("display order")) {

            fields.add(
                    field(
                            "orderIndex",
                            "Display Order"));
        }

        return uniqueFields(fields);
    }


    private static String pdfAssessmentSectionText(
            String source) {

        String normalizedSource =
                normalizeForSearch(source);

        List<String> assessmentHeadings =
                List.of(
                        "assessment plan",
                        "kế hoạch đánh giá",
                        "ke hoach danh gia");

        int start = -1;
        int headingLength = 0;

        for (String heading : assessmentHeadings) {
            String normalizedHeading =
                    normalizeForSearch(heading);

            int candidate =
                    normalizedSource.indexOf(
                            normalizedHeading);

            if (candidate >= 0
                    && (start < 0 || candidate < start)) {

                start = candidate;
                headingLength =
                        normalizedHeading.length();
            }
        }

        if (start < 0) {
            return "";
        }

        int contentStart =
                Math.min(
                        normalizedSource.length(),
                        start + headingLength);

        int end =
                normalizedSource.length();

        List<String> otherSectionHeadings =
                List.of(
                        "general information",
                        "thông tin chung",
                        "thong tin chung",
                        "course objectives",
                        "mục tiêu",
                        "muc tieu",
                        "course learning outcomes",
                        "chuẩn đầu ra",
                        "chuan dau ra",
                        "course content",
                        "nội dung",
                        "noi dung",
                        "learning outcomes matrix",
                        "clo-plo",
                        "clo – plo",
                        "clo x plo",
                        "planned learning activities",
                        "kế hoạch giảng dạy",
                        "ke hoach giang day",
                        "examination forms",
                        "study and examination requirements",
                        "hình thức thi",
                        "hinh thuc thi",
                        "reading list",
                        "tài liệu tham khảo",
                        "tai lieu tham khao",
                        "date revised",
                        "ngày cập nhật",
                        "ngay cap nhat");

        for (String heading : otherSectionHeadings) {
            String normalizedHeading =
                    normalizeForSearch(heading);

            int candidate =
                    normalizedSource.indexOf(
                            normalizedHeading,
                            contentStart);

            if (candidate >= 0
                    && candidate < end) {

                end = candidate;
            }
        }

        return normalizedSource.substring(
                contentStart,
                end);
    }
    private static List<SyllabusImportData.TemplateField> assessmentFields(
            SyllabusImportData data) {

        List<SyllabusImportData.TemplateField> fields =
                new ArrayList<>();

        if (data.getAssessmentPassNote() != null) {
            fields.add(field("assessmentPassNote", "Assessment Pass Note"));
        }

        if (data.getAssessments().isEmpty()) {
            return fields;
        }

        fields.add(field("name", "Assessment"));

        if (data.getAssessments().stream().anyMatch(item -> item.getNameVn() != null)) {
            fields.add(field("nameVn", "Vietnamese Name"));
        }
        if (data.getAssessments().stream().anyMatch(item -> item.getAssessmentType() != null)) {
            fields.add(field("assessmentType", "Assessment Type"));
        }
        if (data.getAssessments().stream().anyMatch(item -> item.getWeightPercent() != null)) {
            fields.add(field("weightPercent", "Weight (%)"));
        }
        if (data.getAssessments().stream().anyMatch(item -> item.getMinScore() != null)) {
            fields.add(field("minScore", "Minimum Score"));
        }
        if (data.getAssessments().stream().anyMatch(item -> item.getMaxScore() != null)) {
            fields.add(field("maxScore", "Maximum Score"));
        }
        if (data.getAssessments().stream().anyMatch(item -> item.getOrderIndex() != null)) {
            fields.add(field("orderIndex", "Display Order"));
        }

        return fields;
    }

    private static List<SyllabusImportData.TemplateField> examinationFields(
            SyllabusImportData data) {

        List<SyllabusImportData.TemplateField> fields =
                new ArrayList<>();

        addIfNotNull(fields, data::getExamForms,
                "examForms", "Examination Forms");
        addIfNotNull(fields, data::getExamRequirements,
                "examRequirements", "Study / Examination Requirements");

        return fields;
    }
    private static List<SyllabusImportData.TemplateField> pdfReadingFields(
            String source,
            SyllabusImportData data) {

        List<SyllabusImportData.TemplateField> fields =
                new ArrayList<>(
                        readingFields(data));

        String normalized =
                pdfReadingSectionText(source);

        String headerProbe =
                normalized.length() <= 300
                        ? normalized
                        : normalized.substring(0, 300);

        String paddedHeader =
                " " + headerProbe + " ";

        if (paddedHeader.contains(" title ")) {
            fields.add(
                    field(
                            "title",
                            "Title"));
        }

        if (paddedHeader.contains(" author ")) {
            fields.add(
                    field(
                            "author",
                            "Author"));
        }

        if (paddedHeader.contains(" publisher ")) {
            fields.add(
                    field(
                            "publisher",
                            "Publisher"));
        }

        if (paddedHeader.contains(" year ")
                || headerProbe.contains("publication year")) {

            fields.add(
                    field(
                            "year",
                            "Publication Year"));
        }

        if (headerProbe.contains("usage type")
                || headerProbe.contains("resource type")
                || headerProbe.contains("book type")) {

            fields.add(
                    field(
                            "usageType",
                            "Usage Type"));
        }

        return uniqueFields(fields);
    }


    private static String pdfReadingSectionText(
            String source) {

        String normalizedSource =
                normalizeForSearch(source);

        List<String> readingHeadings =
                List.of(
                        "reading list",
                        "tài liệu tham khảo",
                        "tai lieu tham khao");

        int start = -1;
        int headingLength = 0;

        for (String heading : readingHeadings) {
            String normalizedHeading =
                    normalizeForSearch(heading);

            int candidate =
                    normalizedSource.indexOf(
                            normalizedHeading);

            if (candidate >= 0
                    && (start < 0 || candidate < start)) {

                start = candidate;
                headingLength =
                        normalizedHeading.length();
            }
        }

        if (start < 0) {
            return "";
        }

        int contentStart =
                Math.min(
                        normalizedSource.length(),
                        start + headingLength);

        int end =
                normalizedSource.length();

        List<String> otherSectionHeadings =
                List.of(
                        "general information",
                        "thông tin chung",
                        "thong tin chung",
                        "course objectives",
                        "mục tiêu",
                        "muc tieu",
                        "course learning outcomes",
                        "chuẩn đầu ra",
                        "chuan dau ra",
                        "course content",
                        "nội dung",
                        "noi dung",
                        "learning outcomes matrix",
                        "clo-plo",
                        "clo – plo",
                        "clo x plo",
                        "planned learning activities",
                        "kế hoạch giảng dạy",
                        "ke hoach giang day",
                        "assessment plan",
                        "kế hoạch đánh giá",
                        "ke hoach danh gia",
                        "examination forms",
                        "study and examination requirements",
                        "hình thức thi",
                        "hinh thuc thi",
                        "date revised",
                        "ngày cập nhật",
                        "ngay cap nhat");

        for (String heading : otherSectionHeadings) {
            String normalizedHeading =
                    normalizeForSearch(heading);

            int candidate =
                    normalizedSource.indexOf(
                            normalizedHeading,
                            contentStart);

            if (candidate >= 0
                    && candidate < end) {

                end = candidate;
            }
        }

        return normalizedSource.substring(
                contentStart,
                end);
    }
    private static List<SyllabusImportData.TemplateField> readingFields(
            SyllabusImportData data) {

        if (data.getReadings().isEmpty()) {
            return List.of();
        }

        List<SyllabusImportData.TemplateField> fields =
                new ArrayList<>();

        fields.add(field("title", "Title"));

        if (data.getReadings().stream().anyMatch(item -> item.getAuthor() != null)) {
            fields.add(field("author", "Author"));
        }
        if (data.getReadings().stream().anyMatch(item -> item.getPublisher() != null)) {
            fields.add(field("publisher", "Publisher"));
        }
        if (data.getReadings().stream().anyMatch(item -> item.getYear() != null)) {
            fields.add(field("year", "Publication Year"));
        }
        if (data.getReadings().stream().anyMatch(item -> item.getType() != null)) {
            fields.add(field("usageType", "Usage Type"));
        }

        return fields;
    }

    private static List<SyllabusImportData.TemplateField> revisionFields(
            SyllabusImportData data) {

        if (data.getDateRevised() == null) {
            return List.of();
        }

        return List.of(
                field("dateRevised", "Date Revised"));
    }

    private static void collectXlsxGeneralFields(
        Sheet sheet,
        DataFormatter formatter,
        List<SyllabusImportData.TemplateField> general,
        List<SyllabusImportData.TemplateField> workload,
        List<SyllabusImportData.TemplateField> requirements,
        List<SyllabusImportData.TemplateField> examination) {

    int fieldColumn = -1;

    for (Row row : sheet) {

        for (Cell cell : row) {

            if ("field".equals(
                    normalize(
                            formatter.formatCellValue(cell)
                    )
            )) {
                fieldColumn =
                        cell.getColumnIndex();
                break;
            }
        }

        if (fieldColumn >= 0) {
            break;
        }
    }

    if (fieldColumn < 0) {
        return;
    }

    for (Row row : sheet) {

        Cell cell =
                row.getCell(fieldColumn);

        if (cell == null) {
            continue;
        }

        String sourceField =
                SyllabusXlsxParser
                        .canonicalGeneralField(
                                formatter.formatCellValue(cell)
                        );

        switch (sourceField) {

            case "course code" ->
                    general.add(
                            field(
                                    "courseCode",
                                    "Course Code"
                            )
                    );

            case "course name" ->
                    general.add(
                            field(
                                    "courseName",
                                    "Course Name"
                            )
                    );

            case "course designation" ->
                    general.add(
                            field(
                                    "courseDesignation",
                                    "Course Designation"
                            )
                    );

            case "course types" ->
                    general.add(
                            field(
                                    "courseTypes",
                                    "Course Type"
                            )
                    );

            case "semester" ->
                    general.add(
                            field(
                                    "semester",
                                    "Semester"
                            )
                    );

            case "person responsible" ->
                    general.add(
                            field(
                                    "personResponsible",
                                    "Person Responsible for the Course"
                            )
                    );

            case "language" ->
                    general.add(
                            field(
                                    "language",
                                    "Language of Instruction"
                            )
                    );

            case "relation" ->
                    general.add(
                            field(
                                    "relation",
                                    "Relation to Curriculum"
                            )
                    );

            case "teaching methods" ->
                    general.add(
                            field(
                                    "teachingMethods",
                                    "Teaching Methods"
                            )
                    );

            case "major" ->
                    general.add(
                            field(
                                    "major",
                                    "Major / Academic Area"
                            )
                    );

            case "workload total" ->
                    workload.add(
                            field(
                                    "workloadTotal",
                                    "Total Workload"
                            )
                    );

            case "workload contact" ->
                    workload.add(
                            field(
                                    "workloadContact",
                                    "Contact Hours"
                            )
                    );

            case "workload private" ->
                    workload.add(
                            field(
                                    "workloadPrivate",
                                    "Self-study / Private Study Hours"
                            )
                    );

            case "workload student responsibility" ->
                    workload.add(
                            field(
                                    "workloadStudentResponsibility",
                                    "Student Responsibility"
                            )
                    );

            case "credit points" ->
                    workload.add(
                            field(
                                    "creditPoints",
                                    "Credit Points — Total"
                            )
                    );

            case "lecture credits" ->
                    workload.add(
                            field(
                                    "lectureCredits",
                                    "Credits — Lecture"
                            )
                    );

            case "laboratory credits" ->
                    workload.add(
                            field(
                                    "laboratoryCredits",
                                    "Credits — Laboratory"
                            )
                    );

            case "prerequisites" ->
                    requirements.add(
                            field(
                                    "prerequisites",
                                    "Required / Recommended Prerequisites"
                            )
                    );

            case "objectives" ->
                    requirements.add(
                            field(
                                    "objectives",
                                    "Course Objectives"
                            )
                    );

            case "exam forms" ->
                    examination.add(
                            field(
                                    "examForms",
                                    "Examination Forms"
                            )
                    );

            case "exam requirements" ->
                    examination.add(
                            field(
                                    "examRequirements",
                                    "Study / Examination Requirements"
                            )
                    );

            default -> {
                // Unknown rows are intentionally ignored here.
            }
        }
    }
}

    private static String canonicalXlsxSheetName(
        String value) {

    String sheetName =
            normalize(value);

    return switch (sheetName) {

        case "general information" ->
                "general info";

        case "learning outcomes",
             "course learning outcomes" ->
                "clo";

        case "course content",
             "course topics" ->
                "topics";

        case "assessment plan",
             "assessment scheme",
             "assessment methods" ->
                "assessments";

        case "references",
             "bibliography" ->
                "reading list";

        default ->
                sheetName;
    };
}
    
    private static List<SyllabusImportData.TemplateField> xlsxHeaderFields(
            Sheet sheet,
            DataFormatter formatter,
            Map<String, SyllabusImportData.TemplateField> definitions) {

        for (Row row : sheet) {
            List<SyllabusImportData.TemplateField> fields =
                    new ArrayList<>();

            for (Cell cell : row) {
                String header =
        SyllabusXlsxParser.canonicalHeader(
                formatter.formatCellValue(cell)
        );

                SyllabusImportData.TemplateField definition =
                        definitions.get(header);

                if (definition != null) {
                    fields.add(definition);
                }
            }

            if (!fields.isEmpty()) {
                return uniqueFields(fields);
            }
        }

        return List.of();
    }

    private static void addPdfCandidate(
        List<SectionCandidate> candidates,
        String source,
        String key,
        String label,
        List<String> headings,
        List<SyllabusImportData.TemplateField> fields) {

    int position =
            firstHeadingPosition(
                    source,
                    headings);

    if (position < 0) {
        return;
    }

    /*
     * A recognized source heading is evidence that the section exists,
     * even when the parser could not extract any canonical field/value.
     *
     * Keep the empty section so provenance can later distinguish
     * UNRESOLVED from ABSENT_IN_SOURCE.
     */
    if (fields == null || fields.isEmpty()) {
        candidates.add(
                new SectionCandidate(
                        position,
                        0,
                        section(
                                key,
                                label,
                                List.of())));
        return;
    }

    addCandidate(
            candidates,
            position,
            0,
            key,
            label,
            fields);
}
    
    private static int firstHeadingPosition(
            String source,
            List<String> headings) {

        String normalizedSource =
                normalizeForSearch(source);

        int result = -1;

        for (String heading : headings) {
            int candidate =
                    normalizedSource.indexOf(
                            normalizeForSearch(heading));

            if (candidate >= 0
                    && (result < 0 || candidate < result)) {
                result = candidate;
            }
        }

        return result;
    }

    private static void addDerivedRelationCandidate(
            List<SectionCandidate> candidates,
            String key,
            String label,
            List<?> values,
            String parentKey,
            List<SyllabusImportData.TemplateField> fields) {

        if (values == null || values.isEmpty()) {
            return;
        }

        boolean alreadyPresent =
                candidates.stream()
                        .anyMatch(candidate ->
                                key.equals(
                                        candidate.section().getKey()));

        if (alreadyPresent) {
            return;
        }

        int parentPosition =
                positionOf(
                        candidates,
                        parentKey);

        int position =
                parentPosition < 0
                        ? Integer.MAX_VALUE - 20
                        : parentPosition;

        int subOrder =
                parentPosition < 0
                        ? 0
                        : nextSubOrder(
                                candidates,
                                position);

        addCandidate(
                candidates,
                position,
                subOrder,
                key,
                label,
                fields);
    }

    private static int positionOf(
            List<SectionCandidate> candidates,
            String key) {

        return candidates.stream()
                .filter(candidate ->
                        key.equals(
                                candidate.section().getKey()))
                .mapToInt(SectionCandidate::position)
                .min()
                .orElse(-1);
    }

    private static int nextSubOrder(
            List<SectionCandidate> candidates,
            int position) {

        return candidates.stream()
                .filter(candidate ->
                        candidate.position() == position)
                .mapToInt(SectionCandidate::subOrder)
                .max()
                .orElse(0)
                + 1;
    }

    private static void mergeCandidate(
            List<SectionCandidate> candidates,
            int position,
            int subOrder,
            String key,
            String label,
            List<SyllabusImportData.TemplateField> fields) {

        if (fields.isEmpty()) {
            return;
        }

        for (int index = 0; index < candidates.size(); index++) {
            SectionCandidate candidate =
                    candidates.get(index);

            if (!key.equals(candidate.section().getKey())) {
                continue;
            }

            List<SyllabusImportData.TemplateField> merged =
                    new ArrayList<>(
                            candidate.section().getFields());

            merged.addAll(fields);

            candidate.section().setFields(
                    uniqueFields(merged));

            if (position < candidate.position()) {
                candidates.set(
                        index,
                        new SectionCandidate(
                                position,
                                subOrder,
                                candidate.section()));
            }

            return;
        }

        addCandidate(
                candidates,
                position,
                subOrder,
                key,
                label,
                fields);
    }
    /**
     * Adds a section whose existence has already been proven by the source.
     *
     * Unlike addCandidate(), an empty field list is allowed here because
     * source recognition and successful field extraction are different facts.
     * An empty recognized section is required so provenance can later classify
     * parser failures as UNRESOLVED instead of ABSENT_IN_SOURCE.
     */
    private static void addRecognizedCandidate(
            List<SectionCandidate> candidates,
            int position,
            int subOrder,
            String key,
            String label,
            List<SyllabusImportData.TemplateField> fields) {

        List<SyllabusImportData.TemplateField> safeFields =
                fields == null
                        ? List.of()
                        : fields;

        for (int index = 0; index < candidates.size(); index++) {
            SectionCandidate candidate =
                    candidates.get(index);

            if (!key.equals(candidate.section().getKey())) {
                continue;
            }

            if (!safeFields.isEmpty()) {
                List<SyllabusImportData.TemplateField> merged =
                        new ArrayList<>(
                                candidate.section().getFields());

                merged.addAll(safeFields);

                candidate.section().setFields(
                        uniqueFields(merged));
            }

            if (position < candidate.position()) {
                candidates.set(
                        index,
                        new SectionCandidate(
                                position,
                                subOrder,
                                candidate.section()));
            }

            return;
        }

        candidates.add(
                new SectionCandidate(
                        position,
                        subOrder,
                        section(
                                key,
                                label,
                                safeFields)));
    }
    private static void addCandidate(
            List<SectionCandidate> candidates,
            int position,
            int subOrder,
            String key,
            String label,
            List<SyllabusImportData.TemplateField> fields) {

        if (fields == null || fields.isEmpty()) {
            return;
        }

        boolean exists =
                candidates.stream()
                        .anyMatch(candidate ->
                                key.equals(
                                        candidate.section().getKey()));

        if (exists) {
            mergeCandidate(
                    candidates,
                    position,
                    subOrder,
                    key,
                    label,
                    fields);
            return;
        }

        candidates.add(
                new SectionCandidate(
                        position,
                        subOrder,
                        section(
                                key,
                                label,
                                fields)));
    }

    private static List<SyllabusImportData.TemplateSection> orderedSections(
            List<SectionCandidate> candidates) {

        return candidates.stream()
                .sorted(
                        Comparator
                                .comparingInt(SectionCandidate::position)
                                .thenComparingInt(SectionCandidate::subOrder))
                .map(SectionCandidate::section)
                .toList();
    }

    private static SyllabusImportData.TemplateSection section(
            String key,
            String label,
            List<SyllabusImportData.TemplateField> fields) {

        return SyllabusImportData.TemplateSection.builder()
                .key(key)
                .label(label)
                .fields(uniqueFields(fields))
                .build();
    }

    private static SyllabusImportData.TemplateField field(
            String key,
            String label) {

        return SyllabusImportData.TemplateField.builder()
                .key(key)
                .label(label)
                .build();
    }

    private static List<SyllabusImportData.TemplateField> uniqueFields(
            List<SyllabusImportData.TemplateField> fields) {

        Map<String, SyllabusImportData.TemplateField> unique =
                new LinkedHashMap<>();

        for (SyllabusImportData.TemplateField field : fields) {
            if (field == null
                    || field.getKey() == null
                    || field.getKey().isBlank()) {
                continue;
            }

            unique.putIfAbsent(
                    field.getKey(),
                    field);
        }

        return new ArrayList<>(unique.values());
    }

    private static void addIfNotNull(
            List<SyllabusImportData.TemplateField> fields,
            Supplier<String> supplier,
            String key,
            String label) {

        if (supplier.get() != null) {
            fields.add(
                    field(
                            key,
                            label));
        }
    }

    private static String cellText(
            XWPFTableCell cell) {

        return cell == null
                ? ""
                : cell.getText();
    }

    private static String normalizeHeading(
            String value) {

        return NUMBERED_HEADING_PREFIX
                .matcher(
                        normalize(value))
                .replaceFirst("")
                .trim();
    }

    private static boolean matchesAny(
            String value,
            String... candidates) {

        for (String candidate : candidates) {
            if (value.contains(
                    normalize(candidate))) {
                return true;
            }
        }

        return false;
    }

    private static String normalizeForSearch(
            String value) {

        return normalize(value)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String normalize(
            String value) {

        if (value == null) {
            return "";
        }

        String ascii =
                Normalizer.normalize(
                                value,
                                Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "");

        return ascii
                .toLowerCase(Locale.ROOT)
                .replace('\u00a0', ' ')
                .replaceAll("[^a-z0-9%×x–—/-]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record SectionCandidate(
            int position,
            int subOrder,
            SyllabusImportData.TemplateSection section) {
    }
}
