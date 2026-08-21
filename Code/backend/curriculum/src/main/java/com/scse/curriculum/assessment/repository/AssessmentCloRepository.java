package com.scse.curriculum.assessment.repository;

import com.scse.curriculum.assessment.entity.AssessmentClo;
import com.scse.curriculum.assessment.entity.AssessmentCloId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssessmentCloRepository
        extends JpaRepository<AssessmentClo, AssessmentCloId> {

    List<AssessmentClo> findByAssessmentComponent_Id(Integer assessmentComponentId);

    List<AssessmentClo> findByClo_Id(Integer cloId);

    List<AssessmentClo> findByAssessmentComponent_Syllabus_Id(Integer syllabusId);

    @Query("""
            SELECT assessmentClo
            FROM AssessmentClo assessmentClo
            JOIN FETCH assessmentClo.assessmentComponent component
            JOIN FETCH assessmentClo.clo clo
            WHERE component.syllabus.id = :syllabusId
            ORDER BY component.orderIndex, clo.orderIndex, clo.code
            """)
    List<AssessmentClo> findForPdfBySyllabusId(
            @Param("syllabusId") Integer syllabusId);
}
