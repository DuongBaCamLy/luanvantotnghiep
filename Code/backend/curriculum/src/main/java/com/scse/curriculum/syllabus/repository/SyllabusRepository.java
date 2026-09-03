package com.scse.curriculum.syllabus.repository;


import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;



@Repository
public interface SyllabusRepository
        extends JpaRepository<Syllabus, Integer> {



    /*
     * =====================================================
     * EXISTING
     * =====================================================
     */


    Optional<Syllabus>
    findTopByCourseIdOrderByVersionNumberDescIdDesc(
            Integer courseId
    );



    Optional<Syllabus>
    findTopByCourseIdAndStatusOrderByVersionNumberDescIdDesc(
            Integer courseId,
            SyllabusStatus status
    );





    /*
     * =====================================================
     * LOAD FULL SYLLABUS LIST
     *
     * IMPORTANT:
     * DO NOT FETCH MULTIPLE @OneToMany LISTS HERE
     *
     * Avoid:
     * clos + topics + assessments + references
     *
     * Hibernate MultipleBagFetchException
     *
     * =====================================================
     */


    @Query("""
        SELECT DISTINCT s

        FROM Syllabus s

        LEFT JOIN FETCH s.course

        LEFT JOIN FETCH s.createdBy

        LEFT JOIN FETCH s.approvedBy

        ORDER BY s.createdAt DESC

    """)
    List<Syllabus> findAllWithRelations();

    /**
     * Canonical curriculum scope used by both Syllabus Catalog and Curriculum Map.
     * A syllabus belongs to a program/cohort only through the real
     * course_program.syllabus_id link; unlinked curriculum courses are excluded.
     */
    @Query("""
        SELECT DISTINCT s
        FROM CourseProgram cp
        JOIN cp.syllabus s
        JOIN FETCH s.course c
        LEFT JOIN FETCH s.createdBy
        LEFT JOIN FETCH s.approvedBy
        WHERE cp.program.id = :programId
          AND (:cohortId IS NULL OR cp.cohort.id = :cohortId)
        ORDER BY s.createdAt DESC
    """)
    List<Syllabus> findCatalogScope(
            @Param("programId") Integer programId,
            @Param("cohortId") Integer cohortId);

    @Query("""
        SELECT DISTINCT s FROM Syllabus s
        LEFT JOIN FETCH s.course c
        LEFT JOIN FETCH c.department
        LEFT JOIN FETCH s.createdBy
        LEFT JOIN FETCH s.approvedBy
        WHERE c.department.id = :departmentId
        ORDER BY s.createdAt DESC
    """)
    List<Syllabus> findAllWithRelationsByDepartmentId(
            @Param("departmentId") Integer departmentId);







    /*
     * =====================================================
     * DETAIL
     *
     * Load one collection only
     *
     * =====================================================
     */


    @Query("""
        SELECT DISTINCT s

        FROM Syllabus s

        LEFT JOIN FETCH s.course

        LEFT JOIN FETCH s.createdBy

        LEFT JOIN FETCH s.approvedBy

        LEFT JOIN FETCH s.clos

        WHERE s.id = :id

    """)
    Optional<Syllabus> findByIdWithRelations(
            @Param("id") Integer id
    );








    /*
     * =====================================================
     * COURSE SEARCH
     * =====================================================
     */


    @Query("""
        SELECT DISTINCT s

        FROM Syllabus s

        LEFT JOIN FETCH s.course

        LEFT JOIN FETCH s.createdBy

        LEFT JOIN FETCH s.approvedBy

        WHERE s.course.id = :courseId

        ORDER BY
            s.versionNumber DESC,
            s.id DESC

    """)
    List<Syllabus> findByCourseIdWithRelations(
            @Param("courseId") Integer courseId
    );









    /*
     * =====================================================
     * STATUS SEARCH
     * =====================================================
     */


    @Query("""
        SELECT DISTINCT s

        FROM Syllabus s

        LEFT JOIN FETCH s.course

        LEFT JOIN FETCH s.createdBy

        LEFT JOIN FETCH s.approvedBy

        WHERE s.status = :status

        ORDER BY s.createdAt DESC

    """)
    List<Syllabus> findByStatusWithRelations(
            @Param("status") SyllabusStatus status
    );









    /*
     * =====================================================
     * PDF SUPPORT
     * =====================================================
     */


    @Query("""
        SELECT DISTINCT s

        FROM Syllabus s

        LEFT JOIN FETCH s.course

        LEFT JOIN FETCH s.clos

        WHERE s.id = :id

    """)
    Optional<Syllabus> findByIdForPdf(
            @Param("id") Integer id
    );









    /*
     * =====================================================
     * VERSION SUPPORT
     * =====================================================
     */


    @Query("""
        SELECT COALESCE(MAX(s.versionNumber),0)

        FROM Syllabus s

        WHERE s.course.id = :courseId

    """)
    Integer findMaxVersionNumberByCourseId(
            @Param("courseId") Integer courseId
    );









    /*
     * =====================================================
     * DASHBOARD SUPPORT
     * =====================================================
     */


    @Query("""
        SELECT DISTINCT s

        FROM Syllabus s

        LEFT JOIN FETCH s.course c

        LEFT JOIN FETCH c.department d

        WHERE d.id IN :departmentIds

    """)
    List<Syllabus> findDeanDashboardCandidates(
            @Param("departmentIds")
            List<Integer> departmentIds
    );









    /*
     * =====================================================
     * ADMIN LIST
     * =====================================================
     */


    @Query("""
        SELECT DISTINCT s

        FROM Syllabus s

        LEFT JOIN FETCH s.course

        LEFT JOIN FETCH s.createdBy

        LEFT JOIN FETCH s.approvedBy

        ORDER BY s.createdAt DESC

    """)
    List<Syllabus> findAdminList();

    @Query("""
        SELECT DISTINCT s FROM Syllabus s
        LEFT JOIN FETCH s.course c
        LEFT JOIN FETCH c.department
        LEFT JOIN FETCH s.createdBy
        LEFT JOIN FETCH s.approvedBy
        WHERE c.department.id = :departmentId
        ORDER BY s.createdAt DESC
    """)
    List<Syllabus> findCatalogByDepartmentId(
            @Param("departmentId") Integer departmentId);









    /*
     * =====================================================
     * FILTER SUPPORT
     * =====================================================
     */


    @Query("""
        SELECT DISTINCT s.courseCodeSnapshot

        FROM Syllabus s

        WHERE s.courseCodeSnapshot IS NOT NULL

        ORDER BY s.courseCodeSnapshot

    """)
    List<String> findCourseFilters();





    @Query("""
        SELECT DISTINCT s.program

        FROM Syllabus s

        WHERE s.program IS NOT NULL

        ORDER BY s.program

    """)
    List<String> findProgramFilters();





    @Query("""
        SELECT DISTINCT s.semester

        FROM Syllabus s

        WHERE s.semester IS NOT NULL

        ORDER BY s.semester

    """)
    List<String> findSemesterFilters();





    @Query("""
        SELECT DISTINCT s.status

        FROM Syllabus s

        ORDER BY s.status

    """)
    List<SyllabusStatus> findStatusFilters();





    @Query("""
        SELECT DISTINCT s.createdBy.username

        FROM Syllabus s

        WHERE s.createdBy IS NOT NULL

        ORDER BY s.createdBy.username

    """)
    List<String> findUserFilters();









    /*
     * =====================================================
     * CREATE CONTEXT
     * =====================================================
     */


    @Query("""
        SELECT DISTINCT s

        FROM Syllabus s

        LEFT JOIN FETCH s.course

        LEFT JOIN FETCH s.clos

        WHERE s.course.id = :courseId

        AND s.status =
        com.scse.curriculum.syllabus.entity.SyllabusStatus.APPROVED

        ORDER BY

        s.versionNumber DESC,
        s.id DESC

    """)
    Optional<Syllabus> findLatestApproved(
            @Param("courseId")
            Integer courseId
    );








    @Query("""
        SELECT s

        FROM Syllabus s

        WHERE s.course.id = :courseId

        ORDER BY

        s.versionNumber DESC,
        s.id DESC

    """)
    Optional<Syllabus> findLatestVersion(
            @Param("courseId")
            Integer courseId
    );









    /*
     * =====================================================
     * CLONE
     * =====================================================
     */


    @Query("""
        SELECT DISTINCT s

        FROM Syllabus s

        LEFT JOIN FETCH s.clos

        WHERE s.id = :id

    """)
    Optional<Syllabus> findForClone(
            @Param("id")
            Integer id
    );


}
