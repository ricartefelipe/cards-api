package com.altbank.cardsapi.application.dto;

import java.util.UUID;

public record ReissuePhysicalCardResponse(
        UUID oldPhysicalCardId,
        UUID newPhysicalCardId,
        String trackingId
) {
}
