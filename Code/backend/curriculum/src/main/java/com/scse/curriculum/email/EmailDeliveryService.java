package com.scse.curriculum.email;

import java.time.LocalDateTime;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailDeliveryService {

    private static final int MAX_ERROR_LENGTH = 4000;

    private final EmailOutboxRepository repository;
    private final JavaMailSender mailSender;
    private final MailNotificationProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliver(Long outboxId) {
        EmailOutbox email = repository.findById(outboxId).orElse(null);
        if (email == null || !isReady(email)) {
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    true,
                    "UTF-8");

            helper.setFrom(
                    properties.getFrom(),
                    properties.getFromName());
            helper.setTo(email.getRecipientEmail());
            helper.setSubject(email.getSubject());
            helper.setText(email.getTextBody(), email.getHtmlBody());

            mailSender.send(mimeMessage);

            email.setAttemptCount(email.getAttemptCount() + 1);
            email.setStatus(EmailDeliveryStatus.SENT);
            email.setSentAt(LocalDateTime.now());
            email.setLastError(null);
            repository.save(email);
        } catch (Exception ex) {
            registerFailure(email, ex);
        }
    }

    private boolean isReady(EmailOutbox email) {
        return (email.getStatus() == EmailDeliveryStatus.PENDING
                || email.getStatus() == EmailDeliveryStatus.RETRY)
                && !email.getNextAttemptAt().isAfter(LocalDateTime.now());
    }

    private void registerFailure(EmailOutbox email, Exception ex) {
        int attempts = email.getAttemptCount() + 1;
        email.setAttemptCount(attempts);
        email.setLastError(truncate(errorMessage(ex)));

        if (attempts >= Math.max(1, properties.getMaxRetries())) {
            email.setStatus(EmailDeliveryStatus.FAILED);
            log.error(
                    "Email outbox {} failed permanently after {} attempts: {}",
                    email.getId(),
                    attempts,
                    email.getLastError());
        } else {
            email.setStatus(EmailDeliveryStatus.RETRY);
            email.setNextAttemptAt(
                    LocalDateTime.now().plusMinutes(backoffMinutes(attempts)));
            log.warn(
                    "Email outbox {} failed, retry {}/{} scheduled: {}",
                    email.getId(),
                    attempts,
                    properties.getMaxRetries(),
                    email.getLastError());
        }

        repository.save(email);
    }

    private long backoffMinutes(int attempt) {
        return Math.min(60L, 1L << Math.min(attempt - 1, 6));
    }

    private String errorMessage(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank()
                ? ex.getClass().getSimpleName()
                : ex.getClass().getSimpleName() + ": " + message;
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }
}
