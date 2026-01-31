package com.altbank.cardsapi.application.dto;

import java.time.LocalDateTime;

public record CvvResponse(
        int cvv,
        LocalDateTime expirationDate
) {
}
