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

       return switch (user.getRole()) {
    case ADMIN, DEAN -> true;

    case INSTRUCTOR -> hasFacultyAssignment(
            user,
            syllabus.getCourse().getId(),
            syllabus.getAcademicYear(),
            syllabus.getSemester());

    case DEPT_HEAD -> Objects.equals(
            currentDepartmentId(user),
            courseDepartmentId(syllabus.getCourse()));
};
    }

    public void assertCanView(Syllabus syllabus) {
        if (!canView(syllabus)) {
            throw new ForbiddenOperationException(
                    "You are not authorized to view this syllabus. "
                            + "Instructors may only view assigned courses; "
                            + "Heads of Department may only view courses in their own department.");
        }
    }

    /** Approval/version history is readable by every authenticated syllabus role. */
    public void assertCanViewHistory(Syllabus syllabus) {
        if (currentUser() == null || syllabus == null) {
            throw new ForbiddenOperationException(
                    "You are not authorized to view syllabus history.");
        }
    }

    /** Allows the syllabus creation screen to read course/template context. */
    public void assertCanCreateContext(Course course) {
        UserAccount user = currentUser();
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.INSTRUCTOR) {
            return;
        }
        throw new ForbiddenOperationException(
                "Only an Instructor or Admin may create a syllabus.");
    }

    public void assertCanModify(Syllabus syllabus) {
        UserAccount user = currentUser();

        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        if (user.getRole() != UserRole.INSTRUCTOR
                || !hasFacultyAssignment(
                        user,
                        syllabus.getCourse().getId(),
                        syllabus.getAcademicYear(),
                        syllabus.getSemester())) {
            throw new ForbiddenOperationException(
                    "Only the instructor assigned to this course or an Admin "
                            + "may edit this syllabus.");
        }
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

    UserAccount user = currentUser();

    /*
     * Admin được phép tạo dữ liệu không qua ClassSection.
     */
    if (user.getRole() == UserRole.ADMIN
            && classSectionId == null) {

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
    if (user.getRole() != UserRole.ADMIN
            && user.getRole() != UserRole.INSTRUCTOR) {

        throw new ForbiddenOperationException(
                "Only an assigned instructor or Admin "
                        + "may create a syllabus.");
    }

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

    return new CreationAuthorization(
            user,
            assignment.getCourse(),
            assignment.getAcademicYear().trim(),
            canonicalSemester(assignment.getSemester()),
            List.of(assignment));
}

    /** Kiểm tra lại assignment khi update thay đổi course/năm học/học kỳ. */
    public void assertCanUseAssignmentFor(
            Course course,
            String academicYear,
            String semesterText) {

        UserAccount user = currentUser();
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        if (user.getRole() != UserRole.INSTRUCTOR
                || !hasFacultyAssignment(
                        user,
                        course.getId(),
                        academicYear,
                        semesterText)) {
            throw new ForbiddenOperationException(
                    "You do not have an ACTIVE assignment matching the selected course, academic year, and semester.");
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
        if (user.getRole() == UserRole.ADMIN
                && classSectionId == null) {

            return new CloneAuthorization(
                    user,
                    source.getCourse(),
                    requireAcademicYear(requestedAcademicYear),
                    requireSemesterText(requestedSemesterText),
                    List.of());
        }

        if (user.getRole() != UserRole.ADMIN
                && user.getRole() != UserRole.INSTRUCTOR) {
            throw new ForbiddenOperationException(
                    "Only an assigned instructor or Admin "
                            + "may clone a syllabus.");
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

        assertOptionalTargetMatchesAssignment(
                assignment,
                requestedAcademicYear,
                requestedSemesterText);

        return new CloneAuthorization(
                user,
                assignment.getCourse(),
                assignment.getAcademicYear().trim(),
                canonicalSemester(assignment.getSemester()),
                List.of(assignment));
    }

    public void assertCanClone(Syllabus source) {
        assertCanModify(source);
    }

    public void assertCanReview(ApprovalRequest approval) {
        UserAccount reviewer = currentUser();

        if (reviewer.getRole() == UserRole.ADMIN) {
            return;
        }

        if (approval.getStep() == ApprovalStep.STEP1_DEPT_HEAD) {
            if (reviewer.getRole() != UserRole.DEPT_HEAD
                    || !Objects.equals(
                            currentDepartmentId(reviewer),
                            courseDepartmentId(approval.getSyllabus().getCourse()))) {
                throw new ForbiddenOperationException(
                        "You may only review syllabi from the department you manage.");
            }
            return;
        }

        if (approval.getStep() == ApprovalStep.STEP3_DEAN) {
            if (reviewer.getRole() != UserRole.DEAN) {
                throw new ForbiddenOperationException(
                        "Only the Dean may process this approval step.");
            }
            return;
        }

        throw new ForbiddenOperationException(
                "You are not authorized to process this approval step.");
    }

    public void assertCanAccessApproval(ApprovalRequest approval) {
        UserAccount user = currentUser();

        if (user.getRole() == UserRole.ADMIN
                || user.getRole() == UserRole.DEAN) {
            return;
        }

        if (user.getRole() == UserRole.DEPT_HEAD
                && Objects.equals(
                        currentDepartmentId(user),
                        courseDepartmentId(approval.getSyllabus().getCourse()))) {
            return;
        }

        if (user.getRole() == UserRole.INSTRUCTOR
                && canView(approval.getSyllabus(), user)) {
            return;
        }

        throw new ForbiddenOperationException(
                "You are not authorized to view this approval request.");
    }

    public void assertCanOpenPendingStep(ApprovalStep step) {
        UserAccount user = currentUser();

        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        if (step == ApprovalStep.STEP1_DEPT_HEAD
                && user.getRole() == UserRole.DEPT_HEAD) {
            return;
        }

        if (step == ApprovalStep.STEP3_DEAN
                && user.getRole() == UserRole.DEAN) {
            return;
        }

        throw new ForbiddenOperationException(
                "Your role is not authorized to view the queue for this step.");
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

    public List<UserAccount> findActiveDeptHeadsFor(Course course) {
        Integer departmentId = courseDepartmentId(course);

        List<UserAccount> deptHeads = userAccountRepository
                .findActiveByRoleAndInstructorDepartmentId(
                        UserRole.DEPT_HEAD,
                        departmentId);

        if (deptHeads.isEmpty()) {
            throw new IllegalStateException(
                    "The course department does not have an active Head of Department account.");
        }

        return deptHeads;
    }

    public boolean hasFacultyAssignment(
            UserAccount user,
            Integer courseId,
            String academicYear,
            String semesterText) {

        if (user.getInstructorId() == null
                || academicYear == null
                || academicYear.isBlank()) {
            return false;
        }

        Integer semester = parseSemester(semesterText);
        if (semester == null) {
            return false;
        }

        return !classSectionRepository.findActiveAssignmentsExact(
                user.getInstructorId(),
                courseId,
                academicYear.trim(),
                semester).isEmpty();
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

    if (assignment.getAcademicYear() == null
            || assignment.getAcademicYear().isBlank()) {

        throw new IllegalStateException(
                "The teaching assignment does not have a valid academic year.");
    }

    if (assignment.getSemester() == null) {
        throw new IllegalStateException(
                "The teaching assignment does not have a valid semester.");
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

    String normalizedAcademicYear =
            requireAcademicYear(requestedAcademicYear);

    if (!assignment.getAcademicYear()
            .trim()
            .equalsIgnoreCase(normalizedAcademicYear)) {

        throw new IllegalArgumentException(
                "The academic year in the form does not match "
                        + "the selected teaching assignment.");
    }

    Integer requestedSemester =
            requireSemester(requestedSemesterText);

    if (!Objects.equals(
            assignment.getSemester(),
            requestedSemester)) {

        throw new IllegalArgumentException(
                "The semester in the form does not match "
                        + "the selected teaching assignment.");
    }
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
