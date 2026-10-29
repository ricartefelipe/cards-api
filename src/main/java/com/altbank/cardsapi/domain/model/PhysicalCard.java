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

import static com.altbank.cardsapi.domain.model.support.Strings.normalizeOptional;
import static com.altbank.cardsapi.domain.model.support.Strings.requireNonBlank;

@Entity
@Table(name = "physical_cards")
@DiscriminatorValue("PHYSICAL")
@PrimaryKeyJoinColumn(name = "card_id")
@Access(AccessType.FIELD)
public class PhysicalCard extends Card {

    @Column(name = "tracking_id", length = 64, unique = true)
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

    @Column(name = "previous_physical_card_id", length = 36, unique = true)
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
        this.trackingId = requireNonBlank(trackingId, "trackingId");
        this.deliveryStatus = Objects.requireNonNull(initialStatus, "initialStatus");
        this.deliveryAddress = normalizeOptional(deliveryAddressSnapshot);
        this.reissueReason = reissueReason;
        this.previousPhysicalCardId = previousCard == null ? null : previousCard.id().toString();
    }

    /** Cartao criado antes da remessa: sem tracking ate a transportadora responder. */
    public PhysicalCard(Account account,
                        String deliveryAddressSnapshot,
                        ReissueReason reissueReason,
                        PhysicalCard previousCard,
                        Clock clock) {
        super(account, clock);
        this.deliveryStatus = DeliveryStatus.PENDING;
        this.deliveryAddress = normalizeOptional(deliveryAddressSnapshot);
        this.reissueReason = reissueReason;
        this.previousPhysicalCardId = previousCard == null ? null : previousCard.id().toString();
    }

    /** A transportadora aceitou: o cartao recebe o tracking e o estado inicial da entrega. */
    public void shipped(String trackingId, DeliveryStatus initialStatus) {
        if (this.trackingId != null) {
            throw new IllegalStateException("card " + id() + " already has a shipment");
        }
        this.trackingId = requireNonBlank(trackingId, "trackingId");
        this.deliveryStatus = Objects.requireNonNull(initialStatus, "initialStatus");
    }

    public boolean awaitsShipment() {
        return trackingId == null;
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

    @Override
    public CardType type() {
        return CardType.PHYSICAL;
    }

    public boolean isDelivered() {
        return deliveredAt != null;
    }

    public boolean isValidated() {
        return validatedAt != null;
    }

    /**
     * Aplica um evento de entrega da transportadora. Ignora eventos estale (data igual ou
     * anterior a ultima registrada, ou sem data quando ja existe uma) e eventos que
     * regrediriam um estado terminal (DELIVERED/RETURNED) para um nao-terminal. Assim,
     * eventos fora de ordem ou repetidos nao regridem o estado (idempotente a reenvio).
     */
    public void applyCarrierDeliveryUpdate(DeliveryStatus newStatus,
                                          LocalDateTime deliveryDate,
                                          String returnReason,
                                          String deliveryAddress,
                                          Clock clock) {
        Objects.requireNonNull(newStatus, "newStatus");
        if (isStaleDeliveryEvent(deliveryDate) || regressesTerminalState(newStatus)) {
            return;
        }
        this.deliveryStatus = newStatus;
        this.deliveryDate = deliveryDate;
        this.deliveryReturnReason = normalizeOptional(returnReason);
        this.deliveryAddress = normalizeOptional(deliveryAddress);
        if (newStatus == DeliveryStatus.DELIVERED && this.deliveredAt == null) {
            this.deliveredAt = deliveryDate != null ? deliveryDate : LocalDateTime.now(Objects.requireNonNull(clock, "clock"));
        }
    }

    private boolean isStaleDeliveryEvent(LocalDateTime incoming) {
        if (this.deliveryDate == null) {
            return false;
        }
        if (incoming == null) {
            return true;
        }
        return !incoming.isAfter(this.deliveryDate);
    }

    private boolean regressesTerminalState(DeliveryStatus incoming) {
        boolean currentlyTerminal = isTerminal(this.deliveryStatus);
        return currentlyTerminal && !isTerminal(incoming);
    }

    private static boolean isTerminal(DeliveryStatus status) {
        return status == DeliveryStatus.DELIVERED || status == DeliveryStatus.RETURNED;
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

}
