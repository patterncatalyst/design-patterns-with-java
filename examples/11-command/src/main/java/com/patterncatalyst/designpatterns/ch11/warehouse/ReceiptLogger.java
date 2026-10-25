package com.patterncatalyst.designpatterns.ch11.warehouse;

/**
 * A plain callable for an action with nothing to undo: appending a line to a
 * restock receipt. A functional interface, satisfied by a lambda, is the
 * right shape here precisely because there is no second method, undo,
 * worth giving this action.
 */
@FunctionalInterface
public interface ReceiptLogger {

    void log(int quantity, String bin);
}
