package com.patterncatalyst.designpatterns.ch15.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentTest {

    @Test
    void acceptsAValidShipment() {
        Shipment shipment = new Shipment(3.5, DestinationZone.REGIONAL, 12_000);

        assertThat(shipment.weightKg()).isEqualTo(3.5);
        assertThat(shipment.zone()).isEqualTo(DestinationZone.REGIONAL);
        assertThat(shipment.declaredValueCents()).isEqualTo(12_000);
    }

    @Test
    void rejectsNegativeWeight() {
        assertThatThrownBy(() -> new Shipment(-1.0, DestinationZone.LOCAL, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weightKg");
    }

    @Test
    void rejectsNullZone() {
        assertThatThrownBy(() -> new Shipment(1.0, null, 0))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNegativeDeclaredValue() {
        assertThatThrownBy(() -> new Shipment(1.0, DestinationZone.LOCAL, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("declaredValueCents");
    }
}
