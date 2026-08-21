package com.scse.curriculum.plo.repository;

import com.scse.curriculum.plo.entity.Plo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PloRepository
        extends JpaRepository<Plo, Integer> {

    List<Plo> findAllByCode(String code);

    List<Plo> findByProgramId(Integer programId);

    boolean existsByProgramIdAndCodeAndVersionNumber(
            Integer programId,
            String code,
            Integer versionNumber);

    boolean existsByProgramIdAndCodeAndVersionNumberAndIdNot(
            Integer programId,
            String code,
            Integer versionNumber,
            Integer id);
}