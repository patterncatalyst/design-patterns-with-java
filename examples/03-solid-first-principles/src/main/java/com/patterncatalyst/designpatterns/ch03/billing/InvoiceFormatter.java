package com.patterncatalyst.designpatterns.ch03.billing;

import java.util.Locale;

/**
 * The one type in this example whose reason to change is "the receipt
 * should read differently." It knows nothing about how a {@link Totals}
 * value was derived and nothing about where an {@link Invoice} is stored; it
 * only turns the two into text.
 */
public final class InvoiceFormatter {

    private InvoiceFormatter() {
    }

    public static String format(Invoice invoice, Totals totals) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("Invoice ").append(invoice.id())
                .append(" - ").append(invoice.customerName()).append(System.lineSeparator());
        for (LineItem item : invoice.lineItems()) {
            receipt.append(String.format(Locale.US, "  %d x %-32s @ %10s = %10s%n",
                    item.quantity(), item.description(),
                    formatCents(item.unitPriceCents()), formatCents(item.extendedPriceCents())));
        }
        receipt.append(String.format(Locale.US, "  Subtotal: %s%n", formatCents(totals.subtotalCents())));
        receipt.append(String.format(Locale.US, "  Total:    %s%n", formatCents(totals.totalCents())));
        return receipt.toString();
    }

    private static String formatCents(long cents) {
        String sign = cents < 0 ? "-" : "";
        return String.format(Locale.US, "%s$%,.2f", sign, Math.abs(cents) / 100.0);
    }
}
