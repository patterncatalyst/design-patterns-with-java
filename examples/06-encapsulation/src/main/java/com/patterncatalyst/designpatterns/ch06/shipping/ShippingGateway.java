package com.patterncatalyst.designpatterns.ch06.shipping;

import java.util.Optional;

/**
 * The only type a caller outside this package ever depends on to create and
 * track shipments. {@link #inMemory()} is the static factory a caller
 * actually calls; it returns {@code InMemoryShippingGateway}, a
 * package-private class whose name does not appear anywhere outside this
 * file. A caller cannot write {@code new InMemoryShippingGateway()} from a
 * different package even if it wanted to; the compiler refuses to compile
 * it. That is Java's answer to the PImpl idiom: the public interface is the
 * representation this book's catalogue trains you to design around, and the
 * concrete class behind it is free to change shape, swap storage, or be
 * replaced outright without a single caller noticing.
 */
public interface ShippingGateway {

    Shipment createShipment(String origin, String destination, Manifest manifest);

    Optional<Shipment> find(String trackingId);

    Shipment recordEvent(String trackingId, TrackingEvent event);

    static ShippingGateway inMemory() {
        return new InMemoryShippingGateway();
    }
}
