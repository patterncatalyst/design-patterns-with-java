package com.patterncatalyst.designpatterns.ch11.warehouse;

import java.util.Objects;

/**
 * A picked order line: how much of which SKU was reserved out of which bin,
 * and whether the line has since been cancelled.
 *
 * @param sku the item identifier
 * @param bin the bin the quantity was reserved from
 * @param quantity the reserved quantity; zero once the line is cancelled
 * @param cancelled whether this line has been cancelled
 */
public record OrderLine(String sku, String bin, int quantity, boolean cancelled) {

    public OrderLine {
        Objects.requireNonNull(sku, "sku");
        Objects.requireNonNull(bin, "bin");
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity cannot be negative: " + quantity);
        }
    }
}
