package com.patterncatalyst.designpatterns.ch24.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateQuoteTest {

    @Test
    void rejectsABlankCarrierId() {
        assertThatThrownBy(() -> new RateQuote("  ", 100))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANegativeQuote() {
        assertThatThrownBy(() -> new RateQuote("acme", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
