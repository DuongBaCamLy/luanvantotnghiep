package com.scse.curriculum.email;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "email_outbox",
        indexes = {
            @Index(
                    name = "idx_email_outbox_ready",
                    columnList = "status,next_attempt_at,created_at"),
            @Index(
                    name = "idx_email_outbox_syllabus",
                    columnList = "syllabus_id,created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Column(name = "recipient_name", length = 255)
    private String recipientName;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(name = "html_body", nullable = false, columnDefinition = "LONGTEXT")
    private String htmlBody;

    @Column(name = "text_body", nullable = false, columnDefinition = "LONGTEXT")
    private String textBody;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "syllabus_id")
    private Integer syllabusId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailDeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) {
            status = EmailDeliveryStatus.PENDING;
        }
        if (attemptCount == null) {
            attemptCount = 0;
        }
        if (nextAttemptAt == null) {
            nextAttemptAt = now;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
