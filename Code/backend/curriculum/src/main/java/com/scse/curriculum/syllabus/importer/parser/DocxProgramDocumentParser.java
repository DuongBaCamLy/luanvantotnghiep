package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.syllabus.importer.dto.SyllabusImportIssue;
import lombok.RequiredArgsConstructor;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
@RequiredArgsConstructor
public class DocxProgramDocumentParser implements ProgramDocumentParser {
    private static final String MIME="application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final Pattern ENGLISH_ANCHOR=Pattern.compile("(?i)^\\s*Course\\s*Code\\s*:\\s*([A-Z]{2,4}\\s*[- ]?\\d{2,3}(?:IU|WE)?)");
    private static final Pattern VIETNAMESE_ANCHOR=Pattern.compile("(?i)^\\s*M[aã]\\s*h[oọ]c\\s*ph[aầ]n\\s*:\\s*([A-Z]{2,4}\\s*[- ]?\\d{2,3}(?:IU|WE)?)");
    private final SyllabusDocxParser syllabusParser;

    public boolean supports(String filename, String contentType, byte[] content) {
        return MIME.equalsIgnoreCase(contentType) && content.length > 4
                && content[0]=='P' && content[1]=='K' && !containsMacro(content);
    }
    public ParsedProgramDocument parse(byte[] content) throws IOException {
        // The official programme dossier embeds obfuscated fonts that legitimately
        // compress below POI's 1% default. Keep bomb protection enabled with a
        // lower ratio plus the strict upload and per-entry size limits.
        ZipSecureFile.setMinInflateRatio(0.001d);
        ZipSecureFile.setMaxEntrySize(100L * 1024 * 1024);
        try (XWPFDocument doc=new XWPFDocument(new ByteArrayInputStream(content))) {
            List<IBodyElement> body=doc.getBodyElements();
            List<Integer> englishStarts=new ArrayList<>(), vietnameseStarts=new ArrayList<>();
            for(int i=0;i<body.size();i++) {
                if(body.get(i) instanceof XWPFParagraph p) {
                    if(ENGLISH_ANCHOR.matcher(p.getText()).find()) englishStarts.add(i);
                    else if(VIETNAMESE_ANCHOR.matcher(p.getText()).find()) vietnameseStarts.add(i);
                }
            }
            // Bilingual program dossiers commonly contain a Vietnamese summary and a
            // complete English syllabus appendix. Prefer one complete section so a
            // course is never imported twice from the same source document.
            List<Integer> anchors=englishStarts.isEmpty()?vietnameseStarts:englishStarts;
            List<Integer> starts=new ArrayList<>();
            for(int anchor:anchors) {
                int start=anchor;
                for(int candidate=anchor-1;candidate>=Math.max(0,anchor-4);candidate--) {
                    if(body.get(candidate) instanceof XWPFParagraph paragraph
                            && paragraph.getText().matches("(?is).*Course\\s*Name\\s*:.*")) {
                        start=candidate;
                        break;
                    }
                }
                starts.add(start);
            }
            if(starts.isEmpty()) throw new IllegalArgumentException("No syllabus Course Code anchors were found in the DOCX.");
            List<ParsedItem> items=new ArrayList<>();
            for(int n=0;n<starts.size();n++) {
                int start=starts.get(n), end=n+1<starts.size()?starts.get(n+1)-1:body.size()-1;
                List<SyllabusImportIssue> issues=new ArrayList<>();
                var data=syllabusParser.parse(new ArrayList<>(body.subList(start,end+1)),issues);
                String map="{\"courseCode\":\"paragraph:0\",\"boundaryType\":\"OOXML_BODY_ELEMENT\"}";
                // Snapshot bytes are materialized lazily when the user confirms an
                // item. Preview therefore opens the 32 MB package only once.
                items.add(new ParsedItem(start,end,data,issues,null,map));
            }
            return new ParsedProgramDocument(body.size(),items);
        }
    }
    public byte[] snapshot(byte[] original,int start,int end) throws IOException {
        try(XWPFDocument copy=new XWPFDocument(new ByteArrayInputStream(original)); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            for(int i=copy.getBodyElements().size()-1;i>=0;i--) if(i<start||i>end) copy.removeBodyElement(i);
            copy.write(out); return out.toByteArray();
        }
    }
    private boolean containsMacro(byte[] bytes) {
        try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for(ZipEntry entry=zip.getNextEntry();entry!=null;entry=zip.getNextEntry()) {
                if(entry.getName().toLowerCase(Locale.ROOT).endsWith("vbaproject.bin")) return true;
            }
            return false;
        } catch(IOException invalidZip) {
            return true;
        }
    }
}
