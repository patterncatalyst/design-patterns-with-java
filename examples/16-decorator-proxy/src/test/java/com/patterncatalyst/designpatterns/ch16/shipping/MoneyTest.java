package com.patterncatalyst.designpatterns.ch16.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void rejectsNegativeCents() {
        assertThatThrownBy(() -> Money.ofCents(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cents");
    }

    @Test
    void plusAddsExactly() {
        Money sum = Money.ofCents(1_130).plus(Money.ofCents(96));

        assertThat(sum).isEqualTo(Money.ofCents(1_226));
    }

    @Test
    void formatsAsDollars() {
        assertThat(Money.ofCents(1_130).toString()).isEqualTo("$11.30");
    }
}
