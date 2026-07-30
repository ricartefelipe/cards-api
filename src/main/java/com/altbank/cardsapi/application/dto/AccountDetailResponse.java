package com.altbank.cardsapi.application.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AccountDetailResponse(
        UUID id,
        String status,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt,
        CustomerSummaryResponse customer,
        List<UUID> physicalCardIds,
        List<UUID> virtualCardIds
) {
}
