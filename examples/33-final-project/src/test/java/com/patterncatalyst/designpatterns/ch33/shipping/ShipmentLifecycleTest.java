package com.patterncatalyst.designpatterns.ch33.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentLifecycleTest {

    private Shipment newShipment() {
        return new ShipmentBuilder()
                .id("SHIP-LIFECYCLE")
                .carrier("GROUND")
                .destinationZone(DestinationZone.REGIONAL)
                .weightKg(5.0)
                .build();
    }

    @Test
    void aFreshShipmentStartsCreated() {
        assertThat(newShipment().status()).isEqualTo(ShipmentStatus.CREATED);
    }

    @Test
    void theFullHappyPathLifecycleReachesDelivered() {
        Shipment shipment = newShipment();

        shipment.advance(ShipmentEvent.PICK_UP);
        assertThat(shipment.status()).isEqualTo(ShipmentStatus.PICKED_UP);

        shipment.advance(ShipmentEvent.DISPATCH);
        assertThat(shipment.status()).isEqualTo(ShipmentStatus.IN_TRANSIT);

        shipment.advance(ShipmentEvent.DELIVER);
        assertThat(shipment.status()).isEqualTo(ShipmentStatus.DELIVERED);
    }

    @Test
    void cancelIsLegalFromCreated() {
        Shipment shipment = newShipment();
        shipment.advance(ShipmentEvent.CANCEL);
        assertThat(shipment.status()).isEqualTo(ShipmentStatus.CANCELLED);
    }

    @Test
    void cancelIsLegalFromPickedUp() {
        Shipment shipment = newShipment();
        shipment.advance(ShipmentEvent.PICK_UP);
        shipment.advance(ShipmentEvent.CANCEL);
        assertThat(shipment.status()).isEqualTo(ShipmentStatus.CANCELLED);
    }

    @Test
    void cancelIsNotLegalOnceInTransit() {
        Shipment shipment = newShipment();
        shipment.advance(ShipmentEvent.PICK_UP);
        shipment.advance(ShipmentEvent.DISPATCH);

        assertThatThrownBy(() -> shipment.advance(ShipmentEvent.CANCEL))
                .isInstanceOf(IllegalShipmentTransitionException.class)
                .hasMessageContaining("CANCEL")
                .hasMessageContaining("IN_TRANSIT");

        assertThat(shipment.status()).isEqualTo(ShipmentStatus.IN_TRANSIT);
    }

    @Test
    void deliverIsNotLegalBeforeDispatch() {
        Shipment shipment = newShipment();
        shipment.advance(ShipmentEvent.PICK_UP);

        assertThatThrownBy(() -> shipment.advance(ShipmentEvent.DELIVER))
                .isInstanceOf(IllegalShipmentTransitionException.class);
        assertThat(shipment.status()).isEqualTo(ShipmentStatus.PICKED_UP);
    }

    @Test
    void anEventOnceDeliveredIsAlwaysRejected() {
        Shipment shipment = newShipment();
        shipment.advance(ShipmentEvent.PICK_UP);
        shipment.advance(ShipmentEvent.DISPATCH);
        shipment.advance(ShipmentEvent.DELIVER);

        assertThatThrownBy(() -> shipment.advance(ShipmentEvent.PICK_UP))
                .isInstanceOf(IllegalShipmentTransitionException.class);
    }
}
