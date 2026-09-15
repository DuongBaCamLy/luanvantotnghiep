package com.scse.curriculum.deadline.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.scse.curriculum.deadline.entity.SyllabusDeadline;
import com.scse.curriculum.deadline.repository.SyllabusDeadlineReminderLogRepository;
import com.scse.curriculum.email.DeadlineEmailMessage;
import com.scse.curriculum.email.EmailOutboxService;
import com.scse.curriculum.email.EmailTone;
import com.scse.curriculum.notification.entity.Notification;
import com.scse.curriculum.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeadlineReminderDeliveryService {

    private static final DateTimeFormatter DEADLINE_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");

    private final SyllabusDeadlineReminderLogRepository reminderLogRepository;
    private final NotificationService notificationService;
    private final EmailOutboxService emailOutboxService;

    /**
     * Mỗi người nhận được xử lý trong một transaction riêng. INSERT IGNORE
     * đóng vai trò khóa idempotency, giúp hai scheduler/node không gửi trùng.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean deliver(
            SyllabusDeadline deadline,
            DeadlineReminderTarget target,
            int reminderDay,
            int actualDaysRemaining,
            LocalDateTime now) {

        String courseCodes = target.missingCourses().stream()
                .map(DeadlineReminderTarget.MissingCourse::courseCode)
                .collect(Collectors.joining(", "));

        int claimed = reminderLogRepository.tryClaimDelivery(
                deadline.getId(),
                deadline.getRevision(),
                target.user().getId(),
                reminderDay,
                target.missingCourses().size(),
                courseCodes,
                now);

        if (claimed == 0) {
            return false;
        }

        String deadlineText = deadline.getDeadlineAt().format(DEADLINE_FORMAT);
        String timingText = timingText(actualDaysRemaining);
        String title = actualDaysRemaining == 0
                ? "Deadline nộp đề cương là hôm nay"
                : "Nhắc deadline nộp đề cương";
        String message = "Hạn nộp đề cương "
                + semesterLabel(deadline.getSemester())
                + " - " + deadline.getAcademicYear()
                + " " + timingText + " (" + deadlineText + "). "
                + "Thầy/Cô còn " + target.missingCourses().size()
                + " môn chưa nộp: " + courseCodes + ".";
        String eventType = actualDaysRemaining == 0
                ? "SYLLABUS_DEADLINE_DUE_TODAY"
                : "SYLLABUS_DEADLINE_REMINDER";

        Notification notification = notificationService.createNotification(
                target.user().getId(),
                title,
                message,
                eventType);

        DeadlineEmailMessage email = new DeadlineEmailMessage(
                eventType,
                "[SCSE] " + title + " · "
                        + semesterLabel(deadline.getSemester())
                        + " " + deadline.getAcademicYear(),
                title,
                actualDaysRemaining <= 1 ? EmailTone.DANGER : EmailTone.WARNING,
                message,
                deadline.getAcademicYear(),
                semesterLabel(deadline.getSemester()),
                deadlineText,
                actualDaysRemaining,
                target.missingCourses().stream()
                        .map(course -> new DeadlineEmailMessage.MissingCourseLine(
                                course.courseCode(),
                                course.courseName()))
                        .toList(),
                "Mở danh sách đề cương",
                "/instructor/syllabus");

        boolean emailQueued = emailOutboxService.enqueueDeadlineReminder(
                target.user(),
                email);

        reminderLogRepository.markDelivered(
                deadline.getId(),
                deadline.getRevision(),
                target.user().getId(),
                reminderDay,
                notification.getId(),
                emailQueued);

        return true;
    }

    private String timingText(int daysBefore) {
        if (daysBefore <= 0) {
            return "đến hạn hôm nay";
        }
        if (daysBefore == 1) {
            return "còn 1 ngày";
        }
        return "còn " + daysBefore + " ngày";
    }

    private String semesterLabel(Integer semester) {
        return "Semester " + semester;
    }
}
