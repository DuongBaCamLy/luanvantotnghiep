package com.scse.curriculum.email;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scse.curriculum.approval.entity.ApprovalStep;
import com.scse.curriculum.notification.service.NotificationService;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkflowNotificationService {

    private final NotificationService notificationService;
    private final EmailOutboxService emailOutboxService;

    @Transactional
    public void notifySubmitted(
            Syllabus syllabus,
            Collection<UserAccount> departmentHeads) {

        notifyRecipients(
                departmentHeads,
                "Syllabus requires your review",
                courseLabel(syllabus)
                        + " syllabus submitted by " + syllabus.getCreatedBy().getUsername()
                        + " requires your review.",
                "SYLLABUS_SUBMITTED",
                EmailTone.INFO,
                "Đề cương mới cần phê duyệt",
                "Chờ Trưởng bộ môn",
                syllabus.getCreatedBy().getUsername(),
                null,
                "Review syllabus",
                "/dept-head/syllabus/" + syllabus.getId(),
                syllabus);

        notifyRecipients(
                List.of(syllabus.getCreatedBy()),
                "Nộp đề cương thành công",
                courseLabel(syllabus)
                        + " phiên bản " + syllabus.getVersionLabel()
                        + " đã được nộp và đang chờ Trưởng bộ môn xử lý.",
                "SYLLABUS_SUBMISSION_CONFIRMED",
                EmailTone.SUCCESS,
                "Nộp đề cương thành công",
                "Đã nộp",
                syllabus.getCreatedBy().getUsername(),
                null,
                "Theo dõi đề cương",
                detailPath(syllabus.getCreatedBy(), syllabus.getId()),
                syllabus);
    }

    @Transactional
    public void notifyDepartmentHeadApproved(
            Syllabus syllabus,
            UserAccount reviewer,
            Collection<UserAccount> deans) {

        notifyRecipients(
                deans,
                "Đề cương chờ Trưởng khoa duyệt",
                courseLabel(syllabus)
                        + " đã được Trưởng bộ môn duyệt và chuyển lên Trưởng khoa.",
                "SYLLABUS_WAITING_FOR_DEAN",
                EmailTone.INFO,
                "Đề cương được chuyển lên Trưởng khoa",
                "Chờ Trưởng khoa",
                reviewer.getUsername(),
                null,
                "Mở danh sách chờ duyệt",
                "/dean/approvals",
                syllabus);

        notifyRecipients(
                List.of(syllabus.getCreatedBy()),
                "Đề cương đã qua bước Trưởng bộ môn",
                courseLabel(syllabus)
                        + " đã được Trưởng bộ môn duyệt và đang chờ Trưởng khoa.",
                "SYLLABUS_DEPT_HEAD_APPROVED",
                EmailTone.SUCCESS,
                "Đề cương đã qua bước Trưởng bộ môn",
                "Đang chờ Trưởng khoa",
                reviewer.getUsername(),
                null,
                "Xem đề cương",
                detailPath(syllabus.getCreatedBy(), syllabus.getId()),
                syllabus);
    }

    @Transactional
    public void notifyRevisionRequested(
            Syllabus syllabus,
            UserAccount reviewer,
            String comment,
            ApprovalStep step,
            Collection<UserAccount> departmentHeads) {

        String level = step == ApprovalStep.STEP3_DEAN
                ? "Trưởng khoa"
                : "Trưởng bộ môn";

        notifyRecipients(
                List.of(syllabus.getCreatedBy()),
                "Đề cương cần chỉnh sửa",
                courseLabel(syllabus)
                        + " đã được " + level
                        + " trả về để chỉnh sửa.",
                "SYLLABUS_REVISION_REQUESTED",
                EmailTone.WARNING,
                "Đề cương cần được chỉnh sửa",
                "Yêu cầu chỉnh sửa",
                reviewer.getUsername(),
                comment,
                "Xem nhận xét và đề cương",
                detailPath(syllabus.getCreatedBy(), syllabus.getId()),
                syllabus);

        if (step == ApprovalStep.STEP3_DEAN) {
            notifyRecipients(
                    departmentHeads,
                    "Trưởng khoa yêu cầu chỉnh sửa đề cương",
                    courseLabel(syllabus)
                            + " đã được Trưởng khoa trả về cho giảng viên chỉnh sửa.",
                    "SYLLABUS_DEAN_REVISION_REQUESTED",
                    EmailTone.WARNING,
                    "Trưởng khoa yêu cầu chỉnh sửa",
                    "Đã trả về giảng viên",
                    reviewer.getUsername(),
                    comment,
                    "Xem đề cương",
                    "/dept-head/syllabus/" + syllabus.getId(),
                    syllabus);
        }
    }

    @Transactional
    public void notifyFullyApproved(
            Syllabus syllabus,
            UserAccount reviewer,
            Collection<UserAccount> departmentHeads) {

        notifyRecipients(
                List.of(syllabus.getCreatedBy()),
                "Đề cương đã được phê duyệt",
                courseLabel(syllabus)
                        + " đã được Trưởng khoa phê duyệt hoàn tất.",
                "SYLLABUS_APPROVED",
                EmailTone.SUCCESS,
                "Đề cương đã được phê duyệt",
                "Hoàn tất phê duyệt",
                reviewer.getUsername(),
                null,
                "Xem đề cương đã duyệt",
                detailPath(syllabus.getCreatedBy(), syllabus.getId()),
                syllabus);

        notifyRecipients(
                departmentHeads,
                "Đề cương đã hoàn tất phê duyệt",
                courseLabel(syllabus)
                        + " đã được Trưởng khoa phê duyệt chính thức.",
                "SYLLABUS_APPROVED_FOR_DEPT_HEAD",
                EmailTone.SUCCESS,
                "Đề cương đã hoàn tất phê duyệt",
                "Đã phê duyệt",
                reviewer.getUsername(),
                null,
                "Xem đề cương",
                "/dept-head/syllabus/" + syllabus.getId(),
                syllabus);
    }

    private void notifyRecipients(
            Collection<UserAccount> recipients,
            String notificationTitle,
            String notificationMessage,
            String eventType,
            EmailTone tone,
            String emailHeading,
            String statusLabel,
            String actorName,
            String comment,
            String actionLabel,
            String actionPath,
            Syllabus syllabus) {

        WorkflowEmailMessage email = new WorkflowEmailMessage(
                eventType,
                "[SCSE] " + notificationTitle + " · "
                        + syllabus.getCourse().getCourseCode(),
                emailHeading,
                statusLabel,
                tone,
                notificationMessage,
                actorName,
                comment,
                actionLabel,
                actionPath,
                syllabus.getId(),
                syllabus.getCourse().getCourseCode(),
                syllabus.getCourse().getName(),
                syllabus.getVersionLabel(),
                syllabus.getAcademicYear(),
                syllabus.getSemester());

        for (UserAccount recipient : uniqueActiveRecipients(recipients)) {
            notificationService.createNotification(
                    recipient.getId(),
                    notificationTitle,
                    notificationMessage,
                    eventType);
            emailOutboxService.enqueue(recipient, email);
        }
    }

    private List<UserAccount> uniqueActiveRecipients(
            Collection<UserAccount> recipients) {

        Map<String, UserAccount> unique = new LinkedHashMap<>();
        if (recipients == null) {
            return List.of();
        }

        for (UserAccount user : recipients) {
            if (user == null || Boolean.FALSE.equals(user.getIsActive())) {
                continue;
            }
            String key = user.getEmail() == null || user.getEmail().isBlank()
                    ? "id:" + user.getId()
                    : "email:" + user.getEmail().trim().toLowerCase(Locale.ROOT);
            unique.putIfAbsent(key, user);
        }
        return List.copyOf(unique.values());
    }

    private String detailPath(UserAccount user, Integer syllabusId) {
        UserRole role = user == null ? null : user.getRole();
        if (role == UserRole.ADMIN) {
            return "/admin/syllabus/" + syllabusId;
        }
        if (role == UserRole.DEAN) {
            return "/dean/syllabus/" + syllabusId;
        }
        if (role == UserRole.DEPT_HEAD) {
            return "/dept-head/syllabus/" + syllabusId;
        }
        return "/instructor/syllabus/" + syllabusId;
    }

    private String courseLabel(Syllabus syllabus) {
        return "Đề cương môn "
                + syllabus.getCourse().getName()
                + " (" + syllabus.getCourse().getCourseCode() + ")";
    }
}
