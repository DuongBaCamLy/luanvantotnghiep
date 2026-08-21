package com.scse.curriculum.dashboard.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.stereotype.Component;

/**
 * Tách nguồn thời gian để việc tính countdown có thể kiểm thử xác định.
 */
@Component
public class FacultyDashboardTimeProvider {

    public ZonedDateTime now(ZoneId zoneId) {
        return ZonedDateTime.now(zoneId);
    }
}
