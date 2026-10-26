package com.patterncatalyst.designpatterns.ch23.shipping;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The warehouse system's one source of shipment confirmation numbers. Every
 * facility that hands a package to a carrier needs a number no other
 * facility can produce at the same moment, which is exactly the force that
 * used to be resolved with double-checked locking over a {@code volatile}
 * field: lazy construction, one instance, safe under concurrent callers.
 *
 * <p>The enum form gets all three guarantees from the language itself. The
 * JVM constructs {@code INSTANCE} exactly once when this class loads, no
 * {@code synchronized} block anywhere advertises a locking cost, and the
 * {@link AtomicLong} counter is what carries the per-call thread safety
 * once the singleton itself already exists.
 */
public enum ConfirmationNumberGenerator {
    INSTANCE;

    private final AtomicLong sequence = new AtomicLong(100_000L);

    /**
     * Returns a confirmation number unique across every caller of this
     * method for the life of the process, in {@code CARRIER-NNNNNN} form.
     */
    public String nextConfirmationNumber(String carrierCode) {
        Objects.requireNonNull(carrierCode, "carrierCode");
        long next = sequence.getAndIncrement();
        return carrierCode.toUpperCase(Locale.ROOT) + "-" + next;
    }
}
