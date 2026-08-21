package com.scse.curriculum.deadline.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.deadline.dto.DeadlineEscalationDispatchResponse;
import com.scse.curriculum.deadline.dto.DeadlineEscalationLogResponse;
import com.scse.curriculum.deadline.dto.DeadlineEscalationPreviewResponse;
import com.scse.curriculum.deadline.dto.SyllabusDeadlineResponse;
import com.scse.curriculum.deadline.entity.SyllabusDeadline;
import com.scse.curriculum.deadline.entity.SyllabusDeadlineEscalationLog;
import com.scse.curriculum.deadline.repository.SyllabusDeadlineEscalationLogRepository;
import com.scse.curriculum.deadline.repository.SyllabusDeadlineRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadlineEscalationService {

    private final SyllabusDeadlineRepository deadlineRepository;
    private final SyllabusDeadlineEscalationLogRepository escalationLogRepository;
    private final DeadlineEscalationTargetResolver targetResolver;
    private final DeadlineEscalationDeliveryService deliveryService;
    private final DeadlineEscalationProperties properties;
    private final SyllabusDeadlineService deadlineService;

    @Transactional(readOnly = true)
    public DeadlineEscalationPreviewResponse preview(Long deadlineId) {
        SyllabusDeadline deadline = requireDeadline(deadlineId);
        LocalDateTime current = now();
        DeadlineEscalationTargetResolver.Resolution resolution =
                targetResolver.resolve(deadline.getAcademicYear(), deadline.getSemester());

        List<DeadlineEscalationPreviewResponse.DepartmentPreview> departments =
                resolution.departments().stream()
                        .map(this::toDepartmentPreview)
                        .toList();

        List<DeadlineEscalationPreviewResponse.RecipientPreview> recipients =
                resolution.recipients().stream()
                        .map(target -> new DeadlineEscalationPreviewResponse.RecipientPreview(
                                target.recipient().getId(),
                                target.recipient().getUsername(),
                                target.recipient().getEmail(),
                                target.recipientRole().name(),
                                target.scopeKey(),
                                target.scopeLabel(),
                                target.overdueInstructorCount(),
                                target.missingCourseCount()))
                        .toList();

        return new DeadlineEscalationPreviewResponse(
                deadlineService.getById(deadlineId),
                current.isAfter(deadline.getDeadlineAt()),
                daysOverdue(current, deadline.getDeadlineAt()),
                recipients.size(),
                departments.size(),
                resolution.overdueInstructorCount(),
                resolution.missingCourseCount(),
                resolution.departmentsWithoutHead(),
                resolution.missingDean(),
                departments,
                recipients);
    }

    public int dispatchDueEscalations() {
        if (!properties.isEnabled()) {
            return 0;
        }
        LocalDateTime current = now();
        int delivered = 0;
        for (SyllabusDeadline deadline : deadlineRepository
                .findByActiveTrueAndDeadlineAtBeforeOrderByDeadlineAtAsc(current)) {
            DeadlineEscalationDispatchResponse result = dispatchDeadline(
                    deadline.getId(),
                    false,
                    current);
            delivered += result.notificationsQueued();
        }
        return delivered;
    }

    public DeadlineEscalationDispatchResponse dispatchNow(Long id, boolean force) {
        return dispatchDeadline(id, force, now());
    }

    public DeadlineEscalationDispatchResponse dispatchDeadline(
            Long id,
            boolean force,
            LocalDateTime current) {
        SyllabusDeadline deadline = requireDeadline(id);
        long overdueDays = daysOverdue(current, deadline.getDeadlineAt());
        boolean overdue = current.isAfter(deadline.getDeadlineAt());

        if (!Boolean.TRUE.equals(deadline.getActive())) {
            return empty(deadline, overdueDays, force, false, null,
                    "Deadline đang tắt nên hệ thống không escalation.");
        }

        if (!overdue) {
            return empty(deadline, 0, force, false, null,
                    "Deadline chưa quá hạn nên chưa thể escalation.");
        }

        List<Integer> escalationDays = parseDays(deadline.getEscalationDays());
        Integer dueDay = resolveDueEscalationDay(escalationDays, overdueDays);
        boolean due = dueDay != null;

        if (!force && !due) {
            return empty(deadline, overdueDays, false, false, null,
                    "Chưa đến mốc escalation đã cấu hình.");
        }

        DeadlineEscalationTargetResolver.Resolution resolution =
                targetResolver.resolve(deadline.getAcademicYear(), deadline.getSemester());

        int sent = 0;
        int skipped = 0;
        int failed = 0;
        int actualDays = safeDays(overdueDays);
        int deliveryKey = force ? -1 : dueDay;

        for (DeadlineEscalationTarget target : resolution.recipients()) {
            try {
                boolean delivered = deliveryService.deliver(
                        deadline,
                        target,
                        deliveryKey,
                        actualDays,
                        current);
                if (delivered) {
                    sent++;
                } else {
                    skipped++;
                }
            } catch (RuntimeException ex) {
                failed++;
                log.error(
                        "Không thể gửi FR-05.7 deadlineId={} revision={} userId={} scope={}",
                        deadline.getId(),
                        deadline.getRevision(),
                        target.recipient().getId(),
                        target.scopeKey(),
                        ex);
            }
        }

        String message;
        if (resolution.overdueInstructorCount() == 0) {
            message = "Không còn giảng viên hoặc môn học nào chưa nộp đề cương.";
        } else if (resolution.recipients().isEmpty()) {
            message = "Có đề cương quá hạn nhưng chưa cấu hình đủ Trưởng bộ môn/Trưởng khoa để nhận escalation.";
        } else if (sent == 0 && failed == 0) {
            message = "Tất cả lãnh đạo phù hợp đã nhận escalation ở revision/mốc này.";
        } else if (failed > 0) {
            message = "Đã tạo " + sent + " escalation; có " + failed
                    + " người nhận lỗi và scheduler sẽ thử lại.";
        } else {
            message = "Đã tạo " + sent
                    + " thông báo escalation và đưa email vào outbox.";
        }

        return new DeadlineEscalationDispatchResponse(
                deadline.getId(),
                deadline.getRevision(),
                overdueDays,
                force,
                due,
                dueDay,
                resolution.recipients().size(),
                resolution.departments().size(),
                resolution.overdueInstructorCount(),
                resolution.missingCourseCount(),
                sent,
                skipped,
                failed,
                resolution.departmentsWithoutHead(),
                resolution.missingDean(),
                message);
    }

    @Transactional(readOnly = true)
    public List<DeadlineEscalationLogResponse> getLogs(Long deadlineId) {
        requireDeadline(deadlineId);
        return escalationLogRepository
                .findTop200ByDeadlineIdOrderByCreatedAtDesc(deadlineId)
                .stream()
                .map(this::toLogResponse)
                .toList();
    }

    private DeadlineEscalationPreviewResponse.DepartmentPreview toDepartmentPreview(
            DeadlineEscalationTargetResolver.DepartmentResolution department) {
        return new DeadlineEscalationPreviewResponse.DepartmentPreview(
                department.departmentId(),
                department.departmentCode(),
                department.departmentName(),
                department.overdueInstructors().size(),
                department.missingCourseCount(),
                department.departmentHeads().stream()
                        .map(user -> new DeadlineEscalationPreviewResponse.LeaderPreview(
                                user.getId(),
                                user.getUsername(),
                                user.getEmail()))
                        .toList(),
                department.overdueInstructors().stream()
                        .map(this::toInstructorPreview)
                        .toList());
    }

    private DeadlineEscalationPreviewResponse.OverdueInstructorPreview toInstructorPreview(
            DeadlineEscalationTarget.OverdueInstructor item) {
        return new DeadlineEscalationPreviewResponse.OverdueInstructorPreview(
                item.instructorId(),
                item.instructorName(),
                item.instructorEmail(),
                item.missingCourses().stream()
                        .map(course -> new DeadlineEscalationPreviewResponse.MissingCoursePreview(
                                course.courseId(),
                                course.courseCode(),
                                course.courseName()))
                        .toList());
    }

    private DeadlineEscalationLogResponse toLogResponse(
            SyllabusDeadlineEscalationLog log) {
        return new DeadlineEscalationLogResponse(
                log.getId(),
                log.getDeadlineRevision(),
                log.getRecipient().getId(),
                log.getRecipient().getUsername(),
                log.getRecipient().getEmail(),
                log.getRecipientRole().name(),
                log.getScopeKey(),
                log.getDepartmentId(),
                log.getDepartmentCode(),
                log.getDepartmentName(),
                log.getEscalationDay(),
                log.getActualDaysOverdue(),
                log.getOverdueInstructorCount(),
                log.getMissingCourseCount(),
                log.getInstructorNames(),
                log.getMissingCourseCodes(),
                log.getNotificationId(),
                log.getEmailQueued(),
                log.getCreatedAt());
    }

    private DeadlineEscalationDispatchResponse empty(
            SyllabusDeadline deadline,
            long overdueDays,
            boolean force,
            boolean due,
            Integer dueDay,
            String message) {
        return new DeadlineEscalationDispatchResponse(
                deadline.getId(),
                deadline.getRevision(),
                overdueDays,
                force,
                due,
                dueDay,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                false,
                message);
    }

    private SyllabusDeadline requireDeadline(Long id) {
        return deadlineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy cấu hình deadline."));
    }

    private List<Integer> parseDays(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of(0, 1, 3, 7, 14);
        }
        List<Integer> result = new ArrayList<>();
        Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .forEach(value -> {
                    try {
                        int day = Integer.parseInt(value);
                        if (day >= 0 && day <= 365) {
                            result.add(day);
                        }
                    } catch (NumberFormatException ignored) {
                        // Bỏ qua dữ liệu cũ không hợp lệ để scheduler vẫn hoạt động.
                    }
                });
        return result.stream()
                .distinct()
                .sorted()
                .toList();
    }

    private Integer resolveDueEscalationDay(List<Integer> days, long overdueDays) {
        if (overdueDays < 0 || overdueDays > Integer.MAX_VALUE) {
            return null;
        }
        int actual = (int) overdueDays;
        return days.stream()
                .filter(day -> day <= actual)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private long daysOverdue(LocalDateTime current, LocalDateTime deadlineAt) {
        if (!current.isAfter(deadlineAt)) {
            return 0;
        }
        LocalDate deadlineDate = deadlineAt.toLocalDate();
        LocalDate currentDate = current.toLocalDate();
        return Math.max(0, ChronoUnit.DAYS.between(deadlineDate, currentDate));
    }

    private int safeDays(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, value));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(properties.zoneId());
    }
}
