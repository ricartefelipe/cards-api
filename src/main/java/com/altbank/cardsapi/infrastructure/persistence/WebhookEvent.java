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
 * Registro durável de evento de webhook processado, para idempotência/replay.
 * O {@code fingerprint} e unico; nao guarda material sensivel (e um hash).
 */
@Entity
@Table(name = "webhook_events")
@Access(AccessType.FIELD)
public class WebhookEvent {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private UUID id;

    @Column(name = "fingerprint", length = 128, nullable = false, unique = true, updatable = false)
    private String fingerprint;

    @Column(name = "event_type", length = 32, nullable = false, updatable = false)
    private String eventType;

    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    protected WebhookEvent() {
    }

    public WebhookEvent(String fingerprint, String eventType, Clock clock) {
        this.id = UUID.randomUUID();
        this.fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        this.receivedAt = LocalDateTime.now(Objects.requireNonNull(clock, "clock"));
    }

    public UUID id() {
        return id;
    }

    public String fingerprint() {
        return fingerprint;
    }
}
