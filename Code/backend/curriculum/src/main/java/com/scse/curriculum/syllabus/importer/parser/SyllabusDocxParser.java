package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.syllabus.importer.dto.AssessmentImportData;
import com.scse.curriculum.syllabus.importer.dto.CloImportData;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportIssue;
import com.scse.curriculum.syllabus.importer.dto.TopicImportData;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Semantic DOCX parser. It keys off bilingual labels and table headers instead of page positions,
 * so departments can reorder sections or restyle the Word template without changing the DTO.
 */
@Component
public class SyllabusDocxParser implements SyllabusFileParser {
    private static final Pattern COURSE_CODE = Pattern.compile("(?i)Course\\s*Code\\s*:\\s*([A-Z]{2,}\\d+[A-Z]*)");
    private static final Pattern WEIGHT = Pattern.compile("\\((\\d+(?:\\.\\d+)?)%\\)");
    private static final Pattern YEAR = Pattern.compile("(?<!\\d)(19|20)\\d{2}(?!\\d)");

    @Override
    public boolean supports(String fileName, String contentType) {
        return (fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".docx"))
                || "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                .equalsIgnoreCase(contentType);
    }

    @Override
    public SyllabusImportData parse(InputStream input, List<SyllabusImportIssue> issues) throws IOException {
        try (XWPFDocument document = new XWPFDocument(input)) {
            return parse(document.getBodyElements(), issues);
        }
    }

    public SyllabusImportData parse(List<IBodyElement> body, List<SyllabusImportIssue> issues) {
            SyllabusImportData data = SyllabusImportData.builder().build();
            parseCourseCode(body, data);
            parseGeneralInformation(body.stream().filter(XWPFTable.class::isInstance)
                    .map(XWPFTable.class::cast).toList(), data);
            parseObjectives(body, data);
            parseClos(body, data);
            parseTopics(body, data);
            parseWeeklyPlan(body, data);
            parseAssessments(body, data);
            parseReadingList(body, data);
            parseRevisionDate(body, data);
            validate(data, issues);
            return data;
    }

    private void parseCourseCode(List<IBodyElement> body, SyllabusImportData data) {
        String previousParagraph = null;
        for (IBodyElement element : body) {
            if (!(element instanceof XWPFParagraph paragraph)) continue;
            String paragraphText = clean(paragraph.getText());
            Matcher matcher = COURSE_CODE.matcher(paragraphText);
            if (matcher.find()) {
                data.setSourceCourseCode(matcher.group(1).toUpperCase(Locale.ROOT));
                if (previousParagraph != null && previousParagraph.matches("(?is).*Course\\s*Name\\s*:.*")) {
                    data.setSourceCourseName(previousParagraph.replaceFirst(
                            "(?i)^\\s*(?:\\d+\\.\\s*)?Course\\s*Name\\s*:\\s*", ""));
                }
                return;
            }
            if (!paragraphText.isBlank()) previousParagraph = paragraphText;
        }
    }

    private void parseGeneralInformation(List<XWPFTable> tables, SyllabusImportData data) {
        for (XWPFTable table : tables) {
            for (XWPFTableRow row : table.getRows()) {
                if (row.getTableCells().size() < 2) continue;
                String label = normalized(cell(row, 0));
                String value = clean(cell(row, 1));
                if (matches(label, "course name", "ten hoc phan") && data.getSourceCourseName() == null) data.setSourceCourseName(value);
                else if (matches(label, "course designation", "mo ta hoc phan")) data.setCourseDesignation(value);
                else if (matches(label, "course type", "loai hoc phan")) data.setCourseTypes(value);
                else if (matches(label, "semester", "hoc ky giang day")) data.setSemester(normalizeSemester(value));
                else if (matches(label, "person responsible", "nguoi phu trach")) data.setPersonResponsible(value);
                else if (matches(label, "language", "ngon ngu giang day")) data.setLanguage(value);
                else if (matches(label, "relation to curriculum", "quan he voi chuong trinh")) data.setRelation(value);
                else if (matches(label, "teaching methods", "phuong phap giang day")) data.setTeachingMethods(value);
                else if (matches(label, "workload", "khoi luong hoc tap")) parseWorkload(value, data);
                else if (matches(label, "credit points", "so tin chi")) parseCredits(value, data);
                else if (matches(label, "prerequisites", "dieu kien tien quyet")) data.setPrerequisites(value);
            }
        }
    }

    private void parseObjectives(List<IBodyElement> body, SyllabusImportData data) {
        data.setObjectives(sectionParagraphs(body,
                List.of("course objectives", "muc tieu hoc phan"),
                List.of("course learning outcomes", "chuan dau ra hoc phan")));
    }

    private void parseClos(List<IBodyElement> body, SyllabusImportData data) {
        XWPFTable table = tableAfterHeading(body, "course learning outcomes", "chuan dau ra hoc phan");
        if (table == null) {
            parseLearningOutcomeMatrix(body, data);
            return;
        }
        List<CloImportData> clos = new ArrayList<>();
        List<SyllabusImportData.CloPloMappingItem> mappings = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < table.getNumberOfRows(); rowIndex++) {
            XWPFTableRow row = table.getRow(rowIndex);
            int codeColumn = findCell(row, "CLO\\s*\\d+");
            if (codeColumn < 0) continue;
            String code = clean(cell(row, codeColumn)).replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
            String competency = codeColumn > 0 ? competency(cell(row, codeColumn - 1)) : null;
            String description = codeColumn + 1 < row.getTableCells().size() ? clean(cell(row, codeColumn + 1)) : "";
            clos.add(CloImportData.builder().code(code).description(description)
                    .competencyLevel(competency).orderIndex(clos.size() + 1).build());
            for (String plo : extractCodes(cell(row, row.getTableCells().size() - 1), "PLO")) {
                mappings.add(SyllabusImportData.CloPloMappingItem.builder()
                        .cloCode(code).ploCode(plo).value("x").contributionWeight(100f).build());
            }
        }
        if(clos.isEmpty()) parseLearningOutcomeMatrix(body,data);
        else {
            data.setClos(clos);
            data.setCloPloMappings(mappings);
        }
    }

    private void parseLearningOutcomeMatrix(List<IBodyElement> body, SyllabusImportData data) {
        XWPFTable table=tableAfterHeading(body,"learning outcomes matrix","ma tran chuan dau ra");
        if(table==null||table.getNumberOfRows()<2) return;
        List<CloImportData> clos=new ArrayList<>();
        List<SyllabusImportData.CloPloMappingItem> mappings=new ArrayList<>();
        for(int rowIndex=1;rowIndex<table.getNumberOfRows();rowIndex++) {
            XWPFTableRow row=table.getRow(rowIndex);
            Integer ordinal=integer(clean(cell(row,0)));
            if(ordinal==null) continue;
            String cloCode="CLO"+ordinal;
            // This compact matrix declares the CLO identities and their SLO/PLO
            // relations, but contains no descriptions. Preserve that absence.
            clos.add(CloImportData.builder().code(cloCode).orderIndex(ordinal).build());
            for(int column=1;column<row.getTableCells().size();column++) {
                if(!clean(cell(row,column)).isBlank()) mappings.add(
                        SyllabusImportData.CloPloMappingItem.builder()
                                .cloCode(cloCode).ploCode("PLO"+column)
                                .value("x").contributionWeight(100f).build());
            }
        }
        data.setClos(clos);
        data.setCloPloMappings(mappings);
    }

    private void parseTopics(List<IBodyElement> body, SyllabusImportData data) {
        XWPFTable table = tableAfterHeading(body, "course content", "noi dung hoc phan");
        if(table==null) table=tableAfterHeading(body,"planned learning activities","ke hoach giang day");
        if (table == null) return;
        List<TopicImportData> topics = new ArrayList<>();
        List<SyllabusImportData.TopicCloMappingItem> mappings = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < table.getNumberOfRows(); rowIndex++) {
            XWPFTableRow row = table.getRow(rowIndex);
            if (row.getTableCells().size() < 3 || clean(cell(row, 0)).isBlank()) continue;
            int topicIndex = topics.size() + 1;
            Integer sourceWeek=integer(clean(cell(row,0)));
            boolean weeklyShape=sourceWeek!=null&&row.getTableCells().size()>=5;
            topics.add(TopicImportData.builder().weekNumber(weeklyShape?sourceWeek:topicIndex).orderInWeek(1)
                    .name(clean(cell(row,weeklyShape?1:0)))
                    .contentWeight(weeklyShape?null:clean(cell(row,1)))
                    .contentLevel(weeklyShape?null:clean(cell(row,2)))
                    .teachingLevel(weeklyShape?null:clean(cell(row,2)))
                    .teachingMethod(weeklyShape?clean(cell(row,4)):null)
                    .learningActivity(weeklyShape?clean(cell(row,4)):null)
                    .resources(weeklyShape&&row.getTableCells().size()>5?clean(cell(row,5)):null)
                    .topicType("LECTURE").build());
            int cloColumn=weeklyShape?2:3;
            if (row.getTableCells().size() > cloColumn) {
                for (String clo : extractOrdinalReferences(cell(row,cloColumn), "CLO")) {
                    mappings.add(SyllabusImportData.TopicCloMappingItem.builder()
                            .topicIndex(topicIndex).cloCode(clo).build());
                }
            }
        }
        data.setTopics(topics);
        data.setTopicCloMappings(mappings);
    }

    private void parseWeeklyPlan(List<IBodyElement> body, SyllabusImportData data) {
        XWPFTable table = tableAfterHeading(body, "planned learning activities", "ke hoach giang day");
        if (table == null) return;
        List<SyllabusImportData.WeeklyActivityItem> weekly = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < table.getNumberOfRows(); rowIndex++) {
            XWPFTableRow row = table.getRow(rowIndex);
            Integer week = integer(clean(cell(row, 0)));
            if (week == null || row.getTableCells().size() < 2) continue;
            weekly.add(SyllabusImportData.WeeklyActivityItem.builder().week(week)
                    .topic(clean(cell(row, 1)))
                    .clo(row.getTableCells().size() > 2 ? clean(cell(row, 2)) : null)
                    .assessments(row.getTableCells().size() > 3 ? clean(cell(row, 3)) : null)
                    .learningActivities(row.getTableCells().size() > 4 ? clean(cell(row, 4)) : null)
                    .resources(row.getTableCells().size() > 5 ? clean(cell(row, 5)) : null).build());
        }
        data.setWeeklyActivities(weekly);
    }

    private void parseAssessments(List<IBodyElement> body, SyllabusImportData data) {
        XWPFTable table = tableAfterHeading(body, "assessment plan", "ke hoach danh gia");
        if (table == null) return;
        List<AssessmentImportData> assessments = new ArrayList<>();
        List<SyllabusImportData.AssessmentCloMappingItem> mappings = new ArrayList<>();
        int current = 0;
        for (int rowIndex = 1; rowIndex < table.getNumberOfRows(); rowIndex++) {
            XWPFTableRow row = table.getRow(rowIndex);
            String first = clean(cell(row, 0));
            Matcher weight = WEIGHT.matcher(first);
            if (weight.find()) {
                current = assessments.size() + 1;
                assessments.add(AssessmentImportData.builder().name(first)
                        .assessmentType(assessmentType(first)).weightPercent(Float.parseFloat(weight.group(1)))
                        .minScore(0f).maxScore(100f).orderIndex(current).build());
                continue;
            }
            if (current == 0) continue;
            for (int column = 1; column < row.getTableCells().size(); column++) {
                Matcher percentage = Pattern.compile("(\\d+(?:\\.\\d+)?)%").matcher(cell(row, column));
                if (percentage.find()) mappings.add(SyllabusImportData.AssessmentCloMappingItem.builder()
                        .assessmentIndex(current).cloCode("CLO" + column)
                        .percentage(Double.parseDouble(percentage.group(1))).build());
            }
        }
        data.setAssessments(assessments);
        data.setAssessmentCloMappings(mappings);
    }

    private void parseReadingList(List<IBodyElement> body, SyllabusImportData data) {
        String section = sectionParagraphs(body, List.of("reading list", "tai lieu tham khao"),
                List.of("date revised", "ngay cap nhat", "lecturer", "giang vien"));
        List<SyllabusImportData.ReadingItem> readings = new ArrayList<>();
        for (String item : section.split("(?=\\[\\d+])")) {
            String value = clean(item).replaceFirst("^\\[\\d+]\\s*", "");
            if (value.isBlank()) continue;
            Matcher year = YEAR.matcher(value);
            readings.add(SyllabusImportData.ReadingItem.builder().title(value)
                    .year(year.find() ? Integer.parseInt(year.group()) : null).type("REFERENCE").build());
        }
        data.setReadings(readings);
    }

    private void parseRevisionDate(List<IBodyElement> body, SyllabusImportData data) {
        Pattern pattern = Pattern.compile("(?i)(?:Date revised|Ngay cap nhat)\\s*:\\s*(\\d{1,2}/\\d{1,2}/\\d{4})");
        for (IBodyElement element : body) {
            if (!(element instanceof XWPFParagraph paragraph)) continue;
            String ascii = ascii(paragraph.getText());
            Matcher matcher = pattern.matcher(ascii);
            if (matcher.find()) {
                data.setDateRevised(LocalDate.parse(matcher.group(1), DateTimeFormatter.ofPattern("d/M/uuuu")).toString());
                return;
            }
        }
    }

    private String sectionParagraphs(List<IBodyElement> body, List<String> starts, List<String> ends) {
        boolean active = false;
        List<String> values = new ArrayList<>();
        for (IBodyElement element : body) {
            if (!(element instanceof XWPFParagraph paragraph)) continue;
            String text = clean(paragraph.getText());
            String normalized = normalized(text);
            if (!active && starts.stream().anyMatch(normalized::contains)) { active = true; continue; }
            if (active && ends.stream().map(this::normalized).anyMatch(normalized::contains)) break;
            if (active && !text.isBlank()) values.add(text);
        }
        return clean(String.join(" ", values));
    }

    private XWPFTable tableAfterHeading(List<IBodyElement> body, String... aliases) {
        boolean headingFound = false;
        for (IBodyElement element : body) {
            if (element instanceof XWPFParagraph paragraph) {
                String value = normalized(paragraph.getText());
                if (List.of(aliases).stream().map(this::normalized).anyMatch(value::contains)) headingFound = true;
            } else if (headingFound && element instanceof XWPFTable table) return table;
        }
        return null;
    }

    private void parseWorkload(String value, SyllabusImportData data) {
        data.setWorkloadTotal(group(value, "(?i)(?:Total|Tong)\\s*:\\s*(\\d+)"));
        data.setWorkloadContact(group(value, "(?i)(\\d+)\\s*(?:contact periods|tiet len lop)"));
        data.setWorkloadPrivate(group(value, "(?i)(\\d+)\\s*(?:self-study hours|gio tu hoc)"));
    }

    private void parseCredits(String value, SyllabusImportData data) {
        data.setCreditPoints(group(value, "(?i)(\\d+)\\s*(?:credits|tin chi)"));
        data.setLectureCredits(group(value, "(?i)(?:Theory|Ly thuyet)\\s*:\\s*(\\d+)"));
        data.setLaboratoryCredits(group(value, "(?i)(?:Practice|Thuc hanh)\\s*:\\s*(\\d+)"));
    }

    private String assessmentType(String value) {
        String normalized = normalized(value);
        if (normalized.contains("midterm") || normalized.contains("giua ky")) return "MIDTERM_EXAM";
        if (normalized.contains("final") || normalized.contains("cuoi ky")) return "FINAL_EXAM";
        return "ASSIGNMENT";
    }

    private String competency(String value) {
        String normalized = normalized(value);
        if (normalized.contains("knowledge") || normalized.contains("kien thuc")) return "KNOWLEDGE";
        if (normalized.contains("skill") || normalized.contains("ky nang")) return "SKILL";
        if (normalized.contains("autonomy") || normalized.contains("tu chu") || normalized.contains("responsibility")) return "ATTITUDE";
        return null;
    }

    private List<String> extractCodes(String value, String prefix) {
        List<String> result = new ArrayList<>();
        Matcher matcher = Pattern.compile("(?i)" + prefix + "\\s*(\\d+)").matcher(value);
        while (matcher.find()) result.add(prefix + matcher.group(1));
        return result;
    }

    private List<String> extractOrdinalReferences(String value,String prefix) {
        List<String> explicit=extractCodes(value,prefix);
        if(!explicit.isEmpty()) return explicit;
        List<String> result=new ArrayList<>();
        Matcher matcher=Pattern.compile("(?<!\\d)(\\d+)(?!\\d)").matcher(value==null?"":value);
        while(matcher.find()) result.add(prefix+matcher.group(1));
        return result;
    }

    private int findCell(XWPFTableRow row, String expression) {
        for (int i = 0; i < row.getTableCells().size(); i++)
            if (Pattern.compile(expression, Pattern.CASE_INSENSITIVE).matcher(cell(row, i)).find()) return i;
        return -1;
    }

    private String cell(XWPFTableRow row, int index) {
        if (index < 0 || index >= row.getTableCells().size()) return "";
        XWPFTableCell cell = row.getCell(index);
        return cell == null ? "" : cell.getText();
    }

    private boolean matches(String value, String... aliases) {
        return List.of(aliases).stream().map(this::normalized).anyMatch(value::contains);
    }

    private String normalized(String value) { return ascii(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim(); }
    private String ascii(String value) { return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD).replaceAll("\\p{M}", ""); }
    private String clean(String value) { return value == null ? "" : value.replace('☑', ' ').replace('•', ' ').replaceAll("\\s+", " ").trim(); }
    private Integer integer(String value) { try { return Integer.valueOf(value.trim()); } catch (Exception ignored) { return null; } }
    private String group(String value, String regex) { Matcher matcher = Pattern.compile(regex).matcher(ascii(value)); return matcher.find() ? matcher.group(1) : null; }
    private String normalizeSemester(String value) { String number = group(value, "(?<!\\d)([1-8])(?!\\d)"); return number == null ? value : "Semester " + number; }

    private void validate(SyllabusImportData data, List<SyllabusImportIssue> issues) {
        Map<String, String> required = Map.of("sourceCourseCode", clean(data.getSourceCourseCode()),
                "sourceCourseName", clean(data.getSourceCourseName()), "objectives", clean(data.getObjectives()));
        required.forEach((field, value) -> { if (value.isBlank()) issues.add(issue("WARNING", field, "Field was not recognized from the DOCX template.")); });
        if (data.getClos().isEmpty()) issues.add(issue("WARNING", "clos", "No course learning outcomes were found."));
        if (data.getTopics().isEmpty()) issues.add(issue("WARNING", "topics", "No course content rows were found."));
        if (data.getAssessments().isEmpty()) issues.add(issue("WARNING", "assessments", "No assessment groups were found."));
    }

    private SyllabusImportIssue issue(String severity, String field, String message) {
        return SyllabusImportIssue.builder().severity(severity).section("DOCX semantic import").field(field).message(message).build();
    }
}
