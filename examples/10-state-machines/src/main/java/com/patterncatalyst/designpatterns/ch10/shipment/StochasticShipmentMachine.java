package com.patterncatalyst.designpatterns.ch10.shipment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Every machine above waits for a caller to issue an event. This one breaks
 * that contract for exactly one transition: once a shipment is
 * {@code IN_TRANSIT}, nothing calls {@code deliver()} on it. Instead,
 * {@link #advance()} draws a weighted outcome from {@link #IN_TRANSIT_OUTCOMES}
 * using an injected {@link RandomGenerator}, so a production caller can pass
 * a real, unpredictable generator while a test passes a seeded one and gets
 * an exact, reproducible sequence back.
 */
public final class StochasticShipmentMachine {

    private static final List<WeightedOutcome> IN_TRANSIT_OUTCOMES = List.of(
            new WeightedOutcome(ShipmentStatus.DELIVERED, 0.85),
            new WeightedOutcome(ShipmentStatus.IN_TRANSIT, 0.10),
            new WeightedOutcome(ShipmentStatus.LOST, 0.05));

    private final RandomGenerator rng;
    private ShipmentStatus status;

    public StochasticShipmentMachine(RandomGenerator rng) {
        this(rng, ShipmentStatus.CREATED);
    }

    public StochasticShipmentMachine(RandomGenerator rng, ShipmentStatus initial) {
        this.rng = Objects.requireNonNull(rng, "rng");
        this.status = Objects.requireNonNull(initial, "initial");
    }

    public ShipmentStatus status() {
        return status;
    }

    public void pack() {
        requireStatus(ShipmentStatus.CREATED, ShipmentEvent.PACK);
        status = ShipmentStatus.PACKED;
    }

    public void ship() {
        requireStatus(ShipmentStatus.PACKED, ShipmentEvent.SHIP);
        status = ShipmentStatus.IN_TRANSIT;
    }

    public void cancel() {
        if (status != ShipmentStatus.CREATED && status != ShipmentStatus.PACKED) {
            throw new IllegalTransitionException(status, ShipmentEvent.CANCEL);
        }
        status = ShipmentStatus.CANCELLED;
    }

    /**
     * Draws a weighted outcome for a shipment currently {@code IN_TRANSIT}.
     * Eighty-five times in a hundred the draw lands on {@code DELIVERED}.
     * Ten times in a hundred it lands back on {@code IN_TRANSIT} itself,
     * modeling a carrier delay with no status change. Five times in a
     * hundred it lands on {@code LOST}, a status no deterministic event in
     * this example can ever produce.
     */
    public ShipmentStatus advance() {
        if (status != ShipmentStatus.IN_TRANSIT) {
            throw new IllegalTransitionException(status, ShipmentEvent.ADVANCE);
        }
        double draw = rng.nextDouble();
        double cumulative = 0.0;
        for (WeightedOutcome candidate : IN_TRANSIT_OUTCOMES) {
            cumulative += candidate.weight();
            if (draw < cumulative) {
                status = candidate.outcome();
                return status;
            }
        }
        status = IN_TRANSIT_OUTCOMES.getLast().outcome();
        return status;
    }

    /**
     * Calls {@link #advance()} in a loop until the status stops being
     * {@code IN_TRANSIT}, collecting every status observed along the way.
     * This is what turns a single weighted draw into a self-looping chain:
     * a shipment can be delayed zero, one, or several times before it
     * finally resolves.
     */
    public List<ShipmentStatus> runToTerminal(int maxSteps) {
        List<ShipmentStatus> observed = new ArrayList<>();
        int steps = 0;
        while (status == ShipmentStatus.IN_TRANSIT && steps < maxSteps) {
            observed.add(advance());
            steps++;
        }
        return List.copyOf(observed);
    }

    private void requireStatus(ShipmentStatus required, ShipmentEvent event) {
        if (status != required) {
            throw new IllegalTransitionException(status, event);
        }
    }
}
