package com.scse.curriculum.syllabus.importer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scse.curriculum.auth.security.CurrentUserService;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.cohort.service.CohortServiceImpl;
import com.scse.curriculum.cohort.dto.CreateCohortRequest;
import com.scse.curriculum.program.service.ProgramServiceImpl;
import com.scse.curriculum.program.dto.CloneProgramRequest;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.cohort.service.CohortOperationalGuard;
import com.scse.curriculum.program.repository.ProgramRepository;
import com.scse.curriculum.syllabus.dto.SyllabusResponse;
import com.scse.curriculum.syllabus.importer.dto.*;
import com.scse.curriculum.syllabus.importer.parser.*;
import com.scse.curriculum.syllabus.pdf.*;
import com.scse.curriculum.syllabus.service.*;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.syllabus.template.TargetTemplateProfileResolver;
import com.scse.curriculum.syllabus.word.SyllabusWordRenderer;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.user.repository.UserAccountRepository;
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
import static org.mockito.Mockito.*;

/** Actual DB/service/exports, strictly copy-only and rolled back. No HTTP server or scheduled work. */
@EnabledIfSystemProperty(named="neCe.copy",matches="curriculum_ne_ce_test_20261012")
@SpringBootTest(classes=SharedProgramCopyIntegrationTest.Config.class,webEnvironment=SpringBootTest.WebEnvironment.NONE)
class SharedProgramCopyIntegrationTest {
    private static final Path ROOT=Path.of("../../.workflow-checks/ne-ce-extension-20261012");
    @Configuration @EnableAutoConfiguration @EntityScan("com.scse.curriculum") @EnableJpaRepositories("com.scse.curriculum")
    @Import({CurrentUserService.class,SyllabusAccessService.class,TargetTemplateProfileResolver.class,SyllabusPdfDataLoader.class,
            SyllabusIdentityService.class,CohortOperationalGuard.class,SyllabusPdfParser.class,PdfProgramDocumentParser.class})
    static class Config {
        static Object service(Class<?> type,ConfigurableListableBeanFactory beans)throws Exception {
            var constructor=type.getConstructors()[0];var classes=constructor.getParameterTypes();var generic=constructor.getGenericParameterTypes();
            Object[] arguments=new Object[classes.length];
            for(int i=0;i<classes.length;i++) {
                if(classes[i]==List.class) arguments[i]=generic[i].getTypeName().contains("ProgramDocumentParser")
                        ? List.of(beans.getBean(PdfProgramDocumentParser.class)):List.of(beans.getBean(SyllabusPdfParser.class));
                else arguments[i]=beans.getBeanNamesForType(classes[i]).length>0?beans.getBean(classes[i]):mock(classes[i]);
            }
            return constructor.newInstance(arguments);
        }
        @Bean SyllabusImportServiceImpl importer(ConfigurableListableBeanFactory beans)throws Exception {return (SyllabusImportServiceImpl)service(SyllabusImportServiceImpl.class,beans);}
        @Bean SyllabusServiceImpl syllabuses(ConfigurableListableBeanFactory beans)throws Exception {return (SyllabusServiceImpl)service(SyllabusServiceImpl.class,beans);}
        @Bean CohortServiceImpl cohortService(ConfigurableListableBeanFactory beans)throws Exception {return (CohortServiceImpl)service(CohortServiceImpl.class,beans);}
        @Bean ProgramServiceImpl programService(ConfigurableListableBeanFactory beans)throws Exception {return (ProgramServiceImpl)service(ProgramServiceImpl.class,beans);}
    }
    @DynamicPropertySource static void copyOnly(DynamicPropertyRegistry properties)throws Exception {
        var source=new Properties();try(var stream=Files.newInputStream(Path.of("src/main/resources/application.properties"))){source.load(stream);}
        String url=source.getProperty("spring.datasource.url").replace("/curriculum_iu?","/curriculum_ne_ce_test_20261012?");
        assertTrue(url.contains("/curriculum_ne_ce_test_20261012?"));assertTrue(Set.of("localhost","127.0.0.1","::1").contains(java.net.URI.create(url.substring(5)).getHost()));
        properties.add("spring.datasource.url",()->url);properties.add("spring.datasource.username",()->source.getProperty("spring.datasource.username"));
        properties.add("spring.datasource.password",()->source.getProperty("spring.datasource.password"));
        properties.add("spring.jpa.hibernate.ddl-auto",()->"none");properties.add("spring.sql.init.mode",()->"never");
        properties.add("spring.jpa.show-sql",()->"false");properties.add("spring.flyway.enabled",()->"false");properties.add("spring.liquibase.enabled",()->"false");
        properties.add("logging.level.org.hibernate.SQL",()->"OFF");properties.add("logging.level.org.hibernate.orm.jdbc.bind",()->"OFF");
        properties.add("app.mail.enabled",()->"false");
    }
    @Autowired SyllabusImportServiceImpl importer;
    @Autowired SyllabusServiceImpl syllabuses;
    @Autowired SyllabusPdfDataLoader loader;
    @Autowired UserAccountRepository users;
    @Autowired ProgramRepository programs;
    @Autowired CohortRepository cohorts;
    @Autowired SyllabusRepository repository;
    @Autowired PlatformTransactionManager transactions;
    @Autowired SyllabusPdfParser sourceParser;
    @Autowired CohortServiceImpl cohortService;
    @Autowired ProgramServiceImpl programService;
    @Autowired CourseProgramRepository assignments;

