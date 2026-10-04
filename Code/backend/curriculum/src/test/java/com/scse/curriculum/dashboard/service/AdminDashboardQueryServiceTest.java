package com.scse.curriculum.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.scse.curriculum.classsection.entity.ClassSection;
import com.scse.curriculum.classsection.entity.SectionType;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.coursetype.entity.CourseType;
import com.scse.curriculum.department.entity.Department;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;

class AdminDashboardQueryServiceTest {

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(
            value = SyllabusStatus.class,
            names = {"SUBMITTED", "DRAFT"})
    void linkedWorkflowVersionOverridesHistoricalCurrentSyllabus(
            SyllabusStatus linkedStatus) {

        UserAccount instructorUser = instructorUser(
                20,
                "faculty-a",
                "Assigned instructor",
                "a@iu.edu.vn");

        Course course = Course.builder()
                .id(100)
                .courseCode("IT001IU")
                .name("Course")
                .build();

        ClassSection assignment = section(
                1,
                course,
                instructorUser,
                1);

        Syllabus history = Syllabus.builder()
                .id(1000)
                .course(course)
                .createdBy(instructorUser)
                .academicYear("2026-2027")
                .semester("1")
                .versionNumber(99)
                .status(SyllabusStatus.APPROVED)
                .isCurrent(true)
                .build();

        Syllabus linked = Syllabus.builder()
                .id(1001)
                .course(course)
                .versionNumber(2)
                .status(linkedStatus)
                .isCurrent(false)
                .build();

        assignment.setSyllabus(linked);

        var result = AdminDashboardQueryService.aggregate(
                List.of(assignment),
                Map.of(instructorUser.getId(), instructorUser),
                List.of(history),
                Map.of(),
                true);

        assertThat(result.statusOverview().get(linkedStatus.name()))
                .isEqualTo(1L);

        assertThat(result.approvedCount())
                .isZero();

        assertThat(result.submittedCount())
                .isEqualTo(
                        linkedStatus == SyllabusStatus.SUBMITTED
                                ? 1
                                : 0);

        assertThat(result.missingCount())
                .isEqualTo(
                        linkedStatus == SyllabusStatus.DRAFT
                                ? 1
                                : 0);

        assertThat(result.overdueCount())
                .isEqualTo(
                        linkedStatus == SyllabusStatus.DRAFT
                                ? 1
                                : 0);
    }

    @Test
    void aggregateDeduplicatesSectionsAndCountsOnlySubmittedWorkflowStates() {
        Department department = Department.builder()
                .id(1)
                .code("CSE")
                .name("Computer Science")
                .build();

        UserAccount instructorUser = instructorUser(
                20,
                "faculty-a",
                "Nguyễn Văn A",
                "a@iu.edu.vn");

        Course submittedCourse = Course.builder()
                .id(100)
                .courseCode("IT001IU")
                .name("Introduction")
                .nameVn("Nhập môn Tin học")
                .department(department)
                .build();

        Course draftCourse = Course.builder()
                .id(101)
                .courseCode("IT002IU")
                .name("Programming")
                .nameVn("Lập trình")
                .department(department)
                .build();

        ClassSection submittedSection1 = section(
                1,
                submittedCourse,
                instructorUser,
                1);

        ClassSection submittedSection2 = section(
                2,
                submittedCourse,
                instructorUser,
                2);

        ClassSection draftSection = section(
                3,
                draftCourse,
                instructorUser,
                1);

        Syllabus submitted = Syllabus.builder()
                .id(1000)
                .course(submittedCourse)
                .createdBy(instructorUser)
                .academicYear("2026-2027")
                .semester("1")
                .versionNumber(1)
                .versionLabel("v1.0")
                .status(SyllabusStatus.SUBMITTED)
                .isCurrent(true)
                .build();

        Syllabus draft = Syllabus.builder()
                .id(1001)
                .course(draftCourse)
                .createdBy(instructorUser)
                .academicYear("2026-2027")
                .semester("1")
                .versionNumber(1)
                .versionLabel("v1.0")
                .status(SyllabusStatus.DRAFT)
                .isCurrent(true)
                .build();

        CourseType required = CourseType.builder()
                .id(1)
                .code("REQUIRED")
                .name("Required")
                .nameVn("Môn bắt buộc")
                .build();

        var result = AdminDashboardQueryService.aggregate(
                List.of(
                        submittedSection1,
                        submittedSection2,
                        draftSection),
                Map.of(
                        instructorUser.getId(),
                        instructorUser),
                List.of(
                        submitted,
                        draft),
                Map.of(
                        submittedCourse.getId(),
                        required,
                        draftCourse.getId(),
                        required),
                true);

        assertThat(result.assignmentCount())
                .isEqualTo(2);

        assertThat(result.submittedCount())
                .isEqualTo(1);

        assertThat(result.missingCount())
                .isEqualTo(1);

        assertThat(result.overdueCount())
                .isEqualTo(1);

        assertThat(result.faculties())
                .hasSize(1);

        assertThat(result.faculties().get(0).getInstructorId())
                .isEqualTo(instructorUser.getId());

        assertThat(result.faculties().get(0).getInstructorName())
                .isEqualTo("Nguyễn Văn A");

        assertThat(result.faculties().get(0).getMissingCount())
                .isEqualTo(1);

        assertThat(result.faculties().get(0).getMissingCourses())
                .extracting("courseCode")
                .containsExactly("IT002IU");

        assertThat(result.groups())
                .singleElement()
                .satisfies(group -> {
                    assertThat(group.getAssignedCourses())
                            .isEqualTo(2);

                    assertThat(group.getAssignedSections())
                            .isEqualTo(3);

                    assertThat(group.getSubmittedCourses())
                            .isEqualTo(1);

                    assertThat(group.getNotSubmittedCourses())
                            .isEqualTo(1);

                    assertThat(group.getSubmissionRate())
                            .isEqualTo(50.0);
                });
    }

    private UserAccount instructorUser(
            Integer id,
            String username,
            String fullName,
            String email) {

        return UserAccount.builder()
                .id(id)
                .username(username)
                .fullName(fullName)
                .email(email)
                .passwordHash("test")
                .role(UserRole.INSTRUCTOR)
                .isActive(true)
                .build();
    }

    private ClassSection section(
            int id,
            Course course,
            UserAccount instructorUser,
            int groupNumber) {

        return ClassSection.builder()
                .id(id)
                .course(course)
                .instructorUser(instructorUser)
                .academicYear("2026-2027")
                .semester(1)
                .groupNumber(groupNumber)
                .sectionType(SectionType.THEORY)
                .isActive(true)
                .build();
    }
}