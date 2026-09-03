package com.scse.curriculum.syllabus.service;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scse.curriculum.approval.entity.ApprovalRequest;
import com.scse.curriculum.approval.entity.ApprovalStep;
import com.scse.curriculum.auth.security.CurrentUserService;
import com.scse.curriculum.classsection.entity.ClassSection;
import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.common.exception.ForbiddenOperationException;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.instructor.entity.Instructor;
import com.scse.curriculum.instructor.repository.InstructorRepository;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.user.repository.UserAccountRepository;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.major.entity.Major;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SyllabusAccessService {

    private static final Pattern FIRST_NUMBER = Pattern.compile("(\\d+)");

    private final CurrentUserService currentUserService;
    private final InstructorRepository instructorRepository;
    private final ClassSectionRepository classSectionRepository;
    private final UserAccountRepository userAccountRepository;
    private final CourseProgramRepository courseProgramRepository;

    public record CreationAuthorization(
        UserAccount creator,
        Course course,
        String academicYear,
        String semester,
        List<ClassSection> assignments) {
    }

    public record CloneAuthorization(
        UserAccount creator,
        Course course,
        String academicYear,
        String semester,
        List<ClassSection> assignments) {
    }

    public UserAccount currentUser() {
        return currentUserService.getCurrentUser();
    }

    public boolean canView(Syllabus syllabus) {
        return canView(syllabus, currentUser());
    }

    public boolean canView(Syllabus syllabus, UserAccount user) {
        if (user == null || syllabus == null || syllabus.getCourse() == null) {
            return false;
        }

        if (user.getRole() == UserRole.INSTRUCTOR) {
            return hasFacultyAssignment(user, syllabus);
        }
        if (user.getRole() == UserRole.DEPT_HEAD) {
            try {
                return Objects.equals(
                        currentManagedMajorId(user),
                        resolveSyllabusMajor(syllabus).getId());
            } catch (ForbiddenOperationException exception) {
                return false;
            }
        }
        return true;
    }

    public void assertCanView(Syllabus syllabus) {
        if (!canView(syllabus)) {
            throw new ForbiddenOperationException(
                    "You are not authorized to view this syllabus. "
                            + "Instructors may only view assigned courses; "
                            + "Heads of Department may only view syllabuses in their managed Major.");
        }
    }

    /**
     * Approval/version history follows the same data scope as the syllabus.
     * Authentication alone is not sufficient: Faculty ownership and
     * department ownership must still be enforced.
     */
    public void assertCanViewHistory(Syllabus syllabus) {
        assertCanView(syllabus);
    }

    /** Allows the syllabus creation screen to read course/template context. */
    public void assertCanCreateContext(Course course) {
        UserAccount user = currentUser();
        if (user.getRole() == UserRole.INSTRUCTOR
                && !hasFacultyAssignment(user, course.getId())) {
            throw new ForbiddenOperationException(
                    "You may only create a syllabus for an assigned course.");
        }
    }

    public void assertCanModify(Syllabus syllabus) {
        UserAccount user = currentUser();
        if (user.getRole() == UserRole.DEPT_HEAD || user.getRole() == UserRole.DEAN) {
            throw new ForbiddenOperationException(
                    "Reviewers may view, approve, or reject a syllabus but cannot edit its content.");
        }
        if (user.getRole() == UserRole.INSTRUCTOR
                && !hasFacultyAssignment(user, syllabus)) {
            throw new ForbiddenOperationException(
                    "You may only modify a syllabus for an assigned course covered by your active teaching assignment.");
        }
    }

    private boolean hasFacultyAssignment(
            UserAccount user,
            Syllabus syllabus) {

        if (user.getInstructorId() == null
                || syllabus == null
                || syllabus.getCourse() == null) {
            return false;
        }

        if (syllabus.getId() != null
                && classSectionRepository
                        .existsByInstructor_IdAndSyllabus_IdAndIsActiveTrue(
                                user.getInstructorId(),
                                syllabus.getId())) {
            return true;
        }

        return hasFacultyAssignment(
                user,
                syllabus.getCourse().getId(),
                syllabus.getAcademicYear(),
                syllabus.getSemester());
    }

    /**
     * FR-03.1:
     * - Không tin createdBy từ client.
     * - Faculty phải có assignment ACTIVE khớp chính xác course + academicYear + semester.
     * - Assignment phải được tạo trước và chưa gắn syllabus.
     * - Admin vẫn được phép tạo dữ liệu phục vụ quản trị/migration.
     */
    public CreationAuthorization authorizeCreate(
        Integer classSectionId,
        Course requestedCourse,
        String requestedAcademicYear,
        String requestedSemesterText) {
        return authorizeCreate(classSectionId, requestedCourse, requestedAcademicYear,
                requestedSemesterText, null, null);
    }

    public CreationAuthorization authorizeCreate(
        Integer classSectionId,
        Course requestedCourse,
        String requestedAcademicYear,
        String requestedSemesterText,
        Integer requestedProgramId,
        Integer requestedCohortId) {

    UserAccount user = currentUser();

    /*
     * Admin được phép tạo dữ liệu không qua ClassSection.
     */
    if (classSectionId == null && user.getRole() != UserRole.INSTRUCTOR) {

        return new CreationAuthorization(
                user,
                requestedCourse,
                requireAcademicYear(requestedAcademicYear),
                requireSemesterText(requestedSemesterText),
                List.of());
    }

    /*
     * Faculty bắt buộc phải chọn một assignment cụ thể.
     */
    if (classSectionId == null) {
        throw new IllegalArgumentException(
                "Please select a teaching assignment "
                        + "before creating a syllabus.");
    }

    ClassSection assignment = classSectionRepository
            .findById(classSectionId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Teaching assignment not found."));
    
    assertAssignmentReadyForSyllabus(assignment);

    /*
     * Faculty chỉ được sử dụng assignment của chính mình.
     * Admin không bị giới hạn bởi instructor.
     */
    if (user.getRole() == UserRole.INSTRUCTOR) {
        if (user.getInstructorId() == null) {
            throw new ForbiddenOperationException(
                    "The account is not linked "
                            + "to an instructor profile.");
        }

        if (assignment.getInstructor() == null
                || !Objects.equals(
                        user.getInstructorId(),
                        assignment.getInstructor().getId())) {

            throw new ForbiddenOperationException(
                    "You are not assigned to "
                            + "the selected class section.");
        }
    }

    /*
     * Client vẫn gửi course/year/semester để hiển thị form,
     * nhưng backend kiểm tra chúng không bị sửa.
     */
    assertRequestMatchesAssignment(
            assignment,
            requestedCourse,
            requestedAcademicYear,
            requestedSemesterText);
    assertCurriculumScopeMatchesAssignment(
            assignment, requestedProgramId, requestedCohortId);

    return new CreationAuthorization(
            user,
            assignment.getCourse(),
            requireAcademicYear(requestedAcademicYear),
            requireSemesterText(requestedSemesterText),
            List.of(assignment));
}

    /**
     * Authorizes the direct import pipeline. Instructor context is derived from
     * the selected active assignment, never from client-supplied user data.
     */
    public CreationAuthorization authorizeImport(
            Integer classSectionId,
            Course requestedCourse) {
        return authorizeImport(classSectionId, requestedCourse, null, null);
    }

    public CreationAuthorization authorizeImport(
            Integer classSectionId,
            Course requestedCourse,
            Integer requestedProgramId,
            Integer requestedCohortId) {

        UserAccount user = currentUser();
        if (user.getRole() != UserRole.INSTRUCTOR) {
            return new CreationAuthorization(user, requestedCourse, null, null, List.of());
        }

        if (classSectionId == null) {
            throw new ForbiddenOperationException(
                    "You are not assigned to this course and cannot create or import its syllabus.");
        }

        ClassSection assignment = classSectionRepository.findById(classSectionId)
                .orElseThrow(() -> new ForbiddenOperationException(
                        "You are not assigned to this course and cannot create or import its syllabus."));

        if (user.getInstructorId() == null
                || assignment.getInstructor() == null
                || !Objects.equals(user.getInstructorId(), assignment.getInstructor().getId())
                || requestedCourse == null
                || assignment.getCourse() == null
                || !Objects.equals(requestedCourse.getId(), assignment.getCourse().getId())) {
            throw new ForbiddenOperationException(
                    "You are not assigned to this course and cannot create or import its syllabus.");
        }
        assertAssignmentReadyForSyllabus(assignment);
        assertCurriculumScopeMatchesAssignment(
                assignment, requestedProgramId, requestedCohortId);

        return new CreationAuthorization(
                user,
                assignment.getCourse(),
                requireAcademicYear(assignment.getAcademicYear()),
                canonicalSemester(assignment.getSemester()),
                List.of(assignment));
    }

    private void assertCurriculumScopeMatchesAssignment(
            ClassSection assignment,
            Integer requestedProgramId,
            Integer requestedCohortId) {
        if (requestedProgramId != null
                && (assignment.getProgram() == null
                || !Objects.equals(requestedProgramId, assignment.getProgram().getId()))) {
            throw new ForbiddenOperationException("The selected program does not match this teaching assignment.");
        }
        if (requestedCohortId != null
                && (assignment.getCohort() == null
                || !Objects.equals(requestedCohortId, assignment.getCohort().getId()))) {
            throw new ForbiddenOperationException("The selected cohort does not match this teaching assignment.");
        }
    }

    /** Kiểm tra lại assignment khi update thay đổi course/năm học/học kỳ. */
    public void assertCanUseAssignmentFor(
            Course course,
            String academicYear,
            String semesterText) {

        UserAccount user = currentUser();
        if (user.getRole() == UserRole.INSTRUCTOR
                && !hasFacultyAssignment(user, course.getId(), academicYear, semesterText)) {
            throw new ForbiddenOperationException(
                    "The course, academic year, and semester are not covered by your active teaching assignment.");
        }
    }

    /**
     * FR-03.2: xác thực học kỳ đích khi clone syllabus.
     *
     * Faculty được clone một version cũ của đúng môn học vào assignment mới
     * mà mình đang phụ trách. Không yêu cầu Faculty phải còn assignment ở
     * học kỳ nguồn, vì đây chính là use case "clone từ học kỳ trước".
     */
    public CloneAuthorization authorizeClone(
            Syllabus source,
            Integer classSectionId,
            String requestedAcademicYear,
            String requestedSemesterText) {

        if (source == null || source.getCourse() == null) {
            throw new IllegalArgumentException(
                    "The source syllabus is invalid.");
        }

        UserAccount user = currentUser();

        /*
         * Admin có thể clone dữ liệu migration không cần ClassSection,
         * nhưng phải chỉ rõ năm học và học kỳ đích.
         */
        if (classSectionId == null && user.getRole() != UserRole.INSTRUCTOR) {

            return new CloneAuthorization(
                    user,
                    source.getCourse(),
                    requireAcademicYear(requestedAcademicYear),
                    requireSemesterText(requestedSemesterText),
                    List.of());
        }

        if (classSectionId == null) {
            throw new IllegalArgumentException(
                    "Please select the target-semester assignment.");
        }

        ClassSection assignment = classSectionRepository
                .findById(classSectionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Target-semester assignment not found."));

        assertAssignmentReadyForSyllabus(assignment);

        if (!Objects.equals(
                assignment.getCourse().getId(),
                source.getCourse().getId())) {
            throw new IllegalArgumentException(
                    "A syllabus may only be cloned to an assignment for the same course.");
        }

        if (user.getRole() == UserRole.INSTRUCTOR) {
            if (user.getInstructorId() == null) {
                throw new ForbiddenOperationException(
                        "The account is not linked to an instructor profile.");
            }

            if (assignment.getInstructor() == null
                    || !Objects.equals(
                            user.getInstructorId(),
                            assignment.getInstructor().getId())) {
                throw new ForbiddenOperationException(
                        "You are not assigned to the selected target semester.");
            }

            /*
             * Không cho lấy Draft đang làm dở của người khác.
             * Các version đã submit/review/approve/reject/archive của cùng môn
             * có thể dùng làm nguồn cho assignment mới.
             */
            if (source.getStatus() == com.scse.curriculum.syllabus.entity.SyllabusStatus.DRAFT
                    && source.getCreatedBy() != null
                    && !Objects.equals(
                            source.getCreatedBy().getId(),
                            user.getId())) {
                throw new ForbiddenOperationException(
                        "You cannot clone another instructor's Draft.");
            }
        }

        return new CloneAuthorization(
                user,
                assignment.getCourse(),
                requireAcademicYear(requestedAcademicYear),
                requireSemesterText(requestedSemesterText),
                List.of(assignment));
    }

    public void assertCanClone(Syllabus source) {
        assertCanModify(source);
    }

    public void assertCanReview(ApprovalRequest approval) {
        UserAccount user = currentUser();
        rejectInstructorReviewerAction();
        if (approval == null || approval.getSyllabus() == null) {
            throw new ForbiddenOperationException("The approval request is invalid.");
        }
        if (approval.getStep() == ApprovalStep.STEP3_DEAN
                && user.getRole() != UserRole.DEAN) {
            throw new ForbiddenOperationException(
                    "Only the Dean may perform final syllabus approval.");
        }
        if (user.getRole() == UserRole.DEPT_HEAD) {
            if (approval.getStep() != ApprovalStep.STEP1_DEPT_HEAD) {
                throw new ForbiddenOperationException(
                        "Heads of Department cannot perform final Dean approval.");
            }
            assertCanView(approval.getSyllabus());
        } else if (user.getRole() == UserRole.DEAN
                && approval.getStep() != ApprovalStep.STEP3_DEAN) {
            throw new ForbiddenOperationException(
                    "The selected request is not awaiting Dean review.");
        }
    }

    public void assertCanAccessApproval(ApprovalRequest approval) {
        UserAccount user = currentUser();
        if (user.getRole() == UserRole.INSTRUCTOR || user.getRole() == UserRole.DEPT_HEAD) {
            if (approval == null || approval.getSyllabus() == null) {
                throw new ForbiddenOperationException("The approval request is invalid.");
            }
            assertCanView(approval.getSyllabus());
        }
    }

    public void assertCanOpenPendingStep(ApprovalStep step) {
        UserAccount user = currentUser();
        rejectInstructorReviewerAction();
        if (step == ApprovalStep.STEP3_DEAN
                && user.getRole() != UserRole.DEAN) {
            throw new ForbiddenOperationException(
                    "Only the Dean may open the final approval queue.");
        }
        if (user.getRole() == UserRole.DEPT_HEAD && step != ApprovalStep.STEP1_DEPT_HEAD) {
            throw new ForbiddenOperationException(
                    "Heads of Department may open only the Department review queue.");
        }
        if (user.getRole() == UserRole.DEAN && step != ApprovalStep.STEP3_DEAN) {
            throw new ForbiddenOperationException(
                    "The Dean may open only the Dean review queue.");
        }
    }

    private void rejectInstructorReviewerAction() {
        if (currentUser().getRole() == UserRole.INSTRUCTOR) {
            throw new ForbiddenOperationException(
                    "Instructors cannot approve or reject syllabus review steps.");
        }
    }

    public Integer currentDepartmentId(UserAccount user) {
        if (user.getInstructorId() == null) {
            throw new ForbiddenOperationException(
                    "The account is not linked to an instructor/department profile.");
        }

        Instructor instructor = instructorRepository
                .findById(user.getInstructorId())
                .orElseThrow(() -> new ForbiddenOperationException(
                        "No instructor profile was found for this account."));

        if (instructor.getDepartment() == null) {
            throw new ForbiddenOperationException(
                    "The instructor profile is not assigned to a department.");
        }

        return instructor.getDepartment().getId();
    }

    public Integer currentManagedMajorId(UserAccount user) {
        if (user == null || user.getRole() != UserRole.DEPT_HEAD
                || user.getManagedMajor() == null) {
            throw new ForbiddenOperationException(
                    "The Head of Department account is not assigned to a Managed Major.");
        }
        return user.getManagedMajor().getId();
    }

    public List<UserAccount> findActiveDeptHeadsFor(Syllabus syllabus) {
        Major major = resolveSyllabusMajor(syllabus);

        List<UserAccount> deptHeads = userAccountRepository
                .findByRoleAndManagedMajor_IdAndIsActiveTrue(
                        UserRole.DEPT_HEAD, major.getId());

        if (deptHeads.isEmpty()) {
            throw new IllegalStateException(
                    "No active Head of Department is assigned to the "
                            + major.getCode() + " program. Please contact the administrator.");
        }

        return deptHeads;
    }

    public Major resolveSyllabusMajor(Syllabus syllabus) {
        if (syllabus == null || syllabus.getId() == null) {
            throw new ForbiddenOperationException("The syllabus has no Program/Major context.");
        }
        Major fromProgram = courseProgramRepository.findBySyllabus_Id(syllabus.getId()).stream()
                .filter(cp -> cp.getProgram() != null && cp.getProgram().getMajor() != null)
                .map(cp -> cp.getProgram().getMajor()).findFirst().orElse(null);
        if (fromProgram != null) return fromProgram;
        return classSectionRepository.findBySyllabusId(syllabus.getId()).stream()
                .filter(cs -> cs.getProgram() != null && cs.getProgram().getMajor() != null)
                .map(cs -> cs.getProgram().getMajor()).findFirst()
                .orElseThrow(() -> new ForbiddenOperationException(
                        "The syllabus is not linked to a Teaching Assignment Program/Major."));
    }

    public boolean hasFacultyAssignment(
            UserAccount user,
            Integer courseId,
            String academicYear,
            String semesterText) {

        if (user == null || user.getInstructorId() == null || courseId == null) {
            return false;
        }
        Integer semester = parseSemester(semesterText);
        if (academicYear == null || academicYear.isBlank() || semester == null) {
            return false;
        }
        return !classSectionRepository.findActiveAssignmentsExact(
                user.getInstructorId(), courseId, academicYear.trim(), semester).isEmpty();
    }

    private boolean hasFacultyAssignment(
            UserAccount user,
            Integer courseId) {

        if (user.getInstructorId() == null || courseId == null) {
            return false;
        }

        return classSectionRepository.findActiveByInstructorId(
                        user.getInstructorId())
                .stream()
                .anyMatch(section -> section.getCourse() != null
                        && Objects.equals(section.getCourse().getId(), courseId));
    }
private void assertAssignmentReadyForSyllabus(
        ClassSection assignment) {

    if (!Boolean.TRUE.equals(assignment.getIsActive())) {
        throw new IllegalStateException(
                "The teaching assignment is inactive, so "
                        + "the syllabus cannot be created.");
    }

    if (assignment.getCourse() == null) {
        throw new IllegalStateException(
                "The teaching assignment is not linked to a course.");
    }

    if (assignment.getInstructor() == null) {
        throw new IllegalStateException(
                "The teaching assignment is not linked to an instructor.");
    }

    if (assignment.getSyllabus() != null) {
        throw new IllegalStateException(
                "This teaching assignment already has a syllabus. "
                        + "Open the existing syllabus to edit it.");
    }
}

private void assertOptionalTargetMatchesAssignment(
        ClassSection assignment,
        String requestedAcademicYear,
        String requestedSemesterText) {

    if (requestedAcademicYear != null
            && !requestedAcademicYear.isBlank()
            && !assignment.getAcademicYear().trim()
                    .equalsIgnoreCase(requestedAcademicYear.trim())) {
        throw new IllegalArgumentException(
                "The target academic year does not match the selected assignment.");
    }

    if (requestedSemesterText != null
            && !requestedSemesterText.isBlank()) {
        Integer requestedSemester =
                requireSemester(requestedSemesterText);

        if (!Objects.equals(
                assignment.getSemester(),
                requestedSemester)) {
            throw new IllegalArgumentException(
                    "The target semester does not match the selected assignment.");
        }
    }
}

private void assertRequestMatchesAssignment(
        ClassSection assignment,
        Course requestedCourse,
        String requestedAcademicYear,
        String requestedSemesterText) {

    if (requestedCourse == null
            || !Objects.equals(
                    assignment.getCourse().getId(),
                    requestedCourse.getId())) {

        throw new IllegalArgumentException(
                "The course in the form does not match "
                        + "the selected teaching assignment.");
    }

    requireAcademicYear(requestedAcademicYear);
    requireSemester(requestedSemesterText);
}

private String canonicalSemester(Integer semester) {
    if (semester == null) {
        throw new IllegalArgumentException(
                "The assignment semester is invalid.");
    }

    return "HK" + semester;
}

private String requireSemesterText(String value) {
    if (value == null || value.isBlank()) {
        throw new IllegalArgumentException(
                "Please select a semester.");
    }

    return value.trim();
}
    private Integer courseDepartmentId(Course course) {
        if (course == null || course.getDepartment() == null) {
            throw new ForbiddenOperationException(
                    "The course is not assigned to a department, so authorization cannot be evaluated.");
        }
        return course.getDepartment().getId();
    }

    private String requireAcademicYear(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Select the academic year / applicable cohort that matches the teaching assignment.");
        }
        return value.trim();
    }

    private Integer requireSemester(String value) {
        Integer semester = parseSemester(value);
        if (semester == null) {
            throw new IllegalArgumentException(
                    "Select the semester that matches the teaching assignment.");
        }
        return semester;
    }

    private Integer parseSemester(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        Matcher matcher = FIRST_NUMBER.matcher(value);
        if (!matcher.find()) {
            return null;
        }

        return Integer.valueOf(matcher.group(1));
    }
}
