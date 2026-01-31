package com.altbank.cardsapi.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record IssueVirtualCardResponse(
        UUID virtualCardId,
        String processorCardId,
        LocalDateTime cvvExpirationAt
) {
}
