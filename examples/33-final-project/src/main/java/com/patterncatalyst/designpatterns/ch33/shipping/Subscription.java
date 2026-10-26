package com.patterncatalyst.designpatterns.ch33.shipping;

/**
 * A handle returned by {@link Shipment#subscribe(ShipmentStatusListener)}.
 * Closing it removes the listener, which makes a subscription a natural fit
 * for try-with-resources. {@code close()} declares no checked exception,
 * because removing a listener from a map cannot fail.
 */
public interface Subscription extends AutoCloseable {

    @Override
    void close();
}
