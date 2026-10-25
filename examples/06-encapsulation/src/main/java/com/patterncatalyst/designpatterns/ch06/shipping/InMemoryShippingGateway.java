package com.patterncatalyst.designpatterns.ch06.shipping;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The implementation {@link ShippingGateway#inMemory()} hands back. No
 * access modifier on this class declaration means package-private: a class
 * anywhere outside {@code com.patterncatalyst.designpatterns.ch06.shipping}
 * cannot reference it by name, construct it, or cast to it, even though it
 * can hold and call it through the {@link ShippingGateway} reference the
 * factory returned. That is the whole implementation hidden behind a public
 * contract, not by convention or a Javadoc warning, but by the compiler.
 */
final class InMemoryShippingGateway implements ShippingGateway {

    private final Map<String, Shipment> shipmentsByTrackingId = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public Shipment createShipment(String origin, String destination, Manifest manifest) {
        String trackingId = "SHIP-" + sequence.incrementAndGet();
        Shipment shipment = Shipment.created(trackingId, origin, destination, manifest);
        shipmentsByTrackingId.put(trackingId, shipment);
        return shipment;
    }

    @Override
    public Optional<Shipment> find(String trackingId) {
        return Optional.ofNullable(shipmentsByTrackingId.get(Objects.requireNonNull(trackingId, "trackingId")));
    }

    @Override
    public Shipment recordEvent(String trackingId, TrackingEvent event) {
        Objects.requireNonNull(trackingId, "trackingId");
        Objects.requireNonNull(event, "event");
        return shipmentsByTrackingId.compute(trackingId, (id, existing) -> {
            if (existing == null) {
                throw new NoSuchElementException("no shipment tracked under " + trackingId);
            }
            return existing.withEvent(event);
        });
    }
}
