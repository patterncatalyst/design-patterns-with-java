package com.patterncatalyst.designpatterns.ch25.dispatch;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simulated connection to a carrier's tracking API. Opening one stands in
 * for the handshake a real HTTP or gRPC client pays once per connection, a
 * DNS lookup, a TCP handshake, and often a TLS negotiation and an
 * authenticated session, work that costs orders of magnitude more than the
 * lookup it then serves. {@link CarrierConnectionPool} exists so a dispatch
 * service pays that setup cost a bounded number of times, not once per
 * tracking request.
 */
public final class CarrierApiConnection {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private final int id;

    public CarrierApiConnection() {
        this.id = NEXT_ID.getAndIncrement();
    }

    public int id() {
        return id;
    }

    public String fetchTrackingStatus(String parcelId) {
        return "status for " + parcelId + " via connection-" + id;
    }
}
