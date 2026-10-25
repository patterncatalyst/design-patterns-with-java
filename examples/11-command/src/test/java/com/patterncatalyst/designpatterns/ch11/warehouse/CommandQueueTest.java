package com.patterncatalyst.designpatterns.ch11.warehouse;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CommandQueueTest {

    @Test
    void pendingCountTracksEnqueuedCommands() {
        CommandQueue queue = new CommandQueue();
        WarehouseLedger ledger = new WarehouseLedger();

        queue.enqueue(new AllocateStockCommand(ledger, "B2", "GASKET-7", 25));
        queue.enqueue(new AllocateStockCommand(ledger, "B2", "GASKET-7", 5));

        assertThat(queue.pendingCount()).isEqualTo(2);
    }

    @Test
    void processAllRunsCommandsInSubmissionOrderNotReverseOrder() {
        WarehouseLedger ledger = new WarehouseLedger();
        CommandQueue queue = new CommandQueue();
        // Submitted in an order that only produces a sensible final state if
        // processed FIFO: receive 25 into B2 before 10 of it transfers out.
        // Running the transfer first would leave B2 short.
        queue.enqueue(new AllocateStockCommand(ledger, "B2", "GASKET-7", 25));
        queue.enqueue(new TransferStockCommand(ledger, "B2", "PICK-FACE-1", "GASKET-7", 10));

        int processed = queue.processAll();

        assertThat(processed).isEqualTo(2);
        assertThat(ledger.quantityOf("B2", "GASKET-7")).isEqualTo(15);
        assertThat(ledger.quantityOf("PICK-FACE-1", "GASKET-7")).isEqualTo(10);
    }

    @Test
    void processAllDrainsTheQueueToEmpty() {
        WarehouseLedger ledger = new WarehouseLedger();
        CommandQueue queue = new CommandQueue();
        queue.enqueue(new AllocateStockCommand(ledger, "B2", "GASKET-7", 25));
        queue.enqueue(new AllocateStockCommand(ledger, "B2", "GASKET-7", 5));

        queue.processAll();

        assertThat(queue.pendingCount()).isZero();
    }

    @Test
    void processAllWithHistoryRecordsEachCommandSoTheBatchStaysUndoable() {
        WarehouseLedger ledger = new WarehouseLedger();
        CommandQueue queue = new CommandQueue();
        CommandHistory history = new CommandHistory();
        queue.enqueue(new AllocateStockCommand(ledger, "B2", "GASKET-7", 25));
        queue.enqueue(new TransferStockCommand(ledger, "B2", "PICK-FACE-1", "GASKET-7", 10));

        queue.processAll(history);
        assertThat(ledger.quantityOf("B2", "GASKET-7")).isEqualTo(15);

        history.undo();
        assertThat(ledger.quantityOf("B2", "GASKET-7")).isEqualTo(25);
        assertThat(ledger.quantityOf("PICK-FACE-1", "GASKET-7")).isZero();

        history.undo();
        assertThat(ledger.quantityOf("B2", "GASKET-7")).isZero();
    }

    @Test
    void processAllWithoutHistoryLeavesNothingToUndo() {
        WarehouseLedger ledger = new WarehouseLedger();
        CommandQueue queue = new CommandQueue();
        CommandHistory history = new CommandHistory();
        queue.enqueue(new AllocateStockCommand(ledger, "B2", "GASKET-7", 25));

        queue.processAll();

        assertThat(ledger.quantityOf("B2", "GASKET-7")).isEqualTo(25);
        assertThat(history.undo()).isFalse();
    }
}
