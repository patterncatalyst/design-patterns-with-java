package com.patterncatalyst.designpatterns.ch22.fulfillment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Each {@link PackagingTier} constant computes its own surcharge through its
 * own constant-specific method body; this asserts each one computes the
 * right number, not merely that calling it does not throw.
 */
class PackagingTierTest {

    @Test
    void standardNeverAddsASurcharge() {
        Shipment shipment = new Shipment("RETAIL-1", 50.0, DestinationZone.NATIONAL, PackagingTier.STANDARD, 1_000);

        assertThat(PackagingTier.STANDARD.surchargeCents(shipment)).isZero();
    }

    @Test
    void fragileAddsAFlatAmountPlusAPerKilogramRate() {
        Shipment shipment = new Shipment("RETAIL-1", 1.5, DestinationZone.LOCAL, PackagingTier.FRAGILE, 1_000);

        // 350 flat + round(1.5 * 40) = 350 + 60 = 410.
        assertThat(PackagingTier.FRAGILE.surchargeCents(shipment)).isEqualTo(410L);
    }

    @Test
    void oversizedAddsAFlatAmountPlusAHigherPerKilogramRate() {
        Shipment shipment = new Shipment("WHOLESALE-1", 35.0, DestinationZone.NATIONAL, PackagingTier.OVERSIZED, 1_000);

        // 900 flat + round(35 * 75) = 900 + 2625 = 3525.
        assertThat(PackagingTier.OVERSIZED.surchargeCents(shipment)).isEqualTo(3525L);
    }

    @Test
    void everyConstantSuppliesASurchargeImplementation() {
        Shipment shipment = new Shipment("RETAIL-1", 1.0, DestinationZone.LOCAL, PackagingTier.STANDARD, 1_000);

        for (PackagingTier tier : PackagingTier.values()) {
            assertThat(tier.surchargeCents(shipment)).isGreaterThanOrEqualTo(0L);
        }
    }
}
