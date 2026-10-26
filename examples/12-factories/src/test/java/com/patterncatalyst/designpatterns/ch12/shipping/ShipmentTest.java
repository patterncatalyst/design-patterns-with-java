package com.patterncatalyst.designpatterns.ch12.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentTest {

    @Test
    void acceptsAValidShipment() {
        Shipment shipment = new Shipment("Canada", 4.2, 18_000, Region.NORTH_AMERICA);

        assertThat(shipment.destinationCountry()).isEqualTo("Canada");
        assertThat(shipment.weightKg()).isEqualTo(4.2);
        assertThat(shipment.declaredValueCents()).isEqualTo(18_000);
        assertThat(shipment.region()).isEqualTo(Region.NORTH_AMERICA);
    }

    @Test
    void rejectsNonPositiveWeight() {
        assertThatThrownBy(() -> new Shipment("Canada", 0.0, 18_000, Region.NORTH_AMERICA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weightKg");
    }

    @Test
    void rejectsNegativeDeclaredValue() {
        assertThatThrownBy(() -> new Shipment("Canada", 4.2, -1, Region.NORTH_AMERICA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("declaredValueCents");
    }

    @Test
    void rejectsNullDestination() {
        assertThatThrownBy(() -> new Shipment(null, 4.2, 18_000, Region.NORTH_AMERICA))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullRegion() {
        assertThatThrownBy(() -> new Shipment("Canada", 4.2, 18_000, null))
                .isInstanceOf(NullPointerException.class);
    }
}
