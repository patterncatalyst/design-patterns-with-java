package com.patterncatalyst.designpatterns.ch11.warehouse;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs every command in this example end to end against one
 * {@link WarehouseLedger}, printing the ledger state after each step so the
 * undo/redo stack, the FIFO queue, and the functional shortcut are something
 * you can watch happen at the terminal, not just assertions that say they
 * work.
 */
public final class WarehouseDemo {

    private WarehouseDemo() {
    }

    public static void main(String[] args) {
        WarehouseLedger ledger = new WarehouseLedger();
        CommandHistory history = new CommandHistory();

        System.out.println("== Command objects: execute changes state, undo restores it ==");
        history.execute(new AllocateStockCommand(ledger, "A1", "WIDGET-100", 50));
        System.out.println("A1/WIDGET-100 after AllocateStockCommand: " + ledger.quantityOf("A1", "WIDGET-100"));

        history.execute(new TransferStockCommand(ledger, "A1", "PICK-FACE-3", "WIDGET-100", 20));
        System.out.println("A1/WIDGET-100 after TransferStockCommand: " + ledger.quantityOf("A1", "WIDGET-100"));
        System.out.println(
                "PICK-FACE-3/WIDGET-100 after TransferStockCommand: " + ledger.quantityOf("PICK-FACE-3", "WIDGET-100"));

        history.undo();
        System.out.println("A1/WIDGET-100 after undo: " + ledger.quantityOf("A1", "WIDGET-100"));
        System.out.println("PICK-FACE-3/WIDGET-100 after undo: " + ledger.quantityOf("PICK-FACE-3", "WIDGET-100"));

        history.redo();
        System.out.println("PICK-FACE-3/WIDGET-100 after redo: " + ledger.quantityOf("PICK-FACE-3", "WIDGET-100"));

        System.out.println();
        System.out.println("== A cycle-count correction and a cancelled order line ==");
        history.execute(new RecountStockCommand(ledger, "PICK-FACE-3", "WIDGET-100", 12));
        System.out.println(
                "PICK-FACE-3/WIDGET-100 after cycle-count correction: " + ledger.quantityOf("PICK-FACE-3", "WIDGET-100"));

        ledger.addOrderLine("LINE-9001", new OrderLine("WIDGET-100", "PICK-FACE-3", 5, false));
        history.execute(new CancelOrderLineCommand(ledger, "LINE-9001"));
        System.out.println(
                "PICK-FACE-3/WIDGET-100 after cancelling LINE-9001: " + ledger.quantityOf("PICK-FACE-3", "WIDGET-100"));

        history.undo();
        System.out.println(
                "PICK-FACE-3/WIDGET-100 after undoing the cancellation: " + ledger.quantityOf("PICK-FACE-3", "WIDGET-100"));

        System.out.println();
        System.out.println("== Command queue: deferred, batched, FIFO execution ==");
        CommandQueue queue = new CommandQueue();
        queue.enqueue(new AllocateStockCommand(ledger, "B2", "GASKET-7", 25));
        queue.enqueue(new TransferStockCommand(ledger, "B2", "PICK-FACE-1", "GASKET-7", 10));
        queue.enqueue(new RecountStockCommand(ledger, "B2", "GASKET-7", 18));
        System.out.println("Pending before processing: " + queue.pendingCount());
        int processed = queue.processAll(history);
        System.out.println("Processed in submission order: " + processed);
        System.out.println("B2/GASKET-7 after the batch: " + ledger.quantityOf("B2", "GASKET-7"));
        System.out.println("PICK-FACE-1/GASKET-7 after the batch: " + ledger.quantityOf("PICK-FACE-1", "GASKET-7"));

        System.out.println();
        System.out.println("== ReceiptLogger shortcut: a queued action with nothing to undo ==");
        List<String> receiptLines = new ArrayList<>();
        ReceiptLogger logger = RestockLog.appendingTo(receiptLines);
        logger.log(30, "B2");
        logger.log(5, "B2");
        receiptLines.forEach(line -> System.out.println("GASKET-7: " + line));
    }
}
