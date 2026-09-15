package com.scse.curriculum.syllabus.service;

import com.scse.curriculum.CurriculumApplication;
import com.scse.curriculum.approval.dto.ReviewApprovalRequest;
import com.scse.curriculum.approval.entity.*;
import com.scse.curriculum.approval.repository.ApprovalRequestRepository;
import com.scse.curriculum.approval.service.ApprovalRequestService;
import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.email.WorkflowNotificationService;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.syllabus.entity.*;
import com.scse.curriculum.syllabus.history.SyllabusHistoryService;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.user.entity.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.scse.curriculum.auditlog.listener.AuditLogAsyncProcessor;
@SpringBootTest(classes=CurriculumApplication.class)
@ActiveProfiles("test")
@Transactional
class SyllabusWorkflowDatabaseTest {
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired SyllabusService service;
    @Autowired ApprovalRequestService reviews;
    @Autowired SyllabusHistoryService history;
    @Autowired SyllabusRepository syllabuses;
    @Autowired ApprovalRequestRepository approvals;
    @MockBean SyllabusAccessService access;
    @MockBean WorkflowNotificationService notifications;
@MockBean AuditLogAsyncProcessor auditLogAsyncProcessor;
    @Test void persistedLifecycleKeepsIdAndImmutableContentThroughRevision() {
        assertThat(jdbc.queryForObject("select database()", String.class)).isEqualTo("curriculum_iu_test");
        Course course = Course.builder().courseCode("WORKFLOW_TEST").name("Workflow test").nameVn("Workflow test").build();
        em.persist(course);
        Program program = Program.builder().code("WF-2021").name("Workflow test").build(); em.persist(program);
        Cohort cohort = Cohort.builder().program(program).entryYear(2026).name("WF2026").build(); em.persist(cohort);
        UserAccount instructor = user("workflow-instructor", UserRole.INSTRUCTOR);
        UserAccount head = user("workflow-head", UserRole.DEPT_HEAD);
        UserAccount dean = user("workflow-dean", UserRole.DEAN);
        Syllabus syllabus = Syllabus.builder().course(course).program(program.getCode()).academicYear(cohort.getName())
                .semester("Semester 2").versionNumber(1).status(SyllabusStatus.DRAFT).createdBy(instructor)
                .objectives("Original learning objectives").build();
        em.persist(syllabus);
        CourseProgram link = CourseProgram.builder().course(course).program(program).cohort(cohort)
                .semesterSuggest(2).syllabus(syllabus).build(); em.persist(link); em.flush();
        int id = syllabus.getId();
        long count = syllabuses.count();
        when(access.currentUser()).thenReturn(instructor);
        when(access.findActiveDeptHeadsFor(any())).thenReturn(List.of(head));
        assertThat(service.submit(id).getId()).isEqualTo(id);
        assertThat(syllabuses.count()).isEqualTo(count);
        ApprovalRequest first = approvals.findBySyllabusIdOrderByCreatedAtDesc(id).getFirst();
        when(access.currentUser()).thenReturn(head);
        reviews.review(first.getId(), decision(ApprovalStatus.REJECTED, "Clarify learning objectives"));
        assertThat(syllabus.getStatus()).isEqualTo(SyllabusStatus.REJECTED);
        assertThat(syllabuses.count()).isEqualTo(count);
        when(access.currentUser()).thenReturn(instructor);
        assertThat(service.createRevisionDraftFromRejected(id).getId()).isEqualTo(id);
        syllabus.setObjectives("Revised learning objectives"); em.flush();
        assertThat(service.submit(id).getVersionNumber()).isEqualTo(2);
        ApprovalRequest second = approvals.findBySyllabusIdOrderByCreatedAtDesc(id).getFirst();
        when(access.currentUser()).thenReturn(head);
        reviews.review(second.getId(), decision(ApprovalStatus.APPROVED, "Objectives clarified"));
        ApprovalRequest finalReview = approvals.findBySyllabusIdOrderByCreatedAtDesc(id).getFirst();
        when(access.currentUser()).thenReturn(dean);
        reviews.review(finalReview.getId(), decision(ApprovalStatus.APPROVED, "Approved"));
        em.flush();
        assertThat(syllabuses.count()).isEqualTo(count);
        assertThat(syllabus.getStatus()).isEqualTo(SyllabusStatus.APPROVED);
        assertThat(syllabus.getVersionNumber()).isEqualTo(2);
        assertThat(jdbc.queryForObject("select syllabus_id from course_program where id=?", Integer.class, link.getId())).isEqualTo(id);
        assertThat(jdbc.queryForObject("select syllabus_version_number from approval_request where id=?", Integer.class, first.getId())).isEqualTo(1);
        var snapshots = history.history(id);
        assertThat(snapshots).hasSize(6);
        assertThat(snapshots.getFirst().content().path("syllabus").path("objectives").asText()).isEqualTo("Original learning objectives");
        assertThat(snapshots.getLast().content().path("syllabus").path("objectives").asText()).isEqualTo("Revised learning objectives");
        assertThat(snapshots).allSatisfy(snapshot -> assertThat(snapshot.content().has("student_score")).isFalse());
        assertThatThrownBy(() -> history.assertDeletable(id)).isInstanceOf(IllegalStateException.class);
    }

    private UserAccount user(String name, UserRole role) {
        UserAccount user = UserAccount.builder().username(name).email(name+"@test.invalid")
                .passwordHash("unused-test-value").role(role).isActive(true).build();
        em.persist(user); return user;
    }
    private ReviewApprovalRequest decision(ApprovalStatus status, String comment) {
        var request = new ReviewApprovalRequest(); request.setStatus(status); request.setComment(comment); return request;
    }
}
