package com.altbank.cardsapi.domain.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import static com.altbank.cardsapi.domain.model.support.Strings.normalizeOptional;
import static com.altbank.cardsapi.domain.model.support.Strings.requireNonBlank;

@Entity
@Table(name = "customers")
@Access(AccessType.FIELD)
public class Customer {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private UUID id;

    @Column(name = "full_name", length = 120, nullable = false)
    private String fullName;

    @Column(name = "document", length = 32, nullable = false, unique = true)
    private String document;

    @Column(name = "email", length = 120, nullable = false)
    private String email;

    @Column(name = "phone", length = 40)
    private String phone;

    @Embedded
    private Address address;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Customer() {
    }

    public Customer(String fullName, String document, String email, String phone, Address address, Clock clock) {
        this.id = UUID.randomUUID();
        this.fullName = requireNonBlank(fullName, "fullName");
        this.document = requireNonBlank(document, "document");
        this.email = requireNonBlank(email, "email");
        this.phone = normalizeOptional(phone);
        this.address = Objects.requireNonNull(address, "address");
        this.createdAt = LocalDateTime.now(Objects.requireNonNull(clock, "clock"));
    }

    public UUID id() {
        return id;
    }

    public String fullName() {
        return fullName;
    }

    public String document() {
        return document;
    }

    public String email() {
        return email;
    }

    public String phone() {
        return phone;
    }

    public Address address() {
        return address;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer customer)) return false;
        return Objects.equals(id, customer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
