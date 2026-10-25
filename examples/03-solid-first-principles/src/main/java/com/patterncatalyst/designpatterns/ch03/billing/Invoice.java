package com.patterncatalyst.designpatterns.ch03.billing;

import java.util.List;
import java.util.Objects;

/**
 * A freight invoice: an id, a customer name, and the line items billed to
 * that customer. {@code lineItems} is defensively copied into an immutable
 * list so a caller holding the original mutable list cannot change an
 * invoice after it has been built.
 */
public record Invoice(String id, String customerName, List<LineItem> lineItems) {

    public Invoice {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(customerName, "customerName");
        Objects.requireNonNull(lineItems, "lineItems");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
        if (lineItems.isEmpty()) {
            throw new IllegalArgumentException("invoice must have at least one line item");
        }
        lineItems = List.copyOf(lineItems);
    }
}
