package com.patterncatalyst.designpatterns.ch14.shipping;

import java.util.Map;

/**
 * The initialization-on-demand holder idiom: a lazy, thread-safe singleton
 * with no {@code synchronized} keyword anywhere in sight. The outer class
 * loads with no side effect at all; the nested {@link Holder} class, and the
 * {@code INSTANCE} field inside it, are not loaded or initialized until
 * {@link #getInstance()} is first called.
 *
 * <p>The thread safety comes from the Java Language Specification's class
 * initialization rules, not from any lock this code writes. The JVM
 * guarantees that a class's static initializers run exactly once, and that
 * every thread racing to trigger that initialization blocks until it
 * completes. Two threads calling {@link #getInstance()} for the first time
 * at the same moment cannot observe two different instances, and neither
 * thread pays a locking cost on every subsequent call, because there is no
 * lock left to acquire once {@link Holder} has finished loading.
 */
public final class HolderCarrierRateTable implements RateLookup {

    private final Map<String, Money> ratesByCarrierCode;

    private HolderCarrierRateTable() {
        this.ratesByCarrierCode = CarrierRates.baseRatesByCarrierCode();
    }

    public static HolderCarrierRateTable getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public Money baseRateFor(String carrierCode) {
        Money rate = ratesByCarrierCode.get(carrierCode);
        if (rate == null) {
            throw new IllegalArgumentException("no base rate for carrier code: " + carrierCode);
        }
        return rate;
    }

    private static final class Holder {
        private static final HolderCarrierRateTable INSTANCE = new HolderCarrierRateTable();
    }
}
