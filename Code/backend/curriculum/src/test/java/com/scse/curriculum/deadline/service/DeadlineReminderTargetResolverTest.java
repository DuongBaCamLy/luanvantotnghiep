package com.scse.curriculum.deadline.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scse.curriculum.classsection.entity.ClassSection;
import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.instructor.entity.Instructor;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class DeadlineReminderTargetResolverTest {

    @Mock
    private ClassSectionRepository classSectionRepository;
    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private DeadlineReminderTargetResolver resolver;

    private Instructor instructor;
    private UserAccount user;
    private Course submittedCourse;
    private Course missingCourse;

    @BeforeEach
    void setUp() {
        instructor = Instructor.builder()
                .id(50)
                .staffCode("GV050")
                .fullName("Nguyễn Văn A")
                .email("a@iu.edu.vn")
                .isActive(true)
                .build();
        user = UserAccount.builder()
                .id(500)
                .username("faculty.a")
                .email("a@iu.edu.vn")
                .instructorId(50)
                .isActive(true)
                .build();
        submittedCourse = Course.builder()
                .id(10)
                .courseCode("IT101")
                .name("Introduction to IT")
                .build();
        missingCourse = Course.builder()
                .id(20)
                .courseCode("IT102")
                .name("Programming Fundamentals")
                .build();
    }

    @Test
    void resolvesOnlyCoursesWithoutSubmittedSyllabus() {
        Syllabus submitted = Syllabus.builder()
                .id(100)
                .status(SyllabusStatus.SUBMITTED)
                .build();
        Syllabus draft = Syllabus.builder()
                .id(200)
                .status(SyllabusStatus.DRAFT)
                .build();

        List<ClassSection> assignments = List.of(
                section(1, submittedCourse, submitted),
                section(2, submittedCourse, null),
                section(3, missingCourse, draft));

        when(classSectionRepository.findActiveForDeadline("2026-2027", 1))
                .thenReturn(assignments);
        when(userAccountRepository.findByInstructorIdIn(anyCollection()))
                .thenReturn(List.of(user));

        DeadlineReminderTargetResolver.Resolution result = resolver.resolve(
                "2026-2027",
                1);

        assertThat(result.skippedWithoutUserAccount()).isZero();
        assertThat(result.targets()).hasSize(1);
        DeadlineReminderTarget target = result.targets().getFirst();
        assertThat(target.user()).isSameAs(user);
        assertThat(target.instructorId()).isEqualTo(50);
        assertThat(target.missingCourses())
                .extracting(DeadlineReminderTarget.MissingCourse::courseCode)
                .containsExactly("IT102");
    }

    @Test
    void skipsInstructorWithoutActiveLinkedUserAccount() {
        when(classSectionRepository.findActiveForDeadline("2026-2027", 1))
                .thenReturn(List.of(section(1, missingCourse, null)));
        when(userAccountRepository.findByInstructorIdIn(anyCollection()))
                .thenReturn(List.of());

        DeadlineReminderTargetResolver.Resolution result = resolver.resolve(
                "2026-2027",
                1);

        assertThat(result.targets()).isEmpty();
        assertThat(result.skippedWithoutUserAccount()).isEqualTo(1);
    }

    @Test
    void emptyAssignmentsDoNotQueryUserAccounts() {
        when(classSectionRepository.findActiveForDeadline("2026-2027", 1))
                .thenReturn(List.of());

        DeadlineReminderTargetResolver.Resolution result = resolver.resolve(
                "2026-2027",
                1);

        assertThat(result.targets()).isEmpty();
        assertThat(result.skippedWithoutUserAccount()).isZero();
        verifyNoInteractions(userAccountRepository);
    }

    private ClassSection section(
            int id,
            Course course,
            Syllabus syllabus) {
        return ClassSection.builder()
                .id(id)
                .course(course)
                .syllabus(syllabus)
                .instructor(instructor)
                .semester(1)
                .academicYear("2026-2027")
                .isActive(true)
                .build();
    }
}
