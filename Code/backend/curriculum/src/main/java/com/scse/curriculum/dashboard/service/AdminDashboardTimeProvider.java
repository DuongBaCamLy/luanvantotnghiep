package com.scse.curriculum.dashboard.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

@Component
public class AdminDashboardTimeProvider {
    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}
