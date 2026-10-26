package com.patterncatalyst.designpatterns.ch22.fulfillment;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A base shipping rate per {@link DestinationZone}, which does not
 * necessarily cover every zone this warehouse might one day ship to.
 * {@link #baseRateFor} returns {@link Optional} rather than {@code null} or
 * a sentinel {@link Money} value, so a caller cannot forget there is a
 * missing case to handle; the method's return type says so. This class
 * never stores an {@code Optional} field itself, only ever returns one: the
 * table underneath is a plain {@code Map}, because a value either known or
 * completely absent is the right shape for a method's answer, not for a
 * piece of state this object carries around.
 */
public final class ZoneRateLookup {

    private final Map<DestinationZone, Money> baseRates;

    public ZoneRateLookup(Map<DestinationZone, Money> baseRates) {
        this.baseRates = Map.copyOf(Objects.requireNonNull(baseRates, "baseRates"));
    }

    public Optional<Money> baseRateFor(DestinationZone zone) {
        Objects.requireNonNull(zone, "zone");
        return Optional.ofNullable(baseRates.get(zone));
    }
}
