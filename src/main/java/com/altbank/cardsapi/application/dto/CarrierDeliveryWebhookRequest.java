package com.altbank.cardsapi.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CarrierDeliveryWebhookRequest(
        @NotBlank String trackingId,
        @NotBlank String deliveryStatus,
        @NotNull LocalDateTime deliveryDate,
        String deliveryReturnReason,
        String deliveryAddress
) {
}
