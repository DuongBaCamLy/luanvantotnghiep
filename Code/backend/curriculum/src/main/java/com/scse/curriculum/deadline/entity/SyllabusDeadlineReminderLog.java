package com.scse.curriculum.deadline.entity;

import java.time.LocalDateTime;

import com.scse.curriculum.user.entity.UserAccount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "syllabus_deadline_reminder_log",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_deadline_reminder_delivery",
                columnNames = {
                    "deadline_id",
                    "deadline_revision",
                    "recipient_user_id",
                    "days_before"
                }),
        indexes = {
            @Index(
                    name = "idx_deadline_reminder_log_deadline",
                    columnList = "deadline_id,created_at"),
            @Index(
                    name = "idx_deadline_reminder_log_recipient",
                    columnList = "recipient_user_id,created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyllabusDeadlineReminderLog {

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

    /**
     * 0..60 là mốc scheduler; -1 là lượt gửi thử thủ công. Giá trị này
     * tham gia khóa unique để chống gửi trùng theo deadline revision.
     */
    @Column(name = "days_before", nullable = false)
    private Integer daysBefore;

    @Column(name = "missing_course_count", nullable = false)
    private Integer missingCourseCount;

    @Column(name = "missing_course_codes", nullable = false, columnDefinition = "TEXT")
    private String missingCourseCodes;

    @Column(name = "notification_id")
    private Integer notificationId;

    @Column(name = "email_queued", nullable = false)
    private Boolean emailQueued;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
