package com.scse.curriculum.courseprogram.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scse.curriculum.courseprogram.entity.CourseProgram;


public interface CourseProgramRepository
        extends JpaRepository<CourseProgram, Integer> {

    List<CourseProgram> findByProgram_Id(
            Integer programId);

    List<CourseProgram> findByCohort_Id(
            Integer cohortId);

    List<CourseProgram> findByCourse_Id(
            Integer courseId);

    List<CourseProgram> findByCourse_IdAndCohort_Id(
            Integer courseId,
            Integer cohortId);

    List<CourseProgram> findBySyllabus_Id(
            Integer syllabusId);

    List<CourseProgram> findByProgram_IdAndCohort_Id(
            Integer programId,
            Integer cohortId);

    @Query("""
        SELECT DISTINCT cp
        FROM CourseProgram cp
        JOIN FETCH cp.course
        JOIN FETCH cp.program
        LEFT JOIN FETCH cp.cohort
        LEFT JOIN FETCH cp.courseType
        LEFT JOIN FETCH cp.syllabus
        WHERE cp.program.id = :programId
          AND cp.cohort.id = :cohortId
    """)
    List<CourseProgram> findByProgramIdAndCohortIdWithRelations(
            @Param("programId") Integer programId,
            @Param("cohortId") Integer cohortId);

    @Query("""
        SELECT DISTINCT cp
        FROM CourseProgram cp
        JOIN FETCH cp.course
        JOIN FETCH cp.program
        LEFT JOIN FETCH cp.cohort
        LEFT JOIN FETCH cp.courseType
        LEFT JOIN FETCH cp.syllabus
        WHERE cp.program.id = :programId
          AND (cp.cohort.id = :cohortId OR cp.cohort IS NULL)
    """)
    List<CourseProgram> findEffectiveByProgramIdAndCohortIdWithRelations(
            @Param("programId") Integer programId,
            @Param("cohortId") Integer cohortId);

    Optional<CourseProgram> findByCourse_IdAndProgram_IdAndCohort_Id(
            Integer courseId,
            Integer programId,
            Integer cohortId);

            @Query("""
    SELECT DISTINCT cp
    FROM CourseProgram cp
    JOIN FETCH cp.course
    JOIN FETCH cp.program
    LEFT JOIN FETCH cp.cohort
    LEFT JOIN FETCH cp.courseType
    LEFT JOIN FETCH cp.syllabus
    WHERE cp.program.id = :programId
      AND cp.cohort IS NULL
""")
List<CourseProgram> findGeneralByProgramIdWithRelations(
        @Param("programId") Integer programId
);
}