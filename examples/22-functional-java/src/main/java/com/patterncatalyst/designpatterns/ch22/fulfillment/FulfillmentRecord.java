package com.patterncatalyst.designpatterns.ch22.fulfillment;

import java.util.Objects;

/**
 * The outcome of running a {@link Shipment} through
 * {@link FulfillmentPipeline#process}. {@code manifestId} and
 * {@code rejectionReason} are plain {@code String} fields, not
 * {@code Optional<String>} fields: {@code manifestId} is empty on a
 * rejected record and {@code rejectionReason} is empty on an accepted one,
 * which is exactly the "absent most of the time" case {@code Optional} is
 * for on a method's return value, but the wrong fit for a value this record
 * carries permanently once built. A caller reads {@link #accepted()} to
 * decide which of the two fields matters.
 */
public record FulfillmentRecord(
        String manifestId,
        Shipment shipment,
        Money total,
        boolean accepted,
        String rejectionReason) {

    public FulfillmentRecord {
        Objects.requireNonNull(manifestId, "manifestId");
        Objects.requireNonNull(shipment, "shipment");
        Objects.requireNonNull(total, "total");
        Objects.requireNonNull(rejectionReason, "rejectionReason");
    }

    public static FulfillmentRecord accepted(String manifestId, Shipment shipment, Money total) {
        return new FulfillmentRecord(manifestId, shipment, total, true, "");
    }

    public static FulfillmentRecord rejected(Shipment shipment, String reason) {
        return new FulfillmentRecord("", shipment, Money.ZERO, false, reason);
    }
}
