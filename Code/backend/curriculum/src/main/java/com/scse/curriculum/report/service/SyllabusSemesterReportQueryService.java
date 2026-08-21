package com.scse.curriculum.report.service;

import com.scse.curriculum.approval.entity.ApprovalRequest;
import com.scse.curriculum.instructor.entity.Instructor;
import com.scse.curriculum.instructor.repository.InstructorRepository;
import com.scse.curriculum.report.dto.SyllabusSemesterReportData;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.user.entity.UserAccount;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SyllabusSemesterReportQueryService {
    private final EntityManager entityManager;
    private final InstructorRepository instructorRepository;

    @Transactional(readOnly = true)
    public SyllabusSemesterReportData load(String academicYear, String semester,
                                           Integer programId, Integer cohortId,
                                           String status) {
        String year = required(academicYear, "academicYear");
        String term = required(semester, "semester");
        SyllabusStatus parsedStatus = parseStatus(status);
        validateProgramCohort(programId, cohortId);

        StringBuilder jpql = new StringBuilder("""
            SELECT DISTINCT s FROM Syllabus s
            JOIN FETCH s.course c
            LEFT JOIN FETCH s.createdBy creator
            LEFT JOIN FETCH s.approvedBy approver
            WHERE s.academicYear = :academicYear AND s.semester = :semester
            """);
        if (parsedStatus != null) jpql.append(" AND s.status = :status ");
        if (programId != null) jpql.append(" AND EXISTS (SELECT cp.id FROM CourseProgram cp WHERE cp.course = c AND cp.program.id = :programId ");
        if (programId != null && cohortId != null) jpql.append(" AND (cp.cohort.id = :cohortId OR cp.cohort IS NULL) ");
        if (programId != null) jpql.append(") ");
        jpql.append(" ORDER BY c.courseCode, s.versionNumber DESC, s.updatedAt DESC, s.id DESC");

        TypedQuery<Syllabus> query = entityManager.createQuery(jpql.toString(), Syllabus.class)
                .setParameter("academicYear", year).setParameter("semester", term);
        if (parsedStatus != null) query.setParameter("status", parsedStatus);
        if (programId != null) query.setParameter("programId", programId);
        if (programId != null && cohortId != null) query.setParameter("cohortId", cohortId);

        List<Syllabus> representatives = selectRepresentatives(query.getResultList());
        List<Integer> syllabusIds = representatives.stream().map(Syllabus::getId).filter(Objects::nonNull).toList();
        Map<Integer, ApprovalRequest> finalApprovals = loadFinalApprovals(syllabusIds);
        Map<Integer, String> cohortsByCourse = loadCohorts(representatives, programId, cohortId);
        Map<Integer, Instructor> instructors = loadInstructors(representatives, finalApprovals);

        List<SyllabusSemesterReportData.Row> rows = representatives.stream()
                .map(s -> toRow(s, finalApprovals.get(s.getId()), cohortsByCourse.get(s.getCourse().getId()), instructors))
                .sorted(Comparator.comparing(SyllabusSemesterReportData.Row::getCourseCode,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        Object[] scope = loadScope(programId, cohortId);
        return SyllabusSemesterReportData.builder()
                .academicYear(year).semester(term).programId(programId)
                .programCode(scope[0] == null ? null : String.valueOf(scope[0]))
                .cohortId(cohortId).cohortName(scope[1] == null ? null : String.valueOf(scope[1]))
                .status(parsedStatus == null ? "ALL" : parsedStatus.name()).rows(rows).build();
    }

    static List<Syllabus> selectRepresentatives(List<Syllabus> candidates) {
        Map<Integer, Syllabus> selected = new LinkedHashMap<>();
        if (candidates == null) return List.of();
        Comparator<Syllabus> preference = Comparator
                .comparing((Syllabus s) -> Boolean.TRUE.equals(s.getIsCurrent()))
                .thenComparing(s -> value(s.getVersionNumber()))
                .thenComparing(s -> date(s.getUpdatedAt()))
                .thenComparing(s -> value(s.getId()));
        for (Syllabus syllabus : candidates) {
            if (syllabus == null || syllabus.getCourse() == null || syllabus.getCourse().getId() == null) continue;
            selected.merge(syllabus.getCourse().getId(), syllabus,
                    (left, right) -> preference.compare(left, right) >= 0 ? left : right);
        }
        return new ArrayList<>(selected.values());
    }

    private Map<Integer, ApprovalRequest> loadFinalApprovals(List<Integer> ids) {
        if (ids.isEmpty()) return Map.of();
        List<ApprovalRequest> approvals = entityManager.createQuery("""
            SELECT a FROM ApprovalRequest a
            LEFT JOIN FETCH a.reviewedBy
            WHERE a.syllabus.id IN :ids AND a.resolvedAt IS NOT NULL
            ORDER BY a.syllabus.id, a.resolvedAt DESC, a.id DESC
            """, ApprovalRequest.class).setParameter("ids", ids).getResultList();
        Map<Integer, ApprovalRequest> result = new LinkedHashMap<>();
        for (ApprovalRequest approval : approvals) result.putIfAbsent(approval.getSyllabus().getId(), approval);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, String> loadCohorts(List<Syllabus> syllabuses, Integer programId, Integer cohortId) {
        List<Integer> courseIds = syllabuses.stream().map(s -> s.getCourse().getId()).distinct().toList();
        if (courseIds.isEmpty()) return Map.of();
        StringBuilder jpql = new StringBuilder("""
            SELECT cp.course.id, cp.cohort.name FROM CourseProgram cp
            WHERE cp.course.id IN :courseIds AND cp.cohort IS NOT NULL
            """);
        if (programId != null) jpql.append(" AND cp.program.id = :programId");
        if (cohortId != null) jpql.append(" AND cp.cohort.id = :cohortId");
        var q = entityManager.createQuery(jpql.toString()).setParameter("courseIds", courseIds);
        if (programId != null) q.setParameter("programId", programId);
        if (cohortId != null) q.setParameter("cohortId", cohortId);
        List<Object[]> data = q.getResultList();
        return data.stream().collect(Collectors.groupingBy(r -> (Integer) r[0], LinkedHashMap::new,
                Collectors.mapping(r -> String.valueOf(r[1]), Collectors.collectingAndThen(
                        Collectors.toCollection(java.util.TreeSet::new), set -> String.join(", ", set)))));
    }

    private Map<Integer, Instructor> loadInstructors(List<Syllabus> syllabuses, Map<Integer, ApprovalRequest> approvals) {
        List<Integer> ids = new ArrayList<>();
        for (Syllabus s : syllabuses) addInstructorId(ids, s.getCreatedBy());
        for (ApprovalRequest a : approvals.values()) addInstructorId(ids, a.getReviewedBy());
        if (ids.isEmpty()) return Map.of();
        return instructorRepository.findAllById(ids.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(Instructor::getId, Function.identity()));
    }

    private SyllabusSemesterReportData.Row toRow(Syllabus s, ApprovalRequest approval, String cohortNames,
                                                  Map<Integer, Instructor> instructors) {
        UserAccount creator = s.getCreatedBy();
        UserAccount reviewer = approval != null && approval.getReviewedBy() != null
                ? approval.getReviewedBy() : s.getApprovedBy();
        LocalDateTime reviewedAt = approval != null ? approval.getResolvedAt() : s.getApprovedAt();
        return SyllabusSemesterReportData.Row.builder()
                .syllabusId(s.getId()).courseId(s.getCourse().getId()).courseCode(s.getCourse().getCourseCode())
                .courseName(s.getCourse().getName()).courseNameVn(s.getCourse().getNameVn())
                .versionNumber(s.getVersionNumber()).versionLabel(s.getVersionLabel())
                .status(s.getStatus() == null ? "" : s.getStatus().name())
                .instructorUsername(username(creator)).instructorFullName(fullName(creator, instructors))
                .academicYear(s.getAcademicYear()).semester(s.getSemester())
                .cohortNames(cohortNames == null || cohortNames.isBlank() ? "Chưa gắn cohort" : cohortNames)
                .createdAt(s.getCreatedAt()).submittedAt(s.getSubmittedAt()).approvedAt(s.getApprovedAt())
                .finalReviewerUsername(username(reviewer)).finalReviewerFullName(fullName(reviewer, instructors))
                .finalApprovalStep(approval == null || approval.getStep() == null ? "" : approval.getStep().name())
                .finalApprovalStatus(approval == null || approval.getStatus() == null ? "" : approval.getStatus().name())
                .finalReviewedAt(reviewedAt).finalComment(approval == null ? "" : approval.getComment()).build();
    }

    private void validateProgramCohort(Integer programId, Integer cohortId) {
        if (cohortId != null && programId == null) throw new IllegalArgumentException("programId là bắt buộc khi có cohortId.");
        if (cohortId == null) return;
        Long count = entityManager.createQuery("SELECT COUNT(c) FROM Cohort c WHERE c.id=:cohortId AND c.program.id=:programId", Long.class)
                .setParameter("cohortId", cohortId).setParameter("programId", programId).getSingleResult();
        if (count == 0) throw new IllegalArgumentException("Cohort không thuộc chương trình đã chọn.");
    }

    private Object[] loadScope(Integer programId, Integer cohortId) {
        String programCode = null, cohortName = null;
        if (programId != null) programCode = entityManager.createQuery("SELECT p.code FROM Program p WHERE p.id=:id", String.class)
                .setParameter("id", programId).getResultStream().findFirst().orElse(null);
        if (cohortId != null) cohortName = entityManager.createQuery("SELECT c.name FROM Cohort c WHERE c.id=:id", String.class)
                .setParameter("id", cohortId).getResultStream().findFirst().orElse(null);
        return new Object[]{programCode, cohortName};
    }

    private static SyllabusStatus parseStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) return null;
        try { return SyllabusStatus.valueOf(status.trim().toUpperCase()); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Trạng thái syllabus không hợp lệ: " + status); }
    }
    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " là bắt buộc.");
        return value.trim();
    }
    private static int value(Integer v) { return v == null ? 0 : v; }
    private static LocalDateTime date(LocalDateTime v) { return v == null ? LocalDateTime.MIN : v; }
    private static void addInstructorId(List<Integer> ids, UserAccount u) { if (u != null && u.getInstructorId() != null) ids.add(u.getInstructorId()); }
    private static String username(UserAccount u) { return u == null || u.getUsername() == null ? "" : u.getUsername(); }
    private static String fullName(UserAccount u, Map<Integer, Instructor> instructors) {
        if (u == null || u.getInstructorId() == null) return username(u);
        Instructor i = instructors.get(u.getInstructorId());
        return i == null || i.getFullName() == null ? username(u) : i.getFullName();
    }
}
