package com.altbank.cardsapi.application.dto;

import java.util.UUID;

public record CreateAccountResponse(
        UUID customerId,
        UUID accountId,
        UUID physicalCardId,
        String trackingId
) {
}
