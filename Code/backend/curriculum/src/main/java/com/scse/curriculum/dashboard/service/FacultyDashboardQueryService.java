package com.scse.curriculum.dashboard.service;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scse.curriculum.auth.security.CurrentUserService;
import com.scse.curriculum.classsection.entity.ClassSection;
import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.common.exception.ForbiddenOperationException;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.dashboard.dto.DashboardFacultyResponse;
import com.scse.curriculum.deadline.entity.SyllabusDeadline;
import com.scse.curriculum.deadline.repository.SyllabusDeadlineRepository;
import com.scse.curriculum.deadline.service.DeadlineReminderProperties;
import com.scse.curriculum.department.entity.Department;
import com.scse.curriculum.instructor.entity.Instructor;
import com.scse.curriculum.instructor.repository.InstructorRepository;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.user.repository.UserAccountRepository;

import lombok.RequiredArgsConstructor;

/**
 * Read model chuyên biệt cho FR-06.3.
 *
 * Chỉ thực hiện một query ClassSection và một query deadline, sau đó group trong
 * bộ nhớ. Cách này tránh N+1 và giữ thời gian tải dashboard ổn định khi số lớp
 * học phần tăng lên.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacultyDashboardQueryService {

    private static final int DUE_SOON_DAYS = 7;

    private static final Set<SyllabusStatus> IN_PROGRESS_STATES = Set.of(
            SyllabusStatus.SUBMITTED,
            SyllabusStatus.UNDER_REVIEW);

    private static final Set<SyllabusStatus> ACTION_REQUIRED_STATES = Set.of(
            SyllabusStatus.DRAFT,
            SyllabusStatus.REVISION_REQUESTED,
            SyllabusStatus.REJECTED,
            SyllabusStatus.ARCHIVED);

    private final CurrentUserService currentUserService;
    private final UserAccountRepository userAccountRepository;
    private final InstructorRepository instructorRepository;
    private final ClassSectionRepository classSectionRepository;
    private final SyllabusDeadlineRepository syllabusDeadlineRepository;
    private final DeadlineReminderProperties deadlineReminderProperties;
    private final FacultyDashboardTimeProvider timeProvider;

    public DashboardFacultyResponse getMyDashboard() {
        UserAccount current = currentUserService.getCurrentUser();
        if (current.getRole() != UserRole.INSTRUCTOR) {
            throw new ForbiddenOperationException(
                    "Chỉ giảng viên được xem dashboard giảng viên của chính mình.");
        }
        return buildDashboard(current);
    }

    public DashboardFacultyResponse getForRequestedUser(long requestedUserId) {
        UserAccount current = currentUserService.getCurrentUser();
        int requestedId = Math.toIntExact(requestedUserId);

        if (current.getRole() == UserRole.ADMIN) {
            UserAccount requested = userAccountRepository.findById(requestedId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User not found"));
            return buildDashboard(requested);
        }

        if (current.getRole() != UserRole.INSTRUCTOR
                || !Objects.equals(current.getId(), requestedId)) {
            throw new ForbiddenOperationException(
                    "Bạn chỉ được xem dashboard giảng viên của chính mình.");
        }

        return buildDashboard(current);
    }

    private DashboardFacultyResponse buildDashboard(UserAccount facultyUser) {
        if (facultyUser.getInstructorId() == null) {
            throw new ForbiddenOperationException(
                    "Tài khoản giảng viên chưa liên kết hồ sơ Instructor.");
        }

        Instructor instructor = instructorRepository
                .findById(facultyUser.getInstructorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hồ sơ giảng viên đã liên kết."));

        List<ClassSection> assignments = classSectionRepository
                .findActiveByInstructorId(instructor.getId());

        Map<TermKey, SyllabusDeadline> deadlinesByTerm =
                syllabusDeadlineRepository
                        .findByActiveTrueOrderByDeadlineAtAsc()
                        .stream()
                        .collect(Collectors.toMap(
                                deadline -> new TermKey(
                                        normalizeAcademicYear(
                                                deadline.getAcademicYear()),
                                        deadline.getSemester()),
                                Function.identity(),
                                this::preferLatestDeadlineConfiguration,
                                LinkedHashMap::new));

        ZoneId zoneId = deadlineReminderProperties.zoneId();
        ZonedDateTime now = timeProvider.now(zoneId);

        Map<AssignmentKey, List<ClassSection>> groupedAssignments = assignments
                .stream()
                .filter(section -> section.getCourse() != null)
                .collect(Collectors.groupingBy(
                        section -> new AssignmentKey(
                                section.getCourse().getId(),
                                normalizeAcademicYear(
                                        section.getAcademicYear()),
                                section.getSemester()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<DashboardFacultyResponse.CourseAssignment> items =
                groupedAssignments.entrySet().stream()
                        .map(entry -> mapAssignment(
                                entry.getKey(),
                                entry.getValue(),
                                deadlinesByTerm,
                                now,
                                zoneId))
                        .sorted(assignmentComparator())
                        .toList();

        DashboardFacultyResponse response = new DashboardFacultyResponse();
        response.setFacultyUserId(facultyUser.getId());
        response.setInstructorId(instructor.getId());
        response.setInstructorName(instructor.getFullName());
        response.setStaffCode(instructor.getStaffCode());
        Department department = instructor.getDepartment();
        response.setDepartmentCode(
                department == null ? null : department.getCode());
        response.setDepartmentName(
                department == null ? null : department.getName());
        response.setTimeZone(zoneId.getId());
        response.setGeneratedAt(now.toOffsetDateTime());

        response.setAssignedCourses(items.size());
        response.setAssignedSections(assignments.size());
        response.setCompletedSyllabuses(countStatus(
                items, SyllabusStatus.APPROVED.name()));
        response.setInProgressSyllabuses(items.stream()
                .filter(item -> IN_PROGRESS_STATES.stream()
                        .map(Enum::name)
                        .anyMatch(status -> status.equals(item.getStatus())))
                .count());
        response.setPendingSyllabuses(
                items.size() - response.getCompletedSyllabuses());
        response.setActionRequiredCourses(items.stream()
                .filter(DashboardFacultyResponse.CourseAssignment::isActionRequired)
                .count());
        response.setOverdueCourses(items.stream()
                .filter(item -> "OVERDUE".equals(item.getDeadlineState()))
                .count());
        response.setDueSoonCourses(items.stream()
                .filter(item -> "DUE_SOON".equals(item.getDeadlineState())
                        || "DUE_TODAY".equals(item.getDeadlineState()))
                .count());
        response.setUnconfiguredDeadlineCourses(items.stream()
                .filter(item -> "NOT_CONFIGURED".equals(
                        item.getDeadlineState()))
                .count());

        List<DashboardFacultyResponse.TermSummary> terms =
                buildTermSummaries(items);
        response.setTerms(terms);
        response.setDefaultTermKey(resolveDefaultTermKey(items));
        response.setUpcomingDeadlines(items);
        return response;
    }

    private DashboardFacultyResponse.CourseAssignment mapAssignment(
            AssignmentKey key,
            List<ClassSection> sections,
            Map<TermKey, SyllabusDeadline> deadlinesByTerm,
            ZonedDateTime now,
            ZoneId zoneId) {

        List<ClassSection> orderedSections = sections.stream()
                .sorted(Comparator
                        .comparing(ClassSection::getGroupNumber,
                                Comparator.nullsLast(
                                        Comparator.naturalOrder()))
                        .thenComparing(ClassSection::getId,
                                Comparator.nullsLast(
                                        Comparator.naturalOrder())))
                .toList();

        Course course = orderedSections.getFirst().getCourse();
        List<Syllabus> linkedSyllabuses = orderedSections.stream()
                .map(ClassSection::getSyllabus)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        Syllabus::getId,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new))
                .values()
                .stream()
                .toList();

        Syllabus syllabus = linkedSyllabuses.stream()
                .max(Comparator
                        .comparingInt(this::safeVersionNumber)
                        .thenComparingInt(this::safeSyllabusId))
                .orElse(null);

        SyllabusDeadline deadline = deadlinesByTerm.get(
                new TermKey(key.academicYear(), key.semester()));

        DashboardFacultyResponse.CourseAssignment item =
                new DashboardFacultyResponse.CourseAssignment();
        item.setCourseId(course.getId());
        item.setCourseCode(course.getCourseCode());
        item.setCourseName(course.getName());
        item.setCourseNameVn(course.getNameVn());
        item.setTotalCredits(safeCredits(course));
        Department department = course.getDepartment();
        item.setDepartmentCode(
                department == null ? null : department.getCode());
        item.setDepartmentName(
                department == null ? null : department.getName());

        item.setTermKey(termKey(
                orderedSections.getFirst().getAcademicYear(),
                key.semester()));
        item.setAcademicYear(
                orderedSections.getFirst().getAcademicYear());
        item.setSemester(key.semester());
        item.setPrimaryClassSectionId(orderedSections.getFirst().getId());
        item.setClassSectionIds(orderedSections.stream()
                .map(ClassSection::getId)
                .filter(Objects::nonNull)
                .toList());
        item.setSectionCount(orderedSections.size());
        item.setGroupNumbers(distinctSorted(
                orderedSections.stream()
                        .map(ClassSection::getGroupNumber)
                        .filter(Objects::nonNull)
                        .toList()));
        item.setSectionTypes(distinctSortedStrings(
                orderedSections.stream()
                        .map(ClassSection::getSectionType)
                        .filter(Objects::nonNull)
                        .map(Enum::name)
                        .toList()));
        item.setRooms(distinctSortedStrings(
                orderedSections.stream()
                        .map(ClassSection::getRoom)
                        .filter(this::hasText)
                        .toList()));
        item.setSchedules(distinctSortedStrings(
                orderedSections.stream()
                        .map(ClassSection::getSchedule)
                        .filter(this::hasText)
                        .toList()));

        String status = syllabus == null || syllabus.getStatus() == null
                ? "NOT_CREATED"
                : syllabus.getStatus().name();
        item.setSyllabusId(syllabus == null ? null : syllabus.getId());
        item.setSyllabusVersionNumber(
                syllabus == null ? null : syllabus.getVersionNumber());
        item.setSyllabusVersionLabel(
                syllabus == null ? null : syllabus.getVersionLabel());
        item.setStatus(status);
        item.setCurrentVersion(
                syllabus == null ? null : syllabus.getIsCurrent());

        boolean actionRequired = syllabus == null
                || syllabus.getStatus() == null
                || ACTION_REQUIRED_STATES.contains(syllabus.getStatus());
        item.setActionRequired(actionRequired);
        item.setRecommendedAction(recommendedAction(syllabus));
        item.setDataQualityState(resolveDataQualityState(
                orderedSections, linkedSyllabuses));

        applyDeadline(item, deadline, syllabus, actionRequired, now, zoneId);
        return item;
    }

    private void applyDeadline(
            DashboardFacultyResponse.CourseAssignment item,
            SyllabusDeadline deadline,
            Syllabus syllabus,
            boolean actionRequired,
            ZonedDateTime now,
            ZoneId zoneId) {

        if (deadline == null) {
            item.setDeadlineId(null);
            item.setDeadlineRevision(null);
            item.setDeadline(null);
            item.setDaysRemaining(null);
            item.setMinutesRemaining(null);
            item.setDeadlineState("NOT_CONFIGURED");
            return;
        }

        ZonedDateTime deadlineAt = deadline.getDeadlineAt().atZone(zoneId);
        long daysRemaining = ChronoUnit.DAYS.between(
                now.toLocalDate(),
                deadlineAt.toLocalDate());
        long minutesRemaining = Duration.between(now, deadlineAt).toMinutes();

        item.setDeadlineId(deadline.getId());
        item.setDeadlineRevision(deadline.getRevision());
        item.setDeadline(deadlineAt.toOffsetDateTime());
        item.setDaysRemaining(daysRemaining);
        item.setMinutesRemaining(minutesRemaining);
        item.setDeadlineState(resolveDeadlineState(
                syllabus,
                actionRequired,
                now,
                deadlineAt,
                daysRemaining));
    }

    private String resolveDeadlineState(
            Syllabus syllabus,
            boolean actionRequired,
            ZonedDateTime now,
            ZonedDateTime deadlineAt,
            long daysRemaining) {
        if (syllabus != null
                && syllabus.getStatus() == SyllabusStatus.APPROVED) {
            return "COMPLETED";
        }
        if (syllabus != null
                && syllabus.getStatus() == SyllabusStatus.UNDER_REVIEW) {
            return "IN_REVIEW";
        }
        if (syllabus != null
                && syllabus.getStatus() == SyllabusStatus.SUBMITTED) {
            return "SUBMITTED";
        }
        if (!actionRequired) {
            return "SUBMITTED";
        }
        if (now.isAfter(deadlineAt)) {
            return "OVERDUE";
        }
        if (now.toLocalDate().equals(deadlineAt.toLocalDate())) {
            return "DUE_TODAY";
        }
        if (daysRemaining <= DUE_SOON_DAYS) {
            return "DUE_SOON";
        }
        return "UPCOMING";
    }

    private List<DashboardFacultyResponse.TermSummary> buildTermSummaries(
            List<DashboardFacultyResponse.CourseAssignment> items) {
        Map<String, List<DashboardFacultyResponse.CourseAssignment>> groups =
                items.stream().collect(Collectors.groupingBy(
                        DashboardFacultyResponse.CourseAssignment::getTermKey,
                        LinkedHashMap::new,
                        Collectors.toList()));

        return groups.entrySet().stream()
                .map(entry -> {
                    List<DashboardFacultyResponse.CourseAssignment> termItems =
                            entry.getValue();
                    DashboardFacultyResponse.CourseAssignment first =
                            termItems.getFirst();
                    DashboardFacultyResponse.TermSummary summary =
                            new DashboardFacultyResponse.TermSummary();
                    summary.setKey(entry.getKey());
                    summary.setAcademicYear(first.getAcademicYear());
                    summary.setSemester(first.getSemester());
                    summary.setCourseCount(termItems.size());
                    summary.setSectionCount(termItems.stream()
                            .mapToLong(item -> item.getSectionCount() == null
                                    ? 0
                                    : item.getSectionCount())
                            .sum());
                    summary.setDeadlineId(first.getDeadlineId());
                    summary.setDeadlineRevision(first.getDeadlineRevision());
                    summary.setDeadline(first.getDeadline());
                    summary.setDeadlineConfigured(first.getDeadline() != null);
                    summary.setActionRequiredCount(termItems.stream()
                            .filter(DashboardFacultyResponse.CourseAssignment::isActionRequired)
                            .count());
                    summary.setOverdueCount(termItems.stream()
                            .filter(item -> "OVERDUE".equals(
                                    item.getDeadlineState()))
                            .count());
                    summary.setDueSoonCount(termItems.stream()
                            .filter(item -> "DUE_SOON".equals(
                                    item.getDeadlineState())
                                    || "DUE_TODAY".equals(
                                            item.getDeadlineState()))
                            .count());
                    return summary;
                })
                .sorted(Comparator
                        .comparing(
                                DashboardFacultyResponse.TermSummary::getDeadline,
                                Comparator.nullsLast(
                                        Comparator.naturalOrder()))
                        .thenComparing(
                                DashboardFacultyResponse.TermSummary::getAcademicYear,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()))
                        .thenComparing(
                                DashboardFacultyResponse.TermSummary::getSemester,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder())))
                .toList();
    }

    private String resolveDefaultTermKey(
            List<DashboardFacultyResponse.CourseAssignment> items) {
        return items.isEmpty() ? null : items.getFirst().getTermKey();
    }

    private Comparator<DashboardFacultyResponse.CourseAssignment>
    assignmentComparator() {
        return Comparator
                .comparingInt(this::urgencyRank)
                .thenComparing(
                        DashboardFacultyResponse.CourseAssignment::getDeadline,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(
                        DashboardFacultyResponse.CourseAssignment::getAcademicYear,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(
                        DashboardFacultyResponse.CourseAssignment::getSemester,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(
                        DashboardFacultyResponse.CourseAssignment::getCourseCode,
                        Comparator.nullsLast(
                                String.CASE_INSENSITIVE_ORDER));
    }

    private int urgencyRank(
            DashboardFacultyResponse.CourseAssignment item) {
        return switch (item.getDeadlineState()) {
            case "OVERDUE" -> 0;
            case "DUE_TODAY" -> 1;
            case "DUE_SOON" -> 2;
            case "UPCOMING" -> item.isActionRequired() ? 3 : 6;
            case "NOT_CONFIGURED" -> item.isActionRequired() ? 4 : 7;
            case "SUBMITTED", "IN_REVIEW" -> 5;
            case "COMPLETED" -> 8;
            default -> 9;
        };
    }

    private long countStatus(
            List<DashboardFacultyResponse.CourseAssignment> items,
            String status) {
        return items.stream()
                .filter(item -> status.equals(item.getStatus()))
                .count();
    }

    private String recommendedAction(Syllabus syllabus) {
        if (syllabus == null || syllabus.getStatus() == null) {
            return "CREATE";
        }
        return switch (syllabus.getStatus()) {
            case DRAFT -> "EDIT";
            case REVISION_REQUESTED, REJECTED -> "REVISE";
            case SUBMITTED, UNDER_REVIEW -> "VIEW_PROGRESS";
            case APPROVED -> "VIEW_APPROVED";
            case ARCHIVED -> "VIEW_HISTORY";
        };
    }

    private String resolveDataQualityState(
            List<ClassSection> sections,
            List<Syllabus> linkedSyllabuses) {
        long linkedCount = sections.stream()
                .filter(section -> section.getSyllabus() != null)
                .count();
        if (linkedCount == 0) {
            return "UNLINKED";
        }
        if (linkedSyllabuses.size() > 1) {
            return "MULTIPLE_LINKS";
        }
        if (linkedCount < sections.size()) {
            return "PARTIAL_LINKAGE";
        }
        return "CONSISTENT";
    }

    private SyllabusDeadline preferLatestDeadlineConfiguration(
            SyllabusDeadline first,
            SyllabusDeadline second) {
        int firstRevision = first.getRevision() == null
                ? 0
                : first.getRevision();
        int secondRevision = second.getRevision() == null
                ? 0
                : second.getRevision();
        if (firstRevision != secondRevision) {
            return firstRevision > secondRevision ? first : second;
        }
        long firstId = first.getId() == null ? 0 : first.getId();
        long secondId = second.getId() == null ? 0 : second.getId();
        return firstId >= secondId ? first : second;
    }

    private int safeVersionNumber(Syllabus syllabus) {
        return syllabus.getVersionNumber() == null
                ? 0
                : syllabus.getVersionNumber();
    }

    private int safeSyllabusId(Syllabus syllabus) {
        return syllabus.getId() == null ? 0 : syllabus.getId();
    }

    private int safeCredits(Course course) {
        int theory = course.getCreditTheory() == null
                ? 0
                : course.getCreditTheory();
        int lab = course.getCreditLab() == null
                ? 0
                : course.getCreditLab();
        return theory + lab;
    }

    private List<Integer> distinctSorted(List<Integer> values) {
        return values.stream().distinct().sorted().toList();
    }

    private List<String> distinctSortedStrings(List<String> values) {
        return values.stream()
                .map(String::trim)
                .filter(this::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalizeAcademicYear(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String termKey(String academicYear, Integer semester) {
        String displayYear = academicYear == null
                ? ""
                : academicYear.trim();
        return displayYear + "::" + (semester == null ? "" : semester);
    }

    private record AssignmentKey(
            Integer courseId,
            String academicYear,
            Integer semester) {
    }

    private record TermKey(
            String academicYear,
            Integer semester) {
    }
}
