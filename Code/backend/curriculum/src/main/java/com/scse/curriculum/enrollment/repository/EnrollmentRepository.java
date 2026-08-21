package com.scse.curriculum.enrollment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scse.curriculum.enrollment.entity.Enrollment;

public interface EnrollmentRepository
        extends JpaRepository<Enrollment, Integer> {

    List<Enrollment> findByStudent_Id(
            Integer studentId);

    List<Enrollment> findByClassSection_Id(
            Integer classSectionId);

    boolean existsByClassSection_Id(
            Integer classSectionId);

    List<Enrollment>
    findByStudent_StudentCodeContainingIgnoreCaseOrStudent_FullNameContainingIgnoreCaseOrClassSection_Course_CourseCodeContainingIgnoreCaseOrClassSection_Course_NameContainingIgnoreCase(
            String studentCode,
            String studentName,
            String courseCode,
            String courseName);

    boolean existsByStudent_IdAndClassSection_Id(
            Integer studentId,
            Integer classSectionId);
}