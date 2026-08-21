package com.scse.curriculum.programtype.repository;

import com.scse.curriculum.programtype.entity.ProgramType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProgramTypeRepository
        extends JpaRepository<ProgramType, Integer> {

    Optional<ProgramType> findByCode(String code);

    boolean existsByCode(String code);
}