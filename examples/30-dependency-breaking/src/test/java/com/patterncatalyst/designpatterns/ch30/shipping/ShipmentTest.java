package com.patterncatalyst.designpatterns.ch30.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentTest {

    @Test
    void rejectsABlankId() {
        assertThatThrownBy(() -> new Shipment("  ", 1_000))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANegativeBaseCost() {
        assertThatThrownBy(() -> new Shipment("SH-1", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsAValidShipment() {
        Shipment shipment = new Shipment("SH-1", 5_000);

        assertThat(shipment.id()).isEqualTo("SH-1");
        assertThat(shipment.baseCostCents()).isEqualTo(5_000);
    }
}
