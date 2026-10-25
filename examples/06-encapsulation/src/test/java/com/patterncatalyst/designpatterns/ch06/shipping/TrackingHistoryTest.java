package com.patterncatalyst.designpatterns.ch06.shipping;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrackingHistoryTest {

    @Test
    void emptyReturnsTheSameCachedInstanceEveryTime() {
        assertThat(TrackingHistory.empty()).isSameAs(TrackingHistory.empty());
    }

    @Test
    void compactConstructorDefensivelyCopiesTheCallersList() {
        List<TrackingEvent> mutable = new ArrayList<>();
        mutable.add(new TrackingEvent(Instant.parse("2026-01-01T00:00:00Z"), "Origin", TrackingStatus.CREATED));

        TrackingHistory history = new TrackingHistory(mutable);
        mutable.add(new TrackingEvent(Instant.parse("2026-01-02T00:00:00Z"), "Added later", TrackingStatus.EXCEPTION));

        assertThat(history.events()).hasSize(1);
    }

    @Test
    void eventsCannotBeMutatedByACaller() {
        TrackingHistory history = TrackingHistory.empty()
                .withEvent(new TrackingEvent(Instant.parse("2026-01-01T00:00:00Z"), "Origin", TrackingStatus.CREATED));

        assertThatThrownBy(() -> history.events().add(
                new TrackingEvent(Instant.parse("2026-01-02T00:00:00Z"), "Hack", TrackingStatus.EXCEPTION)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void withEventReturnsANewHistoryAndLeavesTheOriginalUntouched() {
        TrackingHistory original = TrackingHistory.empty();
        TrackingEvent event = new TrackingEvent(Instant.parse("2026-01-01T00:00:00Z"), "Origin", TrackingStatus.CREATED);

        TrackingHistory updated = original.withEvent(event);

        assertThat(original.events()).isEmpty();
        assertThat(updated.events()).containsExactly(event);
        assertThat(updated.latest()).contains(event);
    }
}
