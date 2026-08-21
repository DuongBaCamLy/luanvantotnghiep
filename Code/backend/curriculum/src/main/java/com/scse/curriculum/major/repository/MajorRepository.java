package com.scse.curriculum.major.repository;

import com.scse.curriculum.major.entity.Major;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MajorRepository
        extends JpaRepository<Major, Integer> {

    Optional<Major> findByCode(String code);

    boolean existsByCode(String code);

    List<Major> findAllByOrderByCodeAsc();
}