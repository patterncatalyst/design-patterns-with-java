package com.patterncatalyst.designpatterns.ch05.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrackingFeedHandleTest {

    @Test
    void pollsAnInTransitUpdateWhileSubscribed() {
        TrackingFeedHandle handle = TrackingFeedHandle.subscribe("fedex-booking.example");

        TrackingUpdate update = handle.poll("PKG-5001");

        assertThat(update.trackingId()).isEqualTo("PKG-5001");
        assertThat(update.status()).isEqualTo("IN_TRANSIT");
    }

    @Test
    void closeEndsTheSubscription() {
        TrackingFeedHandle handle = TrackingFeedHandle.subscribe("fedex-booking.example");

        handle.close();

        assertThat(handle.isSubscribed()).isFalse();
    }

    @Test
    void refusesToPollAfterClose() {
        TrackingFeedHandle handle = TrackingFeedHandle.subscribe("fedex-booking.example");
        handle.close();

        assertThatThrownBy(() -> handle.poll("PKG-5002"))
                .isInstanceOf(IllegalStateException.class);
    }
}
