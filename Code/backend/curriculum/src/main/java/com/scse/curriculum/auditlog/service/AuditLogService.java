package com.scse.curriculum.auditlog.service;

import com.scse.curriculum.auditlog.dto.AuditLogResponse;

import java.util.List;

/**
 * FR-01.7: audit_log là nhật ký bất biến — service CHỈ cung cấp các thao tác đọc/search.
 * KHÔNG có create/update/delete: việc ghi log do hệ thống tự động thực hiện thông qua
 * AuditLogHibernateListener + AuditLogAsyncProcessor, không thông qua tầng service/API này.
 */
public interface AuditLogService {

    List<AuditLogResponse> getAll();

    List<AuditLogResponse> getRecent(int limit);

    AuditLogResponse getById(Long id);

    List<AuditLogResponse> getByTableName(String tableName);

    List<AuditLogResponse> search(String keyword);
}
