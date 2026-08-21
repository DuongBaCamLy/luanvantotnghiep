package com.scse.curriculum.email.dto;

import java.time.LocalDateTime;

import com.scse.curriculum.email.EmailDeliveryStatus;

public record EmailOutboxResponse(
        Long id,
        String recipientEmail,
        String recipientName,
        String subject,
        String eventType,
        Integer syllabusId,
        EmailDeliveryStatus status,
        Integer attemptCount,
        LocalDateTime nextAttemptAt,
        String lastError,
        LocalDateTime sentAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
