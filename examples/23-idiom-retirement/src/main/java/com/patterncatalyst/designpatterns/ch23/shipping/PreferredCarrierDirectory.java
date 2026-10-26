package com.patterncatalyst.designpatterns.ch23.shipping;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Looks up a customer's preferred carrier, when one is on file. Before
 * {@link Optional}, a lookup like this either returned {@code null} and
 * pushed a scattered null check onto every caller, or returned a
 * {@code NullCarrierPreference} Null Object that had to implement the same
 * interface as a real preference just to answer "no preference" safely.
 * Returning {@code Optional<String>} retires both: absence is a value the
 * type system already accounts for, and a caller composes past it with
 * {@code map}, {@code filter}, and {@code orElse} instead of branching on
 * {@code null} or dispatching to a do-nothing object.
 */
public final class PreferredCarrierDirectory {

    private final Map<String, String> preferredCarrierByCustomerId;

    public PreferredCarrierDirectory(Map<String, String> preferredCarrierByCustomerId) {
        this.preferredCarrierByCustomerId =
                Map.copyOf(Objects.requireNonNull(preferredCarrierByCustomerId, "preferredCarrierByCustomerId"));
    }

    public Optional<String> preferredCarrierFor(String customerId) {
        Objects.requireNonNull(customerId, "customerId");
        return Optional.ofNullable(preferredCarrierByCustomerId.get(customerId));
    }
}
