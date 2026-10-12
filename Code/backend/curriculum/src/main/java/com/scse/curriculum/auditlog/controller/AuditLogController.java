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
@PreAuthorize("@phaseRoleGuard.isAdmin(authentication)")
public class AuditLogController {

    private final AuditLogService service;

    @org.springframework.web.bind.annotation.ExceptionHandler(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public java.util.Map<String, String> invalidParameter(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException exception) {
        return java.util.Map.of("message", "Invalid audit log parameter: " + exception.getName());
    }

    @GetMapping("/page")
    public com.scse.curriculum.auditlog.dto.AuditLogPageResponse getPage(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) com.scse.curriculum.auditlog.entity.AuditAction action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return service.getPage(q, action, page, size);
    }

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
