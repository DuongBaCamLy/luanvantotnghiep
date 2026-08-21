package com.scse.curriculum.dashboard.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scse.curriculum.auth.security.CurrentUserService;
import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.common.exception.ForbiddenOperationException;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.dashboard.dto.DashboardDeanResponse;
import com.scse.curriculum.deadline.service.DeadlineReminderProperties;
import com.scse.curriculum.major.entity.Major;
import com.scse.curriculum.major.repository.MajorRepository;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;


/**
 * Read model chuyên biệt cho FR-06.1.
 *
 * Nguồn chuẩn của số môn cần có Syllabus là CourseProgram hiệu lực của Cohort,
 * không phải bảng Syllabus. Mỗi course chỉ được tính đúng một lần; bản ghi
 * cohort-specific ghi đè cấu hình dùng chung của Program. Sau đó service mới
 * chọn một phiên bản Syllabus đại diện cho course để tính tỷ lệ phê duyệt.
 */
@Service
@Transactional(readOnly = true)
public class DeanDashboardQueryService {

    private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");
    private static final int MIN_SEMESTER = 1;
    private static final int MAX_SEMESTER = 8;

    private final CurrentUserService currentUserService;
    private final MajorRepository majorRepository;
    private final CohortRepository cohortRepository;
    private final CourseProgramRepository courseProgramRepository;
    private final SyllabusRepository syllabusRepository;
    private final DeadlineReminderProperties deadlineReminderProperties;
    private final DeanDashboardTimeProvider timeProvider;

    public DeanDashboardQueryService(
            CurrentUserService currentUserService,
            MajorRepository majorRepository,
            CohortRepository cohortRepository,
            CourseProgramRepository courseProgramRepository,
            SyllabusRepository syllabusRepository,
            DeadlineReminderProperties deadlineReminderProperties,
            DeanDashboardTimeProvider timeProvider) {
        this.currentUserService = currentUserService;
        this.majorRepository = majorRepository;
        this.cohortRepository = cohortRepository;
        this.courseProgramRepository = courseProgramRepository;
        this.syllabusRepository = syllabusRepository;
        this.deadlineReminderProperties = deadlineReminderProperties;
        this.timeProvider = timeProvider;
    }

