package com.patterncatalyst.designpatterns.ch26.shipping;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ShipmentEventTest {

    @Test
    void rejectsANullEventType() {
        assertThatThrownBy(() -> new ShipmentEvent(null, "SH-1", "detail"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANullTrackingId() {
        assertThatThrownBy(() -> new ShipmentEvent(ShipmentEventType.CREATED, null, "detail"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANullDetail() {
        assertThatThrownBy(() -> new ShipmentEvent(ShipmentEventType.CREATED, "SH-1", null))
                .isInstanceOf(NullPointerException.class);
    }
}
