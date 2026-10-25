package com.patterncatalyst.designpatterns.ch03.billing;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceFormatterTest {

    @Test
    void formatsInvoiceIdCustomerLineItemsAndTotals() {
        Invoice invoice = new Invoice("INV-3087", "Anchor Point Imports", List.of(
                new LineItem("Freight handling", 2, 82_500),
                new LineItem("Warehouse storage", 6, 4_500)));
        Totals totals = new Totals(192_000, 201_260);

        String receipt = InvoiceFormatter.format(invoice, totals);

        assertThat(receipt)
                .contains("INV-3087")
                .contains("Anchor Point Imports")
                .contains("Freight handling")
                .contains("Warehouse storage")
                .contains("$1,920.00")
                .contains("$2,012.60");
    }

    @Test
    void formatterNeverComputesItsOwnTotals() {
        // The formatter is handed an already-computed Totals and never calls
        // InvoiceCalculator itself; a deliberately wrong Totals value proves
        // it, since a self-computing formatter could not be fed a false one.
        Invoice invoice = new Invoice("INV-1", "Anchor Point Imports",
                List.of(new LineItem("Freight handling", 1, 100_00)));
        Totals fabricatedTotals = new Totals(100_00, 999_99);

        String receipt = InvoiceFormatter.format(invoice, fabricatedTotals);

        assertThat(receipt).contains("$999.99");
    }
}
