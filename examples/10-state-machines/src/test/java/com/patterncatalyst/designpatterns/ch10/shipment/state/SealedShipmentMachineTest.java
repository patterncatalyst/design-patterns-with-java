package com.patterncatalyst.designpatterns.ch10.shipment.state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.patterncatalyst.designpatterns.ch10.shipment.IllegalTransitionException;
import com.patterncatalyst.designpatterns.ch10.shipment.ShipmentStatus;
import org.junit.jupiter.api.Test;

class SealedShipmentMachineTest {

    @Test
    void legalSequenceReachesDeliveredAndCarriesTrackingData() {
        SealedShipmentMachine machine = new SealedShipmentMachine();

        machine.pack();
        machine.ship("1Z-TEST-0001");

        assertThat(machine.state())
                .isInstanceOf(InTransit.class)
                .extracting(state -> ((InTransit) state).carrierTrackingNumber())
                .isEqualTo("1Z-TEST-0001");

        machine.deliver();

        assertThat(machine.state().status()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(machine.isTerminal()).isTrue();
    }

    @Test
    void cancelCarriesAReason() {
        SealedShipmentMachine machine = new SealedShipmentMachine();
        machine.pack();

        machine.cancel("customer requested cancellation");

        assertThat(machine.state())
                .isInstanceOf(Cancelled.class)
                .extracting(state -> ((Cancelled) state).reason())
                .isEqualTo("customer requested cancellation");
        assertThat(machine.isTerminal()).isTrue();
    }

    @Test
    void returnToSenderFromInTransitCarriesAReason() {
        SealedShipmentMachine machine = new SealedShipmentMachine();
        machine.pack();
        machine.ship("1Z-TEST-0002");

        machine.returnToSender("address undeliverable");

        assertThat(machine.state())
                .isInstanceOf(Returned.class)
                .extracting(state -> ((Returned) state).reason())
                .isEqualTo("address undeliverable");
    }

    @Test
    void shippingBeforePackingIsRefused() {
        SealedShipmentMachine machine = new SealedShipmentMachine();

        assertThatThrownBy(() -> machine.ship("1Z-SHOULD-NOT-SHIP"))
                .isInstanceOf(IllegalTransitionException.class)
                .hasMessageContaining("SHIP")
                .hasMessageContaining("CREATED");

        assertThat(machine.state())
                .describedAs("a rejected event leaves the machine exactly where it was")
                .isInstanceOf(Created.class);
    }

    @Test
    void deliveringBeforeShippingIsRefused() {
        SealedShipmentMachine machine = new SealedShipmentMachine();
        machine.pack();

        assertThatThrownBy(machine::deliver).isInstanceOf(IllegalTransitionException.class);
    }

    @Test
    void cancellingAfterShippingIsRefused() {
        SealedShipmentMachine machine = new SealedShipmentMachine();
        machine.pack();
        machine.ship("1Z-TEST-0003");

        assertThatThrownBy(() -> machine.cancel("too late")).isInstanceOf(IllegalTransitionException.class);
    }

    @Test
    void freshShipmentIsNotTerminal() {
        SealedShipmentMachine machine = new SealedShipmentMachine();

        assertThat(machine.isTerminal()).isFalse();
    }
}
