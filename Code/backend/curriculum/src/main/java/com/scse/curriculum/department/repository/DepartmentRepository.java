package com.scse.curriculum.department.repository;

import com.scse.curriculum.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository
        extends JpaRepository<Department, Integer> {

    Optional<Department> findByCode(String code);

    boolean existsByCode(String code);
    
}