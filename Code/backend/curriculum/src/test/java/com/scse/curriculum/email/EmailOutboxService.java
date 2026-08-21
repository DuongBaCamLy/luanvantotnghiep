package com.scse.curriculum.email;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import com.scse.curriculum.user.entity.UserAccount;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailOutboxService {

    private final EmailOutboxRepository repository;
    private final EmailTemplateService templateService;
    private final MailNotificationProperties properties;

    @Transactional
    public void enqueue(
            UserAccount recipient,
            WorkflowEmailMessage email) {

        if (recipient == null
                || recipient.getEmail() == null
                || recipient.getEmail().isBlank()) {
            return;
        }

        String recipientName = recipient.getUsername();
        String actionUrl = absoluteUrl(email.actionPath());

        EmailOutbox outbox = EmailOutbox.builder()
                .recipientEmail(recipient.getEmail().trim())
                .recipientName(recipientName)
                .subject(email.subject())
                .htmlBody(templateService.renderHtml(
                        email,
                        recipientName,
                        actionUrl))
                .textBody(templateService.renderText(
                        email,
                        recipientName,
                        actionUrl))
                .eventType(email.eventType())
                .syllabusId(email.syllabusId())
                .status(EmailDeliveryStatus.PENDING)
                .attemptCount(0)
                .nextAttemptAt(LocalDateTime.now())
                .build();

        repository.save(outbox);
    }

    @Transactional
    public boolean enqueueDeadlineReminder(
            UserAccount recipient,
            DeadlineEmailMessage email) {

        if (recipient == null
                || recipient.getEmail() == null
                || recipient.getEmail().isBlank()) {
            return false;
        }

        String recipientName = recipient.getUsername();
        String actionUrl = absoluteUrl(email.actionPath());

        EmailOutbox outbox = EmailOutbox.builder()
                .recipientEmail(recipient.getEmail().trim())
                .recipientName(recipientName)
                .subject(email.subject())
                .htmlBody(templateService.renderDeadlineHtml(
                        email,
                        recipientName,
                        actionUrl))
                .textBody(templateService.renderDeadlineText(
                        email,
                        recipientName,
                        actionUrl))
                .eventType(email.eventType())
                .syllabusId(null)
                .status(EmailDeliveryStatus.PENDING)
                .attemptCount(0)
                .nextAttemptAt(LocalDateTime.now())
                .build();

        repository.save(outbox);
        return true;
    }

    @Transactional
    public boolean enqueueDeadlineEscalation(
            UserAccount recipient,
            DeadlineEscalationEmailMessage email) {

        if (recipient == null
                || recipient.getEmail() == null
                || recipient.getEmail().isBlank()) {
            return false;
        }

        String recipientName = recipient.getUsername();
        String actionUrl = absoluteUrl(email.actionPath());

        EmailOutbox outbox = EmailOutbox.builder()
                .recipientEmail(recipient.getEmail().trim())
                .recipientName(recipientName)
                .subject(email.subject())
                .htmlBody(templateService.renderEscalationHtml(
                        email,
                        recipientName,
                        actionUrl))
                .textBody(templateService.renderEscalationText(
                        email,
                        recipientName,
                        actionUrl))
                .eventType(email.eventType())
                .syllabusId(null)
                .status(EmailDeliveryStatus.PENDING)
                .attemptCount(0)
                .nextAttemptAt(LocalDateTime.now())
                .build();

        repository.save(outbox);
        return true;
    }


    @Transactional
    public void enqueuePasswordReset(
            UserAccount recipient,
            String actionPath,
            long expirationMinutes) {

        if (recipient == null
                || recipient.getEmail() == null
                || recipient.getEmail().isBlank()) {
            return;
        }

        String actionUrl = absoluteUrl(actionPath);
        String safeActionUrl = HtmlUtils.htmlEscape(actionUrl == null ? "" : actionUrl);
        String safeName = HtmlUtils.htmlEscape(
                recipient.getUsername() == null ? "User" : recipient.getUsername());

        String subject = "Reset your SCSE Curriculum Management password";
        String textBody = "Hello " + recipient.getUsername() + ",\n\n"
                + "We received a request to reset your password.\n"
                + "Open the following link to create a new password:\n"
                + actionUrl + "\n\n"
                + "This link expires in " + expirationMinutes + " minutes and can only be used once.\n"
                + "If you did not request a password reset, you can ignore this email.\n\n"
                + "--\nSCSE Curriculum Management System";

        String htmlBody = """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1">
                  <title>Password Reset</title>
                </head>
                <body style="margin:0;padding:0;background:#eef2f7;font-family:Inter,Segoe UI,Arial,sans-serif;color:#0f172a">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#eef2f7;padding:28px 12px">
                    <tr><td align="center">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:620px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 12px 34px rgba(15,23,42,.10)">
                        <tr><td style="background:linear-gradient(135deg,#006b72,#0b8d91);padding:24px 32px">
                          <div style="color:#c9f4f3;font-size:12px;font-weight:700;letter-spacing:.12em;text-transform:uppercase">SCSE · IU</div>
                          <div style="color:#ffffff;font-size:20px;font-weight:750;margin-top:5px">Curriculum Management System</div>
                        </td></tr>
                        <tr><td style="padding:30px 32px">
                          <div style="font-size:14px;color:#64748b;margin-bottom:10px">Hello <strong style="color:#334155">%s</strong>,</div>
                          <h1 style="margin:0;font-size:24px;line-height:1.3;color:#0f172a">Reset your password</h1>
                          <p style="margin:18px 0 0;font-size:15px;line-height:1.7;color:#475569">We received a request to reset the password for your account. Use the button below to create a new password.</p>
                          <div style="padding:24px 0 18px">
                            <a href="%s" style="display:inline-block;background:#f0aa39;color:#ffffff;text-decoration:none;font-size:14px;font-weight:700;padding:13px 24px;border-radius:8px">Reset Password</a>
                          </div>
                          <p style="margin:0;font-size:13px;line-height:1.7;color:#64748b">This link expires in <strong>%d minutes</strong> and can only be used once. If you did not request this reset, you can safely ignore this email.</p>
                        </td></tr>
                        <tr><td style="border-top:1px solid #e2e8f0;background:#f8fafc;padding:18px 32px;color:#64748b;font-size:12px;line-height:1.6">This is an automated security email from the SCSE Curriculum Management System.</td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                        safeName,
                        safeActionUrl,
                        expirationMinutes);

        EmailOutbox outbox = EmailOutbox.builder()
                .recipientEmail(recipient.getEmail().trim())
                .recipientName(recipient.getUsername())
                .subject(subject)
                .htmlBody(htmlBody)
                .textBody(textBody)
                .eventType("PASSWORD_RESET")
                .syllabusId(null)
                .status(EmailDeliveryStatus.PENDING)
                .attemptCount(0)
                .nextAttemptAt(LocalDateTime.now())
                .build();

        repository.save(outbox);
    }

    private String absoluteUrl(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }

        String base = properties.getFrontendBaseUrl();
        if (base == null || base.isBlank()) {
            return path;
        }

        String normalizedBase = base.endsWith("/")
                ? base.substring(0, base.length() - 1)
                : base;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBase + normalizedPath;
    }
}
