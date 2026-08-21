package com.scse.curriculum.email.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scse.curriculum.email.EmailDeliveryStatus;
import com.scse.curriculum.email.EmailOutboxAdminService;
import com.scse.curriculum.email.EmailOutboxDispatcher;
import com.scse.curriculum.email.dto.EmailOutboxResponse;
import com.scse.curriculum.email.dto.EmailOutboxSummaryResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/email-outbox")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EmailOutboxAdminController {

    private final EmailOutboxAdminService service;
    private final EmailOutboxDispatcher dispatcher;

    @GetMapping
    public List<EmailOutboxResponse> getRecent(
            @RequestParam(required = false) EmailDeliveryStatus status,
            @RequestParam(required = false) Integer limit) {
        return service.getRecent(status, limit);
    }

    @GetMapping("/summary")
    public EmailOutboxSummaryResponse getSummary() {
        return service.getSummary();
    }

    @PostMapping("/{id}/retry")
    public EmailOutboxResponse retry(@PathVariable Long id) {
        return service.retry(id);
    }

    @PostMapping("/dispatch-now")
    public Map<String, Object> dispatchNow() {
        int processed = dispatcher.dispatchNow();
        return Map.of(
                "processed", processed,
                "message", processed == 0
                        ? "Không có email sẵn sàng hoặc dispatcher đang bận."
                        : "Đã yêu cầu xử lý " + processed + " email.");
    }
}
