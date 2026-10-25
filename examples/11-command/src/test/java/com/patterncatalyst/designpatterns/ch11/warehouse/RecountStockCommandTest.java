package com.patterncatalyst.designpatterns.ch11.warehouse;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RecountStockCommandTest {

    @Test
    void executeSetsTheQuantityToTheCountedValue() {
        WarehouseLedger ledger = new WarehouseLedger();
        ledger.adjustStock("PICK-FACE-3", "WIDGET-100", 20);
        RecountStockCommand command = new RecountStockCommand(ledger, "PICK-FACE-3", "WIDGET-100", 12);

        command.execute();

        assertThat(ledger.quantityOf("PICK-FACE-3", "WIDGET-100")).isEqualTo(12);
    }

    @Test
    void undoRestoresTheQuantityObservedWhenExecuteRan() {
        WarehouseLedger ledger = new WarehouseLedger();
        ledger.adjustStock("PICK-FACE-3", "WIDGET-100", 20);
        RecountStockCommand command = new RecountStockCommand(ledger, "PICK-FACE-3", "WIDGET-100", 12);

        command.execute();
        command.undo();

        assertThat(ledger.quantityOf("PICK-FACE-3", "WIDGET-100")).isEqualTo(20);
    }

    @Test
    void executeCapturesPriorQuantityAtExecuteTimeNotConstructionTime() {
        WarehouseLedger ledger = new WarehouseLedger();
        ledger.adjustStock("PICK-FACE-3", "WIDGET-100", 20);
        RecountStockCommand command = new RecountStockCommand(ledger, "PICK-FACE-3", "WIDGET-100", 12);

        // Something else touches the same bin/SKU after the command is built
        // but before it runs, the way a queued command would sit behind
        // other work. A constructor-time capture would have recorded the
        // stale value of 20; undo must still reverse against whatever
        // execute() actually saw.
        ledger.adjustStock("PICK-FACE-3", "WIDGET-100", 8);
        command.execute();

        assertThat(ledger.quantityOf("PICK-FACE-3", "WIDGET-100")).isEqualTo(12);

        command.undo();

        assertThat(ledger.quantityOf("PICK-FACE-3", "WIDGET-100")).isEqualTo(28);
    }
}
