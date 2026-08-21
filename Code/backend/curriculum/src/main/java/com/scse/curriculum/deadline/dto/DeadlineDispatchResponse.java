package com.scse.curriculum.deadline.dto;

public record DeadlineDispatchResponse(
        Long deadlineId,
        Integer deadlineRevision,
        long daysRemaining,
        boolean forced,
        boolean reminderDue,
        Integer reminderDay,
        int recipientCount,
        int missingCourseCount,
        int notificationsQueued,
        int skippedAlreadySent,
        int failedDeliveries,
        int skippedWithoutUserAccount,
        String message) {
}
