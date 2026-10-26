package com.patterncatalyst.designpatterns.ch33.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommandHistoryTest {

    private Shipment newShipment() {
        return new ShipmentBuilder()
                .id("SHIP-HIST")
                .carrier("GROUND")
                .destinationZone(DestinationZone.LOCAL)
                .weightKg(5.0)
                .build();
    }

    @Test
    void undoWithNothingExecutedReturnsFalse() {
        assertThat(new CommandHistory().undo()).isFalse();
    }

    @Test
    void redoWithNothingUndoneReturnsFalse() {
        assertThat(new CommandHistory().redo()).isFalse();
    }

    @Test
    void undoStepsBackThroughCommandsInReverseOrder() {
        Shipment shipment = newShipment();
        CommandHistory history = new CommandHistory();

        history.execute(new AdvanceShipmentCommand(shipment, ShipmentEvent.PICK_UP));
        history.execute(new AdvanceShipmentCommand(shipment, ShipmentEvent.DISPATCH));
        assertThat(shipment.status()).isEqualTo(ShipmentStatus.IN_TRANSIT);

        assertThat(history.undo()).isTrue();
        assertThat(shipment.status()).isEqualTo(ShipmentStatus.PICKED_UP);

        assertThat(history.undo()).isTrue();
        assertThat(shipment.status()).isEqualTo(ShipmentStatus.CREATED);

        assertThat(history.undo()).isFalse();
    }

    @Test
    void redoReappliesAnUndoneCommand() {
        Shipment shipment = newShipment();
        CommandHistory history = new CommandHistory();

        history.execute(new AdvanceShipmentCommand(shipment, ShipmentEvent.PICK_UP));
        history.undo();
        assertThat(shipment.status()).isEqualTo(ShipmentStatus.CREATED);

        assertThat(history.redo()).isTrue();
        assertThat(shipment.status()).isEqualTo(ShipmentStatus.PICKED_UP);
    }

    @Test
    void executingANewCommandClearsTheRedoBranch() {
        Shipment shipment = newShipment();
        CommandHistory history = new CommandHistory();

        history.execute(new AdvanceShipmentCommand(shipment, ShipmentEvent.PICK_UP));
        history.undo();

        history.execute(new AdjustWeightCommand(shipment, 9.0));

        assertThat(history.redo()).isFalse();
        assertThat(shipment.status()).isEqualTo(ShipmentStatus.CREATED);
    }
}
