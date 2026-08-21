package com.scse.curriculum.assessment.repository;

import com.scse.curriculum.assessment.entity.AssessmentComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentComponentRepository
        extends JpaRepository<AssessmentComponent, Integer> {

    List<AssessmentComponent> findBySyllabusId(
            Integer syllabusId);

}