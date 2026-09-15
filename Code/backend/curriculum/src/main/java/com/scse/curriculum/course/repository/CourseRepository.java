package com.scse.curriculum.course.repository;

import com.scse.curriculum.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository
        extends JpaRepository<Course,Integer> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Course c where c.id = :id")
    Optional<Course> lockIdentity(@org.springframework.data.repository.query.Param("id") Integer id);

    Optional<Course> findByCourseCode(String courseCode);

    boolean existsByCourseCode(String courseCode);

    List<Course> findByDepartmentIdOrderByCourseCode(Integer departmentId);
}
