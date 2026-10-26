package com.patterncatalyst.designpatterns.ch16.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentTest {

    @Test
    void rejectsNegativeWeight() {
        assertThatThrownBy(() -> new Shipment(-1.0, DestinationZone.LOCAL, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weightKg");
    }

    @Test
    void rejectsNegativeDeclaredValue() {
        assertThatThrownBy(() -> new Shipment(1.0, DestinationZone.LOCAL, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("declaredValueCents");
    }

    @Test
    void rejectsNullZone() {
        assertThatThrownBy(() -> new Shipment(1.0, null, 0))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void twoShipmentsWithIdenticalFieldsAreEqual() {
        Shipment a = new Shipment(2.0, DestinationZone.REGIONAL, 12_000);
        Shipment b = new Shipment(2.0, DestinationZone.REGIONAL, 12_000);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
