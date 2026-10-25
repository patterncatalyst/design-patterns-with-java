package com.patterncatalyst.designpatterns.ch08.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * {@link Money} tracks cents as a {@code long}, never a {@code double}, so
 * these tests confirm addition and formatting stay exact rather than
 * accumulating floating point drift across repeated quotes.
 */
class MoneyTest {

    @Test
    void addsTwoAmountsExactly() {
        Money combined = Money.ofCents(1130).plus(Money.ofCents(96));

        assertThat(combined).isEqualTo(Money.ofCents(1226));
    }

    @Test
    void roundsDollarsAndCentsToTheNearestCent() {
        assertThat(Money.ofRoundedDollars(12.005)).isEqualTo(Money.ofCents(1201));
    }

    @Test
    void formatsAsADollarString() {
        assertThat(Money.ofCents(1226).toString()).isEqualTo("$12.26");
    }

    @Test
    void rejectsANegativeAmount() {
        assertThatThrownBy(() -> Money.ofCents(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
