package com.scse.curriculum.program.repository;

import com.scse.curriculum.program.entity.Program;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProgramRepository
        extends JpaRepository<Program, Integer> {

    Optional<Program> findByCode(String code);

    boolean existsByCode(String code);
}