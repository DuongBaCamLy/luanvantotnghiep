package com.scse.curriculum.dashboard.service;

import com.scse.curriculum.approval.entity.ApprovalRequest;
import com.scse.curriculum.approval.entity.ApprovalStatus;
import com.scse.curriculum.approval.entity.ApprovalStep;
import com.scse.curriculum.approval.repository.ApprovalRequestRepository;
import com.scse.curriculum.auth.security.CurrentUserService;
import com.scse.curriculum.common.exception.ForbiddenOperationException;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.major.entity.Major;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeptHeadDashboardScopeTest {
    @Mock private EntityManager entityManager;
    @Mock private CurrentUserService currentUserService;
    @Mock private FacultyDashboardQueryService facultyDashboardQueryService;
    @Mock private DeanDashboardQueryService deanDashboardQueryService;
    @Mock private AdminDashboardQueryService adminDashboardQueryService;
    @Mock private ApprovalRequestRepository approvalRequestRepository;
    @Mock(answer = Answers.RETURNS_SELF) private TypedQuery<Course> courses;
    @Mock(answer = Answers.RETURNS_SELF) private TypedQuery<Syllabus> syllabuses;
    @Mock(answer = Answers.RETURNS_SELF) private TypedQuery<String> instructors;
    @InjectMocks private DashboardServiceImpl service;

    private UserAccount head() {
        return UserAccount.builder().id(10).role(UserRole.DEPT_HEAD)
                .managedMajor(Major.builder().id(7).code("CS").build()).build();
    }

    @Test
    void cannotRequestAnotherHeadAccount() {
        when(currentUserService.getCurrentUser()).thenReturn(head());
        assertThatThrownBy(() -> service.getDeptHeadDashboard(11))
                .isInstanceOf(ForbiddenOperationException.class);
        verifyNoInteractions(entityManager, approvalRequestRepository);
    }

    @Test
    void missingManagedMajorIsForbidden() {
        UserAccount head = head();
        head.setManagedMajor(null);
        when(currentUserService.getCurrentUser()).thenReturn(head);
        assertThatThrownBy(() -> service.getDeptHeadDashboard(10))
                .isInstanceOf(ForbiddenOperationException.class).hasMessageContaining("Managed Major");
        verifyNoInteractions(entityManager, approvalRequestRepository);
    }

    @Test
    void otherRolesCannotCallServiceEvenForOwnId() {
        UserAccount user = head();
        user.setRole(UserRole.INSTRUCTOR);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        assertThatThrownBy(() -> service.getDeptHeadDashboard(10))
                .isInstanceOf(ForbiddenOperationException.class);
        verifyNoInteractions(entityManager, approvalRequestRepository);
    }

    @Test
    void adminCanInspectRequestedHeadWithEmptyScope() {
        when(currentUserService.getCurrentUser()).thenReturn(UserAccount.builder().id(1).role(UserRole.ADMIN).build());
        when(entityManager.find(UserAccount.class, 10)).thenReturn(head());
        mockScopedQueries();
        assertThat(service.getDeptHeadDashboard(10).getCoursesStatus()).isEmpty();
        verify(courses).setParameter("majorId", 7);
        verify(syllabuses).setParameter("majorId", 7);
    }

    @Test
    void courseStatusInstructorAndReviewCountUseSameManagedMajor() {
        when(currentUserService.getCurrentUser()).thenReturn(head());
        mockScopedQueries();
        Course course = Course.builder().id(100).courseCode("IT001IU").name("Course").build();
        Syllabus linked = Syllabus.builder().id(20).course(course).status(SyllabusStatus.DRAFT).build();
        when(courses.getResultList()).thenReturn(List.of(course));
        when(syllabuses.getResultList()).thenReturn(List.of(linked));
        when(entityManager.createQuery(argThat(q -> q != null && q.contains("cs.program.major.id = :majorId")), eq(String.class)))
                .thenReturn(instructors);
        when(instructors.getResultList()).thenReturn(List.of("Managed major instructor"));
        when(approvalRequestRepository.findPendingByManagedMajor(ApprovalStep.STEP1_DEPT_HEAD, ApprovalStatus.PENDING, 7))
                .thenReturn(List.of(ApprovalRequest.builder().id(1).build()));

        var result = service.getDeptHeadDashboard(10);
        assertThat(result.getTotalCoursesInDept()).isEqualTo(1);
        assertThat(result.getSyllabusesToReview()).isEqualTo(1);
        assertThat(result.getCoursesStatus()).singleElement().satisfies(item -> {
            assertThat(item.getCourseCode()).isEqualTo("IT001IU");
            assertThat(item.getCourseName()).isEqualTo("Course");
            assertThat(item.getStatus()).isEqualTo("DRAFT");
            assertThat(item.getInstructorName()).isEqualTo("Managed major instructor");
        });
        verify(courses).setParameter("majorId", 7);
        verify(syllabuses).setParameter("majorId", 7);
        verify(instructors).setParameter("majorId", 7);
        verify(instructors).setParameter("syllabusId", 20);
    }

    @Test
    void uncreatedCourseInstructorLookupCannotLeakOtherMajors() {
        when(currentUserService.getCurrentUser()).thenReturn(head());
        mockScopedQueries();
        when(courses.getResultList()).thenReturn(List.of(Course.builder().id(100).courseCode("IT001IU").build()));
        when(entityManager.createQuery(argThat(q -> q != null && q.contains("cs.program.major.id = :majorId")
                && q.contains("cs.isActive = true")), eq(String.class))).thenReturn(instructors);
        var result = service.getDeptHeadDashboard(10);
        assertThat(result.getCoursesStatus().getFirst().getStatus()).isEqualTo("NOT_CREATED");
        assertThat(result.getCoursesStatus().getFirst().getInstructorName()).isEqualTo("Not assigned");
        verify(instructors).setParameter("majorId", 7);
    }

    private void mockScopedQueries() {
        when(entityManager.createQuery(argThat(q -> q != null && q.contains("cp.program.major.id = :majorId")), eq(Course.class)))
                .thenReturn(courses);
        when(entityManager.createQuery(argThat(q -> q != null && q.contains("cp.syllabus.id = s.id")
                && q.contains("cp.program.major.id = :majorId")), eq(Syllabus.class))).thenReturn(syllabuses);
    }
}
