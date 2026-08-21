package com.scse.curriculum.email;

import java.util.List;

public record DeadlineEmailMessage(
        String eventType,
        String subject,
        String heading,
        EmailTone tone,
        String message,
        String academicYear,
        String semester,
        String deadlineText,
        long daysRemaining,
        List<MissingCourseLine> missingCourses,
        String actionLabel,
        String actionPath) {

    public record MissingCourseLine(
            String courseCode,
            String courseName) {
    }
}
