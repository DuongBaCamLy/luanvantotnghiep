package com.scse.curriculum.deadline.entity;

import java.time.LocalDateTime;

import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "syllabus_deadline_escalation_log",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_deadline_escalation_delivery",
                columnNames = {
                    "deadline_id",
                    "deadline_revision",
                    "recipient_user_id",
                    "escalation_day",
                    "scope_key"
                }),
        indexes = {
            @Index(
                    name = "idx_deadline_escalation_log_deadline",
                    columnList = "deadline_id,created_at"),
            @Index(
                    name = "idx_deadline_escalation_log_recipient",
                    columnList = "recipient_user_id,created_at"),
            @Index(
                    name = "idx_deadline_escalation_log_scope",
                    columnList = "scope_key,created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyllabusDeadlineEscalationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deadline_id", nullable = false)
    private SyllabusDeadline deadline;

    @Column(name = "deadline_revision", nullable = false)
    private Integer deadlineRevision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private UserAccount recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_role", nullable = false, length = 40)
    private UserRole recipientRole;

    @Column(name = "scope_key", nullable = false, length = 100)
    private String scopeKey;

    @Column(name = "department_id")
    private Integer departmentId;

    @Column(name = "department_code", length = 50)
    private String departmentCode;

    @Column(name = "department_name", length = 255)
    private String departmentName;

    /** -1 là gửi thủ công; 0..365 là mốc tự động sau hạn. */
    @Column(name = "escalation_day", nullable = false)
    private Integer escalationDay;

    @Column(name = "actual_days_overdue", nullable = false)
    private Integer actualDaysOverdue;

    @Column(name = "overdue_instructor_count", nullable = false)
    private Integer overdueInstructorCount;

    @Column(name = "missing_course_count", nullable = false)
    private Integer missingCourseCount;

    @Column(name = "instructor_names", nullable = false, columnDefinition = "TEXT")
    private String instructorNames;

    @Column(name = "missing_course_codes", nullable = false, columnDefinition = "TEXT")
    private String missingCourseCodes;

    @Column(name = "notification_id")
    private Integer notificationId;

    @Column(name = "email_queued", nullable = false)
    private Boolean emailQueued;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
