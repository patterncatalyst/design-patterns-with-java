package com.patterncatalyst.designpatterns.ch22.fulfillment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void plusAddsCentsExactly() {
        Money sum = Money.ofCents(650).plus(Money.ofCents(350));

        assertThat(sum).isEqualTo(Money.ofCents(1000));
    }

    @Test
    void scaledByRoundsToTheNearestCent() {
        Money scaled = Money.ofCents(333).scaledBy(0.5);

        assertThat(scaled).isEqualTo(Money.ofCents(167));
    }

    @Test
    void toStringFormatsAsDollarsAndCents() {
        assertThat(Money.ofCents(1130)).hasToString("$11.30");
        assertThat(Money.ofCents(5)).hasToString("$0.05");
        assertThat(Money.ZERO).hasToString("$0.00");
    }
}
