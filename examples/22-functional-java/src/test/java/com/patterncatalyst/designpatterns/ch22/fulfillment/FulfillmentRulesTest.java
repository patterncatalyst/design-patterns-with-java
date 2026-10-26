package com.patterncatalyst.designpatterns.ch22.fulfillment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FulfillmentRulesTest {

    private final ZoneRateLookup rates = new ZoneRateLookup(Map.of(
            DestinationZone.REGIONAL, Money.ofCents(650),
            DestinationZone.NATIONAL, Money.ofCents(1_200)));

    @Test
    void retailValidatePassesBelowTheDeclaredValueCap() {
        Shipment shipment = new Shipment("RETAIL-1", 1.0, DestinationZone.REGIONAL, PackagingTier.STANDARD, 8_000);

        assertThat(FulfillmentRules.retailValidate(shipment)).isEqualTo(Optional.empty());
    }

    @Test
    void retailValidateRejectsAboveTheDeclaredValueCap() {
        Shipment shipment = new Shipment("RETAIL-2", 1.0, DestinationZone.REGIONAL, PackagingTier.STANDARD, 60_000);

        assertThat(FulfillmentRules.retailValidate(shipment)).isPresent();
    }

    @Test
    void wholesaleValidateRejectsBelowTheMinimumWeight() {
        Shipment shipment = new Shipment("WHOLESALE-1", 5.0, DestinationZone.NATIONAL, PackagingTier.STANDARD, 1_000);

        assertThat(FulfillmentRules.wholesaleValidate(shipment)).isPresent();
    }

    @Test
    void wholesaleValidatePassesAtOrAboveTheMinimumWeight() {
        Shipment shipment = new Shipment("WHOLESALE-2", 35.0, DestinationZone.NATIONAL, PackagingTier.OVERSIZED, 1_000);

        assertThat(FulfillmentRules.wholesaleValidate(shipment)).isEqualTo(Optional.empty());
    }

    @Test
    void retailPriceIsBaseRatePlusTierSurchargeWithNoDiscount() {
        Shipment shipment = new Shipment("RETAIL-1", 2.0, DestinationZone.REGIONAL, PackagingTier.STANDARD, 8_000);

        assertThat(FulfillmentRules.retailPrice(rates, shipment)).isEqualTo(Money.ofCents(650));
    }

    @Test
    void wholesalePriceAppliesTheBulkDiscountToTheBaseRateOnly() {
        Shipment shipment = new Shipment("WHOLESALE-1", 35.0, DestinationZone.NATIONAL, PackagingTier.OVERSIZED, 1_000);

        // base 1200 * 0.85 = 1020, + OVERSIZED surcharge 900 + round(35 * 75) = 3525. Total 4545.
        assertThat(FulfillmentRules.wholesalePrice(rates, shipment)).isEqualTo(Money.ofCents(4_545));
    }
}
