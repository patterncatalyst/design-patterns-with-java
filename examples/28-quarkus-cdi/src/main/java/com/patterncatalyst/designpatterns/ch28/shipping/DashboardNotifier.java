package com.patterncatalyst.designpatterns.ch28.shipping;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * One of two independent observers of {@link ShipmentStatusChanged}. This
 * bean never references {@link CheckoutService}, never registers a
 * listener, and never calls {@code subscribe()} on anything: declaring an
 * {@code @Observes} parameter is the entire subscription, the fan-out this
 * book's Observer chapter built with a listener list and a
 * {@code Subscription} handle.
 */
@ApplicationScoped
public class DashboardNotifier {

    private volatile String currentStatusLine = "no shipment dispatched yet";

    public void onStatusChanged(@Observes ShipmentStatusChanged event) {
        currentStatusLine = event.trackingId() + ": " + event.previousStatus() + " -> " + event.newStatus();
    }

    public String currentStatusLine() {
        return currentStatusLine;
    }
}
