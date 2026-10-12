package com.scse.curriculum.auditlog.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;

import com.scse.curriculum.auditlog.dto.AuditLogResponse;
import com.scse.curriculum.auditlog.entity.AuditLog;
import com.scse.curriculum.auditlog.repository.AuditLogRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
/**
 * FR-01.7: chỉ đọc/search. Không còn create/update/delete (xem AuditLogService).
 */
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository repository;

    @Override
    @Transactional(readOnly = true)
    public com.scse.curriculum.auditlog.dto.AuditLogPageResponse getPage(String keyword,
            com.scse.curriculum.auditlog.entity.AuditAction action, int page, int size) {
        String q = keyword == null ? "" : keyword.trim().toLowerCase(java.util.Locale.ROOT);
        String pattern = "%" + q.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        var result = repository.searchPage(pattern, action, PageRequest.of(Math.max(0, page),
                Math.max(1, Math.min(100, size)), org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Order.desc("changedAt"),
                        org.springframework.data.domain.Sort.Order.desc("id"))));
        return new com.scse.curriculum.auditlog.dto.AuditLogPageResponse(
                result.getContent().stream().map(this::map).toList(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAll() {
        return repository.findAll().stream().map(this::map).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getRecent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return repository.findAllByOrderByChangedAtDesc(PageRequest.of(0, safeLimit))
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponse getById(Long id) {
        return map(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log not found")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getByTableName(String tableName) {
        return repository.findByTableNameOrderByChangedAtDesc(tableName).stream().map(this::map).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> search(String keyword) {
        String q = keyword == null ? "" : keyword.trim();
        return repository.findByTableNameContainingIgnoreCaseOrChangedBy_UsernameContainingIgnoreCase(q, q)
                .stream().map(this::map).toList();
    }

    private AuditLogResponse map(AuditLog auditLog) {
        boolean hasActor = auditLog.getChangedBy() != null;
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .tableName(auditLog.getTableName())
                .recordId(auditLog.getRecordId())
                .action(auditLog.getAction())
                .oldValue(auditLog.getOldValue())
                .newValue(auditLog.getNewValue())
                .changedById(hasActor ? auditLog.getChangedBy().getId() : null)
                .changedByUsername(hasActor ? auditLog.getChangedBy().getUsername() : "Hệ thống")
                .changedAt(auditLog.getChangedAt())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .build();
    }
}
