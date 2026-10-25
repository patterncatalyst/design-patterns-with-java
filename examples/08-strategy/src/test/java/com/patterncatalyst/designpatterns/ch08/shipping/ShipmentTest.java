package com.patterncatalyst.designpatterns.ch08.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Edge cases on the {@link Shipment} value type itself: the invariants a
 * record's compact constructor enforces once, for every caller, rather than
 * leaving each {@link RateStrategy} to re-check a negative weight on its own.
 */
class ShipmentTest {

    @Test
    void rejectsANegativeWeight() {
        assertThatThrownBy(() -> new Shipment(-0.1, DestinationZone.LOCAL, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weightKg");
    }

    @Test
    void rejectsANegativeDeclaredValue() {
        assertThatThrownBy(() -> new Shipment(1.0, DestinationZone.LOCAL, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("declaredValueCents");
    }

    @Test
    void rejectsANullZone() {
        assertThatThrownBy(() -> new Shipment(1.0, null, 0))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void acceptsAZeroWeightShipment() {
        Shipment shipment = new Shipment(0.0, DestinationZone.LOCAL, 0);

        RateQuote quote = Carrier.GROUND.quote(shipment);

        // base 500 + round(180 * 0) = 500.
        assertThat(quote).isEqualTo(new RateQuote("ground", Money.ofCents(500)));
    }
}
