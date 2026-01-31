package com.altbank.cardsapi.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReissueVirtualCardResponse(
        UUID oldVirtualCardId,
        UUID newVirtualCardId,
        String processorCardId,
        LocalDateTime cvvExpirationAt
) {
}
