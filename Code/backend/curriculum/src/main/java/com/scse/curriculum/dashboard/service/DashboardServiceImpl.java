package com.scse.curriculum.dashboard.service;

import com.scse.curriculum.syllabus.entity.SyllabusVersion;

import com.scse.curriculum.auth.security.CurrentUserService;
import com.scse.curriculum.approval.entity.ApprovalStatus;
import com.scse.curriculum.approval.entity.ApprovalStep;
import com.scse.curriculum.approval.repository.ApprovalRequestRepository;
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

import java.util.Objects;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
        private final ApprovalRequestRepository approvalRequestRepository;

        public DashboardServiceImpl(
                        EntityManager entityManager,
                        CurrentUserService currentUserService,
                        FacultyDashboardQueryService facultyDashboardQueryService,
                        DeanDashboardQueryService deanDashboardQueryService,
                        AdminDashboardQueryService adminDashboardQueryService,
                        ApprovalRequestRepository approvalRequestRepository) {
                this.entityManager = entityManager;
                this.currentUserService = currentUserService;
                this.facultyDashboardQueryService = facultyDashboardQueryService;
                this.deanDashboardQueryService = deanDashboardQueryService;
                this.adminDashboardQueryService = adminDashboardQueryService;
                this.approvalRequestRepository = approvalRequestRepository;
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

                if (headUser.getManagedMajor() == null) {
                        throw new ForbiddenOperationException(
                                        "The Head of Department account is not assigned to a Managed Major.");
                }
                int majorId = headUser.getManagedMajor().getId();

                List<Course> courses = entityManager.createQuery(
                                "SELECT DISTINCT c FROM CourseProgram cp JOIN cp.course c " +
                                                "WHERE cp.program.major.id = :majorId " +
                                                "ORDER BY c.courseCode",
                                Course.class)
                                .setParameter("majorId", majorId)
                                .getResultList();

                response.setTotalCoursesInDept(courses.size());

                List<Syllabus> syllabuses = entityManager.createQuery(
                                "SELECT s FROM Syllabus s " +
                                                "JOIN FETCH s.course c " +
                                                "LEFT JOIN FETCH s.createdBy u " +
                                                "WHERE EXISTS (SELECT cp.id FROM CourseProgram cp WHERE cp.syllabus.id = s.id AND cp.program.major.id = :majorId) "
                                                +
                                                "ORDER BY c.courseCode ASC, " +
                                                "CASE WHEN s.isCurrent = true THEN 0 ELSE 1 END ASC, " +
                                                "s.versionNumber DESC, s.id DESC",
                                Syllabus.class)
                                .setParameter("majorId", majorId)
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

                long toReview = approvalRequestRepository
                                .findPendingByManagedMajor(
                                                ApprovalStep.STEP1_DEPT_HEAD,
                                                ApprovalStatus.PENDING,
                                                majorId)
                                .size();

                response.setSyllabusesToReview(toReview);

                List<DashboardDeptHeadResponse.CourseSyllabusStatus> statuses = new ArrayList<>();

                for (Course course : courses) {
                        Syllabus latest = latestSyllabusByCourse.get(course.getId());

                        DashboardDeptHeadResponse.CourseSyllabusStatus item = new DashboardDeptHeadResponse.CourseSyllabusStatus();

                        item.setCourseCode(course.getCourseCode());
                        item.setCourseName(course.getName());
                        item.setInstructorName(resolveResponsibleInstructor(course, latest, majorId));
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
                        Syllabus syllabus,
                        int majorId) {

                if (syllabus != null && syllabus.getId() != null) {
                        List<String> linkedInstructorNames = entityManager.createQuery(
                                        "SELECT i.fullName " +
                                                        "FROM ClassSection cs " +
                                                        "JOIN cs.instructor i " +
                                                        "WHERE cs.syllabus.id = :syllabusId " +
                                                        "AND cs.program.major.id = :majorId " +
                                                        "ORDER BY CASE WHEN cs.isActive = true THEN 0 ELSE 1 END, cs.id DESC",
                                        String.class)
                                        .setParameter("syllabusId", syllabus.getId())
                                        .setParameter("majorId", majorId)
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
                                                "AND cs.program.major.id = :majorId " +
                                                "AND cs.isActive = true " +
                                                "ORDER BY cs.academicYear DESC, cs.semester DESC, cs.id DESC",
                                String.class)
                                .setParameter("courseId", course.getId())
                                .setParameter("majorId", majorId)
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
        Integer majorId,
        Integer programId,
        Integer cohortId) {

    return adminDashboardQueryService.getDashboard(
            academicYear,
            semester,
            majorId,
            programId,
            cohortId);
}

        @Override
        public List<DashboardAdminResponse.TermOption> getAdminTermOptions() {
                return adminDashboardQueryService.getTermOptions();
        }

        @Override
        public DashboardHeatmapResponse getHeatmapCoverage(long programId, Integer cohortId, String academicYear,
                        String semester, Integer courseTypeId) {
                return getHeatmapCoverage(programId, cohortId, academicYear, semester,
                                courseTypeId, null, null);
        }

        @Override
        public DashboardHeatmapResponse getHeatmapCoverage(long programId, Integer cohortId, String academicYear,
                        String semester, Integer courseTypeId,
                        String search, String status) {
                UserAccount currentUser = currentUserService.getCurrentUser();
                Integer managedMajorId = null;
                if (currentUser.getRole() == UserRole.DEPT_HEAD) {
                        if (currentUser.getManagedMajor() == null) {
                                throw new ForbiddenOperationException(
                                                "The Head of Department account is not assigned to a managed Major.");
                        }
                        managedMajorId = currentUser.getManagedMajor().getId();
                }

                Program program = entityManager.find(Program.class, (int) programId);
                if (program == null) {
                        throw new ResourceNotFoundException("Program not found");
                }
                if (managedMajorId != null && (program.getMajor() == null
                                || !managedMajorId.equals(program.getMajor().getId()))) {
                        throw new ForbiddenOperationException(
                                        "You can only view curriculum analytics for your managed Major.");
                }

                DashboardHeatmapResponse response = new DashboardHeatmapResponse();
                response.setProgramId(program.getId());
                response.setProgramCode(program.getCode());
                response.setProgramName(program.getName());
                response.setProgramNameVn(program.getNameVn());
response.setAcademicYear(academicYear);
response.setSemester(semester);
                if (cohortId != null) {
                        com.scse.curriculum.cohort.entity.Cohort cohort = entityManager.find(
                                        com.scse.curriculum.cohort.entity.Cohort.class, cohortId);
                        if (cohort == null || cohort.getProgram() == null
                                        || !program.getId().equals(cohort.getProgram().getId())) {
                                throw new ResourceNotFoundException("Cohort does not belong to the selected program");
                        }
                        response.setCohortId(cohort.getId());
                        response.setCohortName(cohort.getName());
                        response.setCohortEntryYear(cohort.getEntryYear());
                } else {
                        response.setCohortName("All Cohorts");
                }
                if (courseTypeId != null) {
    com.scse.curriculum.coursetype.entity.CourseType courseType =
            entityManager.find(
                    com.scse.curriculum.coursetype.entity.CourseType.class,
                    courseTypeId);

    if (courseType == null) {
        throw new ResourceNotFoundException(
                "Course type not found");
    }

    response.setCourseTypeId(courseType.getId());
    response.setCourseTypeCode(courseType.getCode());
    response.setCourseTypeName(courseType.getName());
    response.setCourseTypeNameVn(courseType.getNameVn());
}
                response.setDataSource("APPROVED syllabus linked to the selected curriculum scope");
                response.setScopeKey(buildScopeKey(program.getId(), cohortId));

                List<Plo> activePloVersions = entityManager.createQuery(
                                "SELECT p FROM Plo p " +
                                                "WHERE p.program.id = :pid " +
                                                "AND (p.isActive = true OR p.isActive IS NULL) " +
                                                "ORDER BY p.code, p.versionNumber DESC, p.id DESC",
                                Plo.class)
                                .setParameter("pid", (int) programId)
                                .getResultList();

                List<Plo> plos = latestActivePloVersions(activePloVersions);

                response.setPlos(
                                plos.stream()
                                                .map(Plo::getCode)
                                                .toList());

                String courseProgramJpql = "SELECT cp FROM CourseProgram cp " +
                                "JOIN FETCH cp.course c " +
                                "LEFT JOIN FETCH cp.courseType ct " +
                                "LEFT JOIN FETCH cp.syllabus linkedSyllabus " +
                                "JOIN FETCH cp.cohort cpCohort " +
                                "WHERE cp.program.id = :pid " +
                                "AND (:cohortId IS NULL OR cpCohort.id = :cohortId) " +
                                "ORDER BY c.courseCode, cp.id";

                var courseProgramQuery = entityManager.createQuery(courseProgramJpql, CourseProgram.class)
                                .setParameter("pid", (int) programId)
                                .setParameter("cohortId", cohortId);
               List<CourseProgram> scopedCoursePrograms =
        courseProgramQuery.getResultList()
                .stream()
                .filter(cp ->
                        matchesCatalogFilters(
                                cp,
                                search,
                                academicYear,
                                semester,
                                status,
                                courseTypeId))
                .toList();
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
                        DashboardHeatmapResponse.CourseCoverage courseCoverage = new DashboardHeatmapResponse.CourseCoverage();
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
                        Syllabus explicitlyLinked = scopedCourseProgram != null
                                        ? scopedCourseProgram.getSyllabus()
                                        : null;
                        if (explicitlyLinked == null
                                        || explicitlyLinked.getStatus() != SyllabusStatus.APPROVED) {
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
                        Syllabus syllabus = explicitlyLinked;
                        courseCoverage.setHasApprovedSyllabus(true);
                        courseCoverage.setSyllabusId(syllabus.getId());
                        courseCoverage.setSyllabusVersion(syllabus.getVersionNumber());
                        courseCoverage.setSyllabusVersionLabel(syllabus.getVersionLabel());
                        courseCoverage.setSyllabusAcademicYear(syllabus.getAcademicYear());
                        courseCoverage.setSyllabusSemester(syllabus.getSemester());
                        courseCoverage.setExplicitCurriculumLink(
                                        scopedCourseProgram != null
                                                        && scopedCourseProgram.getSyllabus() != null
                                                        && scopedCourseProgram.getSyllabus().getId()
                                                                        .equals(syllabus.getId()));

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
                                List<CloPloMapping> ploMappings = mappingsByPlo.getOrDefault(plo.getId(), List.of());

                                DashboardHeatmapResponse.CellCoverage cell = buildCellCoverage(
                                                plo,
                                                ploMappings);

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
                        DashboardHeatmapResponse.PloColumn column = new DashboardHeatmapResponse.PloColumn();
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
        buildHeatmapSummary(
                plos,
                courseCoverages
        );

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

        static List<Plo> latestActivePloVersions(
                        List<Plo> candidates) {

                if (candidates == null
                                || candidates.isEmpty()) {
                        return List.of();
                }

                Map<String, Plo> latestByCode = new LinkedHashMap<>();

                for (Plo plo : candidates) {

                        if (plo == null
                                        || Boolean.FALSE.equals(
                                                        plo.getIsActive())) {
                                continue;
                        }

                        String key = normalizePloCode(
                                        plo.getCode());

                        if (key.isBlank()) {
                                continue;
                        }

                        Plo current = latestByCode.get(key);

                        if (current == null
                                        || comparePloVersion(
                                                        plo,
                                                        current) > 0) {

                                latestByCode.put(
                                                key,
                                                plo);
                        }
                }

                return latestByCode
                                .values()
                                .stream()
                                .sorted(
                                                Comparator.comparing(
                                                                plo -> normalizePloCode(
                                                                                plo.getCode())))
                                .toList();
        }

        private static int comparePloVersion(
                        Plo left,
                        Plo right) {

                int leftVersion = left.getVersionNumber() == null
                                ? 1
                                : left.getVersionNumber();

                int rightVersion = right.getVersionNumber() == null
                                ? 1
                                : right.getVersionNumber();

                int versionComparison = Integer.compare(
                                leftVersion,
                                rightVersion);

                if (versionComparison != 0) {
                        return versionComparison;
                }

                /*
                 * Legacy safety:
                 * nếu database vô tình có hai PLO cùng code
                 * và cùng version thì chọn row mới hơn.
                 */
                int leftId = left.getId() == null
                                ? 0
                                : left.getId();

                int rightId = right.getId() == null
                                ? 0
                                : right.getId();

                return Integer.compare(
                                leftId,
                                rightId);
        }

        private static String normalizePloCode(
                        String value) {

                return value == null
                                ? ""
                                : value
                                                .trim()
                                                .toUpperCase(Locale.ROOT);
        }
        static DashboardHeatmapResponse.HeatmapSummary
buildHeatmapSummary(
        List<Plo> plos,
        List<DashboardHeatmapResponse.CourseCoverage>
                courseCoverages) {

    List<Plo> safePlos =
            plos == null
                    ? List.of()
                    : plos;

    List<DashboardHeatmapResponse.CourseCoverage>
            safeCourses =
            courseCoverages == null
                    ? List.of()
                    : courseCoverages.stream()
                            .filter(Objects::nonNull)
                            .toList();

    int totalPlos =
            safePlos.size();

    int totalCourses =
            safeCourses.size();

    int coursesWithApprovedSyllabus =
            (int) safeCourses.stream()
                    .filter(course ->
                            Boolean.TRUE.equals(
                                    course.getHasApprovedSyllabus()
                            )
                    )
                    .count();

    int coursesWithoutApprovedSyllabus =
            Math.max(
                    totalCourses
                            - coursesWithApprovedSyllabus,
                    0
            );

    int totalClos =
            safeCourses.stream()
                    .mapToInt(course ->
                            nonNegative(
                                    course.getTotalClos()
                            )
                    )
                    .sum();

    int mappedClos =
            safeCourses.stream()
                    .mapToInt(course -> {

                        int courseTotal =
                                nonNegative(
                                        course.getTotalClos()
                                );

                        int courseMapped =
                                nonNegative(
                                        course.getMappedClos()
                                );

                        return Math.min(
                                courseMapped,
                                courseTotal
                        );
                    })
                    .sum();

    Set<String> coveredPloKeys =
            new HashSet<>();

    for (DashboardHeatmapResponse.CourseCoverage course
            : safeCourses) {

        if (course.getCells() == null) {
            continue;
        }

        for (DashboardHeatmapResponse.CellCoverage cell
                : course.getCells()) {

            if (cell == null
                    || nonNegative(
                            cell.getMappingCount()
                    ) == 0) {
                continue;
            }

            if (cell.getPloId() != null) {

                coveredPloKeys.add(
                        "ID:" + cell.getPloId()
                );

            } else if (cell.getPloCode() != null
                    && !cell.getPloCode()
                            .isBlank()) {

                coveredPloKeys.add(
                        "CODE:"
                                + normalizePloCode(
                                        cell.getPloCode()
                                )
                );
            }
        }
    }

    int coveredPlos =
            Math.min(
                    coveredPloKeys.size(),
                    totalPlos
            );

    int uncoveredPlos =
            Math.max(
                    totalPlos - coveredPlos,
                    0
            );

    int unmappedClos =
            Math.max(
                    totalClos - mappedClos,
                    0
            );

    DashboardHeatmapResponse.HeatmapSummary summary =
            new DashboardHeatmapResponse
                    .HeatmapSummary();

    summary.setTotalPlos(
            totalPlos
    );

    summary.setCoveredPlos(
            coveredPlos
    );

    summary.setUncoveredPlos(
            uncoveredPlos
    );

    summary.setPloCoveragePercentage(
            percent(
                    coveredPlos,
                    totalPlos
            )
    );

    summary.setTotalCourses(
            totalCourses
    );

    summary.setCoursesWithApprovedSyllabus(
            coursesWithApprovedSyllabus
    );

    summary.setCoursesWithoutApprovedSyllabus(
            coursesWithoutApprovedSyllabus
    );

    summary.setApprovedSyllabusPercentage(
            percent(
                    coursesWithApprovedSyllabus,
                    totalCourses
            )
    );

    summary.setTotalClos(
            totalClos
    );

    summary.setMappedClos(
            mappedClos
    );

    summary.setUnmappedClos(
            unmappedClos
    );

    summary.setCloMappingPercentage(
            percent(
                    mappedClos,
                    totalClos
            )
    );

    return summary;
}
static int nonNegative(
        Integer value) {

    return value == null
            ? 0
            : Math.max(value, 0);
}
static DashboardHeatmapResponse.CellCoverage
buildCellCoverage(
        Plo plo,
        List<CloPloMapping> mappings) {

    List<CloPloMapping> orderedMappings =
        mappings == null
                ? List.of()
                : mappings.stream()
                        .filter(Objects::nonNull)
                        .filter(mapping ->
                                mapping.getClo() != null)
                        .sorted(
                                Comparator
                                        .comparingInt(
                                                (CloPloMapping mapping) -> {
                                                    Integer order =
                                                            mapping.getClo()
                                                                    .getOrderIndex();

                                                    return order == null
                                                            ? Integer.MAX_VALUE
                                                            : order;
                                                }
                                        )
                                        .thenComparing(
                                                mapping -> {
                                                    String code =
                                                            mapping.getClo()
                                                                    .getCode();

                                                    return code == null
                                                            ? ""
                                                            : code;
                                                },
                                                String.CASE_INSENSITIVE_ORDER
                                        )
                        )
                        .toList();
    ContributionLevel highestLevel =
            highestContributionLevel(
                    orderedMappings.stream()
                            .map(
                                    CloPloMapping::getLevel
                            )
                            .toList()
            );

    List<String> cloCodes =
            orderedMappings.stream()
                    .map(mapping ->
                            mapping.getClo().getCode())
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

    List<DashboardHeatmapResponse.CloContribution>
            contributions =
            orderedMappings.stream()
                    .map(mapping -> {

                        DashboardHeatmapResponse
                                .CloContribution contribution =
                                new DashboardHeatmapResponse
                                        .CloContribution();

                        contribution.setCloId(
                                mapping.getClo().getId());

                        contribution.setCloCode(
                                mapping.getClo().getCode());

                        contribution.setDescription(
                                mapping.getClo()
                                        .getDescription());

                        contribution.setDescriptionVn(
                                mapping.getClo()
                                        .getDescriptionVn());

                        contribution.setLevel(
                                toCoverageLevel(
                                        mapping.getLevel()
                                )
                        );

                        return contribution;
                    })
                    .toList();

    DashboardHeatmapResponse.CellCoverage cell =
            new DashboardHeatmapResponse.CellCoverage();

    cell.setPloId(
            plo == null ? null : plo.getId());

    cell.setPloCode(
            plo == null ? null : plo.getCode());

    cell.setLevel(
            toCoverageLevel(highestLevel));

    cell.setMappingCount(
            cloCodes.size());

    cell.setCloCodes(
            cloCodes);

    cell.setCloContributions(
            contributions);

    return cell;
}
        private List<DashboardHeatmapResponse.CellCoverage>
buildEmptyCells(
        List<Plo> plos) {

    if (plos == null
            || plos.isEmpty()) {
        return List.of();
    }

    return plos.stream()
            .map(plo ->
                    buildCellCoverage(
                            plo,
                            List.of()
                    )
            )
            .toList();
}

        private DashboardHeatmapResponse.HeatmapWarning buildWarning(
                        String severity,
                        String code,
                        String message,
                        List<String> references) {
                DashboardHeatmapResponse.HeatmapWarning warning = new DashboardHeatmapResponse.HeatmapWarning();
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

                if (current.getRole() != UserRole.DEPT_HEAD
                                || !current.getId().equals(Math.toIntExact(requestedUserId))) {
                        throw new ForbiddenOperationException(
                                        "Bạn chỉ được xem dashboard của chính mình.");
                }

                return current;
        }

        static double percent(
        int numerator,
        int denominator) {

    if (denominator <= 0) {
        return 0.0;
    }

    int safeNumerator =
            Math.max(
                    0,
                    Math.min(
                            numerator,
                            denominator
                    )
            );

    return Math.round(
            (safeNumerator * 1000.0)
                    / denominator
    ) / 10.0;
}

        private String buildScopeKey(Integer programId, Integer cohortId) {
                return "program=" + programId
                                + "|cohort=" + cohortId;
        }

        static ContributionLevel highestContributionLevel(
                        List<ContributionLevel> levels) {

                if (levels == null
                                || levels.isEmpty()) {
                        return null;
                }

                return levels.stream()
                                .filter(Objects::nonNull)
                                .max(
                                                Comparator.comparingInt(
                                                                DashboardServiceImpl::contributionRank))
                                .orElse(null);
        }

        static int contributionRank(
                        ContributionLevel level) {

                if (level == null) {
                        return 0;
                }

                return switch (level) {
                        case I -> 1;
                        case D -> 2;
                        case A -> 3;
                };
        }

        static String toCoverageLevel(
                        ContributionLevel level) {

                if (level == null) {
                        return null;
                }

                return switch (level) {
                        case I -> "X";
                        case D -> "XX";
                        case A -> "XXX";
                };
        }

        private boolean matchesCatalogFilters(
        CourseProgram courseProgram,
        String search,
        String academicYear,
        String semester,
        String status,
        Integer courseTypeId) {

    Syllabus syllabus =
            courseProgram.getSyllabus();

    if (courseTypeId != null
            && (
                courseProgram.getCourseType() == null
                || !courseTypeId.equals(
                    courseProgram
                        .getCourseType()
                        .getId())
            )) {
        return false;
    }

    if (academicYear != null
            && !academicYear.isBlank()
            && (
                syllabus == null
                || syllabus.getAcademicYear() == null
                || !syllabus
                    .getAcademicYear()
                    .trim()
                    .equalsIgnoreCase(
                        academicYear.trim())
            )) {
        return false;
    }

    if (status != null
            && !status.isBlank()
            && (
                syllabus == null
                || syllabus.getStatus() == null
                || !syllabus
                    .getStatus()
                    .name()
                    .equalsIgnoreCase(
                        status.trim())
            )) {
        return false;
    }

    if (semester != null
            && !semester.isBlank()
            && (
                syllabus == null
                || !normalizeSemester(
                    syllabus.getSemester())
                    .equals(
                        normalizeSemester(
                            semester))
            )) {
        return false;
    }

    if (search == null
            || search.isBlank()) {
        return true;
    }

    String keyword =
            search.trim()
                    .toLowerCase(
                        Locale.ROOT);

    Course course =
            courseProgram.getCourse();

    String searchable =
            String.join(
                " ",
                safeText(
                    course == null
                        ? null
                        : course.getCourseCode()),
                safeText(
                    course == null
                        ? null
                        : course.getName()),
                safeText(
                    course == null
                        ? null
                        : course.getNameVn()),
                safeText(
                    syllabus == null
                        ? null
                        : syllabus.getVersionLabel()),
                syllabus == null
                    || syllabus.getVersionNumber() == null
                        ? ""
                        : SyllabusVersion.format(syllabus.getVersionNumber()),
                syllabus == null
                    || syllabus.getCreatedBy() == null
                        ? ""
                        : safeText(
                            syllabus
                                .getCreatedBy()
                                .getUsername()))
            .toLowerCase(
                Locale.ROOT);

    return searchable.contains(keyword);
}
        private String normalizeSemester(String value) {
                if (value == null)
                        return "";
                String normalized = value.trim().toLowerCase(Locale.ROOT);
                java.util.regex.Matcher matcher = java.util.regex.Pattern
                                .compile("(?:semester|hk)?\\s*([1-8])", java.util.regex.Pattern.CASE_INSENSITIVE)
                                .matcher(normalized);
                return matcher.matches() ? matcher.group(1) : normalized;
        }

        private String safeText(String value) {
                return value == null ? "" : value.trim();
        }

}
