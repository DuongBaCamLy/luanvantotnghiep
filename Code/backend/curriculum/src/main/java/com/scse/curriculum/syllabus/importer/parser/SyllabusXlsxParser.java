package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.syllabus.importer.dto.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;

/** Reads the five-sheet Field/Value and header-table template in Code/templates. */
@Component
public class SyllabusXlsxParser implements SyllabusFileParser {
    public static final String MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Override
    public boolean supports(String fileName, String contentType) {
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx")
                || MIME.equalsIgnoreCase(contentType);
    }

    @Override
    public SyllabusImportData parse(InputStream input, List<SyllabusImportIssue> issues) throws IOException {
        SyllabusImportData data = SyllabusImportData.builder().build();
        try (InputStream source = input; XSSFWorkbook workbook = new XSSFWorkbook(source)) {
            Reader reader = new Reader(workbook, issues);
            reader.rows(
        List.of(
                "General Info",
                "General Information"
        ),
        true,
        List.of("Field", "Value"),
        row -> {
                String rawField =
        normalize(
                row.text("Field")
        );

String field =
        canonicalGeneralField(
                rawField
        );

String value =
        row.text("Value");

if (!reader.generalFields.add(field)) {

    row.issue(
            "ERROR",
            "Field",
            "Duplicate general information field: "
                    + rawField
    );

    return;
}

switch (field) {
                    case "course code" -> {
                        String code = value.toUpperCase(Locale.ROOT).replaceAll("[\\s_-]+", "");
                        data.setSourceCourseCode(code);
                        if (!code.isBlank() && !code.matches("[A-Z]{2,}\\d+[A-Z]*")) {
                            row.issue("ERROR", "course code", "Enter the academic course code, for example IT116 or IT116IU.");
                        }
                    }
                    case "course name" -> data.setSourceCourseName(value);
                    case "course designation" -> data.setCourseDesignation(value);
                    case "course types" -> data.setCourseTypes(value);
                    case "semester" -> data.setSemester(value);
                    case "language" -> data.setLanguage(value);
                    case "relation" -> data.setRelation(value);
                    case "teaching methods" -> data.setTeachingMethods(value);
                    case "workload total" -> data.setWorkloadTotal(value);
                    case "workload contact" -> data.setWorkloadContact(value);
                    case "workload private" -> data.setWorkloadPrivate(value);
                    case "prerequisites" -> data.setPrerequisites(value);
                    case "objectives" -> data.setObjectives(value);
                    case "exam forms" -> data.setExamForms(value);
                    case "exam requirements" -> data.setExamRequirements(value);
                    case "major" -> data.setMajor(value);
                    case "rubrics" -> {
                        if (!value.isBlank()) data.getRubricItems().add(
                                SyllabusImportData.RubricItem.builder().title(value).build());
                    }
                    default -> row.issue("WARNING", "Field", "Unrecognized general information field: " + field);
                }
            });
            Set<String> cloCodes = new HashSet<>();
            reader.rows(
        List.of(
                "CLO",
                "Learning Outcomes",
                "Course Learning Outcomes"
        ),
        false,
        List.of("Code", "Description"),
        row -> {
                String code = row.text("Code").toUpperCase(Locale.ROOT).replaceAll("[\\s._-]+", "");
                if (!code.matches("CLO[1-9]\\d*")) {
                    row.issue("ERROR", "Code", "Use a CLO code such as CLO1 or CLO2.");
                    return;
                }
                if (!cloCodes.add(code)) {
                    row.issue("ERROR", "Code", "Duplicate CLO code: " + code);
                    return;
                }
                data.getClos().add(CloImportData.builder().code(code)
                        .description(row.required("Description")).descriptionVn(row.text("Description VN"))
                        .competencyLevel(row.enumeration("Competency Level", "KNOWLEDGE", "SKILL", "ATTITUDE"))
                        .bloomLevel(row.enumeration("Bloom Level", "REMEMBER", "UNDERSTAND", "APPLY", "ANALYZE", "EVALUATE", "CREATE"))
                        .orderIndex(row.integer("Order Index", 1)).build());
            });
            reader.rows(
        List.of(
                "Topics",
                "Course Content",
                "Course Topics"
        ),
        false,
        List.of(
                "Week Number",
                "Order In Week",
                "Name"
        ),
        row ->
                    data.getTopics().add(TopicImportData.builder()
                            .weekNumber(row.integer("Week Number", 1)).orderInWeek(row.integer("Order In Week", 1))
                            .name(row.required("Name")).nameVn(row.text("Name VN"))
                            .teachingHours(row.integer("Teaching Hours", 0)).labHours(row.integer("Lab Hours", 0))
                            .selfStudyHours(row.integer("Self Study Hours", 0))
                            .topicType(row.enumeration("Topic Type", "LECTURE", "LAB", "SEMINAR", "EXAM", "PROJECT", "SELF_STUDY"))
                            .teachingMethod(row.text("Teaching Method")).learningActivity(row.text("Learning Activity"))
                            .notes(row.text("Notes")).build()));
            reader.rows(
        List.of(
                "Assessments",
                "Assessment Plan",
                "Assessment Scheme",
                "Assessment Methods"
        ),
        false,
        List.of(
                "Name",
                "Assessment Type",
                "Weight Percent"
        ),
        row -> {
                Float weight = row.number("Weight Percent", 0, 100);
                Float min = row.number("Min Score", 0, Float.MAX_VALUE);
                Float max = row.number("Max Score", 0, Float.MAX_VALUE);
                if (min != null && max != null && min > max) row.issue("ERROR", "Min Score", "Min Score must not exceed Max Score.");
                data.getAssessments().add(AssessmentImportData.builder()
                        .name(row.required("Name")).nameVn(row.text("Name VN"))
                        .assessmentType(row.enumeration("Assessment Type", "QUIZ", "ASSIGNMENT", "LAB_REPORT", "MIDTERM_EXAM", "FINAL_EXAM", "PROJECT", "PRESENTATION", "PARTICIPATION"))
                        .weightPercent(weight).minScore(min).maxScore(max).orderIndex(row.integer("Order Index", 1)).build());
            });
            reader.rows(
        List.of(
                "Reading List",
                "References",
                "Bibliography"
        ),
        false,
        List.of(
                "Title",
                "Author",
                "Publisher",
                "Year",
                "Book Type"
        ),
        row -> {
                data.getReadings().add(SyllabusImportData.ReadingItem.builder()
                        .title(row.required("Title")).author(row.text("Author")).publisher(row.text("Publisher"))
                        .year(row.integer("Year", 1)).type(row.text("Book Type")).build());
                for (String field : List.of("Edition", "ISBN", "URL", "Usage Type", "Order Index")) {
                    if (!row.text(field).isBlank()) row.issue("WARNING", field,
                            "This reading metadata is not supported by the current import DTO; it is not imported.");
                }
            });
            if (data.getSourceCourseCode() == null || data.getSourceCourseCode().isBlank()) {
                reader.issue("ERROR", "General Info", null, "course code",
                        "Fill the course code row in General Info with the selected course's code before importing.");
            }
            if (data.getSourceCourseName() == null || data.getSourceCourseName().isBlank()) {
                reader.issue("ERROR", "General Info", null, "course name",
                        "Fill the course name row in General Info with the selected course's name before importing.");
            }

            /*
             * Preserve workbook sheet/header order for template-aware
             * comparison. Unknown sheets remain import warnings and are not
             * converted into fake syllabus sections.
             */
            data.setTemplateSections(
                    SyllabusTemplateSectionDetector.fromXlsx(
                            workbook,
                            data));

        } catch (IOException | RuntimeException exception) {
            issues.add(SyllabusImportIssue.builder().severity("ERROR").section("Workbook")
                    .message("Cannot read this XLSX workbook. Use an unencrypted syllabus import template with valid cells.").build());
        }
        return data;
    }
    static String canonicalHeader(
        String value) {

    String header =
            normalize(value);

    return switch (header) {

        case "clo code" ->
                "code";

        case "learning outcome",
             "clo description",
             "outcome description" ->
                "description";

        case "week" ->
                "week number";

        case "topic order" ->
                "order in week";

        case "topic name" ->
                "name";

        case "assessment name" ->
                "name";

        case "assessment category" ->
                "assessment type";

        case "weight",
             "weight %",
             "weight (%)" ->
                "weight percent";

        case "reference title" ->
                "title";

        case "publication year" ->
                "year";

        case "resource type" ->
                "book type";

        default ->
                header;
    };
}
static String canonicalGeneralField(
        String value) {

    String field =
            normalize(value);

    return switch (field) {

        case "module code" ->
                "course code";

        case "module name" ->
                "course name";

        case "course classification",
             "course category" ->
                "course designation";

        case "course type" ->
                "course types";

        case "medium of instruction",
             "language of instruction",
             "instruction language" ->
                "language";

        case "curriculum relation",
             "relation to curriculum",
             "relation to the curriculum" ->
                "relation";

        case "instructional methods",
             "teaching methodology",
             "teaching and learning methods",
             "methods of instruction" ->
                "teaching methods";

        case "required prerequisites",
             "recommended prerequisites" ->
                "prerequisites";

        case "course aims" ->
                "objectives";

        default ->
                field;
    };
}


private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static final class Reader {
        private final Workbook workbook;
        private final List<SyllabusImportIssue> issues;
        private final Set<String> generalFields = new HashSet<>();
        private final DataFormatter formatter = new DataFormatter(Locale.ROOT);

        Reader(Workbook workbook, List<SyllabusImportIssue> issues) {
            this.workbook = workbook;
            this.issues = issues;
            // Cached results avoid evaluating external links or unsupported formulas.
            formatter.setUseCachedValuesForFormulaCells(true);
        }

        void issue(String severity, String sheet, Integer row, String field, String message) {
            issues.add(SyllabusImportIssue.builder().severity(severity).section(sheet).row(row)
                    .field(field).message(message).build());
        }

        void rows(
        String name,
        boolean mandatory,
        List<String> requiredHeaders,
        Consumer<DataRow> consumer) {

    rows(
            List.of(name),
            mandatory,
            requiredHeaders,
            consumer
    );
}

void rows(
        List<String> names,
        boolean mandatory,
        List<String> requiredHeaders,
        Consumer<DataRow> consumer) {

    String canonicalName =
            names.getFirst();

    Set<String> normalizedNames =
            names.stream()
                    .map(SyllabusXlsxParser::normalize)
                    .collect(
                            java.util.stream.Collectors.toSet()
                    );

    List<Sheet> matches =
            new ArrayList<>();

    for (Sheet sheet : workbook) {

        if (normalizedNames.contains(
                normalize(
                        sheet.getSheetName()
                )
        )) {
            matches.add(sheet);
        }
    }

    if (matches.size() != 1) {

        issue(
                matches.isEmpty() && !mandatory
                        ? "WARNING"
                        : "ERROR",
                canonicalName,
                null,
                null,
                matches.isEmpty()
                        ? "Sheet is missing."
                        : "Ambiguous duplicate sheet names."
        );

        return;
    }

    Sheet sheet =
            matches.getFirst();

    for (Row row : sheet) {

        Map<String, Integer> columns =
                new LinkedHashMap<>();

        boolean duplicate =
                false;

        for (Cell cell : row) {

    String rawHeader =
            formatter.formatCellValue(cell);

    String header =
            canonicalHeader(
                    rawHeader
            );

    if (!header.isEmpty()
            && columns.putIfAbsent(
            header,
            cell.getColumnIndex()
    ) != null) {

        duplicate = true;
    }
}
        if (!requiredHeaders.stream()
        .map(SyllabusXlsxParser::canonicalHeader)
        .allMatch(columns::containsKey)) {

    continue;
}

        if (duplicate) {

            issue(
                    "ERROR",
                    canonicalName,
                    row.getRowNum() + 1,
                    null,
                    "Duplicate column headers."
            );

            return;
        }

        int count =
                0;

        for (int index =
             row.getRowNum() + 1;
             index <= sheet.getLastRowNum();
             index++) {

            Row source =
                    sheet.getRow(index);

            if (source == null) {
                continue;
            }

            boolean populated =
                    false;

            for (Cell cell : source) {

                if (!formatter
                        .formatCellValue(cell)
                        .isBlank()) {

                    populated = true;
                }
            }

            if (!populated) {
                continue;
            }

            consumer.accept(
                    new DataRow(
                            this,
                            source,
                            columns
                    )
            );

            count++;
        }

        if (count == 0) {

            issue(
                    mandatory
                            ? "ERROR"
                            : "WARNING",
                    canonicalName,
                    null,
                    null,
                    "Sheet has no data rows."
            );
        }

        return;
    }

    issue(
            "ERROR",
            canonicalName,
            null,
            null,
            "Missing table headers: "
                    + String.join(
                    ", ",
                    requiredHeaders
            )
    );
}
    }

