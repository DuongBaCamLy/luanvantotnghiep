package com.scse.curriculum.deadline.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scse.curriculum.deadline.entity.SyllabusDeadlineReminderLog;

public interface SyllabusDeadlineReminderLogRepository
        extends JpaRepository<SyllabusDeadlineReminderLog, Long> {

    @Modifying
    @Query(
            value = """
                    INSERT IGNORE INTO syllabus_deadline_reminder_log (
                        deadline_id,
                        deadline_revision,
                        recipient_user_id,
                        days_before,
                        missing_course_count,
                        missing_course_codes,
                        email_queued,
                        created_at
                    ) VALUES (
                        :deadlineId,
                        :deadlineRevision,
                        :recipientUserId,
                        :daysBefore,
                        :missingCourseCount,
                        :missingCourseCodes,
                        FALSE,
                        :createdAt
                    )
                    """,
            nativeQuery = true)
    int tryClaimDelivery(
            @Param("deadlineId") Long deadlineId,
            @Param("deadlineRevision") Integer deadlineRevision,
            @Param("recipientUserId") Integer recipientUserId,
            @Param("daysBefore") Integer daysBefore,
            @Param("missingCourseCount") Integer missingCourseCount,
            @Param("missingCourseCodes") String missingCourseCodes,
            @Param("createdAt") LocalDateTime createdAt);

    @Modifying
    @Query(
            value = """
                    UPDATE syllabus_deadline_reminder_log
                    SET notification_id = :notificationId,
                        email_queued = :emailQueued
                    WHERE deadline_id = :deadlineId
                      AND deadline_revision = :deadlineRevision
                      AND recipient_user_id = :recipientUserId
                      AND days_before = :daysBefore
                    """,
            nativeQuery = true)
    int markDelivered(
            @Param("deadlineId") Long deadlineId,
            @Param("deadlineRevision") Integer deadlineRevision,
            @Param("recipientUserId") Integer recipientUserId,
            @Param("daysBefore") Integer daysBefore,
            @Param("notificationId") Integer notificationId,
            @Param("emailQueued") boolean emailQueued);

    List<SyllabusDeadlineReminderLog> findTop100ByDeadlineIdOrderByCreatedAtDesc(
            Long deadlineId);
}
