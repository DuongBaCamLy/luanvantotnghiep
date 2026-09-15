package com.scse.curriculum.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scse.curriculum.auth.security.CurrentUserService;
import com.scse.curriculum.classsection.entity.ClassSection;
import com.scse.curriculum.classsection.entity.SectionType;
import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.common.exception.ForbiddenOperationException;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.dashboard.dto.DashboardFacultyResponse;
import com.scse.curriculum.deadline.entity.SyllabusDeadline;
import com.scse.curriculum.deadline.repository.SyllabusDeadlineRepository;
import com.scse.curriculum.deadline.service.DeadlineReminderProperties;
import com.scse.curriculum.department.entity.Department;
import com.scse.curriculum.instructor.entity.Instructor;
import com.scse.curriculum.instructor.repository.InstructorRepository;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class FacultyDashboardQueryServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final ZonedDateTime NOW = ZonedDateTime.of(
            2026, 7, 25, 9, 0, 0, 0, ZONE);

    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private InstructorRepository instructorRepository;
    @Mock
    private ClassSectionRepository classSectionRepository;
    @Mock
    private SyllabusDeadlineRepository syllabusDeadlineRepository;
    @Mock
    private DeadlineReminderProperties deadlineReminderProperties;
    @Mock
    private FacultyDashboardTimeProvider timeProvider;

    @InjectMocks
    private FacultyDashboardQueryService service;

    private Department department;
    private Instructor instructor;
    private UserAccount faculty;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id(4)
                .code("CHEM")
                .name("Department of Chemistry")
                .nameVn("Bộ môn Hóa học")
                .isActive(true)
                .build();

        instructor = Instructor.builder()
                .id(50)
                .staffCode("GV050")
                .fullName("Nguyễn Văn An")
                .email("an@hcmiu.edu.vn")
                .department(department)
                .isActive(true)
                .build();

        faculty = UserAccount.builder()
                .id(10)
                .username("faculty.an")
                .email("an@hcmiu.edu.vn")
                .role(UserRole.INSTRUCTOR)
                .instructorId(50)
                .isActive(true)
                .build();
    }

    @Test
    void groupsActiveClassSectionsAndUsesRealDeadlineForExactTerm() {
        Course chemistry = course(
                101,
                "CH011IU",
                "Chemistry for Engineer",
                "Hóa học cho Kỹ sư",
                3,
                0);
        Course laboratory = course(
                102,
                "CH012IU",
                "Chemistry Laboratory",
                "Thí nghiệm Hóa học",
                1,
                1);

        Syllabus draft = syllabus(201, 1, "v1.0", SyllabusStatus.DRAFT);
        Syllabus approved = syllabus(
                202, 2, "v2.0", SyllabusStatus.APPROVED);

        List<ClassSection> assignments = List.of(
                section(1, chemistry, draft, 1, "2025-2026", 1),
                section(2, chemistry, draft, 1, "2025-2026", 2),
                section(3, laboratory, approved, 1, "2025-2026", 1));

        SyllabusDeadline deadline = deadline(
                1L,
                "2025-2026",
                1,
                LocalDateTime.of(2026, 7, 30, 17, 0),
                3);

        mockCurrentFaculty(assignments, List.of(deadline));

        DashboardFacultyResponse result = service.getMyDashboard();

        assertThat(result.getInstructorName()).isEqualTo("Nguyễn Văn An");
        assertThat(result.getDepartmentCode()).isEqualTo("CHEM");
        assertThat(result.getAssignedCourses()).isEqualTo(2);
        assertThat(result.getAssignedSections()).isEqualTo(3);
        assertThat(result.getCompletedSyllabuses()).isEqualTo(1);
        assertThat(result.getPendingSyllabuses()).isEqualTo(1);
        assertThat(result.getActionRequiredCourses()).isEqualTo(1);
        assertThat(result.getDueSoonCourses()).isEqualTo(1);
        assertThat(result.getOverdueCourses()).isZero();
        assertThat(result.getTerms()).hasSize(1);
        assertThat(result.getDefaultTermKey()).isEqualTo("2025-2026::1");

        DashboardFacultyResponse.CourseAssignment item =
                result.getUpcomingDeadlines().getFirst();
        assertThat(item.getCourseCode()).isEqualTo("CH011IU");
        assertThat(item.getSectionCount()).isEqualTo(2);
        assertThat(item.getGroupNumbers()).containsExactly(1, 2);
        assertThat(item.getStatus()).isEqualTo("DRAFT");
        assertThat(item.getRecommendedAction()).isEqualTo("EDIT");
        assertThat(item.getDeadlineState()).isEqualTo("DUE_SOON");
        assertThat(item.getDeadlineRevision()).isEqualTo(3);
        assertThat(item.getDeadline()).isEqualTo(
                deadline.getDeadlineAt().atZone(ZONE).toOffsetDateTime());
        assertThat(item.getDataQualityState()).isEqualTo("CONSISTENT");
    }

    @Test
    void marksMissingSyllabusAsOverdueAndRecommendsCreate() {
        Course course = course(
                101,
                "CH011IU",
                "Chemistry for Engineer",
                "Hóa học cho Kỹ sư",
                3,
                0);
        ClassSection assignment = section(
                1, course, null, 1, "2025-2026", 1);
        SyllabusDeadline deadline = deadline(
                1L,
                "2025-2026",
                1,
                LocalDateTime.of(2026, 7, 24, 17, 0),
                2);

        mockCurrentFaculty(List.of(assignment), List.of(deadline));

        DashboardFacultyResponse result = service.getMyDashboard();
        DashboardFacultyResponse.CourseAssignment item =
                result.getUpcomingDeadlines().getFirst();

        assertThat(result.getOverdueCourses()).isEqualTo(1);
        assertThat(result.getActionRequiredCourses()).isEqualTo(1);
        assertThat(item.getStatus()).isEqualTo("NOT_CREATED");
        assertThat(item.getDeadlineState()).isEqualTo("OVERDUE");
        assertThat(item.getRecommendedAction()).isEqualTo("CREATE");
        assertThat(item.getPrimaryClassSectionId()).isEqualTo(1);
        assertThat(item.getDataQualityState()).isEqualTo("UNLINKED");
        assertThat(item.getMinutesRemaining()).isNegative();
    }

    @Test
    void reportsUnconfiguredDeadlineWithoutInventingTbdValue() {
        Course course = course(
                101,
                "CH011IU",
                "Chemistry for Engineer",
                "Hóa học cho Kỹ sư",
                3,
                0);

        mockCurrentFaculty(
                List.of(section(1, course, null, 2, "2025-2026", 1)),
                List.of());

        DashboardFacultyResponse result = service.getMyDashboard();
        DashboardFacultyResponse.CourseAssignment item =
                result.getUpcomingDeadlines().getFirst();

        assertThat(result.getUnconfiguredDeadlineCourses()).isEqualTo(1);
        assertThat(item.getDeadline()).isNull();
        assertThat(item.getDaysRemaining()).isNull();
        assertThat(item.getDeadlineState()).isEqualTo("NOT_CONFIGURED");
    }

    @Test
    void instructorCannotReadAnotherFacultyDashboardByChangingUrlId() {
        when(currentUserService.getCurrentUser()).thenReturn(faculty);

        assertThatThrownBy(() -> service.getForRequestedUser(999))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("chính mình");
    }

    @Test
    void instructorCanReadOwnIdWithEmptyAssignments() {
        mockCurrentFaculty(List.of(), List.of());
        assertThat(service.getForRequestedUser(faculty.getId()).getAssignedCourses()).isZero();
        org.mockito.Mockito.verify(classSectionRepository).findActiveByInstructorId(50);
    }

    @Test
    void adminCanInspectRequestedFaculty() {
        mockCurrentFaculty(List.of(), List.of());
        when(currentUserService.getCurrentUser()).thenReturn(
                UserAccount.builder().id(1).role(UserRole.ADMIN).build());
        when(userAccountRepository.findById(faculty.getId())).thenReturn(Optional.of(faculty));
        assertThat(service.getForRequestedUser(faculty.getId()).getFacultyUserId()).isEqualTo(faculty.getId());
    }

    @Test
    void selfEndpointRequiresInstructorRole() {
        when(currentUserService.getCurrentUser()).thenReturn(
                UserAccount.builder().id(1).role(UserRole.ADMIN).build());
        assertThatThrownBy(service::getMyDashboard).isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void missingInstructorProfileIsReportedClearly() {
        faculty.setInstructorId(null);
        when(currentUserService.getCurrentUser()).thenReturn(faculty);
        assertThatThrownBy(service::getMyDashboard)
                .isInstanceOf(ForbiddenOperationException.class).hasMessageContaining("Instructor");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = SyllabusStatus.class, names = {"SUBMITTED", "DRAFT"})
    void followsRelinkedWorkflowVersion(SyllabusStatus status) {
        Course course = course(101, "IT001IU", "Course", "Course", 3, 0);
        Syllabus linked = syllabus(202, 2, "v2.0", status);
        mockCurrentFaculty(List.of(section(1, course, linked, 1, "2026-2027", 1)), List.of());
        var item = service.getMyDashboard().getUpcomingDeadlines().getFirst();
        assertThat(item.getSyllabusId()).isEqualTo(202);
        assertThat(item.getStatus()).isEqualTo(status.name());
    }

    @Test
    void rejectedHistoryDoesNotRecommendEditing() {
        Course course = course(101, "IT001IU", "Course", "Course", 3, 0);
        mockCurrentFaculty(List.of(section(1, course,
                syllabus(201, 1, "v1.0", SyllabusStatus.REJECTED), 1, "2026-2027", 1)), List.of());
        assertThat(service.getMyDashboard().getUpcomingDeadlines().getFirst().getRecommendedAction())
                .isEqualTo("VIEW_HISTORY");
    }

    private void mockCurrentFaculty(
            List<ClassSection> assignments,
            List<SyllabusDeadline> deadlines) {
        when(currentUserService.getCurrentUser()).thenReturn(faculty);
        when(instructorRepository.findById(50))
                .thenReturn(Optional.of(instructor));
        when(classSectionRepository.findActiveByInstructorId(50))
                .thenReturn(assignments);
        when(syllabusDeadlineRepository
                .findByActiveTrueOrderByDeadlineAtAsc())
                .thenReturn(deadlines);
        when(deadlineReminderProperties.zoneId()).thenReturn(ZONE);
        when(timeProvider.now(ZONE)).thenReturn(NOW);
    }

    private Course course(
            int id,
            String code,
            String name,
            String nameVn,
            int theory,
            int lab) {
        return Course.builder()
                .id(id)
                .courseCode(code)
                .name(name)
                .nameVn(nameVn)
                .department(department)
                .creditTheory(theory)
                .creditLab(lab)
                .isActive(true)
                .build();
    }

    private Syllabus syllabus(
            int id,
            int version,
            String label,
            SyllabusStatus status) {
        return Syllabus.builder()
                .id(id)
                .versionNumber(version)
                .versionLabel(label)
                .status(status)
                .isCurrent(status == SyllabusStatus.APPROVED)
                .build();
    }

    private ClassSection section(
            int id,
            Course course,
            Syllabus syllabus,
            int semester,
            String academicYear,
            int group) {
        return ClassSection.builder()
                .id(id)
                .course(course)
                .syllabus(syllabus)
                .instructor(instructor)
                .semester(semester)
                .academicYear(academicYear)
                .groupNumber(group)
                .sectionType(SectionType.THEORY)
                .room("A1.101")
                .schedule("Monday 08:00")
                .isActive(true)
                .build();
    }

    private SyllabusDeadline deadline(
            long id,
            String academicYear,
            int semester,
            LocalDateTime deadlineAt,
            int revision) {
        return SyllabusDeadline.builder()
                .id(id)
                .academicYear(academicYear)
                .semester(semester)
                .deadlineAt(deadlineAt)
                .reminderDays("14,7,3,1,0")
                .escalationDays("0,1,3,7,14")
                .active(true)
                .revision(revision)
                .build();
    }
}
