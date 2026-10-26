package com.patterncatalyst.designpatterns.ch12.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {

    @Test
    void ofCentsWrapsTheExactAmount() {
        assertThat(Money.ofCents(1532).cents()).isEqualTo(1532);
    }

    @Test
    void plusAddsExactly() {
        Money sum = Money.ofCents(650).plus(Money.ofCents(882));
        assertThat(sum).isEqualTo(Money.ofCents(1532));
    }

    @Test
    void toStringFormatsDollarsAndCents() {
        assertThat(Money.ofCents(1532).toString()).isEqualTo("$15.32");
        assertThat(Money.ofCents(800).toString()).isEqualTo("$8.00");
    }
}
