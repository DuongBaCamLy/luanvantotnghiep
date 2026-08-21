package com.scse.curriculum.deadline.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scse.curriculum.deadline.entity.SyllabusDeadline;

public interface SyllabusDeadlineRepository
        extends JpaRepository<SyllabusDeadline, Long> {

    List<SyllabusDeadline> findAllByOrderByDeadlineAtDesc();

    List<SyllabusDeadline> findByActiveTrueOrderByDeadlineAtAsc();

    List<SyllabusDeadline> findByActiveTrueAndDeadlineAtBeforeOrderByDeadlineAtAsc(
            LocalDateTime deadlineAt);

    Optional<SyllabusDeadline> findByAcademicYearIgnoreCaseAndSemester(
            String academicYear,
            Integer semester);

    Optional<SyllabusDeadline> findByAcademicYearIgnoreCaseAndSemesterAndActiveTrue(
            String academicYear,
            Integer semester);

    boolean existsByAcademicYearIgnoreCaseAndSemesterAndIdNot(
            String academicYear,
            Integer semester,
            Long id);
}
