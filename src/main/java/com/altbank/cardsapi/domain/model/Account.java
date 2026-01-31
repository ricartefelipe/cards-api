package com.altbank.cardsapi.domain.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
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
@Table(name = "accounts")
@Access(AccessType.FIELD)
public class Account {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 24, nullable = false)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    protected Account() {
    }

    public Account(Customer customer, Clock clock) {
        this.id = UUID.randomUUID();
        this.customer = Objects.requireNonNull(customer, "customer");
        this.status = AccountStatus.ACTIVE;
        this.createdAt = LocalDateTime.now(Objects.requireNonNull(clock, "clock"));
    }

    public UUID id() {
        return id;
    }

    public Customer customer() {
        return customer;
    }

    public AccountStatus status() {
        return status;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime cancelledAt() {
        return cancelledAt;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public void cancel(Clock clock) {
        if (status == AccountStatus.CANCELLED) {
            return;
        }
        status = AccountStatus.CANCELLED;
        cancelledAt = LocalDateTime.now(Objects.requireNonNull(clock, "clock"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account account)) return false;
        return Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
