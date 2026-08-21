package com.scse.curriculum.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scse.curriculum.auth.security.CurrentUserService;
import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.coursetype.entity.CourseType;
import com.scse.curriculum.dashboard.dto.DashboardDeanResponse;
import com.scse.curriculum.deadline.service.DeadlineReminderProperties;
import com.scse.curriculum.major.entity.Major;
import com.scse.curriculum.major.repository.MajorRepository;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;

@ExtendWith(MockitoExtension.class)
class DeanDashboardQueryServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private MajorRepository majorRepository;
    @Mock
    private CohortRepository cohortRepository;
    @Mock
    private CourseProgramRepository courseProgramRepository;
    @Mock
    private SyllabusRepository syllabusRepository;
    @Mock
    private DeadlineReminderProperties deadlineReminderProperties;
    @Mock
    private DeanDashboardTimeProvider timeProvider;

    @InjectMocks
    private DeanDashboardQueryService service;

    private Major csMajor;
    private Major itMajor;
    private Program csProgram;
    private Program itProgram;
    private Cohort cs2021;
    private Cohort it2021;

    @BeforeEach
    void setUp() {
        csMajor = Major.builder()
                .id(1)
                .code("CS")
                .name("Computer Science")
                .nameVn("Khoa học Máy tính")
                .build();
        itMajor = Major.builder()
                .id(2)
                .code("IT")
                .name("Information Technology")
                .nameVn("Công nghệ Thông tin")
                .build();

        csProgram = Program.builder()
                .id(10)
                .code("CS-2021")
                .name("Bachelor of Computer Science")
                .major(csMajor)
                .isActive(true)
                .build();
        itProgram = Program.builder()
                .id(20)
                .code("IT-2021")
                .name("Bachelor of Information Technology")
                .major(itMajor)
                .isActive(true)
                .build();

        cs2021 = Cohort.builder()
                .id(100)
                .program(csProgram)
                .entryYear(2021)
                .name("CS2021")
                .isActive(true)
                .build();
        it2021 = Cohort.builder()
                .id(200)
                .program(itProgram)
                .entryYear(2021)
                .name("IT2021")
                .isActive(true)
                .build();

        UserAccount dean = UserAccount.builder()
                .id(2)
                .username("dean.scse")
                .email("dean.scse@hcmiu.edu.vn")
                .role(UserRole.DEAN)
                .isActive(true)
                .build();
        when(currentUserService.getCurrentUser()).thenReturn(dean);
        when(majorRepository.findAllByOrderByCodeAsc())
                .thenReturn(List.of(csMajor, itMajor));
        when(cohortRepository.findActiveForDeanDashboard())
                .thenReturn(List.of(cs2021, it2021));
    }

    @Test
    void filtersByMajorCohortSemesterAndCountsEachCourseOnce() {
        stubDashboardClock();
        Course algorithms = course(1, "IT013IU", "Algorithms", 4, 0);
        Course database = course(2, "IT079IU", "Database", 3, 1);
        CourseType compulsory = CourseType.builder()
                .id(1)
                .code("COMPULSORY")
                .name("Compulsory")
                .nameVn("Bắt buộc")
                .build();

        CourseProgram generalAlgorithms = mapping(
                1, algorithms, null, compulsory, 1);
        CourseProgram cohortAlgorithms = mapping(
                2, algorithms, cs2021, compulsory, 2);
        CourseProgram cohortDatabase = mapping(
                3, database, cs2021, compulsory, 2);

        Syllabus algorithmsApproved = syllabus(
                11, algorithms, 1, "v1.0", SyllabusStatus.APPROVED,
                true, "CS2021", "2");
        Syllabus algorithmsHistorical = syllabus(
                10, algorithms, 0, "v0.9", SyllabusStatus.ARCHIVED,
                false, "CS2021", "2");

        when(courseProgramRepository
                .findEffectiveByProgramIdAndCohortIdWithRelations(10, 100))
                .thenReturn(List.of(
                        generalAlgorithms,
                        cohortAlgorithms,
                        cohortDatabase));
        when(syllabusRepository.findDeanDashboardCandidates(List.of(1, 2)))
                .thenReturn(List.of(
                        algorithmsApproved,
                        algorithmsHistorical));

        DashboardDeanResponse result = service.getDashboard(1, 100, 2);

        assertThat(result.getScope().getMajorCode()).isEqualTo("CS");
        assertThat(result.getScope().getCohortName()).isEqualTo("CS2021");
        assertThat(result.getScope().getSemester()).isEqualTo(2);
        assertThat(result.getSummary().getExpectedCourses()).isEqualTo(2);
        assertThat(result.getSummary().getApprovedSyllabuses()).isEqualTo(1);
        assertThat(result.getSummary().getNotApprovedSyllabuses()).isEqualTo(1);
        assertThat(result.getSummary().getMissingSyllabuses()).isEqualTo(1);
        assertThat(result.getSummary().getApprovalRate()).isEqualTo(50.0);
        assertThat(result.getCourses())
                .extracting(DashboardDeanResponse.CourseProgress::getCourseCode)
                .containsExactly("IT013IU", "IT079IU");
        assertThat(result.getCourses().getFirst().getCourseProgramId())
                .isEqualTo(2);
    }

    @Test
    void usesOneRepresentativeSyllabusInsteadOfCountingEveryVersion() {
        stubDashboardClock();
        Course course = course(1, "IT013IU", "Algorithms", 4, 0);
        CourseProgram mapping = mapping(1, course, cs2021, null, 1);
        Syllabus approvedCurrent = syllabus(
                12, course, 2, "v2.0", SyllabusStatus.APPROVED,
                true, "CS2021", "1");
        Syllabus olderApproved = syllabus(
                11, course, 1, "v1.0", SyllabusStatus.APPROVED,
                false, "CS2021", "1");
        Syllabus draftOtherCohort = syllabus(
                13, course, 3, "v3.0", SyllabusStatus.DRAFT,
                false, "IT2021", "1");

        when(courseProgramRepository
                .findEffectiveByProgramIdAndCohortIdWithRelations(10, 100))
                .thenReturn(List.of(mapping));
        when(syllabusRepository.findDeanDashboardCandidates(List.of(1)))
                .thenReturn(List.of(
                        draftOtherCohort,
                        approvedCurrent,
                        olderApproved));

        DashboardDeanResponse result = service.getDashboard(1, 100, null);

        assertThat(result.getSummary().getExpectedCourses()).isEqualTo(1);
        assertThat(result.getSummary().getApprovedSyllabuses()).isEqualTo(1);
        assertThat(result.getCourses()).hasSize(1);
        assertThat(result.getCourses().getFirst().getSyllabusId()).isEqualTo(12);
        assertThat(result.getCourses().getFirst().getStatus())
                .isEqualTo("APPROVED");
    }

    @Test
    void rejectsCohortThatDoesNotBelongToRequestedMajor() {
        assertThatThrownBy(() -> service.getDashboard(1, 200, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không thuộc ngành");
    }

    private void stubDashboardClock() {
        when(deadlineReminderProperties.zoneId()).thenReturn(ZONE);
        when(timeProvider.now(ZONE)).thenReturn(ZonedDateTime.of(
                2026, 7, 25, 15, 0, 0, 0, ZONE));
    }

    private Course course(
            int id,
            String code,
            String name,
            int theoryCredits,
            int labCredits) {
        return Course.builder()
                .id(id)
                .courseCode(code)
                .name(name)
                .nameVn(name)
                .creditTheory(theoryCredits)
                .creditLab(labCredits)
                .isActive(true)
                .build();
    }

    private CourseProgram mapping(
            int id,
            Course course,
            Cohort cohort,
            CourseType courseType,
            int semester) {
        return CourseProgram.builder()
                .id(id)
                .course(course)
                .program(csProgram)
                .cohort(cohort)
                .courseType(courseType)
                .semesterSuggest(semester)
                .yearSuggest((semester + 1) / 2)
                .required(true)
                .build();
    }

    private Syllabus syllabus(
            int id,
            Course course,
            int version,
            String label,
            SyllabusStatus status,
            boolean current,
            String academicYear,
            String semester) {
        return Syllabus.builder()
                .id(id)
                .course(course)
                .versionNumber(version)
                .versionLabel(label)
                .status(status)
                .isCurrent(current)
                .academicYear(academicYear)
                .semester(semester)
                .build();
    }
}
