package com.scse.curriculum.cloplomapping.repository;

import com.scse.curriculum.cloplomapping.entity.CloPloMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CloPloMappingRepository
        extends JpaRepository<CloPloMapping, Integer> {

    List<CloPloMapping> findByCloId(Integer cloId);

    List<CloPloMapping> findByPloId(Integer ploId);

    List<CloPloMapping> findByClo_Syllabus_Id(Integer syllabusId);

    boolean existsByCloIdAndPloId(
            Integer cloId,
            Integer ploId);

    @Query("""
            SELECT mapping
            FROM CloPloMapping mapping
            JOIN FETCH mapping.clo clo
            JOIN FETCH mapping.plo plo
            WHERE clo.syllabus.id = :syllabusId
            ORDER BY clo.orderIndex, clo.code, plo.code
            """)
    List<CloPloMapping> findForPdfBySyllabusId(
            @Param("syllabusId") Integer syllabusId);
}
