package com.altbank.cardsapi.domain.model;

import java.util.Locale;

public enum DeliveryStatus {
    PENDING,
    SHIPPED,
    IN_TRANSIT,
    DELIVERED,
    FAILED,
    RETURNED;

    public static DeliveryStatus fromExternal(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("deliveryStatus is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return DeliveryStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown delivery status: " + value);
        }
    }
}
