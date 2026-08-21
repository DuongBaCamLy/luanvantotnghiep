package com.scse.curriculum.email;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailOutboxDispatcher {

    private final EmailOutboxRepository repository;
    private final EmailDeliveryService deliveryService;
    private final MailNotificationProperties properties;

    private final AtomicBoolean dispatching = new AtomicBoolean(false);

    /**
     * Ghi log rõ ràng và xử lý ngay email tồn đọng khi backend đã khởi động.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info(
                "FR-05.4 email dispatcher ready: enabled={}, batchSize={}, maxRetries={}",
                properties.isEnabled(),
                properties.getBatchSize(),
                properties.getMaxRetries());

        if (properties.isEnabled()) {
            dispatchNow();
        }
    }

    @Scheduled(
            fixedDelayString = "${app.mail.dispatch-interval-ms:5000}",
            initialDelayString = "${app.mail.initial-delay-ms:3000}")
    public void dispatchReadyEmails() {
        if (!properties.isEnabled()) {
            return;
        }
        dispatchNow();
    }

    /**
     * Có thể gọi từ scheduler hoặc API Admin. Trả về số email đã lấy ra xử lý.
     */
    public int dispatchNow() {
        if (!properties.isEnabled()) {
            log.warn("FR-05.4 email dispatcher is disabled by app.mail.enabled=false");
            return 0;
        }

        if (!dispatching.compareAndSet(false, true)) {
            log.debug("FR-05.4 email dispatch skipped because another run is active");
            return 0;
        }

        try {
            List<Long> readyIds = findReadyIds();
            if (readyIds.isEmpty()) {
                log.debug("FR-05.4 email dispatcher found no ready messages");
                return 0;
            }

            log.info("FR-05.4 email dispatcher processing {} message(s): {}", readyIds.size(), readyIds);
            readyIds.forEach(deliveryService::deliver);
            return readyIds.size();
        } catch (Exception ex) {
            log.error("FR-05.4 email dispatcher failed before delivery", ex);
            return 0;
        } finally {
            dispatching.set(false);
        }
    }

    private List<Long> findReadyIds() {
        int batchSize = Math.max(1, properties.getBatchSize());
        return repository.findReadyIds(
                List.of(
                        EmailDeliveryStatus.PENDING,
                        EmailDeliveryStatus.RETRY),
                LocalDateTime.now(),
                PageRequest.of(0, batchSize));
    }
}
