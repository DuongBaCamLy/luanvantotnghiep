package com.scse.curriculum.syllabus.importer.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.apache.pdfbox.multipdf.Splitter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PdfProgramDocumentParser implements ProgramDocumentParser {
    private final SyllabusPdfParser parser;
    public boolean supports(String filename, String contentType, byte[] content) {
        return content.length >= 5 && new String(content, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")
                && "application/pdf".equalsIgnoreCase(contentType);
    }
    public ParsedProgramDocument parse(byte[] content) throws IOException {
        var batch = parser.parsePdfBatchWithMetadata(new ByteArrayInputStream(content));
        var sections = batch.sections();
        var items = sections.stream().map(s -> new ParsedItem(s.startPage(), s.endPage(),
                s.data(), s.issues(), null, null)).toList();
        return new ParsedProgramDocument(batch.pageCount(), items);
    }
    public ParsedProgramDocument parseFor2027(byte[] content) throws IOException {
        return parseScoped(content, true, 1, Integer.MAX_VALUE);
    }
    public ParsedProgramDocument parseScoped(byte[] content, boolean new2027, int firstPage, int lastPage) throws IOException {
        var batch = new2027
                ? firstPage == 1 && lastPage == Integer.MAX_VALUE
                    ? parser.parse2027PdfBatchWithMetadata(new ByteArrayInputStream(content))
                    : parser.parse2027PdfBatchWithMetadata(new ByteArrayInputStream(content), firstPage, lastPage)
                : parser.parsePdfBatchWithMetadata(new ByteArrayInputStream(content), firstPage, lastPage);
        if (!new2027) {
            // The shared English source wraps the labels in its general table.
            // Recover only these two scalar cells; flat text otherwise absorbs
            // course objectives, CLOs and footnotes into their values.
            try(var source=org.apache.pdfbox.Loader.loadPDF(content)) {
                for(var section:batch.sections()) for(int page=section.startPage();page<=Math.min(section.startPage()+1,section.endPage());page++) {
                    var scalar=new com.scse.curriculum.syllabus.importer.dto.SyllabusImportData();
                    Cohort2027PdfTableRecovery.recoverGeneral(Cohort2027PdfTableRecovery.cells(source.getPage(page-1)),scalar);
                    if(scalar.getTeachingMethods()!=null && !scalar.getTeachingMethods().isBlank())section.data().setTeachingMethods(scalar.getTeachingMethods());
                    if(scalar.getPrerequisites()!=null && !scalar.getPrerequisites().isBlank())section.data().setPrerequisites(scalar.getPrerequisites());
                }
            }
            return new ParsedProgramDocument(batch.pageCount(), batch.sections().stream()
                    .map(s -> new ParsedItem(s.startPage(), s.endPage(), s.data(), s.issues(), null, null)).toList());
        }
        var items=new java.util.ArrayList<ParsedItem>();
        try(var source=org.apache.pdfbox.Loader.loadPDF(content)) {
            for(var section:batch.sections()) {
                // A shallow page import leaves link destinations pointing into the source page tree.
                // Splitter copies annotations and remaps destinations, preserving page resources
                // without embedding the entire programme PDF in each syllabus snapshot.
                var splitter=new Splitter();
                splitter.setStartPage(section.startPage());
                splitter.setEndPage(section.endPage());
                splitter.setSplitAtPage(Integer.MAX_VALUE);
                var excerpts=splitter.split(source);
                try(var bytes=new ByteArrayOutputStream()) {
                    if(excerpts.size()!=1)throw new IOException("Expected one syllabus source excerpt");
                    excerpts.get(0).save(bytes);
                    items.add(new ParsedItem(section.startPage(),section.endPage(),section.data(),section.issues(),bytes.toByteArray(),null));
                } finally {
                    for(var excerpt:excerpts)excerpt.close();
                }
            }
        }
        return new ParsedProgramDocument(batch.pageCount(),items);
    }
}
