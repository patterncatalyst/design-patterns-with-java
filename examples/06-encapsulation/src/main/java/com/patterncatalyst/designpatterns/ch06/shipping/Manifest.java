package com.patterncatalyst.designpatterns.ch06.shipping;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * The bill of lading for a shipment: a stable id, the line items it carries,
 * and when it was prepared. {@code items} is defended by {@link
 * List#copyOf(java.util.Collection)} in the compact constructor below, the
 * same one-call idiom {@link TrackingHistory} uses, so a caller's list
 * cannot be mutated after construction to change what this manifest reports,
 * and {@link #items()} cannot be mutated by whoever calls it either. The
 * equivalent hand-written form takes two separate steps: a defensive copy
 * into an {@code ArrayList} in the constructor, then a
 * {@code Collections.unmodifiableList} wrap before the getter returns;
 * {@code List.copyOf} does both in the one call below.
 *
 * @param manifestId a stable identifier; never blank
 * @param items      the line items on this manifest; never empty
 * @param preparedAt when this manifest was finalized
 */
public record Manifest(String manifestId, List<ManifestLineItem> items, Instant preparedAt) {

    public Manifest {
        Objects.requireNonNull(manifestId, "manifestId");
        if (manifestId.isBlank()) {
            throw new IllegalArgumentException("manifestId cannot be blank");
        }
        Objects.requireNonNull(items, "items");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("a manifest needs at least one line item");
        }
        items = List.copyOf(items);
        Objects.requireNonNull(preparedAt, "preparedAt");
    }

    public int totalUnits() {
        return items.stream().mapToInt(ManifestLineItem::quantity).sum();
    }
}
