package com.scse.curriculum.course.repository;

import com.scse.curriculum.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseRepository
        extends JpaRepository<Course,Integer> {

    Optional<Course> findByCourseCode(String courseCode);

    boolean existsByCourseCode(String courseCode);
}