    public DashboardDeanResponse getDashboard(
            Integer requestedMajorId,
            Integer requestedCohortId,
            Integer requestedSemester) {

        UserAccount current = currentUserService.getCurrentUser();
        if (current.getRole() != UserRole.DEAN && current.getRole() != UserRole.ADMIN) {
            throw new ForbiddenOperationException(
                    "Chỉ Trưởng khoa được xem Dean Dashboard.");
        }

        validateSemester(requestedSemester);

        List<Major> majors = majorRepository.findAllByOrderByCodeAsc();
        List<Cohort> activeCohorts = cohortRepository
                .findActiveForDeanDashboard();

        Selection selection = resolveSelection(
                requestedMajorId,
                requestedCohortId,
                requestedSemester,
                majors,
                activeCohorts);

        DashboardDeanResponse response = new DashboardDeanResponse();
        ZoneId zoneId = deadlineReminderProperties.zoneId();
        response.setGeneratedAt(
                timeProvider.now(zoneId).toOffsetDateTime());
        response.setTimeZone(zoneId.getId());
        response.setDataSource(
                "CourseProgram hiệu lực theo Cohort + một Syllabus đại diện cho mỗi môn");
        response.setMajors(mapMajorOptions(majors, activeCohorts));
        response.setCohorts(mapCohortOptions(
                activeCohorts,
                selection.major() == null
                        ? null
                        : selection.major().getId()));
        response.setScope(mapScope(selection));

        if (selection.cohort() == null) {
            response.setSummary(emptySummary());
            response.setWarnings(List.of(warning(
                    "WARNING",
                    "NO_ACTIVE_COHORT",
                    "Ngành được chọn chưa có Cohort đang hoạt động.",
                    List.of())));
            return response;
        }

        List<DashboardDeanResponse.DataWarning> warnings = new ArrayList<>();
        List<CourseProgram> rawMappings = courseProgramRepository
                .findEffectiveByProgramIdAndCohortIdWithRelations(
                        selection.program().getId(),
                        selection.cohort().getId());

        List<CourseProgram> effectiveMappings = resolveEffectiveMappings(
                rawMappings,
                selection.cohort().getId(),
                warnings);

        Set<Integer> courseIds = effectiveMappings.stream()
                .map(CourseProgram::getCourse)
                .filter(Objects::nonNull)
                .map(Course::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Integer, List<Syllabus>> candidatesByCourse = courseIds.isEmpty()
                ? Map.of()
                : syllabusRepository
                        .findDeanDashboardCandidates(
                                new ArrayList<>(courseIds))
                        .stream()
                        .collect(Collectors.groupingBy(
                                syllabus -> syllabus.getCourse().getId(),
                                LinkedHashMap::new,
                                Collectors.toList()));

        List<CourseSnapshot> allSnapshots = effectiveMappings.stream()
                .map(mapping -> buildSnapshot(
                        mapping,
                        selection,
                        candidatesByCourse.getOrDefault(
                                mapping.getCourse().getId(),
                                List.of()),
                        zoneId,
                        warnings))
                .sorted(snapshotComparator())
                .toList();

        response.setSemesters(buildSemesterOptions(allSnapshots));
        List<CourseSnapshot> scopedSnapshots = requestedSemester == null
                ? allSnapshots
                : allSnapshots.stream()
                        .filter(snapshot -> Objects.equals(
                                snapshot.semester(),
                                requestedSemester))
                        .toList();

        response.setSummary(buildSummary(scopedSnapshots));
        response.setStatusDistribution(buildStatusDistribution(
                scopedSnapshots));
        response.setSemesterProgress(buildSemesterProgress(allSnapshots));
        response.setCourses(scopedSnapshots.stream()
                .map(CourseSnapshot::dto)
                .toList());

        if (effectiveMappings.isEmpty()) {
            warnings.add(warning(
                    "WARNING",
                    "NO_CURRICULUM_COURSES",
                    "Cohort được chọn chưa có môn trong CourseProgram.",
                    List.of(selection.cohort().getName())));
        }
        if (allSnapshots.stream().anyMatch(
                snapshot -> snapshot.semester() == null)) {
            List<String> refs = allSnapshots.stream()
                    .filter(snapshot -> snapshot.semester() == null)
                    .map(snapshot -> snapshot.dto().getCourseCode())
                    .toList();
            warnings.add(warning(
                    "WARNING",
                    "COURSE_WITHOUT_SEMESTER",
                    "Một số môn chưa được xếp học kỳ trong CTĐT.",
                    refs));
        }
        response.setWarnings(deduplicateWarnings(warnings));
        return response;
    }

    private Selection resolveSelection(
            Integer requestedMajorId,
            Integer requestedCohortId,
            Integer requestedSemester,
            List<Major> majors,
            List<Cohort> activeCohorts) {

        Cohort requestedCohort = null;
        if (requestedCohortId != null) {
            requestedCohort = activeCohorts.stream()
                    .filter(cohort -> Objects.equals(
                            cohort.getId(),
                            requestedCohortId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy Cohort đang hoạt động."));
        }

        Major selectedMajor;
        if (requestedCohort != null) {
            selectedMajor = requestedCohort.getProgram().getMajor();
            if (requestedMajorId != null
                    && !Objects.equals(
                            selectedMajor.getId(),
                            requestedMajorId)) {
                throw new IllegalArgumentException(
                        "Cohort không thuộc ngành đã chọn.");
            }
        } else if (requestedMajorId != null) {
            selectedMajor = majors.stream()
                    .filter(major -> Objects.equals(
                            major.getId(),
                            requestedMajorId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy ngành được chọn."));
        } else {
            selectedMajor = activeCohorts.stream()
                    .max(defaultCohortComparator())
                    .map(cohort -> cohort.getProgram().getMajor())
                    .orElseGet(() -> majors.isEmpty()
                            ? null
                            : majors.getFirst());
        }

        Cohort selectedCohort = requestedCohort;
        if (selectedCohort == null && selectedMajor != null) {
            selectedCohort = activeCohorts.stream()
                    .filter(cohort -> cohort.getProgram().getMajor() != null)
                    .filter(cohort -> Objects.equals(
                            cohort.getProgram().getMajor().getId(),
                            selectedMajor.getId()))
                    .max(defaultCohortComparator())
                    .orElse(null);
        }

        Program selectedProgram = selectedCohort == null
                ? null
                : selectedCohort.getProgram();
        return new Selection(
                selectedMajor,
                selectedProgram,
                selectedCohort,
                requestedSemester);
    }

    private Comparator<Cohort> defaultCohortComparator() {
        return Comparator
                .comparing(Cohort::getEntryYear,
                        Comparator.nullsFirst(
                                Comparator.naturalOrder()))
                .thenComparing(Cohort::getId,
                        Comparator.nullsFirst(
                                Comparator.naturalOrder()));
    }

    private List<CourseProgram> resolveEffectiveMappings(
            List<CourseProgram> rawMappings,
            Integer cohortId,
            List<DashboardDeanResponse.DataWarning> warnings) {

        Map<Integer, CourseProgram> selected = new LinkedHashMap<>();
        Map<Integer, List<CourseProgram>> duplicates = new HashMap<>();

        for (CourseProgram candidate : rawMappings) {
            if (candidate.getCourse() == null
                    || candidate.getCourse().getId() == null) {
                continue;
            }
            Integer courseId = candidate.getCourse().getId();
            duplicates.computeIfAbsent(courseId, ignored -> new ArrayList<>())
                    .add(candidate);

            CourseProgram existing = selected.get(courseId);
            if (existing == null
                    || compareMappingPriority(candidate, existing, cohortId) > 0) {
                selected.put(courseId, candidate);
            }
        }

        duplicates.values().stream()
                .filter(items -> items.size() > 2
                        || items.stream()
                                .filter(item -> item.getCohort() != null)
                                .count() > 1)
                .forEach(items -> warnings.add(warning(
                        "WARNING",
                        "DUPLICATE_CURRICULUM_MAPPING",
                        "Phát hiện nhiều cấu hình CourseProgram cho cùng một môn; hệ thống đã ưu tiên cấu hình Cohort cụ thể mới nhất.",
                        List.of(items.getFirst().getCourse().getCourseCode()))));

        return selected.values().stream()
                .sorted(Comparator
                        .comparing(CourseProgram::getSemesterSuggest,
                                Comparator.nullsLast(
                                        Comparator.naturalOrder()))
                        .thenComparing(item -> item.getCourse().getCourseCode()))
                .toList();
    }

    private int compareMappingPriority(
            CourseProgram left,
            CourseProgram right,
            Integer cohortId) {
        int leftSpecific = left.getCohort() != null
                && Objects.equals(left.getCohort().getId(), cohortId) ? 1 : 0;
        int rightSpecific = right.getCohort() != null
                && Objects.equals(right.getCohort().getId(), cohortId) ? 1 : 0;
        if (leftSpecific != rightSpecific) {
            return Integer.compare(leftSpecific, rightSpecific);
        }
        return Comparator.nullsFirst(Integer::compareTo)
                .compare(left.getId(), right.getId());
    }

    private CourseSnapshot buildSnapshot(
            CourseProgram mapping,
            Selection selection,
            List<Syllabus> candidates,
            ZoneId zoneId,
            List<DashboardDeanResponse.DataWarning> warnings) {

        SyllabusResolution resolution = resolveSyllabus(
                mapping,
                selection,
                candidates);
        Syllabus syllabus = resolution.syllabus();
        Course course = mapping.getCourse();

        DashboardDeanResponse.CourseProgress dto =
                new DashboardDeanResponse.CourseProgress();
        dto.setCourseProgramId(mapping.getId());
        dto.setCourseId(course.getId());
        dto.setCourseCode(course.getCourseCode());
        dto.setCourseName(course.getName());
        dto.setCourseNameVn(course.getNameVn());
        dto.setSemester(mapping.getSemesterSuggest());
        dto.setSemesterLabel(semesterLabel(mapping.getSemesterSuggest()));
        dto.setYearSuggest(mapping.getYearSuggest());
        dto.setCourseType(mapping.getCourseType() == null
                ? null
                : mapping.getCourseType().getName());
        dto.setRequired(Boolean.TRUE.equals(mapping.getRequired()));
        int credits = safeInteger(course.getCreditTheory())
                + safeInteger(course.getCreditLab());
        dto.setCredits(credits);
        dto.setExplicitCurriculumLink(resolution.explicitLink());
        dto.setDataQualityState(resolution.dataQualityState());

        if (syllabus == null) {
            dto.setStatus("MISSING");
            dto.setStatusLabel("Chưa có đề cương");
        } else {
            dto.setSyllabusId(syllabus.getId());
            dto.setVersionNumber(syllabus.getVersionNumber());
            dto.setVersionLabel(syllabus.getVersionLabel());
            String status = syllabus.getStatus() == null
                    ? "DRAFT"
                    : syllabus.getStatus().name();
            dto.setStatus(status);
            dto.setStatusLabel(statusLabel(status));
            dto.setPreparedBy(syllabus.getCreatedBy() == null
                    ? null
                    : syllabus.getCreatedBy().getUsername());
            dto.setSubmittedAt(toOffsetDateTime(
                    syllabus.getSubmittedAt(), zoneId));
            dto.setApprovedAt(toOffsetDateTime(
                    syllabus.getApprovedAt(), zoneId));
            LocalDateTime updated = syllabus.getUpdatedAt() == null
                    ? syllabus.getCreatedAt()
                    : syllabus.getUpdatedAt();
            dto.setLastUpdatedAt(toOffsetDateTime(updated, zoneId));
        }

        if ("MULTIPLE_CURRENT".equals(resolution.dataQualityState())) {
            warnings.add(warning(
                    "WARNING",
                    "MULTIPLE_CURRENT_SYLLABUS",
                    "Môn có nhiều phiên bản được đánh dấu hiện hành; Dashboard đã chọn phiên bản mới nhất.",
                    List.of(course.getCourseCode())));
        }
        if ("RESOLVED_FALLBACK".equals(resolution.dataQualityState())) {
            warnings.add(warning(
                    "INFO",
                    "SYLLABUS_FALLBACK",
                    "Môn chưa liên kết Syllabus trực tiếp trong CourseProgram; Dashboard dùng phiên bản phù hợp mới nhất theo môn.",
                    List.of(course.getCourseCode())));
        }
        if ("INVALID_EXPLICIT_LINK".equals(
        resolution.dataQualityState())) {
    warnings.add(warning(
            "ERROR",
            "INVALID_EXPLICIT_SYLLABUS_LINK",
            "CourseProgram đang liên kết tới Syllabus của môn khác; Dashboard không sử dụng liên kết này.",
            List.of(course.getCourseCode())));
}

        return new CourseSnapshot(
                mapping.getSemesterSuggest(),
                credits,
                dto);
    }

    private SyllabusResolution resolveSyllabus(
        CourseProgram mapping,
        Selection selection,
        List<Syllabus> candidates) {

    if (mapping.getSyllabus() != null) {
        Syllabus explicit = mapping.getSyllabus();

        boolean sameCourse = explicit.getCourse() != null
                && explicit.getCourse().getId() != null
                && mapping.getCourse() != null
                && Objects.equals(
                        explicit.getCourse().getId(),
                        mapping.getCourse().getId());

        if (!sameCourse) {
            return new SyllabusResolution(
                    null,
                    true,
                    "INVALID_EXPLICIT_LINK");
        }

        return new SyllabusResolution(
                explicit,
                true,
                "EXPLICIT_LINK");
    }

    if (candidates.isEmpty()) {
        return new SyllabusResolution(
                null,
                false,
                "MISSING");
    }

    List<Syllabus> scopedCandidates = candidates.stream()
            .filter(candidate -> candidate.getCourse() != null)
            .filter(candidate -> mapping.getCourse() != null)
            .filter(candidate -> Objects.equals(
                    candidate.getCourse().getId(),
                    mapping.getCourse().getId()))
            .filter(candidate ->
                    cohortMatchScore(candidate, selection) > 0)
            .toList();

    if (scopedCandidates.isEmpty()) {
        return new SyllabusResolution(
                null,
                false,
                "MISSING");
    }

    long currentCount = scopedCandidates.stream()
            .filter(candidate ->
                    Boolean.TRUE.equals(candidate.getIsCurrent()))
            .count();

    Syllabus selected = scopedCandidates.stream()
            .sorted(candidateComparator(selection, mapping))
            .findFirst()
            .orElse(null);

    String quality = currentCount > 1
            ? "MULTIPLE_CURRENT"
            : "RESOLVED_FALLBACK";

    return new SyllabusResolution(
            selected,
            false,
            quality);
}
    private Comparator<Syllabus> candidateComparator(
            Selection selection,
            CourseProgram mapping) {
        return Comparator
                .comparingInt((Syllabus syllabus) ->
                        cohortMatchScore(syllabus, selection))
                .reversed()
                .thenComparing(
                        (Syllabus syllabus) -> semesterMatches(
                                syllabus.getSemester(),
                                mapping.getSemesterSuggest()),
                        Comparator.reverseOrder())
                .thenComparing(
                        (Syllabus syllabus) -> Boolean.TRUE.equals(
                                syllabus.getIsCurrent()),
                        Comparator.reverseOrder())
                .thenComparing(
                        (Syllabus syllabus) -> syllabus.getStatus()
                                != SyllabusStatus.ARCHIVED,
                        Comparator.reverseOrder())
                .thenComparing(Syllabus::getVersionNumber,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()))
                .thenComparing(Syllabus::getUpdatedAt,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()))
                .thenComparing(Syllabus::getId,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()));
    }

   private int cohortMatchScore(
        Syllabus syllabus,
        Selection selection) {

    String academicYear = normalize(syllabus.getAcademicYear());
    if (academicYear.isBlank()) {
        return 0;
    }

    String cohortName = normalize(selection.cohort().getName());
    String programCode = normalize(selection.program().getCode());
    String entryYear = selection.cohort().getEntryYear() == null
            ? ""
            : String.valueOf(selection.cohort().getEntryYear());

    String selectedMajorCode = selection.major() == null
            ? ""
            : normalize(selection.major().getCode());

    String syllabusMajor = normalize(syllabus.getMajor());

    if (academicYear.equals(cohortName)) {
        return 4;
    }

    if (!programCode.isBlank()
            && academicYear.equals(programCode)) {
        return 3;
    }

    boolean majorMatches = !selectedMajorCode.isBlank()
            && selectedMajorCode.equals(syllabusMajor);

    if (majorMatches
            && !entryYear.isBlank()
            && academicYear.contains(entryYear)) {
        return 2;
    }

    String shortYear = entryYear.length() >= 2
            ? entryYear.substring(entryYear.length() - 2)
            : entryYear;

    if (majorMatches
            && !shortYear.isBlank()
            && academicYear.equals("k" + shortYear)) {
        return 1;
    }

    return 0;
}

    private boolean semesterMatches(
            String rawSemester,
            Integer expectedSemester) {
        if (expectedSemester == null || rawSemester == null) {
            return false;
        }
        Matcher matcher = INTEGER_PATTERN.matcher(rawSemester);
        while (matcher.find()) {
            try {
                if (Integer.parseInt(matcher.group()) == expectedSemester) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // Giá trị cực lớn không được xem là học kỳ hợp lệ.
            }
        }
        return false;
    }

    private DashboardDeanResponse.Summary buildSummary(
            List<CourseSnapshot> snapshots) {
        DashboardDeanResponse.Summary summary = emptySummary();
        summary.setExpectedCourses(snapshots.size());
        summary.setExpectedCredits(snapshots.stream()
                .mapToInt(CourseSnapshot::credits)
                .sum());

        Map<String, Long> counts = snapshots.stream()
                .collect(Collectors.groupingBy(
                        snapshot -> snapshot.dto().getStatus(),
                        LinkedHashMap::new,
                        Collectors.counting()));

        long approved = counts.getOrDefault("APPROVED", 0L);
        long missing = counts.getOrDefault("MISSING", 0L);
        long created = snapshots.size() - missing;
        summary.setCreatedSyllabuses(created);
        summary.setApprovedSyllabuses(approved);
        summary.setNotApprovedSyllabuses(snapshots.size() - approved);
        summary.setMissingSyllabuses(missing);
        summary.setDraftSyllabuses(counts.getOrDefault("DRAFT", 0L));
        summary.setSubmittedSyllabuses(
                counts.getOrDefault("SUBMITTED", 0L));
        summary.setUnderReviewSyllabuses(
                counts.getOrDefault("UNDER_REVIEW", 0L));
        summary.setRevisionRequestedSyllabuses(
                counts.getOrDefault("REVISION_REQUESTED", 0L));
        summary.setRejectedSyllabuses(
                counts.getOrDefault("REJECTED", 0L));
        summary.setArchivedSyllabuses(
                counts.getOrDefault("ARCHIVED", 0L));
        summary.setPendingReviewSyllabuses(
                summary.getSubmittedSyllabuses()
                        + summary.getUnderReviewSyllabuses());
        summary.setActionRequiredCourses(
                missing
                        + summary.getDraftSyllabuses()
                        + summary.getRevisionRequestedSyllabuses()
                        + summary.getRejectedSyllabuses()
                        + summary.getArchivedSyllabuses());
        summary.setApprovedCredits(snapshots.stream()
                .filter(snapshot -> "APPROVED".equals(
                        snapshot.dto().getStatus()))
                .mapToInt(CourseSnapshot::credits)
                .sum());
        summary.setApprovalRate(percentage(approved, snapshots.size()));
        summary.setCreationRate(percentage(created, snapshots.size()));
        return summary;
    }

    private DashboardDeanResponse.Summary emptySummary() {
        return new DashboardDeanResponse.Summary();
    }

    private List<DashboardDeanResponse.StatusSlice> buildStatusDistribution(
            List<CourseSnapshot> snapshots) {
        Map<String, Long> counts = snapshots.stream()
                .collect(Collectors.groupingBy(
                        snapshot -> snapshot.dto().getStatus(),
                        LinkedHashMap::new,
                        Collectors.counting()));
        List<String> order = List.of(
                "APPROVED",
                "UNDER_REVIEW",
                "SUBMITTED",
                "REVISION_REQUESTED",
                "DRAFT",
                "REJECTED",
                "ARCHIVED",
                "MISSING");
        return order.stream()
                .map(key -> {
                    DashboardDeanResponse.StatusSlice slice =
                            new DashboardDeanResponse.StatusSlice();
                    long count = counts.getOrDefault(key, 0L);
                    slice.setKey(key);
                    slice.setLabel(statusLabel(key));
                    slice.setCount(count);
                    slice.setPercentage(percentage(
                            count,
                            snapshots.size()));
                    return slice;
                })
                .filter(slice -> slice.getCount() > 0)
                .toList();
    }

    private List<DashboardDeanResponse.SemesterProgress> buildSemesterProgress(
            List<CourseSnapshot> snapshots) {
        Map<Integer, List<CourseSnapshot>> grouped = snapshots.stream()
                .filter(snapshot -> snapshot.semester() != null)
                .collect(Collectors.groupingBy(
                        CourseSnapshot::semester,
                        LinkedHashMap::new,
                        Collectors.toList()));
        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    long approved = entry.getValue().stream()
                            .filter(snapshot -> "APPROVED".equals(
                                    snapshot.dto().getStatus()))
                            .count();
                    long missing = entry.getValue().stream()
                            .filter(snapshot -> "MISSING".equals(
                                    snapshot.dto().getStatus()))
                            .count();
                    DashboardDeanResponse.SemesterProgress progress =
                            new DashboardDeanResponse.SemesterProgress();
                    progress.setSemester(entry.getKey());
                    progress.setLabel(semesterLabel(entry.getKey()));
                    progress.setExpected(entry.getValue().size());
                    progress.setApproved(approved);
                    progress.setNotApproved(
                            entry.getValue().size() - approved);
                    progress.setMissing(missing);
                    progress.setApprovalRate(percentage(
                            approved,
                            entry.getValue().size()));
                    return progress;
                })
                .toList();
    }

