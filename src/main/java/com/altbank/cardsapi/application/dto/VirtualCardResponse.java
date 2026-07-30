package com.altbank.cardsapi.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record VirtualCardResponse(
        UUID id,
        UUID accountId,
        String status,
        String processorAccountId,
        String processorCardId,
        LocalDateTime cvvExpirationAt,
        String reissueReason,
        String previousVirtualCardId,
        LocalDateTime createdAt,
        LocalDateTime deactivatedAt
) {
}
