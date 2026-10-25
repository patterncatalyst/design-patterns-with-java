package com.patterncatalyst.designpatterns.ch05.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrackingFeedRegistryTest {

    @Test
    void reusesTheSameHandleForTheSameCarrier() {
        try (TrackingFeedRegistry registry = new TrackingFeedRegistry()) {
            TrackingFeedHandle first = registry.handleFor("fedex-booking.example");
            TrackingFeedHandle second = registry.handleFor("fedex-booking.example");

            assertThat(first).isSameAs(second);
            assertThat(registry.openHandleCount()).isEqualTo(1);
        }
    }

    @Test
    void closingTheRegistryClosesEveryPooledHandle() {
        TrackingFeedRegistry registry = new TrackingFeedRegistry();
        TrackingFeedHandle fedex = registry.handleFor("fedex-booking.example");
        TrackingFeedHandle ups = registry.handleFor("ups-booking.example");

        registry.close();

        assertThat(fedex.isSubscribed()).isFalse();
        assertThat(ups.isSubscribed()).isFalse();
        assertThat(registry.openHandleCount()).isZero();
    }
}
