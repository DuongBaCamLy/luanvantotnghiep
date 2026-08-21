package com.scse.curriculum.deadline.dto;

public record DeadlineEscalationDispatchResponse(
        Long deadlineId,
        Integer deadlineRevision,
        long daysOverdue,
        boolean forced,
        boolean escalationDue,
        Integer escalationDay,
        int recipientCount,
        int departmentCount,
        int overdueInstructorCount,
        int missingCourseCount,
        int notificationsQueued,
        int skippedAlreadySent,
        int failedDeliveries,
        int departmentsWithoutHead,
        boolean missingDean,
        String message) {
}
