package com.patterncatalyst.designpatterns.ch11.warehouse;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CommandHistoryTest {

    @Test
    void executeRunsTheCommandImmediately() {
        WarehouseLedger ledger = new WarehouseLedger();
        CommandHistory history = new CommandHistory();

        history.execute(new AllocateStockCommand(ledger, "A1", "WIDGET-100", 50));

        assertThat(ledger.quantityOf("A1", "WIDGET-100")).isEqualTo(50);
    }

    @Test
    void aSequenceOfCommandsUndoesInReverseOrder() {
        WarehouseLedger ledger = new WarehouseLedger();
        CommandHistory history = new CommandHistory();

        history.execute(new AllocateStockCommand(ledger, "A1", "WIDGET-100", 50));
        history.execute(new TransferStockCommand(ledger, "A1", "PICK-FACE-3", "WIDGET-100", 20));
        history.execute(new RecountStockCommand(ledger, "PICK-FACE-3", "WIDGET-100", 12));

        assertThat(ledger.quantityOf("PICK-FACE-3", "WIDGET-100")).isEqualTo(12);

        // Undoing the recount first restores the transferred quantity.
        history.undo();
        assertThat(ledger.quantityOf("PICK-FACE-3", "WIDGET-100")).isEqualTo(20);

        // Undoing the transfer next moves the stock back to A1.
        history.undo();
        assertThat(ledger.quantityOf("A1", "WIDGET-100")).isEqualTo(50);
        assertThat(ledger.quantityOf("PICK-FACE-3", "WIDGET-100")).isZero();

        // Undoing the allocation last zeroes the bin entirely.
        history.undo();
        assertThat(ledger.quantityOf("A1", "WIDGET-100")).isZero();
    }

    @Test
    void redoReappliesAnUndoneCommand() {
        WarehouseLedger ledger = new WarehouseLedger();
        CommandHistory history = new CommandHistory();
        history.execute(new AllocateStockCommand(ledger, "A1", "WIDGET-100", 50));

        history.undo();
        assertThat(ledger.quantityOf("A1", "WIDGET-100")).isZero();

        history.redo();
        assertThat(ledger.quantityOf("A1", "WIDGET-100")).isEqualTo(50);
    }

    @Test
    void executingANewCommandClearsTheRedoBranch() {
        WarehouseLedger ledger = new WarehouseLedger();
        CommandHistory history = new CommandHistory();
        history.execute(new AllocateStockCommand(ledger, "A1", "WIDGET-100", 50));
        history.undo();

        history.execute(new AllocateStockCommand(ledger, "A1", "WIDGET-100", 10));

        assertThat(history.canRedo()).isFalse();
        assertThat(history.redo()).isFalse();
        assertThat(ledger.quantityOf("A1", "WIDGET-100")).isEqualTo(10);
    }

    @Test
    void undoAndRedoReturnFalseWithNoEffectWhenNothingIsAvailable() {
        CommandHistory history = new CommandHistory();

        assertThat(history.canUndo()).isFalse();
        assertThat(history.canRedo()).isFalse();
        assertThat(history.undo()).isFalse();
        assertThat(history.redo()).isFalse();
    }
}
