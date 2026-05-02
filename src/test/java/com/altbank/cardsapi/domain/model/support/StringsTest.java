package com.altbank.cardsapi.domain.model.support;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StringsTest {

    @Test
    void requireNonBlank_trims() {
        Assertions.assertEquals("x", Strings.requireNonBlank("  x ", "f"));
    }

    @Test
    void requireNonBlank_whenNull_shouldThrow() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Strings.requireNonBlank(null, "field"));
    }

    @Test
    void normalizeOptional_blankBecomesNull() {
        Assertions.assertNull(Strings.normalizeOptional("   "));
        Assertions.assertNull(Strings.normalizeOptional(null));
        Assertions.assertEquals("a", Strings.normalizeOptional(" a "));
    }
}
