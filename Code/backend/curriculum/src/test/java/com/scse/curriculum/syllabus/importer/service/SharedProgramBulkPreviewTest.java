package com.scse.curriculum.syllabus.importer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scse.curriculum.auth.security.CurrentUserService;
import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.cohort.service.CohortOperationalGuard;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.course.repository.CourseRepository;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.coursetype.entity.CourseType;
import com.scse.curriculum.coursetype.repository.CourseTypeRepository;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.program.repository.ProgramRepository;
import com.scse.curriculum.syllabus.importer.parser.*;
import com.scse.curriculum.syllabus.source.entity.*;
import com.scse.curriculum.syllabus.source.repository.*;
import com.scse.curriculum.syllabus.template.TargetTemplateProfileResolver;
import com.scse.curriculum.user.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;

class SharedProgramBulkPreviewTest {
    private static void set(Object target,String name,Object value)throws Exception {
        var field=SyllabusImportServiceImpl.class.getDeclaredField(name);field.setAccessible(true);field.set(target,value);
    }
    private SyllabusImportServiceImpl service(String key,int year,PdfProgramDocumentParser parser) throws Exception {
        var service=mock(SyllabusImportServiceImpl.class,CALLS_REAL_METHODS);
        var programs=mock(ProgramRepository.class);var cohorts=mock(CohortRepository.class);
        var current=mock(CurrentUserService.class);var documents=mock(SourceDocumentRepository.class);var snapshots=mock(SyllabusSourceSnapshotRepository.class);
        var catalog=mock(CourseRepository.class);var assignments=mock(CourseProgramRepository.class);var types=mock(CourseTypeRepository.class);
        var program=Program.builder().id(50).code(key).build();var cohort=Cohort.builder().id(60).name(key+year).entryYear(year).program(program).build();
        when(programs.findById(50)).thenReturn(Optional.of(program));when(cohorts.findById(60)).thenReturn(Optional.of(cohort));
        when(programs.getReferenceById(50)).thenReturn(program);when(cohorts.getReferenceById(60)).thenReturn(cohort);
        when(current.getCurrentUser()).thenReturn(UserAccount.builder().id(1).role(UserRole.ADMIN).build());
        when(documents.save(any())).thenAnswer(call->{SourceDocument source=call.getArgument(0);source.setId(1L);return source;});
        when(snapshots.save(any())).thenAnswer(call->{SyllabusSourceSnapshot source=call.getArgument(0);source.setId(2L);return source;});
        when(catalog.findAll()).thenReturn(List.of());
        when(types.findByCode(anyString())).thenAnswer(call->Optional.of(CourseType.builder().id(1).code(call.getArgument(0)).build()));
        set(service,"programRepository",programs);set(service,"cohortRepository",cohorts);set(service,"cohortOperationalGuard",mock(CohortOperationalGuard.class));
        set(service,"currentUserService",current);set(service,"sourceDocumentRepository",documents);set(service,"sourceSnapshotRepository",snapshots);
        set(service,"programDocumentParsers",List.of(parser));set(service,"targetTemplateProfileResolver",new TargetTemplateProfileResolver());
        set(service,"courseRepository",catalog);set(service,"courseProgramRepository",assignments);set(service,"courseTypeRepository",types);
        return service;
    }
    @Test void realLegacySourcesUseSelectedSpecializationWithinTheExistingPreviewService() throws Exception {
        String manifest=System.getProperty("neCeSources");assumeTrue(manifest!=null,"Set neCeSources for local PDF regression");
        var json=new ObjectMapper();var paths=json.readTree(Files.readString(Path.of(manifest)));
        for(String label:List.of("IT2021","IT2026"))for(String program:List.of("NE","CE")) {
            byte[] bytes=Files.readAllBytes(Path.of(paths.path(label).asText()));var parser=new PdfProgramDocumentParser(new SyllabusPdfParser());
            var service=service(program,Integer.parseInt(label.substring(2)),parser);
            var response=service.previewBulk(new MockMultipartFile("file",label+".pdf","application/pdf",bytes),50,60);
            assertEquals("SOURCE_TEMPLATE",response.getTargetTemplateProfile());assertTrue(response.getSyllabusCount()>30);
            var scope=SharedSpecializationPdfScope.extract(bytes,program);var seen=new HashSet<String>();
            for(var item:response.getItems()) {
                assertTrue(item.getStartPage()>=scope.firstPage());assertTrue(item.getEndPage()<=scope.lastPage());
                assertTrue(seen.add(item.getPreview().getData().getSourceCourseCode()),"No duplicated syllabus within one program");
            }
            String output=System.getProperty("neCeAudit");if(output!=null)json.writerWithDefaultPrettyPrinter().writeValue(Path.of(output,program+label.substring(2)+"-real-preview.json").toFile(),response);
        }
    }
    @Test void everyFutureEngineeringCohortUsesTheSharedNew2027ProfileAndParser() throws Exception {
        String manifest=System.getProperty("neCeSources");assumeTrue(manifest!=null,"Set neCeSources for source selection");
        var paths=new ObjectMapper().readTree(Files.readString(Path.of(manifest)));byte[] bytes=Files.readAllBytes(Path.of(paths.path("IT2026").asText()));
        for(String key:List.of("NE","CE"))for(int year:List.of(2027,2028,2030)) {
            var parser=mock(PdfProgramDocumentParser.class);when(parser.supports(any(),any(),any())).thenReturn(true);
            when(parser.parseScoped(any(),eq(true),anyInt(),anyInt())).thenReturn(new ProgramDocumentParser.ParsedProgramDocument(745,List.of()));
            var service=service(key,year,parser);
            var result=service.previewBulk(new MockMultipartFile("file","shared.pdf","application/pdf",bytes),50,60);
            assertEquals("NEW_2027",result.getTargetTemplateProfile());assertFalse(result.getTargetTemplateSections().isEmpty());
            verify(parser).parseScoped(any(),eq(true),eq(1),eq(745));verify(parser,never()).parse(any());
        }
    }

