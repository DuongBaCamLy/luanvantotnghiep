package com.scse.curriculum.syllabus.service;

import com.scse.curriculum.approval.repository.ApprovalRequestRepository;
import com.scse.curriculum.assessment.repository.AssessmentCloRepository;
import com.scse.curriculum.assessment.repository.AssessmentComponentRepository;
import com.scse.curriculum.classsection.entity.ClassSection;
import com.scse.curriculum.classsection.entity.SectionType;
import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.clo.repository.CloRepository;
import com.scse.curriculum.cloplomapping.repository.CloPloMappingRepository;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.course.repository.CourseRepository;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.enrollment.repository.EnrollmentRepository;
import com.scse.curriculum.instructor.entity.Instructor;
import com.scse.curriculum.email.WorkflowNotificationService;
import com.scse.curriculum.studentscore.repository.StudentScoreRepository;
import com.scse.curriculum.syllabus.dto.CreateSyllabusRequest;
import com.scse.curriculum.syllabus.dto.SyllabusResponse;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.syllabusbook.repository.SyllabusBookRepository;
import com.scse.curriculum.topic.repository.TopicRepository;
import com.scse.curriculum.topicclo.repository.TopicCloRepository;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.user.repository.UserAccountRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyllabusServiceImplCreateTest {

    @Mock
    private CourseProgramRepository courseProgramRepository;

    @Mock
    private SyllabusRepository repository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserAccountRepository userRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CloRepository cloRepository;

    @Mock
    private CloPloMappingRepository cloPloMappingRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private TopicCloRepository topicCloRepository;

    @Mock
    private SyllabusBookRepository syllabusBookRepository;

    @Mock
    private AssessmentComponentRepository
            assessmentComponentRepository;

    @Mock
    private AssessmentCloRepository assessmentCloRepository;

    @Mock
    private StudentScoreRepository studentScoreRepository;

    @Mock
    private ApprovalRequestRepository approvalRequestRepository;

    @Mock
    private ClassSectionRepository classSectionRepository;

    @Mock
    private WorkflowNotificationService workflowNotificationService;

    @Mock
    private SyllabusAccessService syllabusAccessService;

    @Mock
    private SyllabusSubmissionValidationService
            submissionValidationService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private SyllabusServiceImpl service;

    private Course course;
    private UserAccount faculty;
    private UserAccount admin;
    private Instructor instructor;
    private ClassSection assignment;

    @BeforeEach
    void setUp() {
        course = Course.builder()
                .id(20)
                .courseCode("EE066IU")
                .name("Concepts in VLSI Design")
                .nameVn("Các khái niệm trong thiết kế VLSI")
                .isActive(true)
                .build();

        faculty = UserAccount.builder()
                .id(10)
                .username("instructor1")
                .email("instructor1@example.com")
                .role(UserRole.INSTRUCTOR)
                .instructorId(3)
                .isActive(true)
                .build();

        admin = UserAccount.builder()
                .id(1)
                .username("admin")
                .email("admin@example.com")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();

        instructor = Instructor.builder()
                .id(3)
                .staffCode("GV003")
                .fullName("Hoàng Thị Mai")
                .email("mai@example.com")
                .isActive(true)
                .build();

        assignment = ClassSection.builder()
                .id(6)
                .course(course)
                .instructor(instructor)
                .syllabus(null)
                .semester(1)
                .academicYear("2099-2100")
                .groupNumber(999)
                .maxStudents(50)
                .sectionType(SectionType.THEORY)
                .isActive(true)
                .build();
    }

    @Test
    void createUsesAuthorizedUserAndLinksExactlySelectedAssignment() {
        CreateSyllabusRequest request =
                createRequest(
                        6,
                        20,
                        "2099-2100",
                        "HK1");

        /*
         * Client cố gửi user và version giả.
         * Backend không được tin hai giá trị này.
         */
        request.setCreatedBy(999);
        request.setVersionNumber(999);

        SyllabusAccessService.CreationAuthorization authorization =
                new SyllabusAccessService.CreationAuthorization(
                        faculty,
                        course,
                        "2099-2100",
                        "HK1",
                        List.of(assignment));

        when(courseRepository.findById(20))
                .thenReturn(Optional.of(course));

        when(syllabusAccessService.authorizeCreate(
                6,
                course,
                "2099-2100",
                "HK1"))
                .thenReturn(authorization);

        when(repository.findMaxVersionNumberByCourseId(20))
                .thenReturn(4);

        stubSyllabusSave(100);

        SyllabusResponse response = service.create(request);

        ArgumentCaptor<Syllabus> syllabusCaptor =
                ArgumentCaptor.forClass(Syllabus.class);

        verify(repository).save(syllabusCaptor.capture());

        Syllabus saved = syllabusCaptor.getValue();

        assertThat(saved.getId()).isEqualTo(100);
        assertThat(saved.getCourse()).isSameAs(course);

        /*
         * createdBy phải đến từ authorization/JWT,
         * không phải request.createdBy = 999.
         */
        assertThat(saved.getCreatedBy()).isSameAs(faculty);
        assertThat(saved.getCreatedBy().getId()).isEqualTo(10);

        /*
         * Version phải do server tạo:
         * max hiện tại là 4 nên version mới là 5.
         */
        assertThat(saved.getVersionNumber()).isEqualTo(5);
        assertThat(saved.getVersionNumber())
                .isNotEqualTo(request.getVersionNumber());

        assertThat(saved.getStatus())
                .isEqualTo(SyllabusStatus.DRAFT);

        assertThat(saved.getAcademicYear())
                .isEqualTo("2099-2100");

        assertThat(saved.getSemester())
                .isEqualTo("HK1");

        /*
         * Chỉ đúng assignment được gắn syllabus.
         */
        assertThat(assignment.getSyllabus())
                .isSameAs(saved);

        verify(classSectionRepository)
                .saveAll(List.of(assignment));

        verify(syllabusAccessService).authorizeCreate(
                6,
                course,
                "2099-2100",
                "HK1");

        verify(repository)
                .findMaxVersionNumberByCourseId(20);

        assertThat(response.getId()).isEqualTo(100);
        assertThat(response.getCourseId()).isEqualTo(20);
        assertThat(response.getCreatedById()).isEqualTo(10);
        assertThat(response.getCreatedByUsername())
                .isEqualTo("instructor1");
        assertThat(response.getVersionNumber()).isEqualTo(5);
        assertThat(response.getStatus()).isEqualTo("DRAFT");
    }

    @Test
    void createUsesYearAndSemesterReturnedByAuthorization() {
        /*
         * Test này bảo vệ SyllabusServiceImpl:
         * nó phải dùng dữ liệu đã xác thực trả về từ
         * SyllabusAccessService, không dùng trực tiếp request.
         */
        CreateSyllabusRequest request =
                createRequest(
                        6,
                        20,
                        "CLIENT-FORGED-YEAR",
                        "HK8");

        SyllabusAccessService.CreationAuthorization authorization =
                new SyllabusAccessService.CreationAuthorization(
                        faculty,
                        course,
                        "2099-2100",
                        "HK1",
                        List.of(assignment));

        when(courseRepository.findById(20))
                .thenReturn(Optional.of(course));

        when(syllabusAccessService.authorizeCreate(
                6,
                course,
                "CLIENT-FORGED-YEAR",
                "HK8"))
                .thenReturn(authorization);

        when(repository.findMaxVersionNumberByCourseId(20))
                .thenReturn(0);

        stubSyllabusSave(101);

        service.create(request);

        ArgumentCaptor<Syllabus> captor =
                ArgumentCaptor.forClass(Syllabus.class);

        verify(repository).save(captor.capture());

        Syllabus saved = captor.getValue();

        assertThat(saved.getAcademicYear())
                .isEqualTo("2099-2100");

        assertThat(saved.getSemester())
                .isEqualTo("HK1");

        assertThat(saved.getAcademicYear())
                .isNotEqualTo(request.getAcademicYear());

        assertThat(saved.getSemester())
                .isNotEqualTo(request.getSemester());
    }

    @Test
    void adminCreationWithoutAssignmentDoesNotLinkClassSection() {
        CreateSyllabusRequest request =
                createRequest(
                        null,
                        20,
                        "2026-2027",
                        "HK2");

        SyllabusAccessService.CreationAuthorization authorization =
                new SyllabusAccessService.CreationAuthorization(
                        admin,
                        course,
                        "2026-2027",
                        "HK2",
                        List.of());

        when(courseRepository.findById(20))
                .thenReturn(Optional.of(course));

        when(syllabusAccessService.authorizeCreate(
                null,
                course,
                "2026-2027",
                "HK2"))
                .thenReturn(authorization);

        when(repository.findMaxVersionNumberByCourseId(20))
                .thenReturn(null);

        stubSyllabusSave(102);

        SyllabusResponse response = service.create(request);

        verifyNoInteractions(classSectionRepository);

        assertThat(response.getId()).isEqualTo(102);
        assertThat(response.getCreatedById()).isEqualTo(1);
        assertThat(response.getVersionNumber()).isEqualTo(1);
        assertThat(response.getAcademicYear())
                .isEqualTo("2026-2027");
        assertThat(response.getSemester()).isEqualTo("HK2");
    }

    private CreateSyllabusRequest createRequest(
            Integer classSectionId,
            Integer courseId,
            String academicYear,
            String semester) {

        CreateSyllabusRequest request =
                new CreateSyllabusRequest();

        request.setClassSectionId(classSectionId);
        request.setCourseId(courseId);
        request.setCourseProgramId(null);
        request.setVersionNumber(null);
        request.setVersionLabel(null);
        request.setAcademicYear(academicYear);
        request.setSemester(semester);

        request.setCourseDesignation("Fundamental course");
        request.setCourseTypes("[\"Fundamental\"]");
        request.setLanguage("English");
        request.setRelation("None");
        request.setTeachingMethods("Lecture");
        request.setWorkloadTotal("10");
        request.setWorkloadContact("4");
        request.setWorkloadPrivate("6");
        request.setPrerequisites("None");
        request.setObjectives("Understand the course");
        request.setExamForms("Assignments");
        request.setExamRequirements("Complete assignments");
        request.setMajor("Computer Science");

        request.setChangeSummary("Create test draft");
        request.setNotes("Automated unit test");

        request.setSourceSyllabusId(null);
        request.setClos(null);
        request.setTopics(null);
        request.setAssessments(null);

        return request;
    }

    private void stubSyllabusSave(Integer generatedId) {
        when(repository.save(any(Syllabus.class)))
                .thenAnswer(invocation -> {
                    Syllabus syllabus =
                            invocation.getArgument(0);

                    syllabus.setId(generatedId);
                    return syllabus;
                });
    }
}