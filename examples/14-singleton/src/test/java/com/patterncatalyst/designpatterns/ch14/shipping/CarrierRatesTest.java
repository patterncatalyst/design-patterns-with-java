package com.patterncatalyst.designpatterns.ch14.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class CarrierRatesTest {

    @Test
    void baseRatesByCarrierCodeCoversEveryCarrierUsedInThisExample() {
        Map<String, Money> rates = CarrierRates.baseRatesByCarrierCode();

        assertThat(rates)
                .containsEntry("GROUND", Money.ofCents(180))
                .containsEntry("AIR", Money.ofCents(640))
                .containsEntry("FREIGHT", Money.ofCents(95));
    }

    @Test
    void baseRatesByCarrierCodeReturnsAnImmutableMap() {
        Map<String, Money> rates = CarrierRates.baseRatesByCarrierCode();

        assertThatThrownBy(() -> rates.put("NEW", Money.ofCents(1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
