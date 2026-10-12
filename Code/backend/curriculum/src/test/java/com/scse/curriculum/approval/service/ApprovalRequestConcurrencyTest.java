package com.scse.curriculum.approval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.scse.curriculum.CurriculumApplication;
import com.scse.curriculum.approval.dto.ReviewApprovalRequest;
import com.scse.curriculum.approval.entity.*;
import com.scse.curriculum.approval.repository.ApprovalRequestRepository;
import com.scse.curriculum.auditlog.listener.AuditLogAsyncProcessor;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.email.WorkflowNotificationService;
import com.scse.curriculum.syllabus.entity.*;
import com.scse.curriculum.syllabus.history.SyllabusRevisionSnapshotRepository;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.syllabus.service.SyllabusAccessService;
import com.scse.curriculum.user.entity.*;
import com.scse.curriculum.user.repository.UserAccountRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Real MySQL transactions deliberately load the same pending entity before either
 * transaction takes the workflow lock. Authentication is tested separately.
 * The test-classpath safety guard permits only curriculum_iu_test.
 */
@SpringBootTest(classes = CurriculumApplication.class)
@ActiveProfiles("test")
class ApprovalRequestConcurrencyTest {
    @Autowired private ApprovalRequestService service;
    @Autowired private ApprovalRequestRepository approvals;
    @Autowired private SyllabusRepository syllabuses;
    @Autowired private SyllabusRevisionSnapshotRepository snapshots;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;
    @MockBean private SyllabusAccessService access;
    @MockBean private WorkflowNotificationService notifications;
    @MockBean private AuditLogAsyncProcessor audit;
    @Autowired private UserAccountRepository users;

    private final ThreadLocal<UserAccount> reviewer = new ThreadLocal<>();
    private final ThreadLocal<Boolean> initiallyLoaded = new ThreadLocal<>();

