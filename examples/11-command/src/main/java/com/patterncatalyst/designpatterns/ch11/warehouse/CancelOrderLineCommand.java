package com.patterncatalyst.designpatterns.ch11.warehouse;

import java.util.Objects;

/**
 * Cancels a picked order line: its reserved quantity returns to the bin it
 * came from, and the line is marked cancelled. Undoing that reverses both
 * parts at once. Like {@link RecountStockCommand}, this command captures its
 * prior state, the whole {@link OrderLine}, inside {@link #execute()} rather
 * than the constructor, so {@link #undo()} restores the line exactly as it
 * was, including a {@code cancelled} flag that was already {@code true} if
 * this line had somehow been cancelled before.
 */
public final class CancelOrderLineCommand implements WarehouseCommand {

    private final WarehouseLedger ledger;
    private final String lineId;
    private OrderLine priorState;

    public CancelOrderLineCommand(WarehouseLedger ledger, String lineId) {
        this.ledger = Objects.requireNonNull(ledger, "ledger");
        this.lineId = Objects.requireNonNull(lineId, "lineId");
    }

    @Override
    public void execute() {
        priorState = ledger.orderLine(lineId);
        ledger.adjustStock(priorState.bin(), priorState.sku(), priorState.quantity());
        ledger.setLineState(lineId, 0, true);
    }

    @Override
    public void undo() {
        ledger.adjustStock(priorState.bin(), priorState.sku(), -priorState.quantity());
        ledger.setLineState(lineId, priorState.quantity(), priorState.cancelled());
    }
}
