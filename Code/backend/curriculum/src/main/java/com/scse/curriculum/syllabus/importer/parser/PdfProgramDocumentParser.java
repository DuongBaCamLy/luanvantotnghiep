package com.scse.curriculum.syllabus.importer.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
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
}
