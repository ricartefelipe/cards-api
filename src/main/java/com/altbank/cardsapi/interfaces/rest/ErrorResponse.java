package com.altbank.cardsapi.interfaces.rest;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        String errorCode,
        String message,
        int status,
        String path,
        LocalDateTime timestamp,
        List<FieldViolation> violations
) {
    public record FieldViolation(String field, String message) {
    }
}
