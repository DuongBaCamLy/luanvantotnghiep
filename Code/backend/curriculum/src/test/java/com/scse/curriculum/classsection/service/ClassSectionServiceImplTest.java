package com.scse.curriculum.classsection.service;
import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.major.entity.Major;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.program.repository.ProgramRepository;
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
private ProgramRepository programRepository;

@Mock
private CohortRepository cohortRepository;

@Mock
private CourseProgramRepository courseProgramRepository;

private Major csMajor;
private Major itMajor;

private Program csProgram;
private Program itProgram;

private Cohort csCohort;
private Cohort itCohort;
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

                csMajor = Major.builder()
        .id(7)
        .code("CS")
        .name("Computer Science")
        .build();

itMajor = Major.builder()
        .id(8)
        .code("IT")
        .name("Information Technology")
        .build();

csProgram = Program.builder()
        .id(70)
        .code("CS")
        .name("Computer Science")
        .major(csMajor)
        .build();

itProgram = Program.builder()
        .id(80)
        .code("IT")
        .name("Information Technology")
        .major(itMajor)
        .build();

csCohort = Cohort.builder()
        .id(700)
        .name("CS2026")
        .entryYear(2026)
        .program(csProgram)
        .isActive(true)
        .build();

itCohort = Cohort.builder()
        .id(800)
        .name("IT2026")
        .entryYear(2026)
        .program(itProgram)
        .isActive(true)
        .build();
        csDepartmentHeadAccount = UserAccount.builder()
        .id(2)
        .username("cs_head")
        .role(UserRole.DEPT_HEAD)
        .instructorId(100)
        .managedMajor(csMajor)
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
void departmentHeadGetAllUsesOwnManagedMajorScope() {
    ClassSection section = createSection(
            2,
            csCourse,
            csInstructor,
            null);

    when(currentUserService.getCurrentUser())
            .thenReturn(csDepartmentHeadAccount);

    when(repository.findAllInScope(7))
            .thenReturn(List.of(section));

    List<ClassSectionResponse> result =
            service.getAll();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getCourseId())
            .isEqualTo(10);

    verify(repository).findAllInScope(7);
}
    @Test
void departmentHeadCannotReadSectionOutsideManagedMajor() {
    when(currentUserService.getCurrentUser())
            .thenReturn(csDepartmentHeadAccount);

    when(repository.findByIdInScope(999, 7))
            .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getById(999))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Class section not found");

    verify(repository).findByIdInScope(999, 7);
}
    @Test
void departmentHeadCannotCreateSectionForProgramOfOtherManagedMajor() {
    CreateClassSectionRequest request =
            createRequest(20, 101);

    request.setProgramId(80);
    request.setCohortId(800);

    when(currentUserService.getCurrentUser())
            .thenReturn(csDepartmentHeadAccount);

    when(courseRepository.findById(20))
            .thenReturn(Optional.of(itCourse));

    when(instructorRepository.findById(101))
            .thenReturn(Optional.of(csInstructor));

    stubCurriculumContext(
            itCourse,
            itProgram,
            itCohort);

    assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(ForbiddenOperationException.class)
            .hasMessageContaining("Managed Major");

    verify(repository, never())
            .save(any(ClassSection.class));
}

    @Test
