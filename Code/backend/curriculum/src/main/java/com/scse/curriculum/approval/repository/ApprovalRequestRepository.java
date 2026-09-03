package com.scse.curriculum.approval.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scse.curriculum.approval.entity.ApprovalRequest;
import com.scse.curriculum.approval.entity.ApprovalStatus;
import com.scse.curriculum.approval.entity.ApprovalStep;

public interface ApprovalRequestRepository
        extends JpaRepository<ApprovalRequest, Integer> {


    List<ApprovalRequest> findBySyllabusId(
            Integer syllabusId);



    // FR-05.8:
    // Lấy lịch sử review/comment của syllabus
    // mới nhất hiển thị trước
    List<ApprovalRequest> findBySyllabusIdOrderByCreatedAtDesc(
            Integer syllabusId);



    List<ApprovalRequest> findByStatus(
            ApprovalStatus status);



    boolean existsBySyllabusIdAndStatus(
            Integer syllabusId,
            ApprovalStatus status);



    boolean existsBySyllabus_Course_IdAndSyllabus_AcademicYearAndSyllabus_SemesterAndStatus(
            Integer courseId,
            String academicYear,
            String semester,
            ApprovalStatus status);



    List<ApprovalRequest> findByStepAndStatusOrderByCreatedAtAsc(
            ApprovalStep step,
            ApprovalStatus status);

    List<ApprovalRequest> findByStepAndStatusAndSyllabus_Course_Department_IdOrderByCreatedAtAsc(
            ApprovalStep step,
            ApprovalStatus status,
            Integer departmentId);

    @Query("""
        SELECT ar FROM ApprovalRequest ar
        WHERE ar.step = :step AND ar.status = :status
          AND EXISTS (
            SELECT cp.id FROM CourseProgram cp
            WHERE cp.syllabus.id = ar.syllabus.id
              AND cp.program.major.id = :majorId
          )
        ORDER BY ar.createdAt ASC
        """)
    List<ApprovalRequest> findPendingByManagedMajor(
            @Param("step") ApprovalStep step,
            @Param("status") ApprovalStatus status,
            @Param("majorId") Integer majorId);

}
