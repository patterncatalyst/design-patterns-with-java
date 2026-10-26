package com.patterncatalyst.designpatterns.ch15.shipping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that a notification kind and a delivery channel vary
 * independently: every combination of the two notification kinds and the two
 * channels this example ships produces the correct message through the
 * correct channel, with neither axis's classes mentioning the other.
 */
class NotificationBridgeTest {

    private static Stream<Arguments> kindsCrossedWithChannels() {
        Function<DeliveryChannel, Notification> created =
                channel -> new ShipmentCreatedNotification(channel, "TRK-1001");
        Function<DeliveryChannel, Notification> delayed =
                channel -> new DeliveryDelayedNotification(channel, "TRK-1001", Duration.ofHours(6));

        return Stream.of(
                Arguments.of(created, new EmailDeliveryChannel(),
                        "EMAIL to ops@example.com: Your shipment TRK-1001 has been created."),
                Arguments.of(created, new SmsDeliveryChannel(),
                        "SMS to ops@example.com: Your shipment TRK-1001 has been created."),
                Arguments.of(delayed, new EmailDeliveryChannel(),
                        "EMAIL to ops@example.com: Shipment TRK-1001 is delayed by 6 hours."),
                Arguments.of(delayed, new SmsDeliveryChannel(),
                        "SMS to ops@example.com: Shipment TRK-1001 is delayed by 6 hours."));
    }

    @ParameterizedTest
    @MethodSource("kindsCrossedWithChannels")
    void everyKindAndChannelCombinationDeliversTheExpectedMessage(
            Function<DeliveryChannel, Notification> notificationFactory,
            DeliveryChannel channel,
            String expectedDelivery) {
        Notification notification = notificationFactory.apply(channel);

        assertThat(notification.send("ops@example.com")).isEqualTo(expectedDelivery);
    }

    @Test
    void aThirdNotificationKindNeedsNoChangeToAnyDeliveryChannel() {
        DeliveryChannel channel = new EmailDeliveryChannel();
        Notification pickupScheduled = new Notification(channel) {
            @Override
            protected String formatMessage() {
                return "A pickup has been scheduled for TRK-1001.";
            }
        };

        assertThat(pickupScheduled.send("ops@example.com"))
                .isEqualTo("EMAIL to ops@example.com: A pickup has been scheduled for TRK-1001.");
    }
}
