package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class Cohort2027DsSourceRecoveryTest {
    @Test void filledPolygonRulesRemainOptInAndRecoverIndependentContentWithoutInventingWeeklyMappings() throws Exception {
        try(var document=new PDDocument()) {
            var page=new PDPage();document.addPage(page);
            try(var stream=new PDPageContentStream(document,page)) {
                for(float y:new float[]{700,660,620,580,540})polygon(stream,50,y,500,0.48f);
                for(float x:new float[]{50,550})for(float y=540;y<700;y+=10)polygon(stream,x,y,0.48f,10);
                for(float x:new float[]{350,420})for(float y=580;y<700;y+=10)polygon(stream,x,y,0.48f,10);
                text(stream,55,680,"Topic");text(stream,355,680,"Weight");text(stream,425,680,"Level");
                text(stream,55,640,"First documented content");text(stream,355,640,"3");text(stream,425,640,"I,T");
                text(stream,55,600,"Second documented content");text(stream,355,600,"2");text(stream,425,600,"T,U");
                text(stream,55,560,"MIDTERM EXAM WEEK OF THE UNIVERSITY");
            }
            assertTrue(Cohort2027PdfTableRecovery.cells(page).isEmpty(),"Legacy cell interpretation changed");
            var data=new SyllabusImportData();
            assertTrue(Cohort2027PdfTableRecovery.recover(document,1,1,data,true).isEmpty());
            assertEquals(List.of("First documented content","Second documented content","MIDTERM EXAM WEEK OF THE UNIVERSITY"),
                    data.getTopics().stream().map(topic->topic.getName()).toList());
            assertTrue(data.getTopics().stream().allMatch(topic->topic.getWeekNumber()==null&&topic.getTeachingHours()==null));
            assertTrue(data.getTopicCloMappings().isEmpty());
            assertEquals(SyllabusImportData.SourceFieldState.PARSED,data.getSourceProvenance().get("content").get("sourceRows").getState());
        }
    }
    @Test void reviewedMathSourceUsesOnlyTheDsPdfAndKeepsItsDistinctThreeOutcomesAndBlankWeights() throws Exception {
        var data=new SyllabusImportData();data.setSourceCourseCode("MA001IU");
        assertTrue(Cohort2027VerifiedSourceRecovery.applyReviewed(Cohort2027DsSourceRecovery.SOURCE_SHA256,156,158,data));
        Cohort2027ImportValidation.validate(data);
        assertEquals(3,data.getClos().size());assertEquals(17,data.getTopics().size());
        assertTrue(data.getClos().get(0).getDescription().contains("Data Science Process"));
        assertTrue(data.getTopics().stream().allMatch(topic->topic.getContentWeight()==null&&topic.getWeekNumber()==null
                &&topic.getTeachingHours()==null&&topic.getContentLevel()==null));
        assertTrue(data.getCloPloMappings().isEmpty());
    }
    @Test void reviewedEvidenceCannotCertifyAnotherSourceRangeOrEditedMathDescription() throws Exception {
        var data=new SyllabusImportData();data.setSourceCourseCode("MA001IU");
        assertFalse(Cohort2027DsSourceRecovery.supports(Cohort2027ReviewedSourceRecoveryTest.HASH));
        assertFalse(Cohort2027DsSourceRecovery.supports(null));
        assertFalse(Cohort2027VerifiedSourceRecovery.applyReviewed(Cohort2027DsSourceRecovery.SOURCE_SHA256,162,167,data));
        assertFalse(Cohort2027VerifiedSourceRecovery.applyReviewed(Cohort2027ReviewedSourceRecoveryTest.HASH,156,158,data));
        assertTrue(Cohort2027VerifiedSourceRecovery.applyReviewed(Cohort2027DsSourceRecovery.SOURCE_SHA256,156,158,data));
        var notes=Cohort2027VerifiedSourceRecovery.reviewedNotes(data);
        assertFalse(notes.isEmpty());assertTrue(data.getCloPloMappings().isEmpty());
        data.getClos().get(0).setDescription("Unverified edited outcome");
        assertThrows(IllegalArgumentException.class,()->Cohort2027VerifiedSourceRecovery.reviewedNotes(data));
    }
    @Test void unverifiedWeeklyReferenceStaysRawAndCannotCreateANewOutcomeOrTopicRelationship() {
        var data=new SyllabusImportData();
        data.setClos(new ArrayList<>(List.of(com.scse.curriculum.syllabus.importer.dto.CloImportData.builder()
                .code("CLO2").description("Actual source outcome").build())));
        data.setWeeklyActivities(new ArrayList<>(List.of(SyllabusImportData.WeeklyActivityItem.builder()
                .week(2).topic("Actual source topic").clo("2, 5").build())));
        var issues=new ArrayList<com.scse.curriculum.syllabus.importer.dto.SyllabusImportIssue>();
        Cohort2027DsSourceRecovery.warnOnUnverifiedWeeklyReferences(data,issues);
        assertEquals("2, 5",data.getWeeklyActivities().get(0).getClo());assertEquals(1,data.getClos().size());
        assertTrue(data.getTopicCloMappings().isEmpty());assertEquals("WARNING",issues.get(0).getSeverity());
        assertTrue(issues.get(0).getMessage().contains("CLO5"));
    }
    @Test void sourceRubricTablesPreserveOriginalScoreTextWithoutTreatingItAsAchievementLevels() throws Exception {
        var data=new SyllabusImportData();data.setSourceCourseCode("IT144IU");
        data.setRubricItems(new ArrayList<>(List.of(SyllabusImportData.RubricItem.builder().title("Preset rubric").build())));
        Cohort2027VerifiedSourceRecovery.applySourceTables(Cohort2027DsSourceRecovery.SOURCE_SHA256,408,416,data);
        assertTrue(data.getRubricItems().isEmpty());
        var notes=Cohort2027VerifiedSourceRecovery.vectorNotes(data);
        assertTrue(notes.containsKey("11"));assertTrue(notes.get("11").toString().contains("Technical content"));
        assertTrue(notes.get("11").toString().contains("DS2026_RUBRIC_GRID_V1"));
    }
    private static void polygon(PDPageContentStream stream,float x,float y,float width,float height)throws Exception {
        stream.moveTo(x,y);stream.lineTo(x+width,y);stream.lineTo(x+width,y+height);stream.lineTo(x,y+height);stream.closePath();stream.fill();
    }
    private static void text(PDPageContentStream stream,float x,float y,String text)throws Exception {
        stream.beginText();stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),10);
        stream.newLineAtOffset(x,y);stream.showText(text);stream.endText();
    }
}
