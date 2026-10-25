package com.patterncatalyst.designpatterns.ch10.shipment;

import static org.assertj.core.api.Assertions.assertThat;

import com.patterncatalyst.designpatterns.ch10.shipment.state.SealedShipmentMachine;
import org.junit.jupiter.api.Test;

/**
 * The enum-plus-table machine and the sealed-interface machine encode the
 * shipment lifecycle independently, in their own idiom. This test drives the
 * same event sequence through both and asserts they land on the same status
 * after every step, which only means something because neither one
 * delegates to the other.
 */
class MachineEquivalenceTest {

    @Test
    void bothMachinesAgreeAcrossAFullHappyPathSequence() {
        EnumShipmentMachine enumMachine = new EnumShipmentMachine();
        SealedShipmentMachine sealedMachine = new SealedShipmentMachine();
        assertThat(enumMachine.status()).isEqualTo(sealedMachine.state().status());

        enumMachine.apply(ShipmentEvent.PACK);
        sealedMachine.pack();
        assertThat(enumMachine.status()).isEqualTo(sealedMachine.state().status());

        enumMachine.apply(ShipmentEvent.SHIP);
        sealedMachine.ship("1Z-EQUIV-0001");
        assertThat(enumMachine.status()).isEqualTo(sealedMachine.state().status());

        enumMachine.apply(ShipmentEvent.DELIVER);
        sealedMachine.deliver();
        assertThat(enumMachine.status()).isEqualTo(sealedMachine.state().status());
        assertThat(enumMachine.status()).isEqualTo(ShipmentStatus.DELIVERED);
    }

    @Test
    void bothMachinesAgreeAcrossACancellationSequence() {
        EnumShipmentMachine enumMachine = new EnumShipmentMachine();
        SealedShipmentMachine sealedMachine = new SealedShipmentMachine();

        enumMachine.apply(ShipmentEvent.PACK);
        sealedMachine.pack();

        enumMachine.apply(ShipmentEvent.CANCEL);
        sealedMachine.cancel("duplicate order");

        assertThat(enumMachine.status()).isEqualTo(sealedMachine.state().status());
        assertThat(enumMachine.status()).isEqualTo(ShipmentStatus.CANCELLED);
    }
}
