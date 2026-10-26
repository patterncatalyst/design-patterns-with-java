package com.patterncatalyst.designpatterns.ch22.fulfillment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ShipmentTest {

    @Test
    void rejectsNegativeWeight() {
        assertThatThrownBy(() ->
                new Shipment("RETAIL-1", -1.0, DestinationZone.LOCAL, PackagingTier.STANDARD, 1_000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weightKg");
    }

    @Test
    void rejectsNegativeDeclaredValue() {
        assertThatThrownBy(() ->
                new Shipment("RETAIL-1", 1.0, DestinationZone.LOCAL, PackagingTier.STANDARD, -1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("declaredValueCents");
    }

    @Test
    void rejectsNullZoneAndTier() {
        assertThatThrownBy(() -> new Shipment("RETAIL-1", 1.0, null, PackagingTier.STANDARD, 1_000))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Shipment("RETAIL-1", 1.0, DestinationZone.LOCAL, null, 1_000))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void acceptsAValidShipment() {
        Shipment shipment = new Shipment("RETAIL-1", 2.5, DestinationZone.REGIONAL, PackagingTier.FRAGILE, 4_000);

        assertThat(shipment.orderId()).isEqualTo("RETAIL-1");
        assertThat(shipment.weightKg()).isEqualTo(2.5);
        assertThat(shipment.zone()).isEqualTo(DestinationZone.REGIONAL);
        assertThat(shipment.tier()).isEqualTo(PackagingTier.FRAGILE);
        assertThat(shipment.declaredValueCents()).isEqualTo(4_000L);
    }
}
