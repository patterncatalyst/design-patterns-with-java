package com.patterncatalyst.designpatterns.ch11.warehouse;

import java.util.Objects;

/**
 * Moves a quantity of a SKU from one bin to another. Like
 * {@link AllocateStockCommand}, the inverse of "move N from A to B" is
 * simply "move N from B to A", computable from the same four values the
 * constructor already captured, so this command is just as safely a
 * {@code record} as the single-bin case.
 *
 * @param ledger the receiver this command mutates
 * @param sourceBin the bin stock moves out of
 * @param destinationBin the bin stock moves into
 * @param sku the item identifier
 * @param quantity the quantity moved
 */
public record TransferStockCommand(
        WarehouseLedger ledger, String sourceBin, String destinationBin, String sku, int quantity)
        implements WarehouseCommand {

    public TransferStockCommand {
        Objects.requireNonNull(ledger, "ledger");
        Objects.requireNonNull(sourceBin, "sourceBin");
        Objects.requireNonNull(destinationBin, "destinationBin");
        Objects.requireNonNull(sku, "sku");
    }

    @Override
    public void execute() {
        ledger.adjustStock(sourceBin, sku, -quantity);
        ledger.adjustStock(destinationBin, sku, quantity);
    }

    @Override
    public void undo() {
        ledger.adjustStock(destinationBin, sku, -quantity);
        ledger.adjustStock(sourceBin, sku, quantity);
    }
}
