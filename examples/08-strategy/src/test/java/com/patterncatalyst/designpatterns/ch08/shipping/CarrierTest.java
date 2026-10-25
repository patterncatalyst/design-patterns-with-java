package com.patterncatalyst.designpatterns.ch08.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * {@link Carrier} is the closed-set expression of Strategy: every constant
 * must supply its own {@code quote} body, so the compiler, not a reviewer,
 * guarantees there is no carrier the switch-based alternative could forget.
 */
class CarrierTest {

    @Test
    void exposesExactlyTheThreeContractedCarriers() {
        assertThat(Carrier.values()).containsExactly(Carrier.GROUND, Carrier.AIR_FLAT, Carrier.FREIGHT_ZONE);
    }

    @Test
    void everyCarrierProducesAQuoteNamedAfterItself() {
        Shipment shipment = new Shipment(1.0, DestinationZone.NATIONAL, 1_000);

        for (Carrier carrier : Carrier.values()) {
            RateQuote quote = carrier.quote(shipment);
            assertThat(quote.quotedBy()).isNotBlank();
            assertThat(quote.amount().cents()).isPositive();
        }
    }

    @Test
    void groundIsUsableAnywhereARateStrategyIsExpected() {
        RateStrategy strategy = Carrier.GROUND;
        Shipment shipment = new Shipment(2.0, DestinationZone.LOCAL, 0);

        assertThat(strategy.quote(shipment)).isEqualTo(Carrier.GROUND.quote(shipment));
    }
}
