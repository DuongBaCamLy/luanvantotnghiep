package com.scse.curriculum.deadline.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scse.curriculum.classsection.entity.ClassSection;
import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.department.entity.Department;
import com.scse.curriculum.instructor.entity.Instructor;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class DeadlineEscalationTargetResolverTest {

    @Mock
    private ClassSectionRepository classSectionRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private DeadlineEscalationTargetResolver resolver;

    @Test
    void shouldSendEachDepartmentHeadOnlyTheirScopeAndDeanTheFacultyScope() {
        Department cs = department(1, "CS", "Computer Science");
        Department it = department(2, "IT", "Information Technology");

        Instructor alice = instructor(11, "Alice Nguyen", "alice@iu.edu.vn", cs);
        Instructor bob = instructor(12, "Bob Tran", "bob@iu.edu.vn", it);

        Course algorithms = course(101, "IT013IU", "Algorithms", cs);
        Course databases = course(102, "IT079IU", "Database Systems", cs);
        Course networks = course(103, "IT082IU", "Computer Networks", cs);
        Course web = course(201, "IT090IU", "Web Application Development", it);

        List<ClassSection> assignments = List.of(
                section(1, algorithms, alice, null),
                section(2, databases, alice, syllabus(301, SyllabusStatus.DRAFT)),
                section(3, networks, alice, syllabus(302, SyllabusStatus.SUBMITTED)),
                section(4, web, bob, null));

        UserAccount csHead = user(501, "head.cs", "head.cs@iu.edu.vn", UserRole.DEPT_HEAD);
        UserAccount dean = user(601, "dean.scse", "dean@iu.edu.vn", UserRole.DEAN);

        when(classSectionRepository.findActiveForDeadline("2026-2027", 1))
                .thenReturn(assignments);
        when(userAccountRepository.findActiveByRoleAndInstructorDepartmentId(
                UserRole.DEPT_HEAD, 1)).thenReturn(List.of(csHead));
        when(userAccountRepository.findActiveByRoleAndInstructorDepartmentId(
                UserRole.DEPT_HEAD, 2)).thenReturn(List.of());
        when(userAccountRepository.findByRoleAndIsActiveTrue(UserRole.DEAN))
                .thenReturn(List.of(dean));

        DeadlineEscalationTargetResolver.Resolution result =
                resolver.resolve("2026-2027", 1);

        assertThat(result.overdueInstructorCount()).isEqualTo(2);
        assertThat(result.missingCourseCount()).isEqualTo(3);
        assertThat(result.departments()).hasSize(2);
        assertThat(result.departmentsWithoutHead()).isEqualTo(1);
        assertThat(result.missingDean()).isFalse();
        assertThat(result.recipients()).hasSize(2);

        DeadlineEscalationTarget headTarget = result.recipients().stream()
                .filter(target -> target.recipientRole() == UserRole.DEPT_HEAD)
                .findFirst()
                .orElseThrow();
        assertThat(headTarget.scopeKey()).isEqualTo("DEPARTMENT:1");
        assertThat(headTarget.overdueInstructors())
                .extracting(DeadlineEscalationTarget.OverdueInstructor::instructorName)
                .containsExactly("Alice Nguyen");
        assertThat(headTarget.missingCourseCount()).isEqualTo(2);

        DeadlineEscalationTarget deanTarget = result.recipients().stream()
                .filter(target -> target.recipientRole() == UserRole.DEAN)
                .findFirst()
                .orElseThrow();
        assertThat(deanTarget.scopeKey()).isEqualTo("FACULTY");
        assertThat(deanTarget.overdueInstructors())
                .extracting(DeadlineEscalationTarget.OverdueInstructor::instructorName)
                .containsExactly("Alice Nguyen", "Bob Tran");
        assertThat(deanTarget.missingCourseCount()).isEqualTo(3);
    }

    @Test
    void shouldReturnNoRecipientsWhenEveryAssignedCourseWasSubmitted() {
        Department cs = department(1, "CS", "Computer Science");
        Instructor alice = instructor(11, "Alice Nguyen", "alice@iu.edu.vn", cs);
        Course algorithms = course(101, "IT013IU", "Algorithms", cs);

        when(classSectionRepository.findActiveForDeadline("2026-2027", 1))
                .thenReturn(List.of(section(
                        1,
                        algorithms,
                        alice,
                        syllabus(301, SyllabusStatus.APPROVED))));

        DeadlineEscalationTargetResolver.Resolution result =
                resolver.resolve("2026-2027", 1);

        assertThat(result.recipients()).isEmpty();
        assertThat(result.departments()).isEmpty();
        assertThat(result.overdueInstructorCount()).isZero();
        assertThat(result.missingCourseCount()).isZero();
        verify(userAccountRepository, never())
                .findByRoleAndIsActiveTrue(UserRole.DEAN);
    }

    @Test
    void shouldDeduplicateLeadershipAccountsByEmail() {
        Department cs = department(1, "CS", "Computer Science");
        Instructor alice = instructor(11, "Alice Nguyen", "alice@iu.edu.vn", cs);
        Course algorithms = course(101, "IT013IU", "Algorithms", cs);

        UserAccount first = user(501, "head.cs.1", "head.cs@iu.edu.vn", UserRole.DEPT_HEAD);
        UserAccount duplicate = user(502, "head.cs.2", "HEAD.CS@iu.edu.vn", UserRole.DEPT_HEAD);
        UserAccount dean = user(601, "dean.scse", "dean@iu.edu.vn", UserRole.DEAN);

        when(classSectionRepository.findActiveForDeadline("2026-2027", 1))
                .thenReturn(List.of(section(1, algorithms, alice, null)));
        when(userAccountRepository.findActiveByRoleAndInstructorDepartmentId(
                UserRole.DEPT_HEAD, 1)).thenReturn(List.of(first, duplicate));
        when(userAccountRepository.findByRoleAndIsActiveTrue(UserRole.DEAN))
                .thenReturn(List.of(dean));

        DeadlineEscalationTargetResolver.Resolution result =
                resolver.resolve("2026-2027", 1);

        assertThat(result.recipients())
                .filteredOn(target -> target.recipientRole() == UserRole.DEPT_HEAD)
                .hasSize(1);
    }

    private Department department(Integer id, String code, String name) {
        return Department.builder()
                .id(id)
                .code(code)
                .name(name)
                .isActive(true)
                .build();
    }

    private Instructor instructor(
            Integer id,
            String name,
            String email,
            Department department) {
        return Instructor.builder()
                .id(id)
                .staffCode("GV" + id)
                .fullName(name)
                .email(email)
                .department(department)
                .isActive(true)
                .build();
    }

    private Course course(
            Integer id,
            String code,
            String name,
            Department department) {
        return Course.builder()
                .id(id)
                .courseCode(code)
                .name(name)
                .nameVn(name)
                .department(department)
                .isActive(true)
                .build();
    }

    private ClassSection section(
            Integer id,
            Course course,
            Instructor instructor,
            Syllabus syllabus) {
        return ClassSection.builder()
                .id(id)
                .course(course)
                .instructor(instructor)
                .syllabus(syllabus)
                .academicYear("2026-2027")
                .semester(1)
                .groupNumber(1)
                .isActive(true)
                .build();
    }

    private Syllabus syllabus(Integer id, SyllabusStatus status) {
        return Syllabus.builder()
                .id(id)
                .status(status)
                .build();
    }

    private UserAccount user(
            Integer id,
            String username,
            String email,
            UserRole role) {
        return UserAccount.builder()
                .id(id)
                .username(username)
                .email(email)
                .passwordHash("test")
                .role(role)
                .isActive(true)
                .build();
    }
}
