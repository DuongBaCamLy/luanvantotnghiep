package com.scse.curriculum.email;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "app.mail")
@Getter
@Setter
public class MailNotificationProperties {

    /** Bật/tắt bộ gửi email. Email vẫn được lưu trong outbox khi tắt. */
    private boolean enabled = true;

    private String from = "no-reply@scse.local";

    private String fromName = "SCSE Curriculum Management";

    private String frontendBaseUrl = "http://localhost:5173";

    private int batchSize = 25;

    private int maxRetries = 5;
}
