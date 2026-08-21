package com.scse.curriculum.student.repository;

import com.scse.curriculum.student.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Integer> {

    boolean existsByStudentCode(String studentCode);

    Optional<Student> findByStudentCode(String studentCode);

    List<Student> findByStudentCodeContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String studentCode,
            String fullName,
            String email);
}