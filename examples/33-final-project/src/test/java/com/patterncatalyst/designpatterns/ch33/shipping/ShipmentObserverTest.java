package com.patterncatalyst.designpatterns.ch33.shipping;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShipmentObserverTest {

    private Shipment newShipment() {
        return new ShipmentBuilder()
                .id("SHIP-OBS")
                .carrier("GROUND")
                .destinationZone(DestinationZone.LOCAL)
                .weightKg(3.0)
                .build();
    }

    @Test
    void aSubscribedListenerHearsALegalTransition() {
        Shipment shipment = newShipment();
        List<String> seen = new ArrayList<>();

        shipment.subscribe((id, from, to) -> seen.add(id + ":" + from + "->" + to));
        shipment.advance(ShipmentEvent.PICK_UP);

        assertThat(seen).containsExactly("SHIP-OBS:CREATED->PICKED_UP");
    }

    @Test
    void multipleListenersAllHearTheSameTransition() {
        Shipment shipment = newShipment();
        List<String> first = new ArrayList<>();
        List<String> second = new ArrayList<>();

        shipment.subscribe((id, from, to) -> first.add(to.toString()));
        shipment.subscribe((id, from, to) -> second.add(to.toString()));
        shipment.advance(ShipmentEvent.PICK_UP);

        assertThat(first).containsExactly("PICKED_UP");
        assertThat(second).containsExactly("PICKED_UP");
    }

    @Test
    void closingASubscriptionStopsFurtherNotifications() {
        Shipment shipment = newShipment();
        List<String> seen = new ArrayList<>();

        Subscription subscription = shipment.subscribe((id, from, to) -> seen.add(to.toString()));
        shipment.advance(ShipmentEvent.PICK_UP);
        subscription.close();
        shipment.advance(ShipmentEvent.DISPATCH);

        assertThat(seen).containsExactly("PICKED_UP");
    }

    @Test
    void anIllegalTransitionNeverNotifiesAnyone() {
        Shipment shipment = newShipment();
        List<String> seen = new ArrayList<>();
        shipment.subscribe((id, from, to) -> seen.add(to.toString()));

        try {
            shipment.advance(ShipmentEvent.DELIVER);
        } catch (IllegalShipmentTransitionException expected) {
            // ignored: the assertion below is the real point of this test
        }

        assertThat(seen).isEmpty();
    }

    @Test
    void aListenerThatUnsubscribesItselfDoesNotBreakTheRemainingFanOut() {
        Shipment shipment = newShipment();
        List<String> survivorSeen = new ArrayList<>();
        Subscription[] selfReference = new Subscription[1];

        selfReference[0] = shipment.subscribe((id, from, to) -> selfReference[0].close());
        shipment.subscribe((id, from, to) -> survivorSeen.add(to.toString()));

        shipment.advance(ShipmentEvent.PICK_UP);

        assertThat(survivorSeen).containsExactly("PICKED_UP");
    }
}
