package com.patterncatalyst.designpatterns.ch18.shipping.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link ShipmentTracker} is the Observer subject: it must notify every
 * currently subscribed listener on a status change, stop notifying a
 * listener once its subscription is closed, and refuse a subscribe once the
 * shipment has reached a terminal status.
 */
@ExtendWith(MockitoExtension.class)
class ShipmentTrackerTest {

    @Mock
    private ShipmentStatusListener listener;

    @Test
    void subscribedListenerIsNotifiedOnStatusChange() {
        ShipmentTracker tracker = new ShipmentTracker("TRK-1001");
        tracker.subscribe(listener);

        tracker.advanceStatus(ShipmentStatus.PICKED_UP);

        verify(listener).onStatusChanged("TRK-1001", ShipmentStatus.CREATED, ShipmentStatus.PICKED_UP);
    }

    @Test
    void unsubscribedListenerIsNotNotified() {
        ShipmentTracker tracker = new ShipmentTracker("TRK-1002");
        Subscription subscription = tracker.subscribe(listener);

        subscription.close();
        tracker.advanceStatus(ShipmentStatus.PICKED_UP);

        verify(listener, never()).onStatusChanged(eq("TRK-1002"), any(), any());
        assertThat(tracker.subscriberCount()).isZero();
    }

    @Test
    void multipleListenersAreNotifiedInSubscriptionOrder() {
        ShipmentTracker tracker = new ShipmentTracker("TRK-1003");
        List<String> order = new ArrayList<>();
        tracker.subscribe((trackingId, oldStatus, newStatus) -> order.add("first"));
        tracker.subscribe((trackingId, oldStatus, newStatus) -> order.add("second"));

        tracker.advanceStatus(ShipmentStatus.PICKED_UP);

        assertThat(order).containsExactly("first", "second");
    }

    @Test
    void subscribingToAShipmentAlreadyInATerminalStatusThrows() {
        ShipmentTracker tracker = new ShipmentTracker("TRK-1004");
        tracker.advanceStatus(ShipmentStatus.PICKED_UP);
        tracker.advanceStatus(ShipmentStatus.DELIVERED);

        assertThatThrownBy(() -> tracker.subscribe(listener))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TRK-1004");
    }

    @Test
    void advancingPastATerminalStatusThrows() {
        ShipmentTracker tracker = new ShipmentTracker("TRK-1005");
        tracker.advanceStatus(ShipmentStatus.EXCEPTION);

        assertThatThrownBy(() -> tracker.advanceStatus(ShipmentStatus.IN_TRANSIT))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aListenerThatUnsubscribesDuringNotificationDoesNotBreakTheRemainingFanOut() {
        ShipmentTracker tracker = new ShipmentTracker("TRK-1006");
        List<String> notified = new ArrayList<>();
        Subscription[] selfUnsubscribing = new Subscription[1];
        selfUnsubscribing[0] = tracker.subscribe((trackingId, oldStatus, newStatus) -> {
            notified.add("self-unsubscribing");
            selfUnsubscribing[0].close();
        });
        tracker.subscribe((trackingId, oldStatus, newStatus) -> notified.add("second"));

        tracker.advanceStatus(ShipmentStatus.PICKED_UP);

        assertThat(notified).containsExactly("self-unsubscribing", "second");
        assertThat(tracker.subscriberCount()).isOne();
    }
}
