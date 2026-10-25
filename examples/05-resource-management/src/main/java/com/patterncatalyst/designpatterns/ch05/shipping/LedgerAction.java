package com.patterncatalyst.designpatterns.ch05.shipping;

import java.io.IOException;

/**
 * The business logic a caller of {@link LedgerOperations#withLedger} hands
 * in as a lambda. It receives an already-open {@link BatchLedgerWriter} and
 * never opens, commits, or closes one itself.
 */
@FunctionalInterface
public interface LedgerAction<T> {
    T writeTo(BatchLedgerWriter writer) throws IOException;
}
