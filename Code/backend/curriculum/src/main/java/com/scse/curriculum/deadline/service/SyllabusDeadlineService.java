package com.scse.curriculum.deadline.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scse.curriculum.auth.security.CurrentUserService;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.deadline.dto.DeadlineDispatchResponse;
import com.scse.curriculum.deadline.dto.DeadlinePreviewResponse;
import com.scse.curriculum.deadline.dto.DeadlineReminderLogResponse;
import com.scse.curriculum.deadline.dto.SyllabusDeadlineResponse;
import com.scse.curriculum.deadline.dto.UpsertSyllabusDeadlineRequest;
import com.scse.curriculum.deadline.entity.SyllabusDeadline;
import com.scse.curriculum.deadline.entity.SyllabusDeadlineReminderLog;
import com.scse.curriculum.deadline.repository.SyllabusDeadlineReminderLogRepository;
import com.scse.curriculum.deadline.repository.SyllabusDeadlineRepository;
import com.scse.curriculum.user.entity.UserAccount;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyllabusDeadlineService {

    private final SyllabusDeadlineRepository deadlineRepository;
    private final SyllabusDeadlineReminderLogRepository reminderLogRepository;
    private final CurrentUserService currentUserService;
    private final DeadlineReminderTargetResolver targetResolver;
    private final DeadlineReminderDeliveryService deliveryService;
    private final DeadlineReminderProperties properties;

    @Transactional(readOnly = true)
    public List<SyllabusDeadlineResponse> getAll() {
        LocalDateTime now = now();
        return deadlineRepository.findAllByOrderByDeadlineAtDesc()
                .stream()
                .map(deadline -> toResponse(deadline, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public SyllabusDeadlineResponse getById(Long id) {
        return toResponse(requireDeadline(id), now());
    }

    @Transactional(readOnly = true)
    public SyllabusDeadlineResponse findActiveForTerm(
            String academicYear,
            Integer semester) {
        if (academicYear == null || academicYear.isBlank() || semester == null) {
            return null;
        }
        return deadlineRepository
                .findByAcademicYearIgnoreCaseAndSemesterAndActiveTrue(
                        normalizeAcademicYear(academicYear),
                        semester)
                .map(deadline -> toResponse(deadline, now()))
                .orElse(null);
    }

    @Transactional
    public SyllabusDeadlineResponse create(
            UpsertSyllabusDeadlineRequest request) {
        UserAccount actor = currentUserService.getCurrentUser();
        String academicYear = normalizeAcademicYear(request.getAcademicYear());
        List<Integer> reminderDays = normalizeReminderDays(request.getReminderDays());
        List<Integer> escalationDays = normalizeEscalationDays(request.getEscalationDays());
        boolean active = request.getActive() == null || request.getActive();

        validateDeadlineForCreate(request.getDeadlineAt(), active);
        assertTermAvailable(academicYear, request.getSemester(), null);

        SyllabusDeadline deadline = SyllabusDeadline.builder()
                .academicYear(academicYear)
                .semester(request.getSemester())
                .deadlineAt(request.getDeadlineAt())
                .reminderDays(toCsv(reminderDays))
                .escalationDays(toCsv(escalationDays))
                .active(active)
                .revision(1)
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        return toResponse(deadlineRepository.save(deadline), now());
    }

    @Transactional
    public SyllabusDeadlineResponse update(
            Long id,
            UpsertSyllabusDeadlineRequest request) {
        SyllabusDeadline deadline = requireDeadline(id);
        UserAccount actor = currentUserService.getCurrentUser();
        String academicYear = normalizeAcademicYear(request.getAcademicYear());
        List<Integer> reminderDays = normalizeReminderDays(request.getReminderDays());
        List<Integer> escalationDays = normalizeEscalationDays(request.getEscalationDays());
        boolean active = request.getActive() == null || request.getActive();

        validateDeadlineForUpdate(deadline, request.getDeadlineAt(), active);
        assertTermAvailable(academicYear, request.getSemester(), id);

        boolean scheduleChanged = !Objects.equals(deadline.getAcademicYear(), academicYear)
                || !Objects.equals(deadline.getSemester(), request.getSemester())
                || !Objects.equals(deadline.getDeadlineAt(), request.getDeadlineAt())
                || !Objects.equals(deadline.getReminderDays(), toCsv(reminderDays))
                || !Objects.equals(deadline.getEscalationDays(), toCsv(escalationDays))
                || !Objects.equals(deadline.getActive(), active);

        deadline.setAcademicYear(academicYear);
        deadline.setSemester(request.getSemester());
        deadline.setDeadlineAt(request.getDeadlineAt());
        deadline.setReminderDays(toCsv(reminderDays));
        deadline.setEscalationDays(toCsv(escalationDays));
        deadline.setActive(active);
        deadline.setUpdatedBy(actor);
        if (scheduleChanged) {
            deadline.setRevision(deadline.getRevision() + 1);
        }

        return toResponse(deadlineRepository.save(deadline), now());
    }

    @Transactional
    public SyllabusDeadlineResponse setActive(Long id, boolean active) {
        SyllabusDeadline deadline = requireDeadline(id);
        if (!Objects.equals(deadline.getActive(), active)) {
            deadline.setActive(active);
            deadline.setRevision(deadline.getRevision() + 1);
            deadline.setUpdatedBy(currentUserService.getCurrentUser());
        }
        return toResponse(deadlineRepository.save(deadline), now());
    }

    @Transactional(readOnly = true)
    public DeadlinePreviewResponse preview(Long id) {
        SyllabusDeadline deadline = requireDeadline(id);
        DeadlineReminderTargetResolver.Resolution resolution = targetResolver.resolve(
                deadline.getAcademicYear(),
                deadline.getSemester());

        List<DeadlinePreviewResponse.RecipientPreview> recipients = resolution.targets()
                .stream()
                .map(target -> new DeadlinePreviewResponse.RecipientPreview(
                        target.user().getId(),
                        target.user().getUsername(),
                        target.user().getEmail(),
                        target.instructorId(),
                        target.instructorName(),
                        target.missingCourses().stream()
                                .map(course -> new DeadlinePreviewResponse.MissingCourse(
                                        course.courseId(),
                                        course.courseCode(),
                                        course.courseName()))
                                .toList()))
                .toList();

        int missingCourseCount = recipients.stream()
                .mapToInt(recipient -> recipient.missingCourses().size())
                .sum();

        return new DeadlinePreviewResponse(
                toResponse(deadline, now()),
                recipients.size(),
                missingCourseCount,
                resolution.skippedWithoutUserAccount(),
                recipients);
    }

    public DeadlineDispatchResponse dispatchNow(Long id, boolean force) {
        return dispatchDeadline(id, force, now());
    }

    public int dispatchDueReminders() {
        if (!properties.isEnabled()) {
            return 0;
        }

        LocalDateTime current = now();
        int delivered = 0;
        for (SyllabusDeadline deadline
                : deadlineRepository.findByActiveTrueOrderByDeadlineAtAsc()) {
            DeadlineDispatchResponse result = dispatchDeadline(
                    deadline.getId(),
                    false,
                    current);
            delivered += result.notificationsQueued();
        }
        return delivered;
    }

    /**
     * Overload có tham số now giúp unit test không phụ thuộc đồng hồ hệ thống.
     */
    public DeadlineDispatchResponse dispatchDeadline(
            Long id,
            boolean force,
            LocalDateTime current) {
        SyllabusDeadline deadline = requireDeadline(id);
        long daysRemaining = daysRemaining(current, deadline.getDeadlineAt());
        List<Integer> reminderDays = parseReminderDays(deadline.getReminderDays());
        boolean beforeOrAtDeadline = !current.isAfter(deadline.getDeadlineAt());
        Integer reminderDay = resolveDueReminderDay(
                reminderDays,
                daysRemaining,
                beforeOrAtDeadline);
        boolean reminderDue = reminderDay != null;

        if (!Boolean.TRUE.equals(deadline.getActive())) {
            return emptyDispatch(
                    deadline,
                    daysRemaining,
                    force,
                    reminderDue,
                    reminderDay,
                    "Deadline đang tắt nên không gửi nhắc.");
        }

        // FR-05.6 chỉ nhắc trước hoặc đúng hạn. Sau hạn thuộc FR-05.7.
        if (!beforeOrAtDeadline) {
            return emptyDispatch(
                    deadline,
                    daysRemaining,
                    force,
                    false,
                    null,
                    "Deadline đã qua; FR-05.7 sẽ xử lý escalation.");
        }

        if (!force && !reminderDue) {
            return emptyDispatch(
                    deadline,
                    daysRemaining,
                    false,
                    false,
                    null,
                    "Chưa đến mốc nhắc đã cấu hình.");
        }

        DeadlineReminderTargetResolver.Resolution resolution = targetResolver.resolve(
                deadline.getAcademicYear(),
                deadline.getSemester());

        int sent = 0;
        int skippedAlreadySent = 0;
        int failedDeliveries = 0;
        int actualDaysRemaining = safeDaysBefore(daysRemaining);
        // -1 là khóa idempotency riêng cho nút "Gửi thử ngay". Luôn tách
        // khỏi mốc scheduler, kể cả Admin bấm thử đúng ngày 14/7/3/1/0,
        // để thao tác kiểm thử không vô tình làm mất lượt nhắc tự động.
        int deliveryKey = force
                ? -1
                : reminderDay;
        int missingCourseCount = resolution.targets().stream()
                .mapToInt(target -> target.missingCourses().size())
                .sum();

        for (DeadlineReminderTarget target : resolution.targets()) {
            try {
                boolean delivered = deliveryService.deliver(
                        deadline,
                        target,
                        deliveryKey,
                        actualDaysRemaining,
                        current);
                if (delivered) {
                    sent++;
                } else {
                    skippedAlreadySent++;
                }
            } catch (RuntimeException ex) {
                failedDeliveries++;
                log.error(
                        "Không thể gửi FR-05.6 deadlineId={} revision={} userId={}",
                        deadline.getId(),
                        deadline.getRevision(),
                        target.user().getId(),
                        ex);
            }
        }

        String message;
        if (resolution.targets().isEmpty()) {
            message = "Không có giảng viên nào còn thiếu đề cương trong học kỳ này.";
        } else if (sent == 0 && failedDeliveries == 0) {
            message = "Tất cả người nhận phù hợp đã được nhắc ở revision/mốc này.";
        } else if (failedDeliveries > 0) {
            message = "Đã tạo " + sent + " reminder; có "
                    + failedDeliveries + " người nhận lỗi và sẽ được thử lại.";
        } else {
            message = "Đã tạo " + sent
                    + " thông báo và email reminder trong outbox.";
        }

        return new DeadlineDispatchResponse(
                deadline.getId(),
                deadline.getRevision(),
                daysRemaining,
                force,
                reminderDue,
                reminderDay,
                resolution.targets().size(),
                missingCourseCount,
                sent,
                skippedAlreadySent,
                failedDeliveries,
                resolution.skippedWithoutUserAccount(),
                message);
    }

    @Transactional(readOnly = true)
    public List<DeadlineReminderLogResponse> getReminderLogs(Long id) {
        requireDeadline(id);
        return reminderLogRepository
                .findTop100ByDeadlineIdOrderByCreatedAtDesc(id)
                .stream()
                .map(this::toLogResponse)
                .toList();
    }

    private DeadlineDispatchResponse emptyDispatch(
            SyllabusDeadline deadline,
            long daysRemaining,
            boolean force,
            boolean reminderDue,
            Integer reminderDay,
            String message) {
        return new DeadlineDispatchResponse(
                deadline.getId(),
                deadline.getRevision(),
                daysRemaining,
                force,
                reminderDue,
                reminderDay,
                0,
                0,
                0,
                0,
                0,
                0,
                message);
    }

    private SyllabusDeadline requireDeadline(Long id) {
        return deadlineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy cấu hình deadline."));
    }

    private void assertTermAvailable(
            String academicYear,
            Integer semester,
            Long currentId) {
        boolean exists = currentId == null
                ? deadlineRepository
                        .findByAcademicYearIgnoreCaseAndSemester(
                                academicYear,
                                semester)
                        .isPresent()
                : deadlineRepository
                        .existsByAcademicYearIgnoreCaseAndSemesterAndIdNot(
                                academicYear,
                                semester,
                                currentId);
        if (exists) {
            throw new IllegalStateException(
                    "Đã có deadline cho " + semesterLabel(semester)
                            + " - " + academicYear + ".");
        }
    }

    private void validateDeadlineForCreate(LocalDateTime deadlineAt, boolean active) {
        if (deadlineAt == null) {
            throw new IllegalArgumentException("Vui lòng chọn thời điểm deadline.");
        }
        if (active && !deadlineAt.isAfter(now())) {
            throw new IllegalArgumentException(
                    "Deadline mới đang hoạt động phải nằm trong tương lai.");
        }
    }

    private void validateDeadlineForUpdate(
            SyllabusDeadline existing,
            LocalDateTime requestedDeadline,
            boolean active) {
        if (requestedDeadline == null) {
            throw new IllegalArgumentException("Vui lòng chọn thời điểm deadline.");
        }
        boolean preservesOverdueDeadline = existing.getDeadlineAt() != null
                && existing.getDeadlineAt().equals(requestedDeadline)
                && !requestedDeadline.isAfter(now());
        if (active && !requestedDeadline.isAfter(now()) && !preservesOverdueDeadline) {
            throw new IllegalArgumentException(
                    "Không thể đổi deadline đang hoạt động sang một thời điểm trong quá khứ.");
        }
    }

    private List<Integer> normalizeReminderDays(List<Integer> reminderDays) {
        if (reminderDays == null || reminderDays.isEmpty()) {
            throw new IllegalArgumentException(
                    "Vui lòng cấu hình ít nhất một mốc nhắc trước hạn.");
        }
        List<Integer> normalized = reminderDays.stream()
                .filter(Objects::nonNull)
                .peek(day -> {
                    if (day < 0 || day > 60) {
                        throw new IllegalArgumentException(
                                "Mốc nhắc phải nằm trong khoảng 0 đến 60 ngày.");
                    }
                })
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "Vui lòng cấu hình ít nhất một mốc nhắc trước hạn.");
        }
        return normalized;
    }

    private List<Integer> normalizeEscalationDays(List<Integer> escalationDays) {
        List<Integer> source = escalationDays == null || escalationDays.isEmpty()
                ? List.of(0, 1, 3, 7, 14)
                : escalationDays;
        List<Integer> normalized = source.stream()
                .filter(Objects::nonNull)
                .peek(day -> {
                    if (day < 0 || day > 365) {
                        throw new IllegalArgumentException(
                                "Mốc escalation phải nằm trong khoảng 0 đến 365 ngày sau hạn.");
                    }
                })
                .distinct()
                .sorted()
                .toList();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "Vui lòng cấu hình ít nhất một mốc escalation.");
        }
        return normalized;
    }

    private String normalizeAcademicYear(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập năm học.");
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() > 50) {
            throw new IllegalArgumentException("Năm học không được vượt quá 50 ký tự.");
        }
        return normalized;
    }

    private String toCsv(List<Integer> values) {
        return values.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private List<Integer> parseReminderDays(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        List<Integer> result = new ArrayList<>();
        Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .forEach(value -> {
                    try {
                        result.add(Integer.valueOf(value));
                    } catch (NumberFormatException ignored) {
                        // Bỏ qua dữ liệu cũ không hợp lệ thay vì làm hỏng scheduler.
                    }
                });
        return result.stream()
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    private List<Integer> parseEscalationDays(String csv) {
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
                        // Bỏ qua dữ liệu cũ không hợp lệ.
                    }
                });
        return result.stream().distinct().sorted().toList();
    }

    private SyllabusDeadlineResponse toResponse(
            SyllabusDeadline deadline,
            LocalDateTime current) {
        return new SyllabusDeadlineResponse(
                deadline.getId(),
                deadline.getAcademicYear(),
                deadline.getSemester(),
                deadline.getDeadlineAt(),
                parseReminderDays(deadline.getReminderDays()),
                parseEscalationDays(deadline.getEscalationDays()),
                deadline.getActive(),
                deadline.getRevision(),
                daysRemaining(current, deadline.getDeadlineAt()),
                state(deadline, current),
                deadline.getCreatedBy() == null
                        ? null
                        : deadline.getCreatedBy().getUsername(),
                deadline.getUpdatedBy() == null
                        ? null
                        : deadline.getUpdatedBy().getUsername(),
                deadline.getCreatedAt(),
                deadline.getUpdatedAt());
    }

    private DeadlineReminderLogResponse toLogResponse(
            SyllabusDeadlineReminderLog log) {
        return new DeadlineReminderLogResponse(
                log.getId(),
                log.getDeadlineRevision(),
                log.getRecipient().getId(),
                log.getRecipient().getUsername(),
                log.getRecipient().getEmail(),
                log.getDaysBefore(),
                log.getMissingCourseCount(),
                log.getMissingCourseCodes(),
                log.getNotificationId(),
                log.getEmailQueued(),
                log.getCreatedAt());
    }

    private long daysRemaining(LocalDateTime current, LocalDateTime deadlineAt) {
        LocalDate currentDate = current.toLocalDate();
        LocalDate deadlineDate = deadlineAt.toLocalDate();
        return ChronoUnit.DAYS.between(currentDate, deadlineDate);
    }

    /**
     * Trả về mốc nhắc cần gửi. Nếu ứng dụng bị dừng đúng ngày mốc, lần chạy
     * kế tiếp sẽ gửi bù mốc gần nhất thay vì bỏ lỡ hoàn toàn. Ví dụ cấu hình
     * 14,7,3,1,0 và hôm nay còn 6 ngày thì mốc 7 ngày được xem là đến hạn.
     */
    private Integer resolveDueReminderDay(
            List<Integer> reminderDays,
            long daysRemaining,
            boolean beforeOrAtDeadline) {
        if (!beforeOrAtDeadline
                || daysRemaining < 0
                || daysRemaining > Integer.MAX_VALUE) {
            return null;
        }
        int actualDays = (int) daysRemaining;
        return reminderDays.stream()
                .filter(day -> day >= actualDays)
                .min(Integer::compareTo)
                .orElse(null);
    }

    private int safeDaysBefore(long daysRemaining) {
        if (daysRemaining < 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, daysRemaining);
    }

    private String state(SyllabusDeadline deadline, LocalDateTime current) {
        if (!Boolean.TRUE.equals(deadline.getActive())) {
            return "INACTIVE";
        }
        if (current.isAfter(deadline.getDeadlineAt())) {
            return "OVERDUE";
        }
        if (current.toLocalDate().equals(deadline.getDeadlineAt().toLocalDate())) {
            return "DUE_TODAY";
        }
        return "UPCOMING";
    }

    private String semesterLabel(Integer semester) {
        return "Semester " + semester;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(properties.zoneId());
    }
}
