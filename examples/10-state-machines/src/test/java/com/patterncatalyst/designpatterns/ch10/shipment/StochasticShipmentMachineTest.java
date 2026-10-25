package com.patterncatalyst.designpatterns.ch10.shipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class StochasticShipmentMachineTest {

    @Test
    void packAndShipAreDeterministicAndLegal() {
        StochasticShipmentMachine machine = new StochasticShipmentMachine(new Random(1));

        machine.pack();
        machine.ship();

        assertThat(machine.status()).isEqualTo(ShipmentStatus.IN_TRANSIT);
    }

    @Test
    void shippingBeforePackingIsRefused() {
        StochasticShipmentMachine machine = new StochasticShipmentMachine(new Random(1));

        assertThatThrownBy(machine::ship).isInstanceOf(IllegalTransitionException.class);
    }

    @Test
    void advanceOutsideInTransitIsRefused() {
        StochasticShipmentMachine machine = new StochasticShipmentMachine(new Random(1));

        assertThatThrownBy(machine::advance)
                .isInstanceOf(IllegalTransitionException.class)
                .hasMessageContaining("ADVANCE")
                .hasMessageContaining("CREATED");
    }

    @Test
    void aSeededRunProducesAnExactReproducibleSequence() {
        StochasticShipmentMachine first = new StochasticShipmentMachine(new Random(38));
        first.pack();
        first.ship();
        List<ShipmentStatus> firstRun = first.runToTerminal(10_000);

        StochasticShipmentMachine second = new StochasticShipmentMachine(new Random(38));
        second.pack();
        second.ship();
        List<ShipmentStatus> secondRun = second.runToTerminal(10_000);

        assertThat(secondRun).isEqualTo(firstRun);
    }

    @Test
    void runToTerminalAlwaysEndsOnDeliveredOrLost() {
        for (long seed = 0; seed < 200; seed++) {
            StochasticShipmentMachine machine = new StochasticShipmentMachine(new Random(seed));
            machine.pack();
            machine.ship();

            List<ShipmentStatus> observed = machine.runToTerminal(10_000);

            assertThat(observed).isNotEmpty();
            ShipmentStatus last = observed.getLast();
            assertThat(last).isIn(ShipmentStatus.DELIVERED, ShipmentStatus.LOST);
            assertThat(machine.status()).isEqualTo(last);
            observed.subList(0, observed.size() - 1)
                    .forEach(status -> assertThat(status).isEqualTo(ShipmentStatus.IN_TRANSIT));
        }
    }

    @Test
    void manySingleDrawsLandWithinTheExpectedWeightedDistribution() {
        int trials = 20_000;
        int delivered = 0;
        int delayed = 0;
        int lost = 0;
        Random rng = new Random(2026);

        for (int i = 0; i < trials; i++) {
            StochasticShipmentMachine machine =
                    new StochasticShipmentMachine(rng, ShipmentStatus.IN_TRANSIT);
            ShipmentStatus outcome = machine.advance();
            switch (outcome) {
                case DELIVERED -> delivered++;
                case IN_TRANSIT -> delayed++;
                case LOST -> lost++;
                default -> throw new AssertionError("unexpected outcome: " + outcome);
            }
        }

        double deliveredShare = (double) delivered / trials;
        double delayedShare = (double) delayed / trials;
        double lostShare = (double) lost / trials;

        assertThat(deliveredShare).isCloseTo(0.85, org.assertj.core.data.Offset.offset(0.02));
        assertThat(delayedShare).isCloseTo(0.10, org.assertj.core.data.Offset.offset(0.02));
        assertThat(lostShare).isCloseTo(0.05, org.assertj.core.data.Offset.offset(0.02));
    }
}
