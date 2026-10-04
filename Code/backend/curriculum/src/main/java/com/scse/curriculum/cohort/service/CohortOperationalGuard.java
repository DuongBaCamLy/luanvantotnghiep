package com.scse.curriculum.cohort.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;

import lombok.RequiredArgsConstructor;

/**
 * Canonical operational-state guard for Cohort-scoped mutations.
 *
 * Archived Cohorts remain readable historical data, but they must not be
 * changed by new operational workflows until explicitly restored.
 */
@Service
@RequiredArgsConstructor
public class CohortOperationalGuard {

    private static final String ARCHIVED_MESSAGE =
            "Archived Cohort is read-only. Restore it before making operational changes.";

    private final CohortRepository cohortRepository;

    private final CourseProgramRepository courseProgramRepository;

    /**
     * Resolve a Cohort by its canonical id and require it to be active.
     */
    @Transactional(readOnly = true)
    public Cohort requireActive(
            Integer cohortId) {

        if (cohortId == null) {
            throw new IllegalArgumentException(
                    "Cohort is required.");
        }

        Cohort cohort = cohortRepository
                .findById(cohortId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cohort not found"));

        assertActive(cohort);

        return cohort;
    }

    /**
     * Require a resolved Cohort entity to be operational.
     */
    public void assertActive(
            Cohort cohort) {

        if (cohort == null) {
            throw new IllegalArgumentException(
                    "Cohort is required.");
        }

        if (Boolean.FALSE.equals(
                cohort.getIsActive())) {

            throw new IllegalStateException(
                    ARCHIVED_MESSAGE);
        }
    }

    /**
     * Existing syllabus mutations are scoped through their canonical
     * CourseProgram -> Cohort relationships.
     *
     * Historical/orphan records with no Cohort link are intentionally not
     * changed by this archive guard. Their existing validation continues to
     * apply elsewhere.
     */
    @Transactional(readOnly = true)
    public void assertSyllabusNotArchived(
            Integer syllabusId) {

        if (syllabusId == null) {
            throw new IllegalArgumentException(
                    "Syllabus is required.");
        }

        List<CourseProgram> mappings =
                courseProgramRepository
                        .findBySyllabus_Id(
                                syllabusId);

        boolean archived =
                mappings.stream()
                        .map(CourseProgram::getCohort)
                        .filter(cohort ->
                                cohort != null)
                        .anyMatch(cohort ->
                                Boolean.FALSE.equals(
                                        cohort.getIsActive()));

        if (archived) {
            throw new IllegalStateException(
                    ARCHIVED_MESSAGE);
        }
    }
}
