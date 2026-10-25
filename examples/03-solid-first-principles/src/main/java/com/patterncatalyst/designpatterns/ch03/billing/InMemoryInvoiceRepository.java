package com.patterncatalyst.designpatterns.ch03.billing;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The one type in this example whose reason to change is "where invoices are
 * stored changed." It never computes a total and never renders a receipt;
 * it remembers an {@link Invoice} by id and hands it back.
 */
public final class InMemoryInvoiceRepository implements InvoiceRepository {

    private final Map<String, Invoice> invoicesById = new HashMap<>();

    @Override
    public void save(Invoice invoice) {
        Objects.requireNonNull(invoice, "invoice");
        invoicesById.put(invoice.id(), invoice);
    }

    @Override
    public Optional<Invoice> findById(String id) {
        return Optional.ofNullable(invoicesById.get(id));
    }
}
