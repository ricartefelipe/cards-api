package com.altbank.cardsapi.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.altbank.cardsapi.domain.model.support.Strings.requireNonBlank;

/**
 * Intencao duravel de pedir uma remessa a transportadora para um cartao fisico.
 *
 * <p>E gravada na mesma transacao que cria o cartao; o efeito externo acontece depois, e so
 * quem conseguir levar o pedido de PENDING a IN_PROGRESS faz a chamada. A finalizacao e
 * condicional: vale apenas se o pedido ainda estiver IN_PROGRESS.
 */
@Entity
@Table(name = "shipment_requests")
public class ShipmentRequest {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "physical_card_id", length = 36, nullable = false, unique = true)
    private String physicalCardId;

    @Column(name = "delivery_address", length = 255, nullable = false)
    private String deliveryAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private ShipmentStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "tracking_id", length = 64)
    private String trackingId;

    @Column(name = "last_error", length = 255)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ShipmentRequest() {
    }

    public ShipmentRequest(PhysicalCard card, String deliveryAddress, Clock clock) {
        this.id = UUID.randomUUID().toString();
        this.physicalCardId = Objects.requireNonNull(card, "card").id().toString();
        this.deliveryAddress = requireNonBlank(deliveryAddress, "deliveryAddress");
        this.status = ShipmentStatus.PENDING;
        this.attempts = 0;
        this.createdAt = LocalDateTime.now(Objects.requireNonNull(clock, "clock"));
        this.updatedAt = createdAt;
    }

    public UUID id() {
        return UUID.fromString(id);
    }

    public UUID physicalCardId() {
        return UUID.fromString(physicalCardId);
    }

    public String deliveryAddress() {
        return deliveryAddress;
    }

    public ShipmentStatus status() {
        return status;
    }

    public int attempts() {
        return attempts;
    }

    public String trackingId() {
        return trackingId;
    }

    public String lastError() {
        return lastError;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    /** Reivindica o envio. So um chamador sai daqui com {@code true}: o pedido precisa estar PENDING. */
    public boolean start(Clock clock) {
        if (status != ShipmentStatus.PENDING) {
            return false;
        }
        status = ShipmentStatus.IN_PROGRESS;
        attempts++;
        touch(clock);
        return true;
    }

    public boolean isInProgress() {
        return status == ShipmentStatus.IN_PROGRESS;
    }

    public void confirm(String trackingId, Clock clock) {
        requireInProgress();
        this.trackingId = requireNonBlank(trackingId, "trackingId");
        this.lastError = null;
        this.status = ShipmentStatus.CONFIRMED;
        touch(clock);
    }

    public void reject(String reason, Clock clock) {
        requireInProgress();
        this.lastError = cut(reason);
        this.status = ShipmentStatus.REJECTED;
        touch(clock);
    }

    public void uncertain(String reason, Clock clock) {
        requireInProgress();
        this.lastError = cut(reason);
        this.status = ShipmentStatus.UNCERTAIN;
        touch(clock);
    }

    /** Um envio reivindicado ha mais tempo que a tolerancia e um chamador que caiu: a resposta nunca vira. */
    public boolean isStale(Clock clock, Duration grace) {
        return isInProgress() && updatedAt.plus(grace).isBefore(LocalDateTime.now(clock));
    }

    private void requireInProgress() {
        if (!isInProgress()) {
            throw new IllegalStateException("shipment request " + id + " is " + status + ", not IN_PROGRESS");
        }
    }

    private void touch(Clock clock) {
        updatedAt = LocalDateTime.now(Objects.requireNonNull(clock, "clock"));
    }

    private static String cut(String reason) {
        if (reason == null) {
            return null;
        }
        return reason.length() <= 255 ? reason : reason.substring(0, 255);
    }
}
