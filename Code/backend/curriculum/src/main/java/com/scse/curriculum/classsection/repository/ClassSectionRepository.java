package com.scse.curriculum.classsection.repository;

import com.scse.curriculum.classsection.entity.ClassSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassSectionRepository
        extends JpaRepository<ClassSection, Integer> {

    /*
     * Những method cũ vẫn được giữ lại vì các service khác
     * trong project có thể đang sử dụng.
     */

    List<ClassSection> findByCourse_Id(Integer courseId);

    List<ClassSection> findBySyllabusId(Integer syllabusId);

    List<ClassSection>
    findByCourse_CourseCodeContainingIgnoreCaseOrCourse_NameContainingIgnoreCaseOrInstructor_FullNameContainingIgnoreCaseOrAcademicYearContainingIgnoreCase(
            String courseCode,
            String courseName,
            String instructorName,
            String academicYear);

    List<ClassSection>
    findByCourse_IdAndInstructor_IdAndSemesterAndAcademicYearIgnoreCaseAndGroupNumber(
            Integer courseId,
            Integer instructorId,
            Integer semester,
            String academicYear,
            Integer groupNumber);

    /*
     * =====================================================
     * DATA SCOPE CHO ADMIN VÀ DEPT_HEAD
     * =====================================================
     *
     * departmentId = null:
     * - Admin xem được toàn bộ.
     *
     * departmentId có giá trị:
     * - DeptHead chỉ xem ClassSection thuộc bộ môn đó.
     */

    @Query("""
            SELECT DISTINCT cs
            FROM ClassSection cs
            JOIN FETCH cs.course c
            JOIN FETCH cs.instructor i
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
            @Param("departmentId") Integer departmentId);

    @Query("""
            SELECT cs
            FROM ClassSection cs
            JOIN FETCH cs.course c
            JOIN FETCH cs.instructor i
            LEFT JOIN FETCH cs.syllabus s
            WHERE cs.id = :id
              AND (
                  :departmentId IS NULL
                  OR cs.program.major.id = :departmentId
              )
            """)
    Optional<ClassSection> findByIdInScope(
            @Param("id") Integer id,
            @Param("departmentId") Integer departmentId);

    @Query("""
            SELECT DISTINCT cs
            FROM ClassSection cs
            JOIN FETCH cs.course c
            JOIN FETCH cs.instructor i
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
            @Param("courseId") Integer courseId,
            @Param("departmentId") Integer departmentId);

    @Query("""
            SELECT DISTINCT cs
            FROM ClassSection cs
            JOIN FETCH cs.course c
            JOIN FETCH cs.instructor i
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
                OR LOWER(i.fullName)
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
            @Param("keyword") String keyword,
            @Param("departmentId") Integer departmentId);

    /*
     * Kiểm tra trùng đúng theo nghiệp vụ:
     *
     * course + semester + academicYear + groupNumber.
     *
     * Không đưa instructorId vào điều kiện vì đổi giảng viên
     * không được phép tạo thêm một lớp trùng nhóm.
     *
     * excludedId:
     * - null khi tạo mới.
     * - ID hiện tại khi cập nhật.
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
            @Param("courseId") Integer courseId,
            @Param("semester") Integer semester,
            @Param("academicYear") String academicYear,
            @Param("groupNumber") Integer groupNumber,
            @Param("excludedId") Integer excludedId);

    @Query("""
            SELECT COUNT(DISTINCT cs.course.id)
            FROM ClassSection cs
            WHERE cs.instructor.id = :instructorId
            """)
    Integer countDistinctCoursesByInstructorId(
            @Param("instructorId") Integer instructorId);

    @Query("""
            SELECT cs
            FROM ClassSection cs
            JOIN FETCH cs.course c
            LEFT JOIN FETCH c.department d
            JOIN FETCH cs.instructor i
            LEFT JOIN FETCH cs.syllabus s
            WHERE i.id = :instructorId
              AND cs.isActive = true
            ORDER BY
                cs.academicYear DESC,
                cs.semester ASC,
                c.courseCode ASC,
                cs.groupNumber ASC
            """)
    List<ClassSection> findActiveByInstructorId(
            @Param("instructorId") Integer instructorId);

    /**
     * FR-05.6: lấy toàn bộ phân công ACTIVE của đúng năm học + học kỳ,
     * đồng thời fetch course/instructor/syllabus để scheduler không gặp N+1
     * hoặc LazyInitializationException.
     */
    @Query("""
            SELECT DISTINCT cs
            FROM ClassSection cs
            JOIN FETCH cs.course c
            LEFT JOIN FETCH c.department d
            JOIN FETCH cs.instructor i
            LEFT JOIN FETCH cs.syllabus s
            WHERE cs.isActive = true
              AND LOWER(TRIM(cs.academicYear))
                    = LOWER(TRIM(:academicYear))
              AND cs.semester = :semester
            ORDER BY i.id, c.courseCode, cs.groupNumber
            """)
    List<ClassSection> findActiveForDeadline(
            @Param("academicYear") String academicYear,
            @Param("semester") Integer semester);

    /**
     * Kiểm tra assignment chính xác theo:
     * - Giảng viên
     * - Môn học
     * - Năm học
     * - Học kỳ
     *
     * Không dùng wildcard khi academicYear hoặc semester null,
     * vì đây là điều kiện phân quyền dữ liệu.
     */
    @Query("""
            SELECT cs
            FROM ClassSection cs
            JOIN FETCH cs.course c
            JOIN FETCH cs.instructor i
            LEFT JOIN FETCH cs.syllabus s
            WHERE i.id = :instructorId
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
            @Param("instructorId") Integer instructorId,
            @Param("courseId") Integer courseId,
            @Param("academicYear") String academicYear,
            @Param("semester") Integer semester);

    /**
     * A direct ClassSection -> Syllabus link is the authoritative assignment
     * for an existing syllabus.  It must be checked independently from the
     * textual academic-year fields because imported legacy syllabi may store
     * a cohort code there while ClassSection stores the teaching year.
     */
    boolean existsByInstructor_IdAndSyllabus_IdAndIsActiveTrue(
            Integer instructorId,
            Integer syllabusId);

    @Query("""
            SELECT section
            FROM ClassSection section
            JOIN FETCH section.instructor instructor
            JOIN FETCH section.course course
            WHERE section.syllabus.id = :syllabusId
            ORDER BY instructor.fullName, section.groupNumber
            """)
    List<ClassSection> findForPdfBySyllabusId(
            @Param("syllabusId") Integer syllabusId);

}
