package com.scse.curriculum.email;

public record WorkflowEmailMessage(
        String eventType,
        String subject,
        String heading,
        String statusLabel,
        EmailTone tone,
        String message,
        String actorName,
        String comment,
        String actionLabel,
        String actionPath,
        Integer syllabusId,
        String courseCode,
        String courseName,
        String versionLabel,
        String academicYear,
        String semester) {
}
