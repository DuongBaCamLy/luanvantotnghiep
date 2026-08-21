package com.scse.curriculum.deadline.dto;

import java.util.List;

public record DeadlineEscalationPreviewResponse(
        SyllabusDeadlineResponse deadline,
        boolean overdue,
        long daysOverdue,
        int recipientCount,
        int departmentCount,
        int overdueInstructorCount,
        int missingCourseCount,
        int departmentsWithoutHead,
        boolean missingDean,
        List<DepartmentPreview> departments,
        List<RecipientPreview> recipients) {

    public record DepartmentPreview(
            Integer departmentId,
            String departmentCode,
            String departmentName,
            int overdueInstructorCount,
            int missingCourseCount,
            List<LeaderPreview> departmentHeads,
            List<OverdueInstructorPreview> overdueInstructors) {
    }

    public record LeaderPreview(
            Integer userId,
            String username,
            String email) {
    }

    public record RecipientPreview(
            Integer userId,
            String username,
            String email,
            String role,
            String scopeKey,
            String scopeLabel,
            int overdueInstructorCount,
            int missingCourseCount) {
    }

    public record OverdueInstructorPreview(
            Integer instructorId,
            String instructorName,
            String instructorEmail,
            List<MissingCoursePreview> missingCourses) {
    }

    public record MissingCoursePreview(
            Integer courseId,
            String courseCode,
            String courseName) {
    }
}
