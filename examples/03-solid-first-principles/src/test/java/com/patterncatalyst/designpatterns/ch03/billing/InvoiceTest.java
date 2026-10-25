package com.patterncatalyst.designpatterns.ch03.billing;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InvoiceTest {

    @Test
    void rejectsEmptyLineItems() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Invoice("INV-1", "Anchor Point Imports", List.of()))
                .withMessageContaining("line item");
    }

    @Test
    void defensivelyCopiesLineItemsSoCallerMutationDoesNotLeak() {
        List<LineItem> mutableItems = new ArrayList<>();
        mutableItems.add(new LineItem("Freight handling", 1, 82_500));
        Invoice invoice = new Invoice("INV-1", "Anchor Point Imports", mutableItems);

        mutableItems.add(new LineItem("Warehouse storage", 1, 4_500));

        assertThat(invoice.lineItems()).hasSize(1);
    }

    @Test
    void lineItemsListIsImmutable() {
        Invoice invoice = new Invoice("INV-1", "Anchor Point Imports",
                List.of(new LineItem("Freight handling", 1, 82_500)));

        assertThrows(UnsupportedOperationException.class,
                () -> invoice.lineItems().add(new LineItem("Warehouse storage", 1, 4_500)));
    }
}
