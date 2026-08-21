package com.scse.curriculum.studentscore.repository;

import com.scse.curriculum.studentscore.entity.StudentScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentScoreRepository extends JpaRepository<StudentScore, Integer> {

    boolean existsByEnrollment_IdAndAssessmentComponent_Id(Integer enrollmentId, Integer assessmentComponentId);

    List<StudentScore> findByAssessmentComponent_Id(Integer assessmentComponentId);

    java.util.List<StudentScore> findByEnrollment_Student_StudentCodeContainingIgnoreCaseOrEnrollment_Student_FullNameContainingIgnoreCaseOrAssessmentComponent_NameContainingIgnoreCase(
            String studentCode,
            String studentName,
            String componentName);
}
