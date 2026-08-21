package com.scse.curriculum.syllabus.repository;

import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SyllabusRepository
        extends JpaRepository<Syllabus, Integer> {

    List<Syllabus> findByCourseId(
            Integer courseId);

    List<Syllabus> findByStatus(
            SyllabusStatus status);

    Optional<Syllabus> findByCourseIdAndVersionNumber(
            Integer courseId,
            Integer versionNumber);

    Optional<Syllabus> findTopByCourseIdAndStatusOrderByVersionNumberDescIdDesc(
            Integer courseId,
            SyllabusStatus status);

    Optional<Syllabus> findTopByCourseIdOrderByVersionNumberDescIdDesc(
            Integer courseId);

    boolean existsByCourseIdAndVersionNumber(
            Integer courseId,
            Integer versionNumber);

    // =========================
    // FIX LAZY LOADING - NEW
    // =========================
    @Query("""
            SELECT s
            FROM Syllabus s
            JOIN FETCH s.course
            JOIN FETCH s.createdBy
            LEFT JOIN FETCH s.approvedBy
            """)
    List<Syllabus> findAllWithRelations();

    @Query("""
            SELECT s
            FROM Syllabus s
            JOIN FETCH s.course
            JOIN FETCH s.createdBy
            LEFT JOIN FETCH s.approvedBy
            WHERE s.id = :id
            """)
    Optional<Syllabus> findByIdWithRelations(
            @Param("id") Integer id);

    // =========================
    // FIX MISSING METHODS
    // =========================
    @Query("""
            SELECT s
            FROM Syllabus s
            JOIN FETCH s.course
            JOIN FETCH s.createdBy
            LEFT JOIN FETCH s.approvedBy
            WHERE s.course.id = :courseId
            ORDER BY s.versionNumber DESC
            """)
    List<Syllabus> findByCourseIdWithRelations(
            @Param("courseId") Integer courseId);

    @Query("""
            SELECT s
            FROM Syllabus s
            JOIN FETCH s.course
            JOIN FETCH s.createdBy
            LEFT JOIN FETCH s.approvedBy
            WHERE s.status = :status
            """)
    List<Syllabus> findByStatusWithRelations(
            @Param("status") SyllabusStatus status);


    @Query("""
            SELECT COALESCE(MAX(s.versionNumber), 0)
            FROM Syllabus s
            WHERE s.course.id = :courseId
            """)
    Integer findMaxVersionNumberByCourseId(
            @Param("courseId") Integer courseId);

    @Modifying
    @Query(
            value = "DELETE FROM syllabus WHERE id = :id",
            nativeQuery = true
    )
    void deleteSyllabusNative(
            @Param("id") Integer id);

    @Query("""
    SELECT s
    FROM Syllabus s
    JOIN FETCH s.course c
    WHERE (:academicYear IS NULL OR s.academicYear = :academicYear)
      AND (:major IS NULL OR s.major = :major)
      AND s.isCurrent = true
""")
    List<Syllabus> findCurrentByAcademicYearAndMajor(
            @Param("academicYear") String academicYear,
            @Param("major") String major
    );

    


    /**
     * FR-06.1: tải tất cả ứng viên Syllabus của một tập môn trong đúng một
     * query. Service sẽ chọn một phiên bản đại diện cho từng môn, vì vậy không
     * phát sinh N+1 và không đếm trùng lịch sử phiên bản.
     */
    @Query("""
            SELECT s
            FROM Syllabus s
            JOIN FETCH s.course c
            LEFT JOIN FETCH s.createdBy creator
            LEFT JOIN FETCH s.approvedBy approver
            WHERE c.id IN :courseIds
            ORDER BY c.id, s.versionNumber DESC, s.updatedAt DESC, s.id DESC
            """)
    List<Syllabus> findDeanDashboardCandidates(
            @Param("courseIds") List<Integer> courseIds);

    @Query("""
            SELECT s
            FROM Syllabus s
            JOIN FETCH s.course c
            LEFT JOIN FETCH c.department d
            LEFT JOIN FETCH s.createdBy creator
            LEFT JOIN FETCH s.approvedBy approver
            WHERE s.id = :id
            """)
    Optional<Syllabus> findByIdForPdf(@Param("id") Integer id);

}
