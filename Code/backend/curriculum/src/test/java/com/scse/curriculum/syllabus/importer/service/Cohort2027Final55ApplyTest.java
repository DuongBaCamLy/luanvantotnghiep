package com.scse.curriculum.syllabus.importer.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.scse.curriculum.syllabus.importer.dto.*;
import com.scse.curriculum.syllabus.dto.CreateSyllabusRequest;
import com.scse.curriculum.syllabus.service.SyllabusServiceImpl;
import com.scse.curriculum.syllabus.pdf.*;
import com.scse.curriculum.syllabus.word.SyllabusWordRenderer;
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

/** Opt-in guarded runner; local apply additionally requires the verified copy gate. */
@EnabledIfSystemProperty(named="cs2027.final55",matches="trial-copy|apply-copy|apply-local|verify-copy|verify-local")
@SpringBootTest(classes=Cohort2027CopyReadVerificationTest.ReadConfig.class,webEnvironment=SpringBootTest.WebEnvironment.NONE)
class Cohort2027Final55ApplyTest {
    static final Path ROOT=Cohort2027Final55VerificationTest.ROOT;
    @DynamicPropertySource static void copyOnly(DynamicPropertyRegistry p)throws Exception {
        Cohort2027Final55VerificationTest.copyOnly(p);
        if(System.getProperty("cs2027.final55","").endsWith("-local")) {
            var properties=new Properties();try(var in=Files.newInputStream(Path.of("src/main/resources/application.properties"))){properties.load(in);}
            String url=properties.getProperty("spring.datasource.url");
            assertTrue(url.contains("/curriculum_iu?"));
            assertTrue(Set.of("localhost","127.0.0.1","::1").contains(java.net.URI.create(url.substring(5)).getHost()));
            p.add("spring.datasource.url",()->url);
        }
    }
    @Autowired SyllabusImportServiceImpl importer;
    @Autowired SyllabusServiceImpl syllabi;
    @Autowired UserAccountRepository users;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcTemplate jdbc;
    @Autowired SyllabusPdfDataLoader loader;
    final ObjectMapper json=new ObjectMapper().findAndRegisterModules();

