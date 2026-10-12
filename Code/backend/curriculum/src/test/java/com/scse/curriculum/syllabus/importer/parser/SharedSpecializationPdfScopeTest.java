package com.scse.curriculum.syllabus.importer.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SharedSpecializationPdfScopeTest {
    private String codes(String prefix) {
        return String.join("\n",java.util.stream.IntStream.range(100,112).mapToObj(n -> prefix+n+"IU").toList());
    }
    @Test void dividersAndListsAreDerivedFromSourceAndShiftWithInsertedPages() {
        var pages = new ArrayList<>(List.of("8. PROGRAM STRUCTURE\na) Bachelor of Engineering – Network Engineering Major\n"+codes("IT"),
                "b) Bachelor of Engineering – Computer Engineering Major\n"+codes("EE"),"9. CURRICULUM",
                "Bachelor of Engineering in Information Technology\n– Network Engineering Major","NE syllabus",
                "Bachelor of Engineering in Information Technology\n– Computer Engineering Major","CE syllabus"));
        var ne = SharedSpecializationPdfScope.fromPageTexts(pages,"NE-2021");
        var ce = SharedSpecializationPdfScope.fromPageTexts(pages,"CE-2027");
        assertEquals(5,ne.firstPage());assertEquals(5,ne.lastPage());assertEquals(7,ce.firstPage());
        assertTrue(ne.courseCodes().contains("IT100"));assertFalse(ne.courseCodes().contains("EE100"));
        pages.add(0,"New cover");assertEquals(6,SharedSpecializationPdfScope.fromPageTexts(pages,"NE-2030").firstPage());
    }
    @Test void commonAppendixPoolUsesSeparateVietnameseCourseTables() {
        var pages=List.of("Bảng 7: Các môn học thuộc CTĐT - chuyên ngành Kỹ thuật mạng\n"+codes("IT")
                +"\nBảng 8: Các môn học thuộc CTĐT - chuyên ngành Kỹ thuật Máy tính\n"+codes("EE"),
                "Bảng 9: Kế hoạch giảng dạy cho chuyên ngành Kỹ thuật mạng\nEE999IU", "Shared appendices");
        var ne=SharedSpecializationPdfScope.fromPageTexts(pages,"NE");var ce=SharedSpecializationPdfScope.fromPageTexts(pages,"CE");
        assertEquals(1,ne.firstPage());assertEquals(3,ne.lastPage());assertEquals(12,ne.courseCodes().size());
        assertFalse(ne.courseCodes().contains("EE999"));assertTrue(ce.courseCodes().contains("EE100"));
    }
    @Test void missingOrAmbiguousScopeIsBlockedAndOtherProgramsAreNotSelected() {
        assertFalse(SharedSpecializationPdfScope.supports("CS-2021"));assertFalse(SharedSpecializationPdfScope.supports("DS"));
        assertFalse(SharedSpecializationPdfScope.supports("IT-2021"));
        assertThrows(IllegalArgumentException.class,()->SharedSpecializationPdfScope.fromPageTexts(List.of("Unidentified programme"),"NE"));
    }
    @Test void suppliedSourcesHaveDistinctScopesWithoutUsingFixed2021RangesFor2026() throws Exception {
        String sources=System.getProperty("neCeSources");assumeTrue(sources!=null,"Set neCeSources to the source manifest for local PDF regression");
        var json=new ObjectMapper();var paths=json.readTree(Files.readString(Path.of(sources)));var evidence=json.createObjectNode();
        for(String label:List.of("IT2021","IT2026")) {
            byte[] bytes=Files.readAllBytes(Path.of(paths.path(label).asText()));
            var ne=SharedSpecializationPdfScope.extract(bytes,"NE");var ce=SharedSpecializationPdfScope.extract(bytes,"CE");
            assertTrue(ne.courseCodes().size()>40);assertTrue(ce.courseCodes().size()>40);
            assertNotEquals(ne.courseCodes(),ce.courseCodes());
            if(label.equals("IT2021")) {assertEquals(55,ne.firstPage());assertEquals(395,ne.lastPage());assertEquals(397,ce.firstPage());assertEquals(762,ce.lastPage());}
            else {assertEquals(1,ne.firstPage());assertEquals(745,ne.lastPage());assertEquals(1,ce.firstPage());assertEquals(745,ce.lastPage());}
            evidence.set(label,json.valueToTree(Map.of("NE",ne,"CE",ce)));
        }
        String output=System.getProperty("neCeAudit");if(output!=null)json.writerWithDefaultPrettyPrinter().writeValue(Path.of(output,"source-scopes.json").toFile(),evidence);
    }
}
