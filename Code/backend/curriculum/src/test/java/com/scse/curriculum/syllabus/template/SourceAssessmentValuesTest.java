package com.scse.curriculum.syllabus.template;
import com.scse.curriculum.syllabus.dto.CreateSyllabusRequest;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class SourceAssessmentValuesTest {
 @Test void missingSourceScoresAreBlankAndARealUserValueClearsTheMissingFlag() {
  var dto=new CreateSyllabusRequest.AssessmentDTO();dto.setName("Source exam");dto.setOrderIndex(1);
  String unknown=SourceAssessmentValues.annotate("{\"sourceFaithful2027\":true}",List.of(dto));
  assertNull(SourceAssessmentValues.visible(unknown,1,"Source exam","weightPercent",0f));
  assertNull(SourceAssessmentValues.visible(unknown,1,"Source exam","minScore",0f));
  assertNull(SourceAssessmentValues.visible(unknown,1,"Source exam","maxScore",100f));
  dto.setWeightPercent(40f);dto.setMinScore(0f);dto.setMaxScore(20f);
  String edited=SourceAssessmentValues.annotate(unknown,List.of(dto));
  assertEquals(40f,SourceAssessmentValues.visible(edited,1,"Source exam","weightPercent",40f));
  assertEquals(0f,SourceAssessmentValues.visible(edited,1,"Source exam","minScore",0f));
  assertEquals(20f,SourceAssessmentValues.visible(edited,1,"Source exam","maxScore",20f));
 }
 @Test void legacyTextAndAnotherAssessmentKeepTheirActualValues() {
  assertEquals(100f,SourceAssessmentValues.visible("Legacy instructor notes",1,"Exam","maxScore",100f));
  assertEquals("Legacy instructor notes",SourceAssessmentValues.annotate("Legacy instructor notes",List.of()));
  var dto=new CreateSyllabusRequest.AssessmentDTO();dto.setName("Exam A");dto.setOrderIndex(1);
  String notes=SourceAssessmentValues.annotate("{\"sourceFaithful2027\":true}",List.of(dto));
  assertEquals(100f,SourceAssessmentValues.visible(notes,2,"Exam B","maxScore",100f));
 }
}
