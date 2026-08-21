package com.scse.curriculum.syllabus.importer.service;

import com.scse.curriculum.syllabus.dto.SyllabusResponse;
import com.scse.curriculum.syllabus.importer.dto.ConfirmSyllabusImportRequest;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportPreviewResponse;
import org.springframework.web.multipart.MultipartFile;

public interface SyllabusImportService {
    SyllabusImportPreviewResponse preview(Integer syllabusId, MultipartFile file);
    SyllabusResponse confirm(Integer syllabusId, ConfirmSyllabusImportRequest request);
}
