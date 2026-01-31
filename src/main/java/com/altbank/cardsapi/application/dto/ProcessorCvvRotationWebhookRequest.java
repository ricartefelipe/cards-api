package com.altbank.cardsapi.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ProcessorCvvRotationWebhookRequest(
        @NotBlank String accountId,
        @NotBlank String cardId,
        int nextCvv,
        @NotNull LocalDateTime expirationDate
) {
}
