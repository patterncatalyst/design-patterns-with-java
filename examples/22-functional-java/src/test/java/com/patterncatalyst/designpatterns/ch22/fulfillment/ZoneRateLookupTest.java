package com.patterncatalyst.designpatterns.ch22.fulfillment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * {@code baseRateFor} returns {@link Optional}, the honest "maybe" a caller
 * cannot ignore the way it could ignore a {@code null} return value. This
 * exercises both the present and the absent case, and the idiomatic
 * {@code Optional} chain a caller uses to handle each.
 */
class ZoneRateLookupTest {

    private final ZoneRateLookup lookup = new ZoneRateLookup(Map.of(
            DestinationZone.LOCAL, Money.ofCents(300),
            DestinationZone.REGIONAL, Money.ofCents(650)));

    @Test
    void returnsAPresentOptionalForAPricedZone() {
        Optional<Money> rate = lookup.baseRateFor(DestinationZone.LOCAL);

        assertThat(rate).contains(Money.ofCents(300));
    }

    @Test
    void returnsAnEmptyOptionalForAZoneWithNoBaseRate() {
        Optional<Money> rate = lookup.baseRateFor(DestinationZone.INTERNATIONAL);

        assertThat(rate).isEmpty();
    }

    @Test
    void mapAndOrElseChainThroughAMissingRateWithoutANullCheck() {
        String description = lookup.baseRateFor(DestinationZone.INTERNATIONAL)
                .map(Money::toString)
                .orElse("not priced");

        assertThat(description).isEqualTo("not priced");
    }

    @Test
    void orElseThrowSurfacesAMissingRateAsAnException() {
        assertThatThrownBy(() ->
                        lookup.baseRateFor(DestinationZone.INTERNATIONAL)
                                .orElseThrow(() -> new NoSuchElementException("no rate")))
                .isInstanceOf(NoSuchElementException.class);
    }
}
