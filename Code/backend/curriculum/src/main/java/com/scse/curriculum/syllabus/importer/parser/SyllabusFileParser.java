package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportIssue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Format-independent contract. An AI-backed parser can implement this as a later fallback. */
public interface SyllabusFileParser {
    boolean supports(String fileName, String contentType);

    SyllabusImportData parse(InputStream input, List<SyllabusImportIssue> issues) throws IOException;
}
