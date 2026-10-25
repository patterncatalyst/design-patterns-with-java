package com.patterncatalyst.designpatterns.ch05.shipping;

import java.io.IOException;
import java.nio.file.Path;

/**
 * The Execute-Around Method idiom applied to {@link BatchLedgerWriter}.
 * {@link #withLedger(Path, LedgerAction)} is the library method: it owns
 * opening the writer, committing once the caller's action returns
 * normally, and guaranteeing the writer closes on every exit path,
 * including one the caller's action throws. A caller never calls
 * {@code open()}, {@code commit()}, or {@code close()} directly; it
 * supplies only the business logic, as a lambda.
 */
public final class LedgerOperations {

    private LedgerOperations() {
    }

    public static <T> T withLedger(Path ledgerPath, LedgerAction<T> action) {
        return withLedgerWriter(() -> BatchLedgerWriter.open(ledgerPath), action);
    }

    // Package-private seam: lets the test suite substitute a spy BatchLedgerWriter
    // for the one withLedger(Path, LedgerAction) would open from disk, so a test can
    // verify close() runs on the exception path without depending on filesystem state.
    static <T> T withLedgerWriter(WriterSupplier supplier, LedgerAction<T> action) {
        try (BatchLedgerWriter writer = supplier.open()) {
            T result = action.writeTo(writer);
            writer.commit();
            return result;
        } catch (IOException e) {
            throw new LedgerWriteException("ledger write failed", e);
        }
    }

    @FunctionalInterface
    interface WriterSupplier {
        BatchLedgerWriter open() throws IOException;
    }
}
