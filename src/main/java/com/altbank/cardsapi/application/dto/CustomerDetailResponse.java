package com.altbank.cardsapi.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerDetailResponse(
        UUID id,
        String fullName,
        String document,
        String email,
        String phone,
        AddressResponse address,
        UUID accountId,
        LocalDateTime createdAt
) {
}
