package com.scse.curriculum.deadline.service;

import java.util.List;

import com.scse.curriculum.user.entity.UserAccount;

public record DeadlineReminderTarget(
        UserAccount user,
        Integer instructorId,
        String instructorName,
        List<MissingCourse> missingCourses) {

    public record MissingCourse(
            Integer courseId,
            String courseCode,
            String courseName) {
    }
}
