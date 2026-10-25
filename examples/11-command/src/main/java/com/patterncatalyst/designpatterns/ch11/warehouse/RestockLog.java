package com.patterncatalyst.designpatterns.ch11.warehouse;

import java.util.List;
import java.util.Objects;

/**
 * Builds a {@link ReceiptLogger} that appends each logged restock to a
 * shared list of receipt lines. The lambda this factory returns captures
 * {@code receiptLines} and nothing else, no ledger, no undo state, because
 * logging that a restock happened needs nothing else to do its one job.
 */
public final class RestockLog {

    private RestockLog() {
    }

    public static ReceiptLogger appendingTo(List<String> receiptLines) {
        Objects.requireNonNull(receiptLines, "receiptLines");
        return (quantity, bin) -> receiptLines.add("+%d into %s".formatted(quantity, bin));
    }
}
