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
import com.scse.curriculum.instructor.entity.Instructor;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;

class AdminDashboardQueryServiceTest {

    @Test
    void aggregateDeduplicatesSectionsAndCountsOnlySubmittedWorkflowStates() {
        Department department = Department.builder()
                .id(1).code("CSE").name("Computer Science").build();
        Instructor instructor = Instructor.builder()
                .id(10).staffCode("GV010").fullName("Nguyễn Văn A")
                .email("a@iu.edu.vn").department(department).isActive(true).build();
        UserAccount account = UserAccount.builder()
                .id(20).username("faculty-a").email("a@iu.edu.vn")
                .role(UserRole.INSTRUCTOR).instructorId(10).isActive(true).build();

        Course submittedCourse = Course.builder()
                .id(100).courseCode("IT001IU").name("Introduction")
                .nameVn("Nhập môn Tin học").department(department).build();
        Course draftCourse = Course.builder()
                .id(101).courseCode("IT002IU").name("Programming")
                .nameVn("Lập trình").department(department).build();

        ClassSection submittedSection1 = section(1, submittedCourse, instructor, 1);
        ClassSection submittedSection2 = section(2, submittedCourse, instructor, 2);
        ClassSection draftSection = section(3, draftCourse, instructor, 1);

        Syllabus submitted = Syllabus.builder()
                .id(1000).course(submittedCourse).createdBy(account)
                .academicYear("2026-2027").semester("1")
                .versionNumber(1).versionLabel("v1.0")
                .status(SyllabusStatus.SUBMITTED).isCurrent(true).build();
        Syllabus draft = Syllabus.builder()
                .id(1001).course(draftCourse).createdBy(account)
                .academicYear("2026-2027").semester("1")
                .versionNumber(1).versionLabel("v1.0")
                .status(SyllabusStatus.DRAFT).isCurrent(true).build();

        CourseType required = CourseType.builder()
                .id(1).code("REQUIRED").name("Required").nameVn("Môn bắt buộc").build();

        var result = AdminDashboardQueryService.aggregate(
                List.of(submittedSection1, submittedSection2, draftSection),
                Map.of(instructor.getId(), account),
                List.of(submitted, draft),
                Map.of(submittedCourse.getId(), required, draftCourse.getId(), required),
                true);

        assertThat(result.assignmentCount()).isEqualTo(2);
        assertThat(result.submittedCount()).isEqualTo(1);
        assertThat(result.missingCount()).isEqualTo(1);
        assertThat(result.overdueCount()).isEqualTo(1);
        assertThat(result.faculties()).hasSize(1);
        assertThat(result.faculties().get(0).getMissingCount()).isEqualTo(1);
        assertThat(result.faculties().get(0).getMissingCourses())
                .extracting("courseCode").containsExactly("IT002IU");
        assertThat(result.groups()).singleElement().satisfies(group -> {
            assertThat(group.getAssignedCourses()).isEqualTo(2);
            assertThat(group.getAssignedSections()).isEqualTo(3);
            assertThat(group.getSubmittedCourses()).isEqualTo(1);
            assertThat(group.getNotSubmittedCourses()).isEqualTo(1);
            assertThat(group.getSubmissionRate()).isEqualTo(50.0);
        });
    }

    private ClassSection section(
            int id,
            Course course,
            Instructor instructor,
            int groupNumber) {
        return ClassSection.builder()
                .id(id)
                .course(course)
                .instructor(instructor)
                .academicYear("2026-2027")
                .semester(1)
                .groupNumber(groupNumber)
                .sectionType(SectionType.THEORY)
                .isActive(true)
                .build();
    }
}
