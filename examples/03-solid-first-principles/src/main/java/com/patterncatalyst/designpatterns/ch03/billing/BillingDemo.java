package com.patterncatalyst.designpatterns.ch03.billing;

import java.util.List;
import java.util.Locale;

/**
 * Prices one freight invoice three ways so SRP, OCP, and LSP are observable
 * at the terminal rather than only inside an assertion. No containers: this
 * class is exercised directly with {@code mvn exec:java} by {@code demo.sh}.
 */
public final class BillingDemo {

    private BillingDemo() {
    }

    public static void main(String[] args) {
        Invoice invoice = new Invoice("INV-3087", "Anchor Point Imports", List.of(
                new LineItem("Freight handling (40-ft container)", 2, 82_500),
                new LineItem("Warehouse storage (per week)", 6, 4_500)));

        System.out.println("== Single Responsibility: calculate, format, persist ==");
        InvoiceCalculator calculator = new InvoiceCalculator(List.of(
                new PercentageTaxRule("sales tax", 0.08),
                new StandardDiscountRule("contract discount", 0.05),
                new FlatServiceFeeRule("dock handling fee", 3_500)));
        Totals totals = calculator.totalsFor(invoice);
        System.out.print(InvoiceFormatter.format(invoice, totals));

        InvoiceRepository repository = new InMemoryInvoiceRepository();
        repository.save(invoice);
        boolean reloaded = repository.findById(invoice.id()).isPresent();
        System.out.println("Persisted and reloaded by id: " + (reloaded ? "yes" : "no"));

        System.out.println();
        System.out.println("== Open/Closed: three rule kinds, InvoiceCalculator never edited ==");
        for (InvoiceRule rule : List.of(
                new PercentageTaxRule("sales tax", 0.08),
                new StandardDiscountRule("contract discount", 0.05),
                new FlatServiceFeeRule("dock handling fee", 3_500))) {
            System.out.println(rule.name() + ": " + formatCents(rule.adjustmentCents(totals.subtotalCents())));
        }
        System.out.println("Invoice total: " + formatCents(totals.totalCents()));

        System.out.println();
        System.out.println("== Liskov Substitution: a loyalty discount stands in for the base rule ==");
        InvoiceCalculator loyaltyCalculator = new InvoiceCalculator(List.of(
                new PercentageTaxRule("sales tax", 0.08),
                new LoyaltyDiscountRule("loyalty discount", 0.05, 0.01, 6),
                new FlatServiceFeeRule("dock handling fee", 3_500)));
        Totals loyaltyTotals = loyaltyCalculator.totalsFor(invoice);
        System.out.println("Invoice total for a 6-year loyalty customer: " + formatCents(loyaltyTotals.totalCents()));

        System.out.println();
        System.out.println("== Liskov Substitution violated: a rule that skips the clamp ==");
        InvoiceCalculator brokenCalculator = new InvoiceCalculator(List.of(
                new PercentageTaxRule("sales tax", 0.08),
                new ClearanceEventDiscountRule("clearance event (150% off)", 1.5),
                new FlatServiceFeeRule("dock handling fee", 3_500)));
        Totals brokenTotals = brokenCalculator.totalsFor(invoice);
        System.out.println("Invoice total with the unclamped rule: " + formatCents(brokenTotals.totalCents())
                + " (negative: the invariant every other rule honors)");
    }

    private static String formatCents(long cents) {
        String sign = cents < 0 ? "-" : "";
        return String.format(Locale.US, "%s$%,.2f", sign, Math.abs(cents) / 100.0);
    }
}
