package com.scse.curriculum.report.service;

import com.scse.curriculum.approval.entity.ApprovalRequest;
import com.scse.curriculum.auth.security.CurrentUserService;
import com.scse.curriculum.common.exception.ForbiddenOperationException;
import com.scse.curriculum.report.dto.SyllabusSemesterReportData;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SyllabusSemesterReportQueryService {

    private final EntityManager entityManager;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public SyllabusSemesterReportData load(
            String academicYear,
            String semester,
            Integer programId,
            Integer cohortId,
            String status) {

        String year = required(academicYear, "academicYear");
        String term = required(semester, "semester");
        SyllabusStatus parsedStatus = parseStatus(status);

        validateProgramCohort(programId, cohortId);

        Integer managedMajorId = currentManagedMajorScope();

        StringBuilder jpql = new StringBuilder("""
                SELECT DISTINCT s
                FROM Syllabus s
                JOIN FETCH s.course c
                LEFT JOIN FETCH s.createdBy creator
                LEFT JOIN FETCH s.approvedBy approver
                WHERE s.academicYear = :academicYear
                  AND s.semester = :semester
                """);

        if (parsedStatus != null) {
            jpql.append(" AND s.status = :status ");
        }

        /*
         * Keep all CourseProgram-based scope conditions inside the same EXISTS.
         * This is important when a course belongs to more than one program:
         * a DEPT_HEAD must not pass the managed-major check through one program
         * while filtering the report by a different program.
         */
        if (managedMajorId != null || programId != null) {
            jpql.append("""
                     AND EXISTS (
                         SELECT cp.id
                         FROM CourseProgram cp
                         WHERE cp.course = c
                    """);

            if (managedMajorId != null) {
                jpql.append(" AND cp.program.major.id = :managedMajorId ");
            }

            if (programId != null) {
                jpql.append(" AND cp.program.id = :programId ");

                if (cohortId != null) {
                    jpql.append(" AND (cp.cohort.id = :cohortId OR cp.cohort IS NULL) ");
                }
            }

            jpql.append(") ");
        }

        jpql.append(
                " ORDER BY c.courseCode, s.versionNumber DESC, s.updatedAt DESC, s.id DESC");

        TypedQuery<Syllabus> query = entityManager
                .createQuery(jpql.toString(), Syllabus.class)
                .setParameter("academicYear", year)
                .setParameter("semester", term);

        if (parsedStatus != null) {
            query.setParameter("status", parsedStatus);
        }

        if (managedMajorId != null) {
            query.setParameter("managedMajorId", managedMajorId);
        }

        if (programId != null) {
            query.setParameter("programId", programId);
        }

        if (programId != null && cohortId != null) {
            query.setParameter("cohortId", cohortId);
        }

        List<Syllabus> representatives =
                selectRepresentatives(query.getResultList());

        List<Integer> syllabusIds = representatives.stream()
                .map(Syllabus::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Integer, ApprovalRequest> finalApprovals =
                loadFinalApprovals(syllabusIds);

        Map<Integer, String> cohortsByCourse =
                loadCohorts(representatives, programId, cohortId);

        List<SyllabusSemesterReportData.Row> rows = representatives.stream()
                .map(s -> toRow(
                        s,
                        finalApprovals.get(s.getId()),
                        cohortsByCourse.get(s.getCourse().getId())))
                .sorted(Comparator.comparing(
                        SyllabusSemesterReportData.Row::getCourseCode,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        Object[] scope = loadScope(programId, cohortId);

        return SyllabusSemesterReportData.builder()
                .academicYear(year)
                .semester(term)
                .programId(programId)
                .programCode(
                        scope[0] == null
                                ? null
                                : String.valueOf(scope[0]))
                .cohortId(cohortId)
                .cohortName(
                        scope[1] == null
                                ? null
                                : String.valueOf(scope[1]))
                .status(
                        parsedStatus == null
                                ? "ALL"
                                : parsedStatus.name())
                .rows(rows)
                .build();
    }

    /**
     * DEPT_HEAD authorization scope is defined by UserAccount.managedMajor.
     * Other roles are not restricted by a managed-major scope here.
     */
    private Integer currentManagedMajorScope() {
        UserAccount user = currentUserService.getCurrentUser();

        if (user.getRole() != UserRole.DEPT_HEAD) {
            return null;
        }

        if (user.getManagedMajor() == null
                || user.getManagedMajor().getId() == null) {
            throw new ForbiddenOperationException(
                    "The Department Head account is not assigned to a managed major.");
        }

        return user.getManagedMajor().getId();
    }

    static List<Syllabus> selectRepresentatives(
            List<Syllabus> candidates) {

        Map<Integer, Syllabus> selected =
                new LinkedHashMap<>();

        if (candidates == null) {
            return List.of();
        }

        Comparator<Syllabus> preference = Comparator
                .comparing(
                        (Syllabus s) ->
                                Boolean.TRUE.equals(
                                        s.getIsCurrent()))
                .thenComparing(
                        s -> value(s.getVersionNumber()))
                .thenComparing(
                        s -> date(s.getUpdatedAt()))
                .thenComparing(
                        s -> value(s.getId()));

        for (Syllabus syllabus : candidates) {
            if (syllabus == null
                    || syllabus.getCourse() == null
                    || syllabus.getCourse().getId() == null) {
                continue;
            }

            selected.merge(
                    syllabus.getCourse().getId(),
                    syllabus,
                    (left, right) ->
                            preference.compare(left, right) >= 0
                                    ? left
                                    : right);
        }

        return new ArrayList<>(selected.values());
    }

    private Map<Integer, ApprovalRequest> loadFinalApprovals(
            List<Integer> ids) {

        if (ids.isEmpty()) {
            return Map.of();
        }

        List<ApprovalRequest> approvals =
                entityManager.createQuery("""
                        SELECT a
                        FROM ApprovalRequest a
                        LEFT JOIN FETCH a.reviewedBy
                        WHERE a.syllabus.id IN :ids
                          AND a.resolvedAt IS NOT NULL
                        ORDER BY a.syllabus.id,
                                 a.resolvedAt DESC,
                                 a.id DESC
                        """, ApprovalRequest.class)
                        .setParameter("ids", ids)
                        .getResultList();

        Map<Integer, ApprovalRequest> result =
                new LinkedHashMap<>();

        for (ApprovalRequest approval : approvals) {
            result.putIfAbsent(
                    approval.getSyllabus().getId(),
                    approval);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, String> loadCohorts(
            List<Syllabus> syllabuses,
            Integer programId,
            Integer cohortId) {

        List<Integer> courseIds = syllabuses.stream()
                .map(s -> s.getCourse().getId())
                .distinct()
                .toList();

        if (courseIds.isEmpty()) {
            return Map.of();
        }

        StringBuilder jpql = new StringBuilder("""
                SELECT cp.course.id, cp.cohort.name
                FROM CourseProgram cp
                WHERE cp.course.id IN :courseIds
                  AND cp.cohort IS NOT NULL
                """);

        if (programId != null) {
            jpql.append(
                    " AND cp.program.id = :programId");
        }

        if (cohortId != null) {
            jpql.append(
                    " AND cp.cohort.id = :cohortId");
        }

        var query = entityManager.createQuery(
                        jpql.toString())
                .setParameter(
                        "courseIds",
                        courseIds);

        if (programId != null) {
            query.setParameter(
                    "programId",
                    programId);
        }

        if (cohortId != null) {
            query.setParameter(
                    "cohortId",
                    cohortId);
        }

        List<Object[]> data =
                query.getResultList();

        return data.stream()
                .collect(Collectors.groupingBy(
                        row -> (Integer) row[0],
                        LinkedHashMap::new,
                        Collectors.mapping(
                                row -> String.valueOf(row[1]),
                                Collectors.collectingAndThen(
                                        Collectors.toCollection(
                                                java.util.TreeSet::new),
                                        set -> String.join(", ", set)))));
    }

    private SyllabusSemesterReportData.Row toRow(
            Syllabus syllabus,
            ApprovalRequest approval,
            String cohortNames) {

        UserAccount creator =
                syllabus.getCreatedBy();

        UserAccount reviewer =
                approval != null
                        && approval.getReviewedBy() != null
                        ? approval.getReviewedBy()
                        : syllabus.getApprovedBy();

        LocalDateTime reviewedAt =
                approval != null
                        ? approval.getResolvedAt()
                        : syllabus.getApprovedAt();

        return SyllabusSemesterReportData.Row.builder()
                .syllabusId(syllabus.getId())
                .courseId(syllabus.getCourse().getId())
                .courseCode(
                        syllabus.getCourse().getCourseCode())
                .courseName(
                        syllabus.getCourse().getName())
                .courseNameVn(
                        syllabus.getCourse().getNameVn())
                .versionNumber(
                        syllabus.getVersionNumber())
                .versionLabel(
                        syllabus.getVersionLabel())
                .status(
                        syllabus.getStatus() == null
                                ? ""
                                : syllabus.getStatus().name())
                .instructorUsername(
                        username(creator))
                .instructorFullName(
                        fullName(creator))
                .academicYear(
                        syllabus.getAcademicYear())
                .semester(
                        syllabus.getSemester())
                .cohortNames(
                        cohortNames == null
                                || cohortNames.isBlank()
                                ? "Chưa gắn cohort"
                                : cohortNames)
                .createdAt(
                        syllabus.getCreatedAt())
                .submittedAt(
                        syllabus.getSubmittedAt())
                .approvedAt(
                        syllabus.getApprovedAt())
                .finalReviewerUsername(
                        username(reviewer))
                .finalReviewerFullName(
                        fullName(reviewer))
                .finalApprovalStep(
                        approval == null
                                || approval.getStep() == null
                                ? ""
                                : approval.getStep().name())
                .finalApprovalStatus(
                        approval == null
                                || approval.getStatus() == null
                                ? ""
                                : approval.getStatus().name())
                .finalReviewedAt(
                        reviewedAt)
                .finalComment(
                        approval == null
                                ? ""
                                : approval.getComment())
                .build();
    }

    private void validateProgramCohort(
            Integer programId,
            Integer cohortId) {

        if (cohortId != null && programId == null) {
            throw new IllegalArgumentException(
                    "programId là bắt buộc khi có cohortId.");
        }

        if (cohortId == null) {
            return;
        }

        Long count = entityManager.createQuery(
                        """
                        SELECT COUNT(c)
                        FROM Cohort c
                        WHERE c.id = :cohortId
                          AND c.program.id = :programId
                        """,
                        Long.class)
                .setParameter(
                        "cohortId",
                        cohortId)
                .setParameter(
                        "programId",
                        programId)
                .getSingleResult();

        if (count == 0) {
            throw new IllegalArgumentException(
                    "Cohort không thuộc chương trình đã chọn.");
        }
    }

    private Object[] loadScope(
            Integer programId,
            Integer cohortId) {

        String programCode = null;
        String cohortName = null;

        if (programId != null) {
            programCode = entityManager.createQuery(
                            """
                            SELECT p.code
                            FROM Program p
                            WHERE p.id = :id
                            """,
                            String.class)
                    .setParameter(
                            "id",
                            programId)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
        }

        if (cohortId != null) {
            cohortName = entityManager.createQuery(
                            """
                            SELECT c.name
                            FROM Cohort c
                            WHERE c.id = :id
                            """,
                            String.class)
                    .setParameter(
                            "id",
                            cohortId)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
        }

        return new Object[] {
                programCode,
                cohortName
        };
    }

    private static SyllabusStatus parseStatus(
            String status) {

        if (status == null
                || status.isBlank()
                || "ALL".equalsIgnoreCase(status)) {
            return null;
        }

        try {
            return SyllabusStatus.valueOf(
                    status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Trạng thái syllabus không hợp lệ: "
                            + status);
        }
    }

    private static String required(
            String value,
            String name) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    name + " là bắt buộc.");
        }

        return value.trim();
    }

    private static int value(
            Integer value) {

        return value == null
                ? 0
                : value;
    }

    private static LocalDateTime date(
            LocalDateTime value) {

        return value == null
                ? LocalDateTime.MIN
                : value;
    }

    private static String username(
            UserAccount user) {

        return user == null
                || user.getUsername() == null
                ? ""
                : user.getUsername();
    }

    private static String fullName(
            UserAccount user) {

        if (user == null) {
            return "";
        }

        if (user.getFullName() != null
                && !user.getFullName().isBlank()) {
            return user.getFullName().trim();
        }

        return username(user);
    }
}
