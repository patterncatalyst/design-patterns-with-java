package com.patterncatalyst.designpatterns.ch19.shipping.manifest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentTest {

    @Test
    void rejectsNegativeWeight() {
        assertThatThrownBy(() -> new Shipment("TRK-1", DestinationZone.LOCAL, -1.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullZone() {
        assertThatThrownBy(() -> new Shipment("TRK-1", null, 1.0))
                .isInstanceOf(NullPointerException.class);
    }
}
