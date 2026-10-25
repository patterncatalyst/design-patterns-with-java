package com.patterncatalyst.designpatterns.ch10.shipment;

import java.util.Objects;

/**
 * One possible draw out of a stochastic transition: a status and the
 * probability it is chosen. A list of these, summing to 1.0, is the entire
 * rule set {@link StochasticShipmentMachine#advance()} draws from.
 */
public record WeightedOutcome(ShipmentStatus outcome, double weight) {

    public WeightedOutcome {
        Objects.requireNonNull(outcome, "outcome");
        if (weight <= 0.0 || weight > 1.0) {
            throw new IllegalArgumentException("weight must be in (0, 1]: " + weight);
        }
    }
}
