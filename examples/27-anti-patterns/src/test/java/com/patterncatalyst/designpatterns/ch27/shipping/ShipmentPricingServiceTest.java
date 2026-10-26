package com.patterncatalyst.designpatterns.ch27.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentPricingServiceTest {

    @Test
    void twoIndependentlyConfiguredServicesNeverInterfereWithEachOther() {
        ShipmentPricingService domestic = new ShipmentPricingService(new RateSettings(1.0, 250));
        ShipmentPricingService international = new ShipmentPricingService(new RateSettings(1.35, 900));

        assertThat(domestic.price(Money.ofCents(10_000)).totalCost()).isEqualTo(Money.ofCents(10_250));
        assertThat(international.price(Money.ofCents(10_000)).totalCost()).isEqualTo(Money.ofCents(14_400));

        // domestic's answer stays put no matter what international just
        // computed, because each service owns its own RateSettings value
        // instead of reading a shared global.
        assertThat(domestic.price(Money.ofCents(10_000)).totalCost()).isEqualTo(Money.ofCents(10_250));
    }

    @Test
    void surchargeAppliedTravelsWithTheResultItDescribes() {
        ShipmentPricingService noSurcharge = new ShipmentPricingService(new RateSettings(1.0, 0));
        ShipmentPricingService withSurcharge = new ShipmentPricingService(new RateSettings(1.20, 0));

        assertThat(noSurcharge.price(Money.ofCents(5_000)).surchargeApplied()).isFalse();
        assertThat(withSurcharge.price(Money.ofCents(5_000)).surchargeApplied()).isTrue();
    }

    @Test
    void substitutingAFakeRateIsOneLineWithNoInterfaceOrMockRequired() {
        ShipmentPricingService promotional = new ShipmentPricingService(new RateSettings(0.80, 0));

        assertThat(promotional.price(Money.ofCents(10_000)).totalCost()).isEqualTo(Money.ofCents(8_000));
    }

    @Test
    void priceRejectsANonPositiveBaseCost() {
        ShipmentPricingService service = new ShipmentPricingService(new RateSettings(1.0, 0));

        assertThatThrownBy(() -> service.price(Money.ofCents(0)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
