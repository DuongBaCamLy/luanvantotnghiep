package com.scse.curriculum.syllabus.pdf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scse.curriculum.syllabus.importer.parser.SyllabusPdfParser;
import com.scse.curriculum.syllabus.importer.parser.SyllabusDocxParser;
import com.scse.curriculum.syllabus.importer.service.SyllabusImportMapper;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.syllabus.service.SyllabusAccessService;
import com.scse.curriculum.syllabus.word.SyllabusWordRenderer;
import com.scse.curriculum.topic.entity.Topic;
import com.scse.curriculum.topic.repository.TopicRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SyllabusExportRegressionTest {
    @Test
    void independentLongContentSurvivesBothExportsWithBlankWeekAndHours() throws Exception {
        String longName = "Verified source content about mathematical methods and applications ".repeat(8).trim();
        String notes = new ObjectMapper().writeValueAsString(Map.of("sourceContentRows", List.of(
                Map.of("name", "First independent source row", "weight", "1", "level", "I,T", "relatedCloCodes", List.of()),
                Map.of("name", longName, "weight", "2", "level", "T,U", "relatedCloCodes", List.of()))));
        var data = replace(new SyllabusPdfRendererTest().sampleDocument(),
                Map.of("targetTemplateProfile", "NEW_2027", "notes", notes, "topics", List.of()));
        try (var doc = new XWPFDocument(new ByteArrayInputStream(word.render(data)))) {
            var content = doc.getTables().stream().filter(t -> t.getRow(0).getCell(0).getText().equals("Topic — Vietnamese")
                    || (t.getRow(0).getTableCells().size() == 7 && t.getRow(0).getCell(1).getText().contains("Topic"))).findFirst().orElseThrow();
            assertEquals(3, content.getNumberOfRows());
            assertEquals("First independent source row", content.getRow(1).getCell(1).getText());
            assertEquals(longName, content.getRow(2).getCell(1).getText());
            for (int i = 1; i <= 2; i++) {
                assertEquals("", content.getRow(i).getCell(5).getText());
                assertEquals("", content.getRow(i).getCell(6).getText());
            }
        }
        try (var doc = Loader.loadPDF(pdf.render(data, SyllabusPdfMode.EXPORT))) {
            String text = new PDFTextStripper().getText(doc).replaceAll("\\s+", " ");
            assertTrue(text.contains(longName));
            assertTrue(text.indexOf("First independent source row") < text.indexOf(longName));
        }
    }
    private final SyllabusPdfRenderer pdf = new SyllabusPdfRenderer(new SyllabusPdfFontProvider("", ""),
            new ObjectMapper(), "INTERNATIONAL UNIVERSITY", "SCHOOL OF COMPUTER SCIENCE AND ENGINEERING");
    private final SyllabusWordRenderer word = new SyllabusWordRenderer(pdf);

    @Test
    void dsReadingListExportsEachCompleteReferenceAsOneRow() throws Exception {
        var json = new ObjectMapper();
        try (var input = getClass().getResourceAsStream("/syllabus-import/verified-ds2026-reading-rows.json")) {
            var resource = json.readTree(input);
            var course = java.util.stream.StreamSupport.stream(resource.path("courses").spliterator(), false)
                    .filter(c -> c.path("sourceCode").asText().equals("IT056")).findFirst().orElseThrow();
            var source = json.createObjectNode();source.putObject("sourceImportEvidence")
                    .put("sourceSha256", resource.path("sourceSha256").asText()).put("courseCode", "IT056").put("startPage",437).put("endPage",447);
            source.set("sourceReadingList", course.path("legacyReadingList"));
            String notes = com.scse.curriculum.syllabus.template.Ds2027ReadingNotes.forDisplay(source.toString(), "IT056IU", "NEW_2027");
            var data = replace(new SyllabusPdfRendererTest().sampleDocument(), Map.of(
                    "targetTemplateProfile", "NEW_2027", "notes", notes, "courseCode", "IT056IU", "books", List.of()));
            byte[] pdfBytes = pdf.render(data, SyllabusPdfMode.EXPORT), wordBytes = word.render(data);
            try (var doc = new XWPFDocument(new ByteArrayInputStream(wordBytes))) {
                var list = doc.getTables().stream().filter(t -> t.getRow(0).getCell(1) != null
                        && t.getRow(0).getCell(1).getText().equals("Reference (source text)")).findFirst().orElseThrow();
                assertEquals(4, list.getNumberOfRows());
                for (int i = 0; i < 3; i++) {
                    var expected = course.path("section12").get(0).path("rows").get(i);
                    assertEquals(expected.get(0).asText(), list.getRow(i+1).getCell(0).getText());
                    assertEquals(expected.get(1).asText(), list.getRow(i+1).getCell(1).getText());
                }
            }
            try (var doc = Loader.loadPDF(pdfBytes)) {
                String text = new PDFTextStripper().getText(doc).replaceAll("\\s+", " ");
                for (var row : course.path("section12").get(0).path("rows")) assertTrue(text.contains(row.get(1).asText()));
            }
            String output = System.getProperty("dsReadingExportOutput");
            if (output != null) {Path out = Path.of(output);Files.createDirectories(out);
                Files.write(out.resolve("IT056-reading-export-fixture.pdf"),pdfBytes);
                Files.write(out.resolve("IT056-reading-export-fixture.docx"),wordBytes);}
        }
    }

    @Test
    void dsRubricSourceColumnsMergedCellsAndEmptyScoreFieldsSurviveBothExports() throws Exception {
        var json = new ObjectMapper();
        Object section = com.scse.curriculum.syllabus.template.Ds2027RubricNotes.section(
                "fd279731316e301fce93cd25c9231959b1f73631f62c7d38e883d87bfaba1f32", "IT094", 448, 456);
        String notes = json.writeValueAsString(Map.of("sourceSupplementalSections", Map.of("11", section)));
        var data = replace(new SyllabusPdfRendererTest().sampleDocument(),
                Map.of("targetTemplateProfile", "NEW_2027", "notes", notes, "rubrics", ""));
        byte[] pdfBytes = pdf.render(data, SyllabusPdfMode.EXPORT), wordBytes = word.render(data);
        try (var doc = new XWPFDocument(new ByteArrayInputStream(wordBytes))) {
            var checklist = doc.getTables().stream().filter(t -> t.getRow(0).getCell(0).getText()
                    .equals("Grading checklist for Written Reports")).findFirst().orElseThrow();
            assertEquals("Max.", checklist.getRow(2).getCell(1).getText());
            assertEquals("Score", checklist.getRow(2).getCell(2).getText());
            assertEquals("Comments", checklist.getRow(2).getCell(3).getText());
            assertEquals("10", checklist.getRow(4).getCell(1).getText());
            assertEquals("", checklist.getRow(4).getCell(2).getText());
            assertEquals("", checklist.getRow(4).getCell(3).getText());
            var allText = doc.getTables().stream().flatMap(t -> t.getRows().stream())
                    .flatMap(r -> r.getTableCells().stream()).map(c -> c.getText()).collect(java.util.stream.Collectors.toSet());
            for (var block : json.valueToTree(section).get(0).path("blocks"))
                for (var row : block.path("rows")) for (var cell : row)
                    assertTrue(allText.contains(cell.path("text").asText()), "Word changed a source rubric cell");
        }
        try (var document = Loader.loadPDF(pdfBytes)) {
            String text = new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
            assertTrue(text.contains("Capstone"));assertTrue(text.contains("Milestone"));assertTrue(text.contains("Benchmark"));
            assertFalse(text.contains("Original extracted source text"));assertFalse(text.contains("Nguyen Van Sinh"));
        }
        if (System.getProperty("dsRubricExportOutput") != null) {
            Path out = Path.of(System.getProperty("dsRubricExportOutput"));Files.createDirectories(out);
            Files.write(out.resolve("IT094-rubric-export-fixture.pdf"), pdfBytes);
            Files.write(out.resolve("IT094-rubric-export-fixture.docx"), wordBytes);
        }
    }

    @Test
    void realPdfImportMappingAndExportRetainResourcesByTopic() throws Exception {
        try (var input = getClass().getResourceAsStream("/syllabus-import/CS-IT116.pdf")) {
            assertNotNull(input);
            var batch = new SyllabusPdfParser().parsePdfBatchWithMetadata(input);
            assertFalse(batch.sections().isEmpty());
            var source = batch.sections().stream().map(SyllabusPdfParser.ParsedSyllabusSection::data)
                    .filter(d -> !d.getTopics().isEmpty()).findFirst().orElseThrow();
            assertTrue(source.getTopics().stream().anyMatch(t -> t.getResources() != null && !t.getResources().isBlank()),
                    "The real source must exercise Resources, not only blank cells");
            checkImported(source, "real-pdf");
        }
    }

    @Test
    void realWordTemplateImportMappingAndExportRetainWeeklyColumns() throws Exception {
        try (var input = Files.newInputStream(Path.of("../../templates/syllabus-import-template.docx"))) {
            var source = new SyllabusDocxParser().parse(input, new ArrayList<>());
            assertFalse(source.getTopics().isEmpty());
            checkImported(source, "real-docx");
        }
    }

    private void checkImported(SyllabusImportData source, String name) throws Exception {
        var request = new SyllabusImportMapper().toCreateRequest(source, 1);
        List<Topic> saved = new ArrayList<>();
        for (int i = 0; i < request.getTopics().size(); i++) {
            var dto = request.getTopics().get(i);
            assertEquals(source.getTopics().get(i).getResources(), dto.getResources());
            saved.add(Topic.builder().id(i + 1).weekNumber(dto.getWeekNumber()).orderInWeek(dto.getOrderInWeek())
                    .name(dto.getName()).nameVn(dto.getNameVn()).teachingMethod(dto.getTeachingMethod())
                    .learningActivity(dto.getLearningActivity()).assessments(dto.getAssessments())
                    .resources(dto.getResources()).notes(dto.getNotes()).build());
        }
        var course = mock(com.scse.curriculum.course.entity.Course.class);
        when(course.getCourseCode()).thenReturn(source.getSourceCourseCode());
        when(course.getName()).thenReturn(source.getSourceCourseName());
        var syllabus = Syllabus.builder().id(1).course(course).versionNumber(1)
                .courseDesignation(request.getCourseDesignation()).courseTypes(request.getCourseTypes())
                .language(request.getLanguage()).relation(request.getRelation()).semester(request.getSemester())
                .teachingMethods(request.getTeachingMethods()).workloadTotal(request.getWorkloadTotal())
                .workloadContact(request.getWorkloadContact()).workloadPrivate(request.getWorkloadPrivate())
                .prerequisites(request.getPrerequisites()).objectives(request.getObjectives())
                .examForms(request.getExamForms()).examRequirements(request.getExamRequirements()).build();
        var data = loaderSaved(saved, syllabus).load(1);
        assertEquals(source.getSourceCourseCode(), data.courseCode());
        assertEquals(source.getSourceCourseName(), data.courseName());
        assertEquals(saved.size(), data.topics().size());
        for (int i = 0; i < saved.size(); i++) {
            assertEquals(Objects.requireNonNullElse(saved.get(i).getResources(), ""), data.topics().get(i).resources());
        }
        compare(data, name);
    }

    @Test
    void explicitBlankResourcesOverrideLegacyMetadataAndNeverUseNotes() {
        var saved = List.of(
                Topic.builder().id(1).weekNumber(1).name("A").resources("")
                        .assessments("").notes("{\"schemaVersion\":1}").build(),
                Topic.builder().id(2).weekNumber(2).name("B").build());
        var data = loader(saved, "{\"topicDetails\":{\"0\":{\"resources\":\"stale\"},"
                + "\"1\":{\"resources\":\"[2] Chapter 5\",\"assessments\":\"Quiz\"}}}").load(1);
        assertEquals("", data.topics().get(0).resources());
        assertEquals("", data.topics().get(0).assessments());
        assertEquals("[2] Chapter 5", data.topics().get(1).resources());
        assertEquals("Quiz", data.topics().get(1).assessments());
    }

    @Test
    void longUnicodeAndBlankWeeklyCellsRemainEditableAndRepeatHeaders() throws Exception {
        List<Topic> saved = new ArrayList<>();
        for (int i = 1; i <= 45; i++) saved.add(Topic.builder().id(i).weekNumber(i)
                .name("Tuần " + i + " — Giải quyết vấn đề và phân tích dữ liệu ".repeat(4))
                .learningActivity("Thảo luận\nBài tập thực hành")
                .assessments(i % 2 == 0 ? "" : "Quiz " + i)
                .resources(i % 2 == 0 ? "" : "Tài liệu " + i + ": chương học về dữ liệu và thuật toán ".repeat(3))
                .notes("{\"schemaVersion\":1,\"contentWeight\":99}").build());
        var data = loader(saved, null).load(1);
        compare(data, "unicode-multipage");
        try (var doc = Loader.loadPDF(pdf.render(data, SyllabusPdfMode.PREVIEW))) {
            assertTrue(doc.getNumberOfPages() > 3);
            assertTrue(new PDFTextStripper().getText(doc).contains("PREVIEW"));
        }
    }

    @Test
    void new2027RetainsSameSectionOrderAndImportedActivityCellsInBothFormats() throws Exception {
        var base = new SyllabusPdfRendererTest().sampleDocument();
        var data = replace(base, Map.of("targetTemplateProfile", "NEW_2027", "notes",
                "{\"plannedActivities\":[{\"week\":3,\"topic\":\"Đồ thị\",\"clo\":\"CLO1\","
                        + "\"assessments\":\"Quiz\",\"learningActivities\":\"Thực hành\",\"resources\":\"[2] Chương 7\"}]}"));
        try (var doc = new XWPFDocument(new ByteArrayInputStream(word.render(data)))) {
            String text = doc.getParagraphs().stream().map(p -> p.getText()).reduce("", (a,b) -> a + "\n" + b);
            assertTrue(text.indexOf("1. General Information") < text.indexOf("9. Planned Learning"));
            var activity = doc.getTables().stream().filter(t -> t.getRow(0).getCell(0).getText().equals("Week")).findFirst().orElseThrow();
            assertEquals("[2] Chương 7", activity.getRow(1).getCell(5).getText());
            assertEquals("Quiz", activity.getRow(1).getCell(4).getText());
        }
        try (var doc = Loader.loadPDF(pdf.render(data, SyllabusPdfMode.EXPORT))) {
            assertTrue(new PDFTextStripper().getText(doc).contains("[2] Chương 7"));
        }
    }

    private void compare(SyllabusPdfDocument data, String name) throws Exception {
        byte[] pdfBytes = pdf.render(data, SyllabusPdfMode.EXPORT);
        byte[] wordBytes = word.render(data);
        Path qa = Path.of("target/export-qa"); Files.createDirectories(qa);
        Files.write(qa.resolve(name + ".pdf"), pdfBytes); Files.write(qa.resolve(name + ".docx"), wordBytes);
        try (var pd = Loader.loadPDF(pdfBytes); var doc = new XWPFDocument(new ByteArrayInputStream(wordBytes))) {
            String extracted = new PDFTextStripper().getText(pd).replaceAll("\\s+", " ");
            // Read the Resources column independently; repeated table/page headers can
            // interrupt a cell that legitimately continues on the following PDF page.
            StringBuilder resourceText = new StringBuilder();
            for (var page : pd.getPages()) {
                var stripper = new org.apache.pdfbox.text.PDFTextStripperByArea();
                stripper.setSortByPosition(true);
                float pageWidth = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();
                stripper.addRegion("resources", new java.awt.geom.Rectangle2D.Float(
                        38 + (pageWidth - 76) * .84f, 50, (pageWidth - 76) * .16f, pageHeight - 94));
                stripper.extractRegions(page);
                resourceText.append(stripper.getTextForRegion("resources").replace("Resources", ""));
            }
            String resources = resourceText.toString().replaceAll("\\s+", " ");
            var weekly = doc.getTables().stream().filter(t -> t.getRow(0).getCell(0).getText().equals("Week")).findFirst().orElseThrow();
            assertEquals(6, weekly.getRow(0).getTableCells().size());
            assertEquals(data.topics().size() + 1, weekly.getNumberOfRows());
            assertTrue(weekly.getRow(0).isRepeatHeader());
            assertTrue(doc.getAllPictures().isEmpty(), "Word must use editable text/tables");
            for (var table : doc.getTables()) {
                int gridWidth = Arrays.stream(table.getCTTbl().getTblGrid().getGridColArray())
                        .mapToInt(c -> Integer.parseInt(c.getW().toString())).sum();
                assertEquals(table.getWidth(), gridWidth, "Word table grid must match its declared width");
            }
            for (int i = 0; i < data.topics().size(); i++) {
                var topic = data.topics().get(i); var row = weekly.getRow(i + 1);
                assertEquals(topic.resources().trim(), row.getCell(5).getText());
                assertEquals(topic.assessments().trim(), row.getCell(3).getText());
                if (!topic.resources().isBlank()) assertTrue(resources.contains(topic.resources().trim().replaceAll("\\s+", " ")),
                        "Missing PDF resource for week " + topic.weekNumber());
            }
            assertFalse(extracted.contains("schemaVersion"));
            assertFalse(extracted.contains("contentWeight"));
        }
    }

    @Test
    void wideAlignmentMatrixUsesLandscapeSectionsWithHeadersAndFooter() throws Exception {
        var base = new SyllabusPdfRendererTest().sampleDocument();
        var plos = java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(i -> new SyllabusPdfDocument.PloColumn(i, "PLO" + i, "Outcome " + i)).toList();
        var data = replace(base, Map.of("plos", plos));
        try (var doc = new XWPFDocument(new ByteArrayInputStream(word.render(data)))) {
            List<org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr> sections = new ArrayList<>();
            for (var p : doc.getParagraphs()) if (p.getCTP().isSetPPr() && p.getCTP().getPPr().isSetSectPr())
                sections.add(p.getCTP().getPPr().getSectPr());
            sections.add(doc.getDocument().getBody().getSectPr());
            assertTrue(sections.stream().anyMatch(s ->
                    Integer.parseInt(s.getPgSz().getW().toString()) > Integer.parseInt(s.getPgSz().getH().toString())));
            for (var s : sections) {
                assertEquals(1, s.sizeOfHeaderReferenceArray());
                assertEquals(1, s.sizeOfFooterReferenceArray());
            }
        }
    }

    private SyllabusPdfDataLoader loader(List<Topic> topics, String notes) {
        return loaderSaved(topics, Syllabus.builder().id(1).notes(notes).build());
    }

    private SyllabusPdfDataLoader loaderSaved(List<Topic> topics, Syllabus syllabus) {
        var syllabuses = mock(SyllabusRepository.class);
        when(syllabuses.findByIdForPdf(1)).thenReturn(Optional.of(syllabus));
        var topicRepository = mock(TopicRepository.class);
        when(topicRepository.findBySyllabusIdOrderByWeekNumberAscOrderInWeekAsc(1)).thenReturn(topics);
        return new SyllabusPdfDataLoader(syllabuses, mock(SyllabusAccessService.class),
                mock(com.scse.curriculum.classsection.repository.ClassSectionRepository.class),
                mock(com.scse.curriculum.clo.repository.CloRepository.class),
                mock(com.scse.curriculum.cloplomapping.repository.CloPloMappingRepository.class), topicRepository,
                mock(com.scse.curriculum.topicclo.repository.TopicCloRepository.class),
                mock(com.scse.curriculum.assessment.repository.AssessmentComponentRepository.class),
                mock(com.scse.curriculum.assessment.repository.AssessmentCloRepository.class),
                mock(com.scse.curriculum.syllabusbook.repository.SyllabusBookRepository.class),
                mock(com.scse.curriculum.courseprogram.repository.CourseProgramRepository.class),
                mock(com.scse.curriculum.syllabus.template.TargetTemplateProfileResolver.class));
    }

    private SyllabusPdfDocument replace(SyllabusPdfDocument base, Map<String,Object> values) throws Exception {
        var fields = SyllabusPdfDocument.class.getRecordComponents();
        Object[] args = new Object[fields.length]; Class<?>[] types = new Class<?>[fields.length];
        for (int i = 0; i < fields.length; i++) {
            types[i] = fields[i].getType();
            args[i] = values.containsKey(fields[i].getName()) ? values.get(fields[i].getName()) : fields[i].getAccessor().invoke(base);
        }
        return SyllabusPdfDocument.class.getDeclaredConstructor(types).newInstance(args);
    }
}
