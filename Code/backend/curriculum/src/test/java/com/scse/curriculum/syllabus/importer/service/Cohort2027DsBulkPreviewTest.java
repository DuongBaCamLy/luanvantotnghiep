package com.scse.curriculum.syllabus.importer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.cohort.service.CohortOperationalGuard;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.program.repository.ProgramRepository;
import com.scse.curriculum.syllabus.importer.dto.*;
import com.scse.curriculum.syllabus.source.entity.*;
import com.scse.curriculum.syllabus.importer.parser.*;
import com.scse.curriculum.syllabus.source.repository.*;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.syllabus.template.TargetTemplateProfileResolver;
import com.scse.curriculum.user.entity.*;
import com.scse.curriculum.auth.security.CurrentUserService;
import org.apache.pdfbox.Loader;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;

/** Real DS PDF/parser/service; only database repositories and actor context are mocked. */
class Cohort2027DsBulkPreviewTest {
    @Test void everyScopedDsSyllabusPassesWithoutLosingSourceContentOrChangingCurriculum() throws Exception {
        String sourcePath=System.getProperty("ds2026Pdf");
        assumeTrue(sourcePath!=null && Files.isRegularFile(Path.of(sourcePath)),"Set ds2026Pdf to the original DS2026 PDF");
        byte[] source=Files.readAllBytes(Path.of(sourcePath));
        assertEquals("fd279731316e301fce93cd25c9231959b1f73631f62c7d38e883d87bfaba1f32",digest(source));
        var service=mock(SyllabusImportServiceImpl.class,CALLS_REAL_METHODS);
        var programs=mock(ProgramRepository.class);var cohorts=mock(CohortRepository.class);
        var documents=mock(SourceDocumentRepository.class);var snapshots=mock(SyllabusSourceSnapshotRepository.class);
        var currentUser=mock(CurrentUserService.class);var assignments=mock(CourseProgramRepository.class);
        var syllabuses=mock(SyllabusRepository.class);var json=new ObjectMapper();
        var program=Program.builder().id(3).code("DS").build();
        var cohort=Cohort.builder().id(17).entryYear(2027).name("DS2027").isActive(true).program(program).build();
        when(programs.findById(3)).thenReturn(Optional.of(program));when(cohorts.findById(17)).thenReturn(Optional.of(cohort));
        when(currentUser.getCurrentUser()).thenReturn(UserAccount.builder().id(1).role(UserRole.ADMIN).build());
        when(documents.save(any(SourceDocument.class))).thenAnswer(call->call.getArgument(0));
        var savedSnapshots=new ArrayList<SyllabusSourceSnapshot>();
        when(snapshots.save(any(SyllabusSourceSnapshot.class))).thenAnswer(call->{
            var snapshot=(SyllabusSourceSnapshot)call.getArgument(0);savedSnapshots.add(snapshot);return snapshot;
        });
        set(service,"programRepository",programs);set(service,"cohortRepository",cohorts);
        set(service,"cohortOperationalGuard",mock(CohortOperationalGuard.class));
        set(service,"targetTemplateProfileResolver",new TargetTemplateProfileResolver());
        set(service,"programDocumentParsers",List.of(new PdfProgramDocumentParser(new SyllabusPdfParser())));
        set(service,"sourceDocumentRepository",documents);set(service,"sourceSnapshotRepository",snapshots);
        set(service,"currentUserService",currentUser);set(service,"objectMapper",json);
        set(service,"courseProgramRepository",assignments);set(service,"syllabusRepository",syllabuses);
        var result=service.previewBulk(new MockMultipartFile("file","DS2026.pdf","application/pdf",source),3,17);
        assertEquals(513,result.getPageCount());assertEquals(46,result.getSyllabusCount());assertEquals("NEW_2027",result.getTargetTemplateProfile());
        assertEquals(46,savedSnapshots.size());
        var selected=new LinkedHashMap<String,SyllabusImportData>();
        var notesMethod=SyllabusImportServiceImpl.class.getDeclaredMethod("buildStandardFormNotes",SyllabusImportData.class,boolean.class);
        notesMethod.setAccessible(true);
        var evidence=json.createArrayNode();
        for(var item:result.getItems()) {
            var preview=item.getPreview();var data=preview.getData();selected.put(data.getSourceCourseCode(),data);
            assertTrue(preview.isValid(),data.getSourceCourseCode()+": "+preview.getIssues());
            assertEquals(0,preview.getErrorCount());
            assertTrue(data.getRubricItems().isEmpty(),"Preset rubric values must not be imported into DS2027");
            var notes=json.readTree((String)notesMethod.invoke(service,data,true));
            var sourceRows=notes.path("sourceContentRows");
            if(!data.getTopics().isEmpty()) {
                assertEquals(data.getTopics().size(),sourceRows.size(),data.getSourceCourseCode()+" Content lost during notes persistence");
                for(int row=0;row<data.getTopics().size();row++) {
                    assertEquals(data.getTopics().get(row).getName(),sourceRows.get(row).path("name").asText());
                    assertTrue(sourceRows.get(row).path("weekNumber").isNull());
                }
                assertTrue(data.getTopicCloMappings().isEmpty());
            }
            var record=evidence.addObject();record.put("code",data.getSourceCourseCode());record.put("contentRows",sourceRows.size());record.set("notes",notes);
        }
        assertEquals(15,selected.get("EN011IU").getTopics().size());
        assertTrue(selected.get("EN011IU").getTopics().stream().anyMatch(t->t.getName().equals("MIDTERM EXAM WEEK OF THE UNIVERSITY")));
        assertEquals(12,selected.get("IT136").getTopics().size());
        assertEquals("6+4",selected.get("IT136").getTopics().get(3).getContentWeight());
        assertEquals("Process Models and Process Discovery",selected.get("IT144IU").getTopics().get(1).getName());
        assertTrue(selected.get("IT144IU").getWeeklyActivities().stream().anyMatch(row->"2, 5".equals(row.getClo())));
        assertEquals(3,selected.get("MA001IU").getClos().size());assertEquals(17,selected.get("MA001IU").getTopics().size());
        assertFalse(selected.containsKey("IT154"));assertFalse(selected.containsKey("IT158IU"));
        for(var snapshot:savedSnapshots)try(var pdf=Loader.loadPDF(snapshot.getContent())) {
            assertEquals(snapshot.getEndBoundary()-snapshot.getStartBoundary()+1,pdf.getNumberOfPages());
            assertEquals(digest(snapshot.getContent()),snapshot.getSha256());
        }
        verifyNoInteractions(assignments,syllabuses);
        var out=System.getProperty("dsAuditOutput");
        if(out!=null) {
            Path directory=Path.of(out);Files.createDirectories(directory);
            json.writerWithDefaultPrettyPrinter().writeValue(directory.resolve("DS-real-bulk-preview.json").toFile(),result);
            json.writerWithDefaultPrettyPrinter().writeValue(directory.resolve("DS-notes-roundtrip.json").toFile(),evidence);
        }
    }
    private static void set(Object service,String fieldName,Object value)throws Exception {
        var field=SyllabusImportServiceImpl.class.getDeclaredField(fieldName);field.setAccessible(true);field.set(service,value);
    }
    private static String digest(byte[] source)throws Exception {
        return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(source));
    }
}