    @Test void real2026SourceUsesNew2027ContentRecoveryAndRetainsUnverifiedScanIssues() throws Exception {
        String manifest=System.getProperty("neCeSources");assumeTrue(manifest!=null,"Set neCeSources for real NEW_2027 regression");
        var json=new ObjectMapper();var paths=json.readTree(Files.readString(Path.of(manifest)));byte[] bytes=Files.readAllBytes(Path.of(paths.path("IT2026").asText()));
        for(String key:List.of("NE","CE")) {
            var sourceParser=new SyllabusPdfParser();var field=SyllabusPdfParser.class.getDeclaredField("ocrFallback");field.setAccessible(true);
            // These scans have not been manually reviewed. Do not certify OCR values
            // during an isolated test; retain SOURCE_NOT_VERIFIED through the real parser.
            field.set(sourceParser,mock(field.getType(),call->{throw new java.io.IOException("OCR intentionally disabled in source verification test");}));
            var service=service(key,2027,new PdfProgramDocumentParser(sourceParser));
            var result=service.previewBulk(new MockMultipartFile("file","IT2026.pdf","application/pdf",bytes),50,60);
            assertEquals("NEW_2027",result.getTargetTemplateProfile());assertTrue(result.getSyllabusCount()>40);
            assertTrue(result.getItems().stream().allMatch(item->item.getSourceSnapshotId()!=null));
            assertTrue(result.getItems().stream().anyMatch(item->item.getPreview().getIssues().stream().anyMatch(issue->issue.getMessage().contains("SOURCE_NOT_VERIFIED"))));
            var content=result.getItems().stream().flatMap(item->item.getPreview().getData().getTopics().stream()).toList();
            assertFalse(content.isEmpty());assertTrue(content.stream().anyMatch(topic->topic.getWeekNumber()==null && topic.getTeachingHours()==null));
            String output=System.getProperty("neCeAudit");if(output!=null)json.writerWithDefaultPrettyPrinter().writeValue(Path.of(output,key+"2027-real-preview.json").toFile(),result);
        }
    }

    @Test void realCs2027ReferenceKeepsAll55CodesContentAndValidation() throws Exception {
        Path manifest=Path.of("../../.workflow-checks/cohort-2027-audit/source_manifest.json");
        assumeTrue(Files.isRegularFile(manifest),"Local CS source manifest required");
        var json=new ObjectMapper();var source=json.readTree(manifest.toFile()).path("CS");
        byte[] bytes=Files.readAllBytes(Path.of(source.path("path").asText()));
        var service=service("CS-2021",2027,new PdfProgramDocumentParser(new SyllabusPdfParser()));
        var actual=service.previewBulk(new MockMultipartFile("file","CS2026.pdf","application/pdf",bytes),50,60);
        var reference=json.readTree(Path.of("../../.workflow-checks/cohort-2027-audit/final55-actual-bulk-preview.json").toFile());
        assertEquals(55,actual.getSyllabusCount());assertTrue(actual.getItems().stream().allMatch(item->item.getPreview().isValid()));
        for(int index=0;index<55;index++) {
            var data=json.readTree(json.writeValueAsBytes(actual.getItems().get(index).getPreview().getData()));var old=reference.path("items").get(index).path("preview").path("data");
            for(String field:List.of("sourceCourseCode","sourceCourseName","objectives","clos","topics","assessments","weeklyActivities","readings"))
                assertEquals(old.path(field),data.path(field),"CS reference changed: "+index+" "+field);
        }
    }
}
