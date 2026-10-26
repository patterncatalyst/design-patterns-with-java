package com.patterncatalyst.designpatterns.ch15.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void ofCentsWrapsAnExactAmount() {
        assertThat(Money.ofCents(650).cents()).isEqualTo(650);
    }

    @Test
    void rejectsNegativeCents() {
        assertThatThrownBy(() -> Money.ofCents(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cents");
    }

    @Test
    void plusAddsExactly() {
        Money sum = Money.ofCents(650).plus(Money.ofCents(150));

        assertThat(sum).isEqualTo(Money.ofCents(800));
    }

    @Test
    void toStringFormatsDollarsAndCents() {
        assertThat(Money.ofCents(45_000).toString()).isEqualTo("$450.00");
    }
}
