package com.patterncatalyst.designpatterns.ch11.warehouse;

import java.util.List;
import java.util.Objects;

/**
 * The same restock-logging action as {@link RestockLog#appendingTo(List)},
 * built as a full {@link WarehouseCommand} instead of a plain lambda. There
 * is nothing to reverse about having already logged a line, so
 * {@link #undo()} has no real implementation to give; it throws rather
 * than silently doing nothing, so a caller that mistakenly pushes this
 * command onto a {@link CommandHistory} and tries to undo it fails loudly
 * instead of leaving a stale receipt line behind. This class exists to show
 * the cost of the full Command ceremony for an action that never needed it,
 * not as the recommended way to log a restock.
 */
public final class LogRestockCommand implements WarehouseCommand {

    private final List<String> receiptLines;
    private final int quantity;
    private final String bin;

    public LogRestockCommand(List<String> receiptLines, int quantity, String bin) {
        this.receiptLines = Objects.requireNonNull(receiptLines, "receiptLines");
        this.quantity = quantity;
        this.bin = Objects.requireNonNull(bin, "bin");
    }

    @Override
    public void execute() {
        receiptLines.add("+%d into %s".formatted(quantity, bin));
    }

    @Override
    public void undo() {
        throw new UnsupportedOperationException(
                "a restock log entry has no undo: there is nothing to reverse about having already logged it");
    }
}
