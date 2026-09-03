package com.scse.curriculum.syllabus.importer.service;


import com.scse.curriculum.syllabus.dto.SyllabusResponse;
import com.scse.curriculum.syllabus.importer.dto.ConfirmSyllabusImportRequest;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportPreviewResponse;
import com.scse.curriculum.syllabus.importer.dto.BulkSyllabusImportPreviewResponse;
import com.scse.curriculum.syllabus.importer.dto.CloPloReconciliationResponse;
import com.scse.curriculum.syllabus.importer.dto.BulkConfirmSyllabusImportRequest;

import org.springframework.web.multipart.MultipartFile;


public interface SyllabusImportService {


    /**
     * Upload file và đọc preview
     *
     * Chưa tạo syllabus
     */
    SyllabusImportPreviewResponse preview(
            MultipartFile file
    );

    BulkSyllabusImportPreviewResponse previewBulk(MultipartFile file, Integer programId, Integer cohortId);



    /**
     * Confirm import
     *
     * Tạo syllabus mới
     */
    SyllabusResponse confirm(
            ConfirmSyllabusImportRequest request
    );

    SyllabusResponse confirmBulkItem(BulkConfirmSyllabusImportRequest request);

    CloPloReconciliationResponse reconcileCloPloMappings(
            Integer programId,
            Integer cohortId
    );


}
