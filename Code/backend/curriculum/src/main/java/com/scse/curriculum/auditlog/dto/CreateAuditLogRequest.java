package com.scse.curriculum.auditlog.dto;

import com.scse.curriculum.auditlog.entity.AuditAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAuditLogRequest {

    @NotBlank
    private String tableName;

    @NotNull
    private Integer recordId;

    @NotNull
    private AuditAction action;

    private String oldValue;

    private String newValue;

    @NotNull
    private Integer changedById;

    private String ipAddress;

    private String userAgent;
}