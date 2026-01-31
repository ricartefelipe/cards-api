package com.altbank.cardsapi.application.dto;

import java.time.LocalDateTime;

public record WebhookAckResponse(
        String status,
        LocalDateTime processedAt
) {
}
