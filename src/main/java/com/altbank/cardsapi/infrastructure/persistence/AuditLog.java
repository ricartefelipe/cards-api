package com.altbank.cardsapi.infrastructure.persistence;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Registro de auditoria de acao administrativa. Sem material sensivel.
 */
@Entity
@Table(name = "audit_logs")
@Access(AccessType.FIELD)
public class AuditLog {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private UUID id;

    @Column(name = "correlation_id", length = 36, nullable = false, updatable = false)
    private String correlationId;

    @Column(name = "actor_subject", length = 64, updatable = false)
    private String actorSubject;

    @Column(name = "action", length = 48, nullable = false, updatable = false)
    private String action;

    @Column(name = "target_type", length = 32, updatable = false)
    private String targetType;

    @Column(name = "target_id", length = 36, updatable = false)
    private String targetId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AuditLog() {
    }

    public AuditLog(String correlationId, String actorSubject, String action,
                    String targetType, String targetId, Clock clock) {
        this.id = UUID.randomUUID();
        this.correlationId = Objects.requireNonNull(correlationId, "correlationId");
        this.actorSubject = actorSubject;
        this.action = Objects.requireNonNull(action, "action");
        this.targetType = targetType;
        this.targetId = targetId;
        this.createdAt = LocalDateTime.now(Objects.requireNonNull(clock, "clock"));
    }

    public UUID id() {
        return id;
    }
}
