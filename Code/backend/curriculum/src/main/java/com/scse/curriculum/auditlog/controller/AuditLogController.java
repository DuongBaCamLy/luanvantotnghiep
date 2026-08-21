package com.scse.curriculum.auditlog.controller;

import com.scse.curriculum.auditlog.dto.AuditLogResponse;
import com.scse.curriculum.auditlog.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * FR-01.7: audit_log là nhật ký bất biến.
 * Controller này CHỈ cung cấp các endpoint đọc/search (GET) — không có
 * POST/PUT/DELETE, để không ai (kể cả ADMIN) có thể tạo/sửa/xoá log qua API.
 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService service;

    @GetMapping
    public List<AuditLogResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/recent")
    public List<AuditLogResponse> getRecent(
            @RequestParam(defaultValue = "8") int limit) {
        return service.getRecent(limit);
    }

    @GetMapping("/{id}")
    public AuditLogResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/search")
    public List<AuditLogResponse> search(@RequestParam String q) {
        return service.search(q);
    }

    @GetMapping("/table/{tableName}")
    public List<AuditLogResponse> getByTableName(@PathVariable String tableName) {
        return service.getByTableName(tableName);
    }
}
