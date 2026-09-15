package com.scse.curriculum.auditlog.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_name", nullable = false)
    private String tableName;

    @Column(name = "record_id", nullable = false)
    private Integer recordId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Column(name = "old_value", columnDefinition = "JSON")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "JSON")
    private String newValue;

    @ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(
        name = "changed_by",
        referencedColumnName = "user_id",
        nullable = false)
private AuditActorSnapshot changedBy;

    @Column(name = "changed_at")
    private LocalDateTime changedAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;
}