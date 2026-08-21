package com.scse.curriculum.dashboard.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scse.curriculum.classsection.entity.ClassSection;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.coursetype.entity.CourseType;
import com.scse.curriculum.dashboard.dto.DashboardAdminResponse;
import com.scse.curriculum.deadline.entity.SyllabusDeadline;
import com.scse.curriculum.instructor.entity.Instructor;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.user.entity.UserAccount;

import jakarta.persistence.EntityManager;

@Service
@Transactional(readOnly = true)
public class AdminDashboardQueryService {

   private static final Set<SyllabusStatus> SUBMITTED_STATES = Set.of(
        SyllabusStatus.SUBMITTED,
        SyllabusStatus.UNDER_REVIEW,
        SyllabusStatus.APPROVED);

    private final EntityManager entityManager;
    private final AdminDashboardTimeProvider timeProvider;

    public AdminDashboardQueryService(
            EntityManager entityManager,
            AdminDashboardTimeProvider timeProvider) {
        this.entityManager = entityManager;
        this.timeProvider = timeProvider;
    }

    public DashboardAdminResponse getDashboard(
        String requestedAcademicYear,
        Integer requestedSemester,
        Integer programId,
        Integer cohortId) {
            validateProgramCohortScope(programId, cohortId);
        List<TermRow> terms = loadTerms();
        TermRow scope = resolveScope(terms, requestedAcademicYear, requestedSemester);

        DashboardAdminResponse response = new DashboardAdminResponse();
        response.setGeneratedAt(timeProvider.now());
        response.setDataSource("ClassSection assignment + matching Syllabus + SyllabusDeadline + CourseProgram course type");
        response.setTerms(toTermOptions(terms));

        response.setTotalUsers(count("SELECT COUNT(u) FROM UserAccount u"));
        response.setTotalPrograms(count("SELECT COUNT(p) FROM Program p"));
        response.setTotalCourses(count("SELECT COUNT(c) FROM Course c"));
        response.setTotalDepartments(count("SELECT COUNT(d) FROM Department d"));
        response.setTotalSyllabuses(count("SELECT COUNT(s) FROM Syllabus s"));

        if (scope == null) {
            response.setDeadlineConfigured(false);
            response.setDeadlinePassed(false);
            return response;
        }

        response.setAcademicYear(scope.academicYear());
        response.setSemester(scope.semester());

        LocalDateTime now = timeProvider.now();
        SyllabusDeadline deadline = loadDeadline(scope.academicYear(), scope.semester());
        boolean deadlinePassed = deadline != null && deadline.getDeadlineAt() != null
                && now.isAfter(deadline.getDeadlineAt());
        response.setDeadlineConfigured(deadline != null);
        response.setDeadlinePassed(deadlinePassed);
        if (deadline != null) {
            response.setDeadlineId(deadline.getId());
            response.setDeadlineAt(deadline.getDeadlineAt());
        }

        List<ClassSection> sections =
        loadSections(
                scope.academicYear(),
                scope.semester(),
                programId,
                cohortId);
        response.setTotalAssignedSections(sections.size());
        if (sections.isEmpty()) {
            return response;
        }

        Set<Integer> instructorIds = sections.stream()
                .map(ClassSection::getInstructor)
                .filter(Objects::nonNull)
                .map(Instructor::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
                /*
 * Dashboard KPI:
 * số giảng viên thực sự được phân công trong
 * phạm vi Academic Year / Semester / Program / Cohort
 * hiện tại.
 *
 * Không dùng tổng Instructor active toàn hệ thống.
 */
response.setTotalInstructors(
        instructorIds.size()
);
        Set<Integer> courseIds = sections.stream()
                .map(ClassSection::getCourse)
                .filter(Objects::nonNull)
                .map(Course::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Integer, UserAccount> accountByInstructor = loadAccounts(instructorIds);
        List<Syllabus> syllabuses = loadSyllabuses(
                scope.academicYear(), scope.semester(), courseIds,
                accountByInstructor.values().stream().map(UserAccount::getId).collect(Collectors.toSet()));
        Map<Integer, CourseType> primaryCourseTypeByCourse =
        programId == null
                ? Map.of()
                : loadCourseTypes(
                        courseIds,
                        programId,
                        cohortId);

        DashboardAggregation result = aggregate(
                sections,
                accountByInstructor,
                syllabuses,
                primaryCourseTypeByCourse,
                deadlinePassed);

        long assignedCourseCount =
        sections.stream()
                .map(section ->
                        section.getCourse() != null
                                ? section.getCourse().getId()
                                : null)
                .filter(Objects::nonNull)
                .distinct()
                .count();

response.setTotalAssignedCourses(
        assignedCourseCount);
        response.setSubmittedAssignments(result.submittedCount());
        response.setNotSubmittedAssignments(result.missingCount());
        response.setOverdueAssignments(result.overdueCount());
        response.setApprovedAssignments(
        result.approvedCount());

response.setApprovalRate(
        result.approvalRate());
        response.setSyllabusStatusOverview(
        result.statusOverview());
        response.setFacultiesNotSubmitted(result.faculties());
        if (programId == null) {
    /*
     * Course Group là thuộc tính của CourseProgram,
     * không phải Course.
     *
     * Không có Program thì một môn có thể mang
     * nhiều Course Type khác nhau.
     */
    response.setCourseGroupStatistics(
            List.of());
} else {
    response.setCourseGroupStatistics(
            result.groups());
}
        return response;
    }

    private long count(String jpql) {
        Long value = entityManager.createQuery(jpql, Long.class).getSingleResult();
        return value == null ? 0L : value;
    }
private void validateProgramCohortScope(
        Integer programId,
        Integer cohortId) {

    if (cohortId != null && programId == null) {
        throw new IllegalArgumentException(
                "programId is required when cohortId is provided.");
    }

    if (programId == null) {
        return;
    }

    Long programCount = entityManager.createQuery(
            """
            SELECT COUNT(p)
            FROM Program p
            WHERE p.id = :programId
              AND p.isActive = true
            """,
            Long.class)
            .setParameter("programId", programId)
            .getSingleResult();

    if (programCount == null || programCount == 0) {
        throw new IllegalArgumentException(
                "Program does not exist or is inactive.");
    }

    if (cohortId == null) {
        return;
    }

    Long cohortCount = entityManager.createQuery(
            """
            SELECT COUNT(c)
            FROM Cohort c
            WHERE c.id = :cohortId
              AND c.program.id = :programId
              AND c.isActive = true
            """,
            Long.class)
            .setParameter("cohortId", cohortId)
            .setParameter("programId", programId)
            .getSingleResult();

    if (cohortCount == null || cohortCount == 0) {
        throw new IllegalArgumentException(
                "The selected cohort does not belong to the selected program.");
    }
}

public List<DashboardAdminResponse.TermOption> getTermOptions() {
    return toTermOptions(loadTerms());
}
    private List<TermRow> loadTerms() {
        return entityManager.createQuery(
                "SELECT cs.academicYear, cs.semester, COUNT(cs) " +
                "FROM ClassSection cs " +
                "WHERE cs.isActive = true " +
                "GROUP BY cs.academicYear, cs.semester " +
                "ORDER BY cs.academicYear DESC, cs.semester DESC",
                Object[].class)
                .getResultList().stream()
                .map(row -> new TermRow((String) row[0], (Integer) row[1], ((Number) row[2]).longValue()))
                .toList();
    }

    private TermRow resolveScope(List<TermRow> terms, String academicYear, Integer semester) {
        if (terms.isEmpty()) {
            return null;
        }
        if (academicYear == null || academicYear.isBlank()) {
            return terms.get(0);
        }
        String normalized = academicYear.trim();
        if (semester == null) {
            return terms.stream()
                    .filter(t -> t.academicYear().equalsIgnoreCase(normalized))
                    .max(Comparator.comparing(TermRow::semester))
                    .orElse(new TermRow(normalized, 1, 0));
        }
        return new TermRow(normalized, semester, 0);
    }

    private List<DashboardAdminResponse.TermOption> toTermOptions(List<TermRow> terms) {
        return terms.stream().map(term -> {
            DashboardAdminResponse.TermOption option = new DashboardAdminResponse.TermOption();
            option.setAcademicYear(term.academicYear());
            option.setSemester(term.semester());
            option.setKey(term.academicYear() + "-S" + term.semester());
            option.setLabel(term.academicYear() + " · Học kỳ " + term.semester());
            option.setSectionCount(term.sectionCount());
            return option;
        }).toList();
    }

    private SyllabusDeadline loadDeadline(String academicYear, Integer semester) {
        return entityManager.createQuery(
                "SELECT d FROM SyllabusDeadline d " +
                "WHERE LOWER(d.academicYear) = LOWER(:year) " +
                "AND d.semester = :semester AND d.active = true",
                SyllabusDeadline.class)
                .setParameter("year", academicYear)
                .setParameter("semester", semester)
                .getResultStream().findFirst().orElse(null);
    }

    private List<ClassSection> loadSections(
        String academicYear,
        Integer semester,
        Integer programId,
        Integer cohortId) {

    return entityManager.createQuery(
            """
            SELECT DISTINCT cs
            FROM ClassSection cs
            JOIN FETCH cs.course c
            LEFT JOIN FETCH c.department d
            JOIN FETCH cs.instructor i
            LEFT JOIN FETCH i.department idp
            LEFT JOIN FETCH cs.syllabus linked

            WHERE cs.isActive = true

              AND LOWER(TRIM(cs.academicYear))
                    = LOWER(TRIM(:year))

              AND cs.semester = :semester

              AND (
                    :programId IS NULL
                    OR EXISTS (
                        SELECT cp.id
                        FROM CourseProgram cp
                        WHERE cp.course.id = c.id
                          AND cp.program.id = :programId
                          AND (
                                :cohortId IS NULL
                                OR cp.cohort IS NULL
                                OR cp.cohort.id = :cohortId
                              )
                    )
                  )

            ORDER BY
                i.fullName,
                c.courseCode,
                cs.groupNumber
            """,
            ClassSection.class)
            .setParameter("year", academicYear)
            .setParameter("semester", semester)
            .setParameter("programId", programId)
            .setParameter("cohortId", cohortId)
            .getResultList();
}

    private Map<Integer, UserAccount> loadAccounts(Set<Integer> instructorIds) {
        if (instructorIds.isEmpty()) {
            return Map.of();
        }
        return entityManager.createQuery(
                "SELECT u FROM UserAccount u WHERE u.instructorId IN :ids",
                UserAccount.class)
                .setParameter("ids", instructorIds)
                .getResultList().stream()
                .filter(u -> u.getInstructorId() != null)
                .collect(Collectors.toMap(
                        UserAccount::getInstructorId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private List<Syllabus> loadSyllabuses(
            String academicYear,
            Integer semester,
            Set<Integer> courseIds,
            Set<Integer> accountIds) {
        if (courseIds.isEmpty() || accountIds.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery(
                "SELECT s FROM Syllabus s " +
                "JOIN FETCH s.course c " +
                "JOIN FETCH s.createdBy u " +
                "WHERE c.id IN :courseIds " +
                "AND u.id IN :accountIds " +
                "AND LOWER(s.academicYear) = LOWER(:year) " +
                "AND s.semester = :semester",
                Syllabus.class)
                .setParameter("courseIds", courseIds)
                .setParameter("accountIds", accountIds)
                .setParameter("year", academicYear)
                .setParameter("semester", String.valueOf(semester))
                .getResultList();
    }

    private Map<Integer, CourseType> loadCourseTypes(
        Set<Integer> courseIds,
        Integer programId,
        Integer cohortId) {

    if (courseIds.isEmpty() || programId == null) {
        return Map.of();
    }

    String jpql;

    if (cohortId == null) {

        /*
         * Không chọn cohort:
         * lấy classification mặc định của Program,
         * tức mapping có cohort = NULL.
         */
        jpql = """
                SELECT cp
                FROM CourseProgram cp
                JOIN FETCH cp.course c
                JOIN FETCH cp.courseType ct
                WHERE c.id IN :courseIds
                  AND cp.program.id = :programId
                  AND cp.cohort IS NULL
                ORDER BY cp.id
                """;

    } else {

        /*
         * Có cohort:
         * lấy cả mapping riêng của cohort và mapping chung.
         * Mapping riêng được ưu tiên.
         */
        jpql = """
                SELECT cp
                FROM CourseProgram cp
                JOIN FETCH cp.course c
                JOIN FETCH cp.courseType ct
                LEFT JOIN FETCH cp.cohort co
                WHERE c.id IN :courseIds
                  AND cp.program.id = :programId
                  AND (
                        cp.cohort.id = :cohortId
                        OR cp.cohort IS NULL
                      )
                ORDER BY
                    c.id,
                    CASE
                        WHEN cp.cohort.id = :cohortId
                        THEN 0
                        ELSE 1
                    END,
                    cp.id
                """;
    }

    var query = entityManager.createQuery(
            jpql,
            CourseProgram.class)
            .setParameter(
                    "courseIds",
                    courseIds)
            .setParameter(
                    "programId",
                    programId);

    if (cohortId != null) {
        query.setParameter(
                "cohortId",
                cohortId);
    }

    List<CourseProgram> mappings =
            query.getResultList();

    Map<Integer, CourseType> result =
            new LinkedHashMap<>();

    for (CourseProgram mapping : mappings) {

        if (mapping.getCourse() == null
                || mapping.getCourseType() == null) {
            continue;
        }

        /*
         * putIfAbsent rất quan trọng:
         * vì query đã sort mapping riêng của cohort trước,
         * nên mapping riêng thắng mapping NULL.
         */
        result.putIfAbsent(
                mapping.getCourse().getId(),
                mapping.getCourseType());
    }

    return result;
}
    static DashboardAggregation aggregate(
            List<ClassSection> sections,
            Map<Integer, UserAccount> accountByInstructor,
            List<Syllabus> syllabuses,
            Map<Integer, CourseType> courseTypeByCourse,
            boolean deadlinePassed) {

        Map<AssignmentKey, AssignmentAccumulator> assignments = new LinkedHashMap<>();
        for (ClassSection section : sections) {
            if (section.getInstructor() == null || section.getCourse() == null) {
                continue;
            }
            AssignmentKey key = new AssignmentKey(
                    section.getInstructor().getId(),
                    section.getCourse().getId(),
                    section.getAcademicYear(),
                    section.getSemester());
            assignments.computeIfAbsent(key, ignored -> new AssignmentAccumulator(section))
                    .addSection(section);
        }

        Map<SyllabusKey, List<Syllabus>> syllabusIndex =
        syllabuses.stream()
                .filter(s ->
                        s.getCourse() != null
                        && s.getCreatedBy() != null
                        && s.getCreatedBy()
                                .getInstructorId() != null
                        && s.getAcademicYear() != null
                        && s.getSemester() != null)
                .collect(Collectors.groupingBy(
                        s -> new SyllabusKey(
                                s.getCreatedBy()
                                        .getInstructorId(),

                                s.getCourse()
                                        .getId(),

                                normalizeAcademicYear(
                                        s.getAcademicYear()),

                                parseSemesterValue(
                                        s.getSemester())
                        ),
                        LinkedHashMap::new,
                        Collectors.toList()));

        Comparator<Syllabus> latestComparator = Comparator
                .comparing((Syllabus s) -> Boolean.TRUE.equals(s.getIsCurrent()))
                .thenComparing(s -> s.getVersionNumber() == null ? 0 : s.getVersionNumber())
                .thenComparing(s -> s.getUpdatedAt() == null ? LocalDateTime.MIN : s.getUpdatedAt())
                .thenComparing(s -> s.getId() == null ? 0 : s.getId());

        for (Map.Entry<AssignmentKey, AssignmentAccumulator> entry : assignments.entrySet()) {
            AssignmentAccumulator assignment = entry.getValue();
            List<Syllabus> candidates =
        syllabusIndex.getOrDefault(
                new SyllabusKey(
                        entry.getKey()
                                .instructorId(),

                        entry.getKey()
                                .courseId(),

                        normalizeAcademicYear(
                                entry.getKey()
                                        .academicYear()),

                        entry.getKey()
                                .semester()
                ),
                List.of());
            Syllabus latest = candidates.stream().max(latestComparator).orElse(null);
            assignment.latestSyllabus = latest;
            assignment.submitted = latest != null && latest.getStatus() != null
                    && SUBMITTED_STATES.contains(latest.getStatus());
            assignment.overdue = !assignment.submitted && deadlinePassed;
        }

        Map<Integer, List<AssignmentAccumulator>> missingByInstructor = assignments.values().stream()
                .filter(a -> !a.submitted)
                .collect(Collectors.groupingBy(
                        a -> a.instructor.getId(), LinkedHashMap::new, Collectors.toList()));

        List<DashboardAdminResponse.FacultyNotSubmitted> facultyRows = new ArrayList<>();
        for (Map.Entry<Integer, List<AssignmentAccumulator>> entry : missingByInstructor.entrySet()) {
            List<AssignmentAccumulator> missing = entry.getValue();
            Instructor instructor = missing.get(0).instructor;
            UserAccount account = accountByInstructor.get(instructor.getId());

            DashboardAdminResponse.FacultyNotSubmitted row = new DashboardAdminResponse.FacultyNotSubmitted();
            row.setInstructorId(instructor.getId());
            row.setStaffCode(instructor.getStaffCode());
            row.setInstructorName(instructor.getFullName());
            row.setEmail(account != null && account.getEmail() != null ? account.getEmail() : instructor.getEmail());
            if (instructor.getDepartment() != null) {
                row.setDepartmentCode(instructor.getDepartment().getCode());
                row.setDepartmentName(instructor.getDepartment().getName());
            }
            row.setAssignedCourseCount(assignments.values().stream()
                    .filter(a -> a.instructor.getId().equals(instructor.getId())).count());
            row.setMissingCount(missing.size());
            row.setOverdueCount(missing.stream().filter(a -> a.overdue).count());
            row.setMissingCourses(missing.stream()
                    .sorted(Comparator.comparing(a -> a.course.getCourseCode()))
                    .map(AdminDashboardQueryService::toMissingCourse)
                    .toList());
            facultyRows.add(row);
        }
        facultyRows.sort(Comparator
                .comparingLong(DashboardAdminResponse.FacultyNotSubmitted::getOverdueCount).reversed()
                .thenComparingLong(DashboardAdminResponse.FacultyNotSubmitted::getMissingCount).reversed()
                .thenComparing(DashboardAdminResponse.FacultyNotSubmitted::getInstructorName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        Map<GroupKey, List<AssignmentAccumulator>> byGroup = assignments.values().stream()
                .collect(Collectors.groupingBy(a -> {
                    CourseType type = courseTypeByCourse.get(a.course.getId());
                    return type == null
                            ? new GroupKey(null, "UNCLASSIFIED", "Unclassified", "Chưa phân nhóm")
                            : new GroupKey(type.getId(), type.getCode(), type.getName(), type.getNameVn());
                }, LinkedHashMap::new, Collectors.toList()));

        List<DashboardAdminResponse.CourseGroupStatistic> groups = byGroup.entrySet().stream()
                .map(entry -> toGroupStatistic(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(
                        DashboardAdminResponse.CourseGroupStatistic::getCourseTypeNameVn,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

                Map<String, Long> statusOverview =
        new LinkedHashMap<>();

statusOverview.put("MISSING", 0L);
statusOverview.put("DRAFT", 0L);
statusOverview.put("SUBMITTED", 0L);
statusOverview.put("UNDER_REVIEW", 0L);
statusOverview.put("REVISION_REQUESTED", 0L);
statusOverview.put("REJECTED", 0L);
statusOverview.put("APPROVED", 0L);
statusOverview.put("ARCHIVED", 0L);

for (AssignmentAccumulator assignment
        : assignments.values()) {

    String status;

    if (assignment.latestSyllabus == null
            || assignment.latestSyllabus
                    .getStatus() == null) {

        status = "MISSING";

    } else {

        status =
                assignment.latestSyllabus
                        .getStatus()
                        .name();
    }

    statusOverview.merge(
            status,
            1L,
            Long::sum);
}
        long submitted =
        assignments.values()
                .stream()
                .filter(a -> a.submitted)
                .count();

long missing =
        assignments.size() - submitted;

long overdue =
        assignments.values()
                .stream()
                .filter(a -> a.overdue)
                .count();

/*
 * Syllabus chỉ được tính Approved khi
 * latest syllabus đúng scope hiện tại
 * có trạng thái APPROVED.
 */
long approved =
        assignments.values()
                .stream()
                .filter(a ->
                        a.latestSyllabus != null
                        && a.latestSyllabus.getStatus()
                                == SyllabusStatus.APPROVED)
                .count();

/*
 * Approval Rate:
 * số syllabus approved / tổng số
 * instructor-course responsibility.
 */
double approvalRate =
        assignments.isEmpty()
                ? 0.0
                : Math.round(
                        approved
                        * 10000.0
                        / assignments.size()
                ) / 100.0;

return new DashboardAggregation(
        assignments.size(),
        facultyRows,
        groups,
        submitted,
        missing,
        overdue,
        approved,
        approvalRate,
        statusOverview
);
    }

    private static DashboardAdminResponse.MissingCourse toMissingCourse(AssignmentAccumulator assignment) {
        DashboardAdminResponse.MissingCourse row = new DashboardAdminResponse.MissingCourse();
        row.setCourseId(assignment.course.getId());
        row.setCourseCode(assignment.course.getCourseCode());
        row.setCourseName(assignment.course.getName());
        row.setCourseNameVn(assignment.course.getNameVn());
        row.setSectionCount(assignment.sectionIds.size());
        row.setOverdue(assignment.overdue);
        if (assignment.latestSyllabus == null) {
            row.setLatestStatus("MISSING");
        } else {
            row.setSyllabusId(assignment.latestSyllabus.getId());
            row.setVersionLabel(assignment.latestSyllabus.getVersionLabel());
            row.setLatestStatus(assignment.latestSyllabus.getStatus() == null
                    ? "UNKNOWN"
                    : assignment.latestSyllabus.getStatus().name());
        }
        return row;
    }

    private static DashboardAdminResponse.CourseGroupStatistic toGroupStatistic(
        GroupKey key,
        List<AssignmentAccumulator> assignments) {

    DashboardAdminResponse.CourseGroupStatistic row =
            new DashboardAdminResponse.CourseGroupStatistic();

    row.setCourseTypeId(key.id());
    row.setCourseTypeCode(key.code());
    row.setCourseTypeName(key.name());
    row.setCourseTypeNameVn(key.nameVn());

    /*
     * Gom tất cả responsibility theo course.
     *
     * Ví dụ:
     * CH011IU - GV001
     * CH011IU - GV002
     *
     * vẫn chỉ là 1 course.
     */
    Map<Integer, List<AssignmentAccumulator>> byCourse =
            assignments.stream()
                    .filter(a ->
                            a.course != null
                            && a.course.getId() != null)
                    .collect(Collectors.groupingBy(
                            a -> a.course.getId(),
                            LinkedHashMap::new,
                            Collectors.toList()));

    long assignedCourses =
            byCourse.size();

    /*
     * Một course được xem là Submitted
     * chỉ khi TẤT CẢ trách nhiệm syllabus
     * của course đó đã được nộp.
     *
     * Tránh trường hợp:
     * GV001 đã nộp
     * GV002 chưa nộp
     *
     * mà Dashboard vẫn báo cả môn đã hoàn tất.
     */
    long submittedCourses =
            byCourse.values()
                    .stream()
                    .filter(courseAssignments ->
                            courseAssignments.stream()
                                    .allMatch(a -> a.submitted))
                    .count();

    /*
     * Course chưa hoàn tất nếu còn ít nhất
     * một responsibility chưa nộp.
     */
    long notSubmittedCourses =
            assignedCourses
            - submittedCourses;

    /*
     * Course quá hạn nếu còn ít nhất
     * một responsibility overdue.
     */
    long overdueCourses =
            byCourse.values()
                    .stream()
                    .filter(courseAssignments ->
                            courseAssignments.stream()
                                    .anyMatch(a -> a.overdue))
                    .count();

    /*
     * Section cũng nên đếm DISTINCT,
     * tránh khả năng bị lặp khi aggregation.
     */
    long assignedSections =
            assignments.stream()
                    .flatMap(a ->
                            a.sectionIds.stream())
                    .distinct()
                    .count();

    double submissionRate =
            assignedCourses == 0
                    ? 0.0
                    : Math.round(
                            submittedCourses
                            * 10000.0
                            / assignedCourses
                    ) / 100.0;

    row.setAssignedCourses(
            assignedCourses);

    row.setAssignedSections(
            assignedSections);

    row.setSubmittedCourses(
            submittedCourses);

    row.setNotSubmittedCourses(
            notSubmittedCourses);

    row.setOverdueCourses(
            overdueCourses);

    row.setSubmissionRate(
            submissionRate);

    return row;
}
    private record TermRow(String academicYear, Integer semester, long sectionCount) {}
    private record AssignmentKey(Integer instructorId, Integer courseId, String academicYear, Integer semester) {}
    private record SyllabusKey(
        Integer instructorId,
        Integer courseId,
        String academicYear,
        Integer semester
) {}
    private record GroupKey(Integer id, String code, String name, String nameVn) {}

    static final class AssignmentAccumulator {
        private final Instructor instructor;
        private final Course course;
        private final Set<Integer> sectionIds = new LinkedHashSet<>();
        private Syllabus latestSyllabus;
        private boolean submitted;
        private boolean overdue;

        AssignmentAccumulator(ClassSection section) {
            this.instructor = section.getInstructor();
            this.course = section.getCourse();
        }

        void addSection(ClassSection section) {
            if (section.getId() != null) {
                sectionIds.add(section.getId());
            }
        }
    }

    static record DashboardAggregation(
        int assignmentCount,
        List<DashboardAdminResponse.FacultyNotSubmitted> faculties,
        List<DashboardAdminResponse.CourseGroupStatistic> groups,
        long submittedCount,
        long missingCount,
        long overdueCount,
        long approvedCount,
        double approvalRate,
        Map<String, Long> statusOverview) {}

            private static String normalizeAcademicYear(
        String value) {

    if (value == null) {
        return null;
    }

    return value.trim().toUpperCase();
}

private static Integer parseSemesterValue(
        String value) {

    if (value == null || value.isBlank()) {
        return null;
    }

    String digits =
            value.replaceAll("\\D+", "");

    if (digits.isBlank()) {
        return null;
    }

    try {
        return Integer.valueOf(digits);
    } catch (NumberFormatException ex) {
        return null;
    }
}
}
