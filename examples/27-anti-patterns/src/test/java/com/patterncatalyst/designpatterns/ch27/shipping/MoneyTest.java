package com.patterncatalyst.designpatterns.ch27.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void rejectsNegativeCents() {
        assertThatThrownBy(() -> Money.ofCents(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void formatsCentsAsDollarsAndCents() {
        assertThat(Money.ofCents(110_000).toString()).isEqualTo("$1,100.00");
    }
}
