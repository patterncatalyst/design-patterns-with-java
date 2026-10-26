package com.patterncatalyst.designpatterns.ch23.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ShipmentEventNarratorTest {

    private static final Instant WHEN = Instant.parse("2026-10-20T09:00:00Z");

    @Test
    void describesAPickedUpEvent() {
        ShipmentEvent event = new ShipmentEvent.PickedUp(WHEN, "Akron Hub");

        assertThat(ShipmentEventNarrator.describe(event))
                .isEqualTo("picked up at Akron Hub on " + WHEN);
    }

    @Test
    void describesAnInTransitEvent() {
        ShipmentEvent event = new ShipmentEvent.InTransit(WHEN, "Columbus Hub");

        assertThat(ShipmentEventNarrator.describe(event))
                .isEqualTo("in transit, last seen at Columbus Hub on " + WHEN);
    }

    @Test
    void describesADelayedEvent() {
        ShipmentEvent event = new ShipmentEvent.Delayed(WHEN, "weather", Duration.ofMinutes(150));

        assertThat(ShipmentEventNarrator.describe(event))
                .isEqualTo("delayed (weather), expect an extra 150 minutes, reported " + WHEN);
    }

    @Test
    void describesADeliveredEvent() {
        ShipmentEvent event = new ShipmentEvent.Delivered(WHEN, "J. Alvarez");

        assertThat(ShipmentEventNarrator.describe(event))
                .isEqualTo("delivered on " + WHEN + ", signed by J. Alvarez");
    }

    @Test
    void describesALostEvent() {
        ShipmentEvent event = new ShipmentEvent.Lost(WHEN, "Columbus Hub");

        assertThat(ShipmentEventNarrator.describe(event))
                .isEqualTo("lost, last known location Columbus Hub as of " + WHEN);
    }

    @Test
    void aShortDelayDoesNotRequireNotification() {
        ShipmentEvent event = new ShipmentEvent.Delayed(WHEN, "traffic", Duration.ofMinutes(30));

        assertThat(ShipmentEventNarrator.requiresCustomerNotification(event)).isFalse();
    }

    @Test
    void aLongDelayRequiresNotification() {
        ShipmentEvent event = new ShipmentEvent.Delayed(WHEN, "weather", Duration.ofHours(3));

        assertThat(ShipmentEventNarrator.requiresCustomerNotification(event)).isTrue();
    }

    @Test
    void deliveryAlwaysRequiresNotification() {
        ShipmentEvent event = new ShipmentEvent.Delivered(WHEN, "J. Alvarez");

        assertThat(ShipmentEventNarrator.requiresCustomerNotification(event)).isTrue();
    }

    @Test
    void lossAlwaysRequiresNotification() {
        ShipmentEvent event = new ShipmentEvent.Lost(WHEN, "Columbus Hub");

        assertThat(ShipmentEventNarrator.requiresCustomerNotification(event)).isTrue();
    }

    @Test
    void pickupAndTransitDoNotRequireNotification() {
        assertThat(ShipmentEventNarrator.requiresCustomerNotification(
                new ShipmentEvent.PickedUp(WHEN, "Akron Hub"))).isFalse();
        assertThat(ShipmentEventNarrator.requiresCustomerNotification(
                new ShipmentEvent.InTransit(WHEN, "Columbus Hub"))).isFalse();
    }
}
