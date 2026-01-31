package com.altbank.cardsapi.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ValidatePhysicalCardResponse(
        UUID physicalCardId,
        LocalDateTime validatedAt
) {
}
