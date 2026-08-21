package com.scse.curriculum.deadline.dto;

import java.time.LocalDateTime;

public record DeadlineEscalationLogResponse(
        Long id,
        Integer deadlineRevision,
        Integer recipientUserId,
        String recipientUsername,
        String recipientEmail,
        String recipientRole,
        String scopeKey,
        Integer departmentId,
        String departmentCode,
        String departmentName,
        Integer escalationDay,
        Integer actualDaysOverdue,
        Integer overdueInstructorCount,
        Integer missingCourseCount,
        String instructorNames,
        String missingCourseCodes,
        Integer notificationId,
        Boolean emailQueued,
        LocalDateTime createdAt) {
}
