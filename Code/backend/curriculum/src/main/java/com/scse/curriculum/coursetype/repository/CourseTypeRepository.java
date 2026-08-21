package com.scse.curriculum.coursetype.repository;

import com.scse.curriculum.coursetype.entity.CourseType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseTypeRepository
        extends JpaRepository<CourseType, Integer> {

    Optional<CourseType> findByCode(
            String code);
}