package com.scse.curriculum.deadline.service;

import java.util.List;

import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;

public record DeadlineEscalationTarget(
        UserAccount recipient,
        UserRole recipientRole,
        String scopeKey,
        Integer departmentId,
        String departmentCode,
        String departmentName,
        List<OverdueInstructor> overdueInstructors) {

    public int overdueInstructorCount() {
        return overdueInstructors == null ? 0 : overdueInstructors.size();
    }

    public int missingCourseCount() {
        if (overdueInstructors == null) {
            return 0;
        }
        return overdueInstructors.stream()
                .mapToInt(item -> item.missingCourses() == null
                        ? 0
                        : item.missingCourses().size())
                .sum();
    }

    public String scopeLabel() {
        if (recipientRole == UserRole.DEAN) {
            return "Toàn khoa";
        }
        if (departmentCode != null && !departmentCode.isBlank()) {
            return departmentCode;
        }
        return departmentName == null || departmentName.isBlank()
                ? "Bộ môn"
                : departmentName;
    }

    public record OverdueInstructor(
            Integer instructorId,
            String instructorName,
            String instructorEmail,
            Integer departmentId,
            String departmentCode,
            String departmentName,
            List<MissingCourse> missingCourses) {
    }

    public record MissingCourse(
            Integer courseId,
            String courseCode,
            String courseName) {
    }
}
