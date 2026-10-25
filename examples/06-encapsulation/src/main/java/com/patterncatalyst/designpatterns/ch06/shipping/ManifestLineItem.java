package com.patterncatalyst.designpatterns.ch06.shipping;

import java.util.Objects;

/**
 * A single line on a {@link Manifest}: a SKU, how many units, and a plain
 * description. A value in its own right, interchangeable with any other
 * line item carrying the same three facts.
 *
 * @param sku         the stock-keeping unit; never blank
 * @param quantity    units of this SKU on the manifest; always positive
 * @param description a plain-text description shown on the manifest
 */
public record ManifestLineItem(String sku, int quantity, String description) {

    public ManifestLineItem {
        Objects.requireNonNull(sku, "sku");
        if (sku.isBlank()) {
            throw new IllegalArgumentException("sku cannot be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
        Objects.requireNonNull(description, "description");
    }
}
