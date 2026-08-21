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
        Instructor instructor =
                requireInstructor(request.getInstructorId());

        /*
         * Kiểm tra phạm vi bộ môn ở tầng service.
         * Không chỉ phụ thuộc vào @PreAuthorize ở controller.
         */
        assertCourseInManagementScope(actor, course);
        assertInstructorInManagementScope(actor, instructor);

        Syllabus syllabus = resolveOptionalSyllabus(
                request.getSyllabusId(),
                course,
                request);

        assertNoDuplicate(null, request);

        ClassSection section = ClassSection.builder()
                .course(course)
                .syllabus(syllabus)
                .instructor(instructor)
                .semester(request.getSemester())
                .academicYear(normalizeAcademicYear(
                        request.getAcademicYear()))
                .groupNumber(request.getGroupNumber())
                .labGroup(request.getLabGroup())
                .maxStudents(
                        request.getMaxStudents() == null
                                ? 50
                                : request.getMaxStudents())
                .room(normalizeOptionalText(request.getRoom()))
                .schedule(
                        normalizeOptionalText(request.getSchedule()))
                .sectionType(request.getSectionType())
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
        Instructor instructor =
                requireInstructor(request.getInstructorId());

        assertCourseInManagementScope(actor, course);
        assertInstructorInManagementScope(actor, instructor);

        Syllabus syllabus = resolveOptionalSyllabus(
                request.getSyllabusId(),
                course,
                request);

        assertNoDuplicate(id, request);

        section.setCourse(course);
        section.setSyllabus(syllabus);
        section.setInstructor(instructor);
        section.setSemester(request.getSemester());
        section.setAcademicYear(
                normalizeAcademicYear(request.getAcademicYear()));
        section.setGroupNumber(request.getGroupNumber());
        section.setLabGroup(request.getLabGroup());
        section.setMaxStudents(
                request.getMaxStudents() == null
                        ? 50
                        : request.getMaxStudents());
        section.setRoom(
                normalizeOptionalText(request.getRoom()));
        section.setSchedule(
                normalizeOptionalText(request.getSchedule()));
        section.setSectionType(request.getSectionType());
        section.setIsActive(
                request.getIsActive() == null
                        || request.getIsActive());

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
        if (currentUser.getRole() == UserRole.ADMIN) {
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

        if (actor.getRole() != UserRole.ADMIN
                && actor.getRole() != UserRole.DEPT_HEAD) {

            throw new ForbiddenOperationException(
                    "Chỉ Admin hoặc Trưởng bộ môn "
                            + "được quản lý phân công giảng dạy.");
        }

        return actor;
    }

    /**
     * Admin trả về null để repository không giới hạn department.
     * DeptHead trả về department ID của hồ sơ Instructor.
     */
    private Integer resolveDepartmentScope(
            UserAccount actor) {

        if (actor.getRole() == UserRole.ADMIN) {
            return null;
        }

        Instructor currentInstructor =
                requireCurrentUserInstructor(actor);

        if (currentInstructor.getDepartment() == null
                || currentInstructor
                        .getDepartment()
                        .getId() == null) {

            throw new ForbiddenOperationException(
                    "Tài khoản Trưởng bộ môn chưa được "
                            + "gán vào bộ môn.");
        }

        return currentInstructor
                .getDepartment()
                .getId();
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

    private void assertCourseInManagementScope(
            UserAccount actor,
            Course course) {

        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }

        Integer departmentId =
                resolveDepartmentScope(actor);

        if (course.getDepartment() == null
                || course.getDepartment().getId() == null) {

            throw new ForbiddenOperationException(
                    "Môn học chưa được gán bộ môn.");
        }

        if (!Objects.equals(
                departmentId,
                course.getDepartment().getId())) {

            throw new ForbiddenOperationException(
                    "Bạn chỉ được quản lý môn học "
                            + "thuộc bộ môn của mình.");
        }
    }

    private void assertInstructorInManagementScope(
            UserAccount actor,
            Instructor instructor) {

        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }

        Integer departmentId =
                resolveDepartmentScope(actor);

        if (instructor.getDepartment() == null
                || instructor.getDepartment().getId() == null) {

            throw new ForbiddenOperationException(
                    "Giảng viên được chọn chưa được gán bộ môn.");
        }

        if (!Objects.equals(
                departmentId,
                instructor.getDepartment().getId())) {

            throw new ForbiddenOperationException(
                    "Bạn chỉ được phân công giảng viên "
                            + "thuộc bộ môn của mình.");
        }

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

        long duplicateCount =
                repository.countDuplicateAssignment(
                        request.getCourseId(),
                        request.getSemester(),
                        normalizeAcademicYear(
                                request.getAcademicYear()),
                        request.getGroupNumber(),
                        currentId);

        if (duplicateCount > 0) {
            throw new IllegalStateException(
                    "Đã tồn tại phân công cho môn học, "
                            + "học kỳ, năm học và nhóm này.");
        }
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