package com.scse.curriculum.syllabus.importer.repository;


import com.scse.curriculum.syllabus.importer.entity.SyllabusImportHistory;
import com.scse.curriculum.syllabus.importer.entity.ImportStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;



@Repository
public interface SyllabusImportHistoryRepository
        extends JpaRepository<SyllabusImportHistory, Integer> {



    /*
     * Lấy toàn bộ lịch sử import của syllabus
     */
    List<SyllabusImportHistory>
    findBySyllabusIdOrderByCreatedAtDesc(
            Integer syllabusId
    );




    /*
     * Lấy lần import gần nhất
     */
    Optional<SyllabusImportHistory>
    findTopBySyllabusIdOrderByCreatedAtDesc(
            Integer syllabusId
    );




    /*
     * Kiểm tra trạng thái import
     */
    List<SyllabusImportHistory>
    findByImportStatus(
            ImportStatus importStatus
    );



}