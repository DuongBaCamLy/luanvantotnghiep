package com.scse.curriculum.syllabus.word;

import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.syllabus.dto.CreateSyllabusRequest;
import com.scse.curriculum.syllabus.dto.SyllabusResponse;
import com.scse.curriculum.syllabus.service.SyllabusService;
import com.scse.curriculum.syllabus.source.repository.SyllabusSourceSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class SyllabusWordServiceImpl implements SyllabusWordService {
    private final SyllabusSourceSnapshotRepository snapshots;
    private final SyllabusService syllabuses;

    public SyllabusWordResult original(Integer syllabusId) {
        SyllabusResponse syllabus=syllabuses.getById(syllabusId);
        var snapshot=snapshots.findBySyllabus_Id(syllabusId)
                .orElseThrow(() -> new ResourceNotFoundException("This syllabus has no original Word source snapshot."));
        return new SyllabusWordResult(snapshot.getContent(),filename(syllabus,"Original"),snapshot.getSha256());
    }

    public SyllabusWordResult current(Integer syllabusId) {
        SyllabusResponse syllabus=syllabuses.getById(syllabusId);
        var snapshot=snapshots.findBySyllabus_Id(syllabusId)
                .orElseThrow(() -> new ResourceNotFoundException("This syllabus has no Word source template."));
        try(XWPFDocument doc=new XWPFDocument(new ByteArrayInputStream(snapshot.getContent()));
            ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            patchCourseCode(doc,syllabus.getCourseCode());
            patchGeneralInfo(doc,syllabus);
            patchClos(doc,syllabus.getClos());
            doc.write(out); byte[] bytes=out.toByteArray();
            return new SyllabusWordResult(bytes,filename(syllabus,"Current"),sha256(bytes));
        } catch(IOException exception) { throw new IllegalStateException("Cannot export the current Word document",exception); }
    }

    private void patchCourseCode(XWPFDocument doc,String code) {
        Pattern anchor=Pattern.compile("(?i)((?:Course\\s*Code|M[aã]\\s*h[oọ]c\\s*ph[aầ]n)\\s*:\\s*)([A-Z]{2,4}\\s*[- ]?\\d{2,3}(?:IU|WE)?)");
        for(XWPFParagraph p:doc.getParagraphs()) if(anchor.matcher(p.getText()).find()) {
            replaceParagraph(p,anchor.matcher(p.getText()).replaceFirst("$1"+java.util.regex.Matcher.quoteReplacement(code))); return;
        }
    }
    private void patchGeneralInfo(XWPFDocument doc,SyllabusResponse syllabus) {
        for(XWPFTable table:doc.getTables()) for(XWPFTableRow row:table.getRows()) {
            if(row.getTableCells().size()<2) continue;
            String label=ascii(row.getCell(0).getText()).toLowerCase(Locale.ROOT);
            String value=null;
            if(label.contains("course name")||label.contains("ten hoc phan")) value=syllabus.getCourseName();
            else if(label.contains("course designation")||label.contains("mo ta hoc phan")) value=syllabus.getCourseDesignation();
            else if(label.contains("language")||label.contains("ngon ngu")) value=syllabus.getLanguage();
            else if(label.contains("prerequisite")||label.contains("tien quyet")) value=syllabus.getPrerequisites();
            if(value!=null&&!value.isBlank()) replaceCell(row.getCell(1),value);
        }
    }
    private void patchClos(XWPFDocument doc,List<CreateSyllabusRequest.CloDTO> clos) {
        if(clos==null||clos.isEmpty()) return;
        Map<String,String> byCode=new LinkedHashMap<>();
        for(var clo:clos) if(clo.getCode()!=null&&clo.getDescription()!=null) byCode.put(clo.getCode().replaceAll("\\s","").toUpperCase(),clo.getDescription());
        for(XWPFTable table:doc.getTables()) for(XWPFTableRow row:table.getRows()) {
            for(int i=0;i<row.getTableCells().size();i++) {
                String code=row.getCell(i).getText().replaceAll("\\s","").toUpperCase();
                if(byCode.containsKey(code)&&i+1<row.getTableCells().size()) replaceCell(row.getCell(i+1),byCode.get(code));
            }
        }
    }
    private void replaceCell(XWPFTableCell cell,String value) {
        if(cell.getParagraphs().isEmpty()) cell.addParagraph();
        replaceParagraph(cell.getParagraphs().get(0),value);
        for(int i=1;i<cell.getParagraphs().size();i++) replaceParagraph(cell.getParagraphs().get(i),"");
    }
    private void replaceParagraph(XWPFParagraph paragraph,String value) {
        List<XWPFRun> runs=paragraph.getRuns();
        if(runs.isEmpty()) paragraph.createRun().setText(value);
        else { runs.get(0).setText(value,0); for(int i=1;i<runs.size();i++) runs.get(i).setText("",0); }
    }
    private String filename(SyllabusResponse s,String variant) {
        String raw="Syllabus_"+s.getCourseCode()+"_"+variant;
        return Normalizer.normalize(raw,Normalizer.Form.NFD).replaceAll("\\p{M}+","").replaceAll("[^A-Za-z0-9._-]+","_")+".docx";
    }
    private String ascii(String value){return Normalizer.normalize(value==null?"":value,Normalizer.Form.NFD).replaceAll("\\p{M}","");}
    private String sha256(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception e){throw new IllegalStateException(e);}}
}
