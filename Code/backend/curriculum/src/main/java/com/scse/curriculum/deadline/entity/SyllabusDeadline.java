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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "syllabus_deadline",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_syllabus_deadline_term",
                columnNames = {"academic_year", "semester"}),
        indexes = {
            @Index(
                    name = "idx_syllabus_deadline_active_time",
                    columnList = "is_active,deadline_at"),
            @Index(
                    name = "idx_syllabus_deadline_term",
                    columnList = "academic_year,semester")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyllabusDeadline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "academic_year", nullable = false, length = 50)
    private String academicYear;

    @Column(nullable = false)
    private Integer semester;

    @Column(name = "deadline_at", nullable = false)
    private LocalDateTime deadlineAt;

    /**
     * Danh sách số ngày nhắc trước hạn, lưu dạng CSV đã chuẩn hóa,
     * ví dụ: 14,7,3,1,0. Số 0 nghĩa là nhắc vào đúng ngày deadline.
     */
    @Column(name = "reminder_days", nullable = false, length = 255)
    private String reminderDays;

    /**
     * Các mốc tự động escalation sau hạn, lưu CSV đã chuẩn hóa.
     * Ví dụ: 0,1,3,7,14. Số 0 nghĩa là gửi ngay trong ngày vừa quá hạn.
     */
    @Builder.Default
    @Column(name = "escalation_days", nullable = false, length = 255)
    private String escalationDays = "0,1,3,7,14";

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    /**
     * Tăng khi Admin thay đổi cấu hình lịch. Revision được đưa vào khóa
     * idempotency của reminder log để lịch mới có thể gửi lại hợp lệ.
     */
    @Column(nullable = false)
    private Integer revision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserAccount createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private UserAccount updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (active == null) {
            active = true;
        }
        if (revision == null || revision < 1) {
            revision = 1;
        }
        if (escalationDays == null || escalationDays.isBlank()) {
            escalationDays = "0,1,3,7,14";
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
