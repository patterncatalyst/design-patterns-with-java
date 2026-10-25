package com.patterncatalyst.designpatterns.ch05.shipping;

/**
 * Wraps an {@link java.io.IOException} raised while writing or committing a
 * ledger batch, so {@link LedgerOperations#withLedger} callers deal with an
 * unchecked failure instead of a checked one neither the caller nor the
 * lambda it passed in can realistically recover from.
 */
public final class LedgerWriteException extends RuntimeException {

    public LedgerWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
