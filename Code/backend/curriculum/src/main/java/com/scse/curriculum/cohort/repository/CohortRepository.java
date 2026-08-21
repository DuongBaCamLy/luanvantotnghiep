package com.scse.curriculum.cohort.repository;

import com.scse.curriculum.cohort.entity.Cohort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface CohortRepository
        extends JpaRepository<Cohort, Integer> {

    Optional<Cohort> findByName(
            String name);

    boolean existsByProgramIdAndEntryYear(
            Integer programId,
            Integer entryYear);

    List<Cohort> findByProgram_IdOrderByEntryYearDesc(
            Integer programId);

    /**
     * Danh sách filter FR-06.1 được fetch cùng Program và Major để tránh N+1.
     */
    @Query("""
            SELECT c
            FROM Cohort c
            JOIN FETCH c.program p
            JOIN FETCH p.major m
            WHERE (c.isActive = true OR c.isActive IS NULL)
              AND (p.isActive = true OR p.isActive IS NULL)
            ORDER BY m.code, c.entryYear DESC, c.name
            """)
    List<Cohort> findActiveForDeanDashboard();
}
