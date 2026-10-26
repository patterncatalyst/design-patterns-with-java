package com.patterncatalyst.designpatterns.ch14.shipping;

import java.util.Map;

/**
 * The one table of base per-kilogram carrier rates, shared by both singleton
 * forms in this package. Keeping the data in exactly one place means a rate
 * correction lands in both {@link CarrierRateTable} and
 * {@link HolderCarrierRateTable} at once, and lets this example's test
 * suite assert the two idioms agree rather than hoping two copies of the
 * same map stay in sync by hand.
 */
final class CarrierRates {

    private CarrierRates() {
    }

    static Map<String, Money> baseRatesByCarrierCode() {
        return Map.of(
                "GROUND", Money.ofCents(180),
                "AIR", Money.ofCents(640),
                "FREIGHT", Money.ofCents(95));
    }
}
