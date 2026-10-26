package com.patterncatalyst.designpatterns.ch27.shipping.overengineered;

/**
 * OVER-ENGINEERED EXHIBIT. Built exactly as correctly as the enum singleton
 * idiom: the language itself guarantees exactly one {@code INSTANCE} exists
 * for the life of the JVM, and the one mutable field it guards is protected
 * by a {@code synchronized} accessor pair, so this class has no race
 * condition. The mechanism is not this exhibit's problem.
 *
 * <p>The problem is what the mechanism is used for: a single mutable slot,
 * the fuel surcharge multiplier, that more than one part of a pricing
 * pipeline believes it can configure independently. A singleton
 * configuration slot cannot hold two independently valid configurations at
 * the same time, because there is only one slot. See
 * {@code ShippingSurchargeRegistrySpookyActionTest} for the test that
 * forces this cost into the open.
 */
public enum ShippingSurchargeRegistry {
    INSTANCE;

    private volatile double surchargeMultiplier = 1.0;

    public synchronized void setSurchargeMultiplier(double surchargeMultiplier) {
        if (surchargeMultiplier <= 0.0) {
            throw new IllegalArgumentException(
                    "surchargeMultiplier must be positive: " + surchargeMultiplier);
        }
        this.surchargeMultiplier = surchargeMultiplier;
    }

    public double surchargeMultiplier() {
        return surchargeMultiplier;
    }
}
