package com.scse.curriculum.approval.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

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

}