    private List<DashboardDeanResponse.SemesterOption> buildSemesterOptions(
            List<CourseSnapshot> snapshots) {
        Map<Integer, Long> counts = snapshots.stream()
                .filter(snapshot -> snapshot.semester() != null)
                .collect(Collectors.groupingBy(
                        CourseSnapshot::semester,
                        LinkedHashMap::new,
                        Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    DashboardDeanResponse.SemesterOption option =
                            new DashboardDeanResponse.SemesterOption();
                    option.setValue(entry.getKey());
                    option.setLabel(semesterLabel(entry.getKey()));
                    option.setCourseCount(entry.getValue());
                    return option;
                })
                .toList();
    }

    private List<DashboardDeanResponse.MajorOption> mapMajorOptions(
            List<Major> majors,
            List<Cohort> cohorts) {
        Map<Integer, Long> cohortCounts = cohorts.stream()
                .filter(cohort -> cohort.getProgram() != null)
                .filter(cohort -> cohort.getProgram().getMajor() != null)
                .collect(Collectors.groupingBy(
                        cohort -> cohort.getProgram().getMajor().getId(),
                        Collectors.counting()));
        return majors.stream().map(major -> {
            DashboardDeanResponse.MajorOption option =
                    new DashboardDeanResponse.MajorOption();
            option.setId(major.getId());
            option.setCode(major.getCode());
            option.setName(major.getName());
            option.setNameVn(major.getNameVn());
            option.setActiveCohortCount(
                    cohortCounts.getOrDefault(major.getId(), 0L));
            return option;
        }).toList();
    }

