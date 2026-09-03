package com.scse.curriculum.syllabus.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.instructor.entity.Instructor;
import com.scse.curriculum.instructor.repository.InstructorRepository;
import com.scse.curriculum.major.entity.Major;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.approval.entity.ApprovalRequest;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class SyllabusAccessServiceTest {

    @Mock private CurrentUserService currentUserService;
    @Mock private InstructorRepository instructorRepository;
    @Mock private ClassSectionRepository classSectionRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private CourseProgramRepository courseProgramRepository;

    @InjectMocks private SyllabusAccessService service;

    private UserAccount instructorUser;
    private Instructor instructor;
    private Course assignedCourse;

    @BeforeEach
    void setUp() {
        instructorUser = UserAccount.builder()
                .id(10)
                .role(UserRole.INSTRUCTOR)
                .instructorId(100)
                .build();
        instructor = Instructor.builder().id(100).build();
        assignedCourse = Course.builder().id(200).courseCode("IT013IU").build();
        when(currentUserService.getCurrentUser()).thenReturn(instructorUser);
    }

    @Test
    void instructorCannotCreateWithoutASelectedAssignment() {
        assertThatThrownBy(() -> service.authorizeCreate(
                null, assignedCourse, "2026-2027", "HK1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("teaching assignment");
    }

    @Test
    void instructorCannotCreateFromAnotherInstructorsAssignment() {
        ClassSection foreignAssignment = assignment(
                Instructor.builder().id(999).build(), assignedCourse);
        when(classSectionRepository.findById(300))
                .thenReturn(Optional.of(foreignAssignment));

        assertThatThrownBy(() -> service.authorizeCreate(
                300, assignedCourse, "2026-2027", "HK1"))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("not assigned");
    }

    @Test
    void instructorCannotEditSyllabusWithoutAnActiveCourseAssignment() {
        Syllabus syllabus = Syllabus.builder()
                .id(400)
                .course(assignedCourse)
                .academicYear("2026-2027")
                .semester("HK1")
                .build();
        assertThatThrownBy(() -> service.assertCanModify(syllabus))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("assigned");
    }

    @Test
    void instructorCanEditSyllabusDirectlyLinkedToTheirActiveAssignment() {
        Syllabus syllabus = Syllabus.builder()
                .id(400)
                .course(assignedCourse)
                // Legacy import metadata intentionally differs from the
                // teaching term stored by ClassSection.
                .academicYear("CS2021")
                .semester("Semester 2")
                .build();
        when(classSectionRepository
                .existsByInstructor_IdAndSyllabus_IdAndIsActiveTrue(100, 400))
                .thenReturn(true);

        assertThatCode(() -> service.assertCanModify(syllabus))
                .doesNotThrowAnyException();
    }

    @Test
    void instructorCanViewSyllabusMatchingExactActiveAssignment() {
        Syllabus syllabus = Syllabus.builder().id(401).course(assignedCourse)
                .academicYear("2026-2027").semester("HK1").build();
        when(classSectionRepository
                .findActiveAssignmentsExact(100, 200, "2026-2027", 1))
                .thenReturn(List.of(assignment(instructor, assignedCourse)));

        assertThatCode(() -> service.assertCanView(syllabus)).doesNotThrowAnyException();
    }

    @Test
    void instructorCannotViewSyllabusOutsideAssignmentScope() {
        Syllabus syllabus = Syllabus.builder().id(402).course(assignedCourse)
                .academicYear("2026-2027").semester("HK2").build();
        when(classSectionRepository
                .findActiveAssignmentsExact(100, 200, "2026-2027", 2))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.assertCanView(syllabus))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void instructorCanCreateFromOwnActiveAssignment() {
        ClassSection ownAssignment = assignment(instructor, assignedCourse);
        when(classSectionRepository.findById(300)).thenReturn(Optional.of(ownAssignment));

        assertThatCode(() -> service.authorizeCreate(
                300, assignedCourse, "2026-2027", "HK1"))
                .doesNotThrowAnyException();
    }

    @Test
    void instructorImportUsesOwnAssignmentAsAuthoritativeContext() {
        ClassSection ownAssignment = assignment(instructor, assignedCourse);
        when(classSectionRepository.findById(300)).thenReturn(Optional.of(ownAssignment));

        SyllabusAccessService.CreationAuthorization authorization =
                service.authorizeImport(300, assignedCourse);

        assertThat(authorization.creator()).isSameAs(instructorUser);
        assertThat(authorization.course()).isSameAs(assignedCourse);
        assertThat(authorization.academicYear()).isEqualTo("2026-2027");
        assertThat(authorization.semester()).isEqualTo("HK1");
        assertThat(authorization.assignments()).containsExactly(ownAssignment);
    }

    @Test
    void instructorCannotImportWithAnotherInstructorsAssignment() {
        when(classSectionRepository.findById(300)).thenReturn(Optional.of(
                assignment(Instructor.builder().id(999).build(), assignedCourse)));

        assertThatThrownBy(() -> service.authorizeImport(300, assignedCourse))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("You are not assigned to this course and cannot create or import its syllabus.");
    }

    @Test
    void instructorCannotImportASelectedCourseDifferentFromAssignmentCourse() {
        when(classSectionRepository.findById(300))
                .thenReturn(Optional.of(assignment(instructor, assignedCourse)));
        Course unassignedCourse = Course.builder().id(201).courseCode("IT069IU").build();

        assertThatThrownBy(() -> service.authorizeImport(300, unassignedCourse))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("not assigned");
    }

    @Test
    void adminImportRetainsCurrentAssignmentFreeBehavior() {
        UserAccount admin = UserAccount.builder().id(20).role(UserRole.ADMIN).build();
        when(currentUserService.getCurrentUser()).thenReturn(admin);

        SyllabusAccessService.CreationAuthorization authorization =
                service.authorizeImport(null, assignedCourse);

        assertThat(authorization.creator()).isSameAs(admin);
        assertThat(authorization.course()).isSameAs(assignedCourse);
        assertThat(authorization.assignments()).isEmpty();
    }

    @Test
    void instructorCannotActAsReviewer() {
        assertThatThrownBy(() -> service.assertCanReview(new ApprovalRequest()))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("cannot approve or reject");
    }

    @Test
    void adminRetainsUnrestrictedSyllabusBehavior() {
        Syllabus syllabus = Syllabus.builder().id(403).course(assignedCourse).build();
        when(currentUserService.getCurrentUser()).thenReturn(
                UserAccount.builder().id(20).role(UserRole.ADMIN).build());
        assertThatCode(() -> service.assertCanView(syllabus)).doesNotThrowAnyException();
        assertThatCode(() -> service.assertCanModify(syllabus)).doesNotThrowAnyException();
    }

    @Test
    void departmentHeadCanViewOnlyOwnManagedMajorAndCannotModifyContent() {
        Major ownMajor = Major.builder().id(7).code("CS").build();
        Major foreignMajor = Major.builder().id(8).code("IT").build();
        UserAccount head = UserAccount.builder().id(21).role(UserRole.DEPT_HEAD)
                .managedMajor(ownMajor).build();
        when(currentUserService.getCurrentUser()).thenReturn(head);

        Syllabus own = Syllabus.builder().id(404)
                .course(Course.builder().id(204).build()).build();
        Syllabus foreign = Syllabus.builder().id(405)
                .course(Course.builder().id(205).build()).build();
        when(courseProgramRepository.findBySyllabus_Id(404)).thenReturn(List.of(
                CourseProgram.builder().syllabus(own)
                        .program(Program.builder().major(ownMajor).build()).build()));
        when(courseProgramRepository.findBySyllabus_Id(405)).thenReturn(List.of(
                CourseProgram.builder().syllabus(foreign)
                        .program(Program.builder().major(foreignMajor).build()).build()));

        assertThatCode(() -> service.assertCanView(own)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.assertCanView(foreign))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.assertCanModify(own))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("cannot edit");
    }

    private ClassSection assignment(Instructor owner, Course course) {
        return ClassSection.builder()
                .id(300)
                .instructor(owner)
                .course(course)
                .semester(1)
                .academicYear("2026-2027")
                .groupNumber(1)
                .sectionType(SectionType.THEORY)
                .isActive(true)
                .build();
    }
}
