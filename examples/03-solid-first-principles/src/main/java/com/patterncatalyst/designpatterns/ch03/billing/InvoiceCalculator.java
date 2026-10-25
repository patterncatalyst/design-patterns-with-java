package com.patterncatalyst.designpatterns.ch03.billing;

import java.util.List;
import java.util.Objects;

/**
 * The one type in this example whose reason to change is "the math changed."
 * It never formats a receipt and never persists anything; {@link
 * InvoiceFormatter} and {@link InMemoryInvoiceRepository} own those jobs
 * instead. It depends on {@link InvoiceRule} and nothing more concrete, so a
 * brand-new rule type never requires an edit here.
 */
public final class InvoiceCalculator {

    private final List<InvoiceRule> rules;

    public InvoiceCalculator(List<InvoiceRule> rules) {
        this.rules = List.copyOf(Objects.requireNonNull(rules, "rules"));
    }

    /**
     * Totals an invoice by summing its line items into a subtotal, then
     * applying every rule's adjustment to that same original subtotal. No
     * rule ever sees a running total another rule already modified, so each
     * rule's result is independent of the order the rules were supplied in.
     */
    public Totals totalsFor(Invoice invoice) {
        Objects.requireNonNull(invoice, "invoice");
        long subtotalCents = subtotalOf(invoice);
        long totalCents = subtotalCents;
        for (InvoiceRule rule : rules) {
            totalCents += rule.adjustmentCents(subtotalCents);
        }
        return new Totals(subtotalCents, totalCents);
    }

    private static long subtotalOf(Invoice invoice) {
        long subtotal = 0L;
        for (LineItem item : invoice.lineItems()) {
            subtotal += item.extendedPriceCents();
        }
        return subtotal;
    }
}