    private List<DashboardDeanResponse.CohortOption> mapCohortOptions(
            List<Cohort> cohorts,
            Integer selectedMajorId) {
        return cohorts.stream()
                .filter(cohort -> selectedMajorId == null
                        || Objects.equals(
                                cohort.getProgram().getMajor().getId(),
                                selectedMajorId))
                .map(cohort -> {
                    Program program = cohort.getProgram();
                    Major major = program.getMajor();
                    DashboardDeanResponse.CohortOption option =
                            new DashboardDeanResponse.CohortOption();
                    option.setId(cohort.getId());
                    option.setMajorId(major.getId());
                    option.setMajorCode(major.getCode());
                    option.setProgramId(program.getId());
                    option.setProgramCode(program.getCode());
                    option.setProgramName(program.getName());
                    option.setEntryYear(cohort.getEntryYear());
                    option.setName(cohort.getName());
                    option.setActive(cohort.getIsActive());
                    return option;
                })
                .toList();
    }

    private DashboardDeanResponse.SelectedScope mapScope(
            Selection selection) {
        DashboardDeanResponse.SelectedScope scope =
                new DashboardDeanResponse.SelectedScope();
        Major major = selection.major();
        if (major != null) {
            scope.setMajorId(major.getId());
            scope.setMajorCode(major.getCode());
            scope.setMajorName(major.getName());
            scope.setMajorNameVn(major.getNameVn());
        }
        Program program = selection.program();
        if (program != null) {
            scope.setProgramId(program.getId());
            scope.setProgramCode(program.getCode());
            scope.setProgramName(program.getName());
        }
        Cohort cohort = selection.cohort();
        if (cohort != null) {
            scope.setCohortId(cohort.getId());
            scope.setCohortName(cohort.getName());
            scope.setCohortEntryYear(cohort.getEntryYear());
        }
        scope.setSemester(selection.semester());
        scope.setSemesterLabel(semesterLabel(selection.semester()));
        return scope;
    }

