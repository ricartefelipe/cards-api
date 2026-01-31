package com.altbank.cardsapi.domain.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "cards")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "card_type", discriminatorType = DiscriminatorType.STRING, length = 16)
@Access(AccessType.FIELD)
public abstract class Card {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private CardStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    protected Card() {
    }

    protected Card(Account account, Clock clock) {
        this.id = UUID.randomUUID();
        this.account = Objects.requireNonNull(account, "account");
        this.status = CardStatus.ACTIVE;
        this.createdAt = LocalDateTime.now(Objects.requireNonNull(clock, "clock"));
    }

    public UUID id() {
        return id;
    }

    public Account account() {
        return account;
    }

    public CardStatus status() {
        return status;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime deactivatedAt() {
        return deactivatedAt;
    }

    public boolean isActive() {
        return status == CardStatus.ACTIVE;
    }

    public void deactivate(Clock clock) {
        if (status == CardStatus.INACTIVE) {
            return;
        }
        status = CardStatus.INACTIVE;
        deactivatedAt = LocalDateTime.now(Objects.requireNonNull(clock, "clock"));
    }

    public CardType type() {
        if (this instanceof PhysicalCard) {
            return CardType.PHYSICAL;
        }
        if (this instanceof VirtualCard) {
            return CardType.VIRTUAL;
        }
        throw new IllegalStateException("Unknown card type");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Card card)) return false;
        return Objects.equals(id, card.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
