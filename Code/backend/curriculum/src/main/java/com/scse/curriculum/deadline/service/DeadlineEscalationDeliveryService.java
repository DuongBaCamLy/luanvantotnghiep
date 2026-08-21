package com.scse.curriculum.deadline.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.scse.curriculum.deadline.entity.SyllabusDeadline;
import com.scse.curriculum.deadline.repository.SyllabusDeadlineEscalationLogRepository;
import com.scse.curriculum.email.DeadlineEscalationEmailMessage;
import com.scse.curriculum.email.EmailOutboxService;
import com.scse.curriculum.notification.entity.Notification;
import com.scse.curriculum.notification.service.NotificationService;
import com.scse.curriculum.user.entity.UserRole;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeadlineEscalationDeliveryService {

    private static final DateTimeFormatter DEADLINE_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");

    private final SyllabusDeadlineEscalationLogRepository escalationLogRepository;
    private final NotificationService notificationService;
    private final EmailOutboxService emailOutboxService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean deliver(
            SyllabusDeadline deadline,
            DeadlineEscalationTarget target,
            int escalationDay,
            int actualDaysOverdue,
            LocalDateTime now) {

        String instructorNames = target.overdueInstructors().stream()
                .map(DeadlineEscalationTarget.OverdueInstructor::instructorName)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));

        String courseCodes = target.overdueInstructors().stream()
                .flatMap(item -> item.missingCourses().stream())
                .map(DeadlineEscalationTarget.MissingCourse::courseCode)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));

        int claimed = escalationLogRepository.tryClaimDelivery(
                deadline.getId(),
                deadline.getRevision(),
                target.recipient().getId(),
                target.recipientRole().name(),
                target.scopeKey(),
                target.departmentId(),
                target.departmentCode(),
                target.departmentName(),
                escalationDay,
                actualDaysOverdue,
                target.overdueInstructorCount(),
                target.missingCourseCount(),
                instructorNames,
                courseCodes,
                now);

        if (claimed == 0) {
            return false;
        }

        String title = target.recipientRole() == UserRole.DEAN
                ? "Escalation đề cương quá hạn cấp khoa"
                : "Escalation đề cương quá hạn · " + target.scopeLabel();
        String deadlineText = deadline.getDeadlineAt().format(DEADLINE_FORMAT);
        String message = "Deadline nộp đề cương "
                + semesterLabel(deadline.getSemester())
                + " - " + deadline.getAcademicYear()
                + " đã quá hạn " + actualDaysOverdueText(actualDaysOverdue)
                + ". Phạm vi " + target.scopeLabel()
                + " còn " + target.overdueInstructorCount() + " giảng viên với "
                + target.missingCourseCount() + " môn chưa nộp.";

        Notification notification = notificationService.createNotification(
                target.recipient().getId(),
                title,
                message,
                "SYLLABUS_DEADLINE_ESCALATED");

        DeadlineEscalationEmailMessage email = new DeadlineEscalationEmailMessage(
                "SYLLABUS_DEADLINE_ESCALATED",
                "[SCSE] " + title + " · "
                        + semesterLabel(deadline.getSemester()) + " "
                        + deadline.getAcademicYear(),
                title,
                message,
                deadline.getAcademicYear(),
                semesterLabel(deadline.getSemester()),
                deadlineText,
                actualDaysOverdue,
                target.scopeLabel(),
                target.overdueInstructorCount(),
                target.missingCourseCount(),
                target.overdueInstructors().stream()
                        .map(this::toEmailLine)
                        .toList(),
                "Mở danh sách đề cương quá hạn",
                actionPath(target.recipientRole()));

        boolean emailQueued = emailOutboxService.enqueueDeadlineEscalation(
                target.recipient(),
                email);

        escalationLogRepository.markDelivered(
                deadline.getId(),
                deadline.getRevision(),
                target.recipient().getId(),
                escalationDay,
                target.scopeKey(),
                notification.getId(),
                emailQueued);

        return true;
    }

    private DeadlineEscalationEmailMessage.OverdueInstructorLine toEmailLine(
            DeadlineEscalationTarget.OverdueInstructor item) {
        return new DeadlineEscalationEmailMessage.OverdueInstructorLine(
                item.departmentCode(),
                item.departmentName(),
                item.instructorName(),
                item.instructorEmail(),
                item.missingCourses().stream()
                        .map(course -> new DeadlineEscalationEmailMessage.MissingCourseLine(
                                course.courseCode(),
                                course.courseName()))
                        .toList());
    }

    private String actionPath(UserRole role) {
        if (role == UserRole.DEAN) {
            return "/dean/syllabus";
        }
        if (role == UserRole.DEPT_HEAD) {
            return "/dept-head/syllabus";
        }
        return "/admin/escalations";
    }

    private String semesterLabel(Integer semester) {
        return "HK" + semester;
    }

    private String actualDaysOverdueText(int days) {
        if (days <= 0) {
            return "trong hôm nay";
        }
        if (days == 1) {
            return "1 ngày";
        }
        return days + " ngày";
    }
}
