package com.altbank.cardsapi.application;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PageRequestTest {

    @Test
    void of_appliesDefaultsForNulls() {
        PageRequest pr = PageRequest.of(null, null);
        Assertions.assertEquals(0, pr.page());
        Assertions.assertEquals(PageRequest.DEFAULT_SIZE, pr.size());
        Assertions.assertEquals(0, pr.offset());
    }

    @Test
    void clampsNegativePageAndInvalidSize() {
        PageRequest pr = PageRequest.of(-5, 0);
        Assertions.assertEquals(0, pr.page());
        Assertions.assertEquals(PageRequest.DEFAULT_SIZE, pr.size());
    }

    @Test
    void capsSizeAtMax() {
        PageRequest pr = PageRequest.of(0, 10_000);
        Assertions.assertEquals(PageRequest.MAX_SIZE, pr.size());
    }

    @Test
    void offsetIsPageTimesSize() {
        PageRequest pr = PageRequest.of(3, 25);
        Assertions.assertEquals(75, pr.offset());
        Assertions.assertEquals(25, pr.limit());
    }
}
