package com.patterncatalyst.designpatterns.ch27.shipping;

import com.patterncatalyst.designpatterns.ch27.shipping.overengineered.PricedShipmentReceipt;
import com.patterncatalyst.designpatterns.ch27.shipping.overengineered.ShipmentOperationsManager;
import com.patterncatalyst.designpatterns.ch27.shipping.overengineered.ShippingSurchargeRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the simplification is safe: {@link ShipmentPricingService}, the
 * simplified, appropriate form, computes the identical total cost the
 * over-engineered {@link ShipmentOperationsManager} design computes for the
 * same base cost and surcharge multiplier, across a table of inputs rather
 * than one hand-picked example, with the handling fee held at zero so the
 * comparison stays on the one capability both designs share.
 */
class EquivalenceTest {

    @AfterEach
    void resetRegistry() {
        ShippingSurchargeRegistry.INSTANCE.setSurchargeMultiplier(1.0);
    }

    @ParameterizedTest
    @CsvSource({
            "10000, 1.00",
            "10000, 1.10",
            "25000, 1.35",
            "5000,  1.00",
            "999,   1.50"
    })
    void bothDesignsComputeTheSameTotalForTheSameInputs(long baseCostCents, double surchargeMultiplier) {
        ShippingSurchargeRegistry.INSTANCE.setSurchargeMultiplier(surchargeMultiplier);
        ShipmentOperationsManager overEngineered = new ShipmentOperationsManager();
        PricedShipmentReceipt receipt = overEngineered.priceShipment("ORD-EQ", Money.ofCents(baseCostCents));

        ShipmentPricingService simplified = new ShipmentPricingService(new RateSettings(surchargeMultiplier, 0L));
        PricingResult result = simplified.price(Money.ofCents(baseCostCents));

        assertThat(result.totalCost()).isEqualTo(receipt.totalCost());
    }

    @Test
    void bothDesignsRejectANonPositiveBaseCostWithTheSameExceptionType() {
        ShipmentOperationsManager overEngineered = new ShipmentOperationsManager();
        ShipmentPricingService simplified = new ShipmentPricingService(new RateSettings(1.0, 0L));

        assertThatThrownBy(() -> overEngineered.priceShipment("ORD-BAD", Money.ofCents(0)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> simplified.price(Money.ofCents(0)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
