package com.patterncatalyst.designpatterns.ch11.warehouse;

import java.util.Objects;

/**
 * Receives a quantity of a SKU into a bin. This command's undo is a clean
 * arithmetic inverse of its execute, known in full at construction time, so
 * nothing about it needs to capture state later: a {@code record} holds
 * exactly the three values both methods need and nothing more.
 *
 * @param ledger the receiver this command mutates
 * @param bin the bin receiving stock
 * @param sku the item identifier
 * @param quantity the quantity received; its undo subtracts the same amount
 */
public record AllocateStockCommand(WarehouseLedger ledger, String bin, String sku, int quantity)
        implements WarehouseCommand {

    public AllocateStockCommand {
        Objects.requireNonNull(ledger, "ledger");
        Objects.requireNonNull(bin, "bin");
        Objects.requireNonNull(sku, "sku");
    }

    @Override
    public void execute() {
        ledger.adjustStock(bin, sku, quantity);
    }

    @Override
    public void undo() {
        ledger.adjustStock(bin, sku, -quantity);
    }
}
