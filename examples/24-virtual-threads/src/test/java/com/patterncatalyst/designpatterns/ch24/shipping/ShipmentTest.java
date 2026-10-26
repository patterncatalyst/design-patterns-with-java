package com.patterncatalyst.designpatterns.ch24.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentTest {

    @Test
    void rejectsANegativeWeight() {
        assertThatThrownBy(() -> new Shipment("SHIP-1", -0.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsABlankShipmentId() {
        assertThatThrownBy(() -> new Shipment("   ", 1.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsAValidShipment() {
        Shipment shipment = new Shipment("SHIP-1", 4.2);

        assertThat(shipment.shipmentId()).isEqualTo("SHIP-1");
        assertThat(shipment.weightKg()).isEqualTo(4.2);
    }
}