    private record DataRow(Reader reader, Row row, Map<String, Integer> columns) {
        void issue(String severity, String field, String message) {
            reader.issue(severity, row.getSheet().getSheetName(), row.getRowNum() + 1, field, message);
        }

        String text(String field) {
            Integer column =
        columns.get(
                canonicalHeader(field)
        );
            Cell cell = column == null ? null : row.getCell(column);
            if (cell == null) return "";
            CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
            if (type == CellType.ERROR) {
                issue("ERROR", field, "Cell contains an Excel error; correct it and recalculate the workbook.");
                return "";
            }
            if (cell.getCellType() == CellType.FORMULA && !((org.apache.poi.xssf.usermodel.XSSFCell) cell).getCTCell().isSetV()) {
                issue("ERROR", field, "Formula has no cached result. Recalculate and save the workbook in Excel first.");
                return "";
            }
            if (type == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)
                    && !cell.getCellStyle().getDataFormatString().contains("%")) {
                return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            }
            return reader.formatter.formatCellValue(cell).trim();
        }

        String required(String field) {
            String value = text(field);
            if (value.isBlank()) issue("ERROR", field, "Required value is missing.");
            return value;
        }

        Integer integer(String field, int minimum) {
            String value = text(field);
            if (value.isBlank()) return null;
            try {
                int result = new BigDecimal(value).intValueExact();
                if (result < minimum) throw new ArithmeticException();
                return result;
            } catch (NumberFormatException | ArithmeticException exception) {
                issue("ERROR", field, "Expected an integer greater than or equal to " + minimum + ".");
                return null;
            }
        }

        Float number(String field, float min, float max) {
            String value = text(field);
            if (value.isBlank()) return null;
            try {
                float result = Float.parseFloat(value.replaceFirst("%$", "").trim());
                if (!Float.isFinite(result) || result < min || result > max) throw new NumberFormatException();
                return result;
            } catch (NumberFormatException exception) {
                issue("ERROR", field, "Expected a number between " + min + " and " + max + ".");
                return null;
            }
        }

        String enumeration(String field, String... allowed) {
            String value = text(field).toUpperCase(Locale.ROOT).replace(' ', '_');
            if (value.isBlank()) return null;
            if (!Arrays.asList(allowed).contains(value)) {
                issue("ERROR", field, "Unknown value. Expected: " + String.join(", ", allowed));
                return null;
            }
            return value;
        }
    }
}