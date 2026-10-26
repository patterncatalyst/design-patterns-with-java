package com.patterncatalyst.designpatterns.ch19.shipping.manifest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A warehouse manifest that does not store its shipments as one flat list.
 * Shipments are sorted into one bucket per {@link DestinationZone} as they
 * arrive, because that is how a warehouse actually handles inbound freight.
 * Implementing {@link Iterable} is what lets a caller walk every shipment in
 * zone order with no idea the buckets exist underneath; {@link #stream()}
 * then works with no additional code, built on the {@link
 * Iterable#spliterator()} default method rather than any bucket-aware code
 * of its own.
 */
public final class ShipmentManifest implements Iterable<Shipment> {

    private final EnumMap<DestinationZone, List<Shipment>> byZone = new EnumMap<>(DestinationZone.class);

    public ShipmentManifest() {
        for (DestinationZone zone : DestinationZone.values()) {
            byZone.put(zone, new ArrayList<>());
        }
    }

    public void add(Shipment shipment) {
        Objects.requireNonNull(shipment, "shipment");
        byZone.get(shipment.zone()).add(shipment);
    }

    @Override
    public Iterator<Shipment> iterator() {
        return new ZoneOrderIterator();
    }

    /** The same traversal as {@link #iterator()}, expressed as a Stream. */
    public Stream<Shipment> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Flattens the four zone buckets into one forward traversal, in
     * {@link DestinationZone} declaration order. A caller holding this
     * iterator has no way to observe that the manifest underneath it is
     * four lists rather than one.
     */
    private final class ZoneOrderIterator implements Iterator<Shipment> {

        private final Iterator<DestinationZone> zones = List.of(DestinationZone.values()).iterator();
        private Iterator<Shipment> current = Collections.emptyIterator();

        private ZoneOrderIterator() {
            advanceToNextNonEmptyBucket();
        }

        @Override
        public boolean hasNext() {
            return current.hasNext();
        }

        @Override
        public Shipment next() {
            if (!current.hasNext()) {
                throw new NoSuchElementException();
            }
            Shipment shipment = current.next();
            advanceToNextNonEmptyBucket();
            return shipment;
        }

        private void advanceToNextNonEmptyBucket() {
            while (!current.hasNext() && zones.hasNext()) {
                current = byZone.get(zones.next()).iterator();
            }
        }
    }
}
