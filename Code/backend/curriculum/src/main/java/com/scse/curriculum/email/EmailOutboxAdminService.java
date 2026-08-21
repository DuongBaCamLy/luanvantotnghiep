package com.scse.curriculum.email;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.email.dto.EmailOutboxResponse;
import com.scse.curriculum.email.dto.EmailOutboxSummaryResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailOutboxAdminService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;

    private final EmailOutboxRepository repository;

    @Transactional(readOnly = true)
    public List<EmailOutboxResponse> getRecent(
            EmailDeliveryStatus status,
            Integer requestedLimit) {

        int limit = requestedLimit == null
                ? DEFAULT_LIMIT
                : Math.max(1, Math.min(requestedLimit, MAX_LIMIT));
        PageRequest page = PageRequest.of(0, limit);

        return (status == null
                ? repository.findAllByOrderByCreatedAtDesc(page)
                : repository.findByStatusOrderByCreatedAtDesc(status, page))
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmailOutboxSummaryResponse getSummary() {
        long pending = repository.countByStatus(EmailDeliveryStatus.PENDING);
        long retry = repository.countByStatus(EmailDeliveryStatus.RETRY);
        long sent = repository.countByStatus(EmailDeliveryStatus.SENT);
        long failed = repository.countByStatus(EmailDeliveryStatus.FAILED);

        return new EmailOutboxSummaryResponse(
                pending,
                retry,
                sent,
                failed,
                pending + retry + sent + failed);
    }

    @Transactional
    public EmailOutboxResponse retry(Long id) {
        EmailOutbox email = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Email outbox not found"));

        if (email.getStatus() == EmailDeliveryStatus.SENT) {
            throw new IllegalStateException(
                    "Email đã gửi thành công nên không thể đưa vào hàng đợi lại.");
        }
        if (email.getStatus() == EmailDeliveryStatus.PENDING) {
            throw new IllegalStateException(
                    "Email đang ở trong hàng đợi gửi.");
        }

        email.setStatus(EmailDeliveryStatus.PENDING);
        email.setAttemptCount(0);
        email.setNextAttemptAt(LocalDateTime.now());
        email.setLastError(null);
        email.setSentAt(null);

        return map(repository.save(email));
    }

    private EmailOutboxResponse map(EmailOutbox email) {
        return new EmailOutboxResponse(
                email.getId(),
                email.getRecipientEmail(),
                email.getRecipientName(),
                email.getSubject(),
                email.getEventType(),
                email.getSyllabusId(),
                email.getStatus(),
                email.getAttemptCount(),
                email.getNextAttemptAt(),
                email.getLastError(),
                email.getSentAt(),
                email.getCreatedAt(),
                email.getUpdatedAt());
    }
}
