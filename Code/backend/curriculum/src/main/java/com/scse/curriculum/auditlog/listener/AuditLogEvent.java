package com.scse.curriculum.auditlog.listener;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * FR-01.7: Event mang du lieu audit duoc publish boi AuditLogHibernateListener
 * ngay sau khi Hibernate thuc hien INSERT/UPDATE/DELETE tren mot entity.
 *
 * Luu y quan trong: event chi mang theo changedByUsername (chuoi lay truc tiep
 * tu SecurityContext, KHONG truy van DB). Viec phan giai username -> user_account.id
 * duoc thuc hien o AuditLogAsyncProcessor, vi do chay tren mot thread/transaction
 * rieng (do @Async) nen co the an toan truy van DB ma khong xung dot voi Hibernate
 * Session dang flush o listener (tranh loi reentrant flush).
 */
@Getter
public class AuditLogEvent extends ApplicationEvent {

    private final String tableName;
    private final Integer recordId;
    private final String action;
    private final String oldValueJson;
    private final String newValueJson;
    private final String changedByUsername;
    private final LocalDateTime changedAt;
    private final String ipAddress;
    private final String userAgent;

    public AuditLogEvent(Object source, String tableName, Integer recordId, String action,
                         String oldValueJson, String newValueJson, String changedByUsername,
                         LocalDateTime changedAt, String ipAddress, String userAgent) {
        super(source);
        this.tableName = tableName;
        this.recordId = recordId;
        this.action = action;
        this.oldValueJson = oldValueJson;
        this.newValueJson = newValueJson;
        this.changedByUsername = changedByUsername;
        this.changedAt = changedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }
}
