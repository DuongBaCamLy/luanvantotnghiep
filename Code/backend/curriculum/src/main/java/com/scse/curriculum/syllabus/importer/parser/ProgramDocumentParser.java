package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportIssue;
import java.io.IOException;
import java.util.List;

public interface ProgramDocumentParser {
    boolean supports(String filename, String contentType, byte[] content);
    ParsedProgramDocument parse(byte[] content) throws IOException;

    record ParsedProgramDocument(int unitCount, List<ParsedItem> items) {}
    record ParsedItem(int startBoundary, int endBoundary, SyllabusImportData data,
                      List<SyllabusImportIssue> issues, byte[] sourceSnapshot, String fieldMap) {}
}
