package com.patterncatalyst.designpatterns.ch03.billing;

/**
 * The result of pricing an invoice: the subtotal before any rule ran, and
 * the total after every rule's adjustment has been applied.
 */
public record Totals(long subtotalCents, long totalCents) {
}
