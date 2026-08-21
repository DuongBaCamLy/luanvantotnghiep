package com.scse.curriculum.deadline.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scse.curriculum.deadline.entity.SyllabusDeadlineEscalationLog;

public interface SyllabusDeadlineEscalationLogRepository
        extends JpaRepository<SyllabusDeadlineEscalationLog, Long> {

    @Modifying
    @Query(
            value = """
                    INSERT IGNORE INTO syllabus_deadline_escalation_log (
                        deadline_id,
                        deadline_revision,
                        recipient_user_id,
                        recipient_role,
                        scope_key,
                        department_id,
                        department_code,
                        department_name,
                        escalation_day,
                        actual_days_overdue,
                        overdue_instructor_count,
                        missing_course_count,
                        instructor_names,
                        missing_course_codes,
                        email_queued,
                        created_at
                    ) VALUES (
                        :deadlineId,
                        :deadlineRevision,
                        :recipientUserId,
                        :recipientRole,
                        :scopeKey,
                        :departmentId,
                        :departmentCode,
                        :departmentName,
                        :escalationDay,
                        :actualDaysOverdue,
                        :overdueInstructorCount,
                        :missingCourseCount,
                        :instructorNames,
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
            @Param("recipientRole") String recipientRole,
            @Param("scopeKey") String scopeKey,
            @Param("departmentId") Integer departmentId,
            @Param("departmentCode") String departmentCode,
            @Param("departmentName") String departmentName,
            @Param("escalationDay") Integer escalationDay,
            @Param("actualDaysOverdue") Integer actualDaysOverdue,
            @Param("overdueInstructorCount") Integer overdueInstructorCount,
            @Param("missingCourseCount") Integer missingCourseCount,
            @Param("instructorNames") String instructorNames,
            @Param("missingCourseCodes") String missingCourseCodes,
            @Param("createdAt") LocalDateTime createdAt);

    @Modifying
    @Query(
            value = """
                    UPDATE syllabus_deadline_escalation_log
                    SET notification_id = :notificationId,
                        email_queued = :emailQueued
                    WHERE deadline_id = :deadlineId
                      AND deadline_revision = :deadlineRevision
                      AND recipient_user_id = :recipientUserId
                      AND escalation_day = :escalationDay
                      AND scope_key = :scopeKey
                    """,
            nativeQuery = true)
    int markDelivered(
            @Param("deadlineId") Long deadlineId,
            @Param("deadlineRevision") Integer deadlineRevision,
            @Param("recipientUserId") Integer recipientUserId,
            @Param("escalationDay") Integer escalationDay,
            @Param("scopeKey") String scopeKey,
            @Param("notificationId") Integer notificationId,
            @Param("emailQueued") boolean emailQueued);

    List<SyllabusDeadlineEscalationLog>
            findTop200ByDeadlineIdOrderByCreatedAtDesc(Long deadlineId);

    long countByDeadlineId(Long deadlineId);
}
