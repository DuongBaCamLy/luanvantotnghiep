package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class Cohort2027PdfTableRecoveryTest {
 @Test void mergedCloCellsAndAdjacentGeneralFieldsKeepTheirSeparateSourceValues() throws Exception {
  try(var doc=new PDDocument()) {
   var page=new PDPage();doc.addPage(page);
   try(var stream=new PDPageContentStream(doc,page)) {
    for(float[] row:new float[][]{{700,540},{540,470},{470,400}}) {
     for(float y:row){stream.moveTo(50,y);stream.lineTo(550,y);stream.stroke();}
     for(float x:new float[]{50,160,550}){stream.moveTo(x,row[1]);stream.lineTo(x,row[0]);stream.stroke();}
    }
    stream.moveTo(160,620);stream.lineTo(550,620);stream.stroke();
    line(stream,55,680,"Course learning");line(stream,55,665,"outcomes");
    line(stream,165,680,"CLO1: First documented outcome");
    line(stream,165,600,"CLO2: Second documented outcome");
    line(stream,55,520,"Course designation");line(stream,165,520,"Face to face");
    line(stream,55,450,"Study and examination");line(stream,55,435,"requirements");
    line(stream,165,450,"Attend at least 80% of classes.");
   }
   var data=new SyllabusImportData();
   data.setClos(new ArrayList<>(List.of(
    com.scse.curriculum.syllabus.importer.dto.CloImportData.builder().code("CLO1").description("initial first outcome").build(),
    com.scse.curriculum.syllabus.importer.dto.CloImportData.builder().code("CLO2").description("initial second outcome").build())));
   Cohort2027PdfTableRecovery.recover(doc,1,1,data);
   assertEquals(List.of("First documented outcome","Second documented outcome"),
    data.getClos().stream().map(c->c.getDescription()).toList());
   assertEquals("Face to face",data.getCourseDesignation());
   assertEquals("Attend at least 80% of classes.",data.getExamRequirements());
  }
 }
 @Test void followsActualWeeklyHeaderOrderInsteadOfSwappingAssessmentAndActivities() {
  var weekly=new ArrayList<SyllabusImportData.WeeklyActivityItem>();
  Cohort2027PdfTableRecovery.recoverRows(List.of(cell(0,0,30,"Week"),cell(30,0,130,"Topic"),cell(130,0,160,"CLO"),
   cell(160,0,250,"Learning activities"),cell(250,0,350,"Assessments"),cell(350,0,400,"Resources"),
   cell(0,20,30,"2"),cell(30,20,130,"Actual source topic"),cell(130,20,160,"1"),
   cell(160,20,250,"Discussion"),cell(250,20,350,"Quiz"),cell(350,20,400,"[1]")),new ArrayList<>(),weekly,new ArrayList<>());
  assertEquals("Quiz",weekly.get(0).getAssessments());assertEquals("Discussion",weekly.get(0).getLearningActivities());
 }
 @Test void readableGeneralTextCannotConcealAnUnverifiedContentImage() throws Exception {
  try(var doc=new PDDocument()) {
   var page=new PDPage();doc.addPage(page);
   try(var stream=new PDPageContentStream(doc,page)) {
    for(float y:new float[]{700,620}){stream.moveTo(50,y);stream.lineTo(550,y);stream.stroke();}
    for(float x:new float[]{50,160,550}){stream.moveTo(x,620);stream.lineTo(x,700);stream.stroke();}
    line(stream,55,680,"Content");line(stream,165,680,"Source content table follows.");
    var bitmap=new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB);
    stream.drawImage(org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(doc,bitmap),180,630,200,30);
   }
   assertTrue(Cohort2027PdfTableRecovery.recover(doc,1,1,new SyllabusImportData()).stream()
    .anyMatch(issue->issue.contains("SOURCE_NOT_VERIFIED") && issue.contains("image table")));
  }
 }
 @Test void preservesSourceAlignmentNotationWithoutInventingItsMeaningOrDiscardingAdditionalColumns() {
  var mappings=new ArrayList<SyllabusImportData.CloPloMappingItem>();
  Cohort2027PdfTableRecovery.recoverRows(List.of(cell(0,0,50,"SLO"),cell(50,0,100,"1"),cell(100,0,150,"10"),
   cell(0,20,50,"1"),cell(50,20,100,"R,M"),cell(100,20,150,"R,M")),new ArrayList<>(),new ArrayList<>(),mappings);
  assertEquals(2,mappings.size());assertEquals("R,M",mappings.get(1).getValue());
  assertEquals("PLO10",mappings.get(1).getPloCode());assertNull(mappings.get(1).getContributionWeight());
 }

 @Test void retainsObjectivesAcrossBlankLabelPageContinuationAndVerbatimReadingCategories() throws Exception {
  try(var doc=new PDDocument()) {
   for(int index=0;index<2;index++) {
    var page=new PDPage();doc.addPage(page);
    try(var stream=new PDPageContentStream(doc,page)) {
     for(float y:new float[]{700,650,600}){stream.moveTo(50,y);stream.lineTo(550,y);stream.stroke();}
     for(float x:new float[]{50,160,550}){stream.moveTo(x,600);stream.lineTo(x,700);stream.stroke();}
     if(index==0){line(stream,55,680,"Course objectives");line(stream,165,680,"First source objective paragraph.");}
     else {line(stream,165,680,"Continuation with complete academic meaning.");
      line(stream,55,630,"Reading list");line(stream,165,630,"Textbooks: Alpha. References: Beta.");}
    }
   }
   var data=new SyllabusImportData();Cohort2027PdfTableRecovery.recover(doc,1,2,data);
   assertEquals("First source objective paragraph. Continuation with complete academic meaning.",data.getObjectives());
   assertEquals("Textbooks: Alpha. References: Beta.",data.getReadings().stream()
    .filter(r->"RAW_SOURCE".equals(r.getType())).findFirst().orElseThrow().getTitle());
  }
 }
 @Test void cloudTopicsCannotBeMisidentifiedAsCloMatrixHeaders() {
  var topics=new ArrayList<com.scse.curriculum.syllabus.importer.dto.TopicImportData>();
  Cohort2027PdfTableRecovery.recoverRows(List.of(cell(0,0,200,"Topic"),cell(200,0,240,"Weight"),cell(240,0,280,"Level"),
   cell(0,20,200,"Cloud Computing Platforms"),cell(200,20,240,"3"),cell(240,20,280,"T"),
   cell(0,40,200,"Cloud Security"),cell(200,40,240,"2"),cell(240,40,280,"T")),topics,new ArrayList<>(),new ArrayList<>());
  assertEquals(List.of("Cloud Computing Platforms","Cloud Security"),topics.stream().map(t->t.getName()).toList());
 }
 @Test void ignoresEnclosingSpacerCellsAndDoesNotReadExaminationAsContent() {
  var cells=List.of(cell(0,0,100,"Content"),cell(100,0,300,"Topic"),cell(300,0,350,"Weigh t"),
   cell(350,0,400,"Leve l"),cell(400,0,450,""),cell(100,20,300,"First source row"),
   cell(300,20,350,"1"),cell(350,20,400,"I,T"),cell(100,40,300,"Second source row"),
   cell(300,40,350,"2"),cell(350,40,400,"T,U"),cell(0,60,100,"Examination forms"),
   cell(100,60,450,"Written examination"));
  var topics=new ArrayList<com.scse.curriculum.syllabus.importer.dto.TopicImportData>();
  Cohort2027PdfTableRecovery.recoverRows(cells,topics,new ArrayList<>(),new ArrayList<>());
  assertEquals(List.of("First source row","Second source row"),topics.stream().map(t->t.getName()).toList());
  assertTrue(topics.stream().allMatch(t->t.getWeekNumber()==null && t.getTeachingHours()==null));
 }
 @Test void recognizesWeeklyHeaderSeparatelyFromContentHeader() {
  var cells=List.of(cell(0,0,40,"Week"),cell(40,0,100,"Topic"),cell(100,0,140,"CLO"),
   cell(140,0,200,"Assessments"),cell(200,0,280,"Activities"),cell(0,20,40,"7"),
   cell(40,20,100,"Actual weekly topic"),cell(100,20,140,"1"),cell(140,20,200,"Quiz"),cell(200,20,280,"Discussion"));
  var topics=new ArrayList<com.scse.curriculum.syllabus.importer.dto.TopicImportData>();
  var weekly=new ArrayList<SyllabusImportData.WeeklyActivityItem>();
  Cohort2027PdfTableRecovery.recoverRows(cells,topics,weekly,new ArrayList<>());
  assertTrue(topics.isEmpty());assertEquals(1,weekly.size());assertEquals(7,weekly.get(0).getWeek());
 }
 @Test void ignoresClippingRectanglesAndRetainsRowsAcrossPageBreaks() throws Exception {
  try(var doc=new PDDocument()) {
   for(int number=0;number<2;number++) {
    var page=new PDPage();doc.addPage(page);
    try(var stream=new PDPageContentStream(doc,page)) {
     for(float y:new float[]{700,660,620}){stream.moveTo(50,y);stream.lineTo(550,y);stream.stroke();}
     for(float x:new float[]{50,350,420,550}){stream.moveTo(x,620);stream.lineTo(x,700);stream.stroke();}
     if(number==0){line(stream,55,680,"Topic");line(stream,355,680,"Weight");line(stream,425,680,"Level");}
     stream.setNonStrokingColor(java.awt.Color.WHITE);stream.addRect(54,623,292,33);stream.fill();
     stream.setNonStrokingColor(java.awt.Color.BLACK);
     stream.saveGraphicsState();stream.addRect(54,623,292,33);stream.clip();
     line(stream,55,640,number==0?"First source content":"Second source content");stream.restoreGraphicsState();
     line(stream,355,640,"1");line(stream,425,640,number==0?"I":"T,U");
    }
   }
   var data=new SyllabusImportData();Cohort2027PdfTableRecovery.recover(doc,1,2,data);
   assertEquals(List.of("First source content","Second source content"),data.getTopics().stream().map(t->t.getName()).toList());
   assertTrue(data.getTopics().stream().allMatch(t->t.getWeekNumber()==null && t.getTeachingHours()==null));
  }
 }
 @Test void doesNotInferWeeklyAssociationFromAmbiguousPrefix() {
  assertTrue(Cohort2027PdfTableRecovery.matchesWeeklyTopic("Game Development Cycle", "Game Development Cycle – Part 2"));
  assertTrue(Cohort2027PdfTableRecovery.matchesWeeklyTopic("Series", "Series"));
  assertFalse(Cohort2027PdfTableRecovery.matchesWeeklyTopic("Series", "Series and sequences"));
  assertFalse(Cohort2027PdfTableRecovery.matchesWeeklyTopic("", "Series"));
 }
 @Test void retainsBareCreditPointsAndRawSemesterWithoutInventingCreditBreakdown() {
  var data=new SyllabusImportData();
  Cohort2027PdfTableRecovery.recoverGeneral(List.of(
   new Cohort2027PdfTableRecovery.Cell(0,0,100,20,"Credit points"),
   new Cohort2027PdfTableRecovery.Cell(100,0,300,20,"4"),
   new Cohort2027PdfTableRecovery.Cell(0,20,100,40,"Semester(s) in which the course is taught"),
   new Cohort2027PdfTableRecovery.Cell(100,20,300,40,"7,9")),data);
  assertEquals("4",data.getCreditPoints());assertEquals("7,9",data.getSemester());
  assertTrue(data.getLectureCredits()==null || data.getLectureCredits().isBlank());
  assertTrue(data.getLaboratoryCredits()==null || data.getLaboratoryCredits().isBlank());
 }
 @Test void separatesMixedCloDelimitersWithoutFlatteningHierarchicalCodes() {
  var data=new SyllabusImportData();
  data.setClos(new ArrayList<>(List.of(
   com.scse.curriculum.syllabus.importer.dto.CloImportData.builder().code("CLO1").description("old1").build(),
   com.scse.curriculum.syllabus.importer.dto.CloImportData.builder().code("CLO2").description("old2").build(),
   com.scse.curriculum.syllabus.importer.dto.CloImportData.builder().code("CLO3").description("old3").build())));
  Cohort2027PdfTableRecovery.recoverClos("CLO1: First outcome CLO 2 Second outcome CLO3. Third outcome Competency level Knowledge CLO1",data);
  assertEquals("First outcome",data.getClos().get(0).getDescription());
  assertEquals("Second outcome",data.getClos().get(1).getDescription());
  assertEquals("Third outcome",data.getClos().get(2).getDescription());
  Cohort2027PdfTableRecovery.recoverClos("CLO1.1: Hierarchical outcome",data);
  assertEquals("First outcome",data.getClos().get(0).getDescription());
 }
 @Test void extractsAdjacentRuledCellsWithoutInterleavingLabelsIntoObjectives() throws Exception {
  try(var doc=new PDDocument()) {
   var page=new PDPage();doc.addPage(page);
   try(var stream=new PDPageContentStream(doc,page)) {
    for(float y:new float[]{700,640,600}) {stream.moveTo(50,y);stream.lineTo(550,y);stream.stroke();}
    for(float x:new float[]{50,160,550}) {stream.moveTo(x,600);stream.lineTo(x,700);stream.stroke();}
    line(stream,55,680,"Course");line(stream,55,660,"objectives");
    line(stream,165,680,"Design games and playful experiences.");line(stream,165,660,"Use rapid prototyping and play testing.");
    line(stream,55,620,"Language");line(stream,165,620,"English");
   }
   var data=new SyllabusImportData();Cohort2027PdfTableRecovery.recover(doc,1,1,data);
   assertEquals("Design games and playful experiences. Use rapid prototyping and play testing.",data.getObjectives());
   assertEquals("English",data.getLanguage());
  }
 }
 @Test void preservesSourceWorkloadBreakdownWithoutInventingTotals() {
  var data=new SyllabusImportData();
  Cohort2027PdfTableRecovery.recoverWorkload("Total workload: 182.5 hours Contact hours (lecture, exercise, laboratory): Lecture: 37.5 hours + Laboratory: 25 hours. Private study including examination preparation, specified in hours: 120 hours.",data);
  assertEquals("182.5 hours",data.getWorkloadTotal());
  assertEquals("Lecture: 37.5 hours + Laboratory: 25 hours.",data.getWorkloadContact());
  assertEquals("120 hours.",data.getWorkloadPrivate());
  var absent=new SyllabusImportData();Cohort2027PdfTableRecovery.recoverWorkload("Contact hours: 30 periods",absent);
  assertNull(absent.getWorkloadTotal());assertNull(absent.getWorkloadPrivate());
 }
 @Test void preservesNumericAlignmentWithoutConvertingItToXOrPercent() {
  var cells=List.of(cell(0,0,40,"CLO"),cell(40,0,80,"PLO1"),cell(80,0,120,"PLO7"),
                   cell(0,20,40,"1"),cell(40,20,80,"4"),cell(80,20,120,"3"));
  var mappings=new ArrayList<SyllabusImportData.CloPloMappingItem>();
  Cohort2027PdfTableRecovery.recoverRows(cells,new ArrayList<>(),new ArrayList<>(),mappings);
  assertEquals(2,mappings.size());assertEquals("4",mappings.get(0).getValue());
  assertEquals("PLO7",mappings.get(1).getPloCode());assertNull(mappings.get(1).getContributionWeight());
 }
 @Test void contentRowsStaySeparateAndDoNotAcquireAnInventedTeachingWeek() {
  var cells=List.of(cell(0,0,200,"Topic"),cell(200,0,230,"Weight"),cell(230,0,260,"Level"),
   cell(0,20,200,"Limits and continuity"),cell(200,20,230,"1"),cell(230,20,260,"I, T"),
   cell(0,40,200,"Differentiation"),cell(200,40,230,"2"),cell(230,40,260,"T, U"));
  var topics=new ArrayList<com.scse.curriculum.syllabus.importer.dto.TopicImportData>();
  Cohort2027PdfTableRecovery.recoverRows(cells,topics,new ArrayList<>(),new ArrayList<>());
  assertEquals(2,topics.size());assertEquals("Differentiation",topics.get(1).getName());
  assertEquals("T, U",topics.get(1).getContentLevel());assertNull(topics.get(1).getWeekNumber());
 }
 private static Cohort2027PdfTableRecovery.Cell cell(float x,float y,float right,String text){return new Cohort2027PdfTableRecovery.Cell(x,y,right,y+20,text);}
 private static void line(PDPageContentStream stream,float x,float y,String text)throws Exception {
  stream.beginText();stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),10);stream.newLineAtOffset(x,y);stream.showText(text);stream.endText();
 }
}
