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
import com.scse.curriculum.major.entity.Major;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.approval.entity.ApprovalRequest;
import com.scse.curriculum.approval.entity.ApprovalStep;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.user.repository.UserAccountRepository;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;

@ExtendWith(MockitoExtension.class)
class SyllabusAccessServiceTest {

        @Mock
        private CurrentUserService currentUserService;
        @Mock
        private ClassSectionRepository classSectionRepository;
        @Mock
        private UserAccountRepository userAccountRepository;
        @Mock
        private CourseProgramRepository courseProgramRepository;

        @InjectMocks
        private SyllabusAccessService service;

        @Mock
        private com.scse.curriculum.approval.repository.ApprovalRequestRepository approvalRequestRepository;

        private UserAccount instructorUser;
        private Course assignedCourse;

        @BeforeEach
        void setUp() {
                instructorUser = UserAccount.builder()
                                .id(10)
                                .fullName("Instructor User")
                                .username("instructor.user")
                                .role(UserRole.INSTRUCTOR)
                                .isActive(true)
                                .build();
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
                                UserAccount.builder()
                                                .id(999)
                                                .fullName("Other Instructor")
                                                .username("other.instructor")
                                                .role(UserRole.INSTRUCTOR)
                                                .isActive(true)
                                                .build(),
                                assignedCourse);
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
                                .existsByInstructorUser_IdAndSyllabus_IdAndIsActiveTrue(10, 400))
                                .thenReturn(true);

                assertThatCode(() -> service.assertCanModify(syllabus))
                                .doesNotThrowAnyException();
        }

        @Test
        void instructorCanViewSyllabusMatchingExactActiveAssignment() {
                Syllabus syllabus = Syllabus.builder().id(401).course(assignedCourse)
                                .academicYear("2026-2027").semester("HK1").build();
                when(classSectionRepository
                                .findActiveAssignmentsExact(10, 200, "2026-2027", 1))
                                .thenReturn(List.of(assignment(instructorUser, assignedCourse)));

                assertThatCode(() -> service.assertCanView(syllabus)).doesNotThrowAnyException();
        }

        @Test
        void instructorCanViewApprovedSyllabusOutsideAssignmentScope() {

                Syllabus syllabus = Syllabus.builder()
                                .id(402)
                                .course(assignedCourse)
                                .academicYear("2026-2027")
                                .semester("HK2")
                                .status(SyllabusStatus.APPROVED)
                                .build();

                assertThatCode(() -> service.assertCanView(syllabus))
                                .doesNotThrowAnyException();
        }

        @Test
        void instructorCannotViewDraftSyllabusOutsideAssignmentScope() {

                Syllabus syllabus = Syllabus.builder()
                                .id(403)
                                .course(assignedCourse)
                                .academicYear("2026-2027")
                                .semester("HK2")
                                .status(SyllabusStatus.DRAFT)
                                .build();

                when(classSectionRepository
                                .findActiveAssignmentsExact(
                                                10,
                                                200,
                                                "2026-2027",
                                                2))
                                .thenReturn(List.of());

                assertThatThrownBy(
                                () -> service.assertCanView(syllabus))
                                .isInstanceOf(
                                                ForbiddenOperationException.class);
        }

        @Test
        void instructorCannotModifyApprovedSyllabusOutsideAssignmentScope() {

                Syllabus syllabus = Syllabus.builder()
                                .id(404)
                                .course(assignedCourse)
                                .academicYear("2026-2027")
                                .semester("HK2")
                                .status(SyllabusStatus.APPROVED)
                                .build();

                assertThatThrownBy(
                                () -> service.assertCanModify(syllabus))
                                .isInstanceOf(
                                                ForbiddenOperationException.class);
        }

        @Test
        void instructorCanCreateFromOwnActiveAssignment() {
                ClassSection ownAssignment = assignment(instructorUser, assignedCourse);
                when(classSectionRepository.findById(300)).thenReturn(Optional.of(ownAssignment));

                assertThatCode(() -> service.authorizeCreate(
                                300, assignedCourse, "2026-2027", "HK1"))
                                .doesNotThrowAnyException();
        }

        @Test
        void instructorImportUsesOwnAssignmentAsAuthoritativeContext() {
                ClassSection ownAssignment = assignment(instructorUser, assignedCourse);
                when(classSectionRepository.findById(300)).thenReturn(Optional.of(ownAssignment));

                SyllabusAccessService.CreationAuthorization authorization = service.authorizeImport(300,
                                assignedCourse);

                assertThat(authorization.creator()).isSameAs(instructorUser);
                assertThat(authorization.course()).isSameAs(assignedCourse);
                assertThat(authorization.academicYear()).isEqualTo("2026-2027");
                assertThat(authorization.semester()).isEqualTo("Semester 1");
                assertThat(authorization.assignments()).containsExactly(ownAssignment);
        }

        @Test
        void instructorCannotImportWithAnotherInstructorsAssignment() {
                when(classSectionRepository.findById(300)).thenReturn(Optional.of(
                                assignment(UserAccount.builder()
                                                .id(999)
                                                .fullName("Other Instructor")
                                                .username("other.instructor")
                                                .role(UserRole.INSTRUCTOR)
                                                .isActive(true)
                                                .build(), assignedCourse)));

                assertThatThrownBy(() -> service.authorizeImport(300, assignedCourse))
                                .isInstanceOf(ForbiddenOperationException.class)
                                .hasMessage("You are not assigned to this course and cannot create or import its syllabus.");
        }

        @Test
        void instructorCannotImportASelectedCourseDifferentFromAssignmentCourse() {
                when(classSectionRepository.findById(300))
                                .thenReturn(Optional.of(assignment(instructorUser, assignedCourse)));
                Course unassignedCourse = Course.builder().id(201).courseCode("IT069IU").build();

                assertThatThrownBy(() -> service.authorizeImport(300, unassignedCourse))
                                .isInstanceOf(ForbiddenOperationException.class)
                                .hasMessageContaining("not assigned");
        }

        @Test
        void adminImportRetainsCurrentAssignmentFreeBehavior() {
                UserAccount admin = UserAccount.builder().id(20).role(UserRole.ADMIN).build();
                when(currentUserService.getCurrentUser()).thenReturn(admin);

                SyllabusAccessService.CreationAuthorization authorization = service.authorizeImport(null,
                                assignedCourse);

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
        void deanCanModifyOwnDraft() {
                UserAccount dean = UserAccount.builder()
                                .id(30)
                                .role(UserRole.DEAN)
                                .build();

                when(currentUserService.getCurrentUser()).thenReturn(dean);

                Syllabus syllabus = Syllabus.builder()
                                .id(450)
                                .status(SyllabusStatus.DRAFT)
                                .createdBy(dean)
                                .build();

                assertThatCode(() -> service.assertCanModify(syllabus))
                                .doesNotThrowAnyException();
        }

        @Test
        void deanCanViewOwnDraft() {
                UserAccount dean = UserAccount.builder()
                                .id(30)
                                .role(UserRole.DEAN)
                                .build();

                when(currentUserService.getCurrentUser()).thenReturn(dean);

                Syllabus syllabus = Syllabus.builder()
                                .id(456)
                                .course(assignedCourse)
                                .status(SyllabusStatus.DRAFT)
                                .createdBy(dean)
                                .build();

                assertThatCode(() -> service.assertCanView(syllabus))
                                .doesNotThrowAnyException();
        }

        @Test
        void deanCannotModifyAnotherDeansDraft() {
                UserAccount dean = UserAccount.builder()
                                .id(30)
                                .role(UserRole.DEAN)
                                .build();

                UserAccount otherDean = UserAccount.builder()
                                .id(31)
                                .role(UserRole.DEAN)
                                .build();

                when(currentUserService.getCurrentUser()).thenReturn(dean);

                Syllabus syllabus = Syllabus.builder()
                                .id(451)
                                .status(SyllabusStatus.DRAFT)
                                .createdBy(otherDean)
                                .build();

                assertThatThrownBy(() -> service.assertCanModify(syllabus))
                                .isInstanceOf(ForbiddenOperationException.class)
                                .hasMessageContaining("same Dean account");
        }

        @Test
        void deanCannotModifyInstructorDraft() {
                UserAccount dean = UserAccount.builder()
                                .id(30)
                                .role(UserRole.DEAN)
                                .build();

                when(currentUserService.getCurrentUser()).thenReturn(dean);

                Syllabus syllabus = Syllabus.builder()
                                .id(452)
                                .status(SyllabusStatus.DRAFT)
                                .createdBy(instructorUser)
                                .build();

                assertThatThrownBy(() -> service.assertCanModify(syllabus))
                                .isInstanceOf(ForbiddenOperationException.class);
        }

        @Test
        void deanCannotModifyOwnSubmittedSyllabus() {
                UserAccount dean = UserAccount.builder()
                                .id(30)
                                .role(UserRole.DEAN)
                                .build();

                when(currentUserService.getCurrentUser()).thenReturn(dean);

                Syllabus syllabus = Syllabus.builder()
                                .id(453)
                                .status(SyllabusStatus.SUBMITTED)
                                .createdBy(dean)
                                .build();

                assertThatThrownBy(() -> service.assertCanModify(syllabus))
                                .isInstanceOf(ForbiddenOperationException.class);
        }

        @Test
        void deanCannotModifyOwnApprovedSyllabus() {
                UserAccount dean = UserAccount.builder()
                                .id(30)
                                .role(UserRole.DEAN)
                                .build();

                when(currentUserService.getCurrentUser()).thenReturn(dean);

                Syllabus syllabus = Syllabus.builder()
                                .id(454)
                                .status(SyllabusStatus.APPROVED)
                                .createdBy(dean)
                                .build();

                assertThatThrownBy(() -> service.assertCanModify(syllabus))
                                .isInstanceOf(ForbiddenOperationException.class);
        }

        @Test
        void deanCannotModifyOwnRevisionRequestedSyllabus() {
                UserAccount dean = UserAccount.builder()
                                .id(30)
                                .role(UserRole.DEAN)
                                .build();

                when(currentUserService.getCurrentUser()).thenReturn(dean);

                Syllabus syllabus = Syllabus.builder()
                                .id(455)
                                .status(SyllabusStatus.REVISION_REQUESTED)
                                .createdBy(dean)
                                .build();

                assertThatThrownBy(() -> service.assertCanModify(syllabus))
                                .isInstanceOf(ForbiddenOperationException.class);
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

        @Test
        void departmentHeadCanReviewOnlyStep1WithinManagedMajor() {
                Major ownMajor = Major.builder()
                                .id(7)
                                .code("CS")
                                .build();

                UserAccount head = UserAccount.builder()
                                .id(21)
                                .role(UserRole.DEPT_HEAD)
                                .managedMajor(ownMajor)
                                .build();

                when(currentUserService.getCurrentUser())
                                .thenReturn(head);

                Syllabus syllabus = Syllabus.builder()
                                .id(500)
                                .course(assignedCourse)
                                .build();

                when(courseProgramRepository.findBySyllabus_Id(500))
                                .thenReturn(List.of(
                                                CourseProgram.builder()
                                                                .syllabus(syllabus)
                                                                .program(
                                                                                Program.builder()
                                                                                                .major(ownMajor)
                                                                                                .build())
                                                                .build()));

                ApprovalRequest request = ApprovalRequest.builder()
                                .step(ApprovalStep.STEP1_DEPT_HEAD)
                                .syllabus(syllabus)
                                .build();

                assertThatCode(() -> service.assertCanReview(request))
                                .doesNotThrowAnyException();
        }

        @Test
        void deanSecretaryCanReviewLegacyFinalStep() {
                UserAccount secretary = UserAccount.builder()
                                .id(22)
                                .role(UserRole.DEAN_SECRETARY)
                                .build();

                when(currentUserService.getCurrentUser())
                                .thenReturn(secretary);

                ApprovalRequest request = ApprovalRequest.builder()
                                .step(ApprovalStep.STEP2_DEAN_SECRETARY)
                                .syllabus(
                                                Syllabus.builder()
                                                                .id(501)
                                                                .course(assignedCourse)
                                                                .build())
                                .build();

                assertThatCode(() -> service.assertCanReview(request))
                                .doesNotThrowAnyException();
        }

        @Test
        void deanCanReviewOnlyFinalDeanStep() {
                UserAccount dean = UserAccount.builder()
                                .id(23)
                                .role(UserRole.DEAN)
                                .build();

                when(currentUserService.getCurrentUser())
                                .thenReturn(dean);

                ApprovalRequest request = ApprovalRequest.builder()
                                .step(ApprovalStep.STEP3_DEAN)
                                .syllabus(
                                                Syllabus.builder()
                                                                .id(502)
                                                                .course(assignedCourse)
                                                                .build())
                                .build();

                assertThatCode(() -> service.assertCanReview(request))
                                .doesNotThrowAnyException();
        }

        @Test
        void legacyProgramCoordinatorStepCannotBeReviewed() {
                UserAccount dean = UserAccount.builder()
                                .id(23)
                                .role(UserRole.DEAN)
                                .build();

                when(currentUserService.getCurrentUser())
                                .thenReturn(dean);

                ApprovalRequest request = ApprovalRequest.builder()
                                .step(ApprovalStep.STEP2_PROG_COORDINATOR)
                                .syllabus(
                                                Syllabus.builder()
                                                                .id(503)
                                                                .course(assignedCourse)
                                                                .build())
                                .build();

                assertThatThrownBy(() -> service.assertCanReview(request))
                                .isInstanceOf(
                                                ForbiddenOperationException.class)
                                .hasMessageContaining(
                                                "legacy Program Coordinator");
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.CsvSource({
                        "ADMIN,STEP1_DEPT_HEAD",
                        "ADMIN,STEP2_DEAN_SECRETARY",
                        "ADMIN,STEP3_DEAN",
                        "INSTRUCTOR,STEP1_DEPT_HEAD",
                        "INSTRUCTOR,STEP2_DEAN_SECRETARY",
                        "INSTRUCTOR,STEP3_DEAN",
                        "DEAN,STEP1_DEPT_HEAD",
                        "DEAN_SECRETARY,STEP1_DEPT_HEAD",
                        "DEPT_HEAD,STEP2_DEAN_SECRETARY",
                        "DEPT_HEAD,STEP3_DEAN"
        })
        void reviewStepRejectsWrongRoleStepCombinations(
                        UserRole role,
                        ApprovalStep step) {

                when(currentUserService.getCurrentUser())
                                .thenReturn(
                                                UserAccount.builder()
                                                                .id(50)
                                                                .role(role)
                                                                .build());

                ApprovalRequest request = ApprovalRequest.builder()
                                .step(step)
                                .syllabus(
                                                Syllabus.builder()
                                                                .id(504)
                                                                .course(assignedCourse)
                                                                .build())
                                .build();

                assertThatThrownBy(() -> service.assertCanReview(request))
                                .isInstanceOf(
                                                ForbiddenOperationException.class);
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.CsvSource({
                        "ADMIN,STEP1_DEPT_HEAD",
                        "ADMIN,STEP2_DEAN_SECRETARY",
                        "ADMIN,STEP3_DEAN",
                        "DEPT_HEAD,STEP1_DEPT_HEAD",
                        "DEAN_SECRETARY,STEP3_DEAN",
                        "DEAN,STEP3_DEAN"
        })
        void pendingQueueAllowsExpectedRoleStepCombinations(
                        UserRole role,
                        ApprovalStep step) {

                when(currentUserService.getCurrentUser())
                                .thenReturn(
                                                UserAccount.builder()
                                                                .id(60)
                                                                .role(role)
                                                                .build());

                assertThatCode(() -> service.assertCanOpenPendingStep(step))
                                .doesNotThrowAnyException();
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.CsvSource({
                        "DEPT_HEAD,STEP2_DEAN_SECRETARY",
                        "DEPT_HEAD,STEP3_DEAN",
                        "DEAN_SECRETARY,STEP1_DEPT_HEAD",
                        "DEAN_SECRETARY,STEP2_DEAN_SECRETARY",
                        "DEAN,STEP1_DEPT_HEAD",
                        "DEAN,STEP2_DEAN_SECRETARY"
        })
        void pendingQueueRejectsWrongRoleStepCombinations(
                        UserRole role,
                        ApprovalStep step) {

                when(currentUserService.getCurrentUser())
                                .thenReturn(
                                                UserAccount.builder()
                                                                .id(61)
                                                                .role(role)
                                                                .build());

                assertThatThrownBy(() -> service.assertCanOpenPendingStep(step))
                                .isInstanceOf(
                                                ForbiddenOperationException.class);
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.CsvSource({
                "DEAN,STEP3_DEAN", "DEAN_SECRETARY,STEP3_DEAN",
                "DEAN,STEP2_DEAN_SECRETARY", "DEAN_SECRETARY,STEP2_DEAN_SECRETARY"
        })
        void bothFinalRolesCanReviewAndViewCurrentSharedOrLegacyRequests(UserRole role, ApprovalStep step) {
                when(currentUserService.getCurrentUser()).thenReturn(
                        UserAccount.builder().id(70).role(role).build());
                Syllabus syllabus = Syllabus.builder().id(510).course(assignedCourse)
                        .status(SyllabusStatus.UNDER_REVIEW).versionNumber(2).build();
                ApprovalRequest request = ApprovalRequest.builder().syllabus(syllabus).step(step)
                        .status(com.scse.curriculum.approval.entity.ApprovalStatus.PENDING)
                        .syllabusVersionNumber(2).build();
                when(approvalRequestRepository.findBySyllabusId(510)).thenReturn(List.of(request));

                assertThatCode(() -> service.assertCanReview(request)).doesNotThrowAnyException();
                assertThat(service.canView(syllabus)).isTrue();
                request.setSyllabusVersionNumber(1);
                assertThat(service.canView(syllabus)).isFalse();
        }

        private ClassSection assignment(UserAccount owner, Course course) {
                return ClassSection.builder()
                                .id(300)
                                .instructorUser(owner)
                                .course(course)
                                .semester(1)
                                .academicYear("2026-2027")
                                .groupNumber(1)
                                .sectionType(SectionType.THEORY)
                                .isActive(true)
                                .build();
        }
}
