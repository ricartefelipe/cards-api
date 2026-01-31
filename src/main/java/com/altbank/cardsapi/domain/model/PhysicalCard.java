package com.altbank.cardsapi.domain.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "physical_cards")
@DiscriminatorValue("PHYSICAL")
@PrimaryKeyJoinColumn(name = "card_id")
@Access(AccessType.FIELD)
public class PhysicalCard extends Card {

    @Column(name = "tracking_id", length = 64, nullable = false, unique = true)
    private String trackingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", length = 24, nullable = false)
    private DeliveryStatus deliveryStatus;

    @Column(name = "delivery_date")
    private LocalDateTime deliveryDate;

    @Column(name = "delivery_return_reason", length = 255)
    private String deliveryReturnReason;

    @Column(name = "delivery_address", length = 255)
    private String deliveryAddress;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "reissue_reason", length = 24)
    private ReissueReason reissueReason;

    @Column(name = "previous_physical_card_id", length = 36)
    private String previousPhysicalCardId;

    protected PhysicalCard() {
    }

    public PhysicalCard(Account account,
                        String trackingId,
                        DeliveryStatus initialStatus,
                        String deliveryAddressSnapshot,
                        ReissueReason reissueReason,
                        PhysicalCard previousCard,
                        Clock clock) {
        super(account, clock);
        this.trackingId = require(trackingId, "trackingId");
        this.deliveryStatus = Objects.requireNonNull(initialStatus, "initialStatus");
        this.deliveryAddress = normalizeOptional(deliveryAddressSnapshot);
        this.reissueReason = reissueReason;
        this.previousPhysicalCardId = previousCard == null ? null : previousCard.id().toString();
    }

    public String trackingId() {
        return trackingId;
    }

    public DeliveryStatus deliveryStatus() {
        return deliveryStatus;
    }

    public LocalDateTime deliveryDate() {
        return deliveryDate;
    }

    public String deliveryReturnReason() {
        return deliveryReturnReason;
    }

    public String deliveryAddress() {
        return deliveryAddress;
    }

    public LocalDateTime deliveredAt() {
        return deliveredAt;
    }

    public LocalDateTime validatedAt() {
        return validatedAt;
    }

    public ReissueReason reissueReason() {
        return reissueReason;
    }

    public String previousPhysicalCardId() {
        return previousPhysicalCardId;
    }

    public boolean isDelivered() {
        return deliveredAt != null;
    }

    public boolean isValidated() {
        return validatedAt != null;
    }

    public void applyCarrierDeliveryUpdate(DeliveryStatus newStatus,
                                          LocalDateTime deliveryDate,
                                          String returnReason,
                                          String deliveryAddress,
                                          Clock clock) {
        this.deliveryStatus = Objects.requireNonNull(newStatus, "newStatus");
        this.deliveryDate = deliveryDate;
        this.deliveryReturnReason = normalizeOptional(returnReason);
        this.deliveryAddress = normalizeOptional(deliveryAddress);
        if (newStatus == DeliveryStatus.DELIVERED) {
            this.deliveredAt = deliveryDate != null ? deliveryDate : LocalDateTime.now(Objects.requireNonNull(clock, "clock"));
        }
    }

    public void validate(Clock clock) {
        if (!isActive()) {
            throw new IllegalStateException("Card is inactive");
        }
        if (deliveredAt == null) {
            throw new IllegalStateException("Physical card not delivered");
        }
        if (validatedAt != null) {
            return;
        }
        validatedAt = LocalDateTime.now(Objects.requireNonNull(clock, "clock"));
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
