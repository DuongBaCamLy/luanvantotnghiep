package com.scse.curriculum.deadline.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SyllabusDeadlineResponse(
        Long id,
        String academicYear,
        Integer semester,
        LocalDateTime deadlineAt,
        List<Integer> reminderDays,
        List<Integer> escalationDays,
        Boolean active,
        Integer revision,
        Long daysRemaining,
        String state,
        String createdByUsername,
        String updatedByUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
