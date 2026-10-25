package com.patterncatalyst.designpatterns.ch10.shipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EnumShipmentMachineTest {

    @Test
    void legalSequenceReachesDelivered() {
        EnumShipmentMachine machine = new EnumShipmentMachine();

        machine.apply(ShipmentEvent.PACK);
        machine.apply(ShipmentEvent.SHIP);
        machine.apply(ShipmentEvent.DELIVER);

        assertThat(machine.status()).isEqualTo(ShipmentStatus.DELIVERED);
    }

    @Test
    void cancelFromCreatedIsLegal() {
        EnumShipmentMachine machine = new EnumShipmentMachine();

        machine.apply(ShipmentEvent.CANCEL);

        assertThat(machine.status()).isEqualTo(ShipmentStatus.CANCELLED);
    }

    @Test
    void cancelFromPackedIsLegal() {
        EnumShipmentMachine machine = new EnumShipmentMachine();
        machine.apply(ShipmentEvent.PACK);

        machine.apply(ShipmentEvent.CANCEL);

        assertThat(machine.status()).isEqualTo(ShipmentStatus.CANCELLED);
    }

    @Test
    void returnToSenderFromInTransitIsLegal() {
        EnumShipmentMachine machine = new EnumShipmentMachine();
        machine.apply(ShipmentEvent.PACK);
        machine.apply(ShipmentEvent.SHIP);

        machine.apply(ShipmentEvent.RETURN_TO_SENDER);

        assertThat(machine.status()).isEqualTo(ShipmentStatus.RETURNED);
    }

    @Test
    void shippingBeforePackingIsRefused() {
        EnumShipmentMachine machine = new EnumShipmentMachine();

        assertThatThrownBy(() -> machine.apply(ShipmentEvent.SHIP))
                .isInstanceOf(IllegalTransitionException.class)
                .hasMessageContaining("SHIP")
                .hasMessageContaining("CREATED");

        assertThat(machine.status())
                .describedAs("a rejected event leaves the machine exactly where it was")
                .isEqualTo(ShipmentStatus.CREATED);
    }

    @Test
    void cancellingAfterShippingIsRefused() {
        EnumShipmentMachine machine = new EnumShipmentMachine();
        machine.apply(ShipmentEvent.PACK);
        machine.apply(ShipmentEvent.SHIP);

        assertThatThrownBy(() -> machine.apply(ShipmentEvent.CANCEL))
                .isInstanceOf(IllegalTransitionException.class);
    }

    @Test
    void everyEventIsIllegalOnceDelivered() {
        EnumShipmentMachine machine = new EnumShipmentMachine();
        machine.apply(ShipmentEvent.PACK);
        machine.apply(ShipmentEvent.SHIP);
        machine.apply(ShipmentEvent.DELIVER);

        for (ShipmentEvent event : ShipmentEvent.values()) {
            if (event == ShipmentEvent.ADVANCE) {
                continue;
            }
            assertThatThrownBy(() -> machine.apply(event))
                    .as("event %s should be refused once delivered", event)
                    .isInstanceOf(IllegalTransitionException.class);
        }
    }
}
