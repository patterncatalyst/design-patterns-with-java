package com.patterncatalyst.designpatterns.ch14.shipping;

import java.util.Map;

/**
 * The enum singleton, Bloch's recommended form for a type that must have
 * exactly one instance. The language itself guarantees exactly one
 * {@code INSTANCE} exists for the life of the JVM: there is no second
 * constructor to call, reflection cannot forge a second instance of an enum
 * type, and serialization round-trips back to the same constant with no
 * {@code readResolve} method required.
 *
 * <p>This table is the case for a singleton that actually holds up: it is
 * read-only after construction, loaded once from {@link CarrierRates}, and
 * nothing about it ever needs a second, different instance.
 */
public enum CarrierRateTable implements RateLookup {
    INSTANCE;

    private final Map<String, Money> ratesByCarrierCode = CarrierRates.baseRatesByCarrierCode();

    @Override
    public Money baseRateFor(String carrierCode) {
        Money rate = ratesByCarrierCode.get(carrierCode);
        if (rate == null) {
            throw new IllegalArgumentException("no base rate for carrier code: " + carrierCode);
        }
        return rate;
    }
}
