package com.scse.curriculum.classsection.repository;

import com.scse.curriculum.classsection.entity.ClassSection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassSectionRepository
        extends JpaRepository<ClassSection, Integer> {

    @Query("""
            SELECT COUNT(cs)
            FROM ClassSection cs
            WHERE cs.cohort.id = :cohortId
              AND cs.isActive = true
            """)
    long countActiveByCohortId(
            @Param("cohortId")
            Integer cohortId);


    List<ClassSection> findByCourse_Id(
            Integer courseId);

    List<ClassSection> findBySyllabusId(
            Integer syllabusId);

    List<ClassSection>
    findByCourse_CourseCodeContainingIgnoreCaseOrCourse_NameContainingIgnoreCaseOrInstructorUser_FullNameContainingIgnoreCaseOrAcademicYearContainingIgnoreCase(
            String courseCode,
            String courseName,
            String instructorFullName,
            String academicYear);

    List<ClassSection>
    findByCourse_IdAndInstructorUser_IdAndSemesterAndAcademicYearIgnoreCaseAndGroupNumber(
            Integer courseId,
            Integer instructorUserId,
            Integer semester,
            String academicYear,
            Integer groupNumber);

    /*
     * =====================================================
     * DATA SCOPE FOR ADMIN / DEPT_HEAD
     * =====================================================
     *
     * departmentId = null:
     * - Admin sees all assignments.
     *
     * departmentId != null:
     * - Dept Head sees only assignments in the managed Major.
     */

    @Query("""
            SELECT DISTINCT cs
            FROM ClassSection cs
            JOIN FETCH cs.course c
            JOIN FETCH cs.instructorUser iu
            LEFT JOIN FETCH cs.syllabus s
            WHERE (
                :departmentId IS NULL
                OR cs.program.major.id = :departmentId
            )
            ORDER BY
                cs.academicYear DESC,
                cs.semester ASC,
                c.courseCode ASC,
                cs.groupNumber ASC
            """)
    List<ClassSection> findAllInScope(
            @Param("departmentId")
            Integer departmentId);

    @Query("""
            SELECT cs
            FROM ClassSection cs
            JOIN FETCH cs.course c
            JOIN FETCH cs.instructorUser iu
            LEFT JOIN FETCH cs.syllabus s
            WHERE cs.id = :id
              AND (
                  :departmentId IS NULL
                  OR cs.program.major.id = :departmentId
              )
            """)
    Optional<ClassSection> findByIdInScope(
            @Param("id")
            Integer id,

            @Param("departmentId")
            Integer departmentId);

    @Query("""
            SELECT DISTINCT cs
            FROM ClassSection cs
            JOIN FETCH cs.course c
            JOIN FETCH cs.instructorUser iu
            LEFT JOIN FETCH cs.syllabus s
            WHERE c.id = :courseId
              AND (
                  :departmentId IS NULL
                  OR cs.program.major.id = :departmentId
              )
            ORDER BY
                cs.academicYear DESC,
                cs.semester ASC,
                cs.groupNumber ASC
            """)
    List<ClassSection> findByCourseIdInScope(
            @Param("courseId")
            Integer courseId,

            @Param("departmentId")
            Integer departmentId);

    @Query("""
            SELECT DISTINCT cs
            FROM ClassSection cs
            JOIN FETCH cs.course c
            JOIN FETCH cs.instructorUser iu
            LEFT JOIN FETCH cs.syllabus s
            WHERE (
                :departmentId IS NULL
                OR cs.program.major.id = :departmentId
            )
            AND (
                LOWER(c.courseCode)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))

                OR LOWER(c.name)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))

                OR LOWER(iu.fullName)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))

                OR LOWER(iu.username)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))

                OR LOWER(cs.academicYear)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            ORDER BY
                cs.academicYear DESC,
                cs.semester ASC,
                c.courseCode ASC,
                cs.groupNumber ASC
            """)
    List<ClassSection> searchInScope(
            @Param("keyword")
            String keyword,

            @Param("departmentId")
            Integer departmentId);

    /*
     * Duplicate assignment rule:
     *
     * course
     * + semester
     * + academicYear
     * + groupNumber
     *
     * Instructor is intentionally NOT part of this rule.
     */
    @Query("""
            SELECT COUNT(cs)
            FROM ClassSection cs
            WHERE cs.course.id = :courseId
              AND cs.semester = :semester
              AND LOWER(TRIM(cs.academicYear))
                    = LOWER(TRIM(:academicYear))
              AND cs.groupNumber = :groupNumber
              AND (
                  :excludedId IS NULL
                  OR cs.id <> :excludedId
              )
            """)
    long countDuplicateAssignment(
            @Param("courseId")
            Integer courseId,

            @Param("semester")
            Integer semester,

            @Param("academicYear")
            String academicYear,

            @Param("groupNumber")
            Integer groupNumber,

            @Param("excludedId")
            Integer excludedId);

    @Query("""
            SELECT COUNT(DISTINCT cs.course.id)
            FROM ClassSection cs
            WHERE cs.instructorUser.id = :instructorUserId
            """)
    Integer countDistinctCoursesByInstructorUserId(
            @Param("instructorUserId")
            Integer instructorUserId);

    @Query("""
            SELECT cs
            FROM ClassSection cs
            JOIN FETCH cs.course c
            LEFT JOIN FETCH c.department d
            JOIN FETCH cs.instructorUser iu
            LEFT JOIN FETCH cs.syllabus s
            WHERE iu.id = :instructorUserId
              AND cs.isActive = true
            ORDER BY
                cs.academicYear DESC,
                cs.semester ASC,
                c.courseCode ASC,
                cs.groupNumber ASC
            """)
    List<ClassSection> findActiveByInstructorUserId(
            @Param("instructorUserId")
            Integer instructorUserId);

    /**
     * FR-05.6:
     * Load all ACTIVE teaching assignments
     * for the requested academic year and semester.
     */
    @Query("""
            SELECT DISTINCT cs
            FROM ClassSection cs
            JOIN FETCH cs.course c
            LEFT JOIN FETCH c.department d
            JOIN FETCH cs.instructorUser iu
            LEFT JOIN FETCH cs.syllabus s
            WHERE cs.isActive = true
              AND LOWER(TRIM(cs.academicYear))
                    = LOWER(TRIM(:academicYear))
              AND cs.semester = :semester
            ORDER BY
                iu.id,
                c.courseCode,
                cs.groupNumber
            """)
    List<ClassSection> findActiveForDeadline(
            @Param("academicYear")
            String academicYear,

            @Param("semester")
            Integer semester);

    /**
     * Exact active assignment for:
     * instructor account + course + year + semester.
     */
    @Query("""
            SELECT cs
            FROM ClassSection cs
            JOIN FETCH cs.course c
            JOIN FETCH cs.instructorUser iu
            LEFT JOIN FETCH cs.syllabus s
            WHERE iu.id = :instructorUserId
              AND c.id = :courseId
              AND cs.isActive = true
              AND LOWER(TRIM(cs.academicYear))
                    = LOWER(TRIM(:academicYear))
              AND cs.semester = :semester
            ORDER BY
                cs.groupNumber ASC,
                cs.id ASC
            """)
    List<ClassSection> findActiveAssignmentsExact(
            @Param("instructorUserId")
            Integer instructorUserId,

            @Param("courseId")
            Integer courseId,

            @Param("academicYear")
            String academicYear,

            @Param("semester")
            Integer semester);

    /**
     * Direct ClassSection -> Syllabus assignment check
     * based on UserAccount rather than Instructor Profile.
     */
    boolean existsByInstructorUser_IdAndSyllabus_IdAndIsActiveTrue(
            Integer instructorUserId,
            Integer syllabusId);

    @Query("""
            SELECT section
            FROM ClassSection section
            JOIN FETCH section.instructorUser instructorUser
            JOIN FETCH section.course course
            WHERE section.syllabus.id = :syllabusId
            ORDER BY
                instructorUser.fullName,
                section.groupNumber
            """)
    List<ClassSection> findForPdfBySyllabusId(
            @Param("syllabusId")
            Integer syllabusId);
}
