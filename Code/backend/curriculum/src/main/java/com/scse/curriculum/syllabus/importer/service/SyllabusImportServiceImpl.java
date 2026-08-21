package com.scse.curriculum.syllabus.importer.service;

import com.scse.curriculum.assessment.entity.AssessmentType;
import com.scse.curriculum.book.entity.Book;
import com.scse.curriculum.book.entity.BookType;
import com.scse.curriculum.book.repository.BookRepository;
import com.scse.curriculum.clo.entity.BloomLevel;
import com.scse.curriculum.clo.entity.CompetencyLevel;
import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.clo.repository.CloRepository;
import com.scse.curriculum.cloplomapping.entity.CloPloMapping;
import com.scse.curriculum.cloplomapping.entity.ContributionLevel;
import com.scse.curriculum.cloplomapping.repository.CloPloMappingRepository;
import com.scse.curriculum.plo.entity.Plo;
import com.scse.curriculum.plo.repository.PloRepository;
import com.scse.curriculum.topic.entity.Topic;
import com.scse.curriculum.topic.repository.TopicRepository;
import com.scse.curriculum.topicclo.entity.TeachingLevel;
import com.scse.curriculum.topicclo.entity.TopicClo;
import com.scse.curriculum.topicclo.entity.TopicCloId;
import com.scse.curriculum.topicclo.repository.TopicCloRepository;
import com.scse.curriculum.assessment.entity.AssessmentComponent;
import com.scse.curriculum.assessment.repository.AssessmentComponentRepository;
import com.scse.curriculum.assessment.entity.AssessmentClo;
import com.scse.curriculum.assessment.entity.AssessmentCloId;
import com.scse.curriculum.assessment.repository.AssessmentCloRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.syllabus.dto.CreateSyllabusRequest;
import com.scse.curriculum.syllabus.dto.SyllabusResponse;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.syllabus.importer.dto.ConfirmSyllabusImportRequest;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportIssue;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportPreviewResponse;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.syllabus.service.SyllabusAccessService;
import com.scse.curriculum.syllabus.service.SyllabusService;
import com.scse.curriculum.syllabusbook.entity.SyllabusBook;
import com.scse.curriculum.syllabusbook.entity.SyllabusBookId;
import com.scse.curriculum.syllabusbook.entity.UsageType;
import com.scse.curriculum.syllabusbook.repository.SyllabusBookRepository;
import com.scse.curriculum.topic.entity.TopicType;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SyllabusImportServiceImpl implements SyllabusImportService {
    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private final SyllabusRepository syllabusRepository;
    private final SyllabusService syllabusService;
    private final SyllabusAccessService accessService;
    private final BookRepository bookRepository;
    private final SyllabusBookRepository syllabusBookRepository;
    private final CloRepository cloRepository;
    private final CloPloMappingRepository cloPloMappingRepository;
    private final TopicRepository topicRepository;
    private final TopicCloRepository topicCloRepository;
    private final AssessmentComponentRepository assessmentComponentRepository;
    private final AssessmentCloRepository assessmentCloRepository;
    private final PloRepository ploRepository;

    @Override
    @Transactional(readOnly = true)
    public SyllabusImportPreviewResponse preview(Integer syllabusId, MultipartFile file) {
        Syllabus syllabus = editable(syllabusId);
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Please select a PDF or Word file.");
        if (file.getSize() > MAX_SIZE) throw new IllegalArgumentException("The maximum file size is 10 MB.");
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("upload");
        String ext = extension(name);
        if (!Set.of("docx", "xlsx", "pdf").contains(ext))
            throw new IllegalArgumentException("Only .pdf, .docx and .xlsx files are supported.");

        List<SyllabusImportIssue> issues = new ArrayList<>();
        SyllabusImportData data;
        try (InputStream in = file.getInputStream()) {
            data = "xlsx".equals(ext) ? parseExcel(in, issues) : "pdf".equals(ext) ? parsePdf(in, issues) : parseWord(in, issues);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to read the file: " + ex.getMessage(), ex);
        }
        validate(data, issues);
        if (!blank(data.getSourceCourseCode())
                && syllabus.getCourse() != null
                && !normalizeCode(data.getSourceCourseCode()).equals(normalizeCode(syllabus.getCourse().getCourseCode()))) {
            warn(issues, "General Information", null, "courseCode",
                    "Imported course code " + data.getSourceCourseCode() + " does not match the selected course " + syllabus.getCourse().getCourseCode() + ". Review before confirming.");
        }
        int errors = (int) issues.stream().filter(i -> "ERROR".equals(i.getSeverity())).count();
        int warnings = issues.size() - errors;
        return SyllabusImportPreviewResponse.builder()
                .fileName(name).fileType(ext.toUpperCase()).valid(errors == 0)
                .errorCount(errors).warningCount(warnings).data(data).issues(issues).build();
    }

    @Override
    @Transactional
    public SyllabusResponse confirm(Integer syllabusId, ConfirmSyllabusImportRequest request) {
        Syllabus syllabus = editable(syllabusId);
        SyllabusImportData d = request.getData();
        String importMode = normalizeImportMode(request.getImportMode());
        List<SyllabusImportIssue> issues = new ArrayList<>();
        validate(d, issues);
        if (!blank(d.getSourceCourseCode())
                && syllabus.getCourse() != null
                && !normalizeCode(d.getSourceCourseCode()).equals(normalizeCode(syllabus.getCourse().getCourseCode()))) {
            warn(issues, "General Information", null, "courseCode",
                    "Imported course code " + d.getSourceCourseCode() + " does not match the selected course " + syllabus.getCourse().getCourseCode() + ".");
        }
        if (issues.stream().anyMatch(i -> "ERROR".equals(i.getSeverity())))
            throw new IllegalArgumentException("The imported data contains errors. Review and correct them before confirming.");

        SyllabusResponse existing = syllabusService.getById(syllabusId);
        CreateSyllabusRequest update = new CreateSyllabusRequest();
        update.setCourseId(syllabus.getCourse().getId());
        update.setAcademicYear(syllabus.getAcademicYear());
        update.setSemester(importedOrExisting(d.getSemester(), existing.getSemester(), importMode));
        update.setVersionNumber(syllabus.getVersionNumber());
        update.setVersionLabel(syllabus.getVersionLabel());
        update.setCourseDesignation(importedOrExisting(d.getCourseDesignation(), existing.getCourseDesignation(), importMode));
        update.setCourseTypes(importedOrExisting(d.getCourseTypes(), existing.getCourseTypes(), importMode));
        update.setLanguage(importedOrExisting(d.getLanguage(), existing.getLanguage(), importMode));
        update.setRelation(importedOrExisting(d.getRelation(), existing.getRelation(), importMode));
        update.setTeachingMethods(importedOrExisting(d.getTeachingMethods(), existing.getTeachingMethods(), importMode));
        update.setWorkloadTotal(importedOrExisting(d.getWorkloadTotal(), existing.getWorkloadTotal(), importMode));
        update.setWorkloadContact(importedOrExisting(d.getWorkloadContact(), existing.getWorkloadContact(), importMode));
        update.setWorkloadPrivate(importedOrExisting(d.getWorkloadPrivate(), existing.getWorkloadPrivate(), importMode));
        update.setPrerequisites(importedOrExisting(d.getPrerequisites(), existing.getPrerequisites(), importMode));
        update.setObjectives(importedOrExisting(d.getObjectives(), existing.getObjectives(), importMode));
        update.setExamForms(importedOrExisting(d.getExamForms(), existing.getExamForms(), importMode));
        update.setExamRequirements(importedOrExisting(d.getExamRequirements(), existing.getExamRequirements(), importMode));
        update.setRubrics(importedOrExisting(d.getRubrics(), existing.getRubrics(), importMode));
        update.setMajor(importedOrExisting(d.getMajor(), existing.getMajor(), importMode));
        update.setClos(importedOrExistingList(d.getClos(), existing.getClos(), importMode));
        update.setTopics(importedOrExistingList(d.getTopics(), existing.getTopics(), importMode));
        update.setAssessments(importedOrExistingList(d.getAssessments(), existing.getAssessments(), importMode));
        SyllabusResponse response = syllabusService.update(syllabusId, update);
        applyImportedMappings(syllabusId, d, issues);

        if ("REPLACE_ALL".equals(importMode) || !safe(d.getReadingList()).isEmpty()) {
            syllabusBookRepository.deleteAll(syllabusBookRepository.findBySyllabus_Id(syllabusId));
        }
        int idx = 1;
        for (SyllabusImportData.ReadingItem item : safe(d.getReadingList())) {
            Book book = bookRepository.save(Book.builder().title(item.getTitle()).author(item.getAuthor())
                    .publisher(item.getPublisher()).year(item.getYear()).edition(item.getEdition())
                    .isbn(item.getIsbn()).url(item.getUrl()).bookType(enumOr(BookType.class, item.getBookType(), BookType.REFERENCE)).build());
            syllabusBookRepository.save(SyllabusBook.builder().id(new SyllabusBookId(syllabusId, book.getId()))
                    .syllabus(syllabus).book(book).usageType(enumOr(UsageType.class, item.getUsageType(), UsageType.RECOMMENDED))
                    .orderIndex(item.getOrderIndex() == null ? idx++ : item.getOrderIndex()).build());
        }
        return response;
    }

    private Syllabus editable(Integer id) {
        Syllabus s = syllabusRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Syllabus not found"));
        accessService.assertCanModify(s);
        if (s.getStatus() != SyllabusStatus.DRAFT) {
            throw new IllegalStateException("Import is only allowed for a DRAFT syllabus. Clone an immutable version first.");
        }
        return s;
    }

    private static String normalizeImportMode(String value) {
        String mode = value == null ? "MERGE" : value.trim().toUpperCase(Locale.ROOT);
        return Set.of("MERGE", "REPLACE_ALL", "UPDATE_DETECTED_FIELDS_ONLY").contains(mode)
                ? mode : "MERGE";
    }

    private static String importedOrExisting(String imported, String existing, String mode) {
        if ("REPLACE_ALL".equals(mode)) return imported;
        return blank(imported) ? existing : imported;
    }

    private static <T> List<T> importedOrExistingList(List<T> imported, List<T> existing, String mode) {
        if ("REPLACE_ALL".equals(mode)) return imported;
        if (imported == null || imported.isEmpty()) return null;
        // The section-level update replaces only a detected section; absent
        // sections return null above and remain untouched by the service.
        return imported;
    }

    private SyllabusImportData parseExcel(InputStream in, List<SyllabusImportIssue> issues) throws Exception {
        try (Workbook wb = WorkbookFactory.create(in)) {
            SyllabusImportData d = SyllabusImportData.builder().build();
            readGeneral(sheet(wb,"General Info","General","Thông tin chung"), d, issues);
            readClos(sheet(wb,"CLO","CLOs"), d, issues);
            readTopics(sheet(wb,"Topics","Topic","Nội dung"), d, issues);
            readAssessments(sheet(wb,"Assessments","Assessment","Đánh giá"), d, issues);
            readBooks(sheet(wb,"Reading List","Books","Tài liệu"), d, issues);
            return d;
        }
    }

    private SyllabusImportData parseWord(InputStream in, List<SyllabusImportIssue> issues) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(in)) {
            SyllabusImportData d = SyllabusImportData.builder().build();
            StringBuilder fallbackText = new StringBuilder();
            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                if (!paragraph.getText().isBlank()) fallbackText.append(paragraph.getText()).append('\n');
            }
            for (XWPFTable t : doc.getTables()) {
                if (t.getRows().isEmpty()) continue;
                String marker = normalize(text(t.getRow(0),0));
                if (marker.contains("general info") || marker.contains("thông tin chung")) readGeneral(rows(t,1), d, issues);
                else if (marker.equals("clo") || marker.contains("learning outcome")) readClos(rows(t,1), d, issues);
                else if (marker.contains("topic") || marker.contains("nội dung")) readTopics(rows(t,1), d, issues);
                else if (marker.contains("assessment") || marker.contains("đánh giá")) readAssessments(rows(t,1), d, issues);
                else if (marker.contains("reading") || marker.contains("tài liệu")) readBooks(rows(t,1), d, issues);
                for (XWPFTableRow row : t.getRows()) {
                    for (var cell : row.getTableCells()) fallbackText.append(cell.getText()).append(' ');
                    fallbackText.append('\n');
                }
            }
            if (d.getClos().isEmpty() && d.getTopics().isEmpty() && d.getAssessments().isEmpty()) {
                SyllabusImportData generic = parsePdfText(fallbackText.toString(), new ArrayList<>());
                mergeMissing(d, generic);
            }
            return d;
        }
    }

    static SyllabusImportData parsePdf(InputStream in, List<SyllabusImportIssue> issues) throws Exception {
        byte[] bytes = in.readAllBytes();
        try (var document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            if (text == null || text.isBlank()) {
                err(issues, "File", null, "content", "The PDF contains no extractable text. It may be a scanned/image-only document.");
                return SyllabusImportData.builder().build();
            }
            return parsePdfText(text, issues);
        }
    }

    static SyllabusImportData parsePdfText(String text, List<SyllabusImportIssue> issues) {
        SyllabusImportData d = SyllabusImportData.builder().build();
        List<String> lines = Arrays.stream(text.replace("\r", "").split("\n"))
                .map(String::trim).filter(x -> !x.isBlank()).toList();
        String joined = String.join("\n", lines);

        d.setSourceCourseName(valueAfterInlineLabel(lines, "Course Name:"));
        d.setSourceCourseCode(valueAfterInlineLabel(lines, "Course Code:"));
        d.setCourseDesignation(valueAfterLabel(lines, "Course designation"));
        d.setSemester(valueAfterLabel(lines, "Semester(s) in which the course is taught"));
        d.setLanguage(valueAfterLabel(lines, "Language"));
        d.setRelation(valueAfterLabel(lines, "Relation to curriculum"));
        d.setTeachingMethods(valueAfterLabel(lines, "Teaching methods"));
        d.setPrerequisites(valueAfterLabel(lines, "Required and recommended prerequisites for joining the course"));
        d.setObjectives(blockAfterHeading(lines, "Course objectives", "Course learning outcomes"));
        d.setExamForms(valueAfterLabel(lines, "Examination forms"));
        d.setExamRequirements(blockAfterHeading(lines, "Study and examination requirements", "Reading list"));

        Matcher workloadTotal = Pattern.compile("Total workload:\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE).matcher(joined);
        if (workloadTotal.find()) d.setWorkloadTotal(workloadTotal.group(1).trim());
        Matcher contact = Pattern.compile("Contact hours:\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE).matcher(joined);
        if (contact.find()) d.setWorkloadContact(contact.group(1).trim());
        Matcher privateStudy = Pattern.compile("Private study[^:]*:\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE).matcher(joined);
        if (privateStudy.find()) d.setWorkloadPrivate(privateStudy.group(1).trim());

        parseClos(lines, d, issues);
        parseContentTopics(lines, d, issues);
        parseLearningActivities(lines, d, issues);
        parseAssessmentPlan(lines, d, issues);
        parseReadingList(lines, d, issues);
        parseLearningOutcomeMatrix(lines, d, issues);
        return d;
    }

    static void parseClos(List<String> lines, SyllabusImportData d, List<SyllabusImportIssue> issues) {
        int start = indexOfStartsWith(lines, "Course learning outcomes");
        if (start < 0) { warn(issues, "CLO", null, "section", "Course learning outcomes section was not found."); return; }
        int end = firstIndexAfter(lines, start + 1, "Competency level", "Content");
        Pattern p = Pattern.compile("^CLO\\s*(\\d+)\\.\\s*(.+)$", Pattern.CASE_INSENSITIVE);
        String first = lines.get(start);
        int marker = normalize(first).indexOf("course learning outcomes");
        if (marker >= 0) {
            String remainder = first.substring(Math.min(first.length(), marker + "course learning outcomes".length())).trim();
            Matcher m = p.matcher(remainder);
            if (m.find()) addPdfClo(d, m.group(1), m.group(2), p, lines, start, end);
        }
        for (int i = start + 1; i < end; i++) {
            Matcher m = p.matcher(lines.get(i));
            if (m.find()) addPdfClo(d, m.group(1), m.group(2), p, lines, i, end);
        }
        if (d.getClos().isEmpty()) warn(issues, "CLO", null, "data", "No CLO records were detected automatically.");
    }

    static void addPdfClo(SyllabusImportData d, String number, String initial, Pattern p, List<String> lines, int lineIndex, int end) {
        String description = initial.trim();
        int i = lineIndex;
        while (i + 1 < end && !p.matcher(lines.get(i + 1)).find() && !looksLikeHeading(lines.get(i + 1))) {
            String next = lines.get(i + 1);
            if (next.matches("^(Competency level|Knowledge|Skill|Attitude|Content).*$")) break;
            description += " " + next;
            i++;
        }
        d.getClos().add(new CreateSyllabusRequest.CloDTO("CLO" + number, description, null, null, null, d.getClos().size() + 1));
    }

    static void parseContentTopics(List<String> lines, SyllabusImportData d, List<SyllabusImportIssue> issues) {
        int start = indexOf(lines, "Topic Weight Level");
        if (start < 0) { start = indexOfContains(lines, "Topic", "Weight", "Level"); }
        if (start < 0) { warn(issues, "Topics", null, "section", "Content topic table was not detected."); return; }
        int end = firstIndexAfter(lines, start + 1, "Examination forms", "Study and examination requirements", "Reading list");
        Pattern p = Pattern.compile("^(.+?)\\s+(\\d+(?:\\.\\d+)?)\\s+([ITU](?:\\s*,\\s*[ITU])*)$", Pattern.CASE_INSENSITIVE);
        int order = 1;
        for (int i = start + 1; i < end; i++) {
            Matcher m = p.matcher(lines.get(i));
            if (!m.find()) continue;
            String name = m.group(1).trim();
            Integer hours = parseInt(m.group(2));
            String levels = m.group(3).trim().toUpperCase().replace(" ", "");
            d.getTopics().add(new CreateSyllabusRequest.TopicDTO(null, order++, name, null, hours, 0, 0, "LECTURE", "Lecture", null, "Content levels: " + levels));
        }
    }

    static void parseLearningActivities(List<String> lines, SyllabusImportData d, List<SyllabusImportIssue> issues) {
        int start = indexOf(lines, "Week Topic CLO Assessments Learning activities Resources");
        if (start < 0) start = indexOfContains(lines, "Week", "Topic", "CLO", "Assessments");
        if (start < 0) {
            warn(issues, "Topics", null, "section", "Planned learning activities table was not detected.");
            return;
        }

        int end = firstIndexAfter(lines, start + 1, "Assessment plan", "Assessment Type", "Final examination");
        Pattern weekPattern = Pattern.compile("^(\\d{1,2})\\s+(.+)$");
        Integer currentWeek = null;
        StringBuilder block = new StringBuilder();

        for (int i = start + 1; i < end; i++) {
            String line = lines.get(i).trim();
            Matcher wm = weekPattern.matcher(line);
            if (wm.find()) {
                if (currentWeek != null) mergeWeekBlock(d, currentWeek, block.toString());
                currentWeek = Integer.valueOf(wm.group(1));
                block.setLength(0);
                block.append(wm.group(2).trim());
                continue;
            }

            // "Midterm" / "Final" are continuation markers in the source PDF,
            // not standalone topics.
            if (currentWeek != null && !line.equalsIgnoreCase("Midterm") && !line.equalsIgnoreCase("Final")) {
                block.append(" ").append(line);
            }
        }

        if (currentWeek != null) mergeWeekBlock(d, currentWeek, block.toString());
    }

    /**
     * Merge one weekly row into the topics already parsed from the Content table.
     * The old implementation used the first whitespace-delimited fragment as the
     * topic name. PDFBox splits wrapped topic names across lines, so that created
     * duplicate topics with null weekNumber. Instead, match the longest existing
     * Content topic contained in the weekly block and update that object in place.
     */
    static void mergeWeekBlock(SyllabusImportData d, int week, String block) {
        if (block == null || block.isBlank()) return;

        CreateSyllabusRequest.TopicDTO topicDto = findBestTopicForWeek(d.getTopics(), block);
        if (topicDto == null) {
            String fallbackName = block.replaceAll("\\s+", " ").trim();
            d.getTopics().add(new CreateSyllabusRequest.TopicDTO(
                    week, 1, fallbackName, null, 0, 0, 0,
                    "LECTURE", "Lecture", extractLearningActivity(block), null));
            return;
        }

        topicDto.setWeekNumber(week);
        if (topicDto.getOrderInWeek() == null || topicDto.getOrderInWeek() <= 0) {
            topicDto.setOrderInWeek(1);
        }
        String activity = extractLearningActivity(block);
        if (activity != null) topicDto.setLearningActivity(activity);

        // The weekly table can contain multiple CLOs, e.g. "2, 3".
        // Preserve all mappings instead of only the first number.
        for (Integer cloNumber : extractCloNumbers(block, topicDto.getName())) {
            String code = "CLO" + cloNumber;
            boolean exists = d.getTopicCloMappings().stream().anyMatch(m ->
                    Objects.equals(m.getWeekNumber(), week)
                            && normalizeCode(m.getTopicName()).equals(normalizeCode(topicDto.getName()))
                            && normalizeCode(m.getCloCode()).equals(normalizeCode(code)));
            if (!exists) {
                d.getTopicCloMappings().add(
                        SyllabusImportData.TopicCloMappingItem.builder()
                                .weekNumber(week)
                                .topicName(topicDto.getName())
                                .cloCode(code)
                                .teachingLevel("D")
                                .build());
            }
        }
    }

    static CreateSyllabusRequest.TopicDTO findBestTopicForWeek(
            List<CreateSyllabusRequest.TopicDTO> topics,
            String block) {
        String normalizedBlock = normalizeTopicText(block);

        Optional<CreateSyllabusRequest.TopicDTO> direct = topics.stream()
                .filter(t -> !blank(t.getName()))
                .filter(t -> normalizedBlock.contains(normalizeTopicText(t.getName())))
                .max(Comparator.comparingInt(t -> normalizeTopicText(t.getName()).length()));
        if (direct.isPresent()) return direct.get();

        Set<String> blockTokens = Arrays.stream(
                        block.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(token -> !token.isBlank())
                .collect(Collectors.toSet());

        return topics.stream()
                .filter(t -> !blank(t.getName()))
                .map(t -> Map.entry(t, topicTokenCoverage(t.getName(), blockTokens)))
                .filter(e -> e.getValue() >= 0.75d)
                .max(Comparator.<Map.Entry<CreateSyllabusRequest.TopicDTO, Double>>comparingDouble(Map.Entry::getValue)
                        .thenComparingInt(e -> normalizeTopicText(e.getKey().getName()).length()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    static double topicTokenCoverage(String topicName, Set<String> blockTokens) {
        List<String> topicTokens = Arrays.stream(
                        topicName.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(token -> !token.isBlank())
                .toList();
        if (topicTokens.isEmpty()) return 0d;
        long matched = topicTokens.stream().filter(blockTokens::contains).count();
        return (double) matched / topicTokens.size();
    }

    static List<Integer> extractCloNumbers(String block, String topicName) {
        if (block == null) return List.of();

        String remainder = block;
        if (!blank(topicName)) {
            remainder = remainder.replaceFirst(
                    "(?i)" + Pattern.quote(topicName), " ");
        }

        // Prefer the first numeric group immediately before the assessment column.
        // If the PDF layout has collapsed the columns, fall back to all CLO-like
        // values occurring after the topic name and before learning activities.
        Matcher matcher = Pattern.compile(
                "(?:^|\\s)([1-9](?:\\s*,\\s*[1-9])+|[1-9])(?:\\s+)(?=(?:Quiz|Lab|Midterm|Final|Assignment|Lecture|Discussion|In-class|$))",
                Pattern.CASE_INSENSITIVE).matcher(remainder);
        if (matcher.find()) return parseCloGroup(matcher.group(1));

        matcher = Pattern.compile("\\b([1-9](?:\\s*,\\s*[1-9])*)\\b").matcher(remainder);
        if (matcher.find()) return parseCloGroup(matcher.group(1));
        return List.of();
    }

    static List<Integer> parseCloGroup(String value) {
        List<Integer> result = new ArrayList<>();
        for (String part : value.split("\\s*,\\s*")) {
            try {
                int n = Integer.parseInt(part.trim());
                if (n >= 1 && n <= 20 && !result.contains(n)) result.add(n);
            } catch (NumberFormatException ignored) { }
        }
        return result;
    }

    static String normalizeTopicText(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "")
                .trim();
    }

    static String extractLearningActivity(String block) {
        for (String value : List.of("Lecture, Discussion, In-class Exercise", "Lecture, Discussion", "Lecture", "Discussion", "In-class Exercise")) {
            if (block.toLowerCase().contains(value.toLowerCase())) return value;
        }
        return null;
    }

    static void parseAssessmentPlan(List<String> lines, SyllabusImportData d, List<SyllabusImportIssue> issues) {
        int start = indexOf(lines, "Assessment Type CLO1 CLO2 CLO3");
        if (start < 0) start = indexOfContains(lines, "Assessment Type", "CLO1");
        if (start < 0) return;
        int end = firstIndexAfter(lines, start + 1, "Note:", "Rubrics");
        Pattern p = Pattern.compile("^(.+?)\\s*\\((\\d+(?:\\.\\d+)?)%\\)\\s+(.+)$");
        for (int i = start + 1; i < end; i++) {
            Matcher m = p.matcher(lines.get(i));
            if (!m.find()) continue;
            String name = m.group(1).trim();
            Float weight = Float.valueOf(m.group(2));
            String[] percentages = m.group(3).trim().split("\\s+");
            CreateSyllabusRequest.AssessmentDTO a = new CreateSyllabusRequest.AssessmentDTO(name, null, inferAssessmentType(name), weight, 0f, 100f, d.getAssessments().size() + 1);
            d.getAssessments().add(a);
            for (int c = 0; c < percentages.length && c < d.getClos().size(); c++) {
                String pct = percentages[c].replace("%", "");
                try { d.getAssessmentCloMappings().add(SyllabusImportData.AssessmentCloMappingItem.builder().assessmentName(name).cloCode(d.getClos().get(c).getCode()).contributionPercent(Float.valueOf(pct)).build()); } catch (NumberFormatException ignored) { }
            }
        }
    }

    static String inferAssessmentType(String name) {
        String n = name.toLowerCase();
        if (n.contains("final")) return "FINAL_EXAM";
        if (n.contains("midterm")) return "MIDTERM_EXAM";
        if (n.contains("quiz")) return "QUIZ";
        if (n.contains("lab")) return "LAB_REPORT";
        if (n.contains("assignment")) return "ASSIGNMENT";
        return "PARTICIPATION";
    }

    static void parseReadingList(List<String> lines, SyllabusImportData d, List<SyllabusImportIssue> issues) {
        int start = indexOfStartsWith(lines, "Reading list");
        if (start < 0) return;
        int end = firstIndexAfter(lines, start + 1, "Learning Outcomes Matrix", "2. Learning Outcomes Matrix");
        Pattern p = Pattern.compile("^\\d+\\.\\s+(.+)$");
        String first = lines.get(start);
        int marker = normalize(first).indexOf("reading list");
        if (marker >= 0) addReadingItem(d, first.substring(Math.min(first.length(), marker + "reading list".length())).trim(), p);
        for (int i = start + 1; i < end; i++) {
            Matcher m = p.matcher(lines.get(i));
            if (m.find()) addReadingItem(d, m.group(1).trim(), p);
        }
    }

    static void addReadingItem(SyllabusImportData d, String raw, Pattern ignored) {
        if (raw.isBlank()) return;
        raw = raw.replaceFirst("^\\d+\\.\\s*", "");
        Matcher year = Pattern.compile("\\b(19|20)\\d{2}\\b").matcher(raw);
        Integer y = year.find() ? Integer.valueOf(year.group()) : null;
        String author = raw.contains(",") ? raw.substring(0, raw.indexOf(',')).trim() : raw;
        String title = raw.contains(",") ? raw.substring(raw.indexOf(',') + 1).trim() : raw;
        d.getReadingList().add(SyllabusImportData.ReadingItem.builder().title(title).author(author).year(y).bookType("REFERENCE").usageType("RECOMMENDED").orderIndex(d.getReadingList().size() + 1).build());
    }

    static void parseLearningOutcomeMatrix(List<String> lines, SyllabusImportData d, List<SyllabusImportIssue> issues) {
        int start = indexOfContains(lines, "CLO\\SLO", "1", "2", "3");
        if (start < 0) start = indexOfContains(lines, "CLO", "SLO", "1", "2", "3");
        if (start < 0) return;
        Pattern row = Pattern.compile("^(\\d+)\\s+(.+)$");
        for (int i = start + 1; i < Math.min(lines.size(), start + 15); i++) {
            Matcher m = row.matcher(lines.get(i));
            if (!m.find()) continue;
            int clo = Integer.parseInt(m.group(1));
            String[] cells = m.group(2).trim().split("\\s+");
            for (int p = 0; p < cells.length; p++) {
                if (cells[p].toLowerCase().contains("x")) {
                    d.getCloPloMappings().add(SyllabusImportData.CloPloMappingItem.builder().cloCode("CLO" + clo).ploCode("PLO" + (p + 1)).level("A").contributionWeight(1.0f).build());
                }
            }
        }
    }

    private void applyImportedMappings(Integer syllabusId, SyllabusImportData d, List<SyllabusImportIssue> issues) {
        Map<String, Clo> clos = cloRepository.findBySyllabusId(syllabusId).stream().collect(Collectors.toMap(x -> normalizeCode(x.getCode()), x -> x, (a,b)->a));
        Map<String, Topic> topics = topicRepository.findBySyllabusId(syllabusId).stream().collect(Collectors.toMap(x -> topicKey(x.getWeekNumber(), x.getName()), x -> x, (a,b)->a));
        Map<String, AssessmentComponent> assessments = assessmentComponentRepository.findBySyllabusId(syllabusId).stream().collect(Collectors.toMap(x -> normalizeCode(x.getName()), x -> x, (a,b)->a));

        for (var m : safe(d.getCloPloMappings())) {
            Clo clo = clos.get(normalizeCode(m.getCloCode()));
            Plo plo = findPloByCode(m.getPloCode());
            if (clo == null || plo == null) { warn(issues, "CLO-PLO Mapping", null, "mapping", "Unable to match " + m.getCloCode() + " -> " + m.getPloCode() + "."); continue; }
            cloPloMappingRepository.save(CloPloMapping.builder().clo(clo).plo(plo).level(enumOr(ContributionLevel.class, m.getLevel(), ContributionLevel.A)).contributionWeight(m.getContributionWeight() == null ? 1.0f : m.getContributionWeight()).notes(m.getNotes()).build());
        }
        for (var m : safe(d.getTopicCloMappings())) {
            Topic topic = topics.get(topicKey(m.getWeekNumber(), m.getTopicName()));
            Clo clo = clos.get(normalizeCode(m.getCloCode()));
            if (topic == null || clo == null) { warn(issues, "Topic-CLO Mapping", null, "mapping", "Unable to match topic/CLO mapping for " + m.getTopicName() + " / " + m.getCloCode() + "."); continue; }
            TopicCloId id = new TopicCloId(topic.getId(), clo.getId());
            topicCloRepository.save(TopicClo.builder().id(id).topic(topic).clo(clo).teachingLevel(enumOr(TeachingLevel.class, m.getTeachingLevel(), TeachingLevel.D)).build());
        }
        for (var m : safe(d.getAssessmentCloMappings())) {
            AssessmentComponent a = assessments.get(normalizeCode(m.getAssessmentName()));
            Clo clo = clos.get(normalizeCode(m.getCloCode()));
            if (a == null || clo == null) { warn(issues, "Assessment-CLO Mapping", null, "mapping", "Unable to match assessment/CLO mapping for " + m.getAssessmentName() + " / " + m.getCloCode() + "."); continue; }
            assessmentCloRepository.save(AssessmentClo.builder().id(new AssessmentCloId(a.getId(), clo.getId())).assessmentComponent(a).clo(clo).contributionPercent(m.getContributionPercent()).build());
        }
    }

    private Plo findPloByCode(String code) {
        if (blank(code)) return null;
        String normalized = code.trim();
        List<Plo> direct = ploRepository.findAllByCode(normalized);
        if (!direct.isEmpty()) return direct.get(0);
        Matcher digits = Pattern.compile("(\\d+)$").matcher(normalized);
        if (digits.find()) {
            int n = Integer.parseInt(digits.group(1));
            for (String candidate : List.of("PLO" + n, "SLO" + n, String.valueOf(n))) {
                direct = ploRepository.findAllByCode(candidate);
                if (!direct.isEmpty()) return direct.get(0);
            }
        }
        return null;
    }

    static String valueAfterInlineLabel(List<String> lines, String label) {
        String target = normalize(label);
        for (String raw : lines) {
            String line = normalize(raw);
            if (line.startsWith(target)) {
                return raw.substring(Math.min(raw.length(), label.length())).trim();
            }
        }
        return null;
    }

    static String valueAfterLabel(List<String> lines, String label) {
        String n = normalize(label);
        String[] words = n.split("\\s+");
        for (int i = 0; i < lines.size(); i++) {
            String line = normalize(lines.get(i));
            if (line.equals(n) && i + 1 < lines.size()) return lines.get(i + 1).trim();
            if (line.startsWith(n + " ")) return lines.get(i).substring(Math.min(lines.get(i).length(), label.length())).trim();
            if (words.length >= 2 && line.equals(words[0] + " " + words[1])) {
                // PDF layout may split a long label across several rows. Find the
                // last row of the label and use the following row as its value.
                for (int j = i + 1; j < Math.min(lines.size(), i + 6); j++) {
                    String candidate = normalize(lines.get(j));
                    if (candidate.contains(words[words.length - 1])) {
                        return j + 1 < lines.size() ? lines.get(j + 1).trim() : null;
                    }
                }
            }
        }
        return null;
    }

    static String blockAfterHeading(List<String> lines, String heading, String endHeading) {
        int start = indexOf(lines, heading);
        if (start < 0) return null;
        int end = indexOf(lines, endHeading, start + 1);
        if (end < 0) end = Math.min(lines.size(), start + 8);
        return String.join(" ", lines.subList(start + 1, end)).trim();
    }

    static int indexOfStartsWith(List<String> lines, String value) {
        String n = normalize(value);
        for (int i = 0; i < lines.size(); i++) {
            if (normalize(lines.get(i)).startsWith(n)) return i;
        }
        return -1;
    }

    static int indexOf(List<String> lines, String value) { return indexOf(lines, value, 0); }
    static int indexOf(List<String> lines, String value, int from) { String n = normalize(value); for (int i = from; i < lines.size(); i++) if (normalize(lines.get(i)).equals(n)) return i; return -1; }
    static int indexOfContains(List<String> lines, String... values) { outer: for (int i=0;i<lines.size();i++){String n=normalize(lines.get(i));for(String v:values)if(!n.contains(normalize(v)))continue outer;return i;}return -1; }
    static int firstIndexAfter(List<String> lines, int from, String... headings) { int best=lines.size(); for(String h:headings){int x=indexOfContains(lines,h);if(x>=from&&x<best)best=x;}return best; }
    static boolean looksLikeHeading(String s){String n=normalize(s);return n.matches("^(content|reading list|examination forms|assessment plan|rubrics.*|[0-9]+\\..*)$");}
    static Integer parseInt(String s){try{return (int)Double.parseDouble(s.replace(",","."));}catch(Exception e){return null;}}
    static String topicKey(Integer week,String name){return (week == null ? "" : week)+"|"+normalizeCode(name);}
    static String normalizeCode(String s){return s == null ? "" : s.trim().toUpperCase().replaceAll("\\s+", "");}

    private void readGeneral(Sheet s, SyllabusImportData d, List<SyllabusImportIssue> issues){ if(s==null){warn(issues,"General Info",null,"sheet","General Info sheet not found.");return;} readGeneral(rows(s),d,issues); }
    private void readGeneral(List<List<String>> rows, SyllabusImportData d, List<SyllabusImportIssue> issues){
        for(List<String> r:rows){ if(r.size()<2)continue; String k=normalize(r.get(0)),v=r.get(1).trim(); switch(k){
            case "course designation"->d.setCourseDesignation(v); case "course types"->d.setCourseTypes(v); case "semester"->d.setSemester(v);
            case "language"->d.setLanguage(v); case "relation"->d.setRelation(v); case "teaching methods"->d.setTeachingMethods(v);
            case "workload total"->d.setWorkloadTotal(v); case "workload contact"->d.setWorkloadContact(v); case "workload private"->d.setWorkloadPrivate(v);
            case "prerequisites"->d.setPrerequisites(v); case "objectives"->d.setObjectives(v); case "exam forms"->d.setExamForms(v);
            case "exam requirements"->d.setExamRequirements(v); case "rubrics"->d.setRubrics(v); case "major"->d.setMajor(v); default->{} }
        }
    }
    private void readClos(Sheet s,SyllabusImportData d,List<SyllabusImportIssue> i){ if(s==null){warn(i,"CLO",null,"sheet","CLO sheet not found.");return;} readClos(rows(s),d,i); }
    private void readClos(List<List<String>> r,SyllabusImportData d,List<SyllabusImportIssue> issues){ for(int x=1;x<r.size();x++){var a=r.get(x);if(empty(a,0))continue;try{d.getClos().add(new CreateSyllabusRequest.CloDTO(val(a,0),val(a,1),val(a,2),upper(val(a,3)),upper(val(a,4)),integer(val(a,5))));}catch(Exception e){err(issues,"CLO",x+1,"row",e.getMessage());}} }
    private void readTopics(Sheet s,SyllabusImportData d,List<SyllabusImportIssue> i){ if(s==null){warn(i,"Topics",null,"sheet","Topics sheet not found.");return;} readTopics(rows(s),d,i); }
    private void readTopics(List<List<String>> r,SyllabusImportData d,List<SyllabusImportIssue> issues){ for(int x=1;x<r.size();x++){var a=r.get(x);if(empty(a,2))continue;try{d.getTopics().add(new CreateSyllabusRequest.TopicDTO(integer(val(a,0)),integer(val(a,1)),val(a,2),val(a,3),integer(val(a,4)),integer(val(a,5)),integer(val(a,6)),upper(val(a,7)),val(a,8),val(a,9),val(a,10)));}catch(Exception e){err(issues,"Topics",x+1,"row",e.getMessage());}} }
    private void readAssessments(Sheet s,SyllabusImportData d,List<SyllabusImportIssue> i){ if(s==null){warn(i,"Assessments",null,"sheet","Assessments sheet not found.");return;} readAssessments(rows(s),d,i); }
    private void readAssessments(List<List<String>> r,SyllabusImportData d,List<SyllabusImportIssue> issues){ for(int x=1;x<r.size();x++){var a=r.get(x);if(empty(a,0))continue;try{d.getAssessments().add(new CreateSyllabusRequest.AssessmentDTO(val(a,0),val(a,1),upper(val(a,2)),decimal(val(a,3)),decimal(val(a,4)),decimal(val(a,5)),integer(val(a,6))));}catch(Exception e){err(issues,"Assessments",x+1,"row",e.getMessage());}} }
    private void readBooks(Sheet s,SyllabusImportData d,List<SyllabusImportIssue> i){ if(s==null){warn(i,"Reading List",null,"sheet","Reading List sheet not found.");return;} readBooks(rows(s),d,i); }
    private void readBooks(List<List<String>> r,SyllabusImportData d,List<SyllabusImportIssue> issues){ for(int x=1;x<r.size();x++){var a=r.get(x);if(empty(a,0))continue;try{d.getReadingList().add(SyllabusImportData.ReadingItem.builder().title(val(a,0)).author(val(a,1)).publisher(val(a,2)).year(integer(val(a,3))).edition(val(a,4)).isbn(val(a,5)).url(val(a,6)).bookType(upper(val(a,7))).usageType(upper(val(a,8))).orderIndex(integer(val(a,9))).build());}catch(Exception e){err(issues,"Reading List",x+1,"row",e.getMessage());}} }

    private void mergeMissing(SyllabusImportData target, SyllabusImportData source) {
        if (blank(target.getCourseDesignation())) target.setCourseDesignation(source.getCourseDesignation());
        if (blank(target.getSemester())) target.setSemester(source.getSemester());
        if (blank(target.getLanguage())) target.setLanguage(source.getLanguage());
        if (blank(target.getRelation())) target.setRelation(source.getRelation());
        if (blank(target.getTeachingMethods())) target.setTeachingMethods(source.getTeachingMethods());
        if (blank(target.getWorkloadTotal())) target.setWorkloadTotal(source.getWorkloadTotal());
        if (blank(target.getWorkloadContact())) target.setWorkloadContact(source.getWorkloadContact());
        if (blank(target.getWorkloadPrivate())) target.setWorkloadPrivate(source.getWorkloadPrivate());
        if (blank(target.getPrerequisites())) target.setPrerequisites(source.getPrerequisites());
        if (blank(target.getObjectives())) target.setObjectives(source.getObjectives());
        if (blank(target.getExamForms())) target.setExamForms(source.getExamForms());
        if (blank(target.getExamRequirements())) target.setExamRequirements(source.getExamRequirements());
        if (target.getClos().isEmpty()) target.setClos(source.getClos());
        if (target.getTopics().isEmpty()) target.setTopics(source.getTopics());
        if (target.getAssessments().isEmpty()) target.setAssessments(source.getAssessments());
        if (target.getReadingList().isEmpty()) target.setReadingList(source.getReadingList());
        if (target.getCloPloMappings().isEmpty()) target.setCloPloMappings(source.getCloPloMappings());
        if (target.getTopicCloMappings().isEmpty()) target.setTopicCloMappings(source.getTopicCloMappings());
        if (target.getAssessmentCloMappings().isEmpty()) target.setAssessmentCloMappings(source.getAssessmentCloMappings());
    }

    private void validate(SyllabusImportData d,List<SyllabusImportIssue> issues){
        if(d==null){err(issues,"File",null,"data","No data is available for import.");return;}
        Set<String> codes=new HashSet<>(); int row=2;
        for(var c:safe(d.getClos())){ if(blank(c.getCode()))err(issues,"CLO",row,"code","CLO code is required."); else if(!codes.add(c.getCode().trim().toUpperCase()))err(issues,"CLO",row,"code","Duplicate CLO code: "+c.getCode()); if(blank(c.getDescription()))warn(issues,"CLO",row,"description","CLO description is missing."); enumCheck(CompetencyLevel.class,c.getCompetencyLevel(),"CLO",row,"competencyLevel",issues); enumCheck(BloomLevel.class,c.getBloomLevel(),"CLO",row,"bloomLevel",issues);row++; }
        float total=0;row=2;for(var a:safe(d.getAssessments())){if(blank(a.getName()))err(issues,"Assessments",row,"name","Assessment component name is required.");if(a.getWeightPercent()==null)err(issues,"Assessments",row,"weightPercent","Weight is required.");else total+=a.getWeightPercent();enumCheck(AssessmentType.class,a.getAssessmentType(),"Assessments",row,"assessmentType",issues);row++;} if(!d.getAssessments().isEmpty()&&Math.abs(total-100)>0.01)warn(issues,"Assessments",null,"weightPercent","The current total weight is "+total+"%; it should equal 100%.");
        row=2;for(var t:safe(d.getTopics())){if(t.getWeekNumber()==null)err(issues,"Topics",row,"weekNumber","Week number is required.");if(blank(t.getName()))err(issues,"Topics",row,"name","Topic name is required.");enumCheck(TopicType.class,t.getTopicType(),"Topics",row,"topicType",issues);row++;}
        row=2;for(var x:safe(d.getReadingList())){if(blank(x.getTitle()))err(issues,"Reading List",row,"title","Resource title is required.");enumCheck(BookType.class,x.getBookType(),"Reading List",row,"bookType",issues);enumCheck(UsageType.class,x.getUsageType(),"Reading List",row,"usageType",issues);row++;}
    }

    private Sheet sheet(Workbook w,String...names){for(String n:names){Sheet s=w.getSheet(n);if(s!=null)return s;}for(Sheet s:w)for(String n:names)if(normalize(s.getSheetName()).equals(normalize(n)))return s;return null;}
    private List<List<String>> rows(Sheet s){List<List<String>> out=new ArrayList<>();DataFormatter f=new DataFormatter();for(Row r:s){List<String>a=new ArrayList<>();for(int i=0;i<Math.max(r.getLastCellNum(),0);i++)a.add(f.formatCellValue(r.getCell(i,Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)).trim());out.add(a);}return out;}
    private List<List<String>> rows(XWPFTable t,int start){List<List<String>> out=new ArrayList<>();for(int x=start;x<t.getRows().size();x++){XWPFTableRow r=t.getRow(x);List<String>a=new ArrayList<>();r.getTableCells().forEach(c->a.add(c.getText().trim()));out.add(a);}return out;}
    private String text(XWPFTableRow r,int i){return r.getTableCells().size()>i?r.getCell(i).getText():"";}
    private String extension(String n){int p=n.lastIndexOf('.');return p<0?"":n.substring(p+1).toLowerCase();}
    static String normalize(String s){return s==null?"":s.trim().toLowerCase().replace('_',' ').replaceAll("\\s+"," ");}
    private String val(List<String>a,int i){return i<a.size()?a.get(i).trim():"";} private boolean empty(List<String>a,int i){return blank(val(a,i));}
    private Integer integer(String s){if(blank(s))return null;return (int)Double.parseDouble(s.replace(',','.'));} private Float decimal(String s){if(blank(s))return null;return Float.parseFloat(s.replace(',','.'));}
    private String upper(String s){return blank(s)?null:s.trim().toUpperCase().replace(' ','_').replace('-','_');}
    static boolean blank(String s){return s==null||s.trim().isEmpty();}
    private <T> List<T> safe(List<T> x){return x==null?List.of():x;}
    private <E extends Enum<E>> E enumOr(Class<E> c,String v,E def){try{return blank(v)?def:Enum.valueOf(c,upper(v));}catch(Exception e){return def;}}
    private <E extends Enum<E>> void enumCheck(Class<E> c,String v,String sec,Integer row,String field,List<SyllabusImportIssue> out){if(blank(v))return;try{Enum.valueOf(c,upper(v));}catch(Exception e){err(out,sec,row,field,"Invalid value: "+v+". Allowed values: "+Arrays.toString(c.getEnumConstants()));}}
    private static void err(List<SyllabusImportIssue>o,String s,Integer r,String f,String m){o.add(SyllabusImportIssue.builder().severity("ERROR").section(s).row(r).field(f).message(m).build());}
    private static void warn(List<SyllabusImportIssue>o,String s,Integer r,String f,String m){o.add(SyllabusImportIssue.builder().severity("WARNING").section(s).row(r).field(f).message(m).build());}
}
