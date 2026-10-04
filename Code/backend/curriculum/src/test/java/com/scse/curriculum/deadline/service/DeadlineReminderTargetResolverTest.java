package com.scse.curriculum.deadline.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;

@ExtendWith(MockitoExtension.class)
class DeadlineReminderTargetResolverTest {

    @Mock
    private ClassSectionRepository classSectionRepository;

    @InjectMocks
    private DeadlineReminderTargetResolver resolver;

    private UserAccount user;
    private Course submittedCourse;
    private Course missingCourse;

    @BeforeEach
    void setUp() {
        user = UserAccount.builder()
                .id(500)
                .fullName("Nguyễn Văn A")
                .username("faculty.a")
                .email("a@iu.edu.vn")
                .role(UserRole.INSTRUCTOR)
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

        DeadlineReminderTargetResolver.Resolution result = resolver.resolve(
                "2026-2027",
                1);

        assertThat(result.skippedWithoutUserAccount()).isZero();
        assertThat(result.targets()).hasSize(1);

        DeadlineReminderTarget target = result.targets().getFirst();

        assertThat(target.user()).isSameAs(user);
        assertThat(target.instructorUserId()).isEqualTo(500);
        assertThat(target.instructorName()).isEqualTo("Nguyễn Văn A");

        assertThat(target.missingCourses())
                .extracting(DeadlineReminderTarget.MissingCourse::courseCode)
                .containsExactly("IT102");
    }

    @Test
    void skipsInactiveInstructorAccount() {
        user.setIsActive(false);

        when(classSectionRepository.findActiveForDeadline("2026-2027", 1))
                .thenReturn(List.of(
                        section(1, missingCourse, null)));

        DeadlineReminderTargetResolver.Resolution result = resolver.resolve(
                "2026-2027",
                1);

        assertThat(result.targets()).isEmpty();
        assertThat(result.skippedWithoutUserAccount()).isEqualTo(1);
    }

    @Test
    void emptyAssignmentsProduceNoTargets() {
        when(classSectionRepository.findActiveForDeadline("2026-2027", 1))
                .thenReturn(List.of());

        DeadlineReminderTargetResolver.Resolution result = resolver.resolve(
                "2026-2027",
                1);

        assertThat(result.targets()).isEmpty();
        assertThat(result.skippedWithoutUserAccount()).isZero();
    }

    private ClassSection section(
            int id,
            Course course,
            Syllabus syllabus) {

        return ClassSection.builder()
                .id(id)
                .course(course)
                .syllabus(syllabus)
                .instructorUser(user)
                .semester(1)
                .academicYear("2026-2027")
                .isActive(true)
                .build();
    }
}