    @Test void applyExactPreparedSourceOnCopy()throws Exception {
        String mode=System.getProperty("cs2027.final55");boolean trial=mode.equals("trial-copy");
        boolean local=mode.endsWith("-local");String phase=local?"local":"copy";
        String database=local?"curriculum_iu":Cohort2027Final55VerificationTest.COPY;
        assertEquals(database,jdbc.queryForObject("SELECT DATABASE()",String.class));
        var restore=json.readTree(ROOT.resolve("final55-copy-restore-verification.json").toFile());
        assertEquals(Cohort2027Final55VerificationTest.COPY,restore.path("database").asText());
        var localHashes=json.readTree(ROOT.resolve("final55-local-before-fingerprints.json").toFile());
        var copyHashes=json.readTree(ROOT.resolve("final55-copy-before-fingerprints.json").toFile());
        assertEquals(localHashes.path("tables"),copyHashes.path("tables"));assertEquals(localHashes.path("triggers"),copyHashes.path("triggers"));
        var plan=json.readTree(ROOT.resolve("final55-verified-plan.json").toFile());
        var fieldChecks=json.readTree(ROOT.resolve("final55-source-field-checks.json").toFile());assertEquals(55,fieldChecks.size());
        for(var row:fieldChecks)assertEquals(0,row.path("unmatched").size(),row.path("courseCode").asText());
        String planDigest=digest(Files.readAllBytes(ROOT.resolve("final55-verified-plan.json")));
        if(mode.startsWith("verify-") || jdbc.queryForObject("SELECT COUNT(*) FROM syllabus",Integer.class)==151) {
            verifyNoOp(plan,phase,database,planDigest);return;
        }
        if(local) {
            var gate=json.readTree(ROOT.resolve("final55-local-gate.json").toFile());
            for(String key:List.of("privateLocalConfirmedByUser","backupRestored","copyTrialRolledBack","copyApplied","copyIntegrityPassed","copyIdempotencyPassed","sourceVerified","all55UiAndExportsPassed","backendRegressionPassed","frontendBuildPassed","copyRollbackPassed"))
                assertTrue(gate.path(key).asBoolean(false),"Local gate failed: "+key);
            assertEquals(planDigest,gate.path("planSha256").asText());
            var code=gate.path("codeHashes").fields();while(code.hasNext()) {
                var file=code.next();assertEquals(file.getValue().asText(),digest(Files.readAllBytes(Path.of("../..").resolve(file.getKey()))),"Code changed after copy verification: "+file.getKey());
            }
            var current=json.readTree(ROOT.resolve("final55-local-preapply-fingerprints.json").toFile());
            assertEquals(localHashes.path("tables"),current.path("tables"));assertEquals(localHashes.path("triggers"),current.path("triggers"));
        }
        assertEquals(109,jdbc.queryForObject("SELECT COUNT(*) FROM syllabus",Integer.class));
        var source=json.readTree(ROOT.resolve("source_manifest.json").toFile()).path("CS");
        var bytes=Files.readAllBytes(Path.of(source.path("path").asText()));
        assertEquals(plan.path("sourceSha256").asText(),digest(bytes));
        var responses=new ArrayList<Object>();var exports=new ArrayList<Object>();var receipts=json.createArrayNode();
        Path folder=ROOT.resolve("final55-"+(trial?"trial":phase)+"-export");Files.createDirectories(folder);
        var renderer=new SyllabusPdfRenderer(new SyllabusPdfFontProvider("",""),json,
                "VIETNAM NATIONAL UNIVERSITY HCMC - INTERNATIONAL UNIVERSITY","SCHOOL OF COMPUTER SCIENCE AND ENGINEERING");
        try {
            new TransactionTemplate(transactions).executeWithoutResult(status->{
                try {
                    var admin=users.findAll().stream().filter(u->u.getRole()==UserRole.ADMIN).findFirst().orElseThrow();
                    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin.getUsername(),null,List.of()));
                    var assignments=jdbc.queryForList("SELECT * FROM course_program WHERE program_id=1 AND cohort_id=15 ORDER BY id FOR UPDATE");
                    assertEquals(55,assignments.size());
                    for(var entry:plan.path("entries")) {
                        var cp=jdbc.queryForMap("SELECT * FROM course_program WHERE id=? FOR UPDATE",entry.path("courseProgramId").asInt());
                        assertEquals(entry.path("courseId").asInt(),((Number)cp.get("course_id")).intValue());
                        assertEquals(1,((Number)cp.get("program_id")).intValue());assertEquals(15,((Number)cp.get("cohort_id")).intValue());
                        if(entry.path("operation").asText().equals("INSERT"))assertNull(cp.get("syllabus_id"));
                        else {
                            int id=entry.path("syllabusId").asInt();assertEquals(id,((Number)cp.get("syllabus_id")).intValue());
                            assertEquals(entry.path("beforeRow"),tree(jdbc.queryForMap("SELECT * FROM syllabus WHERE id=? FOR UPDATE",id)),"Protected draft was changed: "+id);
                            assertEquals(entry.path("beforeResponse"),tree(syllabi.getById(id)),"Protected child content was changed: "+id);
                        }
                    }
                    var preview=importer.previewBulk(new MockMultipartFile("file",Path.of(source.path("path").asText()).getFileName().toString(),"application/pdf",bytes),1,15);
                    assertEquals(55,preview.getItems().size());
                    int inserted=0,corrected=0;
                    for(var entry:plan.path("entries")) {
                        String code=entry.path("courseCode").asText();var item=preview.getItems().stream().filter(v->v.getPreview().getData().getSourceCourseCode().split("/")[0].equals(code)).findFirst().orElseThrow();
                        assertTrue(item.getPreview().isValid(),code);assertEquals(entry.path("data"),tree(item.getPreview().getData()),"Source preview changed: "+code);
                        assertNotNull(item.getSourceSnapshotId(),code+" source snapshot missing");
                        int id;
                        if(entry.path("operation").asText().equals("INSERT")) {
                            var request=new BulkConfirmSyllabusImportRequest();request.setProgramId(1);request.setCohortId(15);request.setImportMode("CREATE");
                            request.setData(item.getPreview().getData());request.setSourceSnapshotId(item.getSourceSnapshotId());
                            request.setOriginalFileName(preview.getFileName());request.setOriginalFileType("application/pdf");
                            id=importer.confirmBulkItem(request).getId();inserted++;
                        } else {
                            id=entry.path("syllabusId").asInt();var before=syllabi.getById(id);
                            var request=json.treeToValue(entry.path("request"),CreateSyllabusRequest.class);
                            var saved=syllabi.update(id,request);assertEquals(id,saved.getId());corrected++;
                            audit("syllabus",id,json.valueToTree(before),json.valueToTree(saved));
                            reconcileVerifiedContributions(id,item.getPreview().getData());
                            int sourceCount=jdbc.queryForObject("SELECT COUNT(*) FROM syllabus_source_snapshot WHERE syllabus_id=?",Integer.class,id);
                            if(sourceCount==0)assertEquals(1,jdbc.update("UPDATE syllabus_source_snapshot SET syllabus_id=? WHERE id=? AND syllabus_id IS NULL",id,item.getSourceSnapshotId()));
                        }
                        var response=syllabi.getById(id);assertEquals(code,response.getCourseCode());assertEquals("NEW_2027",response.getTargetTemplateProfile());assertEquals("DRAFT",response.getStatus());
                        if(entry.path("operation").asText().equals("INSERT"))audit("syllabus",id,json.nullNode(),tree(response));
                        assertEquals(id,jdbc.queryForObject("SELECT syllabus_id FROM course_program WHERE id=?",Integer.class,entry.path("courseProgramId").asInt()));
                        var expected=entry.path("expectedNotes");assertEquals(expected,json.readTree(response.getNotes()),code+" notes must roundtrip without dropping source content");
                        assertEquals(entry.path("data").path("clos").size(),response.getClos().size(),code);
                        assertEquals(entry.path("data").path("assessments").size(),response.getAssessments().size(),code);
                        var document=loader.load(id);String base=code+"-"+id;
                        Files.write(folder.resolve(base+".pdf"),renderer.render(document,SyllabusPdfMode.EXPORT));
                        Files.write(folder.resolve(base+".docx"),new SyllabusWordRenderer(renderer).render(document));
                        responses.add(response);exports.add(document);
                        var receipt=receipts.addObject();receipt.put("courseCode",code);receipt.put("courseProgramId",entry.path("courseProgramId").asInt());receipt.put("syllabusId",id);receipt.put("operation",entry.path("operation").asText());
                        var snapshot=jdbc.queryForMap("SELECT content,sha256,source_document_id FROM syllabus_source_snapshot WHERE id=?",item.getSourceSnapshotId());
                        var snapshotBytes=(byte[])snapshot.get("content");assertEquals(snapshot.get("sha256"),digest(snapshotBytes));
                        try(var pdf=org.apache.pdfbox.Loader.loadPDF(snapshotBytes)) {
                            assertEquals(entry.path("endPage").asInt()-entry.path("startPage").asInt()+1,pdf.getNumberOfPages());
                            assertNotNull(pdf.getPage(0).getResources(),code+" missing inherited PDF resources");
                        }
                        receipt.put("sourceSnapshotId",item.getSourceSnapshotId());receipt.put("sourceSnapshotBytes",snapshotBytes.length);
                    }
                    assertEquals(42,inserted);assertEquals(13,corrected);assertEquals(151,jdbc.queryForObject("SELECT COUNT(*) FROM syllabus",Integer.class));
                    assertEquals(55,jdbc.queryForObject("SELECT COUNT(DISTINCT syllabus_id) FROM course_program WHERE program_id=1 AND cohort_id=15",Integer.class));
                    var afterAssignments=jdbc.queryForList("SELECT * FROM course_program WHERE program_id=1 AND cohort_id=15 ORDER BY id");
                    for(int index=0;index<assignments.size();index++) {var before=new LinkedHashMap<>(assignments.get(index));var after=new LinkedHashMap<>(afterAssignments.get(index));before.remove("syllabus_id");after.remove("syllabus_id");assertEquals(before,after,"Curriculum fields changed");}
                    assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM topic_clo tc JOIN topic t ON t.id=tc.topic_id JOIN clo c ON c.id=tc.clo_id WHERE t.syllabus_id<>c.syllabus_id",Integer.class));
                    assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM assessment_clo ac JOIN assessment_component a ON a.id=ac.assessment_component_id JOIN clo c ON c.id=ac.clo_id WHERE a.syllabus_id<>c.syllabus_id",Integer.class));
                    json.writerWithDefaultPrettyPrinter().writeValue(ROOT.resolve("final55-"+(trial?"trial":phase)+"-responses.json").toFile(),responses);
                    json.writerWithDefaultPrettyPrinter().writeValue(ROOT.resolve("final55-"+(trial?"trial":phase)+"-export-inputs.json").toFile(),exports);
                    var receipt=json.createObjectNode();receipt.put("database",database);receipt.put("trialRolledBack",trial);receipt.put("planSha256",planDigest);receipt.set("entries",receipts);
                    json.writerWithDefaultPrettyPrinter().writeValue(ROOT.resolve(trial?"final55-trial-receipt.json":"final55-"+phase+"-apply-receipt.json").toFile(),receipt);
                }catch(Exception error){throw new IllegalStateException(error);}
                finally {if(trial)status.setRollbackOnly();}
            });
        }finally{SecurityContextHolder.clearContext();}
        assertEquals(trial?109:151,jdbc.queryForObject("SELECT COUNT(*) FROM syllabus",Integer.class));
        assertEquals(trial?13:55,jdbc.queryForObject("SELECT COUNT(*) FROM course_program WHERE program_id=1 AND cohort_id=15 AND syllabus_id IS NOT NULL",Integer.class));
    }
    private void reconcileVerifiedContributions(int id,SyllabusImportData data)throws Exception {
        var before=jdbc.queryForList("SELECT ac.* FROM assessment_clo ac JOIN assessment_component a ON a.id=ac.assessment_component_id WHERE a.syllabus_id=? ORDER BY ac.assessment_component_id,ac.clo_id",id);
        var expected=new TreeMap<String,Map<String,Object>>();
        for(var item:data.getAssessmentCloMappings()) {
            int assessment=jdbc.queryForObject("SELECT id FROM assessment_component WHERE syllabus_id=? AND order_index=?",Integer.class,id,item.getAssessmentIndex());
            int clo=jdbc.queryForObject("SELECT id FROM clo WHERE syllabus_id=? AND code=?",Integer.class,id,item.getCloCode());
            expected.put(assessment+":"+clo,Map.of("assessment_component_id",assessment,"clo_id",clo,"contribution_percent",item.getPercentage().floatValue()));
        }
        for(var row:before) {
            String key=row.get("assessment_component_id")+":"+row.get("clo_id");var target=expected.remove(key);
            if(target==null){jdbc.update("DELETE FROM assessment_clo WHERE assessment_component_id=? AND clo_id=?",row.get("assessment_component_id"),row.get("clo_id"));audit("assessment_clo",((Number)row.get("assessment_component_id")).intValue(),json.valueToTree(row),json.nullNode());}
            else if(Float.compare(((Number)target.get("contribution_percent")).floatValue(),((Number)row.get("contribution_percent")).floatValue())!=0) {
                jdbc.update("UPDATE assessment_clo SET contribution_percent=? WHERE assessment_component_id=? AND clo_id=?",target.get("contribution_percent"),target.get("assessment_component_id"),target.get("clo_id"));audit("assessment_clo",((Number)row.get("assessment_component_id")).intValue(),json.valueToTree(row),json.valueToTree(target));
            }
        }
        for(var row:expected.values()){jdbc.update("INSERT INTO assessment_clo(assessment_component_id,clo_id,contribution_percent) VALUES(?,?,?)",row.get("assessment_component_id"),row.get("clo_id"),row.get("contribution_percent"));audit("assessment_clo",((Number)row.get("assessment_component_id")).intValue(),json.nullNode(),json.valueToTree(row));}
    }
    private void audit(String table,int id,JsonNode before,JsonNode after)throws Exception {
        int actor=jdbc.queryForObject("SELECT user_id FROM audit_actor_snapshot WHERE username='system' AND actor_type='LEGACY_SYSTEM'",Integer.class);
        String action=before.isNull()?"CREATE":after.isNull()?"DELETE":"UPDATE";
        jdbc.update("INSERT INTO audit_log(table_name,record_id,action,old_value,new_value,changed_by,changed_at,user_agent) VALUES(?,?,?,?,?,?,NOW(),?)",table,id,action,json.writeValueAsString(before),json.writeValueAsString(after),actor,"CS2027_FINAL55_VERIFIED_SOURCE_20261011 / "+System.getProperty("cs2027.final55"));
    }
    static String digest(byte[] bytes)throws Exception {return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}
    // JSON roundtrip normalizes JDBC Long/Integer and Float/Double node representations without changing values.
    private JsonNode tree(Object value)throws Exception{return json.readTree(json.writeValueAsBytes(value));}
    private void verifyNoOp(JsonNode plan,String phase,String database,String planDigest)throws Exception {
        var receipt=json.readTree(ROOT.resolve("final55-"+phase+"-apply-receipt.json").toFile());
        assertEquals(planDigest,receipt.path("planSha256").asText());
        assertEquals(database,receipt.path("database").asText());
        assertEquals(151,jdbc.queryForObject("SELECT COUNT(*) FROM syllabus",Integer.class));
        long auditBefore=jdbc.queryForObject("SELECT COUNT(*) FROM audit_log",Long.class);
        var admin=users.findAll().stream().filter(u->u.getRole()==UserRole.ADMIN).findFirst().orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin.getUsername(),null,List.of()));
        try {
            for(var entry:plan.path("entries")) {
                var saved=java.util.stream.StreamSupport.stream(receipt.path("entries").spliterator(),false)
                        .filter(e->e.path("courseCode").equals(entry.path("courseCode"))).findFirst().orElseThrow();
                int id=saved.path("syllabusId").asInt();
                assertEquals(id,jdbc.queryForObject("SELECT syllabus_id FROM course_program WHERE id=?",Integer.class,entry.path("courseProgramId").asInt()));
                var response=syllabi.getById(id);assertEquals("DRAFT",response.getStatus());assertEquals("NEW_2027",response.getTargetTemplateProfile());
                assertEquals(entry.path("expectedNotes"),json.readTree(response.getNotes()));
                if(entry.path("operation").asText().equals("CORRECTION"))assertEquals(entry.path("syllabusId").asInt(),id);
            }
        }finally{SecurityContextHolder.clearContext();}
        assertEquals(auditBefore,jdbc.queryForObject("SELECT COUNT(*) FROM audit_log",Long.class));
        json.writerWithDefaultPrettyPrinter().writeValue(ROOT.resolve("final55-"+phase+"-idempotency-verification.json").toFile(),Map.of("database",database,"result","NO_OP","planSha256",planDigest,"verifiedSyllabuses",55,"auditEntriesAdded",0));
    }
}
