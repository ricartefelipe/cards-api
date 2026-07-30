package com.altbank.cardsapi.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PhysicalCardResponse(
        UUID id,
        UUID accountId,
        String status,
        String trackingId,
        String deliveryStatus,
        LocalDateTime deliveryDate,
        String deliveryReturnReason,
        String deliveryAddress,
        LocalDateTime deliveredAt,
        LocalDateTime validatedAt,
        String reissueReason,
        String previousPhysicalCardId,
        LocalDateTime createdAt,
        LocalDateTime deactivatedAt
) {
}
