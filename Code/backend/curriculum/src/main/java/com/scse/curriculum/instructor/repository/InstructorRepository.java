package com.scse.curriculum.instructor.repository;

import com.scse.curriculum.instructor.entity.Instructor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InstructorRepository extends JpaRepository<Instructor, Integer> {

    Optional<Instructor> findByStaffCode(String staffCode);

    Optional<Instructor> findByEmail(String email);

    boolean existsByStaffCode(String staffCode);

    boolean existsByEmail(String email);

}