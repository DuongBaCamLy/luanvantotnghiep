package com.scse.curriculum.deadline.dto;

import java.time.LocalDateTime;

public record DeadlineReminderLogResponse(
        Long id,
        Integer deadlineRevision,
        Integer recipientUserId,
        String recipientUsername,
        String recipientEmail,
        Integer daysBefore,
        Integer missingCourseCount,
        String missingCourseCodes,
        Integer notificationId,
        Boolean emailQueued,
        LocalDateTime createdAt) {
}
