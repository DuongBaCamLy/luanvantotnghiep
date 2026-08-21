package com.scse.curriculum.auditlog.dto;

import com.scse.curriculum.auditlog.entity.AuditAction;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogResponse {

    private Long id;
    private String tableName;
    private Integer recordId;
    private AuditAction action;
    private String oldValue;
    private String newValue;
    private Integer changedById;
    private String changedByUsername;
    private LocalDateTime changedAt;
    private String ipAddress;
    private String userAgent;
}