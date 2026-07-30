package com.altbank.cardsapi.application.dto;

public record AddressResponse(
        String street,
        String number,
        String city,
        String state,
        String zipCode,
        String country
) {
}
