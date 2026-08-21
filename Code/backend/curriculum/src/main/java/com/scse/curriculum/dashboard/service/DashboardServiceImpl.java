package com.scse.curriculum.dashboard.service;

import com.scse.curriculum.auth.security.CurrentUserService;
import com.scse.curriculum.common.exception.ForbiddenOperationException;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.dashboard.dto.*;
import com.scse.curriculum.instructor.entity.Instructor;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.plo.entity.Plo;
import com.scse.curriculum.cloplomapping.entity.CloPloMapping;
import com.scse.curriculum.cloplomapping.entity.ContributionLevel;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {


    private final EntityManager entityManager;
    private final CurrentUserService currentUserService;
    private final FacultyDashboardQueryService facultyDashboardQueryService;
    private final DeanDashboardQueryService deanDashboardQueryService;
    private final AdminDashboardQueryService adminDashboardQueryService;

    public DashboardServiceImpl(
            EntityManager entityManager,
            CurrentUserService currentUserService,
            FacultyDashboardQueryService facultyDashboardQueryService,
            DeanDashboardQueryService deanDashboardQueryService,
            AdminDashboardQueryService adminDashboardQueryService) {
        this.entityManager = entityManager;
        this.currentUserService = currentUserService;
        this.facultyDashboardQueryService = facultyDashboardQueryService;
        this.deanDashboardQueryService = deanDashboardQueryService;
        this.adminDashboardQueryService = adminDashboardQueryService;
    }

    @Override
    public DashboardDeanResponse getDeanDashboard(
            Integer majorId,
            Integer cohortId,
            Integer semester) {
        return deanDashboardQueryService.getDashboard(
                majorId, cohortId, semester);
    }

    @Override
    public DashboardDeptHeadResponse getDeptHeadDashboard(long deptHeadUserId) {
        DashboardDeptHeadResponse response = new DashboardDeptHeadResponse();

        UserAccount headUser = resolveRequestedUser(deptHeadUserId);

        if (headUser.getInstructorId() == null) {
            throw new ForbiddenOperationException(
                    "Tài khoản Trưởng bộ môn chưa liên kết hồ sơ giảng viên.");
        }

        Instructor headInstructor = entityManager.find(
                Instructor.class,
                headUser.getInstructorId());

        if (headInstructor == null || headInstructor.getDepartment() == null) {
            throw new ForbiddenOperationException(
                    "Tài khoản Trưởng bộ môn chưa được gán bộ môn.");
        }

        int deptId = headInstructor.getDepartment().getId();

        List<Course> courses = entityManager.createQuery(
                        "SELECT c FROM Course c " +
                        "WHERE c.department.id = :deptId " +
                        "ORDER BY c.courseCode",
                        Course.class)
                .setParameter("deptId", deptId)
                .getResultList();

        response.setTotalCoursesInDept(courses.size());

        List<Syllabus> syllabuses = entityManager.createQuery(
                        "SELECT s FROM Syllabus s " +
                        "JOIN FETCH s.course c " +
                        "LEFT JOIN FETCH s.createdBy u " +
                        "WHERE c.department.id = :deptId " +
                        "ORDER BY c.courseCode ASC, " +
                        "CASE WHEN s.isCurrent = true THEN 0 ELSE 1 END ASC, " +
                        "s.versionNumber DESC, s.id DESC",
                        Syllabus.class)
                .setParameter("deptId", deptId)
                .getResultList();

        Map<Integer, Syllabus> latestSyllabusByCourse = new LinkedHashMap<>();

        for (Syllabus syllabus : syllabuses) {
            if (syllabus.getCourse() == null || syllabus.getCourse().getId() == null) {
                continue;
            }

            latestSyllabusByCourse.putIfAbsent(
                    syllabus.getCourse().getId(),
                    syllabus);
        }

        long toReview = latestSyllabusByCourse.values().stream()
                .filter(syllabus -> syllabus.getStatus() == SyllabusStatus.SUBMITTED)
                .count();

        response.setSyllabusesToReview(toReview);

        List<DashboardDeptHeadResponse.CourseSyllabusStatus> statuses =
                new ArrayList<>();

        for (Course course : courses) {
            Syllabus latest = latestSyllabusByCourse.get(course.getId());

            DashboardDeptHeadResponse.CourseSyllabusStatus item =
                    new DashboardDeptHeadResponse.CourseSyllabusStatus();

            item.setCourseCode(course.getCourseCode());
            item.setCourseName(course.getName());
            item.setInstructorName(resolveResponsibleInstructor(course, latest));
            item.setStatus(
                    latest == null || latest.getStatus() == null
                            ? "NOT_CREATED"
                            : latest.getStatus().name());

            statuses.add(item);
        }

        response.setCoursesStatus(statuses);
        return response;
    }

    private String resolveResponsibleInstructor(
            Course course,
            Syllabus syllabus) {

        if (syllabus != null && syllabus.getId() != null) {
            List<String> linkedInstructorNames = entityManager.createQuery(
                            "SELECT i.fullName " +
                            "FROM ClassSection cs " +
                            "JOIN cs.instructor i " +
                            "WHERE cs.syllabus.id = :syllabusId " +
                            "ORDER BY CASE WHEN cs.isActive = true THEN 0 ELSE 1 END, cs.id DESC",
                            String.class)
                    .setParameter("syllabusId", syllabus.getId())
                    .setMaxResults(1)
                    .getResultList();

            if (!linkedInstructorNames.isEmpty()
                    && linkedInstructorNames.get(0) != null
                    && !linkedInstructorNames.get(0).isBlank()) {
                return linkedInstructorNames.get(0).trim();
            }
        }

        List<String> activeInstructorNames = entityManager.createQuery(
                        "SELECT i.fullName " +
                        "FROM ClassSection cs " +
                        "JOIN cs.instructor i " +
                        "WHERE cs.course.id = :courseId " +
                        "AND cs.isActive = true " +
                        "ORDER BY cs.academicYear DESC, cs.semester DESC, cs.id DESC",
                        String.class)
                .setParameter("courseId", course.getId())
                .setMaxResults(1)
                .getResultList();

        if (!activeInstructorNames.isEmpty()
                && activeInstructorNames.get(0) != null
                && !activeInstructorNames.get(0).isBlank()) {
            return activeInstructorNames.get(0).trim();
        }

        return "Not assigned";
    }

    @Override
    public DashboardFacultyResponse getFacultyDashboard(long facultyUserId) {
        return facultyDashboardQueryService.getForRequestedUser(facultyUserId);
    }

    @Override
    public DashboardFacultyResponse getMyFacultyDashboard() {
        return facultyDashboardQueryService.getMyDashboard();
    }

    @Override
public DashboardAdminResponse getAdminDashboard(
        String academicYear,
        Integer semester,
        Integer programId,
        Integer cohortId) {

    return adminDashboardQueryService.getDashboard(
            academicYear,
            semester,
            programId,
            cohortId);
}
@Override
public List<DashboardAdminResponse.TermOption> getAdminTermOptions() {
    return adminDashboardQueryService.getTermOptions();
}

    @Override
    public DashboardHeatmapResponse getHeatmapCoverage(long programId, Integer cohortId, String academicYear, String semester, Integer courseTypeId) {
        Program program = entityManager.find(Program.class, (int) programId);
        if (program == null) {
            throw new ResourceNotFoundException("Program not found");
        }

        DashboardHeatmapResponse response = new DashboardHeatmapResponse();
        response.setProgramId(program.getId());
        response.setProgramCode(program.getCode());
        response.setProgramName(program.getName());
        response.setProgramNameVn(program.getNameVn());

        if (cohortId == null) {
            throw new IllegalArgumentException("cohortId is required");
        }
        if (academicYear == null || academicYear.isBlank()) {
            throw new IllegalArgumentException("academicYear is required");
        }
        if (semester == null || semester.isBlank()) {
            throw new IllegalArgumentException("semester is required");
        }

        com.scse.curriculum.cohort.entity.Cohort cohort = entityManager.find(
                com.scse.curriculum.cohort.entity.Cohort.class, cohortId);
        if (cohort == null || cohort.getProgram() == null
                || !program.getId().equals(cohort.getProgram().getId())) {
            throw new ResourceNotFoundException("Cohort does not belong to the selected program");
        }
        String normalizedAcademicYear = academicYear.trim();
        String normalizedSemester = semester.trim();

        response.setCohortId(cohort.getId());
        response.setCohortName(cohort.getName());
        response.setCohortEntryYear(cohort.getEntryYear());
        response.setAcademicYear(normalizedAcademicYear);
        response.setSemester(normalizedSemester);
        response.setCourseTypeId(courseTypeId);
        response.setDataSource("APPROVED syllabus selected by program + cohort + academic year + semester"
                + (courseTypeId == null ? "" : " + course type"));
        response.setScopeKey(buildScopeKey(program.getId(), cohort.getId(),
                normalizedAcademicYear, normalizedSemester, courseTypeId));

        List<Plo> plos = entityManager.createQuery(
                        "SELECT p FROM Plo p " +
                        "WHERE p.program.id = :pid " +
                        "AND (p.isActive = true OR p.isActive IS NULL) " +
                        "ORDER BY p.code, p.versionNumber",
                        Plo.class)
                .setParameter("pid", (int) programId)
                .getResultList();

        response.setPlos(plos.stream().map(Plo::getCode).toList());

        String courseProgramJpql =
                "SELECT cp FROM CourseProgram cp " +
                "JOIN FETCH cp.course c " +
                "LEFT JOIN FETCH cp.courseType ct " +
                "LEFT JOIN FETCH cp.syllabus linkedSyllabus " +
                "LEFT JOIN FETCH cp.cohort cpCohort " +
                "WHERE cp.program.id = :pid " +
                "AND (cpCohort.id = :cohortId OR cpCohort IS NULL) " +
                (courseTypeId == null ? "" : "AND ct.id = :courseTypeId ") +
                "ORDER BY c.courseCode, " +
                "CASE WHEN cpCohort.id = :cohortId THEN 0 ELSE 1 END, cp.id";

        var courseProgramQuery = entityManager.createQuery(courseProgramJpql, CourseProgram.class)
                .setParameter("pid", (int) programId)
                .setParameter("cohortId", cohortId);
        if (courseTypeId != null) {
            courseProgramQuery.setParameter("courseTypeId", courseTypeId);
        }
        List<CourseProgram> scopedCoursePrograms = courseProgramQuery.getResultList();

        if (courseTypeId != null) {
            var selectedType = entityManager.find(
                    com.scse.curriculum.coursetype.entity.CourseType.class,
                    courseTypeId);
            if (selectedType == null) {
                throw new ResourceNotFoundException("Course type not found");
            }
            response.setCourseTypeCode(selectedType.getCode());
            response.setCourseTypeName(selectedType.getName());
            response.setCourseTypeNameVn(selectedType.getNameVn());
        }

        Map<Integer, CourseProgram> courseProgramByCourse = new LinkedHashMap<>();
        for (CourseProgram cp : scopedCoursePrograms) {
            courseProgramByCourse.putIfAbsent(cp.getCourse().getId(), cp);
        }
        int duplicateCourseProgramRows = Math.max(
                scopedCoursePrograms.size() - courseProgramByCourse.size(), 0);
        List<Course> courses = courseProgramByCourse.values().stream()
                .map(CourseProgram::getCourse)
                .toList();

        Set<Integer> activePloIds = plos.stream()
                .map(Plo::getId)
                .collect(Collectors.toSet());
        Set<Integer> coveredPloIds = new HashSet<>();
        Map<Integer, Set<Integer>> ploCourseIds = new HashMap<>();
        Map<Integer, Set<Integer>> ploCloIds = new HashMap<>();

        List<DashboardHeatmapResponse.CourseCoverage> courseCoverages = new ArrayList<>();
        List<String> coursesWithoutApprovedSyllabus = new ArrayList<>();
        List<String> unmappedCloReferences = new ArrayList<>();

        int coursesWithApprovedSyllabus = 0;
        int totalClos = 0;
        int mappedClos = 0;

        for (Course course : courses) {
            DashboardHeatmapResponse.CourseCoverage courseCoverage =
                    new DashboardHeatmapResponse.CourseCoverage();
            courseCoverage.setCourseId(course.getId());
            courseCoverage.setCourseCode(course.getCourseCode());
            courseCoverage.setCourseName(course.getName());
            courseCoverage.setCourseNameVn(course.getNameVn());

            CourseProgram scopedCourseProgram = courseProgramByCourse.get(course.getId());
            if (scopedCourseProgram != null && scopedCourseProgram.getCourseType() != null) {
                courseCoverage.setCourseTypeId(scopedCourseProgram.getCourseType().getId());
                courseCoverage.setCourseTypeCode(scopedCourseProgram.getCourseType().getCode());
                courseCoverage.setCourseTypeName(scopedCourseProgram.getCourseType().getName());
                courseCoverage.setCourseTypeNameVn(scopedCourseProgram.getCourseType().getNameVn());
            }
            List<Syllabus> approvedSyllabuses = new ArrayList<>();

            Syllabus explicitlyLinked = scopedCourseProgram != null
                    ? scopedCourseProgram.getSyllabus() : null;
            if (explicitlyLinked != null
                    && explicitlyLinked.getStatus() == SyllabusStatus.APPROVED
                    && normalizedAcademicYear.equalsIgnoreCase(nullSafe(explicitlyLinked.getAcademicYear()))
                    && normalizedSemester.equalsIgnoreCase(nullSafe(explicitlyLinked.getSemester()))) {
                approvedSyllabuses.add(explicitlyLinked);
            } else {
                approvedSyllabuses = entityManager.createQuery(
                                "SELECT s FROM Syllabus s " +
                                "WHERE s.course.id = :courseId " +
                                "AND s.status = :status " +
                                "AND LOWER(TRIM(s.academicYear)) = LOWER(TRIM(:academicYear)) " +
                                "AND LOWER(TRIM(s.semester)) = LOWER(TRIM(:semester)) " +
                                "ORDER BY CASE WHEN s.isCurrent = true THEN 0 ELSE 1 END, " +
                                "s.versionNumber DESC, s.approvedAt DESC, s.id DESC",
                                Syllabus.class)
                        .setParameter("courseId", course.getId())
                        .setParameter("status", SyllabusStatus.APPROVED)
                        .setParameter("academicYear", normalizedAcademicYear)
                        .setParameter("semester", normalizedSemester)
                        .setMaxResults(1)
                        .getResultList();
            }

            if (approvedSyllabuses.isEmpty()) {
                courseCoverage.setHasApprovedSyllabus(false);
                courseCoverage.setTotalClos(0);
                courseCoverage.setMappedClos(0);
                courseCoverage.setUnmappedCloCodes(List.of());
                courseCoverage.setCoverageLevels(plos.stream().map(plo -> (String) null).toList());
                courseCoverage.setCells(buildEmptyCells(plos));
                courseCoverages.add(courseCoverage);
                coursesWithoutApprovedSyllabus.add(course.getCourseCode());
                continue;
            }

            coursesWithApprovedSyllabus++;
            Syllabus syllabus = approvedSyllabuses.get(0);
            courseCoverage.setHasApprovedSyllabus(true);
            courseCoverage.setSyllabusId(syllabus.getId());
            courseCoverage.setSyllabusVersion(syllabus.getVersionNumber());
            courseCoverage.setSyllabusVersionLabel(syllabus.getVersionLabel());
            courseCoverage.setSyllabusAcademicYear(syllabus.getAcademicYear());
            courseCoverage.setSyllabusSemester(syllabus.getSemester());
            courseCoverage.setExplicitCurriculumLink(
                    scopedCourseProgram != null
                    && scopedCourseProgram.getSyllabus() != null
                    && scopedCourseProgram.getSyllabus().getId().equals(syllabus.getId()));

            List<Clo> clos = entityManager.createQuery(
                            "SELECT c FROM Clo c " +
                            "WHERE c.syllabus.id = :syllabusId " +
                            "ORDER BY c.orderIndex, c.code",
                            Clo.class)
                    .setParameter("syllabusId", syllabus.getId())
                    .getResultList();

            List<CloPloMapping> mappings = entityManager.createQuery(
                            "SELECT m FROM CloPloMapping m " +
                            "JOIN FETCH m.clo c " +
                            "JOIN FETCH m.plo p " +
                            "WHERE c.syllabus.id = :syllabusId " +
                            "AND p.program.id = :programId",
                            CloPloMapping.class)
                    .setParameter("syllabusId", syllabus.getId())
                    .setParameter("programId", (int) programId)
                    .getResultList()
                    .stream()
                    .filter(mapping -> activePloIds.contains(mapping.getPlo().getId()))
                    .toList();

            Map<Integer, List<CloPloMapping>> mappingsByPlo = mappings.stream()
                    .collect(Collectors.groupingBy(
                            mapping -> mapping.getPlo().getId(),
                            LinkedHashMap::new,
                            Collectors.toList()));

            Set<Integer> mappedCloIds = mappings.stream()
                    .map(mapping -> mapping.getClo().getId())
                    .collect(Collectors.toSet());

            List<String> unmappedCloCodes = clos.stream()
                    .filter(clo -> !mappedCloIds.contains(clo.getId()))
                    .map(Clo::getCode)
                    .toList();

            totalClos += clos.size();
            mappedClos += mappedCloIds.size();

            courseCoverage.setTotalClos(clos.size());
            courseCoverage.setMappedClos(mappedCloIds.size());
            courseCoverage.setUnmappedCloCodes(unmappedCloCodes);

            for (String cloCode : unmappedCloCodes) {
                unmappedCloReferences.add(course.getCourseCode() + " - " + cloCode);
            }

            List<String> coverageLevels = new ArrayList<>();
            List<DashboardHeatmapResponse.CellCoverage> cells = new ArrayList<>();

            for (Plo plo : plos) {
                List<CloPloMapping> ploMappings =
                        mappingsByPlo.getOrDefault(plo.getId(), List.of());

                ContributionLevel highestLevel = ploMappings.stream()
                        .map(CloPloMapping::getLevel)
                        .filter(level -> level != null)
                        .max(Comparator.comparingInt(Enum::ordinal))
                        .orElse(null);

                List<String> cloCodes = ploMappings.stream()
                        .map(mapping -> mapping.getClo().getCode())
                        .distinct()
                        .sorted()
                        .toList();

                DashboardHeatmapResponse.CellCoverage cell =
                        new DashboardHeatmapResponse.CellCoverage();
                cell.setPloId(plo.getId());
                cell.setPloCode(plo.getCode());
                cell.setLevel(highestLevel == null ? null : highestLevel.name());
                cell.setMappingCount(cloCodes.size());
                cell.setCloCodes(cloCodes);
                cells.add(cell);
                coverageLevels.add(cell.getLevel());

                if (!ploMappings.isEmpty()) {
                    coveredPloIds.add(plo.getId());
                    ploCourseIds.computeIfAbsent(plo.getId(), ignored -> new HashSet<>())
                            .add(course.getId());
                    Set<Integer> cloIdsForPlo = ploCloIds.computeIfAbsent(
                            plo.getId(), ignored -> new HashSet<>());
                    ploMappings.forEach(mapping -> cloIdsForPlo.add(mapping.getClo().getId()));
                }
            }

            courseCoverage.setCoverageLevels(coverageLevels);
            courseCoverage.setCells(cells);
            courseCoverages.add(courseCoverage);
        }

        List<DashboardHeatmapResponse.PloColumn> ploDetails = new ArrayList<>();
        for (Plo plo : plos) {
            DashboardHeatmapResponse.PloColumn column =
                    new DashboardHeatmapResponse.PloColumn();
            column.setId(plo.getId());
            column.setCode(plo.getCode());
            column.setDescription(plo.getDescription());
            column.setDescriptionVn(plo.getDescriptionVn());
            column.setCategory(plo.getCategory());
            column.setVersionNumber(plo.getVersionNumber());
            column.setCovered(coveredPloIds.contains(plo.getId()));
            column.setCourseCount(ploCourseIds.getOrDefault(plo.getId(), Set.of()).size());
            column.setCloCount(ploCloIds.getOrDefault(plo.getId(), Set.of()).size());
            ploDetails.add(column);
        }

        DashboardHeatmapResponse.HeatmapSummary summary =
                new DashboardHeatmapResponse.HeatmapSummary();
        int totalPlos = plos.size();
        int coveredPlos = coveredPloIds.size();
        int uncoveredPlos = Math.max(totalPlos - coveredPlos, 0);
        double coveragePercentage = totalPlos == 0
                ? 0.0
                : Math.round((coveredPlos * 1000.0) / totalPlos) / 10.0;

        summary.setTotalPlos(totalPlos);
        summary.setCoveredPlos(coveredPlos);
        summary.setUncoveredPlos(uncoveredPlos);
        summary.setPloCoveragePercentage(coveragePercentage);
        summary.setTotalCourses(courses.size());
        summary.setCoursesWithApprovedSyllabus(coursesWithApprovedSyllabus);
        summary.setCoursesWithoutApprovedSyllabus(coursesWithoutApprovedSyllabus.size());
        summary.setTotalClos(totalClos);
        summary.setMappedClos(mappedClos);
        summary.setUnmappedClos(Math.max(totalClos - mappedClos, 0));
        summary.setApprovedSyllabusPercentage(percent(
                coursesWithApprovedSyllabus, courses.size()));
        summary.setCloMappingPercentage(percent(mappedClos, totalClos));

        List<DashboardHeatmapResponse.HeatmapWarning> warnings = new ArrayList<>();

        if (plos.isEmpty()) {
            warnings.add(buildWarning(
                    "ERROR",
                    "NO_PLOS",
                    "Chương trình chưa có PLO đang hoạt động. Hãy tạo PLO trước khi xem ma trận.",
                    List.of()));
        }

        if (courses.isEmpty()) {
            warnings.add(buildWarning(
                    "ERROR",
                    "NO_COURSES",
                    "Chương trình chưa có môn học nên chưa thể đánh giá độ bao phủ PLO.",
                    List.of()));
        }

        List<String> uncoveredPloCodes = ploDetails.stream()
                .filter(column -> !Boolean.TRUE.equals(column.getCovered()))
                .map(DashboardHeatmapResponse.PloColumn::getCode)
                .toList();
        if (!uncoveredPloCodes.isEmpty()) {
            warnings.add(buildWarning(
                    "WARNING",
                    "UNCOVERED_PLOS",
                    "Có " + uncoveredPloCodes.size() +
                            " PLO chưa được bất kỳ CLO nào bao phủ.",
                    uncoveredPloCodes));
        }

        if (!coursesWithoutApprovedSyllabus.isEmpty()) {
            warnings.add(buildWarning(
                    "WARNING",
                    "COURSES_WITHOUT_APPROVED_SYLLABUS",
                    "Có " + coursesWithoutApprovedSyllabus.size() +
                            " môn chưa có đề cương APPROVED nên chưa thể tính mapping.",
                    coursesWithoutApprovedSyllabus));
        }

        if (!unmappedCloReferences.isEmpty()) {
            warnings.add(buildWarning(
                    "WARNING",
                    "UNMAPPED_CLOS",
                    "Có " + unmappedCloReferences.size() +
                            " CLO chưa liên kết với PLO.",
                    unmappedCloReferences));
        }

        if (duplicateCourseProgramRows > 0) {
            warnings.add(buildWarning(
                    "INFO",
                    "DUPLICATE_CURRICULUM_ROWS_REMOVED",
                    "Đã loại " + duplicateCourseProgramRows +
                            " dòng CTĐT trùng course; ưu tiên cấu hình riêng của cohort.",
                    List.of()));
        }

        if (!courses.isEmpty() && coursesWithApprovedSyllabus == 0) {
            warnings.add(buildWarning(
                    "ERROR",
                    "NO_APPROVED_SYLLABUS_IN_SCOPE",
                    "Không có đề cương APPROVED nào trong đúng phạm vi đã chọn.",
                    courses.stream().map(Course::getCourseCode).toList()));
        } else if (summary.getApprovedSyllabusPercentage() < 100.0) {
            warnings.add(buildWarning(
                    "WARNING",
                    "PARTIAL_APPROVED_SYLLABUS_COVERAGE",
                    "Tỷ lệ môn có đề cương APPROVED là " +
                            summary.getApprovedSyllabusPercentage() + "%.",
                    coursesWithoutApprovedSyllabus));
        }

        if (coursesWithApprovedSyllabus > 0 && mappedClos == 0) {
            warnings.add(buildWarning(
                    "ERROR",
                    "NO_CLO_PLO_MAPPINGS",
                    "Các đề cương APPROVED trong phạm vi chưa có mapping CLO–PLO.",
                    List.of()));
        } else if (totalClos > 0 && summary.getCloMappingPercentage() < 100.0) {
            warnings.add(buildWarning(
                    "WARNING",
                    "PARTIAL_CLO_MAPPING_COVERAGE",
                    "Tỷ lệ CLO đã mapping là " + summary.getCloMappingPercentage() + "%.",
                    unmappedCloReferences));
        }

        summary.setErrorCount((int) warnings.stream()
                .filter(w -> "ERROR".equalsIgnoreCase(w.getSeverity())).count());
        summary.setWarningCount((int) warnings.stream()
                .filter(w -> "WARNING".equalsIgnoreCase(w.getSeverity())).count());
        summary.setInfoCount((int) warnings.stream()
                .filter(w -> "INFO".equalsIgnoreCase(w.getSeverity())).count());

        response.setPloDetails(ploDetails);
        response.setCourseCoverages(courseCoverages);
        response.setSummary(summary);
        response.setWarnings(warnings);
        return response;
    }

    private List<DashboardHeatmapResponse.CellCoverage> buildEmptyCells(List<Plo> plos) {
        List<DashboardHeatmapResponse.CellCoverage> cells = new ArrayList<>();
        for (Plo plo : plos) {
            DashboardHeatmapResponse.CellCoverage cell =
                    new DashboardHeatmapResponse.CellCoverage();
            cell.setPloId(plo.getId());
            cell.setPloCode(plo.getCode());
            cell.setLevel(null);
            cell.setMappingCount(0);
            cell.setCloCodes(List.of());
            cells.add(cell);
        }
        return cells;
    }

    private DashboardHeatmapResponse.HeatmapWarning buildWarning(
            String severity,
            String code,
            String message,
            List<String> references) {
        DashboardHeatmapResponse.HeatmapWarning warning =
                new DashboardHeatmapResponse.HeatmapWarning();
        warning.setSeverity(severity);
        warning.setCode(code);
        warning.setMessage(message);
        warning.setReferences(references);
        return warning;
    }

    private UserAccount resolveRequestedUser(long requestedUserId) {
        UserAccount current = currentUserService.getCurrentUser();

        if (current.getRole() == UserRole.ADMIN) {
            UserAccount requested = entityManager.find(
                    UserAccount.class,
                    Math.toIntExact(requestedUserId));
            if (requested == null) {
                throw new ResourceNotFoundException("User not found");
            }
            return requested;
        }

        if (!current.getId().equals(Math.toIntExact(requestedUserId))) {
            throw new ForbiddenOperationException(
                    "Bạn chỉ được xem dashboard của chính mình.");
        }

        return current;
    }


    private double percent(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return Math.round((numerator * 1000.0) / denominator) / 10.0;
    }

    private String buildScopeKey(
            Integer programId,
            Integer cohortId,
            String academicYear,
            String semester,
            Integer courseTypeId) {
        return "program=" + programId
                + "|cohort=" + cohortId
                + "|academicYear=" + academicYear
                + "|semester=" + semester
                + "|courseType=" + (courseTypeId == null ? "ALL" : courseTypeId);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value.trim();
    }

}