void departmentHeadCanAssignActiveInstructorWhenProgramIsInManagedMajor() {
    CreateClassSectionRequest request =
            createRequest(10, 201);

    when(currentUserService.getCurrentUser())
            .thenReturn(csDepartmentHeadAccount);

    when(courseRepository.findById(10))
            .thenReturn(Optional.of(csCourse));

    when(instructorRepository.findById(201))
            .thenReturn(Optional.of(itInstructor));

    stubCurriculumContext(
            csCourse,
            csProgram,
            csCohort);

    when(repository.findByCourse_Id(10))
            .thenReturn(List.of());

    when(repository.save(any(ClassSection.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    ClassSectionResponse result =
            service.create(request);

    assertThat(result.getCourseId()).isEqualTo(10);
    assertThat(result.getInstructorId()).isEqualTo(201);
    assertThat(result.getProgramId()).isEqualTo(70);
    assertThat(result.getCohortId()).isEqualTo(700);
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
    void cannotCreateDuplicateInstructorCourseAssignment() {
        CreateClassSectionRequest request =
                createRequest(10, 101);

        when(currentUserService.getCurrentUser())
                .thenReturn(adminAccount);

        when(courseRepository.findById(10))
                .thenReturn(Optional.of(csCourse));

        when(instructorRepository.findById(101))
                .thenReturn(Optional.of(csInstructor));

        stubCurriculumContext(
                csCourse,
                csProgram,
                csCohort);

        ClassSection duplicate =
                ClassSection.builder()
                        .id(99)
                        .course(csCourse)
                        .program(csProgram)
                        .cohort(csCohort)
                        .instructor(csInstructor)
                        .syllabus(null)
                        .semester(1)
                        .academicYear("2026-2027")
                        .groupNumber(1)
                        .sectionType(SectionType.THEORY)
                        .isActive(true)
                        .build();

        when(repository.findByCourse_Id(10))
                .thenReturn(List.of(duplicate));

        assertThatThrownBy(
                () -> service.create(request))
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessageContaining(
                        "Teaching assignment already exists");

        verify(repository, never())
                .save(any(ClassSection.class));
    }

    @Test
    void linkedSyllabusUsesCohortIdentityNotTeachingAcademicYear() {
        /*
         * Regression:
         *
         * Syllabus academicYear stores the curriculum Cohort name:
         *     CS2026
         *
         * ClassSection academicYear stores the teaching academic year:
         *     2026-2027
         *
         * They are different concepts and must not be compared directly.
         */
        Program program = Program.builder()
                .id(71)
                .code("CS-2021")
                .name("Computer Science 2021")
                .major(csMajor)
                .build();

        Cohort cohort = Cohort.builder()
                .id(701)
                .name("CS2026")
                .entryYear(2026)
                .program(program)
                .isActive(true)
                .build();

        CourseProgram curriculumEntry =
                CourseProgram.builder()
                        .course(csCourse)
                        .program(program)
                        .cohort(cohort)
                        .semesterSuggest(2)
                        .build();

        Syllabus syllabus =
                Syllabus.builder()
                        .id(501)
                        .course(csCourse)
                        .program("CS-2021")
                        .academicYear("CS2026")
                        .semester("Semester 2")
                        .versionNumber(1)
                        .build();

        CreateClassSectionRequest request =
                createRequest(10, 101);

        request.setProgramId(71);
        request.setCohortId(701);
        request.setSyllabusId(501);
        /*
         * Simulate stale/wrong client metadata.
         * Curriculum is authoritative and says Semester 2.
         */
        request.setSemester(1);

        /*
         * Teaching academic year deliberately differs from
         * the syllabus Cohort identity.
         */
        request.setAcademicYear("2026-2027");

        when(currentUserService.getCurrentUser())
                .thenReturn(adminAccount);

        when(courseRepository.findById(10))
                .thenReturn(Optional.of(csCourse));

        when(programRepository.findById(71))
                .thenReturn(Optional.of(program));

        when(cohortRepository.findById(701))
                .thenReturn(Optional.of(cohort));

        when(courseProgramRepository
                .findEffectiveByProgramIdAndCohortIdWithRelations(
                        71,
                        701))
                .thenReturn(List.of(curriculumEntry));

        when(courseProgramRepository
                .findByCourse_IdAndProgram_IdAndCohort_Id(
                        10,
                        71,
                        701))
                .thenReturn(Optional.of(curriculumEntry));

        when(instructorRepository.findById(101))
                .thenReturn(Optional.of(csInstructor));

        when(syllabusRepository.findById(501))
                .thenReturn(Optional.of(syllabus));

        when(repository.findByCourse_Id(10))
                .thenReturn(List.of());

        when(repository.save(any(ClassSection.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        ClassSectionResponse result =
                service.create(request);

        assertThat(result.getCourseId())
                .isEqualTo(10);

        assertThat(result.getProgramCode())
                .isEqualTo("CS-2021");

        assertThat(result.getCohortName())
                .isEqualTo("CS2026");

        assertThat(result.getSemester())
                .isEqualTo(2);

        assertThat(result.getAcademicYear())
                .isEqualTo("2026-2027");

        assertThat(result.getSyllabusId())
                .isEqualTo(501);
    }
    private CreateClassSectionRequest createRequest(
            Integer courseId,
            Integer instructorId) {

        CreateClassSectionRequest request =
                new CreateClassSectionRequest();

        request.setCourseId(courseId);
        request.setProgramId(70);
request.setCohortId(700);
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
private void stubCurriculumContext(
        Course course,
        Program program,
        Cohort cohort) {

    when(programRepository.findById(program.getId()))
            .thenReturn(Optional.of(program));

    when(cohortRepository.findById(cohort.getId()))
            .thenReturn(Optional.of(cohort));

    CourseProgram curriculumEntry =
            CourseProgram.builder()
                    .course(course)
                    .program(program)
                    .cohort(cohort)
                    .build();

    when(courseProgramRepository
            .findEffectiveByProgramIdAndCohortIdWithRelations(
                    program.getId(),
                    cohort.getId()))
            .thenReturn(List.of(curriculumEntry));
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
