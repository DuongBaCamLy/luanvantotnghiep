package com.scse.curriculum.syllabus.maintenance;

import com.scse.curriculum.classsection.entity.ClassSection;
import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.common.exception.ForbiddenOperationException;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.syllabus.history.SyllabusRevisionSnapshotRepository;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.syllabus.service.SyllabusService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SyllabusCohortResetService {

    private static final Pattern YEAR_SUFFIX =
            Pattern.compile("(\\d{4})$");

    private static final String AUDIT_PROGRAM_PREFIX =
            "__AUDIT_RESET__";

    private final CohortRepository cohortRepository;
    private final CourseProgramRepository courseProgramRepository;
    private final ClassSectionRepository classSectionRepository;
    private final SyllabusRepository syllabusRepository;
    private final SyllabusRevisionSnapshotRepository revisionSnapshotRepository;
    private final SyllabusService syllabusService;

    @Value("${app.admin.cohort-reset-enabled:false}")
    private boolean cohortResetEnabled;

    @Transactional
    public int resetForReimport(
            Integer programId,
            Integer cohortId,
            String confirmCohort) {

        assertResetEnabled();

        if (programId == null || cohortId == null) {
            throw new IllegalArgumentException(
                    "programId and cohortId are required.");
        }

        Cohort cohort =
                cohortRepository.findById(cohortId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Cohort not found"));

        assertCohortBelongsToProgram(
                cohort,
                programId);

        String cohortName =
                normalizeCohortName(cohort);

        assertConfirmation(
                confirmCohort,
                cohortName);

        /*
         * Scope is defined ONLY through CourseProgram.
         * Do not trust syllabus.academicYear for reset scope.
         */
        List<CourseProgram> scopedMappings =
                courseProgramRepository
                        .findByProgram_IdAndCohort_Id(
                                programId,
                                cohortId);

        LinkedHashSet<Integer> syllabusIds =
                collectSyllabusIds(
                        scopedMappings);

        /*
         * Abort before changing anything if a syllabus
         * is also linked outside the selected Program/Cohort.
         */
        assertNoCrossScopeLinks(
                syllabusIds,
                programId,
                cohortId);

        for (Integer syllabusId : syllabusIds) {

            boolean hasImmutableHistory =
                    !revisionSnapshotRepository
                            .findBySyllabusIdOrderByCapturedAtAscIdAsc(
                                    syllabusId)
                            .isEmpty();

            if (hasImmutableHistory) {

                /*
                 * Revision history is immutable.
                 *
                 * Never delete/update revision snapshots.
                 * Keep this syllabus as the parent of its history,
                 * but remove it from the active curriculum identity.
                 */
                retireAuditPreservedSyllabus(
                        syllabusId,
                        cohortName);

            } else {

                /*
                 * No immutable history:
                 * physical delete is allowed.
                 */
                deleteHistoryFreeSyllabus(
                        syllabusId);
            }
        }

        return syllabusIds.size();
    }

    private void assertResetEnabled() {

        if (!cohortResetEnabled) {
            throw new ForbiddenOperationException(
                    "Cohort reset is disabled. "
                            + "Enable ALLOW_COHORT_RESET only "
                            + "for maintenance.");
        }
    }

    private void assertCohortBelongsToProgram(
            Cohort cohort,
            Integer programId) {

        if (cohort.getProgram() == null
                || cohort.getProgram().getId() == null
                || !Objects.equals(
                        programId,
                        cohort.getProgram().getId())) {

            throw new IllegalArgumentException(
                    "The selected cohort does not belong "
                            + "to the selected program.");
        }
    }

    private String normalizeCohortName(
            Cohort cohort) {

        String cohortName =
                cohort.getName() == null
                        ? ""
                        : cohort.getName().trim();

        if (cohortName.isBlank()) {
            throw new IllegalStateException(
                    "The selected cohort has no valid name.");
        }

        return cohortName;
    }

    private void assertConfirmation(
            String confirmation,
            String cohortName) {

        if (confirmation == null
                || !cohortName.equals(
                        confirmation.trim())) {

            throw new IllegalArgumentException(
                    "Confirmation text must exactly match: "
                            + cohortName);
        }
    }

    private LinkedHashSet<Integer> collectSyllabusIds(
            List<CourseProgram> mappings) {

        LinkedHashSet<Integer> ids =
                new LinkedHashSet<>();

        for (CourseProgram mapping : mappings) {

            Syllabus syllabus =
                    mapping.getSyllabus();

            if (syllabus != null
                    && syllabus.getId() != null) {

                ids.add(
                        syllabus.getId());
            }
        }

        return ids;
    }

    private void assertNoCrossScopeLinks(
            LinkedHashSet<Integer> syllabusIds,
            Integer programId,
            Integer cohortId) {

        for (Integer syllabusId : syllabusIds) {

            List<CourseProgram> allLinks =
                    courseProgramRepository
                            .findBySyllabus_Id(
                                    syllabusId);

            boolean outsideSelectedScope =
                    allLinks.stream()
                            .anyMatch(link ->
                                    link.getProgram() == null
                                            || link.getCohort() == null
                                            || !Objects.equals(
                                                    programId,
                                                    link.getProgram().getId())
                                            || !Objects.equals(
                                                    cohortId,
                                                    link.getCohort().getId()));

            if (outsideSelectedScope) {

                throw new IllegalStateException(
                        "Syllabus "
                                + syllabusId
                                + " is linked outside the selected "
                                + "Program/Cohort. Nothing was reset.");
            }
        }
    }

    private void deleteHistoryFreeSyllabus(
            Integer syllabusId) {

        Syllabus syllabus =
                syllabusRepository
                        .findByIdWithRelations(
                                syllabusId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Syllabus not found: "
                                                + syllabusId));

        /*
         * Maintenance reset is ADMIN-only.
         *
         * Normal delete requires mutable state, so normalize a
         * history-free workflow record to DRAFT first.
         */
        syllabus.setStatus(
                SyllabusStatus.DRAFT);

        syllabusRepository
                .saveAndFlush(
                        syllabus);

        /*
         * Normal delete already performs the correct cleanup:
         * - unlink CourseProgram
         * - unlink ClassSection
         * - remove syllabus-owned current data
         * - keep shared curriculum/master data
         */
        syllabusService.delete(
                syllabusId);
    }

    private void retireAuditPreservedSyllabus(
            Integer syllabusId,
            String originalCohortName) {

        Syllabus syllabus =
                syllabusRepository
                        .findByIdWithRelations(
                                syllabusId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Syllabus not found: "
                                                + syllabusId));

        /*
         * Keep CourseProgram rows.
         * Only remove the syllabus link.
         */
        List<CourseProgram> linkedCoursePrograms =
                courseProgramRepository
                        .findBySyllabus_Id(
                                syllabusId);

        for (CourseProgram courseProgram
                : linkedCoursePrograms) {

            courseProgram.setSyllabus(
                    null);
        }

        if (!linkedCoursePrograms.isEmpty()) {

            courseProgramRepository
                    .saveAll(
                            linkedCoursePrograms);

            courseProgramRepository.flush();
        }

        /*
         * Keep ClassSection rows.
         * Only detach the old syllabus.
         */
        List<ClassSection> linkedClassSections =
                classSectionRepository
                        .findBySyllabusId(
                                syllabusId);

        /*
         * Extra safety:
         * do not detach a ClassSection belonging to another cohort.
         */
        boolean outsideCohort =
                linkedClassSections.stream()
                        .anyMatch(section ->
                                section.getAcademicYear() != null
                                        && !section.getAcademicYear().isBlank()
                                        && !originalCohortName.equalsIgnoreCase(
                                                section.getAcademicYear().trim()));

        if (outsideCohort) {
            throw new IllegalStateException(
                    "Syllabus "
                            + syllabusId
                            + " has a ClassSection outside cohort "
                            + originalCohortName
                            + ". Nothing was reset.");
        }

        for (ClassSection classSection
                : linkedClassSections) {

            classSection.setSyllabus(
                    null);
        }

        if (!linkedClassSections.isEmpty()) {

            classSectionRepository
                    .saveAll(
                            linkedClassSections);

            classSectionRepository.flush();
        }

        /*
         * IMPORTANT:
         *
         * DO NOT delete revision snapshots.
         * DO NOT delete this syllabus parent.
         *
         * The parent remains as the anchor for immutable history.
         *
         * But the original logical identity must be released:
         *
         * course + program + cohort + semester
         *
         * Otherwise the importer and DB unique key would reject
         * the new CS2026 syllabus.
         */
        syllabus.setIsCurrent(
                Boolean.FALSE);

        syllabus.setProgram(
                AUDIT_PROGRAM_PREFIX
                        + syllabusId);

        syllabus.setAcademicYear(
                auditAcademicYear(
                        originalCohortName));

        /*
         * Preserve workflow status:
         * APPROVED stays APPROVED,
         * SUBMITTED stays SUBMITTED.
         *
         * We do not fabricate ARCHIVED state.
         */
        syllabusRepository
                .saveAndFlush(
                        syllabus);
    }

    private String auditAcademicYear(
            String cohortName) {

        Matcher matcher =
                YEAR_SUFFIX.matcher(
                        cohortName);

        if (matcher.find()) {
            return "AUDIT"
                    + matcher.group(1);
        }

        return "AUDIT0000";
    }
}