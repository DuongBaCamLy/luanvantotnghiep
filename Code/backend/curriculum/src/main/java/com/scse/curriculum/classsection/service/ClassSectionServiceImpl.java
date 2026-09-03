package com.scse.curriculum.classsection.service;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scse.curriculum.auth.security.CurrentUserService;
import com.scse.curriculum.classsection.dto.ClassSectionResponse;
import com.scse.curriculum.classsection.dto.CreateClassSectionRequest;
import com.scse.curriculum.classsection.entity.ClassSection;
import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.common.exception.ForbiddenOperationException;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.course.repository.CourseRepository;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.program.repository.ProgramRepository;
import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.enrollment.repository.EnrollmentRepository;
import com.scse.curriculum.instructor.entity.Instructor;
import com.scse.curriculum.instructor.repository.InstructorRepository;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassSectionServiceImpl
        implements ClassSectionService {

    private static final Pattern FIRST_NUMBER =
            Pattern.compile("(\\d+)");

    private final ClassSectionRepository repository;
    private final CourseRepository courseRepository;
    private final ProgramRepository programRepository;
    private final CohortRepository cohortRepository;
    private final CourseProgramRepository courseProgramRepository;
    private final SyllabusRepository syllabusRepository;
    private final InstructorRepository instructorRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CurrentUserService currentUserService;

    /*
     * =====================================================
     * CREATE
     * =====================================================
     */

    @Override
    public ClassSectionResponse create(
            CreateClassSectionRequest request) {

        UserAccount actor = requireManagementUser();

        Course course = requireCourse(request.getCourseId());
        Program program = requireProgram(request.getProgramId());
        Cohort cohort = requireCohort(request.getCohortId());
        assertCurriculumContext(course, program, cohort);
        Instructor instructor =
                requireInstructor(request.getInstructorId());

        /*
         * Kiểm tra phạm vi bộ môn ở tầng service.
         * Không chỉ phụ thuộc vào @PreAuthorize ở controller.
         */
        assertProgramInManagementScope(actor, program);
        assertInstructorInManagementScope(actor, instructor);

        Syllabus syllabus = resolveOptionalSyllabus(
                request.getSyllabusId(),
                course,
                request);

        assertNoDuplicate(null, request);

        ClassSection section = ClassSection.builder()
                .course(course)
                .program(program)
                .cohort(cohort)
                .syllabus(syllabus)
                .instructor(instructor)
                .semester(request.getSemester() == null ? 1 : request.getSemester())
                .academicYear(defaultAcademicYear(request.getAcademicYear()))
                .groupNumber(request.getGroupNumber() == null ? 1 : request.getGroupNumber())
                .labGroup(request.getLabGroup())
                .maxStudents(
                        request.getMaxStudents() == null
                                ? 50
                                : request.getMaxStudents())
                .room(normalizeOptionalText(request.getRoom()))
                .schedule(
                        normalizeOptionalText(request.getSchedule()))
                .sectionType(request.getSectionType() == null
                        ? com.scse.curriculum.classsection.entity.SectionType.THEORY
                        : request.getSectionType())
                .isActive(
                        request.getIsActive() == null
                                || request.getIsActive())
                .build();

        ClassSection saved = repository.save(section);

        return map(saved);
    }

    /*
     * =====================================================
     * UPDATE
     * =====================================================
     */

    @Override
    public ClassSectionResponse update(
            Integer id,
            CreateClassSectionRequest request) {

        UserAccount actor = requireManagementUser();

        /*
         * Phải lấy ClassSection bằng scoped query.
         *
         * Nếu DeptHead cố sửa ID thuộc bộ môn khác,
         * query không trả dữ liệu và API trả về 404.
         */
        ClassSection section =
                requireAccessibleSection(id, actor);

        Course course = requireCourse(request.getCourseId());
        Program program = requireProgram(request.getProgramId());
        Cohort cohort = requireCohort(request.getCohortId());
        assertCurriculumContext(course, program, cohort);
        Instructor instructor =
                requireInstructor(request.getInstructorId());

        assertProgramInManagementScope(actor, program);
        assertInstructorInManagementScope(actor, instructor);

        Syllabus syllabus = resolveOptionalSyllabus(
                request.getSyllabusId(),
                course,
                request);

        assertNoDuplicate(id, request);

        section.setCourse(course);
        section.setProgram(program);
        section.setCohort(cohort);
        section.setSyllabus(syllabus);
        section.setInstructor(instructor);
        // Course and Instructor are the assignment source of truth. Legacy
        // ClassSection metadata is preserved on edit, not re-entered by Admin.
        if (request.getSemester() != null) section.setSemester(request.getSemester());
        if (request.getAcademicYear() != null && !request.getAcademicYear().isBlank()) {
            section.setAcademicYear(request.getAcademicYear().trim());
        }
        if (request.getGroupNumber() != null) section.setGroupNumber(request.getGroupNumber());
        section.setLabGroup(request.getLabGroup());
        section.setMaxStudents(
                request.getMaxStudents() == null
                        ? 50
                        : request.getMaxStudents());
        section.setRoom(
                normalizeOptionalText(request.getRoom()));
        section.setSchedule(
                normalizeOptionalText(request.getSchedule()));
        if (request.getSectionType() != null) section.setSectionType(request.getSectionType());
        if (request.getIsActive() != null) section.setIsActive(request.getIsActive());

        ClassSection saved = repository.save(section);

        return map(saved);
    }

    /*
     * =====================================================
     * DELETE
     * =====================================================
     */

@Override
public void delete(Integer id) {
    UserAccount actor = requireManagementUser();

    ClassSection section =
            requireAccessibleSection(id, actor);

    if (section.getSyllabus() != null) {
        throw new IllegalStateException(
                "Không thể xóa phân công đã gắn đề cương. "
                        + "Hãy chuyển phân công sang trạng thái "
                        + "không hoạt động.");
    }

    if (enrollmentRepository.existsByClassSection_Id(id)) {
        throw new IllegalStateException(
                "Không thể xóa phân công đã có sinh viên đăng ký. "
                        + "Hãy chuyển phân công sang trạng thái "
                        + "không hoạt động.");
    }

    repository.delete(section);
}
    /*
     * =====================================================
     * GET ALL
     * =====================================================
     */

    @Override
    @Transactional(readOnly = true)
    public List<ClassSectionResponse> getAll() {
        UserAccount actor = requireManagementUser();

        Integer departmentId =
                resolveDepartmentScope(actor);

        return repository
                .findAllInScope(departmentId)
                .stream()
                .map(this::map)
                .toList();
    }

    /*
     * =====================================================
     * GET BY ID
     * =====================================================
     */

    @Override
    @Transactional(readOnly = true)
    public ClassSectionResponse getById(Integer id) {
        UserAccount actor = requireManagementUser();

        return map(requireAccessibleSection(id, actor));
    }

    /*
     * =====================================================
     * GET BY COURSE
     * =====================================================
     */

    @Override
    @Transactional(readOnly = true)
    public List<ClassSectionResponse> getByCourseId(
            Integer courseId) {

        UserAccount actor = requireManagementUser();

        Integer departmentId =
                resolveDepartmentScope(actor);

        return repository
                .findByCourseIdInScope(
                        courseId,
                        departmentId)
                .stream()
                .map(this::map)
                .toList();
    }

    /*
     * =====================================================
     * SEARCH
     * =====================================================
     */

    @Override
    @Transactional(readOnly = true)
    public List<ClassSectionResponse> search(
            String keyword) {

        UserAccount actor = requireManagementUser();

        Integer departmentId =
                resolveDepartmentScope(actor);

        String normalizedKeyword =
                keyword == null ? "" : keyword.trim();

        return repository
                .searchInScope(
                        normalizedKeyword,
                        departmentId)
                .stream()
                .map(this::map)
                .toList();
    }

    /*
     * =====================================================
     * FACULTY ASSIGNMENTS
     * =====================================================
     */

    @Override
    @Transactional(readOnly = true)
    public List<ClassSectionResponse>
    getMyActiveAssignments() {

        UserAccount currentUser =
                currentUserService.getCurrentUser();

        /*
         * Admin vẫn được xem toàn bộ assignment đang active.
         */
        if (currentUser.getRole() != UserRole.INSTRUCTOR) {
            return repository
                    .findAllInScope(null)
                    .stream()
                    .filter(section ->
                            Boolean.TRUE.equals(
                                    section.getIsActive()))
                    .map(this::map)
                    .toList();
        }

        if (currentUser.getRole() != UserRole.INSTRUCTOR
                || currentUser.getInstructorId() == null) {

            throw new ForbiddenOperationException(
                    "Chỉ giảng viên được xem danh sách "
                            + "phân công của chính mình.");
        }

        return repository
                .findActiveByInstructorId(
                        currentUser.getInstructorId())
                .stream()
                .map(this::map)
                .toList();
    }

    /*
     * =====================================================
     * AUTHORIZATION HELPERS
     * =====================================================
     */

    /**
     * Controller đã có @PreAuthorize nhưng service vẫn phải
     * kiểm tra lại để phòng trường hợp method được gọi từ
     * service khác hoặc future endpoint.
     */
    private UserAccount requireManagementUser() {
        UserAccount actor =
                currentUserService.getCurrentUser();

        if (actor.getRole() == UserRole.INSTRUCTOR) {
            throw new ForbiddenOperationException(
                    "Instructors may view only their own teaching assignments and cannot manage assignments.");
        }
        return actor;
    }

    /**
     * Admin returns null (all Majors). A Head account returns the Major
     * configured in User Management; instructor Department is not a role scope.
     */
    private Integer resolveDepartmentScope(
            UserAccount actor) {

        if (actor.getRole() != UserRole.DEPT_HEAD) {
            return null;
        }
        if (actor.getManagedMajor() == null || actor.getManagedMajor().getId() == null) {
            throw new ForbiddenOperationException(
                    "The Head of Department account is not assigned to a Managed Major.");
        }
        return actor.getManagedMajor().getId();
    }

    private Instructor requireCurrentUserInstructor(
            UserAccount actor) {

        if (actor.getInstructorId() == null) {
            throw new ForbiddenOperationException(
                    "Tài khoản Trưởng bộ môn chưa liên kết "
                            + "với hồ sơ giảng viên.");
        }

        Instructor instructor =
                instructorRepository
                        .findById(actor.getInstructorId())
                        .orElseThrow(() ->
                                new ForbiddenOperationException(
                                        "Không tìm thấy hồ sơ giảng viên "
                                                + "của tài khoản hiện tại."));

        if (!Boolean.TRUE.equals(instructor.getIsActive())) {
            throw new ForbiddenOperationException(
                    "Hồ sơ giảng viên của tài khoản "
                            + "hiện tại không hoạt động.");
        }

        return instructor;
    }

    private void assertProgramInManagementScope(
            UserAccount actor,
            Program program) {
        if (actor.getRole() != UserRole.DEPT_HEAD) return;

        Integer managedMajorId =
                resolveDepartmentScope(actor);

        if (program.getMajor() == null || program.getMajor().getId() == null) {

            throw new ForbiddenOperationException(
                    "The selected Program is not assigned to a Major.");
        }

        if (!Objects.equals(
                managedMajorId,
                program.getMajor().getId())) {

            throw new ForbiddenOperationException(
                    "You can only manage Teaching Assignments in your Managed Major.");
        }
    }

    private void assertInstructorInManagementScope(
            UserAccount actor,
            Instructor instructor) {
        if (!Boolean.TRUE.equals(instructor.getIsActive())) {
            throw new IllegalStateException(
                    "Không thể phân công giảng viên "
                            + "đang ở trạng thái không hoạt động.");
        }
    }

    /**
     * Truy vấn ClassSection theo đúng data scope.
     *
     * Admin:
     * - departmentId = null.
     *
     * DeptHead:
     * - departmentId = bộ môn của tài khoản hiện tại.
     */
    private ClassSection requireAccessibleSection(
            Integer id,
            UserAccount actor) {

        Integer departmentId =
                resolveDepartmentScope(actor);

        return repository
                .findByIdInScope(id, departmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Class section not found"));
    }

    /*
     * =====================================================
     * ENTITY HELPERS
     * =====================================================
     */

    private Course requireCourse(Integer courseId) {
        return courseRepository
                .findById(courseId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Course not found"));
    }

    private Program requireProgram(Integer programId) {
        return programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));
    }

    private Cohort requireCohort(Integer cohortId) {
        return cohortRepository.findById(cohortId)
                .orElseThrow(() -> new ResourceNotFoundException("Cohort not found"));
    }

    private void assertCurriculumContext(Course course, Program program, Cohort cohort) {
        if (cohort.getProgram() == null
                || !Objects.equals(cohort.getProgram().getId(), program.getId())) {
            throw new IllegalArgumentException("The selected cohort does not belong to the selected program.");
        }
        boolean courseInCurriculum = courseProgramRepository
                .findEffectiveByProgramIdAndCohortIdWithRelations(program.getId(), cohort.getId())
                .stream()
                .anyMatch(item -> item.getCourse() != null
                        && Objects.equals(item.getCourse().getId(), course.getId()));
        if (!courseInCurriculum) {
            throw new IllegalArgumentException("The selected course does not belong to the selected program and cohort curriculum.");
        }
    }

    private Instructor requireInstructor(
            Integer instructorId) {

        return instructorRepository
                .findById(instructorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Instructor not found"));
    }

    /*
     * =====================================================
     * SYLLABUS VALIDATION
     * =====================================================
     */

    private Syllabus resolveOptionalSyllabus(
            Integer syllabusId,
            Course course,
            CreateClassSectionRequest request) {

        if (syllabusId == null) {
            return null;
        }

        Syllabus syllabus =
                syllabusRepository
                        .findById(syllabusId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Syllabus not found"));

        if (syllabus.getCourse() == null
                || !Objects.equals(
                        syllabus.getCourse().getId(),
                        course.getId())) {

            throw new IllegalArgumentException(
                    "Đề cương được chọn không thuộc "
                            + "môn học của phân công.");
        }

        String requestedAcademicYear =
                normalizeAcademicYear(
                        request.getAcademicYear());

        if (syllabus.getAcademicYear() != null
                && !syllabus.getAcademicYear().isBlank()
                && !syllabus
                        .getAcademicYear()
                        .trim()
                        .equalsIgnoreCase(
                                requestedAcademicYear)) {

            throw new IllegalArgumentException(
                    "Năm học của đề cương không khớp "
                            + "với năm học phân công.");
        }

        Integer syllabusSemester =
                parseSemester(syllabus.getSemester());

        if (syllabusSemester != null
                && !Objects.equals(
                        syllabusSemester,
                        request.getSemester())) {

            throw new IllegalArgumentException(
                    "Học kỳ của đề cương không khớp "
                            + "với học kỳ phân công.");
        }

        return syllabus;
    }

    /*
     * =====================================================
     * DUPLICATE VALIDATION
     * =====================================================
     */

    private void assertNoDuplicate(
            Integer currentId,
            CreateClassSectionRequest request) {

        long duplicateCount = repository.findByCourse_Id(request.getCourseId())
                .stream()
                .filter(section -> !Objects.equals(section.getId(), currentId))
                .filter(section -> section.getInstructor() != null
                        && Objects.equals(section.getInstructor().getId(), request.getInstructorId()))
                .count();

        if (duplicateCount > 0) {
            throw new IllegalStateException(
                    "Giảng viên này đã được phân công cho môn học.");
        }
    }

    private String defaultAcademicYear(String value) {
        return value == null || value.isBlank() ? "COURSE_ASSIGNMENT" : value.trim();
    }

    /*
     * =====================================================
     * NORMALIZATION
     * =====================================================
     */

    private String normalizeAcademicYear(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Năm học không được để trống.");
        }

        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private Integer parseSemester(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        Matcher matcher =
                FIRST_NUMBER.matcher(value);

        return matcher.find()
                ? Integer.valueOf(matcher.group(1))
                : null;
    }

    /*
     * =====================================================
     * RESPONSE MAPPING
     * =====================================================
     */

    private ClassSectionResponse map(
            ClassSection section) {

        Syllabus syllabus = section.getSyllabus();

        return ClassSectionResponse.builder()
                .id(section.getId())
                .courseId(section.getCourse().getId())
                .courseCode(
                        section.getCourse().getCourseCode())
                .courseName(
                        section.getCourse().getName())
                .programId(section.getProgram() == null ? null : section.getProgram().getId())
                .programCode(section.getProgram() == null ? null : section.getProgram().getCode())
                .programName(section.getProgram() == null ? null : section.getProgram().getName())
                .cohortId(section.getCohort() == null ? null : section.getCohort().getId())
                .cohortName(section.getCohort() == null ? null : section.getCohort().getName())
                .syllabusId(
                        syllabus == null
                                ? null
                                : syllabus.getId())
                .syllabusVersionNumber(
                        syllabus == null
                                ? null
                                : syllabus.getVersionNumber())
                .syllabusStatus(
                        syllabus == null
                                || syllabus.getStatus() == null
                                ? null
                                : syllabus.getStatus().name())
                .instructorId(
                        section.getInstructor().getId())
                .instructorName(
                        section.getInstructor().getFullName())
                .semester(section.getSemester())
                .academicYear(section.getAcademicYear())
                .groupNumber(section.getGroupNumber())
                .labGroup(section.getLabGroup())
                .maxStudents(section.getMaxStudents())
                .room(section.getRoom())
                .schedule(section.getSchedule())
                .sectionType(section.getSectionType())
                .isActive(section.getIsActive())
                .readyForSyllabusCreation(
                        Boolean.TRUE.equals(
                                section.getIsActive())
                                && syllabus == null)
                .build();
    }
}
