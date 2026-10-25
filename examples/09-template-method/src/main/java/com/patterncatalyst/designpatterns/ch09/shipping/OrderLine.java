package com.patterncatalyst.designpatterns.ch09.shipping;

/**
 * One line of an order: a SKU, a quantity a warehouse picker already
 * confirmed, and the list unit price in cents. Money stays an integer cents
 * value everywhere in this example, never a {@code double}, so a quote is
 * exact and a test assertion can compare it for equality with no epsilon.
 */
public record OrderLine(String sku, int quantity, long unitPriceCents) {

    public OrderLine {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("sku must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
        if (unitPriceCents < 0) {
            throw new IllegalArgumentException("unitPriceCents cannot be negative: " + unitPriceCents);
        }
    }
}
