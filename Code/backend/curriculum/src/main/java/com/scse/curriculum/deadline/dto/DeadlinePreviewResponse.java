package com.scse.curriculum.deadline.dto;

import java.util.List;

public record DeadlinePreviewResponse(
        SyllabusDeadlineResponse deadline,
        int recipientCount,
        int missingCourseCount,
        int skippedWithoutUserAccount,
        List<RecipientPreview> recipients) {

    public record RecipientPreview(
            Integer userId,
            String username,
            String email,
            Integer instructorId,
            String instructorName,
            List<MissingCourse> missingCourses) {
    }

    public record MissingCourse(
            Integer courseId,
            String courseCode,
            String courseName) {
    }
}
