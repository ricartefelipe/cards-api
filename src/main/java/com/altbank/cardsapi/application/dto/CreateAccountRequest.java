package com.altbank.cardsapi.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(
        @NotNull @Valid CustomerInput customer,
        @NotNull @Valid AddressInput address
) {
    public record CustomerInput(
            @NotBlank String fullName,
            @NotBlank String document,
            @NotBlank @Email String email,
            String phone
    ) {
    }

    public record AddressInput(
            @NotBlank String street,
            @NotBlank String number,
            @NotBlank String city,
            @NotBlank String state,
            @NotBlank String zipCode,
            @NotBlank String country
    ) {
    }
}
