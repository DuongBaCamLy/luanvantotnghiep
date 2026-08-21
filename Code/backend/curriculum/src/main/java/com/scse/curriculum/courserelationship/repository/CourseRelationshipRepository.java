package com.scse.curriculum.courserelationship.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scse.curriculum.courserelationship.entity.CourseRelationship;
import com.scse.curriculum.courserelationship.entity.RelationType;

public interface CourseRelationshipRepository extends JpaRepository<CourseRelationship, Integer> {

    @Query("""
        SELECT DISTINCT cr
        FROM CourseRelationship cr
        JOIN FETCH cr.course
        JOIN FETCH cr.relatedCourse
    """)
    List<CourseRelationship> findAllWithCourses();

    @Query("""
        SELECT DISTINCT cr
        FROM CourseRelationship cr
        JOIN FETCH cr.course
        JOIN FETCH cr.relatedCourse
        WHERE cr.course.id = :courseId
    """)
    List<CourseRelationship> findByCourseIdWithCourses(@Param("courseId") Integer courseId);

    @Query("""
        SELECT DISTINCT cr
        FROM CourseRelationship cr
        JOIN FETCH cr.course
        JOIN FETCH cr.relatedCourse
        WHERE cr.id = :id
    """)
    Optional<CourseRelationship> findByIdWithCourses(@Param("id") Integer id);

    List<CourseRelationship> findByCourse_Id(Integer courseId);

    List<CourseRelationship> findByRelationType(RelationType relationType);

    List<CourseRelationship> findByCourse_CourseCodeContainingIgnoreCaseOrCourse_NameContainingIgnoreCaseOrRelatedCourse_CourseCodeContainingIgnoreCaseOrRelatedCourse_NameContainingIgnoreCase(
            String courseCode,
            String courseName,
            String relatedCourseCode,
            String relatedCourseName);

    Optional<CourseRelationship> findByCourse_IdAndRelatedCourse_IdAndRelationType(
            Integer courseId,
            Integer relatedCourseId,
            RelationType relationType);

    boolean existsByCourse_IdAndRelatedCourse_IdAndRelationType(
            Integer courseId,
            Integer relatedCourseId,
            RelationType relationType);
}