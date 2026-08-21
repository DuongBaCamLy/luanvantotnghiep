package com.scse.curriculum.dashboard.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.stereotype.Component;

/** Nguồn thời gian tách riêng để FR-06.1 có thể kiểm thử xác định. */
@Component
public class DeanDashboardTimeProvider {

    public ZonedDateTime now(ZoneId zoneId) {
        return ZonedDateTime.now(zoneId);
    }
}
