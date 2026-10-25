package com.patterncatalyst.designpatterns.ch10.shipment;

import com.patterncatalyst.designpatterns.ch10.shipment.state.SealedShipmentMachine;
import java.util.List;
import java.util.Random;

/**
 * Drives the same Pack -> Ship -> Deliver event sequence through the enum
 * machine and the sealed-interface machine, shows an illegal transition
 * being refused, and then runs the stochastic machine from a fixed seed, so
 * the agreement this example's tests check for is also something you can
 * watch happen at the terminal.
 */
public final class ShipmentDemo {

    private ShipmentDemo() {
    }

    public static void main(String[] args) {
        System.out.println("== Expression one: enum + transition table ==");
        EnumShipmentMachine enumMachine = new EnumShipmentMachine();
        System.out.println("Start: " + enumMachine.status());
        enumMachine.apply(ShipmentEvent.PACK);
        enumMachine.apply(ShipmentEvent.SHIP);
        enumMachine.apply(ShipmentEvent.DELIVER);
        System.out.println("After pack/ship/deliver: " + enumMachine.status());

        System.out.println();
        System.out.println("== Expression two: sealed interface + pattern-matching switch ==");
        SealedShipmentMachine sealedMachine = new SealedShipmentMachine();
        System.out.println("Start: " + sealedMachine.state());
        sealedMachine.pack();
        sealedMachine.ship("1Z-DEMO-TRACKING");
        sealedMachine.deliver();
        System.out.println("After pack/ship/deliver: " + sealedMachine.state());
        System.out.println("isTerminal(): " + sealedMachine.isTerminal());

        System.out.println();
        System.out.println("== Refused illegal transition ==");
        EnumShipmentMachine freshMachine = new EnumShipmentMachine();
        try {
            freshMachine.apply(ShipmentEvent.SHIP);
        } catch (IllegalTransitionException rejected) {
            System.out.println("Rejected: " + rejected.getMessage());
        }

        System.out.println();
        System.out.println("== Expression three: stochastic FSM (seeded for reproducibility) ==");
        long seed = 6_233_379_223_152_327_722L;
        StochasticShipmentMachine stochastic = new StochasticShipmentMachine(new Random(seed));
        stochastic.pack();
        stochastic.ship();
        List<ShipmentStatus> observed = stochastic.runToTerminal(10_000);
        System.out.println("Seed " + seed + ", IN_TRANSIT outcomes: " + observed);
        System.out.println("Final status: " + stochastic.status());
    }
}
