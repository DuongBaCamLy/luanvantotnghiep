package com.scse.curriculum.email;

import java.util.List;

public record DeadlineEscalationEmailMessage(
        String eventType,
        String subject,
        String heading,
        String message,
        String academicYear,
        String semester,
        String deadlineText,
        long daysOverdue,
        String scopeLabel,
        int overdueInstructorCount,
        int missingCourseCount,
        List<OverdueInstructorLine> overdueInstructors,
        String actionLabel,
        String actionPath) {

    public record OverdueInstructorLine(
            String departmentCode,
            String departmentName,
            String instructorName,
            String instructorEmail,
            List<MissingCourseLine> missingCourses) {
    }

    public record MissingCourseLine(
            String courseCode,
            String courseName) {
    }
}
