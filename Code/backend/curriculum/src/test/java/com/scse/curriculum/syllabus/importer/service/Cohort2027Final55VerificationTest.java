package com.scse.curriculum.syllabus.importer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scse.curriculum.syllabus.importer.parser.Cohort2027VerifiedSourceRecovery;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.user.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in verification on the restored copy; preview transaction is rolled back. */
@EnabledIfSystemProperty(named="cs2027.final55",matches="preview|prepare")
@SpringBootTest(classes=Cohort2027CopyReadVerificationTest.ReadConfig.class,webEnvironment=SpringBootTest.WebEnvironment.NONE)
class Cohort2027Final55VerificationTest {
    static final Path ROOT=Path.of("../../.workflow-checks/cohort-2027-audit");
    static final String COPY="curriculum_2027_complete_test_20261011";
    @DynamicPropertySource static void copyOnly(DynamicPropertyRegistry properties)throws Exception {
        var source=new Properties();try(var stream=Files.newInputStream(Path.of("src/main/resources/application.properties"))){source.load(stream);}
        String url=source.getProperty("spring.datasource.url").replace("/curriculum_iu?","/"+COPY+"?");
        assertTrue(url.contains("/"+COPY+"?"));
        assertTrue(Set.of("localhost","127.0.0.1","::1").contains(java.net.URI.create(url.substring(5)).getHost()));
        properties.add("spring.datasource.url",()->url);
        properties.add("spring.datasource.username",()->source.getProperty("spring.datasource.username"));
        properties.add("spring.datasource.password",()->source.getProperty("spring.datasource.password"));
        properties.add("spring.jpa.hibernate.ddl-auto",()->"none");properties.add("spring.sql.init.mode",()->"never");
        properties.add("spring.flyway.enabled",()->"false");properties.add("spring.liquibase.enabled",()->"false");
        properties.add("spring.jpa.show-sql",()->"false");properties.add("logging.level.org.hibernate.SQL",()->"OFF");
        properties.add("logging.level.org.hibernate.orm.jdbc.bind",()->"OFF");
    }
    @Autowired SyllabusImportServiceImpl importer;
    @Autowired UserAccountRepository users;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.scse.curriculum.syllabus.service.SyllabusServiceImpl syllabusService;
    @Test void sourcePreviewOnFreshRestoredCopy()throws Exception {
        assertEquals(COPY,jdbc.queryForObject("SELECT DATABASE()",String.class));
        assertEquals(109,jdbc.queryForObject("SELECT COUNT(*) FROM syllabus",Integer.class));
        var json=new ObjectMapper().findAndRegisterModules();
        var source=json.readTree(ROOT.resolve("source_manifest.json").toFile()).path("CS");
        var bytes=Files.readAllBytes(Path.of(source.path("path").asText()));
        assertEquals(source.path("sha256").asText(),HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)));
        try {
            new TransactionTemplate(transactions).executeWithoutResult(status->{
                try {
                    var admin=users.findAll().stream().filter(u->u.getRole()==UserRole.ADMIN).findFirst().orElseThrow();
                    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin.getUsername(),null,List.of()));
                    var preview=importer.previewBulk(new MockMultipartFile("file","CS2026.pdf","application/pdf",bytes),1,15);
                    json.writerWithDefaultPrettyPrinter().writeValue(ROOT.resolve("final55-actual-bulk-preview.json").toFile(),preview);
                    assertEquals(55,preview.getSyllabusCount());
                    for(var item:preview.getItems()) {
                        assertTrue(item.getPreview().isValid(),item.getPreview().getData().getSourceCourseCode()+": "+item.getPreview().getIssues());
                        String code=item.getPreview().getData().getSourceCourseCode();
                        if(Set.of("PE015IU","PE016IU","PE017IU","PE018IU","PE019IU","MA001IU").contains(code))
                            assertFalse(Cohort2027VerifiedSourceRecovery.reviewedNotes(item.getPreview().getData()).isEmpty(),code);
                    }
                    if("prepare".equals(System.getProperty("cs2027.final55")))preparePlan(json,preview);
                }catch(Exception error){throw new IllegalStateException(error);}
                finally{status.setRollbackOnly();}
            });
        }finally{SecurityContextHolder.clearContext();}
        assertEquals(109,jdbc.queryForObject("SELECT COUNT(*) FROM syllabus",Integer.class));
        assertEquals(56,jdbc.queryForObject("SELECT COUNT(*) FROM source_document",Integer.class));
    }
    private void preparePlan(ObjectMapper json,com.scse.curriculum.syllabus.importer.dto.BulkSyllabusImportPreviewResponse preview)throws Exception {
        var plan=json.createObjectNode();plan.put("sourceSha256","b24a4b9a0d052f8decc79410b92150415126bb5aabce6fa550bf4706d940f68a");
        var entries=plan.putArray("entries");
        var buildNotes=SyllabusImportServiceImpl.class.getDeclaredMethod("buildStandardFormNotes",com.scse.curriculum.syllabus.importer.dto.SyllabusImportData.class,boolean.class);
        buildNotes.setAccessible(true);Object target=org.springframework.test.util.AopTestUtils.getTargetObject(importer);
        for(var item:preview.getItems()) {
            var data=item.getPreview().getData();String code=data.getSourceCourseCode().split("/")[0];
            var mappings=jdbc.queryForList("SELECT cp.id,cp.course_id,cp.syllabus_id FROM course_program cp JOIN course co ON co.id=cp.course_id WHERE cp.program_id=1 AND cp.cohort_id=15 AND co.course_code=?",code);
            assertEquals(1,mappings.size(),code);var cp=mappings.get(0);var entry=entries.addObject();entry.put("courseCode",code);
            entry.put("courseProgramId",((Number)cp.get("id")).intValue());entry.put("courseId",((Number)cp.get("course_id")).intValue());
            entry.put("startPage",item.getStartPage());entry.put("endPage",item.getEndPage());entry.set("data",json.valueToTree(data));
            var notes=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree((String)buildNotes.invoke(target,data,true));
            if(!notes.has("sourceImportEvidence"))notes.set("sourceImportEvidence",json.valueToTree(Map.of("sourceSha256",plan.path("sourceSha256").asText(),"courseCode",code,"startPage",item.getStartPage(),"endPage",item.getEndPage(),"method","Original PDF ruled cells, independent field comparison")));
            if(cp.get("syllabus_id")==null){entry.put("operation","INSERT");}
            else {
                int id=((Number)cp.get("syllabus_id")).intValue();entry.put("operation","CORRECTION");entry.put("syllabusId",id);
                var before=syllabusService.getById(id);assertEquals("DRAFT",before.getStatus());assertEquals(code,before.getCourseCode());
                entry.set("beforeResponse",json.valueToTree(before));
                entry.set("beforeRow",json.valueToTree(jdbc.queryForMap("SELECT * FROM syllabus WHERE id=?",id)));
                var old=json.readTree(before.getNotes());
                for(String preserve:List.of("internalNotes","academicCorrection2027"))if(old.has(preserve))notes.set(preserve,old.get(preserve));
                var request=json.createObjectNode();var fields=json.valueToTree(data);
                for(String field:List.of("courseDesignation","courseTypes","language","relation","teachingMethods","workloadTotal","workloadContact","workloadPrivate","prerequisites","objectives","examForms","examRequirements"))
                    if(fields.hasNonNull(field))request.set(field,fields.get(field));
                var clos=request.putArray("clos");
                for(var imported:data.getClos()) {
                    var row=(com.fasterxml.jackson.databind.node.ObjectNode)json.valueToTree(imported);
                    var existing=before.getClos().stream().filter(c->c.getCode().equals(imported.getCode())).findFirst();
                    existing.ifPresent(c->row.put("id",c.getId()));clos.add(row);
                }
                if(!before.getClos().isEmpty())assertEquals(before.getClos().size(),clos.size(),code+" existing CLOs must be retained");
                var assessments=request.putArray("assessments");
                for(int index=0;index<data.getAssessments().size();index++) {
                    var row=(com.fasterxml.jackson.databind.node.ObjectNode)json.valueToTree(data.getAssessments().get(index));
                    if(!before.getAssessments().isEmpty())row.put("id",before.getAssessments().get(index).getId());assessments.add(row);
                }
                if(!before.getAssessments().isEmpty())assertEquals(before.getAssessments().size(),assessments.size(),code+" existing assessment IDs must be retained");
                request.put("notes",json.writeValueAsString(notes));entry.set("request",request);
            }
            int bytes=json.writeValueAsBytes(notes).length;entry.put("notesUtf8Bytes",bytes);assertTrue(bytes<65535,code+" source notes exceed storage: "+bytes);
            entry.set("expectedNotes",notes);
        }
        assertEquals(42,java.util.stream.StreamSupport.stream(entries.spliterator(),false).filter(v->v.path("operation").asText().equals("INSERT")).count());
        json.writerWithDefaultPrettyPrinter().writeValue(ROOT.resolve("final55-verified-plan.json").toFile(),plan);
    }
}
