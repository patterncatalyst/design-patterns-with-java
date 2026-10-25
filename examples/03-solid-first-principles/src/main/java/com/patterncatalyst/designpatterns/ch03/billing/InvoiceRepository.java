package com.patterncatalyst.designpatterns.ch03.billing;

import java.util.Optional;

/** The persistence boundary for an invoice, separated from calculation and formatting. */
public interface InvoiceRepository {

    void save(Invoice invoice);

    Optional<Invoice> findById(String id);
}
