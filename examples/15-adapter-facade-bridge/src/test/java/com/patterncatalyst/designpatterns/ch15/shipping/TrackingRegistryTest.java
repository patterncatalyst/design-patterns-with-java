package com.patterncatalyst.designpatterns.ch15.shipping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrackingRegistryTest {

    private final TrackingRegistry registry = new TrackingRegistry("globalcourier.example");

    @Test
    void issuesADistinctNumberPerSequence() {
        assertThat(registry.issueTrackingNumber(1)).isEqualTo("TRK-1001");
        assertThat(registry.issueTrackingNumber(2)).isEqualTo("TRK-1002");
    }

    @Test
    void buildsATrackingUrlFromTheIssuedNumber() {
        String trackingNumber = registry.issueTrackingNumber(1);

        assertThat(registry.trackingUrl(trackingNumber))
                .isEqualTo("https://track.globalcourier.example/track/TRK-1001");
    }
}
