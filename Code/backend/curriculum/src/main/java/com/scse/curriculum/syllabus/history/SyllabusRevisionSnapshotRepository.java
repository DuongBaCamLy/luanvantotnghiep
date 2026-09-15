package com.scse.curriculum.syllabus.history;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SyllabusRevisionSnapshotRepository extends JpaRepository<SyllabusRevisionSnapshot,Long> {
    List<SyllabusRevisionSnapshot> findBySyllabusIdOrderByCapturedAtAscIdAsc(Integer syllabusId);
    boolean existsBySyllabusId(Integer syllabusId);
}