    @ParameterizedTest
    @CsvSource({
        "STEP3_DEAN,APPROVED",
        "STEP3_DEAN,REJECTED",
        "STEP2_DEAN_SECRETARY,APPROVED",
        "STEP1_DEPT_HEAD,APPROVED"
    })
    void concurrentDecisionsCommitExactlyOnce(ApprovalStep step, ApprovalStatus secondDecision) throws Exception {
        assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class)).isEqualTo("curriculum_iu_test");
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Fixture fixture = tx.execute(status -> createFixture(step));
        CountDownLatch bothLoaded = new CountDownLatch(2);

        when(access.currentUser()).thenAnswer(i -> reviewer.get());
        doAnswer(i -> {
            if (!Boolean.TRUE.equals(initiallyLoaded.get())) {
                initiallyLoaded.set(true);
                bothLoaded.countDown();
                if (!bothLoaded.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Both transactions must load before locking");
                }
            }
            return null;
        }).when(access).assertCanReview(any(ApprovalRequest.class));

        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> decide(fixture.approvalId(), fixture.first(), ApprovalStatus.APPROVED));
            var second = executor.submit(() -> decide(fixture.approvalId(), fixture.second(), secondDecision));
            List<Outcome> results = List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
            assertThat(results.stream().filter(result -> result.error() == null)).hasSize(1);
            Outcome winner = results.stream().filter(result -> result.error() == null).findFirst().orElseThrow();
            Throwable loser = results.stream().filter(result -> result.error() != null).findFirst().orElseThrow().error();
            assertThat(loser).isInstanceOf(IllegalStateException.class).hasMessageContaining("đã được xử lý");

            tx.executeWithoutResult(status -> {
                entityManager.clear();
                ApprovalRequest resolved = approvals.findById(fixture.approvalId()).orElseThrow();
                Syllabus syllabus = syllabuses.findById(fixture.syllabusId()).orElseThrow();
                assertThat(resolved.getStatus()).isEqualTo(winner.decision());
                assertThat(resolved.getReviewedBy().getId()).isEqualTo(winner.userId());
                assertThat(resolved.getResolvedAt()).isNotNull();
                assertThat(syllabus.getVersionNumber()).isEqualTo(2);
                var history = snapshots.findBySyllabusIdOrderByCapturedAtAscIdAsc(syllabus.getId());
                assertThat(history).hasSize(1);
                assertThat(history.get(0).getActor()).isEqualTo(resolved.getReviewedBy().getUsername());
                assertThat(history.get(0).getVersionNumber()).isEqualTo(2);

                if (step == ApprovalStep.STEP1_DEPT_HEAD) {
                    assertThat(syllabus.getStatus()).isEqualTo(SyllabusStatus.UNDER_REVIEW);
                    var requests = approvals.findBySyllabusId(syllabus.getId());
                    assertThat(requests).hasSize(2);
                    assertThat(requests.stream().filter(r -> r.getStatus() == ApprovalStatus.PENDING))
                            .singleElement().satisfies(r -> assertThat(r.getStep()).isEqualTo(ApprovalStep.STEP3_DEAN));
                    assertThat(history.get(0).getEventType()).isEqualTo("DEPT_HEAD_APPROVED");
                } else if (winner.decision() == ApprovalStatus.APPROVED) {
                    assertThat(syllabus.getStatus()).isEqualTo(SyllabusStatus.APPROVED);
                    assertThat(syllabus.getApprovedBy().getId()).isEqualTo(winner.userId());
                    assertThat(syllabus.getApprovedAt()).isNotNull();
                    assertThat(syllabus.getIsCurrent()).isTrue();
                    assertThat(history.get(0).getEventType()).isEqualTo("APPROVED");
                    assertThat(approvals.findBySyllabusId(syllabus.getId())).hasSize(1);
                } else {
                    assertThat(syllabus.getStatus()).isEqualTo(SyllabusStatus.REJECTED);
                    assertThat(syllabus.getApprovedBy()).isNull();
                    assertThat(syllabus.getApprovedAt()).isNull();
                    assertThat(syllabus.getIsCurrent()).isFalse();
                    assertThat(history.get(0).getEventType()).isEqualTo("REJECTED");
                }
            });
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Outcome decide(Integer approvalId, UserAccount user, ApprovalStatus decision) {
        reviewer.set(user);
        try {
            ReviewApprovalRequest request = new ReviewApprovalRequest();
            request.setStatus(decision);
            request.setComment("Concurrent workflow test");
            service.review(approvalId, request);
            return new Outcome(user.getId(), decision, null);
        } catch (Throwable failure) {
            return new Outcome(user.getId(), decision, failure);
        } finally {
            reviewer.remove();
            initiallyLoaded.remove();
        }
    }

    private Fixture createFixture(ApprovalStep step) {
        String suffix = UUID.randomUUID().toString();
        UserAccount creator = persistUser("faculty-" + suffix, UserRole.INSTRUCTOR);
        UserAccount first = persistUser("first-" + suffix,
                step == ApprovalStep.STEP1_DEPT_HEAD ? UserRole.DEPT_HEAD : UserRole.DEAN);
        UserAccount second = persistUser("second-" + suffix,
                step == ApprovalStep.STEP1_DEPT_HEAD ? UserRole.DEPT_HEAD : UserRole.DEAN_SECRETARY);
        if (step == ApprovalStep.STEP1_DEPT_HEAD) {
            persistUser("dean-" + suffix, UserRole.DEAN);
        }
        Course course = Course.builder().courseCode("CONCURRENT-" + suffix)
                .name("Workflow concurrency").nameVn("Workflow concurrency").isActive(true).build();
        entityManager.persist(course);
        Syllabus syllabus = syllabuses.saveAndFlush(Syllabus.builder().course(course).createdBy(creator)
                .versionNumber(2).versionLabel("v2.0").program("CONCURRENCY")
                .academicYear("TEST").semester("Semester 1").isCurrent(false)
                .status(step == ApprovalStep.STEP1_DEPT_HEAD ? SyllabusStatus.SUBMITTED : SyllabusStatus.UNDER_REVIEW)
                .build());
        ApprovalRequest approval = approvals.saveAndFlush(ApprovalRequest.builder().syllabus(syllabus)
                .syllabusVersionNumber(2).step(step).status(ApprovalStatus.PENDING)
                .requestedBy(creator).createdAt(LocalDateTime.now()).build());
        return new Fixture(approval.getId(), syllabus.getId(), first, second);
    }

    private UserAccount persistUser(String name, UserRole role) {
        return users.saveAndFlush(UserAccount.builder().username(name).email(name + "@test.local")
                .passwordHash("test-only").role(role).isActive(true).build());
    }

    private record Fixture(Integer approvalId, Integer syllabusId, UserAccount first, UserAccount second) {}
    private record Outcome(Integer userId, ApprovalStatus decision, Throwable error) {}
}

