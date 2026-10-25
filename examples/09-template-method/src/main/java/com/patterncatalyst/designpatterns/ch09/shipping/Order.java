package com.patterncatalyst.designpatterns.ch09.shipping;

import java.util.List;
import java.util.Objects;

/**
 * An order headed for fulfillment through exactly one channel pipeline.
 * {@code marketplaceReference} is blank for a retail or wholesale order and
 * required for a marketplace order. Whether a blank reference is acceptable
 * is itself a channel rule, one of the varying steps a pipeline supplies
 * rather than something this record enforces on every order alike.
 */
public record Order(String orderId, String customerName, List<OrderLine> lines, String marketplaceReference) {

    public Order {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }
        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("customerName must not be blank");
        }
        Objects.requireNonNull(lines, "lines");
        lines = List.copyOf(lines);
        marketplaceReference = marketplaceReference == null ? "" : marketplaceReference;
    }
}
