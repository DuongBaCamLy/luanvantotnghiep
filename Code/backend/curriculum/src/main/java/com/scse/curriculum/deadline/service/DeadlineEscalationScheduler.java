package com.scse.curriculum.deadline.service;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeadlineEscalationScheduler {

    private final DeadlineEscalationService escalationService;
    private final DeadlineEscalationProperties properties;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info(
                "FR-05.7 deadline escalation ready: enabled={}, zone={}, startupCheck={}",
                properties.isEnabled(),
                properties.getZone(),
                properties.isStartupCheck());
        if (properties.isEnabled() && properties.isStartupCheck()) {
            runOnce();
        }
    }

    @Scheduled(
            cron = "${app.deadline-escalation.cron:0 15 * * * *}",
            zone = "${app.deadline-escalation.zone:Asia/Ho_Chi_Minh}")
    public void scheduledRun() {
        runOnce();
    }

    public int runOnce() {
        if (!properties.isEnabled()) {
            return 0;
        }
        if (!running.compareAndSet(false, true)) {
            log.debug("FR-05.7 escalation skipped because another run is active");
            return 0;
        }
        try {
            int delivered = escalationService.dispatchDueEscalations();
            log.info("FR-05.7 escalation completed: {} notification(s) queued", delivered);
            return delivered;
        } catch (Exception ex) {
            log.error("FR-05.7 escalation run failed", ex);
            return 0;
        } finally {
            running.set(false);
        }
    }
}
