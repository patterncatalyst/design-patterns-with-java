package com.patterncatalyst.designpatterns.ch29.shipping;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentTest {

    @Test
    void rejectsABlankId() {
        assertThatThrownBy(() -> new Shipment("  ", Instant.now(), 10.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id");
    }

    @Test
    void rejectsANonPositiveWeight() {
        assertThatThrownBy(() -> new Shipment("SHM-5001", Instant.now(), 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weightKg");
    }

    @Test
    void acceptsAWellFormedShipment() {
        Shipment shipment = new Shipment("SHM-5002", Instant.now(), 10.0);

        assertThat(shipment.id()).isEqualTo("SHM-5002");
    }
}
