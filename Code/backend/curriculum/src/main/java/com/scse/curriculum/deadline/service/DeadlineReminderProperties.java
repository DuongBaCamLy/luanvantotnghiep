package com.scse.curriculum.deadline.service;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "app.deadline-reminder")
@Getter
@Setter
public class DeadlineReminderProperties {

    private boolean enabled = true;

    private boolean startupCheck = true;

    private String zone = "Asia/Ho_Chi_Minh";

    public ZoneId zoneId() {
        try {
            return ZoneId.of(zone);
        } catch (Exception ignored) {
            return ZoneId.of("Asia/Ho_Chi_Minh");
        }
    }
}
