package com.scse.curriculum.classsection.service;

import com.scse.curriculum.auth.security.CurrentUserService;
import com.scse.curriculum.classsection.dto.ClassSectionResponse;
import com.scse.curriculum.classsection.dto.CreateClassSectionRequest;
import com.scse.curriculum.classsection.entity.ClassSection;
import com.scse.curriculum.classsection.entity.SectionType;
import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.common.exception.ForbiddenOperationException;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.course.repository.CourseRepository;
import com.scse.curriculum.department.entity.Department;
import com.scse.curriculum.enrollment.repository.EnrollmentRepository;
import com.scse.curriculum.instructor.entity.Instructor;
import com.scse.curriculum.instructor.repository.InstructorRepository;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassSectionServiceImplTest {

    @Mock
    private ClassSectionRepository repository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private SyllabusRepository syllabusRepository;

    @Mock
    private InstructorRepository instructorRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private ClassSectionServiceImpl service;

    private Department csDepartment;
    private Department itDepartment;

    private UserAccount adminAccount;
    private UserAccount csDepartmentHeadAccount;

    private Instructor csDepartmentHeadProfile;
    private Instructor csInstructor;
    private Instructor itInstructor;

    private Course csCourse;
    private Course itCourse;

    @BeforeEach
    void setUp() {
        csDepartment = Department.builder()
                .id(1)
                .code("CS")
                .name("Computer Science")
                .nameVn("Khoa học máy tính")
                .isActive(true)
                .build();

        itDepartment = Department.builder()
                .id(2)
                .code("IT")
                .name("Information Technology")
                .nameVn("Công nghệ thông tin")
                .isActive(true)
                .build();

        adminAccount = UserAccount.builder()
                .id(1)
                .username("admin")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();

        csDepartmentHeadAccount = UserAccount.builder()
                .id(2)
                .username("cs_head")
                .role(UserRole.DEPT_HEAD)
                .instructorId(100)
                .isActive(true)
                .build();

        csDepartmentHeadProfile = Instructor.builder()
                .id(100)
                .staffCode("CS-HEAD")
                .fullName("CS Department Head")
                .email("cs-head@example.com")
                .department(csDepartment)
                .isActive(true)
                .build();

        csInstructor = Instructor.builder()
                .id(101)
                .staffCode("CS-001")
                .fullName("CS Instructor")
                .email("cs-instructor@example.com")
                .department(csDepartment)
                .isActive(true)
                .build();

        itInstructor = Instructor.builder()
                .id(201)
                .staffCode("IT-001")
                .fullName("IT Instructor")
                .email("it-instructor@example.com")
                .department(itDepartment)
                .isActive(true)
                .build();

        csCourse = Course.builder()
                .id(10)
                .courseCode("CS101")
                .name("Computer Science Fundamentals")
                .nameVn("Nhập môn Khoa học máy tính")
                .department(csDepartment)
                .isActive(true)
                .build();

        itCourse = Course.builder()
                .id(20)
                .courseCode("IT101")
                .name("Information Technology Fundamentals")
                .nameVn("Nhập môn Công nghệ thông tin")
                .department(itDepartment)
                .isActive(true)
                .build();
    }

    @Test
    void adminGetAllUsesGlobalScope() {
        ClassSection section = createSection(
                1,
                csCourse,
                csInstructor,
                null);

        when(currentUserService.getCurrentUser())
                .thenReturn(adminAccount);

        when(repository.findAllInScope(null))
                .thenReturn(List.of(section));

        List<ClassSectionResponse> result =
                service.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1);
        assertThat(result.getFirst().getCourseCode())
                .isEqualTo("CS101");

        verify(repository).findAllInScope(null);
    }

    @Test
    void departmentHeadGetAllUsesOwnDepartmentScope() {
        ClassSection section = createSection(
                2,
                csCourse,
                csInstructor,
                null);

        when(currentUserService.getCurrentUser())
                .thenReturn(csDepartmentHeadAccount);

        when(instructorRepository.findById(100))
                .thenReturn(Optional.of(
                        csDepartmentHeadProfile));

        when(repository.findAllInScope(1))
                .thenReturn(List.of(section));

        List<ClassSectionResponse> result =
                service.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getCourseId())
                .isEqualTo(10);

        verify(repository).findAllInScope(1);
    }

    @Test
    void departmentHeadCannotReadSectionOutsideDepartment() {
        when(currentUserService.getCurrentUser())
                .thenReturn(csDepartmentHeadAccount);

        when(instructorRepository.findById(100))
                .thenReturn(Optional.of(
                        csDepartmentHeadProfile));

        when(repository.findByIdInScope(999, 1))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Class section not found");

        verify(repository).findByIdInScope(999, 1);
    }

    @Test
    void departmentHeadCannotCreateSectionForCourseOfOtherDepartment() {
        CreateClassSectionRequest request =
                createRequest(20, 101);

        when(currentUserService.getCurrentUser())
                .thenReturn(csDepartmentHeadAccount);

        when(courseRepository.findById(20))
                .thenReturn(Optional.of(itCourse));

        when(instructorRepository.findById(101))
                .thenReturn(Optional.of(csInstructor));

        when(instructorRepository.findById(100))
                .thenReturn(Optional.of(
                        csDepartmentHeadProfile));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(
                        ForbiddenOperationException.class)
                .hasMessageContaining(
                        "môn học thuộc bộ môn của mình");

        verify(repository, never())
                .save(any(ClassSection.class));
    }

    @Test
    void departmentHeadCannotAssignInstructorOfOtherDepartment() {
        CreateClassSectionRequest request =
                createRequest(10, 201);

        when(currentUserService.getCurrentUser())
                .thenReturn(csDepartmentHeadAccount);

        when(courseRepository.findById(10))
                .thenReturn(Optional.of(csCourse));

        when(instructorRepository.findById(201))
                .thenReturn(Optional.of(itInstructor));

        when(instructorRepository.findById(100))
                .thenReturn(Optional.of(
                        csDepartmentHeadProfile));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(
                        ForbiddenOperationException.class)
                .hasMessageContaining(
                        "giảng viên thuộc bộ môn của mình");

        verify(repository, never())
                .save(any(ClassSection.class));
    }

    @Test
    void cannotDeleteSectionThatAlreadyHasSyllabus() {
        Syllabus syllabus = Syllabus.builder()
                .id(50)
                .course(csCourse)
                .build();

        ClassSection section = createSection(
                3,
                csCourse,
                csInstructor,
                syllabus);

        when(currentUserService.getCurrentUser())
                .thenReturn(adminAccount);

        when(repository.findByIdInScope(3, null))
                .thenReturn(Optional.of(section));

        assertThatThrownBy(() -> service.delete(3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "đã gắn đề cương");

        verify(enrollmentRepository, never())
                .existsByClassSection_Id(3);

        verify(repository, never())
                .delete(any(ClassSection.class));
    }

    @Test
    void cannotDeleteSectionThatHasEnrollment() {
        ClassSection section = createSection(
                4,
                csCourse,
                csInstructor,
                null);

        when(currentUserService.getCurrentUser())
                .thenReturn(adminAccount);

        when(repository.findByIdInScope(4, null))
                .thenReturn(Optional.of(section));

        when(enrollmentRepository
                .existsByClassSection_Id(4))
                .thenReturn(true);

        assertThatThrownBy(() -> service.delete(4))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "đã có sinh viên đăng ký");

        verify(repository, never())
                .delete(any(ClassSection.class));
    }

    @Test
    void canDeleteUnusedSection() {
        ClassSection section = createSection(
                5,
                csCourse,
                csInstructor,
                null);

        when(currentUserService.getCurrentUser())
                .thenReturn(adminAccount);

        when(repository.findByIdInScope(5, null))
                .thenReturn(Optional.of(section));

        when(enrollmentRepository
                .existsByClassSection_Id(5))
                .thenReturn(false);

        service.delete(5);

        verify(repository).delete(section);
    }

    @Test
    void cannotCreateDuplicateCourseTermYearAndGroup() {
        CreateClassSectionRequest request =
                createRequest(10, 101);

        when(currentUserService.getCurrentUser())
                .thenReturn(adminAccount);

        when(courseRepository.findById(10))
                .thenReturn(Optional.of(csCourse));

        when(instructorRepository.findById(101))
                .thenReturn(Optional.of(csInstructor));

        when(repository.countDuplicateAssignment(
                10,
                1,
                "2026-2027",
                1,
                null))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "Đã tồn tại phân công");

        verify(repository, never())
                .save(any(ClassSection.class));
    }

    private CreateClassSectionRequest createRequest(
            Integer courseId,
            Integer instructorId) {

        CreateClassSectionRequest request =
                new CreateClassSectionRequest();

        request.setCourseId(courseId);
        request.setSyllabusId(null);
        request.setInstructorId(instructorId);
        request.setSemester(1);
        request.setAcademicYear("2026-2027");
        request.setGroupNumber(1);
        request.setLabGroup(null);
        request.setMaxStudents(50);
        request.setRoom(null);
        request.setSchedule(null);
        request.setSectionType(SectionType.THEORY);
        request.setIsActive(true);

        return request;
    }

    private ClassSection createSection(
            Integer id,
            Course course,
            Instructor instructor,
            Syllabus syllabus) {

        return ClassSection.builder()
                .id(id)
                .course(course)
                .syllabus(syllabus)
                .instructor(instructor)
                .semester(1)
                .academicYear("2026-2027")
                .groupNumber(1)
                .labGroup(null)
                .maxStudents(50)
                .room(null)
                .schedule(null)
                .sectionType(SectionType.THEORY)
                .isActive(true)
                .build();
    }
}