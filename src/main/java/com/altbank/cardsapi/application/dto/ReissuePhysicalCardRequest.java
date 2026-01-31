package com.altbank.cardsapi.application.dto;

import com.altbank.cardsapi.domain.model.ReissueReason;
import jakarta.validation.constraints.NotNull;

public record ReissuePhysicalCardRequest(
        @NotNull ReissueReason reason
) {
}
