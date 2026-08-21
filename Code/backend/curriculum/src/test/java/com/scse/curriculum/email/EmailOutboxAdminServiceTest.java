package com.scse.curriculum.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailOutboxAdminServiceTest {

    @Mock
    private EmailOutboxRepository repository;

    @InjectMocks
    private EmailOutboxAdminService service;

    @Test
    void retryResetsFailedEmailAndQueuesItImmediately() {
        EmailOutbox email = email(EmailDeliveryStatus.FAILED);
        email.setAttemptCount(5);
        email.setLastError("SMTP timeout");

        when(repository.findById(10L)).thenReturn(Optional.of(email));
        when(repository.save(email)).thenReturn(email);

        var response = service.retry(10L);

        assertThat(response.status()).isEqualTo(EmailDeliveryStatus.PENDING);
        assertThat(response.attemptCount()).isZero();
        assertThat(response.lastError()).isNull();
        assertThat(response.nextAttemptAt()).isNotNull();
        verify(repository).save(email);
    }

    @Test
    void sentEmailCannotBeQueuedAgain() {
        EmailOutbox email = email(EmailDeliveryStatus.SENT);
        when(repository.findById(10L)).thenReturn(Optional.of(email));

        assertThatThrownBy(() -> service.retry(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("đã gửi thành công");
    }

    private EmailOutbox email(EmailDeliveryStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return EmailOutbox.builder()
                .id(10L)
                .recipientEmail("faculty@iu.edu.vn")
                .recipientName("faculty")
                .subject("Syllabus notification")
                .htmlBody("<p>Hello</p>")
                .textBody("Hello")
                .eventType("SYLLABUS_APPROVED")
                .syllabusId(100)
                .status(status)
                .attemptCount(0)
                .nextAttemptAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
