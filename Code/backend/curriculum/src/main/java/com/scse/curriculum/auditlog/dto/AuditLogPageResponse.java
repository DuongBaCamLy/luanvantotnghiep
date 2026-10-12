package com.scse.curriculum.auditlog.dto;

import java.util.List;

public record AuditLogPageResponse(List<AuditLogResponse> content, int number, int size,
                                   long totalElements, int totalPages) { }
