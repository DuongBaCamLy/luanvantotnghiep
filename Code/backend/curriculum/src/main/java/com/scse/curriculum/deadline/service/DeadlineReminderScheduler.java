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
public class DeadlineReminderScheduler {

    private final SyllabusDeadlineService deadlineService;
    private final DeadlineReminderProperties properties;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info(
                "FR-05.6 deadline reminder ready: enabled={}, zone={}, startupCheck={}",
                properties.isEnabled(),
                properties.getZone(),
                properties.isStartupCheck());
        if (properties.isEnabled() && properties.isStartupCheck()) {
            runOnce();
        }
    }

    @Scheduled(
            cron = "${app.deadline-reminder.cron:0 0 8 * * *}",
            zone = "${app.deadline-reminder.zone:Asia/Ho_Chi_Minh}")
    public void scheduledRun() {
        runOnce();
    }

    public int runOnce() {
        if (!properties.isEnabled()) {
            return 0;
        }
        if (!running.compareAndSet(false, true)) {
            log.debug("FR-05.6 reminder run skipped because another run is active");
            return 0;
        }
        try {
            int sent = deadlineService.dispatchDueReminders();
            log.info("FR-05.6 reminder run completed: {} notification(s) queued", sent);
            return sent;
        } catch (Exception ex) {
            log.error("FR-05.6 reminder run failed", ex);
            return 0;
        } finally {
            running.set(false);
        }
    }
}
