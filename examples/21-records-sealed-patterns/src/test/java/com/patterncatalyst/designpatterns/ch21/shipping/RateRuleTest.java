package com.patterncatalyst.designpatterns.ch21.shipping;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class RateRuleTest {

    @Test
    void zoneSurchargeRejectsABlankZone() {
        RateRule base = new PerKilogram(Money.ofCents(150));

        assertThatIllegalArgumentException().isThrownBy(() -> new ZoneSurcharge(base, "", Money.ofCents(500)));
        assertThatNullPointerException().isThrownBy(() -> new ZoneSurcharge(null, "EAST", Money.ofCents(500)));
    }

    @Test
    void minimumChargeRejectsNullComponents() {
        RateRule base = new FlatRate(Money.ofCents(1_000));

        assertThatNullPointerException().isThrownBy(() -> new MinimumCharge(base, null));
        assertThatNullPointerException().isThrownBy(() -> new MinimumCharge(null, Money.ofCents(1_000)));
    }
}
