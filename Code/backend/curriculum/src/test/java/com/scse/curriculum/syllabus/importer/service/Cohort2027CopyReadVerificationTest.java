package com.scse.curriculum.syllabus.importer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scse.curriculum.auth.security.CurrentUserService;
import com.scse.curriculum.syllabus.service.*;
import com.scse.curriculum.syllabus.template.TargetTemplateProfileResolver;
import com.scse.curriculum.syllabus.pdf.*;
import com.scse.curriculum.syllabus.word.SyllabusWordRenderer;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.user.repository.UserAccountRepository;
import com.scse.curriculum.cohort.service.CohortOperationalGuard;
import com.scse.curriculum.syllabus.importer.parser.*;
import com.scse.curriculum.syllabus.importer.dto.BulkConfirmSyllabusImportRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** Opt-in read-only check on the restored copy; no server, migrations, scheduled jobs or writes. */
@EnabledIfSystemProperty(named="cs2027.copyVerification", matches="curriculum_2027_import_test_20261011")
@SpringBootTest(classes=Cohort2027CopyReadVerificationTest.ReadConfig.class,
        webEnvironment=SpringBootTest.WebEnvironment.NONE)
class Cohort2027CopyReadVerificationTest {
    static final Path ROOT=Path.of("../../.workflow-checks/cohort-2027-audit");
    @Configuration @EnableAutoConfiguration
    @EntityScan("com.scse.curriculum") @EnableJpaRepositories("com.scse.curriculum")
    @Import({CurrentUserService.class,SyllabusAccessService.class,TargetTemplateProfileResolver.class,SyllabusPdfDataLoader.class,
            SyllabusIdentityService.class,CohortOperationalGuard.class,SyllabusPdfParser.class,PdfProgramDocumentParser.class})
    static class ReadConfig {
        @Bean SyllabusServiceImpl syllabusService(ConfigurableListableBeanFactory beans) throws Exception {
            var constructor=SyllabusServiceImpl.class.getConstructors()[0];
            var parameters=Arrays.stream(constructor.getParameterTypes()).map(type -> {
                if(beans.getBeanNamesForType(type).length>0)return beans.getBean(type);
                return mock(type);
            }).toArray();
            return (SyllabusServiceImpl)constructor.newInstance(parameters);
        }
        @Bean SyllabusImportServiceImpl importService(ConfigurableListableBeanFactory beans) throws Exception {
            var constructor=SyllabusImportServiceImpl.class.getConstructors()[0];var types=constructor.getParameterTypes();
            var generic=constructor.getGenericParameterTypes();var parameters=new Object[types.length];
            for(int i=0;i<types.length;i++) {
                if(types[i]==List.class)parameters[i]=generic[i].getTypeName().contains("ProgramDocumentParser")
                        ? List.of(beans.getBean(PdfProgramDocumentParser.class)) : List.of(beans.getBean(SyllabusPdfParser.class));
                else parameters[i]=beans.getBeanNamesForType(types[i]).length>0 ? beans.getBean(types[i]) : mock(types[i]);
            }
            return (SyllabusImportServiceImpl)constructor.newInstance(parameters);
        }
    }
    @DynamicPropertySource static void copyOnly(DynamicPropertyRegistry properties) throws Exception {
        var source=new Properties();try(var stream=Files.newInputStream(Path.of("src/main/resources/application.properties"))){source.load(stream);}
        String url=source.getProperty("spring.datasource.url").replace("/curriculum_iu?","/curriculum_2027_import_test_20261011?");
        assertTrue(url.contains("/curriculum_2027_import_test_20261011?"));
        assertTrue(Set.of("localhost","127.0.0.1","::1").contains(java.net.URI.create(url.substring(5)).getHost()));
        properties.add("spring.datasource.url",()->url);
        properties.add("spring.datasource.username",()->source.getProperty("spring.datasource.username"));
        properties.add("spring.datasource.password",()->source.getProperty("spring.datasource.password"));
        properties.add("spring.jpa.hibernate.ddl-auto",()->"none");
        properties.add("spring.sql.init.mode",()->"never");
        properties.add("spring.flyway.enabled",()->"false");
        properties.add("spring.liquibase.enabled",()->"false");
        properties.add("spring.jpa.show-sql",()->"false");
        properties.add("spring.datasource.hikari.read-only",()->"false");
        properties.add("logging.level.org.hibernate.SQL",()->"OFF");
        properties.add("logging.level.org.hibernate.orm.jdbc.bind",()->"OFF");
    }
    @Autowired SyllabusServiceImpl syllabusService;
    @Autowired SyllabusPdfDataLoader loader;
    @Autowired UserAccountRepository users;
    @Autowired PlatformTransactionManager transactions;
    @Autowired SyllabusImportServiceImpl importer;
    @Test void actualBulkPreviewAndConfirmPreserveContentAndRejectDuplicatesInsideRolledBackCopyTransaction() throws Exception {
        var json=new ObjectMapper().findAndRegisterModules();
        var source=json.readTree(ROOT.resolve("source_manifest.json").toFile()).path("CS");
        var transaction=new TransactionTemplate(transactions);
        try {
            transaction.executeWithoutResult(status -> {
                try {
                    var admin=users.findAll().stream().filter(user->user.getRole()==UserRole.ADMIN).findFirst().orElseThrow();
                    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin.getUsername(),null,List.of()));
                    var file=new MockMultipartFile("file","CS2026.pdf","application/pdf",Files.readAllBytes(Path.of(source.path("path").asText())));
                    var preview=importer.previewBulk(file,1,15);
                    assertEquals(55,preview.getSyllabusCount());
                    assertEquals(49,preview.getItems().stream().filter(item->item.getPreview().isValid()).count());
                    json.writerWithDefaultPrettyPrinter().writeValue(ROOT.resolve("import-fix-actual-bulk-preview.json").toFile(),preview);
                    var data=preview.getItems().stream().map(item->item.getPreview().getData())
                            .filter(item->"EN008IU".equals(item.getSourceCourseCode())).findFirst().orElseThrow();
                    var request=new BulkConfirmSyllabusImportRequest();request.setProgramId(1);request.setCohortId(15);
                    request.setData(data);request.setOriginalFileName("CS2026.pdf");request.setOriginalFileType("application/pdf");
                    var saved=importer.confirmBulkItem(request);
                    var reread=syllabusService.getById(saved.getId());
                    assertEquals(saved.getNotes(),reread.getNotes());assertTrue(reread.getTopics().isEmpty());
                    var notes=json.readTree(reread.getNotes());assertEquals(11,notes.path("sourceContentRows").size());
                    assertTrue(notes.path("sourceContentRows").get(0).path("weekNumber").isNull());
                    assertTrue(notes.path("sourceContentRows").get(0).path("teachingHours").isNull());
                    assertThrows(IllegalStateException.class,()->importer.confirmBulkItem(request));
                    var export=loader.load(saved.getId());
                    var renderer=new SyllabusPdfRenderer(new SyllabusPdfFontProvider("",""),json,
                            "VIETNAM NATIONAL UNIVERSITY HCMC - INTERNATIONAL UNIVERSITY","SCHOOL OF COMPUTER SCIENCE AND ENGINEERING");
                    Files.write(ROOT.resolve("import-fix-roundtrip-EN008IU.pdf"),renderer.render(export,SyllabusPdfMode.EXPORT));
                    Files.write(ROOT.resolve("import-fix-roundtrip-EN008IU.docx"),new SyllabusWordRenderer(renderer).render(export));
                    json.writerWithDefaultPrettyPrinter().writeValue(ROOT.resolve("import-fix-roundtrip-EN008IU.json").toFile(),reread);
                } catch(Exception error){throw new IllegalStateException(error);}
                finally {status.setRollbackOnly();}
            });
        } finally {SecurityContextHolder.clearContext();}
    }
    @Test void savedCopyReachesRealResponseMapperAndRealPdfWordLoadersWithoutChangingNotes() throws Exception {
        var json=new ObjectMapper().findAndRegisterModules();
        var manifest=json.readTree(ROOT.resolve("verified-import-fix-manifest.json").toFile());
        Path out=ROOT.resolve("import-fix-service-export");Files.createDirectories(out);
        var renderer=new SyllabusPdfRenderer(new SyllabusPdfFontProvider("",""),json,
                "VIETNAM NATIONAL UNIVERSITY HCMC - INTERNATIONAL UNIVERSITY","SCHOOL OF COMPUTER SCIENCE AND ENGINEERING");
        var word=new SyllabusWordRenderer(renderer);var responses=new ArrayList<Object>();var exports=new ArrayList<Object>();
        var read=new TransactionTemplate(transactions);read.setReadOnly(true);
        try {
            read.executeWithoutResult(status -> {
                var admin=users.findAll().stream().filter(user->user.getRole()==UserRole.ADMIN).findFirst().orElseThrow();
                // Controlled test authentication for an actual account; the live JWT flow is untouched.
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(admin.getUsername(),null,List.of()));
                for(var target:manifest.path("targets")) {
                    int id=target.path("syllabusId").asInt();
                    var response=syllabusService.getById(id);assertEquals("NEW_2027",response.getTargetTemplateProfile());
                    var export=loader.load(id);
                    var change=java.util.stream.StreamSupport.stream(manifest.path("changes").spliterator(),false)
                            .filter(c->c.path("table").asText().equals("syllabus") && c.path("syllabusId").asInt()==id).findFirst().orElseThrow();
                    assertEquals(change.path("after").path("notes").asText(),response.getNotes());
                    responses.add(response);exports.add(export);
                    try {
                        String base=target.path("courseCode").asText()+"-"+id;
                        Files.write(out.resolve(base+".pdf"),renderer.render(export,SyllabusPdfMode.EXPORT));
                        Files.write(out.resolve(base+".docx"),word.render(export));
                    } catch(Exception error){throw new IllegalStateException(error);}
                }
            });
            assertEquals(13,responses.size());
            json.writerWithDefaultPrettyPrinter().writeValue(ROOT.resolve("import-fix-service-responses.json").toFile(),responses);
            json.writerWithDefaultPrettyPrinter().writeValue(ROOT.resolve("import-fix-service-export-inputs.json").toFile(),exports);
        } finally {SecurityContextHolder.clearContext();}
    }
}
