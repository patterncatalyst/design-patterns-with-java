package com.patterncatalyst.designpatterns.ch11.warehouse;

import java.util.Objects;

/**
 * Sets a bin/SKU's quantity to a counted value after a cycle count. "Set the
 * quantity to 12" has no arithmetic opposite the way a receipt or a transfer
 * does; the only way to undo it is to remember what the quantity was before
 * this command ran. That prior value is captured inside {@link #execute()},
 * not the constructor, which is exactly why this command is a mutable class
 * rather than a {@code record}: a command built now and run later, after a
 * {@link CommandQueue} has drained other commands ahead of it, must read the
 * quantity the moment it runs, not the moment it was constructed.
 */
public final class RecountStockCommand implements WarehouseCommand {

    private final WarehouseLedger ledger;
    private final String bin;
    private final String sku;
    private final int countedQuantity;
    private int priorQuantity;

    public RecountStockCommand(WarehouseLedger ledger, String bin, String sku, int countedQuantity) {
        this.ledger = Objects.requireNonNull(ledger, "ledger");
        this.bin = Objects.requireNonNull(bin, "bin");
        this.sku = Objects.requireNonNull(sku, "sku");
        this.countedQuantity = countedQuantity;
    }

    @Override
    public void execute() {
        priorQuantity = ledger.quantityOf(bin, sku);
        ledger.adjustStock(bin, sku, countedQuantity - priorQuantity);
    }

    @Override
    public void undo() {
        ledger.adjustStock(bin, sku, priorQuantity - countedQuantity);
    }
}
