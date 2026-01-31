package com.altbank.cardsapi.domain.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.LocalDateTime;

@Entity
@Table(name = "virtual_cards")
@DiscriminatorValue("VIRTUAL")
@PrimaryKeyJoinColumn(name = "card_id")
@Access(AccessType.FIELD)
public class VirtualCard extends Card {

    @Column(name = "processor_account_id", length = 64, nullable = false)
    private String processorAccountId;

    @Column(name = "processor_card_id", length = 64, nullable = false, unique = true)
    private String processorCardId;

    @Column(name = "cvv_expiration_at")
    private LocalDateTime cvvExpirationAt;

    protected VirtualCard() {
    }

    public VirtualCard(Account account,
                       String processorAccountId,
                       String processorCardId,
                       LocalDateTime cvvExpirationAt,
                       Clock clock) {
        super(account, clock);
        this.processorAccountId = require(processorAccountId, "processorAccountId");
        this.processorCardId = require(processorCardId, "processorCardId");
        this.cvvExpirationAt = cvvExpirationAt;
    }

    public String processorAccountId() {
        return processorAccountId;
    }

    public String processorCardId() {
        return processorCardId;
    }

    public LocalDateTime cvvExpirationAt() {
        return cvvExpirationAt;
    }

    public void updateCvvExpirationAt(LocalDateTime expirationAt) {
        this.cvvExpirationAt = expirationAt;
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    @Override
    public CardType type() {
        return CardType.VIRTUAL;
    }
}
