package com.altbank.cardsapi.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AccountSummaryResponse(
        UUID id,
        String status,
        UUID customerId,
        String customerName,
        String document,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt
) {
}
