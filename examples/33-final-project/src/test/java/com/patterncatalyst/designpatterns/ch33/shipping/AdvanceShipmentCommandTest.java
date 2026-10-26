package com.patterncatalyst.designpatterns.ch33.shipping;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdvanceShipmentCommandTest {

    private Shipment newShipment() {
        return new ShipmentBuilder()
                .id("SHIP-CMD")
                .carrier("GROUND")
                .destinationZone(DestinationZone.LOCAL)
                .weightKg(4.0)
                .build();
    }

    @Test
    void executeAppliesTheEventThroughTheStateMachine() {
        Shipment shipment = newShipment();
        AdvanceShipmentCommand command = new AdvanceShipmentCommand(shipment, ShipmentEvent.PICK_UP);

        command.execute();

        assertThat(shipment.status()).isEqualTo(ShipmentStatus.PICKED_UP);
    }

    @Test
    void undoRestoresThePreviousStatusWithoutWalkingTheTransitionTableBackward() {
        Shipment shipment = newShipment();
        AdvanceShipmentCommand command = new AdvanceShipmentCommand(shipment, ShipmentEvent.PICK_UP);

        command.execute();
        command.undo();

        assertThat(shipment.status()).isEqualTo(ShipmentStatus.CREATED);
    }

    @Test
    void bothExecuteAndUndoNotifySubscribedListenersBecauseBothAreRealEvents() {
        Shipment shipment = newShipment();
        List<String> seen = new ArrayList<>();
        shipment.subscribe((id, from, to) -> seen.add(from + "->" + to));

        AdvanceShipmentCommand command = new AdvanceShipmentCommand(shipment, ShipmentEvent.PICK_UP);
        command.execute();
        command.undo();

        assertThat(seen).containsExactly("CREATED->PICKED_UP", "PICKED_UP->CREATED");
    }
}
