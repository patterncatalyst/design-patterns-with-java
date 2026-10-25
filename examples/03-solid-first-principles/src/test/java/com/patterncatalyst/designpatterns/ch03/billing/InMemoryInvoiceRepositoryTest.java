package com.patterncatalyst.designpatterns.ch03.billing;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryInvoiceRepositoryTest {

    private final InvoiceRepository repository = new InMemoryInvoiceRepository();

    @Test
    void savedInvoiceIsFoundById() {
        Invoice invoice = new Invoice("INV-3087", "Anchor Point Imports",
                List.of(new LineItem("Freight handling", 1, 82_500)));

        repository.save(invoice);

        assertThat(repository.findById("INV-3087")).contains(invoice);
    }

    @Test
    void unknownIdIsNotFound() {
        Optional<Invoice> found = repository.findById("does-not-exist");

        assertThat(found).isEmpty();
    }

    @Test
    void savingAgainWithSameIdReplacesThePreviousInvoice() {
        Invoice first = new Invoice("INV-3087", "Anchor Point Imports",
                List.of(new LineItem("Freight handling", 1, 82_500)));
        Invoice second = new Invoice("INV-3087", "Anchor Point Imports",
                List.of(new LineItem("Freight handling", 2, 82_500)));

        repository.save(first);
        repository.save(second);

        assertThat(repository.findById("INV-3087")).contains(second);
    }
}