    @Test void every2021SyllabusPersistsUnderItsOwnProgramAndCommonCoursesRemainSeparate() throws Exception {
        var json=new ObjectMapper().findAndRegisterModules();var proofs=json.createArrayNode();
        var transaction=new TransactionTemplate(transactions);
        var renderer=new SyllabusPdfRenderer(new SyllabusPdfFontProvider("",""),json,"VIETNAM NATIONAL UNIVERSITY HCMC - INTERNATIONAL UNIVERSITY","SCHOOL OF COMPUTER SCIENCE AND ENGINEERING");
        try {
            transaction.executeWithoutResult(status->{
                try {
                    var admin=users.findAll().stream().filter(user->user.getRole()==UserRole.ADMIN).findFirst().orElseThrow();
                    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin.getUsername(),null,List.of()));
                    var common=new HashMap<String,Integer>();
                    for(String family:List.of("NE","CE")) {
                        var program=programs.findByCode(family+"-2021").orElseThrow();
                        var cohort=cohorts.findAll().stream().filter(c->c.getProgram().getId().equals(program.getId())&&c.getEntryYear()==2021).findFirst().orElseThrow();
                        var preview=json.readValue(ROOT.resolve(family+"2021-real-preview.json").toFile(),BulkSyllabusImportPreviewResponse.class);
                        int count=0;
                        for(var item:preview.getItems()) {
                            var request=new BulkConfirmSyllabusImportRequest();request.setProgramId(program.getId());request.setCohortId(cohort.getId());request.setData(item.getPreview().getData());
                            request.setOriginalFileName("IT Program Specification IT - Full1.pdf");request.setOriginalFileType("application/pdf");
                            var saved=importer.confirmBulkItem(request);var read=syllabuses.getByIdForEdit(saved.getId());
                            assertEquals("SOURCE_TEMPLATE",read.getTargetTemplateProfile());assertEquals(repository.findById(saved.getId()).orElseThrow().getNotes(),read.getNotes());
                            var stored=repository.findById(saved.getId()).orElseThrow();
                            assertEquals(program.getCode(),stored.getProgram());assertEquals(cohort.getName(),stored.getAcademicYear());
                            assertEquals(item.getPreview().getData().getClos().size(),read.getClos().size());count++;
                            var row=proofs.addObject();row.put("cohort",cohort.getName());row.put("code",request.getData().getSourceCourseCode());row.put("syllabusId",saved.getId());
                            if("EN008IU".equals(request.getData().getSourceCourseCode())) {
                                common.put(family,saved.getId());var export=loader.load(saved.getId());
                                Files.write(ROOT.resolve(family+"2021-EN008IU.pdf"),renderer.render(export,SyllabusPdfMode.EXPORT));
                                Files.write(ROOT.resolve(family+"2021-EN008IU.docx"),new SyllabusWordRenderer(renderer).render(export));
                                json.writerWithDefaultPrettyPrinter().writeValue(ROOT.resolve(family+"2021-EN008IU-response.json").toFile(),read);
                            }
                        }
                        assertEquals(family.equals("NE")?52:56,count);
                    }
                    assertNotEquals(common.get("NE"),common.get("CE"));assertEquals(108,proofs.size());
                    json.writerWithDefaultPrettyPrinter().writeValue(ROOT.resolve("copy-108-confirm-read-proof.json").toFile(),proofs);
                }catch(Exception error){throw new IllegalStateException(error);}finally{status.setRollbackOnly();}
            });
        }finally{SecurityContextHolder.clearContext();}
    }

    @Test void realNew2027PreviewConfirmReadAndExportsUseTheSameCsServices() throws Exception {
        var json=new ObjectMapper().findAndRegisterModules();var paths=json.readTree(ROOT.resolve("sources.json").toFile());
        var field=SyllabusPdfParser.class.getDeclaredField("ocrFallback");field.setAccessible(true);Object original=field.get(sourceParser);
        field.set(sourceParser,mock(field.getType(),call->{throw new java.io.IOException("Unreviewed OCR disabled in isolated proof");}));
        var evidence=json.createArrayNode();
        try {
            for(String family:List.of("NE","CE")) {
                new TransactionTemplate(transactions).executeWithoutResult(status->{
                    try {
                        var admin=users.findAll().stream().filter(user->user.getRole()==UserRole.ADMIN).findFirst().orElseThrow();
                        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin.getUsername(),null,List.of()));
                        var program=programs.findByCode(family+"-2026").orElseThrow();
                        var cohort=cohorts.findAll().stream().filter(c->c.getProgram().getId().equals(program.getId())&&c.getEntryYear()==2027).findFirst().orElseThrow();
                        byte[] bytes=Files.readAllBytes(Path.of(paths.path("IT2026").asText()));
                        var preview=importer.previewBulk(new org.springframework.mock.web.MockMultipartFile("file","IT2026.pdf","application/pdf",bytes),program.getId(),cohort.getId());
                        assertEquals("NEW_2027",preview.getTargetTemplateProfile());
                        var item=preview.getItems().stream().filter(i->i.getPreview().isValid()&&"EN008IU".equals(i.getPreview().getData().getSourceCourseCode())).findFirst().orElseThrow();
                        var request=new BulkConfirmSyllabusImportRequest();request.setProgramId(program.getId());request.setCohortId(cohort.getId());
                        request.setData(item.getPreview().getData());request.setSourceSnapshotId(item.getSourceSnapshotId());
                        request.setOriginalFileName("IT2026.pdf");request.setOriginalFileType("application/pdf");
                        var saved=importer.confirmBulkItem(request);var read=syllabuses.getByIdForEdit(saved.getId());
                        assertEquals("NEW_2027",read.getTargetTemplateProfile());assertEquals(saved.getNotes(),read.getNotes());
                        var notes=json.readTree(read.getNotes());assertTrue(notes.path("sourceContentRows").size()>0);
                        assertTrue(notes.path("sourceContentRows").get(0).path("weekNumber").isNull());assertTrue(read.getTopics().isEmpty());
                        var renderer=new SyllabusPdfRenderer(new SyllabusPdfFontProvider("",""),json,"VIETNAM NATIONAL UNIVERSITY HCMC - INTERNATIONAL UNIVERSITY","SCHOOL OF COMPUTER SCIENCE AND ENGINEERING");
                        var export=loader.load(saved.getId());
                        Files.write(ROOT.resolve(family+"2027-EN008IU.pdf"),renderer.render(export,SyllabusPdfMode.EXPORT));
                        Files.write(ROOT.resolve(family+"2027-EN008IU.docx"),new SyllabusWordRenderer(renderer).render(export));
                        json.writerWithDefaultPrettyPrinter().writeValue(ROOT.resolve(family+"2027-EN008IU-response.json").toFile(),read);
                        evidence.addObject().put("cohort",cohort.getName()).put("contentRows",notes.path("sourceContentRows").size()).put("snapshotId",item.getSourceSnapshotId());
                        assertThrows(IllegalStateException.class,()->importer.confirmBulkItem(request));
                    }catch(Exception error){throw new IllegalStateException(error);}finally{status.setRollbackOnly();}
                });
            }
            json.writerWithDefaultPrettyPrinter().writeValue(ROOT.resolve("copy-new2027-roundtrip-proof.json").toFile(),evidence);
        }finally{field.set(sourceParser,original);SecurityContextHolder.clearContext();}
    }

    @Test void futureCohortsAndCurriculumCopiesUseExistingServicesWithoutCreatingAcademicContent() {
        new TransactionTemplate(transactions).executeWithoutResult(status->{
            try {
                for(String key:List.of("NE","CE")) {
                    var program=programs.findByCode(key+"-2026").orElseThrow();
                    var source=cohorts.findAll().stream().filter(c->c.getProgram().getId().equals(program.getId())&&c.getEntryYear()==2027).findFirst().orElseThrow();
                    var create=new CreateCohortRequest();create.setProgramId(program.getId());create.setEntryYear(2028);create.setName("client cannot replace canonical name");
                    var created=cohortService.create(create);assertEquals(key+"2028",created.getName());
                    var target=cohorts.findById(created.getId()).orElseThrow();assertEquals("NEW_2027",new TargetTemplateProfileResolver().resolve(target));
                    var clone=new CloneProgramRequest();clone.setSourceCohortId(source.getId());clone.setTargetCohortId(target.getId());
                    var result=programService.cloneToCohort(program.getId(),clone);
                    assertEquals(key.equals("NE")?79:73,result.getCopiedCount());
                    var copied=assignments.findByProgramIdAndCohortIdWithRelations(program.getId(),target.getId());
                    assertEquals(result.getCopiedCount(),copied.size());assertTrue(copied.stream().allMatch(row->row.getSyllabus()==null));
                }
            }finally{status.setRollbackOnly();}
        });
    }
}
