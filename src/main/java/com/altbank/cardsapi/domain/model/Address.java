package com.altbank.cardsapi.domain.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
@Access(AccessType.FIELD)
public class Address {

    @Column(name = "address_street", nullable = false, length = 160)
    private String street;

    @Column(name = "address_number", nullable = false, length = 40)
    private String number;

    @Column(name = "address_city", nullable = false, length = 80)
    private String city;

    @Column(name = "address_state", nullable = false, length = 80)
    private String state;

    @Column(name = "address_zip_code", nullable = false, length = 24)
    private String zipCode;

    @Column(name = "address_country", nullable = false, length = 80)
    private String country;

    protected Address() {
    }

    public Address(String street, String number, String city, String state, String zipCode, String country) {
        this.street = require(street, "street");
        this.number = require(number, "number");
        this.city = require(city, "city");
        this.state = require(state, "state");
        this.zipCode = require(zipCode, "zipCode");
        this.country = require(country, "country");
    }

    public String street() {
        return street;
    }

    public String number() {
        return number;
    }

    public String city() {
        return city;
    }

    public String state() {
        return state;
    }

    public String zipCode() {
        return zipCode;
    }

    public String country() {
        return country;
    }

    public String asSingleLine() {
        return street + ", " + number + ", " + city + "-" + state + ", " + zipCode + ", " + country;
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address that)) return false;
        return Objects.equals(street, that.street)
                && Objects.equals(number, that.number)
                && Objects.equals(city, that.city)
                && Objects.equals(state, that.state)
                && Objects.equals(zipCode, that.zipCode)
                && Objects.equals(country, that.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(street, number, city, state, zipCode, country);
    }
}
