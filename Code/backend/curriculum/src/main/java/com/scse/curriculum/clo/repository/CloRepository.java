package com.scse.curriculum.clo.repository;

import com.scse.curriculum.clo.entity.Clo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CloRepository extends JpaRepository<Clo, Integer> {

    List<Clo> findBySyllabusId(Integer syllabusId);

    Optional<Clo> findBySyllabusIdAndCode(
            Integer syllabusId,
            String code);

    boolean existsBySyllabusIdAndCode(
            Integer syllabusId,
            String code);

    @Query("""
            SELECT c
            FROM Clo c
            WHERE c.syllabus.id = :syllabusId
            ORDER BY c.orderIndex, c.code
            """)
    List<Clo> findForPdfBySyllabusId(
            @Param("syllabusId") Integer syllabusId);
}
