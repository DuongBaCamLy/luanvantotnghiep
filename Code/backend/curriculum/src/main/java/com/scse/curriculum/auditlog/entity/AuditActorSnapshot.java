package com.scse.curriculum.auditlog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_actor_snapshot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditActorSnapshot {

    @Id
    @Column(name = "user_id", nullable = false)
    private Integer id;

    @Column(
            name = "username",
            nullable = false,
            length = 100)
    private String username;

    @Column(
            name = "role",
            nullable = false,
            length = 50)
    private String role;

    @Column(
            name = "actor_type",
            nullable = false,
            length = 30)
    private String actorType;

    @Column(
            name = "captured_at",
            nullable = false)
    private LocalDateTime capturedAt;
}