package com.altbank.cardsapi.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerSummaryResponse(
        UUID id,
        String fullName,
        String document,
        String email,
        String phone,
        LocalDateTime createdAt
) {
}