    private Comparator<CourseSnapshot> snapshotComparator() {
        return Comparator
                .comparing(CourseSnapshot::semester,
                        Comparator.nullsLast(
                                Comparator.naturalOrder()))
                .thenComparing(snapshot -> snapshot.dto().getCourseCode(),
                        Comparator.nullsLast(
                                Comparator.naturalOrder()));
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "APPROVED" -> "Đã phê duyệt";
            case "UNDER_REVIEW" -> "Đang thẩm định";
            case "SUBMITTED" -> "Đã nộp";
            case "REVISION_REQUESTED" -> "Yêu cầu chỉnh sửa";
            case "REJECTED" -> "Bị từ chối";
            case "ARCHIVED" -> "Đã lưu trữ";
            case "MISSING" -> "Chưa có đề cương";
            default -> "Bản nháp";
        };
    }

    private String semesterLabel(Integer semester) {
        return semester == null
                ? "Tất cả học kỳ"
                : "Học kỳ " + semester;
    }

    private void validateSemester(Integer semester) {
        if (semester != null
                && (semester < MIN_SEMESTER
                        || semester > MAX_SEMESTER)) {
            throw new IllegalArgumentException(
                    "Học kỳ phải nằm trong khoảng 1 đến 8.");
        }
    }

    private OffsetDateTime toOffsetDateTime(
            LocalDateTime value,
            ZoneId zoneId) {
        return value == null
                ? null
                : value.atZone(zoneId).toOffsetDateTime();
    }

    private int safeInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private double percentage(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return Math.round((numerator * 1000.0) / denominator) / 10.0;
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT)
                        .replace("-", "")
                        .replace("_", "")
                        .replace(" ", "");
    }

    private DashboardDeanResponse.DataWarning warning(
            String severity,
            String code,
            String message,
            List<String> references) {
        DashboardDeanResponse.DataWarning warning =
                new DashboardDeanResponse.DataWarning();
        warning.setSeverity(severity);
        warning.setCode(code);
        warning.setMessage(message);
        warning.setReferences(references);
        return warning;
    }

    private List<DashboardDeanResponse.DataWarning> deduplicateWarnings(
            List<DashboardDeanResponse.DataWarning> warnings) {
        Map<String, DashboardDeanResponse.DataWarning> unique =
                new LinkedHashMap<>();
        for (DashboardDeanResponse.DataWarning warning : warnings) {
            String key = warning.getCode() + "::"
                    + String.join(",", warning.getReferences());
            unique.putIfAbsent(key, warning);
        }
        return new ArrayList<>(unique.values());
    }

    private record Selection(
            Major major,
            Program program,
            Cohort cohort,
            Integer semester) {
    }

    private record SyllabusResolution(
            Syllabus syllabus,
            boolean explicitLink,
            String dataQualityState) {
    }

    private record CourseSnapshot(
            Integer semester,
            int credits,
            DashboardDeanResponse.CourseProgress dto) {
    }
}
