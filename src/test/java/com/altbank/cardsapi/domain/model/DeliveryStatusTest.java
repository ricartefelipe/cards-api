package com.altbank.cardsapi.domain.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DeliveryStatusTest {

    @Test
    void fromExternal_acceptsInsensitiveInput() {
        Assertions.assertEquals(DeliveryStatus.SHIPPED, DeliveryStatus.fromExternal(" shipped "));
    }

    @Test
    void fromExternal_whenBlank_shouldThrow() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> DeliveryStatus.fromExternal(" "));
    }

    @Test
    void fromExternal_whenUnknown_shouldThrowWithMessage() {
        IllegalArgumentException ex =
                Assertions.assertThrows(IllegalArgumentException.class, () -> DeliveryStatus.fromExternal("LOST_ON_MARS"));

        Assertions.assertTrue(ex.getMessage().contains("Unknown delivery status"));
    }
}
